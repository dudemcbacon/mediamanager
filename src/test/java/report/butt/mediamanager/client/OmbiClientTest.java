package report.butt.mediamanager.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.util.List;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import report.butt.mediamanager.model.ombi.OmbiMovieRequest;
import report.butt.mediamanager.model.ombi.OmbiReprocessResponse;
import report.butt.mediamanager.model.ombi.OmbiTvRequest;
import report.butt.mediamanager.model.ombi.OmbiTvSearchResult;

@NullMarked
class OmbiClientTest {

    private static final String BASE = "http://ombi";

    private MockRestServiceServer server;
    private OmbiClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        client = new OmbiClient(builder, BASE, "ombi-api-key");
    }

    // --- getMovies ---

    @Test
    void getMoviesReturnsList() {
        server.expect(requestTo(BASE + "/api/v1/Request/movie"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(
                        "[{\"id\":1,\"title\":\"Inception\",\"theMovieDbId\":27205,\"available\":true}]",
                        MediaType.APPLICATION_JSON));

        List<OmbiMovieRequest> movies = client.getMovies();
        server.verify();

        assertEquals(1, movies.size());
        assertEquals("Inception", movies.get(0).getTitle());
        assertEquals(27205, movies.get(0).getTheMovieDbId());
    }

    @Test
    void getMoviesReturnsEmptyList() {
        server.expect(requestTo(BASE + "/api/v1/Request/movie"))
                .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));

        List<OmbiMovieRequest> movies = client.getMovies();
        assertTrue(movies.isEmpty());
    }

    // --- reprocessMovieRequest ---

    @Test
    void reprocessMovieRequestPostsToCorrectUrl() {
        server.expect(requestTo(BASE + "/api/v2/Requests/reprocess/1/42/false"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(
                        "{\"result\":true,\"message\":\"ok\",\"isError\":false}", MediaType.APPLICATION_JSON));

        OmbiReprocessResponse response = client.reprocessMovieRequest(42);
        server.verify();

        assertNotNull(response);
        assertTrue(response.getResult());
    }

    // --- markMovieAvailable ---

    @Test
    void markMovieAvailablePostsToCorrectUrl() {
        server.expect(requestTo(BASE + "/api/v1/Request/movie/available"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(
                        "{\"result\":true,\"message\":\"marked\",\"requestId\":7}", MediaType.APPLICATION_JSON));

        OmbiReprocessResponse response = client.markMovieAvailable(7);
        server.verify();

        assertNotNull(response);
        assertTrue(response.getResult());
        assertEquals(7, response.getRequestId());
    }

    // --- getTvRequests ---

    @Test
    void getTvRequestsReturnsList() {
        server.expect(requestTo(BASE + "/api/v1/Request/tv"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(
                        "[{\"id\":10,\"title\":\"Breaking Bad\",\"tvDbId\":81189}]", MediaType.APPLICATION_JSON));

        List<OmbiTvRequest> tvRequests = client.getTvRequests();
        server.verify();

        assertEquals(1, tvRequests.size());
        assertEquals("Breaking Bad", tvRequests.get(0).getTitle());
        assertEquals(81189, tvRequests.get(0).getTvDbId());
    }

    @Test
    void getTvRequestsReturnsEmptyList() {
        server.expect(requestTo(BASE + "/api/v1/Request/tv")).andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));

        List<OmbiTvRequest> tvRequests = client.getTvRequests();
        assertTrue(tvRequests.isEmpty());
    }

    // --- searchTv ---

    @Test
    void searchTvReturnsResult() {
        server.expect(requestTo(BASE + "/api/v2/search/Tv/81189"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("{\"id\":81189,\"title\":\"Breaking Bad\"}", MediaType.APPLICATION_JSON));

        OmbiTvSearchResult result = client.searchTv(81189);
        server.verify();

        assertNotNull(result);
    }

    // --- markTvAvailable ---

    @Test
    void markTvAvailablePostsToCorrectUrl() {
        server.expect(requestTo(BASE + "/api/v1/Request/tv/available"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(
                        "{\"result\":true,\"message\":\"marked\",\"requestId\":10}", MediaType.APPLICATION_JSON));

        OmbiReprocessResponse response = client.markTvAvailable(10);
        server.verify();

        assertNotNull(response);
        assertTrue(response.getResult());
    }
}
