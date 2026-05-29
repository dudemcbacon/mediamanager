package report.butt.mediamanager.model.sonarr;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class SeriesStatistics {

    @JsonProperty("episodeFileCount")
    private Integer episodeFileCount;

    @JsonProperty("episodeCount")
    private Integer episodeCount;

    @JsonProperty("totalEpisodeCount")
    private Integer totalEpisodeCount;

    public Integer getEpisodeFileCount() {
        return episodeFileCount;
    }

    public void setEpisodeFileCount(Integer episodeFileCount) {
        this.episodeFileCount = episodeFileCount;
    }

    public Integer getEpisodeCount() {
        return episodeCount;
    }

    public void setEpisodeCount(Integer episodeCount) {
        this.episodeCount = episodeCount;
    }

    public Integer getTotalEpisodeCount() {
        return totalEpisodeCount;
    }

    public void setTotalEpisodeCount(Integer totalEpisodeCount) {
        this.totalEpisodeCount = totalEpisodeCount;
    }
}
