package report.butt.mediamanager.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import report.butt.mediamanager.model.sabnzbd.SabnzbdSlot;

class SabnzbdClientTest {

    private static final String BASE = "http://sabnzbd";
    private static final String API_KEY = "sabnzbd-key";

    private MockRestServiceServer server;
    private SabnzbdClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        client = new SabnzbdClient(builder, BASE, API_KEY);
    }

    // --- getQueueSlots: success with slots ---

    @Test
    void getQueueSlotsReturnsSlotsKeyedByNzoId() {
        String body = """
                {
                  "queue": {
                    "slots": [
                      {"nzo_id": "SABnzbd_nzo_abc123", "status": "Downloading", "percentage": "42"},
                      {"nzo_id": "SABnzbd_nzo_def456", "status": "Paused", "percentage": "10"}
                    ]
                  }
                }
                """;

        server.expect(requestTo(BASE + "/api?mode=queue&output=json&apikey=" + API_KEY))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(body, MediaType.APPLICATION_JSON));

        Map<String, SabnzbdSlot> slots = client.getQueueSlots();
        server.verify();

        assertEquals(2, slots.size());

        SabnzbdSlot first = slots.get("SABnzbd_nzo_abc123");
        assertNotNull(first);
        assertEquals("Downloading", first.getStatus());
        assertEquals("42", first.getPercentage());

        SabnzbdSlot second = slots.get("SABnzbd_nzo_def456");
        assertNotNull(second);
        assertEquals("Paused", second.getStatus());
    }

    // --- getQueueSlots: empty slots list ---

    @Test
    void getQueueSlotsReturnsEmptyMapWhenNoSlots() {
        String body = "{\"queue\": {\"slots\": []}}";

        server.expect(requestTo(BASE + "/api?mode=queue&output=json&apikey=" + API_KEY))
                .andRespond(withSuccess(body, MediaType.APPLICATION_JSON));

        Map<String, SabnzbdSlot> slots = client.getQueueSlots();
        server.verify();

        assertTrue(slots.isEmpty());
    }

    // --- getQueueSlots: null queue ---

    @Test
    void getQueueSlotsReturnsEmptyMapWhenQueueIsNull() {
        String body = "{}";

        server.expect(requestTo(BASE + "/api?mode=queue&output=json&apikey=" + API_KEY))
                .andRespond(withSuccess(body, MediaType.APPLICATION_JSON));

        Map<String, SabnzbdSlot> slots = client.getQueueSlots();
        server.verify();

        assertTrue(slots.isEmpty());
    }

    // --- getQueueSlots: slot with null nzo_id is skipped ---

    @Test
    void getQueueSlotsSkipsSlotWithNullNzoId() {
        String body = """
                {
                  "queue": {
                    "slots": [
                      {"nzo_id": null, "status": "Downloading", "percentage": "20"},
                      {"nzo_id": "SABnzbd_nzo_valid", "status": "Downloading", "percentage": "50"}
                    ]
                  }
                }
                """;

        server.expect(requestTo(BASE + "/api?mode=queue&output=json&apikey=" + API_KEY))
                .andRespond(withSuccess(body, MediaType.APPLICATION_JSON));

        Map<String, SabnzbdSlot> slots = client.getQueueSlots();
        server.verify();

        assertEquals(1, slots.size());
        assertTrue(slots.containsKey("SABnzbd_nzo_valid"));
    }

    // --- getQueueSlots: server error → empty map (caught by catch block) ---

    @Test
    void getQueueSlotsReturnsEmptyMapOnServerError() {
        server.expect(requestTo(BASE + "/api?mode=queue&output=json&apikey=" + API_KEY))
                .andRespond(withServerError());

        Map<String, SabnzbdSlot> slots = client.getQueueSlots();
        server.verify();

        assertTrue(slots.isEmpty());
    }
}
