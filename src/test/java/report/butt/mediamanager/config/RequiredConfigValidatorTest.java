package report.butt.mediamanager.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Objects;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.mock.env.MockEnvironment;

class RequiredConfigValidatorTest {

    private final ApplicationContextRunner contextRunner =
            new ApplicationContextRunner().withUserConfiguration(RequiredConfigValidator.class);

    @Test
    void contextStartupAbortsWhenRequiredConfigMissing() {
        // No properties supplied: the validator is active (matchIfMissing) and every key is blank.
        contextRunner.run(context -> assertThat(context).hasFailed());
    }

    @Test
    void contextStartsWhenValidationDisabled() {
        contextRunner
                .withPropertyValues("mediamanager.validate-required-config=false")
                .run(context -> assertThat(context).hasNotFailed());
    }

    private static MockEnvironment environmentWithAllKeys() {
        var environment = new MockEnvironment();
        RequiredConfigValidator.REQUIRED_KEYS.forEach(key -> environment.setProperty(key, "value"));
        return environment;
    }

    @Test
    void passesWhenEveryRequiredKeyIsPresent() {
        var validator = new RequiredConfigValidator(environmentWithAllKeys());
        assertDoesNotThrow(validator::validate);
    }

    @Test
    void failsWhenAKeyIsBlank() {
        MockEnvironment environment = environmentWithAllKeys();
        environment.setProperty("ombi.url", "   ");
        var validator = new RequiredConfigValidator(environment);

        IllegalStateException ex = assertThrows(IllegalStateException.class, validator::validate);
        assertTrue(ex.getMessage().contains("ombi.url"), ex.getMessage());
    }

    @Test
    void failsWhenAKeyIsMissingEntirely() {
        var environment = new MockEnvironment();
        RequiredConfigValidator.REQUIRED_KEYS.stream()
                .filter(key -> !Objects.equals(key, "plex.token"))
                .forEach(key -> environment.setProperty(key, "value"));
        var validator = new RequiredConfigValidator(environment);

        IllegalStateException ex = assertThrows(IllegalStateException.class, validator::validate);
        assertTrue(ex.getMessage().contains("plex.token"), ex.getMessage());
    }

    @Test
    void reportsAllMissingKeysAtOnce() {
        MockEnvironment environment = environmentWithAllKeys();
        environment.setProperty("spring.mail.host", "");
        environment.setProperty("notifications.to", "");
        var validator = new RequiredConfigValidator(environment);

        IllegalStateException ex = assertThrows(IllegalStateException.class, validator::validate);
        assertTrue(ex.getMessage().contains("spring.mail.host"), ex.getMessage());
        assertTrue(ex.getMessage().contains("notifications.to"), ex.getMessage());
    }
}
