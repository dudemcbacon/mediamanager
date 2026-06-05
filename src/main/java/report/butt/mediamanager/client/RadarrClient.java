package report.butt.mediamanager.client;

import jakarta.annotation.PostConstruct;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import report.butt.mediamanager.model.radarr.Movie;
import report.butt.mediamanager.model.radarr.QualityProfile;
import report.butt.mediamanager.model.radarr.RadarrCommand;
import report.butt.mediamanager.model.radarr.RadarrHealthItem;
import report.butt.mediamanager.model.radarr.RadarrQueue;

@Service
public class RadarrClient {

    private static final Logger log = LoggerFactory.getLogger(RadarrClient.class);

    private final RestClient restClient;
    private Map<Integer, String> qualityProfilesById = Map.of();

    public RadarrClient(
            RestClient.Builder builder,
            @Value("${radarr.url}") String radarrUrl,
            @Value("${radarr.api-key}") String radarrApiKey) {
        this.restClient = builder.baseUrl(radarrUrl)
                .defaultHeader("X-Api-Key", radarrApiKey)
                .build();
    }

    /**
     * Quality profiles rarely change, so we fetch them once at startup and reuse the
     * id-to-name map on every refresh instead of re-fetching. A failure here is non-fatal:
     * profile names simply stay unavailable until the next restart.
     */
    @PostConstruct
    void cacheQualityProfiles() {
        try {
            List<QualityProfile> profiles = restClient
                    .get()
                    .uri("/api/v3/qualityprofile")
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .body(new ParameterizedTypeReference<List<QualityProfile>>() {});
            if (profiles != null) {
                this.qualityProfilesById = profiles.stream()
                        .filter(p -> p.getId() != null)
                        .collect(Collectors.toMap(QualityProfile::getId, QualityProfile::getName, (a, b) -> a));
            }
            log.info("Cached {} Radarr quality profiles", qualityProfilesById.size());
        } catch (Exception e) {
            log.warn("Failed to cache Radarr quality profiles; names will be unavailable", e);
        }
    }

    public Map<Integer, String> getQualityProfilesById() {
        return qualityProfilesById;
    }

    /** Resolves a cached quality profile id by its (exact) name, or null if none matches. */
    public Integer getQualityProfileIdByName(String name) {
        return qualityProfilesById.entrySet().stream()
                .filter(e -> e.getValue() != null && e.getValue().equals(name))
                .map(Map.Entry::getKey)
                .findFirst()
                .orElse(null);
    }

    /** Changes a movie's quality profile via Radarr's bulk editor (avoids round-tripping the full movie). */
    public void updateMovieQualityProfile(Integer movieId, Integer qualityProfileId) {
        restClient
                .put()
                .uri("/api/v3/movie/editor")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("movieIds", List.of(movieId), "qualityProfileId", qualityProfileId))
                .retrieve()
                .toBodilessEntity();
    }

    public Movie getMovieById(Long id) {
        return restClient.get().uri("/api/v3/movie/{id}", id).retrieve().body(Movie.class);
    }

    public List<Movie> getMovies() {
        return restClient
                .get()
                .uri("/api/v3/movie")
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .body(new ParameterizedTypeReference<List<Movie>>() {});
    }

    public RadarrCommand searchMovies(List<Integer> movieIds) {
        return restClient
                .post()
                .uri("/api/v3/command")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .body(Map.of("name", "MoviesSearch", "movieIds", movieIds))
                .retrieve()
                .body(RadarrCommand.class);
    }

    public RadarrQueue getQueue() {
        return restClient
                .get()
                .uri(uriBuilder ->
                        uriBuilder.path("/api/v3/queue").queryParam("pageSize", 10000).build())
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .body(RadarrQueue.class);
    }

    public List<RadarrHealthItem> getHealth() {
        return restClient
                .get()
                .uri("/api/v3/health")
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .body(new ParameterizedTypeReference<List<RadarrHealthItem>>() {});
    }

    public List<Movie> getMoviesByTmdbId(Integer tmdbId) {
        return restClient
                .get()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/v3/movie")
                        .queryParam("tmdbId", tmdbId)
                        .queryParam("excludeLocalCovers", false)
                        .build())
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .body(new ParameterizedTypeReference<List<Movie>>() {});
    }
}
