package report.butt.mediamanager.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Checks whether a media file reported by Radarr/Sonarr exists on the local filesystem. The reported path is prepended
 * with {@code mediamanager.local-file-system-prefix} (empty by default) so it can be remapped when the media volume is
 * mounted at a different root than the *arr app sees.
 */
public final class LocalFileInspector {

    private static final Logger log = LoggerFactory.getLogger(LocalFileInspector.class);

    private LocalFileInspector() {}

    /** Whether the reported file resolves to an existing local file and, if so, its size in bytes (else null). */
    public record Result(boolean available, @Nullable Long sizeBytes) {
        static final Result UNAVAILABLE = new Result(false, null);
    }

    /**
     * Resolves {@code prefix + reportedPath} and reports its local availability and size. A null/blank path, a
     * non-existent file, or any I/O / invalid-path error all yield an unavailable result with no size.
     */
    public static Result inspect(String prefix, String reportedPath) {
        if (reportedPath == null || reportedPath.isBlank()) {
            return Result.UNAVAILABLE;
        }
        try {
            var localPath = Path.of(prefix + reportedPath);
            if (Files.exists(localPath)) {
                return new Result(true, Files.size(localPath));
            }
        } catch (InvalidPathException | IOException e) {
            log.warn("Local file check failed for {}{}", prefix, reportedPath, e);
        }
        return Result.UNAVAILABLE;
    }
}
