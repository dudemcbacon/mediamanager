package report.butt.mediamanager.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.headerDoesNotExist;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withUnauthorizedRequest;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import report.butt.mediamanager.model.tdarr.TdarrFile;

@NullMarked
class TdarrClientTest {

    private static final String BASE = "http://tdarr";
    private static final String SEARCH_URL = BASE + "/api/v2/client/search";
    private static final String USERNAME = "tdarr-user";
    private static final String PASSWORD = "tdarr-pass";
    private static final String EXPECTED_BASIC =
            "Basic " + Base64.getEncoder().encodeToString((USERNAME + ":" + PASSWORD).getBytes(StandardCharsets.UTF_8));

    private static final String FULL_PATH =
            "/media/TV/Absolutely Fabulous/Season 3/Absolutely Fabulous - S03E03 - Sex.mkv";
    private static final String FILENAME = "Absolutely Fabulous - S03E03 - Sex.mkv";

    /** Trimmed from a real response; keeps a couple of unmapped keys to prove unknown properties are ignored. */
    private static final String MATCH_BODY = """
            {"array":[{
              "_id":"/media/TV/Absolutely Fabulous/Season 3/Absolutely Fabulous - S03E03 - Sex.mkv",
              "file":"/media/TV/Absolutely Fabulous/Season 3/Absolutely Fabulous - S03E03 - Sex.mkv",
              "container":"mkv",
              "HealthCheck":"Success",
              "TranscodeDecisionMaker":"Transcode success",
              "oldSize":0.28345867712050676,
              "newSize":0.1351956408470869,
              "newVsOldRatio":47.7
            }],"totalCount":1}
            """;

    private static final String NO_MATCH_BODY = """
            {"array":[],"totalCount":0}
            """;

    private MockRestServiceServer server;
    private TdarrClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        client = new TdarrClient(builder, BASE, USERNAME, PASSWORD);
    }

    // --- first request: basic auth, and the filename alone is what gets searched ---

    @Test
    void searchesByFilenameWithBasicAuthOnTheFirstRequest() {
        server.expect(requestTo(SEARCH_URL))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header(HttpHeaders.AUTHORIZATION, EXPECTED_BASIC))
                .andExpect(headerDoesNotExist(HttpHeaders.COOKIE))
                .andExpect(jsonPath("$.data.filters[0].id").value("file"))
                // The full path is never sent — only the basename, which Tdarr matches as a substring.
                .andExpect(jsonPath("$.data.filters[0].value").value(FILENAME))
                .andExpect(jsonPath("$.data.start").value(0))
                .andExpect(jsonPath("$.data.pageSize").value(10))
                .andRespond(withSuccess(MATCH_BODY, MediaType.APPLICATION_JSON));

        TdarrFile file = client.findByPath(FULL_PATH);
        server.verify();

        assertNotNull(file);
        assertEquals("Success", file.getHealthCheck());
        assertEquals("Transcode success", file.getTranscodeDecisionMaker());
        assertEquals(0.28345867712050676, file.getOldSize());
        assertEquals(0.1351956408470869, file.getNewSize());
        assertEquals(FULL_PATH, file.getFile());
    }

    // --- a returned cookie replaces basic auth on later requests ---

    @Test
    void replaysSessionCookieOnSubsequentRequests() {
        server.expect(requestTo(SEARCH_URL))
                .andExpect(header(HttpHeaders.AUTHORIZATION, EXPECTED_BASIC))
                .andRespond(withSuccess(MATCH_BODY, MediaType.APPLICATION_JSON)
                        .header(HttpHeaders.SET_COOKIE, "Tdarr_session=abc123; Path=/; HttpOnly"));

        server.expect(requestTo(SEARCH_URL))
                .andExpect(header(HttpHeaders.COOKIE, "Tdarr_session=abc123"))
                .andExpect(headerDoesNotExist(HttpHeaders.AUTHORIZATION))
                .andRespond(withSuccess(MATCH_BODY, MediaType.APPLICATION_JSON));

        assertNotNull(client.findByPath(FULL_PATH));
        assertNotNull(client.findByPath(FULL_PATH));
        server.verify();
    }

    // --- no cookie offered (Tdarr behind a proxy that authenticates every call): basic auth keeps being sent ---

    @Test
    void keepsSendingBasicAuthWhenNoCookieIsOffered() {
        server.expect(requestTo(SEARCH_URL))
                .andExpect(header(HttpHeaders.AUTHORIZATION, EXPECTED_BASIC))
                .andRespond(withSuccess(MATCH_BODY, MediaType.APPLICATION_JSON));

        server.expect(requestTo(SEARCH_URL))
                .andExpect(header(HttpHeaders.AUTHORIZATION, EXPECTED_BASIC))
                .andExpect(headerDoesNotExist(HttpHeaders.COOKIE))
                .andRespond(withSuccess(MATCH_BODY, MediaType.APPLICATION_JSON));

        assertNotNull(client.findByPath(FULL_PATH));
        assertNotNull(client.findByPath(FULL_PATH));
        server.verify();
    }

    // --- expired cookie: 401 → drop it, re-authenticate with basic auth, retry once ---

    @Test
    void reAuthenticatesWhenTheSessionCookieIsRejected() {
        server.expect(requestTo(SEARCH_URL))
                .andRespond(withSuccess(MATCH_BODY, MediaType.APPLICATION_JSON)
                        .header(HttpHeaders.SET_COOKIE, "Tdarr_session=stale; Path=/"));

        server.expect(requestTo(SEARCH_URL))
                .andExpect(header(HttpHeaders.COOKIE, "Tdarr_session=stale"))
                .andRespond(withUnauthorizedRequest());

        server.expect(requestTo(SEARCH_URL))
                .andExpect(header(HttpHeaders.AUTHORIZATION, EXPECTED_BASIC))
                .andExpect(headerDoesNotExist(HttpHeaders.COOKIE))
                .andRespond(withSuccess(MATCH_BODY, MediaType.APPLICATION_JSON));

        assertNotNull(client.findByPath(FULL_PATH));
        TdarrFile afterReAuth = client.findByPath(FULL_PATH);
        server.verify();

        assertNotNull(afterReAuth);
        assertEquals("Success", afterReAuth.getHealthCheck());
    }

    // --- misses and failures are null, never exceptions ---

    @Test
    void returnsNullWhenTdarrKnowsNothingAboutTheFile() {
        server.expect(requestTo(SEARCH_URL)).andRespond(withSuccess(NO_MATCH_BODY, MediaType.APPLICATION_JSON));

        assertNull(client.findByPath(FULL_PATH));
        server.verify();
    }

    @Test
    void returnsNullWhenTdarrErrors() {
        server.expect(requestTo(SEARCH_URL)).andRespond(withServerError());

        assertNull(client.findByPath(FULL_PATH));
        server.verify();
    }

    // --- substring matching can pull in neighbours; the exact path wins ---

    @Test
    void prefersTheExactPathWhenSeveralFilesMatch() {
        String ambiguous = """
                {"array":[
                  {"file":"/media/TV/Other Show/Season 3/Absolutely Fabulous - S03E03 - Sex.mkv",
                   "HealthCheck":"Failed","TranscodeDecisionMaker":"Queued"},
                  {"file":"/media/TV/Absolutely Fabulous/Season 3/Absolutely Fabulous - S03E03 - Sex.mkv",
                   "HealthCheck":"Success","TranscodeDecisionMaker":"Transcode success"}
                ],"totalCount":2}
                """;

        server.expect(requestTo(SEARCH_URL)).andRespond(withSuccess(ambiguous, MediaType.APPLICATION_JSON));

        TdarrFile file = client.findByPath(FULL_PATH);
        server.verify();

        assertNotNull(file);
        assertEquals(FULL_PATH, file.getFile());
        assertEquals("Success", file.getHealthCheck());
    }

    @Test
    void returnsNullWhenSeveralFilesMatchAndNoneIsTheRequestedOne() {
        String ambiguous = """
                {"array":[
                  {"file":"/media/Movies/Sample/Sample.mkv.part1","HealthCheck":"Success"},
                  {"file":"/media/Movies/Sample/Sample.mkv.part2","HealthCheck":"Failed"}
                ],"totalCount":2}
                """;

        server.expect(requestTo(SEARCH_URL)).andRespond(withSuccess(ambiguous, MediaType.APPLICATION_JSON));

        assertNull(client.findByPath("/media/Movies/Sample/Sample.mkv"));
        server.verify();
    }

    // --- unconfigured Tdarr never reaches the network ---

    @Test
    void makesNoCallWhenTdarrUrlIsBlank() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer unusedServer =
                MockRestServiceServer.bindTo(builder).build();
        var unconfigured = new TdarrClient(builder, "", USERNAME, PASSWORD);

        assertNull(unconfigured.findByPath(FULL_PATH));
        unusedServer.verify(); // no expectations set, so any request would have failed the test
    }
}
