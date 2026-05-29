package report.butt.mediamanager.client;

import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import report.butt.mediamanager.model.radarr.Movie;
import report.butt.mediamanager.model.radarr.RadarrCommand;

@Service
public class RadarrClient {

    private final RestClient restClient;

    public RadarrClient(
            RestClient.Builder builder,
            @Value("${radarr.url}") String radarrUrl,
            @Value("${radarr.api-key}") String radarrApiKey) {
        this.restClient = builder.baseUrl(radarrUrl)
                .defaultHeader("X-Api-Key", radarrApiKey)
                .build();
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
