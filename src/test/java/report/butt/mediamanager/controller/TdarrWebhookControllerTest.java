package report.butt.mediamanager.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import report.butt.mediamanager.model.tdarr.TdarrTranscodeUpdate;
import report.butt.mediamanager.service.TdarrUpdateService;

@NullMarked
class TdarrWebhookControllerTest {

    private static final String TOKEN = "s3cret-token";

    private final TdarrUpdateService updateService = mock(TdarrUpdateService.class);
    private final TdarrWebhookController controller = new TdarrWebhookController(updateService, TOKEN);

    private static TdarrTranscodeUpdate body() {
        var update = new TdarrTranscodeUpdate();
        update.setFilename("1992 (2024).mkv");
        update.setTranscodeDecisionMaker("Transcode success");
        return update;
    }

    @SuppressWarnings("unchecked") // The controller always builds a Map body; the test asserts on its entries.
    private static Map<String, Object> bodyOf(ResponseEntity<?> response) {
        Object body = response.getBody();
        assertNotNull(body);
        return (Map<String, Object>) body;
    }

    // --- token enforcement ---

    @Test
    void rejectsAMissingToken() {
        ResponseEntity<?> response = controller.transcodeComplete(null, body());

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        verify(updateService, never()).apply(any());
    }

    @Test
    void rejectsAWrongToken() {
        ResponseEntity<?> response = controller.transcodeComplete("nope", body());

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        verify(updateService, never()).apply(any());
    }

    /** A token that is a prefix of the real one must not pass. */
    @Test
    void rejectsAPrefixOfTheToken() {
        ResponseEntity<?> response = controller.transcodeComplete(TOKEN.substring(0, 4), body());

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        verify(updateService, never()).apply(any());
    }

    /** Fail closed: with no token configured the endpoint must refuse everything rather than accept anything. */
    @Test
    void rejectsEverythingWhenNoTokenIsConfigured() {
        var unconfigured = new TdarrWebhookController(updateService, "");

        assertEquals(
                HttpStatus.UNAUTHORIZED,
                unconfigured.transcodeComplete(null, body()).getStatusCode());
        assertEquals(
                HttpStatus.UNAUTHORIZED,
                unconfigured.transcodeComplete("", body()).getStatusCode());
        assertEquals(
                HttpStatus.UNAUTHORIZED,
                unconfigured.transcodeComplete("anything", body()).getStatusCode());
        verify(updateService, never()).apply(any());
    }

    // --- happy path and outcomes ---

    @Test
    void appliesTheUpdateWhenTheTokenMatches() {
        when(updateService.apply(any()))
                .thenReturn(new TdarrUpdateService.Result(
                        "1992 (2024).mkv", 1, 0, List.of("MOVIE 42 /media/Movies/1992 (2024).mkv")));

        ResponseEntity<?> response = controller.transcodeComplete(TOKEN, body());

        assertEquals(HttpStatus.OK, response.getStatusCode());
        Map<String, Object> body = bodyOf(response);
        assertEquals("1992 (2024).mkv", body.get("filename"));
        assertEquals(1, body.get("matched"));
        assertEquals(1, body.get("movies"));
        assertEquals(0, body.get("episodes"));
        verify(updateService).apply(any());
    }

    @Test
    void reportsNotFoundWhenTheFilenameMatchesNothing() {
        when(updateService.apply(any())).thenReturn(new TdarrUpdateService.Result("ghost.mkv", 0, 0, List.of()));

        ResponseEntity<?> response = controller.transcodeComplete(TOKEN, body());

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertEquals(0, bodyOf(response).get("matched"));
    }

    @Test
    void reportsBadRequestWhenTheBodyHasNoFilename() {
        when(updateService.apply(any())).thenThrow(new IllegalArgumentException("filename is required"));

        ResponseEntity<?> response = controller.transcodeComplete(TOKEN, new TdarrTranscodeUpdate());

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("filename is required", bodyOf(response).get("error"));
    }

    @Test
    void countsBothMoviesAndEpisodesInTheResponse() {
        when(updateService.apply(any()))
                .thenReturn(new TdarrUpdateService.Result(
                        "x.mkv", 1, 2, List.of("MOVIE 42 /a/x.mkv", "EPISODE 7 /b/x.mkv", "EPISODE 8 /c/x.mkv")));

        Map<String, Object> body = bodyOf(controller.transcodeComplete(TOKEN, body()));

        assertEquals(3, body.get("matched"));
        assertEquals(1, body.get("movies"));
        assertEquals(2, body.get("episodes"));
        assertEquals(3, ((List<?>) body.get("updated")).size());
    }
}
