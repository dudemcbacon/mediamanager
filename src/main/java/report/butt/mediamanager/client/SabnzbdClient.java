package report.butt.mediamanager.client;

import java.util.HashMap;
import java.util.Map;
import org.jspecify.annotations.NullMarked;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import report.butt.mediamanager.model.sabnzbd.SabnzbdResponse;
import report.butt.mediamanager.model.sabnzbd.SabnzbdSlot;

/**
 * Talks to SABnzbd's HTTP API ({@code GET /api?mode=queue}). The queue's slots are the active usenet downloads;
 * Radarr's queue record downloadId is the slot's {@code nzo_id}, which is how a movie is matched to its SABnzbd
 * progress (mirrors {@link DelugeClient} for torrents).
 */
@Service
@NullMarked
public class SabnzbdClient {

    private static final Logger log = LoggerFactory.getLogger(SabnzbdClient.class);

    private final RestClient restClient;
    private final String apiKey;

    public SabnzbdClient(
            RestClient.Builder builder,
            @Value("${sabnzbd.url}") String sabnzbdUrl,
            @Value("${sabnzbd.api-key}") String sabnzbdApiKey) {
        this.apiKey = sabnzbdApiKey;
        this.restClient = builder.baseUrl(sabnzbdUrl).build();
    }

    /** Returns the current queue slots keyed by {@code nzo_id}, or an empty map if the call fails. */
    public Map<String, SabnzbdSlot> getQueueSlots() {
        try {
            SabnzbdResponse response = restClient
                    .get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/api")
                            .queryParam("mode", "queue")
                            .queryParam("output", "json")
                            .queryParam("apikey", apiKey)
                            .build())
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .body(SabnzbdResponse.class);
            if (response == null
                    || response.getQueue() == null
                    || response.getQueue().getSlots() == null) {
                return Map.of();
            }
            Map<String, SabnzbdSlot> byNzoId = new HashMap<>();
            for (SabnzbdSlot slot : response.getQueue().getSlots()) {
                if (slot.getNzoId() != null) {
                    byNzoId.put(slot.getNzoId(), slot);
                }
            }
            return byNzoId;
        } catch (RuntimeException e) {
            log.warn("Failed to fetch SABnzbd queue", e);
            return Map.of();
        }
    }
}
