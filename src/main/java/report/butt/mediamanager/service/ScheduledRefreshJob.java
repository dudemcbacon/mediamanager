package report.butt.mediamanager.service;

import com.newrelic.api.agent.Trace;
import org.jspecify.annotations.NullMarked;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@NullMarked
public class ScheduledRefreshJob {

    private static final Logger log = LoggerFactory.getLogger(ScheduledRefreshJob.class);

    private final MovieRefreshService movieRefreshService;
    private final TvRefreshService tvRefreshService;
    private final ValidatorService validatorService;
    private final NotificationService notificationService;
    private final boolean notificationsEnabled;

    public ScheduledRefreshJob(
            MovieRefreshService movieRefreshService,
            TvRefreshService tvRefreshService,
            ValidatorService validatorService,
            NotificationService notificationService,
            @Value("${notifications.enabled}") boolean notificationsEnabled) {
        this.movieRefreshService = movieRefreshService;
        this.tvRefreshService = tvRefreshService;
        this.validatorService = validatorService;
        this.notificationService = notificationService;
        this.notificationsEnabled = notificationsEnabled;
    }

    @Scheduled(cron = "0 0 * * * *")
    @Trace(dispatcher = true)
    public void refreshAndValidate() {
        log.info("Hourly refresh-and-validate job starting");
        movieRefreshService.refreshAll();
        tvRefreshService.refreshAll();
        validatorService.validateAllMovies();
        validatorService.validateAllTv();
        log.info("Hourly refresh-and-validate job complete");
    }

    @Scheduled(cron = "${notifications.cron}")
    @Trace(dispatcher = true)
    public void runNotifications() {
        if (!notificationsEnabled) {
            log.info("Daily notification job skipped (notifications.enabled=false)");
            return;
        }
        log.info("Daily notification job starting");
        notificationService.runCheck();
        log.info("Daily notification job complete");
    }
}
