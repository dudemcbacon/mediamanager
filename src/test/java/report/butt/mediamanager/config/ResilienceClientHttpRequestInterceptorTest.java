package report.butt.mediamanager.config;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;
import java.io.IOException;
import java.net.URI;
import java.time.Duration;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpResponse;

@NullMarked
class ResilienceClientHttpRequestInterceptorTest {

    private static final byte[] BODY = new byte[0];

    private static HttpRequest request(HttpMethod method) {
        HttpRequest request = mock(HttpRequest.class);
        when(request.getURI()).thenReturn(URI.create("http://radarr/api/v3/movie"));
        when(request.getMethod()).thenReturn(method);
        return request;
    }

    private static ResilienceClientHttpRequestInterceptor interceptor(int maxAttempts, int minimumCalls) {
        var cbRegistry = CircuitBreakerRegistry.of(CircuitBreakerConfig.custom()
                .slidingWindowSize(2)
                .minimumNumberOfCalls(minimumCalls)
                .failureRateThreshold(50)
                .waitDurationInOpenState(Duration.ofSeconds(60))
                .recordException(e -> e instanceof IOException)
                .build());
        var retryRegistry = RetryRegistry.of(RetryConfig.custom()
                .maxAttempts(maxAttempts)
                .waitDuration(Duration.ofMillis(1))
                .retryOnException(e -> e instanceof IOException)
                .build());
        return new ResilienceClientHttpRequestInterceptor(cbRegistry, retryRegistry);
    }

    @Test
    void passesSuccessfulResponseThroughWithoutRetry() throws Exception {
        var response = mock(ClientHttpResponse.class);
        var execution = mock(ClientHttpRequestExecution.class);
        var request = request(HttpMethod.GET);
        when(execution.execute(eq(request), any())).thenReturn(response);

        ClientHttpResponse result = interceptor(2, 100).intercept(request, BODY, execution);

        assertSame(response, result);
        verify(execution, times(1)).execute(eq(request), any());
    }

    @Test
    void retriesFailedGetUpToMaxAttempts() throws Exception {
        var execution = mock(ClientHttpRequestExecution.class);
        var request = request(HttpMethod.GET);
        when(execution.execute(eq(request), any())).thenThrow(new IOException("connection refused"));

        // minimumCalls high so the breaker stays closed and we observe pure retry behaviour.
        assertThrows(IOException.class, () -> interceptor(2, 100).intercept(request, BODY, execution));
        verify(execution, times(2)).execute(eq(request), any());
    }

    @Test
    void doesNotRetryNonIdempotentPost() throws Exception {
        var execution = mock(ClientHttpRequestExecution.class);
        var request = request(HttpMethod.POST);
        when(execution.execute(eq(request), any())).thenThrow(new IOException("connection refused"));

        assertThrows(IOException.class, () -> interceptor(2, 100).intercept(request, BODY, execution));
        verify(execution, times(1)).execute(eq(request), any());
    }

    @Test
    void opensBreakerAndFailsFastAfterRepeatedFailures() throws Exception {
        var execution = mock(ClientHttpRequestExecution.class);
        var request = request(HttpMethod.GET);
        when(execution.execute(eq(request), any())).thenThrow(new IOException("connection refused"));

        // No retry (maxAttempts=1); breaker opens once 2 calls have been seen at a 100% failure rate.
        var interceptor = interceptor(1, 2);
        assertThrows(IOException.class, () -> interceptor.intercept(request, BODY, execution));
        assertThrows(IOException.class, () -> interceptor.intercept(request, BODY, execution));

        // Third call is rejected by the open breaker without touching the downstream execution.
        assertThrows(CallNotPermittedException.class, () -> interceptor.intercept(request, BODY, execution));
        verify(execution, times(2)).execute(eq(request), any());
    }
}
