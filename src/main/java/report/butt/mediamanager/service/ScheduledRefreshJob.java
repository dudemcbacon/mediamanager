package report.butt.mediamanager.service;

import com.newrelic.api.agent.Trace;
import java.util.concurrent.locks.ReentrantLock;
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

    // Guards against overlapping runs: the refresh sweep can outlast the hourly trigger when an integration is slow,
    // and a second concurrent sweep would mutate the same rows. This app runs as a single instance, so an in-JVM lock
    // is sufficient (a multi-instance deployment would need a shared lock such as ShedLock instead).
    private final ReentrantLock refreshLock = new ReentrantLock();

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
        if (!refreshLock.tryLock()) {
            log.warn("Hourly refresh-and-validate still running from a previous trigger; skipping this run");
            return;
        }
        try {
            log.info("Hourly refresh-and-validate job starting");
            // Each step is isolated so a failure in one (e.g. a downed integration) doesn't abort the others.
            runStep("movie refresh", movieRefreshService::refreshAll);
            runStep("tv refresh", tvRefreshService::refreshAll);
            runStep("movie validation", validatorService::validateAllMovies);
            runStep("tv validation", validatorService::validateAllTv);
            log.info("Hourly refresh-and-validate job complete");
        } finally {
            refreshLock.unlock();
        }
    }

    private static void runStep(String name, Runnable step) {
        try {
            step.run();
        } catch (RuntimeException e) {
            log.warn("Hourly refresh-and-validate step '{}' failed; continuing with the rest", name, e);
        }
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
