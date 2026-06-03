package report.butt.mediamanager.client;

import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import report.butt.mediamanager.model.sonarr.Episode;
import report.butt.mediamanager.model.sonarr.Series;
import report.butt.mediamanager.model.sonarr.SeriesHistory;
import report.butt.mediamanager.model.sonarr.SonarrCommand;
import report.butt.mediamanager.model.sonarr.SonarrHealthItem;
import report.butt.mediamanager.model.sonarr.SonarrQueue;

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

    public SonarrCommand searchSeason(Integer seriesId, Integer seasonNumber) {
        return restClient
                .post()
                .uri("/api/v3/command")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .body(Map.of("name", "SeasonSearch", "seriesId", seriesId, "seasonNumber", seasonNumber))
                .retrieve()
                .body(SonarrCommand.class);
    }

    public SonarrCommand searchEpisodes(List<Integer> episodeIds) {
        return restClient
                .post()
                .uri("/api/v3/command")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .body(Map.of("name", "EpisodeSearch", "episodeIds", episodeIds))
                .retrieve()
                .body(SonarrCommand.class);
    }

    public List<Episode> getEpisodes(Integer seriesId) {
        return restClient
                .get()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/v3/episode")
                        .queryParam("seriesId", seriesId)
                        .queryParam("includeEpisodeFile", true)
                        .build())
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .body(new ParameterizedTypeReference<List<Episode>>() {});
    }

    public SonarrQueue getQueue() {
        return restClient
                .get()
                .uri(uriBuilder ->
                        uriBuilder.path("/api/v3/queue").queryParam("pageSize", 10000).build())
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .body(SonarrQueue.class);
    }

    public List<SonarrHealthItem> getHealth() {
        return restClient
                .get()
                .uri("/api/v3/health")
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .body(new ParameterizedTypeReference<List<SonarrHealthItem>>() {});
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
