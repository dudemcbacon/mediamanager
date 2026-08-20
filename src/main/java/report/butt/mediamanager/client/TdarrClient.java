package report.butt.mediamanager.client;

import com.google.errorprone.annotations.Var;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import report.butt.mediamanager.model.tdarr.TdarrFile;
import report.butt.mediamanager.model.tdarr.TdarrSearchResponse;

/**
 * Looks up a media file in Tdarr's file table ({@code POST /api/v2/client/search}) to read back its health-check and
 * transcode verdicts. The table's {@code file} filter matches as a substring, so only the filename is needed — a file
 * at {@code /storage/media/Movies/1992 (2024)/1992 (2024).mkv} is found by searching {@code 1992 (2024).mkv}.
 *
 * <p>Authentication is HTTP basic on the first request. If the response hands back a session cookie, that cookie is
 * replayed on subsequent requests instead of the credentials, and basic auth is retried once when a cookie request
 * comes back 401/403 (expired session). {@code RestClient} does not persist cookies, hence the manual handling — the
 * same approach as {@link DelugeClient}. When no cookie is offered at all (e.g. Tdarr behind a proxy that authenticates
 * every request), basic auth simply keeps being sent.
 *
 * <p>Every failure is swallowed and reported as {@code null}: Tdarr data is supplementary enrichment, so an outage must
 * not fail a refresh.
 */
@Service
@NullMarked
public class TdarrClient {

    private static final Logger log = LoggerFactory.getLogger(TdarrClient.class);

    /** Enough hits to spot an ambiguous filename without pulling the whole table — each document is several KB. */
    private static final int PAGE_SIZE = 10;

    private final RestClient restClient;
    private final String basicAuth;

    /** False when {@code tdarr.url} is unset, in which case every lookup short-circuits instead of calling out. */
    private final boolean configured;

    private volatile @Nullable String sessionCookie;

    public TdarrClient(
            RestClient.Builder builder,
            @Value("${tdarr.url}") String tdarrUrl,
            @Value("${tdarr.username}") String tdarrUsername,
            @Value("${tdarr.password}") String tdarrPassword) {
        this.configured = !tdarrUrl.isBlank();
        this.restClient = builder.baseUrl(tdarrUrl).build();
        this.basicAuth = "Basic "
                + Base64.getEncoder()
                        .encodeToString((tdarrUsername + ":" + tdarrPassword).getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Finds the Tdarr record for the file at {@code fullPath}, searching on its filename alone, or null when Tdarr is
     * unconfigured, knows nothing about the file, returns several equally plausible matches, or cannot be reached.
     */
    public @Nullable TdarrFile findByPath(String fullPath) {
        if (!configured) {
            return null;
        }
        try {
            String filename = basename(fullPath);
            if (filename == null || filename.isBlank()) {
                return null;
            }
            String cookie = this.sessionCookie;
            @Var ResponseEntity<TdarrSearchResponse> entity;
            try {
                entity = search(filename, cookie);
            } catch (RestClientResponseException e) {
                if (cookie == null || !isUnauthorized(e.getStatusCode())) {
                    throw e;
                }
                log.info("Tdarr session cookie rejected ({}); re-authenticating with basic auth", e.getStatusCode());
                this.sessionCookie = null;
                entity = search(filename, null);
            }
            captureSessionCookie(entity);
            TdarrSearchResponse body = entity.getBody();
            return body == null ? null : pick(body.files(), fullPath, filename);
        } catch (RuntimeException e) {
            log.warn("Tdarr lookup failed for {}: {}", fullPath, e.getMessage());
            return null;
        }
    }

    /** Issues one search, authenticating with {@code cookie} when present and with basic auth otherwise. */
    private ResponseEntity<TdarrSearchResponse> search(String filename, @Nullable String cookie) {
        log.info("Requesting Tdarr data for {} using {}", filename, cookie != null ? "session cookie" : "basic auth");
        return restClient
                .post()
                .uri("/api/v2/client/search")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .headers(headers -> {
                    if (cookie != null) {
                        headers.add(HttpHeaders.COOKIE, cookie);
                    } else {
                        headers.add(HttpHeaders.AUTHORIZATION, basicAuth);
                    }
                })
                .body(Map.of(
                        "data",
                        Map.of(
                                "start",
                                0,
                                "pageSize",
                                PAGE_SIZE,
                                "filters",
                                List.of(Map.of("id", "file", "value", filename)),
                                "sorts",
                                List.of(),
                                "opts",
                                Map.of())))
                .retrieve()
                .toEntity(TdarrSearchResponse.class);
    }

    /**
     * Remembers the session cookie Tdarr handed back, if any, so the next request can use it instead of credentials.
     */
    private void captureSessionCookie(ResponseEntity<?> entity) {
        String setCookie = entity.getHeaders().getFirst(HttpHeaders.SET_COOKIE);
        if (setCookie != null) {
            this.sessionCookie = setCookie.split(";", 2)[0];
        }
    }

    /**
     * Chooses the record that really describes {@code fullPath}. The filter is a substring match, so a filename can
     * pull in neighbours (a same-named episode of another show, a {@code .mkv} and its sample). Prefer an exact path
     * match, then an exact filename match, then a lone hit; anything still ambiguous records nothing rather than
     * guessing.
     */
    private static @Nullable TdarrFile pick(List<TdarrFile> candidates, String fullPath, String filename) {
        for (TdarrFile candidate : candidates) {
            if (fullPath.equals(candidate.path())) {
                return candidate;
            }
        }
        for (TdarrFile candidate : candidates) {
            if (filename.equals(basename(candidate.path()))) {
                return candidate;
            }
        }
        if (candidates.size() == 1) {
            return candidates.get(0);
        }
        if (!candidates.isEmpty()) {
            log.warn("Tdarr returned {} ambiguous matches for {}; recording none", candidates.size(), filename);
        }
        return null;
    }

    private static boolean isUnauthorized(HttpStatusCode status) {
        return status.value() == HttpStatus.UNAUTHORIZED.value() || status.value() == HttpStatus.FORBIDDEN.value();
    }

    private static @Nullable String basename(@Nullable String path) {
        if (path == null || path.isBlank()) {
            return null;
        }
        Path fileName = Path.of(path).getFileName();
        return fileName == null ? null : fileName.toString();
    }
}
