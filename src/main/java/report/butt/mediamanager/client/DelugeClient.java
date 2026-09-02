package report.butt.mediamanager.client;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import com.google.errorprone.annotations.Var;
import com.newrelic.api.agent.HeaderType;
import com.newrelic.api.agent.Headers;
import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Trace;

import report.butt.mediamanager.model.deluge.DelugeResponse;
import report.butt.mediamanager.model.deluge.DelugeTorrent;

/**
 * Talks to Deluge's JSON-RPC endpoint ({@code POST /json}). Deluge
 * authenticates with a {@code _session_id} cookie
 * handed back by {@code auth.login}; RestClient doesn't persist cookies, so we
 * capture it on login and replay it on
 * each call. A missing or expired session comes back as an RPC {@code error} in
 * a 200 body (not an HTTP error), so
 * calls re-authenticate and retry once.
 */
@Service
@NullMarked
public class DelugeClient {

    private static final Logger log = LoggerFactory.getLogger(DelugeClient.class);

    private static final ParameterizedTypeReference<DelugeResponse<Boolean>> LOGIN_RESPONSE_TYPE = new ParameterizedTypeReference<>() {
    };
    private static final ParameterizedTypeReference<DelugeResponse<Map<String, DelugeTorrent>>> TORRENTS_RESPONSE_TYPE = new ParameterizedTypeReference<>() {
    };

    private static final List<String> TORRENT_STATUS_FIELDS = List.of("name", "progress", "state", "num_peers",
            "num_seeds", "total_peers", "total_seeds", "time_added");

    private final RestClient restClient;
    private final String password;
    private volatile @Nullable String sessionCookie;

    public DelugeClient(
            RestClient.Builder builder,
            @Value("${deluge.url}") String delugeUrl,
            @Value("${deluge.password}") String delugePassword) {
        this.password = delugePassword;
        this.restClient = builder.baseUrl(delugeUrl).build();
    }

    /**
     * Returns the current torrents keyed by info hash, or an empty map if the call
     * fails.
     */
    @Trace
    public Map<String, DelugeTorrent> getTorrentsStatus() {
        @Var
        DelugeResponse<Map<String, DelugeTorrent>> response = requestTorrentsStatus();
        if (response != null && response.getError() != null) {
            login();
            response = requestTorrentsStatus();
        }
        if (response == null || response.getError() != null || response.getResult() == null) {
            log.warn("Deluge core.get_torrents_status failed: {}", describeError(response));
            return Map.of();
        }
        return response.getResult();
    }

    @Trace
    private @Nullable DelugeResponse<Map<String, DelugeTorrent>> requestTorrentsStatus() {
        return restClient
                .post()
                .uri("/json")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .headers(this::applySessionCookie)
                .headers(DelugeClient::insertDistributedTraceHeaders)
                .body(Map.of(
                        "method",
                        "core.get_torrents_status",
                        "params",
                        List.of(Map.of(), TORRENT_STATUS_FIELDS),
                        "id",
                        3))
                .retrieve()
                .body(TORRENTS_RESPONSE_TYPE);
    }

    private void login() {
        ResponseEntity<DelugeResponse<Boolean>> entity = restClient
                .post()
                .uri("/json")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .body(Map.of("method", "auth.login", "params", List.of(password), "id", 1))
                .retrieve()
                .toEntity(LOGIN_RESPONSE_TYPE);

        String setCookie = entity.getHeaders().getFirst(HttpHeaders.SET_COOKIE);
        if (setCookie != null) {
            this.sessionCookie = setCookie.split(";", 2)[0];
        }

        DelugeResponse<Boolean> body = entity.getBody();
        if (body == null || !Objects.equals(body.getResult(), true)) {
            throw new IllegalStateException("Deluge auth.login failed: " + describeError(body));
        }
        log.info("Authenticated with Deluge");
    }

    private void applySessionCookie(HttpHeaders headers) {
        if (sessionCookie != null) {
            headers.add(HttpHeaders.COOKIE, sessionCookie);
        }
    }

    /**
     * Writes the current transaction's distributed-trace headers onto the outgoing
     * request. Invoked from a RestClient
     * headers callback, which runs synchronously on the calling thread while the
     * {@code @Trace} transaction is still
     * active — the agent needs that context to produce the headers.
     */
    private static void insertDistributedTraceHeaders(HttpHeaders headers) {
        NewRelic.getAgent().getTransaction().insertDistributedTraceHeaders(new HttpHeadersAdapter(headers));
    }

    /**
     * Exposes Spring's {@link HttpHeaders} through the agent's {@link Headers}
     * view.
     */
    private record HttpHeadersAdapter(HttpHeaders headers) implements Headers {

        @Override
        public HeaderType getHeaderType() {
            return HeaderType.HTTP;
        }

        @Override
        public @Nullable String getHeader(String name) {
            return headers.getFirst(name);
        }

        @Override
        public List<String> getHeaders(String name) {
            return headers.getOrEmpty(name);
        }

        @Override
        public void setHeader(String name, String value) {
            headers.set(name, value);
        }

        @Override
        public void addHeader(String name, String value) {
            headers.add(name, value);
        }

        @Override
        public Set<String> getHeaderNames() {
            return headers.headerNames();
        }

        @Override
        public boolean containsHeader(String name) {
            return headers.containsHeader(name);
        }
    }

    private static @Nullable String describeError(@Nullable DelugeResponse<?> response) {
        if (response == null || response.getError() == null) {
            return "no response body";
        }
        return response.getError().getMessage();
    }
}
