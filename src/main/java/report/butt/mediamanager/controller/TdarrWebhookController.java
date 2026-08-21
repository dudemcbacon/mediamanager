package report.butt.mediamanager.controller;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Map;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import report.butt.mediamanager.model.tdarr.TdarrTranscodeUpdate;
import report.butt.mediamanager.service.TdarrUpdateService;

/**
 * Webhook for Tdarr to call when a transcode finishes, so the library picks up the new verdict immediately instead of
 * waiting for the next refresh to poll it. Point a flow's "Send Web Request" node at {@code POST
 * /api/tdarr/transcode-complete}.
 *
 * <p>Authenticated by the {@code X-Tdarr-Token} header rather than a session: Tdarr is a machine caller with no login.
 * The path is therefore opened in {@code SecurityConfig} (and exempted from CSRF) and guarded here instead — so this
 * class deliberately carries no {@code @PreAuthorize}, unlike the other controllers. A blank configured token rejects
 * everything, so a misconfigured deployment fails closed rather than exposing an open write endpoint.
 */
@RestController
@NullMarked
public class TdarrWebhookController {

    /** Public so {@code SecurityConfig} and this controller can't disagree about which path is opened. */
    public static final String PATH = "/api/tdarr/transcode-complete";

    static final String TOKEN_HEADER = "X-Tdarr-Token";

    private static final Logger log = LoggerFactory.getLogger(TdarrWebhookController.class);

    private final TdarrUpdateService tdarrUpdateService;
    private final String expectedToken;

    public TdarrWebhookController(
            TdarrUpdateService tdarrUpdateService, @Value("${mediamanager.tdarr-webhook.token}") String expectedToken) {
        this.tdarrUpdateService = tdarrUpdateService;
        this.expectedToken = expectedToken;
    }

    @PostMapping(PATH)
    ResponseEntity<?> transcodeComplete(
            @RequestHeader(name = TOKEN_HEADER, required = false) @Nullable String token,
            @RequestBody TdarrTranscodeUpdate update) {
        if (!tokenMatches(token)) {
            // The supplied token is deliberately not logged — it would put a credential in the log file.
            log.warn("Rejected Tdarr webhook with missing or invalid {}", TOKEN_HEADER);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "invalid " + TOKEN_HEADER));
        }

        TdarrUpdateService.Result result;
        try {
            result = tdarrUpdateService.apply(update);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }

        var body = Map.<String, Object>of(
                "filename", result.filename(),
                "matched", result.matched(),
                "movies", result.movies(),
                "episodes", result.episodes(),
                "updated", result.updated());
        // 404 rather than an empty 200: a filename this library doesn't know about is a real mismatch, and surfacing it
        // as a failure in the Tdarr flow is more useful than a silent success.
        return result.matched() == 0
                ? ResponseEntity.status(HttpStatus.NOT_FOUND).body(body)
                : ResponseEntity.ok(body);
    }

    /** Constant-time comparison, so a wrong token can't be recovered by timing the response. */
    private boolean tokenMatches(@Nullable String token) {
        if (token == null || expectedToken.isBlank()) {
            return false;
        }
        return MessageDigest.isEqual(
                token.getBytes(StandardCharsets.UTF_8), expectedToken.getBytes(StandardCharsets.UTF_8));
    }
}
