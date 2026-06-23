package report.butt.mediamanager.validation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;

/** Unit tests for ValidationSupport static helpers. Same package so package-private access is permitted. */
@NullMarked
class ValidationSupportTest {

    // --- stripMnt ---

    @Test
    void stripMnt_returnsNullWhenInputIsNull() {
        assertNull(ValidationSupport.stripMnt(null));
    }

    @Test
    void stripMnt_stripsLeadingMntPrefix() {
        assertEquals("/movies/film.mkv", ValidationSupport.stripMnt("/mnt/movies/film.mkv"));
    }

    @Test
    void stripMnt_leavesPathWithoutMntUnchanged() {
        assertEquals("/movies/film.mkv", ValidationSupport.stripMnt("/movies/film.mkv"));
    }

    @Test
    void stripMnt_leavesEmptyStringUnchanged() {
        assertEquals("", ValidationSupport.stripMnt(""));
    }

    @Test
    void stripMnt_doesNotStripMntInMiddleOfPath() {
        // "/data/mnt/movies" does not start with "/mnt" so should be unchanged
        assertEquals("/data/mnt/movies", ValidationSupport.stripMnt("/data/mnt/movies"));
    }

    // --- searchedWithinLastWeek ---

    @Test
    void searchedWithinLastWeek_returnsFalseWhenNull() {
        assertFalse(ValidationSupport.searchedWithinLastWeek(null));
    }

    @Test
    void searchedWithinLastWeek_returnsTrueWhenSearchedYesterday() {
        assertTrue(ValidationSupport.searchedWithinLastWeek(Instant.now().minus(1, ChronoUnit.DAYS)));
    }

    @Test
    void searchedWithinLastWeek_returnsTrueWhenSearchedJustUnderOneWeekAgo() {
        assertTrue(ValidationSupport.searchedWithinLastWeek(Instant.now().minus(6, ChronoUnit.DAYS)));
    }

    @Test
    void searchedWithinLastWeek_returnsFalseWhenSearchedMoreThanWeekAgo() {
        assertFalse(ValidationSupport.searchedWithinLastWeek(Instant.now().minus(8, ChronoUnit.DAYS)));
    }

    @Test
    void searchedWithinLastWeek_returnsFalseWhenSearchedExactlySevenDaysAgo() {
        // Exactly 7 days minus 1 ms → still within window; 7 days exactly → at the boundary,
        // "isAfter" is strict so 7-day-old instant is NOT after (now - 7 days).
        Instant sevenDaysAgo = Instant.now().minus(7, ChronoUnit.DAYS);
        // sevenDaysAgo.isAfter(sevenDaysAgo) == false
        assertFalse(ValidationSupport.searchedWithinLastWeek(sevenDaysAgo));
    }
}
