package report.butt.mediamanager.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
        MockEnvironment environment = new MockEnvironment();
        RequiredConfigValidator.REQUIRED_KEYS.forEach(key -> environment.setProperty(key, "value"));
        return environment;
    }

    @Test
    void passesWhenEveryRequiredKeyIsPresent() {
        RequiredConfigValidator validator = new RequiredConfigValidator(environmentWithAllKeys());
        assertDoesNotThrow(validator::validate);
    }

    @Test
    void failsWhenAKeyIsBlank() {
        MockEnvironment environment = environmentWithAllKeys();
        environment.setProperty("ombi.url", "   ");
        RequiredConfigValidator validator = new RequiredConfigValidator(environment);

        IllegalStateException ex = assertThrows(IllegalStateException.class, validator::validate);
        assertTrue(ex.getMessage().contains("ombi.url"), ex.getMessage());
    }

    @Test
    void failsWhenAKeyIsMissingEntirely() {
        MockEnvironment environment = new MockEnvironment();
        RequiredConfigValidator.REQUIRED_KEYS.stream()
                .filter(key -> !"plex.token".equals(key))
                .forEach(key -> environment.setProperty(key, "value"));
        RequiredConfigValidator validator = new RequiredConfigValidator(environment);

        IllegalStateException ex = assertThrows(IllegalStateException.class, validator::validate);
        assertTrue(ex.getMessage().contains("plex.token"), ex.getMessage());
    }

    @Test
    void reportsAllMissingKeysAtOnce() {
        MockEnvironment environment = environmentWithAllKeys();
        environment.setProperty("spring.mail.host", "");
        environment.setProperty("notifications.to", "");
        RequiredConfigValidator validator = new RequiredConfigValidator(environment);

        IllegalStateException ex = assertThrows(IllegalStateException.class, validator::validate);
        assertTrue(ex.getMessage().contains("spring.mail.host"), ex.getMessage());
        assertTrue(ex.getMessage().contains("notifications.to"), ex.getMessage());
    }
}
