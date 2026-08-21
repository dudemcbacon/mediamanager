package report.butt.mediamanager.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import report.butt.mediamanager.client.PlexClient;
import report.butt.mediamanager.model.MovieRequest;
import report.butt.mediamanager.repository.MovieRequestRepository;

/**
 * Boots the real filter chain to prove the webhook's security wiring, which the plain controller unit test cannot: that
 * {@code SecurityConfig} permits the path (otherwise Vaadin's default-deny would 302 it to the login view) and exempts
 * it from CSRF (otherwise an external POST with no CSRF token would 403). Also covers the round trip from JSON body
 * through the JPQL lookup to a persisted row.
 */
// Same bootstrap/validation overrides as TvHierarchyServiceTest, plus a known webhook token to authenticate with.
@SpringBootTest(
        properties = {
            "mediamanager.bootstrap.username=test",
            "mediamanager.bootstrap.password=test-admin-pw",
            "mediamanager.validate-required-config=false",
            "mediamanager.tdarr-webhook.token=integration-test-token",
            "jobrunr.background-job-server.enabled=false",
            "jobrunr.dashboard.enabled=false"
        })
@AutoConfigureMockMvc
@Transactional
@NullMarked
class TdarrWebhookSecurityTest {

    private static final String TOKEN = "integration-test-token";
    private static final String BODY = """
            {"filename":"%s",
             "HealthCheck":"Success",
             "TranscodeDecisionMaker":"Transcode success",
             "oldSize":0.28345867712050676,
             "newSize":0.1351956408470869}
            """;

    // PlexClient's @PostConstruct aborts without real plex.url config, which tests don't supply — same reason
    // TvHierarchyServiceTest mocks it. Nothing on the webhook path touches Plex.
    @MockitoBean
    private PlexClient plexClient;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MovieRequestRepository movieRequestRepository;

    /** Reaching the controller's own 404 (rather than a redirect or 403) is what proves the path is permitted. */
    @Test
    void aValidTokenReachesTheControllerThroughTheFilterChain() throws Exception {
        mockMvc.perform(post(TdarrWebhookController.PATH)
                        .header(TdarrWebhookController.TOKEN_HEADER, TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(BODY.formatted("no-such-file-in-this-library.mkv")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.matched").value(0));
    }

    /** No CSRF token is sent, so a 401 (not a 403) proves the CSRF exemption is in place. */
    @Test
    void aMissingTokenIs401RatherThanACsrfOrLoginFailure() throws Exception {
        mockMvc.perform(post(TdarrWebhookController.PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(BODY.formatted("anything.mkv")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void aWrongTokenIsUnauthorized() throws Exception {
        mockMvc.perform(post(TdarrWebhookController.PATH)
                        .header(TdarrWebhookController.TOKEN_HEADER, "wrong")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(BODY.formatted("anything.mkv")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void aMatchingFilenameIsResolvedAndPersisted() throws Exception {
        var movie = new MovieRequest("Webhook Test Movie", 999_001, true, 999_001, "Common.Available");
        movie.setPlexMediaFilename("/media/Movies/Webhook Test (2024)/Webhook Test (2024).mkv");
        MovieRequest saved = movieRequestRepository.save(movie);

        mockMvc.perform(post(TdarrWebhookController.PATH)
                        .header(TdarrWebhookController.TOKEN_HEADER, TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(BODY.formatted("Webhook Test (2024).mkv")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.movies").value(1))
                .andExpect(jsonPath("$.episodes").value(0));

        MovieRequest reloaded = movieRequestRepository.findById(saved.getId()).orElseThrow();
        assertEquals("Success", reloaded.getTdarrHealthCheck());
        assertEquals("Transcode success", reloaded.getTdarrTranscodeDecisionMaker());
        assertEquals(0.28345867712050676, reloaded.getTdarrOldSizeGb());
        assertEquals(0.1351956408470869, reloaded.getTdarrNewSizeGb());
        assertNotNull(reloaded.getTdarrLastUpdated(), "the webhook should stamp tdarrLastUpdated");
    }

    /** A full path must resolve too, and must not be widened into a basename match. */
    @Test
    void aFullPathIsResolvedExactly() throws Exception {
        var movie = new MovieRequest("Webhook Path Movie", 999_002, true, 999_002, "Common.Available");
        movie.setPlexMediaFilename("/media/Movies/Webhook Path (2024)/Webhook Path (2024).mkv");
        MovieRequest saved = movieRequestRepository.save(movie);

        mockMvc.perform(post(TdarrWebhookController.PATH)
                        .header(TdarrWebhookController.TOKEN_HEADER, TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(BODY.formatted("/media/Movies/Webhook Path (2024)/Webhook Path (2024).mkv")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.movies").value(1));

        assertEquals(
                "Transcode success",
                movieRequestRepository.findById(saved.getId()).orElseThrow().getTdarrTranscodeDecisionMaker());
    }
}
