package report.butt.mediamanager.security;

import com.vaadin.flow.spring.security.VaadinSecurityConfigurer;
import java.time.Duration;
import java.util.List;
import org.jspecify.annotations.NullMarked;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import report.butt.mediamanager.controller.AdminApiController;
import report.butt.mediamanager.controller.TdarrWebhookController;
import report.butt.mediamanager.route.LoginView;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@NullMarked
public class SecurityConfig {

    /** Path prefix the CORS policy applies to; the Tdarr webhook lives under it. */
    static final String API_PATHS = "/api/**";

    /**
     * Origins allowed to call {@link #API_PATHS} cross-origin. Wildcard subdomains require
     * {@code setAllowedOriginPatterns} — plain {@code setAllowedOrigins} only does exact matches. The apex is listed
     * separately because {@code *.butt.report} does not match {@code butt.report} itself.
     */
    // Immutable via List.of; there is no immutable-typed collection library on the classpath.
    @SuppressWarnings("ImmutableMemberCollection")
    static final List<String> ALLOWED_ORIGIN_PATTERNS = List.of("https://*.butt.report", "https://butt.report");

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.authorizeHttpRequests(auth -> auth.requestMatchers("/*.css", "/*.js", "/*.ico", "/*.png", "/*.svg")
                .permitAll()
                .requestMatchers("/actuator/health/**")
                .permitAll()
                .requestMatchers("/actuator/**")
                .hasRole("ADMIN")
                .requestMatchers("/plex-cache/**")
                .authenticated()
                // Tdarr is a machine caller with no login, so this endpoint authenticates itself with the
                // X-Tdarr-Token header inside TdarrWebhookController rather than with a session. It is permitted
                // here only so the request reaches that check — the controller rejects a missing or wrong token,
                // and also rejects everything if no token is configured.
                .requestMatchers(TdarrWebhookController.PATH)
                .permitAll()
                // Same arrangement as the Tdarr webhook: a curl/agent caller with no login, authenticating itself with
                // the X-Admin-Token header checked inside AdminApiController. Permitted here only so the request
                // reaches that check.
                .requestMatchers(AdminApiController.SEEDLESS_SWEEP_PATH)
                .permitAll());
        // Without this these POSTs are refused as a CSRF failure: an external caller has no CSRF token, and both
        // endpoints are guarded by a shared secret instead of a session, so there is no cookie for CSRF to protect.
        http.csrf(csrf -> csrf.ignoringRequestMatchers(
                TdarrWebhookController.PATH, AdminApiController.SEEDLESS_SWEEP_PATH));
        // Registered on the security chain (not just Spring MVC) so its CorsFilter runs ahead of authorization and
        // answers the preflight OPTIONS itself — an unauthenticated preflight would otherwise be rejected.
        http.cors(cors -> cors.configurationSource(corsConfigurationSource()));
        http.with(VaadinSecurityConfigurer.vaadin(), configurer -> configurer.loginView(LoginView.class));
        return http.build();
    }

    /**
     * Cross-origin policy for the API paths only. The Vaadin views, actuator and plex-cache are deliberately excluded:
     * Vaadin's own traffic is same-origin and gains nothing from CORS, and a narrow rule keeps a compromised
     * {@code butt.report} subdomain from scripting the admin endpoints.
     *
     * <p>Credentials are not allowed, so a browser will not attach the mediamanager session cookie to a cross-origin
     * call. That suits the Tdarr webhook, which authenticates with its own {@code X-Tdarr-Token} header rather than a
     * session, and means a subdomain cannot act as the signed-in admin.
     */
    @Bean
    CorsConfigurationSource corsConfigurationSource() {
        var configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(ALLOWED_ORIGIN_PATTERNS);
        configuration.setAllowedMethods(List.of("GET", "POST", "OPTIONS"));
        // Safe to accept any request header because credentials are disallowed; this covers X-Tdarr-Token and
        // Content-Type without the policy needing an edit each time a header is added.
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(false);
        configuration.setMaxAge(Duration.ofHours(1));

        var source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration(API_PATHS, configuration);
        return source;
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
