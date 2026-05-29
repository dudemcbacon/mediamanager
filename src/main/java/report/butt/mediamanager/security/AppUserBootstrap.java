package report.butt.mediamanager.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class AppUserBootstrap implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(AppUserBootstrap.class);

    private final AppUserRepository repository;
    private final PasswordEncoder passwordEncoder;
    private final String bootstrapUsername;
    private final String bootstrapPassword;

    public AppUserBootstrap(
            AppUserRepository repository,
            PasswordEncoder passwordEncoder,
            @Value("${mediamanager.bootstrap.username:}") String bootstrapUsername,
            @Value("${mediamanager.bootstrap.password:}") String bootstrapPassword) {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
        this.bootstrapUsername = bootstrapUsername;
        this.bootstrapPassword = bootstrapPassword;
    }

    @Override
    public void run(String... args) {
        if (repository.count() > 0) {
            return;
        }
        if (bootstrapUsername == null || bootstrapUsername.isBlank()) {
            throw new IllegalStateException(
                    "MEDIAMANAGER_BOOTSTRAP_USERNAME must be set to bootstrap the initial admin user");
        }
        if (bootstrapPassword == null || bootstrapPassword.isBlank()) {
            throw new IllegalStateException(
                    "MEDIAMANAGER_BOOTSTRAP_PASSWORD must be set to bootstrap the initial admin user");
        }
        if (bootstrapPassword.equals(bootstrapUsername)) {
            throw new IllegalStateException(
                    "MEDIAMANAGER_BOOTSTRAP_PASSWORD must not equal MEDIAMANAGER_BOOTSTRAP_USERNAME");
        }
        AppUser admin = new AppUser(bootstrapUsername, passwordEncoder.encode(bootstrapPassword), "ADMIN");
        repository.save(admin);
        log.warn(
                "Bootstrapped initial admin user '{}'. Change the password immediately via MEDIAMANAGER_BOOTSTRAP_PASSWORD or by updating the app_user table.",
                bootstrapUsername);
    }
}
