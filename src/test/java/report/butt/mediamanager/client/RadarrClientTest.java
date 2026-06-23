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
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import report.butt.mediamanager.model.radarr.Movie;
import report.butt.mediamanager.model.radarr.RadarrCommand;
import report.butt.mediamanager.model.radarr.RadarrHealthItem;
import report.butt.mediamanager.model.radarr.RadarrQueue;

@NullMarked
class RadarrClientTest {

    private static final String BASE = "http://radarr";
    private static final String KEY = "test-key";

    private MockRestServiceServer server;
    private RadarrClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        client = new RadarrClient(builder, BASE, KEY);
    }

    // --- cacheQualityProfiles ---

    @Test
    void cacheQualityProfilesSuccess() {
        server.expect(requestTo(BASE + "/api/v3/qualityprofile"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(
                        "[{\"id\":1,\"name\":\"HD-1080p\"},{\"id\":2,\"name\":\"Any\"}]", MediaType.APPLICATION_JSON));

        client.cacheQualityProfiles();
        server.verify();

        Map<Integer, String> profiles = client.getQualityProfilesById();
        assertEquals("HD-1080p", profiles.get(1));
        assertEquals("Any", profiles.get(2));
    }

    @Test
    void cacheQualityProfilesServerErrorDegracesGracefully() {
        server.expect(requestTo(BASE + "/api/v3/qualityprofile"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withServerError());

        // Should not throw — catch block swallows the error
        client.cacheQualityProfiles();
        server.verify();

        assertTrue(client.getQualityProfilesById().isEmpty());
    }

    // --- getQualityProfileIdByName ---

    @Test
    void getQualityProfileIdByNameReturnsIdWhenFound() {
        server.expect(requestTo(BASE + "/api/v3/qualityprofile"))
                .andRespond(withSuccess("[{\"id\":3,\"name\":\"Remux\"}]", MediaType.APPLICATION_JSON));
        client.cacheQualityProfiles();

        assertEquals(3, client.getQualityProfileIdByName("Remux"));
    }

    @Test
    void getQualityProfileIdByNameReturnsNullWhenNotFound() {
        server.expect(requestTo(BASE + "/api/v3/qualityprofile"))
                .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));
        client.cacheQualityProfiles();

        assertNull(client.getQualityProfileIdByName("NonExistent"));
    }

    // --- getHealth ---

    @Test
    void getHealthReturnsItems() {
        server.expect(requestTo(BASE + "/api/v3/health"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(
                        "[{\"source\":\"IndexerRssCheck\",\"type\":\"warning\",\"message\":\"Disk low\",\"wikiUrl\":\"http://wiki\"}]",
                        MediaType.APPLICATION_JSON));

        List<RadarrHealthItem> health = client.getHealth();
        server.verify();

        assertEquals(1, health.size());
        assertEquals("warning", health.get(0).getType());
        assertEquals("Disk low", health.get(0).getMessage());
    }

    @Test
    void getHealthReturnsEmptyList() {
        server.expect(requestTo(BASE + "/api/v3/health")).andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));

        List<RadarrHealthItem> health = client.getHealth();
        assertTrue(health.isEmpty());
    }

    // --- getMovies ---

    @Test
    void getMoviesReturnsList() {
        server.expect(requestTo(BASE + "/api/v3/movie"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(
                        "[{\"id\":10,\"title\":\"Inception\",\"tmdbId\":27205}]", MediaType.APPLICATION_JSON));

        List<Movie> movies = client.getMovies();
        server.verify();

        assertEquals(1, movies.size());
        assertEquals("Inception", movies.get(0).getTitle());
        assertEquals(10, movies.get(0).getId());
    }

    // --- getMovieById ---

    @Test
    void getMovieByIdReturnsSingleMovie() {
        server.expect(requestTo(BASE + "/api/v3/movie/42"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("{\"id\":42,\"title\":\"Dune\"}", MediaType.APPLICATION_JSON));

        Movie movie = client.getMovieById(42L);
        server.verify();

        assertNotNull(movie);
        assertEquals("Dune", movie.getTitle());
    }

    // --- getMoviesByTmdbId ---

    @Test
    void getMoviesByTmdbIdPassesQueryParams() {
        server.expect(requestTo(BASE + "/api/v3/movie?tmdbId=27205&excludeLocalCovers=false"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("[{\"id\":10,\"title\":\"Inception\"}]", MediaType.APPLICATION_JSON));

        List<Movie> movies = client.getMoviesByTmdbId(27205);
        server.verify();

        assertEquals(1, movies.size());
    }

    // --- searchMovies ---

    @Test
    void searchMoviesPostsCommandAndReturnsResult() {
        server.expect(requestTo(BASE + "/api/v3/command"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(
                        "{\"id\":99,\"name\":\"MoviesSearch\",\"status\":\"queued\"}", MediaType.APPLICATION_JSON));

        RadarrCommand cmd = client.searchMovies(List.of(1, 2));
        server.verify();

        assertEquals(99, cmd.getId());
        assertEquals("queued", cmd.getStatus());
    }

    // --- getQueue ---

    @Test
    void getQueueReturnsQueueObject() {
        server.expect(requestTo(BASE + "/api/v3/queue?pageSize=10000"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("{\"totalRecords\":0,\"records\":[]}", MediaType.APPLICATION_JSON));

        RadarrQueue queue = client.getQueue();
        server.verify();

        assertNotNull(queue);
        assertEquals(0, queue.getTotalRecords());
    }

    // --- deleteQueueItem ---

    @Test
    void deleteQueueItemIssuesDeleteWithQueryParams() {
        server.expect(requestTo(BASE + "/api/v3/queue/55?removeFromClient=true&blocklist=true"))
                .andExpect(method(HttpMethod.DELETE))
                .andRespond(withSuccess("", MediaType.APPLICATION_JSON));

        client.deleteQueueItem(55);
        server.verify();
    }

    // --- updateMovieQualityProfile ---

    @Test
    void updateMovieQualityProfileIssuesPutRequest() {
        server.expect(requestTo(BASE + "/api/v3/movie/editor"))
                .andExpect(method(HttpMethod.PUT))
                .andRespond(withSuccess("", MediaType.APPLICATION_JSON));

        client.updateMovieQualityProfile(10, 2);
        server.verify();
    }
}
