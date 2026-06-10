package report.butt.mediamanager.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import report.butt.mediamanager.model.sonarr.Series;
import report.butt.mediamanager.model.sonarr.SonarrCommand;
import report.butt.mediamanager.model.sonarr.SonarrHealthItem;
import report.butt.mediamanager.model.sonarr.SonarrQueue;

class SonarrClientTest {

    private static final String BASE = "http://sonarr";

    private MockRestServiceServer server;
    private SonarrClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        client = new SonarrClient(builder, BASE, "sonarr-key");
    }

    // --- cacheQualityProfiles ---

    @Test
    void cacheQualityProfilesSuccess() {
        server.expect(requestTo(BASE + "/api/v3/qualityprofile"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("[{\"id\":1,\"name\":\"HD-1080p\"}]", MediaType.APPLICATION_JSON));

        client.cacheQualityProfiles();
        server.verify();

        Map<Integer, String> profiles = client.getQualityProfilesById();
        assertEquals("HD-1080p", profiles.get(1));
    }

    @Test
    void cacheQualityProfilesServerErrorDegracesGracefully() {
        server.expect(requestTo(BASE + "/api/v3/qualityprofile")).andRespond(withServerError());

        client.cacheQualityProfiles();
        server.verify();

        assertTrue(client.getQualityProfilesById().isEmpty());
    }

    @Test
    void getQualityProfileIdByNameReturnsIdWhenFound() {
        server.expect(requestTo(BASE + "/api/v3/qualityprofile"))
                .andRespond(withSuccess("[{\"id\":5,\"name\":\"Any\"}]", MediaType.APPLICATION_JSON));
        client.cacheQualityProfiles();

        assertEquals(5, client.getQualityProfileIdByName("Any"));
    }

    @Test
    void getQualityProfileIdByNameReturnsNullWhenMissing() {
        server.expect(requestTo(BASE + "/api/v3/qualityprofile"))
                .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));
        client.cacheQualityProfiles();

        assertNull(client.getQualityProfileIdByName("4K"));
    }

    // --- getHealth ---

    @Test
    void getHealthReturnsItems() {
        server.expect(requestTo(BASE + "/api/v3/health"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(
                        "[{\"source\":\"CheckHealth\",\"type\":\"error\",\"message\":\"Indexer down\"}]",
                        MediaType.APPLICATION_JSON));

        List<SonarrHealthItem> health = client.getHealth();
        server.verify();

        assertEquals(1, health.size());
        assertEquals("error", health.get(0).getType());
    }

    // --- getAllSeries ---

    @Test
    void getAllSeriesReturnsList() {
        server.expect(requestTo(BASE + "/api/v3/series"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(
                        "[{\"id\":1,\"title\":\"Breaking Bad\",\"tvdbId\":81189}]", MediaType.APPLICATION_JSON));

        List<Series> series = client.getAllSeries();
        server.verify();

        assertEquals(1, series.size());
        assertEquals("Breaking Bad", series.get(0).getTitle());
    }

    // --- getSeriesById ---

    @Test
    void getSeriesByIdReturnsSingle() {
        server.expect(requestTo(BASE + "/api/v3/series/7"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("{\"id\":7,\"title\":\"The Wire\"}", MediaType.APPLICATION_JSON));

        Series s = client.getSeriesById(7L);
        server.verify();

        assertNotNull(s);
        assertEquals("The Wire", s.getTitle());
    }

    // --- getSeriesByTvdbId ---

    @Test
    void getSeriesByTvdbIdPassesQueryParam() {
        server.expect(requestTo(BASE + "/api/v3/series?tvdbId=81189"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("[{\"id\":1,\"title\":\"Breaking Bad\"}]", MediaType.APPLICATION_JSON));

        List<Series> series = client.getSeriesByTvdbId(81189);
        server.verify();

        assertEquals(1, series.size());
    }

    // --- getSeriesHistory ---

    @Test
    void getSeriesHistoryPassesQueryParams() {
        server.expect(requestTo(BASE + "/api/v3/history/series?seriesId=1&includeSeries=false&includeEpisode=false"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));

        var history = client.getSeriesHistory(1);
        server.verify();

        assertTrue(history.isEmpty());
    }

    // --- searchSeries ---

    @Test
    void searchSeriesPostsCommand() {
        server.expect(requestTo(BASE + "/api/v3/command"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess("{\"id\":10,\"status\":\"queued\"}", MediaType.APPLICATION_JSON));

        SonarrCommand cmd = client.searchSeries(List.of(1, 2));
        server.verify();

        assertEquals(10, cmd.getId());
    }

    // --- searchSeason ---

    @Test
    void searchSeasonPostsCommand() {
        server.expect(requestTo(BASE + "/api/v3/command"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess("{\"id\":11,\"status\":\"queued\"}", MediaType.APPLICATION_JSON));

        SonarrCommand cmd = client.searchSeason(1, 3);
        server.verify();

        assertEquals(11, cmd.getId());
    }

    // --- searchEpisodes ---

    @Test
    void searchEpisodesPostsCommand() {
        server.expect(requestTo(BASE + "/api/v3/command"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess("{\"id\":12,\"status\":\"started\"}", MediaType.APPLICATION_JSON));

        SonarrCommand cmd = client.searchEpisodes(List.of(100, 101));
        server.verify();

        assertEquals(12, cmd.getId());
        assertEquals("started", cmd.getStatus());
    }

    // --- getEpisodes ---

    @Test
    void getEpisodesPassesQueryParams() {
        server.expect(requestTo(BASE + "/api/v3/episode?seriesId=5&includeEpisodeFile=true"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));

        var episodes = client.getEpisodes(5);
        server.verify();

        assertTrue(episodes.isEmpty());
    }

    // --- getQueue ---

    @Test
    void getQueueReturnsQueueObject() {
        server.expect(requestTo(BASE + "/api/v3/queue?pageSize=10000&includeEpisode=true"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("{\"totalRecords\":0,\"records\":[]}", MediaType.APPLICATION_JSON));

        SonarrQueue queue = client.getQueue();
        server.verify();

        assertNotNull(queue);
    }

    // --- deleteQueueItem ---

    @Test
    void deleteQueueItemIssuesDeleteWithQueryParams() {
        server.expect(requestTo(BASE + "/api/v3/queue/77?removeFromClient=true&blocklist=true"))
                .andExpect(method(HttpMethod.DELETE))
                .andRespond(withSuccess("", MediaType.APPLICATION_JSON));

        client.deleteQueueItem(77);
        server.verify();
    }

    // --- updateSeriesQualityProfile ---

    @Test
    void updateSeriesQualityProfileIssuesPutRequest() {
        server.expect(requestTo(BASE + "/api/v3/series/editor"))
                .andExpect(method(HttpMethod.PUT))
                .andRespond(withSuccess("", MediaType.APPLICATION_JSON));

        client.updateSeriesQualityProfile(5, 2);
        server.verify();
    }
}
