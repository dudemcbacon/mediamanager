package report.butt.mediamanager.client;

import jakarta.annotation.PostConstruct;
import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;
import report.butt.mediamanager.model.plex.EpisodeKey;
import report.butt.mediamanager.model.plex.PlexDirectory;
import report.butt.mediamanager.model.plex.PlexGuid;
import report.butt.mediamanager.model.plex.PlexMedia;
import report.butt.mediamanager.model.plex.PlexMediaContainer;
import report.butt.mediamanager.model.plex.PlexMetadata;
import report.butt.mediamanager.model.plex.PlexPart;
import report.butt.mediamanager.model.plex.PlexSearchResponse;
import report.butt.mediamanager.service.PlexCacheService;
import tools.jackson.databind.ObjectMapper;

@Service
public class PlexClient {

    private static final Logger log = LoggerFactory.getLogger(PlexClient.class);

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final PlexCacheService plexCacheService;
    private final String plexToken;
    private final String plexUrl;
    private final String plexTvSectionName;
    private final String plexMoviesSectionName;
    private String machineIdentifier;
    private String tvSectionId;
    private String moviesSectionId;

    public PlexClient(
            RestClient.Builder builder,
            ObjectMapper objectMapper,
            PlexCacheService plexCacheService,
            @Value("${plex.url}") String plexUrl,
            @Value("${plex.token}") String plexToken,
            @Value("${plex.tv-section-name:TV Shows}") String plexTvSectionName,
            @Value("${plex.movies-section-name:Movies}") String plexMoviesSectionName) {
        this.objectMapper = objectMapper;
        this.plexCacheService = plexCacheService;
        this.plexUrl = plexUrl;
        this.plexToken = plexToken;
        this.plexTvSectionName = plexTvSectionName;
        this.plexMoviesSectionName = plexMoviesSectionName;
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

    @PostConstruct
    void cacheMoviesSectionId() {
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
            throw new IllegalStateException("Plex sections response was empty; cannot cache moviesSectionId");
        }
        for (PlexDirectory dir : response.getMediaContainer().getDirectory()) {
            if (plexMoviesSectionName.equals(dir.getTitle())) {
                this.moviesSectionId = dir.getKey();
                log.info(
                        "Cached Plex moviesSectionId={} for section '{}'",
                        this.moviesSectionId,
                        plexMoviesSectionName);
                return;
            }
        }
        throw new IllegalStateException("No Plex section found matching name '" + plexMoviesSectionName + "'");
    }

    public String getMachineIdentifier() {
        return machineIdentifier;
    }

    public String getTvSectionId() {
        return tvSectionId;
    }

    public String getMoviesSectionId() {
        return moviesSectionId;
    }

    public String getPlexUrl() {
        return plexUrl;
    }

    public String getPlexTvSectionName() {
        return plexTvSectionName;
    }

    /** The per-item Plex query URL used to look up a movie, without the X-Plex-Token. */
    public String movieQueryUrl(String title) {
        return UriComponentsBuilder.fromUriString(this.plexUrl)
                .path("/library/all")
                .queryParam("type", 1)
                .queryParam("includeGuids", 1)
                .queryParam("title", title)
                .encode()
                .build()
                .toUriString();
    }

    /** The per-item Plex query URL used to look up a show, without the X-Plex-Token. */
    public String showQueryUrl(String title) {
        if (tvSectionId == null) {
            return null;
        }
        return UriComponentsBuilder.fromUriString(this.plexUrl)
                .path("/library/sections/" + tvSectionId + "/all")
                .queryParam("includeGuids", 1)
                .queryParam("title", title)
                .encode()
                .build()
                .toUriString();
    }

    public MetadataResult getMovieByTmdbId(int tmdbId, String title, int year) {
        log.info("Retrieving media from Plex via tmdbId={}, title={}, year={}", tmdbId, title, year);
        String tmdbGuid = "tmdb://" + tmdbId;

        URI uri = UriComponentsBuilder.fromUriString(this.plexUrl)
                .path("/library/all")
                .queryParam("type", 1)
                .queryParam("includeGuids", 1)
                .queryParam("title", title)
                .queryParam("X-Plex-Token", plexToken)
                .encode()
                .build()
                .toUri();

        String body = restClient
                .get()
                .uri(uri)
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .body(String.class);

        String cacheUrl = plexCacheService.store(movieCacheKey(tmdbId), body == null ? "" : body);
        PlexSearchResponse response = parse(body);

        if (response == null || response.getMediaContainer() == null) {
            return new MetadataResult(cacheUrl, null);
        }

        List<PlexMetadata> results = response.getMediaContainer().getMetadata();
        if (results == null || results.isEmpty()) {
            return new MetadataResult(cacheUrl, null);
        }

        PlexMetadata metadata = results.stream()
                .filter(m -> hasTmdbGuid(m, tmdbGuid))
                .findFirst()
                .orElse(results.get(0));

        return new MetadataResult(cacheUrl, metadata);
    }

    private static boolean hasTmdbGuid(PlexMetadata metadata, String tmdbGuid) {
        if (metadata.getGuids() == null) {
            return false;
        }
        return metadata.getGuids().stream().anyMatch(g -> tmdbGuid.equals(g.getId()));
    }

    public Map<Integer, PlexMetadata> getAllMoviesIndexedByTmdb() {
        if (moviesSectionId == null) {
            log.warn("Plex moviesSectionId is not cached; cannot bulk-fetch movies");
            return Map.of();
        }

        log.info("Bulk-fetching all Plex movies from sectionId={}", moviesSectionId);
        URI uri = UriComponentsBuilder.fromUriString(this.plexUrl)
                .path("/library/sections/" + moviesSectionId + "/all")
                .queryParam("type", 1)
                .queryParam("includeGuids", 1)
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
            return Map.of();
        }
        List<PlexMetadata> results = response.getMediaContainer().getMetadata();
        if (results == null || results.isEmpty()) {
            return Map.of();
        }

        Map<Integer, PlexMetadata> indexed = new HashMap<>(results.size());
        for (PlexMetadata m : results) {
            if (m.getGuids() == null) {
                continue;
            }
            for (PlexGuid guid : m.getGuids()) {
                String id = guid.getId();
                if (id == null || !id.startsWith("tmdb://")) {
                    continue;
                }
                try {
                    int tmdbId = Integer.parseInt(id.substring("tmdb://".length()));
                    indexed.putIfAbsent(tmdbId, m);
                } catch (NumberFormatException e) {
                    log.debug("Skipping non-numeric tmdb guid '{}' on Plex item {}", id, m.getRatingKey());
                }
            }
        }
        log.info("Indexed {} Plex movies by tmdbId", indexed.size());
        return indexed;
    }

    public String cacheMovieMetadata(int tmdbId, PlexMetadata metadata) {
        PlexMediaContainer container = new PlexMediaContainer();
        container.setMetadata(metadata == null ? List.of() : List.of(metadata));
        PlexSearchResponse wrapper = new PlexSearchResponse();
        wrapper.setMediaContainer(container);
        String body = objectMapper.writeValueAsString(wrapper);
        return plexCacheService.store(movieCacheKey(tmdbId), body);
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

        String body = restClient
                .get()
                .uri(uri)
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .body(String.class);

        String cacheUrl = plexCacheService.store(tvCacheKey(tvdbId), body == null ? "" : body);
        PlexSearchResponse response = parse(body);

        if (response == null || response.getMediaContainer() == null) {
            return new MetadataResult(cacheUrl, null);
        }

        List<PlexMetadata> results = response.getMediaContainer().getMetadata();
        if (results == null || results.isEmpty()) {
            return new MetadataResult(cacheUrl, null);
        }

        PlexMetadata metadata = results.stream()
                .filter(m -> hasTvdbGuid(m, tvdbGuid))
                .findFirst()
                .orElse(results.get(0));

        return new MetadataResult(cacheUrl, metadata);
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

    public Map<Integer, PlexMetadata> getAllShowsIndexedByTvdb() {
        if (tvSectionId == null) {
            log.warn("Plex tvSectionId is not cached; cannot bulk-fetch shows");
            return Map.of();
        }

        log.info("Bulk-fetching all Plex shows from sectionId={}", tvSectionId);
        URI uri = UriComponentsBuilder.fromUriString(this.plexUrl)
                .path("/library/sections/" + tvSectionId + "/all")
                .queryParam("includeGuids", 1)
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
            return Map.of();
        }
        List<PlexMetadata> results = response.getMediaContainer().getMetadata();
        if (results == null || results.isEmpty()) {
            return Map.of();
        }

        Map<Integer, PlexMetadata> indexed = new HashMap<>(results.size());
        for (PlexMetadata m : results) {
            if (m.getGuids() == null) {
                continue;
            }
            for (PlexGuid guid : m.getGuids()) {
                String id = guid.getId();
                if (id == null || !id.startsWith("tvdb://")) {
                    continue;
                }
                try {
                    int tvdbId = Integer.parseInt(id.substring("tvdb://".length()));
                    indexed.putIfAbsent(tvdbId, m);
                } catch (NumberFormatException e) {
                    log.debug("Skipping non-numeric tvdb guid '{}' on Plex item {}", id, m.getRatingKey());
                }
            }
        }
        log.info("Indexed {} Plex shows by tvdbId", indexed.size());
        return indexed;
    }

    public Map<String, Map<EpisodeKey, String>> getAllEpisodesIndexedByShow() {
        if (tvSectionId == null) {
            log.warn("Plex tvSectionId is not cached; cannot bulk-fetch episodes");
            return Map.of();
        }

        log.info("Bulk-fetching all Plex episodes from sectionId={}", tvSectionId);
        URI uri = UriComponentsBuilder.fromUriString(this.plexUrl)
                .path("/library/sections/" + tvSectionId + "/all")
                .queryParam("type", 4)
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
            return Map.of();
        }
        List<PlexMetadata> results = response.getMediaContainer().getMetadata();
        if (results == null || results.isEmpty()) {
            return Map.of();
        }

        Map<String, Map<EpisodeKey, String>> indexed = new HashMap<>();
        for (PlexMetadata episode : results) {
            String showRatingKey = episode.getGrandparentRatingKey();
            Integer seasonNumber = episode.getParentIndex();
            Integer episodeNumber = episode.getIndex();
            if (showRatingKey == null || seasonNumber == null || episodeNumber == null) {
                continue;
            }
            String file = firstFile(episode);
            if (file == null) {
                continue;
            }
            indexed.computeIfAbsent(showRatingKey, k -> new HashMap<>())
                    .putIfAbsent(new EpisodeKey(seasonNumber, episodeNumber), file);
        }
        log.info("Indexed Plex episodes for {} shows", indexed.size());
        return indexed;
    }

    public String cacheTvMetadata(int tvdbId, PlexMetadata metadata) {
        PlexMediaContainer container = new PlexMediaContainer();
        container.setMetadata(metadata == null ? List.of() : List.of(metadata));
        PlexSearchResponse wrapper = new PlexSearchResponse();
        wrapper.setMediaContainer(container);
        String body = objectMapper.writeValueAsString(wrapper);
        return plexCacheService.store(tvCacheKey(tvdbId), body);
    }

    private static String firstFile(PlexMetadata episode) {
        if (episode.getMedia() == null || episode.getMedia().isEmpty()) {
            return null;
        }
        PlexMedia media = episode.getMedia().get(0);
        if (media.getPart() == null || media.getPart().isEmpty()) {
            return null;
        }
        return media.getPart().get(0).getFile();
    }

    public static String movieCacheKey(int tmdbId) {
        return "movie-" + tmdbId;
    }

    public static String tvCacheKey(int tvdbId) {
        return "tv-" + tvdbId;
    }

    private PlexSearchResponse parse(String body) {
        if (body == null || body.isBlank()) {
            return null;
        }
        return objectMapper.readValue(body, PlexSearchResponse.class);
    }
}
