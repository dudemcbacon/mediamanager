package report.butt.mediamanager.validation;

import java.time.Duration;
import java.time.Instant;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/** Small logic helpers shared by the Movie/TV/episode variants of otherwise independent rules. */
@NullMarked
final class ValidationSupport {

    private static final String MNT_PREFIX = "/mnt";
    private static final Duration ONE_WEEK = Duration.ofDays(7);

    private ValidationSupport() {}

    /** Strips a leading {@code /mnt} mount prefix so Plex and Radarr/Sonarr paths compare equal. */
    static @Nullable String stripMnt(@Nullable String path) {
        if (path == null) {
            return null;
        }
        return path.startsWith(MNT_PREFIX) ? path.substring(MNT_PREFIX.length()) : path;
    }

    /** True when {@code lastSearched} is non-null and falls within the last week. */
    static boolean searchedWithinLastWeek(@Nullable Instant lastSearched) {
        return lastSearched != null && lastSearched.isAfter(Instant.now().minus(ONE_WEEK));
    }
}
