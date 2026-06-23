package report.butt.mediamanager.util;

import java.time.Instant;
import java.time.format.DateTimeParseException;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@NullMarked
public final class DateTimeUtils {

    private static final Logger log = LoggerFactory.getLogger(DateTimeUtils.class);

    private DateTimeUtils() {}

    /**
     * Parses an ISO-8601 instant, returning null for null/blank input or unparseable values. The {@code source} label
     * (e.g. "Radarr", "Sonarr") tags the warning logged on a parse failure.
     */
    public static @Nullable Instant parseInstant(@Nullable String value, String source) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Instant.parse(value);
        } catch (DateTimeParseException e) {
            log.warn("Could not parse {} timestamp '{}'", source, value);
            return null;
        }
    }
}
