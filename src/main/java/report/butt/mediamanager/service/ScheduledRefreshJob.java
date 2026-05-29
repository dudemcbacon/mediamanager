package report.butt.mediamanager.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import report.butt.mediamanager.repository.MovieRequestRepository;
import report.butt.mediamanager.repository.TvRequestRepository;

@Component
public class ScheduledRefreshJob {

    private static final Logger log = LoggerFactory.getLogger(ScheduledRefreshJob.class);

    private final MovieRefreshService movieRefreshService;
    private final TvRefreshService tvRefreshService;
    private final ValidatorService validatorService;
    private final MovieRequestRepository movieRequestRepository;
    private final TvRequestRepository tvRequestRepository;

    public ScheduledRefreshJob(
            MovieRefreshService movieRefreshService,
            TvRefreshService tvRefreshService,
            ValidatorService validatorService,
            MovieRequestRepository movieRequestRepository,
            TvRequestRepository tvRequestRepository) {
        this.movieRefreshService = movieRefreshService;
        this.tvRefreshService = tvRefreshService;
        this.validatorService = validatorService;
        this.movieRequestRepository = movieRequestRepository;
        this.tvRequestRepository = tvRequestRepository;
    }

    @Scheduled(cron = "0 0 * * * *")
    public void refreshAndValidate() {
        log.info("Hourly refresh-and-validate job starting");
        movieRefreshService.refreshAll();
        tvRefreshService.refreshAll();
        movieRequestRepository.findAll().forEach(validatorService::validate);
        tvRequestRepository.findAll().forEach(validatorService::validate);
        log.info("Hourly refresh-and-validate job complete");
    }
}
