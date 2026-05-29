package report.butt.mediamanager.client;

import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import report.butt.mediamanager.model.sonarr.Series;
import report.butt.mediamanager.model.sonarr.SeriesHistory;
import report.butt.mediamanager.model.sonarr.SonarrCommand;

@Service
public class SonarrClient {

    private final RestClient restClient;

    public SonarrClient(
            RestClient.Builder builder,
            @Value("${sonarr.url}") String sonarrUrl,
            @Value("${sonarr.api-key}") String sonarrApiKey) {
        this.restClient = builder.baseUrl(sonarrUrl)
                .defaultHeader("X-Api-Key", sonarrApiKey)
                .build();
    }

    public Series getSeriesById(Long id) {
        return restClient.get().uri("/api/v3/series/{id}", id).retrieve().body(Series.class);
    }

    public List<Series> getAllSeries() {
        return restClient
                .get()
                .uri("/api/v3/series")
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .body(new ParameterizedTypeReference<List<Series>>() {});
    }

    public List<SeriesHistory> getSeriesHistory(Integer sonarrSeriesId) {
        return restClient
                .get()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/v3/history/series")
                        .queryParam("seriesId", sonarrSeriesId)
                        .queryParam("includeSeries", false)
                        .queryParam("includeEpisode", false)
                        .build())
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .body(new ParameterizedTypeReference<List<SeriesHistory>>() {});
    }

    public SonarrCommand searchSeries(List<Integer> seriesIds) {
        return restClient
                .post()
                .uri("/api/v3/command")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .body(Map.of("name", "SeriesSearch", "seriesIds", seriesIds))
                .retrieve()
                .body(SonarrCommand.class);
    }

    public List<Series> getSeriesByTvdbId(Integer tvdbId) {
        return restClient
                .get()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/v3/series")
                        .queryParam("tvdbId", tvdbId)
                        .build())
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .body(new ParameterizedTypeReference<List<Series>>() {});
    }
}
