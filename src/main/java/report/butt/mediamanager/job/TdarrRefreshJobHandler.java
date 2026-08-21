package report.butt.mediamanager.job;

import com.newrelic.api.agent.Trace;
import org.jobrunr.jobs.annotations.Job;
import org.jobrunr.jobs.lambdas.JobRequestHandler;
import org.jspecify.annotations.NullMarked;
import org.springframework.stereotype.Component;
import report.butt.mediamanager.service.TdarrRefreshService;

/**
 * Runs a queued {@link TdarrRefreshJobRequest} on a JobRunr worker by delegating to {@link TdarrRefreshService}. How
 * many run at once — and therefore how hard Tdarr is hit by a library-wide sweep — is capped globally by
 * {@code jobrunr.background-job-server.worker-count}.
 */
@Component
@NullMarked
public class TdarrRefreshJobHandler implements JobRequestHandler<TdarrRefreshJobRequest> {

    private final TdarrRefreshService tdarrRefreshService;

    public TdarrRefreshJobHandler(TdarrRefreshService tdarrRefreshService) {
        this.tdarrRefreshService = tdarrRefreshService;
    }

    @Override
    @Job(name = "Tdarr refresh %0", retries = 1)
    @Trace(dispatcher = true)
    public void run(TdarrRefreshJobRequest jobRequest) {
        // The service swallows lookup failures and logs its own misses, so there is nothing to catch here; a single
        // retry covers a transient blip without hammering a Tdarr that is simply down.
        switch (jobRequest.mediaType()) {
            case MOVIE -> tdarrRefreshService.refreshMovie(jobRequest.requestId());
            case EPISODE -> tdarrRefreshService.refreshEpisode(jobRequest.requestId());
        }
    }
}
