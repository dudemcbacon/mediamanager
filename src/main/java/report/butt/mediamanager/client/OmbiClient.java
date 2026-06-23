package report.butt.mediamanager.client;

import java.util.List;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import report.butt.mediamanager.model.ombi.OmbiMarkAvailableRequest;
import report.butt.mediamanager.model.ombi.OmbiMovieRequest;
import report.butt.mediamanager.model.ombi.OmbiReprocessResponse;
import report.butt.mediamanager.model.ombi.OmbiTvRequest;
import report.butt.mediamanager.model.ombi.OmbiTvSearchResult;

@Service
@NullMarked
public class OmbiClient {

    private final RestClient restClient;

    public OmbiClient(
            RestClient.Builder builder,
            @Value("${ombi.url}") String ombiUrl,
            @Value("${ombi.api-key}") String ombiApiKey) {
        this.restClient =
                builder.baseUrl(ombiUrl).defaultHeader("ApiKey", ombiApiKey).build();
    }

    public List<OmbiMovieRequest> getMovies() {
        return restClient
                .get()
                .uri("/api/v1/Request/movie") // Path variable expansion
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .body(new ParameterizedTypeReference<List<OmbiMovieRequest>>() {});
    }

    public OmbiReprocessResponse reprocessMovieRequest(Integer ombiRequestId) {
        return restClient
                .post()
                .uri("/api/v2/Requests/reprocess/1/{ombiRequestId}/false", ombiRequestId)
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .body(OmbiReprocessResponse.class);
    }

    public OmbiReprocessResponse markMovieAvailable(Integer ombiRequestId) {
        return restClient
                .post()
                .uri("/api/v1/Request/movie/available")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .body(new OmbiMarkAvailableRequest(ombiRequestId))
                .retrieve()
                .body(OmbiReprocessResponse.class);
    }

    public List<OmbiTvRequest> getTvRequests() {
        return restClient
                .get()
                .uri("/api/v1/Request/tv")
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .body(new ParameterizedTypeReference<List<OmbiTvRequest>>() {});
    }

    public @Nullable OmbiTvSearchResult searchTv(Integer externalProviderId) {
        return restClient
                .get()
                .uri("/api/v2/search/Tv/{externalProviderId}", externalProviderId)
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .body(OmbiTvSearchResult.class);
    }

    public OmbiReprocessResponse markTvAvailable(Integer ombiRequestId) {
        return restClient
                .post()
                .uri("/api/v1/Request/tv/available")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .body(new OmbiMarkAvailableRequest(ombiRequestId))
                .retrieve()
                .body(OmbiReprocessResponse.class);
    }
}
