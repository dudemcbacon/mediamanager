package report.butt.mediamanager.config;

import com.newrelic.api.agent.HttpParameters;
import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Segment;
import java.io.IOException;
import java.net.URI;
import org.jspecify.annotations.NullMarked;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Brackets every outbound RestClient call (Ombi/Radarr/Sonarr/Deluge/SABnzbd/Plex/Tdarr) in a New Relic {@link Segment}
 * and writes the distributed-trace headers from that segment, so an instrumented downstream service reports its span as
 * a child of this call.
 *
 * <p>The segment — not the enclosing transaction — has to mint the headers: the header carries the guid of whichever
 * span writes it, and the downstream span is parented to that guid. Minting from the transaction (via
 * {@code Transaction#insertDistributedTraceHeaders}) makes the downstream service a sibling of the HTTP call rather
 * than its child. The agent's own JDK-HttpClient span can't do the job either — {@code RestClient} goes through
 * {@code sendAsync}, so that span is created on another thread after the headers are already on the wire.
 *
 * <p>An interceptor is the right home because it wraps {@code execution.execute}: the segment spans exactly the HTTP
 * call, and request headers are still mutable at this point.
 *
 * <p>Ordering against {@link ResilienceClientHttpRequestInterceptor} is deliberately unconstrained. Whichever runs
 * outermost, the trace is correct; the only difference is whether retried attempts share one external span or get one
 * each.
 *
 * <p>Calls made off a New Relic transaction (e.g. on a {@code ui-task-executor} thread with no linked token) get a
 * no-op segment and no headers, exactly as before this interceptor existed.
 */
@NullMarked
class DistributedTraceClientHttpRequestInterceptor implements ClientHttpRequestInterceptor {

    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution)
            throws IOException {
        var uri = withoutCredentials(request.getURI());
        var host = uri.getHost() == null ? "unknown" : uri.getHost();
        var method = request.getMethod().name();

        var segment = NewRelic.getAgent().getTransaction().startSegment("External", method + " " + host);
        try {
            // Reported before the call so the external attributes survive a connection failure.
            segment.reportAsExternal(HttpParameters
                    .library("RestClient")
                    .uri(uri)
                    .procedure(method)
                    .noInboundHeaders()
                    .build());
            segment.addOutboundRequestHeaders(new HttpHeadersAdapter(request.getHeaders()));
            return execution.execute(request, body);
        } finally {
            segment.end();
        }
    }

    /**
     * Strips the query string and user info from a request URI before it is reported to New Relic. SABnzbd passes its
     * {@code apikey} and Plex its {@code X-Plex-Token} as query parameters, and the reported URI becomes a span
     * attribute — so the query has to go, or those credentials are shipped off-box.
     */
    static URI withoutCredentials(URI uri) {
        return UriComponentsBuilder.fromUri(uri)
                .replaceQuery(null)
                .userInfo(null)
                .build()
                .toUri();
    }
}
