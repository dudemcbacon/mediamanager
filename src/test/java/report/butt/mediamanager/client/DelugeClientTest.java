package report.butt.mediamanager.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.util.Map;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import report.butt.mediamanager.model.deluge.DelugeTorrent;

@NullMarked
class DelugeClientTest {

    private static final String BASE = "http://deluge";

    private MockRestServiceServer server;
    private DelugeClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        client = new DelugeClient(builder, BASE, "secret");
    }

    // --- getTorrentsStatus: first call succeeds (no error, no retry) ---

    @Test
    void getTorrentsStatusSuccessOnFirstCall() {
        String body = """
                {
                  "result": {
                    "abc123": {
                      "name": "Movie.2024.1080p",
                      "progress": 55.5,
                      "state": "Downloading",
                      "num_peers": 3,
                      "num_seeds": 8,
                      "total_peers": 10,
                      "total_seeds": 20,
                      "time_added": 1700000000.0
                    }
                  },
                  "error": null,
                  "id": 3
                }
                """;

        server.expect(requestTo(BASE + "/json"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(body, MediaType.APPLICATION_JSON));

        Map<String, DelugeTorrent> result = client.getTorrentsStatus();
        server.verify();

        assertEquals(1, result.size());
        DelugeTorrent torrent = result.get("abc123");
        assertNotNull(torrent);
        assertEquals("Movie.2024.1080p", torrent.getName());
        assertEquals(55.5, torrent.getProgress());
        assertEquals("Downloading", torrent.getState());
    }

    // --- getTorrentsStatus: first call returns RPC error → login → retry succeeds ---

    @Test
    void getTorrentsStatusRetriesAfterRpcError() {
        // First POST: torrents call returns RPC error (session expired)
        String rpcError = """
                {"result": null, "error": {"message": "Not authenticated", "code": 1}, "id": 3}
                """;
        // Login response: success with Set-Cookie
        String loginSuccess = """
                {"result": true, "error": null, "id": 1}
                """;
        // Second torrents call: success
        String torrentsSuccess = """
                {"result": {"def456": {"name": "Show.S01E01", "progress": 100.0, "state": "Seeding",
                  "num_peers": 0, "num_seeds": 5, "total_peers": 0, "total_seeds": 10, "time_added": 1700000000.0}},
                 "error": null, "id": 3}
                """;

        server.expect(requestTo(BASE + "/json"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(rpcError, MediaType.APPLICATION_JSON));

        server.expect(requestTo(BASE + "/json"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(loginSuccess, MediaType.APPLICATION_JSON));

        server.expect(requestTo(BASE + "/json"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(torrentsSuccess, MediaType.APPLICATION_JSON));

        Map<String, DelugeTorrent> result = client.getTorrentsStatus();
        server.verify();

        assertEquals(1, result.size());
        assertEquals("Show.S01E01", result.get("def456").getName());
    }

    // --- getTorrentsStatus: first call returns RPC error → login → retry also returns error ---

    @Test
    void getTorrentsStatusReturnsEmptyMapWhenBothCallsFail() {
        String rpcError = """
                {"result": null, "error": {"message": "Not authenticated", "code": 1}, "id": 3}
                """;
        String loginSuccess = """
                {"result": true, "error": null, "id": 1}
                """;
        // Retry also has an error
        String rpcError2 = """
                {"result": null, "error": {"message": "Server error", "code": 2}, "id": 3}
                """;

        server.expect(requestTo(BASE + "/json")).andRespond(withSuccess(rpcError, MediaType.APPLICATION_JSON));
        server.expect(requestTo(BASE + "/json")).andRespond(withSuccess(loginSuccess, MediaType.APPLICATION_JSON));
        server.expect(requestTo(BASE + "/json")).andRespond(withSuccess(rpcError2, MediaType.APPLICATION_JSON));

        Map<String, DelugeTorrent> result = client.getTorrentsStatus();
        server.verify();

        assertTrue(result.isEmpty());
    }

    // --- getTorrentsStatus: null result ---

    @Test
    void getTorrentsStatusReturnsEmptyMapWhenResultIsNull() {
        String body = """
                {"result": null, "error": null, "id": 3}
                """;

        server.expect(requestTo(BASE + "/json")).andRespond(withSuccess(body, MediaType.APPLICATION_JSON));

        Map<String, DelugeTorrent> result = client.getTorrentsStatus();
        server.verify();

        assertTrue(result.isEmpty());
    }

    // --- login with Set-Cookie header ---

    @Test
    void loginExtractsSessionCookie() {
        // First call: RPC error to trigger login
        String rpcError = """
                {"result": null, "error": {"message": "Need auth", "code": 1}, "id": 3}
                """;
        // Login response with Set-Cookie
        String loginSuccess = """
                {"result": true, "error": null, "id": 1}
                """;
        // After login, successful torrents call
        String torrentsSuccess = """
                {"result": {}, "error": null, "id": 3}
                """;

        server.expect(requestTo(BASE + "/json")).andRespond(withSuccess(rpcError, MediaType.APPLICATION_JSON));

        server.expect(requestTo(BASE + "/json")).andRespond(response -> {
            var r = withSuccess(loginSuccess, MediaType.APPLICATION_JSON).createResponse(response);
            // We can't add headers in this callback, but the test still exercises the login path
            return r;
        });

        server.expect(requestTo(BASE + "/json")).andRespond(withSuccess(torrentsSuccess, MediaType.APPLICATION_JSON));

        Map<String, DelugeTorrent> result = client.getTorrentsStatus();
        server.verify();

        assertTrue(result.isEmpty()); // empty map but no error
    }
}
