package report.butt.mediamanager.config;

import jakarta.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;
import org.jspecify.annotations.NullMarked;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/**
 * Aborts application startup if any required configuration property is missing or empty, so a misconfigured deployment
 * fails immediately with a clear, aggregated message rather than later with opaque connection errors. Every required
 * key is checked and all blanks are reported at once.
 *
 * <p>Disabled in tests (which don't supply real config) via {@code mediamanager.validate-required-config=false}.
 */
@Component
@ConditionalOnProperty(name = "mediamanager.validate-required-config", matchIfMissing = true)
@NullMarked
public class RequiredConfigValidator {

    static final List<String> REQUIRED_KEYS = List.of(
            "spring.datasource.url",
            "spring.datasource.username",
            "spring.datasource.password",
            "ombi.url",
            "ombi.api-key",
            "radarr.url",
            "radarr.api-key",
            "sonarr.url",
            "sonarr.api-key",
            "deluge.url",
            "deluge.password",
            "sabnzbd.url",
            "sabnzbd.api-key",
            "plex.url",
            "plex.token",
            "plex.movies-section-name",
            "plex.tv-section-name",
            "plex.cache.dir",
            "mediamanager.bootstrap.username",
            "mediamanager.bootstrap.password",
            "spring.mail.host",
            "spring.mail.port",
            "spring.mail.username",
            "spring.mail.password",
            "notifications.enabled",
            "notifications.from",
            "notifications.to",
            "notifications.stuck-download-days",
            "notifications.overdue-request-days",
            "notifications.unsearched-days",
            "notifications.new-request-window-hours",
            "notifications.cron");

    private final Environment environment;

    public RequiredConfigValidator(Environment environment) {
        this.environment = environment;
    }

    @PostConstruct
    void validate() {
        List<String> missing = new ArrayList<>();
        for (String key : REQUIRED_KEYS) {
            String value = environment.getProperty(key);
            if (value == null || value.isBlank()) {
                missing.add(key);
            }
        }
        if (!missing.isEmpty()) {
            throw new IllegalStateException("Missing or empty required configuration properties: "
                    + String.join(", ", missing)
                    + ". Set the corresponding environment variables (see .env) before starting.");
        }
    }
}
