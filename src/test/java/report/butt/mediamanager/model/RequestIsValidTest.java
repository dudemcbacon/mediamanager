package report.butt.mediamanager.model;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * Tests Request.isValid(validatorNames, latestByName) — the concrete subclass MovieRequest is used since Request is
 * abstract.
 */
class RequestIsValidTest {

    private static MovieRequest req() {
        return new MovieRequest("T", 1, false, 1, "S");
    }

    private static Validation passing(MovieRequest r, String name) {
        return new Validation(name, true, r);
    }

    private static Validation failing(MovieRequest r, String name) {
        return new Validation(name, false, r);
    }

    @Test
    void allValidatorsPass_returnsTrue() {
        MovieRequest r = req();
        Validation v1 = passing(r, "PathsMatch");
        Validation v2 = passing(r, "RadarrHasFile");
        Map<String, Validation> map = Map.of("PathsMatch", v1, "RadarrHasFile", v2);

        assertTrue(r.isValid(List.of("PathsMatch", "RadarrHasFile"), map));
    }

    @Test
    void emptyValidatorNames_returnsTrue() {
        MovieRequest r = req();
        assertTrue(r.isValid(List.of(), Map.of()));
    }

    @Test
    void missingValidatorInMap_returnsFalse() {
        MovieRequest r = req();
        Validation v1 = passing(r, "PathsMatch");
        Map<String, Validation> map = Map.of("PathsMatch", v1);

        assertFalse(r.isValid(List.of("PathsMatch", "MissingValidator"), map));
    }

    @Test
    void oneValidatorFails_returnsFalse() {
        MovieRequest r = req();
        Validation v1 = passing(r, "PathsMatch");
        Validation v2 = failing(r, "RadarrHasFile");
        Map<String, Validation> map = Map.of("PathsMatch", v1, "RadarrHasFile", v2);

        assertFalse(r.isValid(List.of("PathsMatch", "RadarrHasFile"), map));
    }

    @Test
    void validatorWithNullResult_returnsFalse() {
        MovieRequest r = req();
        Validation v = new Validation("PathsMatch", null, r);
        Map<String, Validation> map = Map.of("PathsMatch", v);

        assertFalse(r.isValid(List.of("PathsMatch"), map));
    }
}
