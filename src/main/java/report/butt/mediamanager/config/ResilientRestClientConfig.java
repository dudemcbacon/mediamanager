package report.butt.mediamanager.config;

import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;
import java.io.IOException;
import java.time.Duration;
import org.jspecify.annotations.NullMarked;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.restclient.RestClientCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Registers a global {@link RestClientCustomizer} that adds {@link ResilienceClientHttpRequestInterceptor} to every
 * auto-configured {@code RestClient.Builder} — so all six integration clients (Ombi/Radarr/Sonarr/Deluge/SABnzbd/Plex)
 * gain circuit breaking and bounded retry without each client (or its tests) being touched. Thresholds are tunable via
 * {@code mediamanager.resilience.*}; the defaults assume the low call volume of a personal media manager.
 */
@Configuration
@NullMarked
public class ResilientRestClientConfig {

    @Bean
    CircuitBreakerRegistry circuitBreakerRegistry(
            @Value("${mediamanager.resilience.failure-rate-threshold:50}") float failureRateThreshold,
            @Value("${mediamanager.resilience.sliding-window-size:10}") int slidingWindowSize,
            @Value("${mediamanager.resilience.minimum-calls:5}") int minimumNumberOfCalls,
            @Value("${mediamanager.resilience.open-state-seconds:30}") long openStateSeconds) {
        var config = CircuitBreakerConfig.custom()
                .failureRateThreshold(failureRateThreshold)
                .slidingWindowSize(slidingWindowSize)
                .minimumNumberOfCalls(minimumNumberOfCalls)
                .waitDurationInOpenState(Duration.ofSeconds(openStateSeconds))
                .permittedNumberOfCallsInHalfOpenState(3)
                .automaticTransitionFromOpenToHalfOpenEnabled(true)
                // Only connection-level failures (connection refused, connect/read timeout) reach the interceptor as
                // exceptions; HTTP 4xx/5xx come back as a response and are handled by the client's .retrieve().
                .recordException(e -> e instanceof IOException)
                .build();
        return CircuitBreakerRegistry.of(config);
    }

    @Bean
    RetryRegistry retryRegistry(
            @Value("${mediamanager.resilience.max-retry-attempts:2}") int maxAttempts,
            @Value("${mediamanager.resilience.retry-backoff-millis:500}") long backoffMillis) {
        var config = RetryConfig.custom()
                .maxAttempts(maxAttempts)
                .waitDuration(Duration.ofMillis(backoffMillis))
                // Retry only transient connection failures; a CallNotPermittedException (breaker open) is not an
                // IOException, so it fails fast instead of being retried.
                .retryOnException(e -> e instanceof IOException)
                .build();
        return RetryRegistry.of(config);
    }

    @Bean
    RestClientCustomizer resilientRestClientCustomizer(
            CircuitBreakerRegistry circuitBreakerRegistry, RetryRegistry retryRegistry) {
        return builder -> builder.requestInterceptor(
                new ResilienceClientHttpRequestInterceptor(circuitBreakerRegistry, retryRegistry));
    }
}
