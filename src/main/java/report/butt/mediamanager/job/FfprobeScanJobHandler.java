package report.butt.mediamanager.job;

import com.newrelic.api.agent.Trace;
import org.jobrunr.jobs.annotations.Job;
import org.jobrunr.jobs.lambdas.JobRequestHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import report.butt.mediamanager.service.FfprobeScanService;

/**
 * Runs a queued {@link FfprobeScanJobRequest} on a JobRunr worker thread by delegating to the existing
 * {@link FfprobeScanService} — the same scan logic the synchronous "Scan with FFprobe" action used before. JobRunr
 * instantiates this via Spring, so the service is injected normally. How many of these run at once is capped globally
 * by {@code jobrunr.background-job-server.worker-count}.
 */
@Component
public class FfprobeScanJobHandler implements JobRequestHandler<FfprobeScanJobRequest> {

    private static final Logger log = LoggerFactory.getLogger(FfprobeScanJobHandler.class);

    private final FfprobeScanService ffprobeScanService;

    public FfprobeScanJobHandler(FfprobeScanService ffprobeScanService) {
        this.ffprobeScanService = ffprobeScanService;
    }

    @Override
    @Job(name = "FFprobe scan %0", retries = 2)
    @Trace(dispatcher = true)
    public void run(FfprobeScanJobRequest jobRequest) {
        var requestId = jobRequest.requestId();
        log.info("Running queued FFprobe scan for {} {}", jobRequest.mediaType(), requestId);
        switch (jobRequest.mediaType()) {
            case MOVIE -> ffprobeScanService.scanMovie(requestId);
            case EPISODE -> ffprobeScanService.scanEpisode(requestId);
        }
    }
}
