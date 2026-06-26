package report.butt.mediamanager.config;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryRegistry;
import java.io.IOException;
import org.jspecify.annotations.NullMarked;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;

/**
 * Wraps every outbound RestClient call (Ombi/Radarr/Sonarr/Deluge/SABnzbd/Plex) in a per-host circuit breaker, with a
 * bounded retry on idempotent GETs. The breaker trips on connection-level failures — connection refused, connect/read
 * timeout — which are exactly the calls that otherwise hang a ui-task-executor thread for the full read timeout (see
 * {@link AsyncExecutorConfig}); once open it fails fast for the configured wait duration instead of piling more stalled
 * calls onto a downed service. HTTP error statuses (4xx/5xx) surface from RestClient's {@code .retrieve()} after this
 * interceptor returns the response, so they don't count against the breaker.
 *
 * <p>One breaker (and retry) is keyed per request host, so a Plex outage opens Plex's breaker without affecting Radarr.
 */
@NullMarked
class ResilienceClientHttpRequestInterceptor implements ClientHttpRequestInterceptor {

    private final CircuitBreakerRegistry circuitBreakerRegistry;
    private final RetryRegistry retryRegistry;

    ResilienceClientHttpRequestInterceptor(CircuitBreakerRegistry circuitBreakerRegistry, RetryRegistry retryRegistry) {
        this.circuitBreakerRegistry = circuitBreakerRegistry;
        this.retryRegistry = retryRegistry;
    }

    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution)
            throws IOException {
        String host = request.getURI().getHost();
        var name = host == null ? "unknown" : host;
        var circuitBreaker = circuitBreakerRegistry.circuitBreaker(name);

        var guarded = CircuitBreaker.decorateCheckedSupplier(circuitBreaker, () -> execution.execute(request, body));
        // Only retry idempotent GETs: replaying a POST could double-trigger a Radarr/Sonarr search or an Ombi action.
        var call = request.getMethod().equals(HttpMethod.GET)
                ? Retry.decorateCheckedSupplier(retryRegistry.retry(name), guarded)
                : guarded;

        try {
            return call.get();
        } catch (IOException | RuntimeException e) {
            // Connection failures (recorded by the breaker) and CallNotPermittedException (breaker open) propagate
            // as-is
            // so RestClient surfaces them to the caller just as it did before this interceptor existed.
            throw e;
        } catch (Throwable t) {
            // CheckedSupplier#get is declared to throw Throwable; execution.execute only throws IOException, so
            // anything
            // else here is unexpected — preserve it rather than swallow.
            throw new IllegalStateException("Unexpected error executing HTTP request to " + name, t);
        }
    }
}
