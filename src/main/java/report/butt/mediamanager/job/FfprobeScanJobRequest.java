package report.butt.mediamanager.job;

import org.jobrunr.jobs.lambdas.JobRequest;
import org.jspecify.annotations.NullMarked;

/**
 * A queued request to ffprobe-scan one media file. JobRunr serializes this to JSON, stores it in Postgres, and later
 * hands it to {@link FfprobeScanJobHandler} on a background worker. {@code mediaType} selects which
 * {@code FfprobeScanService} entry point runs; {@code requestId} is the {@code MovieRequest} or
 * {@code TvEpisodeRequest} id.
 */
@NullMarked
public record FfprobeScanJobRequest(MediaType mediaType, Long requestId) implements JobRequest {

    /** Which kind of request {@link #requestId} refers to. */
    public enum MediaType {
        MOVIE,
        EPISODE
    }

    @Override
    public Class<FfprobeScanJobHandler> getJobRequestHandler() {
        return FfprobeScanJobHandler.class;
    }
}
