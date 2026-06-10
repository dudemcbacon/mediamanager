package report.butt.mediamanager.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.time.Instant;
import org.junit.jupiter.api.Test;

class DateTimeUtilsTest {

    @Test
    void parseInstant_validIsoString_returnsInstant() {
        Instant result = DateTimeUtils.parseInstant("2024-03-15T10:30:00Z", "TestSource");
        assertEquals(Instant.parse("2024-03-15T10:30:00Z"), result);
    }

    @Test
    void parseInstant_nullInput_returnsNull() {
        assertNull(DateTimeUtils.parseInstant(null, "TestSource"));
    }

    @Test
    void parseInstant_blankInput_returnsNull() {
        assertNull(DateTimeUtils.parseInstant("   ", "TestSource"));
    }

    @Test
    void parseInstant_emptyString_returnsNull() {
        assertNull(DateTimeUtils.parseInstant("", "TestSource"));
    }

    @Test
    void parseInstant_invalidFormat_returnsNull() {
        assertNull(DateTimeUtils.parseInstant("not-a-date", "Radarr"));
    }

    @Test
    void parseInstant_partialDate_returnsNull() {
        assertNull(DateTimeUtils.parseInstant("2024-03-15", "Sonarr"));
    }
}
