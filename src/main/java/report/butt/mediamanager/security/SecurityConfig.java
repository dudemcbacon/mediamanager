package report.butt.mediamanager.security;

import com.vaadin.flow.spring.security.VaadinSecurityConfigurer;
import org.jspecify.annotations.NullMarked;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import report.butt.mediamanager.controller.TdarrWebhookController;
import report.butt.mediamanager.route.LoginView;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@NullMarked
public class SecurityConfig {

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
                .permitAll());
        // Without this the webhook POST is refused as a CSRF failure: an external caller has no CSRF token, and the
        // endpoint is guarded by a shared secret instead of a session, so there is no cookie for CSRF to protect.
        http.csrf(csrf -> csrf.ignoringRequestMatchers(TdarrWebhookController.PATH));
        http.with(VaadinSecurityConfigurer.vaadin(), configurer -> configurer.loginView(LoginView.class));
        return http.build();
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
