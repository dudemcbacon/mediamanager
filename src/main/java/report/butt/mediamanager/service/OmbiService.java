package report.butt.mediamanager.service;

import java.util.List;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import report.butt.mediamanager.model.OmbiMovieRequest;

@Service
public class OmbiService {

  private final RestClient restClient;

  public OmbiService(RestClient.Builder builder) {
    this.restClient = builder
        .baseUrl("http://10.0.10.41:3579")
        .defaultHeader("ApiKey", "")
        .build();
  }

  public OmbiMovieRequest getMoviesById(Long id) {
    return restClient.get()
        .uri("/api/v1/Request/movie/{id}", id) // Path variable expansion
        .retrieve()
        .body(OmbiMovieRequest.class); // Deserializes JSON to a Java object
  }

  public List<OmbiMovieRequest> getMovies() {
    return restClient.get()
        .uri("/api/v1/Request/movie") // Path variable expansion
        .accept(MediaType.APPLICATION_JSON)
        .retrieve()
        .body(new ParameterizedTypeReference<List<OmbiMovieRequest>>() {
        });
  }
}
