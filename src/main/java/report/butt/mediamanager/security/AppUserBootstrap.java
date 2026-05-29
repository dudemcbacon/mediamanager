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
            @Value("${mediamanager.bootstrap.username:admin}") String bootstrapUsername,
            @Value("${mediamanager.bootstrap.password:admin}") String bootstrapPassword) {
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
        AppUser admin = new AppUser(bootstrapUsername, passwordEncoder.encode(bootstrapPassword), "ADMIN");
        repository.save(admin);
        log.warn(
                "Bootstrapped initial admin user '{}'. Change the password immediately via MEDIAMANAGER_BOOTSTRAP_PASSWORD or by updating the app_user table.",
                bootstrapUsername);
    }
}
