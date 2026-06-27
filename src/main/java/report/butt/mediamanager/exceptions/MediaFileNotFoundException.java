package report.butt.mediamanager.exceptions;

import org.jspecify.annotations.NullMarked;

/**
 * Thrown when a request's resolved local media file is not present on disk, so there is nothing for ffprobe to scan.
 * This is a permanent condition (a missing file won't appear by retrying), so callers should treat it as a skip rather
 * than a transient failure.
 */
@NullMarked
public class MediaFileNotFoundException extends RuntimeException {

    public MediaFileNotFoundException(String requestType, Long requestId, String path) {
        super("No local media file for " + requestType + " " + requestId + " at " + path);
    }
}
