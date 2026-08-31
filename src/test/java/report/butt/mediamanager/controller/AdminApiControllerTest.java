package report.butt.mediamanager.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import report.butt.mediamanager.service.SeedlessTorrentTracker;

@NullMarked
class AdminApiControllerTest {

    private static final String TOKEN = "s3cret-token";

    private final SeedlessTorrentTracker seedlessTorrentTracker = mock(SeedlessTorrentTracker.class);
    private final AdminApiController controller = new AdminApiController(seedlessTorrentTracker, TOKEN);

    @SuppressWarnings("unchecked") // Safe: the controller always builds a Map<String, Object> body.
    private static Map<String, Object> bodyOf(ResponseEntity<?> response) {
        Object body = response.getBody();
        assertNotNull(body);
        return (Map<String, Object>) body;
    }

    @Test
    void aValidTokenRunsTheSweepAndReportsWhatChanged() {
        when(seedlessTorrentTracker.sweep()).thenReturn(new SeedlessTorrentTracker.SweepResult(false, 7, 2, 1));

        ResponseEntity<?> response = controller.seedlessSweep(TOKEN);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        Map<String, Object> body = bodyOf(response);
        assertEquals(false, body.get("skipped"));
        assertEquals(7, body.get("seedless"));
        assertEquals(2, body.get("newlySeedless"));
        assertEquals(1, body.get("cleared"));
        verify(seedlessTorrentTracker).sweep();
    }

    @Test
    void aSweepThatDeclinedToRunIsNotReportedAsSuccess() {
        // Deluge returned nothing, so nothing was written; a 200 here would misreport a trace check as meaningful.
        when(seedlessTorrentTracker.sweep()).thenReturn(new SeedlessTorrentTracker.SweepResult(true, 0, 0, 0));

        ResponseEntity<?> response = controller.seedlessSweep(TOKEN);

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, response.getStatusCode());
        assertEquals(true, bodyOf(response).get("skipped"));
    }

    @Test
    void aMissingTokenIsUnauthorizedAndRunsNothing() {
        ResponseEntity<?> response = controller.seedlessSweep(null);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        verify(seedlessTorrentTracker, never()).sweep();
    }

    @Test
    void aWrongTokenIsUnauthorizedAndRunsNothing() {
        ResponseEntity<?> response = controller.seedlessSweep("nope");

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        verify(seedlessTorrentTracker, never()).sweep();
    }

    @Test
    void aBlankConfiguredTokenRejectsEverything() {
        // Fails closed: a deployment that forgot to set the token must not expose an open write endpoint.
        var unconfigured = new AdminApiController(seedlessTorrentTracker, "");

        assertEquals(HttpStatus.UNAUTHORIZED, unconfigured.seedlessSweep("").getStatusCode());
        assertEquals(HttpStatus.UNAUTHORIZED, unconfigured.seedlessSweep("anything").getStatusCode());
        verify(seedlessTorrentTracker, never()).sweep();
    }
}
