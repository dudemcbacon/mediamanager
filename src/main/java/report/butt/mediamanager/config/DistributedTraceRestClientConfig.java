package report.butt.mediamanager.config;

import org.jspecify.annotations.NullMarked;
import org.springframework.boot.restclient.RestClientCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Registers a global {@link RestClientCustomizer} that adds {@link DistributedTraceClientHttpRequestInterceptor} to
 * every auto-configured {@code RestClient.Builder} — so all seven integration clients
 * (Ombi/Radarr/Sonarr/Deluge/SABnzbd/Plex/Tdarr) propagate distributed-trace context without each client (or its tests)
 * being touched.
 */
@Configuration
@NullMarked
public class DistributedTraceRestClientConfig {

    @Bean
    RestClientCustomizer distributedTraceRestClientCustomizer() {
        return builder -> builder.requestInterceptor(new DistributedTraceClientHttpRequestInterceptor());
    }
}
