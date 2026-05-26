package report.butt.mediamanager.client;

import java.net.URI;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import report.butt.mediamanager.model.plex.PlexMetadata;
import report.butt.mediamanager.model.plex.PlexSearchResponse;

@Service
public class PlexClient {

    private static final Logger log = LoggerFactory.getLogger(PlexClient.class);

    private final RestClient restClient;
    private final String plexToken;
    private final String plexUrl;

    public PlexClient(
            RestClient.Builder builder,
            @Value("${plex.url}") String plexUrl,
            @Value("${plex.token}") String plexToken) {
        this.plexUrl = plexUrl;
        this.plexToken = plexToken;
        this.restClient = builder
                .baseUrl(plexUrl)
                .defaultHeader("accept", "application/json")
                .build();
    }

    public MetadataResult getMovieByTmdbId(int tmdbId, String title, int year) {
        log.info("Retrieving media from Plex via tmdbId={}, title={}, year={}", tmdbId, title, year);
        String tmdbGuid = "tmdb://" + tmdbId;

        URI uri = UriComponentsBuilder
                .fromUriString(this.plexUrl)
                .path("/library/all")
                .queryParam("type", 1)
                // .queryParam("year", year)
                .queryParam("includeGuids", 1)
                .queryParam("title", title)
                .queryParam("X-Plex-Token", plexToken)
                .encode()
                .build()
                .toUri();

        PlexSearchResponse response = restClient.get()
                .uri(uri)
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .body(PlexSearchResponse.class);

        if (response == null || response.getMediaContainer() == null) {
            return new MetadataResult(uri.toString(), null);
        }

        List<PlexMetadata> results = response.getMediaContainer().getMetadata();
        if (results == null || results.isEmpty()) {
            return new MetadataResult(uri.toString(), null);
        }

        PlexMetadata metadata = results.stream()
                .filter(m -> hasTmdbGuid(m, tmdbGuid))
                .findFirst()
                .orElse(results.get(0));

        return new MetadataResult(uri.toString(), metadata);
    }

    private static boolean hasTmdbGuid(PlexMetadata metadata, String tmdbGuid) {
        if (metadata.getGuids() == null) {
            return false;
        }
        return metadata.getGuids().stream().anyMatch(g -> tmdbGuid.equals(g.getId()));
    }
}
