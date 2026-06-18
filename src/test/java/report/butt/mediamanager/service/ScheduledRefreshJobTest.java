package report.butt.mediamanager.service;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;

class ScheduledRefreshJobTest {

    private final MovieRefreshService movieRefreshService = mock(MovieRefreshService.class);
    private final TvRefreshService tvRefreshService = mock(TvRefreshService.class);
    private final ValidatorService validatorService = mock(ValidatorService.class);
    private final NotificationService notificationService = mock(NotificationService.class);

    @Test
    void refreshAndValidateDelegatesInOrder() {
        var job = new ScheduledRefreshJob(
                movieRefreshService, tvRefreshService, validatorService, notificationService, true);

        job.refreshAndValidate();

        verify(movieRefreshService).refreshAll();
        verify(tvRefreshService).refreshAll();
        verify(validatorService).validateAllMovies();
        verify(validatorService).validateAllTv();
    }

    @Test
    void runNotificationsCallsServiceWhenEnabled() {
        var job = new ScheduledRefreshJob(
                movieRefreshService, tvRefreshService, validatorService, notificationService, true);

        job.runNotifications();

        verify(notificationService).runCheck();
    }

    @Test
    void runNotificationsSkipsServiceWhenDisabled() {
        var job = new ScheduledRefreshJob(
                movieRefreshService, tvRefreshService, validatorService, notificationService, false);

        job.runNotifications();

        verify(notificationService, never()).runCheck();
    }
}
