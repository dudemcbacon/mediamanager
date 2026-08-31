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
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import report.butt.mediamanager.service.SeedlessTorrentTracker;

/**
 * Machine-callable admin actions, for triggering work that otherwise only runs on a schedule.
 *
 * <p>The seedless sweep is here because it is the one code path that both calls Deluge and writes to the database, which
 * makes it the useful shape for confirming a distributed trace end to end: one web transaction containing an external
 * segment for Deluge and datastore segments for the {@code seedless_torrent} writes.
 *
 * <p>Authenticated by the {@code X-Admin-Token} header rather than a session, so it can be called with curl from outside
 * a browser — the same arrangement as {@link TdarrWebhookController}. The path is therefore opened in
 * {@code SecurityConfig} (and exempted from CSRF) and guarded here instead, so this class deliberately carries no
 * {@code @PreAuthorize}. A blank configured token rejects everything, so a misconfigured deployment fails closed rather
 * than exposing an open write endpoint.
 */
@RestController
@NullMarked
public class AdminApiController {

    /** Public so {@code SecurityConfig} and this controller can't disagree about which path is opened. */
    public static final String SEEDLESS_SWEEP_PATH = "/api/admin/seedless-sweep";

    static final String TOKEN_HEADER = "X-Admin-Token";

    private static final Logger log = LoggerFactory.getLogger(AdminApiController.class);

    private final SeedlessTorrentTracker seedlessTorrentTracker;
    private final String expectedToken;

    public AdminApiController(
            SeedlessTorrentTracker seedlessTorrentTracker,
            @Value("${mediamanager.admin-api.token}") String expectedToken) {
        this.seedlessTorrentTracker = seedlessTorrentTracker;
        this.expectedToken = expectedToken;
    }

    /**
     * Runs the seedless-torrent sweep now instead of waiting for the hourly job. Synchronous on purpose: the caller
     * wants the work to happen inside this request so it lands in one trace, and the response reports what changed.
     */
    @PostMapping(SEEDLESS_SWEEP_PATH)
    ResponseEntity<?> seedlessSweep(@RequestHeader(name = TOKEN_HEADER, required = false) @Nullable String token) {
        if (!tokenMatches(token)) {
            // The supplied token is deliberately not logged — it would put a credential in the log file.
            log.warn("Rejected admin sweep request with missing or invalid {}", TOKEN_HEADER);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "invalid " + TOKEN_HEADER));
        }

        log.info("Running seedless sweep on request to {}", SEEDLESS_SWEEP_PATH);
        SeedlessTorrentTracker.SweepResult result = seedlessTorrentTracker.sweep();

        var body = Map.<String, Object>of(
                "skipped", result.skipped(),
                "seedless", result.seedless(),
                "newlySeedless", result.newlySeedless(),
                "cleared", result.cleared());
        // 503 when the sweep declined to run: Deluge returned nothing, so no write happened and reporting success would
        // be misleading — for a trace check especially, the caller needs to know the interesting half didn't occur.
        return result.skipped()
                ? ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(body)
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
