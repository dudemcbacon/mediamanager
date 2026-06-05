package report.butt.mediamanager.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class ScheduledRefreshJob {

    private static final Logger log = LoggerFactory.getLogger(ScheduledRefreshJob.class);

    private final MovieRefreshService movieRefreshService;
    private final TvRefreshService tvRefreshService;
    private final ValidatorService validatorService;

    public ScheduledRefreshJob(
            MovieRefreshService movieRefreshService,
            TvRefreshService tvRefreshService,
            ValidatorService validatorService) {
        this.movieRefreshService = movieRefreshService;
        this.tvRefreshService = tvRefreshService;
        this.validatorService = validatorService;
    }

    @Scheduled(cron = "0 0 * * * *")
    public void refreshAndValidate() {
        log.info("Hourly refresh-and-validate job starting");
        movieRefreshService.refreshAll();
        tvRefreshService.refreshAll();
        validatorService.validateAllMovies();
        validatorService.validateAllTv();
        log.info("Hourly refresh-and-validate job complete");
    }
}
