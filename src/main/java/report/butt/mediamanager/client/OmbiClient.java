package report.butt.mediamanager.client;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import report.butt.mediamanager.model.ombi.OmbiMarkAvailableRequest;
import report.butt.mediamanager.model.ombi.OmbiMovieRequest;
import report.butt.mediamanager.model.ombi.OmbiReprocessResponse;

@Service
public class OmbiClient {

  private final RestClient restClient;

  public OmbiClient(
      RestClient.Builder builder,
      @Value("${ombi.url}") String ombiUrl,
      @Value("${ombi.api-key}") String ombiApiKey) {
    this.restClient = builder
        .baseUrl(ombiUrl)
        .defaultHeader("ApiKey", ombiApiKey)
        .build();
  }

  public List<OmbiMovieRequest> getMovies() {
    return restClient.get()
        .uri("/api/v1/Request/movie") // Path variable expansion
        .accept(MediaType.APPLICATION_JSON)
        .retrieve()
        .body(new ParameterizedTypeReference<List<OmbiMovieRequest>>() {
        });
  }

  public OmbiReprocessResponse reprocessMovieRequest(Integer ombiRequestId) {
    return restClient.post()
        .uri("/api/v2/Requests/reprocess/1/{ombiRequestId}/false", ombiRequestId)
        .accept(MediaType.APPLICATION_JSON)
        .retrieve()
        .body(OmbiReprocessResponse.class);
  }

  public OmbiReprocessResponse markMovieAvailable(Integer ombiRequestId) {
    return restClient.post()
        .uri("/api/v1/Request/movie/available")
        .contentType(MediaType.APPLICATION_JSON)
        .accept(MediaType.APPLICATION_JSON)
        .body(new OmbiMarkAvailableRequest(ombiRequestId))
        .retrieve()
        .body(OmbiReprocessResponse.class);
  }
}
