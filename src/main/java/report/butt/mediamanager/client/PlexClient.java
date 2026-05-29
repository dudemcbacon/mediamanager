package report.butt.mediamanager.client;

import jakarta.annotation.PostConstruct;
import java.net.URI;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;
import report.butt.mediamanager.model.plex.PlexDirectory;
import report.butt.mediamanager.model.plex.PlexMetadata;
import report.butt.mediamanager.model.plex.PlexSearchResponse;

@Service
public class PlexClient {

    private static final Logger log = LoggerFactory.getLogger(PlexClient.class);

    private final RestClient restClient;
    private final String plexToken;
    private final String plexUrl;
    private final String plexTvSectionName;
    private String machineIdentifier;
    private String tvSectionId;

    public PlexClient(
            RestClient.Builder builder,
            @Value("${plex.url}") String plexUrl,
            @Value("${plex.token}") String plexToken,
            @Value("${plex.tv-section-name:TV Shows}") String plexTvSectionName) {
        this.plexUrl = plexUrl;
        this.plexToken = plexToken;
        this.plexTvSectionName = plexTvSectionName;
        this.restClient = builder.baseUrl(plexUrl)
                .defaultHeader("accept", "application/json")
                .build();
    }

    @PostConstruct
    void cacheMachineIdentifier() {
        if (plexUrl == null || plexUrl.isBlank()) {
            throw new IllegalStateException("plex.url is not configured");
        }
        URI uri = UriComponentsBuilder.fromUriString(this.plexUrl)
                .queryParam("X-Plex-Token", plexToken)
                .encode()
                .build()
                .toUri();
        PlexSearchResponse response = restClient
                .get()
                .uri(uri)
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .body(PlexSearchResponse.class);
        if (response == null || response.getMediaContainer() == null) {
            throw new IllegalStateException("Plex identity response was empty; cannot cache machineIdentifier");
        }
        this.machineIdentifier = response.getMediaContainer().getMachineIdentifier();
        log.info("Cached Plex machineIdentifier={}", this.machineIdentifier);
    }

    @PostConstruct
    void cacheTvSectionId() {
        if (plexUrl == null || plexUrl.isBlank()) {
            throw new IllegalStateException("plex.url is not configured");
        }
        URI uri = UriComponentsBuilder.fromUriString(this.plexUrl)
                .path("/library/sections")
                .queryParam("X-Plex-Token", plexToken)
                .encode()
                .build()
                .toUri();
        PlexSearchResponse response = restClient
                .get()
                .uri(uri)
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .body(PlexSearchResponse.class);
        if (response == null
                || response.getMediaContainer() == null
                || response.getMediaContainer().getDirectory() == null) {
            throw new IllegalStateException("Plex sections response was empty; cannot cache tvSectionId");
        }
        for (PlexDirectory dir : response.getMediaContainer().getDirectory()) {
            if (plexTvSectionName.equals(dir.getTitle())) {
                this.tvSectionId = dir.getKey();
                log.info("Cached Plex tvSectionId={} for section '{}'", this.tvSectionId, plexTvSectionName);
                return;
            }
        }
        throw new IllegalStateException("No Plex section found matching name '" + plexTvSectionName + "'");
    }

    public String getMachineIdentifier() {
        return machineIdentifier;
    }

    public String getTvSectionId() {
        return tvSectionId;
    }

    public String getPlexToken() {
        return plexToken;
    }

    public String getPlexUrl() {
        return plexUrl;
    }

    public String getPlexTvSectionName() {
        return plexTvSectionName;
    }

    public MetadataResult getMovieByTmdbId(int tmdbId, String title, int year) {
        log.info("Retrieving media from Plex via tmdbId={}, title={}, year={}", tmdbId, title, year);
        String tmdbGuid = "tmdb://" + tmdbId;

        URI uri = UriComponentsBuilder.fromUriString(this.plexUrl)
                .path("/library/all")
                .queryParam("type", 1)
                // .queryParam("year", year)
                .queryParam("includeGuids", 1)
                .queryParam("title", title)
                .queryParam("X-Plex-Token", plexToken)
                .encode()
                .build()
                .toUri();

        PlexSearchResponse response = restClient
                .get()
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

    public MetadataResult getShowByTvdbId(int tvdbId, String title, int year) {
        log.info("Retrieving show from Plex via tvdbId={}, title={}, year={}", tvdbId, title, year);
        String tvdbGuid = "tvdb://" + tvdbId;

        if (tvSectionId == null) {
            log.warn("Plex tvSectionId is not cached; cannot search for show '{}'", title);
            return new MetadataResult(null, null);
        }

        URI uri = UriComponentsBuilder.fromUriString(this.plexUrl)
                .path("/library/sections/" + tvSectionId + "/all")
                .queryParam("includeGuids", 1)
                .queryParam("title", title)
                .queryParam("X-Plex-Token", plexToken)
                .encode()
                .build()
                .toUri();

        PlexSearchResponse response = restClient
                .get()
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
                .filter(m -> hasTvdbGuid(m, tvdbGuid))
                .findFirst()
                .orElse(results.get(0));

        return new MetadataResult(uri.toString(), metadata);
    }

    private static boolean hasTvdbGuid(PlexMetadata metadata, String tvdbGuid) {
        if (metadata.getGuids() == null) {
            return false;
        }
        return metadata.getGuids().stream().anyMatch(g -> tvdbGuid.equals(g.getId()));
    }

    public List<PlexMetadata> getShowGrandchildren(String plexMetadataId) {
        log.info("Retrieving Plex grandchildren for plexMetadataId={}", plexMetadataId);

        URI uri = UriComponentsBuilder.fromUriString(this.plexUrl)
                .path("/library/metadata/" + plexMetadataId + "/grandchildren")
                .queryParam("includeGuids", 1)
                .queryParam("includeDetails", 1)
                .queryParam("includeChildren", 1)
                .queryParam("X-Plex-Token", plexToken)
                .encode()
                .build()
                .toUri();

        PlexSearchResponse response = restClient
                .get()
                .uri(uri)
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .body(PlexSearchResponse.class);

        if (response == null || response.getMediaContainer() == null) {
            return List.of();
        }
        List<PlexMetadata> results = response.getMediaContainer().getMetadata();
        return results == null ? List.of() : results;
    }
}
