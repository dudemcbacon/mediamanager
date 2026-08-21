package report.butt.mediamanager.job;

import org.jobrunr.jobs.lambdas.JobRequest;
import org.jspecify.annotations.NullMarked;

/**
 * A queued request to read one file's Tdarr status. One job per file rather than one job for the whole sweep: JobRunr's
 * {@code worker-count} then bounds how many Tdarr calls run at once, progress is visible in its dashboard, and one
 * unreachable file can't abort the rest of the library.
 *
 * <p>{@code mediaType} selects which {@code TdarrRefreshService} entry point runs; {@code requestId} is the
 * {@code MovieRequest} or {@code TvEpisodeRequest} id.
 */
@NullMarked
public record TdarrRefreshJobRequest(MediaType mediaType, Long requestId) implements JobRequest {

    /** Which kind of request {@link #requestId} refers to. */
    public enum MediaType {
        MOVIE,
        EPISODE
    }

    @Override
    public Class<TdarrRefreshJobHandler> getJobRequestHandler() {
        return TdarrRefreshJobHandler.class;
    }
}
