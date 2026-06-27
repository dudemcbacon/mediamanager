package report.butt.mediamanager.service;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;

@NullMarked
class ScheduledRefreshJobTest {

    private final MovieRefreshService movieRefreshService = mock(MovieRefreshService.class);
    private final TvRefreshService tvRefreshService = mock(TvRefreshService.class);
    private final ValidatorService validatorService = mock(ValidatorService.class);
    private final SeedlessTorrentTracker seedlessTorrentTracker = mock(SeedlessTorrentTracker.class);
    private final NotificationService notificationService = mock(NotificationService.class);

    @Test
    void refreshAndValidateDelegatesInOrder() {
        var job = new ScheduledRefreshJob(
                movieRefreshService,
                tvRefreshService,
                validatorService,
                seedlessTorrentTracker,
                notificationService,
                true);

        job.refreshAndValidate();

        verify(movieRefreshService).refreshAll();
        verify(tvRefreshService).refreshAll();
        verify(validatorService).validateAllMovies();
        verify(validatorService).validateAllTv();
        verify(seedlessTorrentTracker).sweep();
    }

    @Test
    void continuesRemainingStepsWhenOneFails() {
        var job = new ScheduledRefreshJob(
                movieRefreshService,
                tvRefreshService,
                validatorService,
                seedlessTorrentTracker,
                notificationService,
                true);
        doThrow(new RuntimeException("ombi down")).when(movieRefreshService).refreshAll();

        job.refreshAndValidate();

        verify(tvRefreshService).refreshAll();
        verify(validatorService).validateAllMovies();
        verify(validatorService).validateAllTv();
    }

    @Test
    void skipsRunWhenAnotherIsInProgress() throws Exception {
        var entered = new CountDownLatch(1);
        var release = new CountDownLatch(1);
        doAnswer(invocation -> {
                    entered.countDown();
                    release.await();
                    return null;
                })
                .when(movieRefreshService)
                .refreshAll();

        var job = new ScheduledRefreshJob(
                movieRefreshService,
                tvRefreshService,
                validatorService,
                seedlessTorrentTracker,
                notificationService,
                true);
        var firstRun = new Thread(job::refreshAndValidate);
        firstRun.start();
        assertTrue(entered.await(2, TimeUnit.SECONDS), "first run should enter the locked section");

        // Second trigger while the first run holds the lock must skip without running any step.
        job.refreshAndValidate();
        verify(tvRefreshService, never()).refreshAll();

        release.countDown();
        firstRun.join(2000);
    }

    @Test
    void runNotificationsCallsServiceWhenEnabled() {
        var job = new ScheduledRefreshJob(
                movieRefreshService,
                tvRefreshService,
                validatorService,
                seedlessTorrentTracker,
                notificationService,
                true);

        job.runNotifications();

        verify(notificationService).runCheck();
    }

    @Test
    void runNotificationsSkipsServiceWhenDisabled() {
        var job = new ScheduledRefreshJob(
                movieRefreshService,
                tvRefreshService,
                validatorService,
                seedlessTorrentTracker,
                notificationService,
                false);

        job.runNotifications();

        verify(notificationService, never()).runCheck();
    }
}
