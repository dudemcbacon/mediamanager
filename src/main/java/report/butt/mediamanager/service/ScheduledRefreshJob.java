package report.butt.mediamanager.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import report.butt.mediamanager.repository.MovieRequestRepository;

@Component
public class ScheduledRefreshJob {

  private static final Logger log = LoggerFactory.getLogger(ScheduledRefreshJob.class);

  private final RefreshService refreshService;
  private final MovieValidatorService movieValidatorService;
  private final MovieRequestRepository movieRequestRepository;

  public ScheduledRefreshJob(RefreshService refreshService, MovieValidatorService movieValidatorService,
      MovieRequestRepository movieRequestRepository) {
    this.refreshService = refreshService;
    this.movieValidatorService = movieValidatorService;
    this.movieRequestRepository = movieRequestRepository;
  }

  @Scheduled(cron = "0 0 * * * *")
  public void refreshAndValidate() {
    log.info("Hourly refresh-and-validate job starting");
    refreshService.refreshAll();
    movieRequestRepository.findAll().forEach(movieValidatorService::validate);
    log.info("Hourly refresh-and-validate job complete");
  }
}
