package report.butt.mediamanager.model.sonarr;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@JsonIgnoreProperties(ignoreUnknown = true)
@NullMarked
public class SeriesStatistics {

    @JsonProperty("episodeFileCount")
    private @Nullable Integer episodeFileCount;

    @JsonProperty("episodeCount")
    private @Nullable Integer episodeCount;

    @JsonProperty("totalEpisodeCount")
    private @Nullable Integer totalEpisodeCount;

    public @Nullable Integer getEpisodeFileCount() {
        return episodeFileCount;
    }

    public void setEpisodeFileCount(@Nullable Integer episodeFileCount) {
        this.episodeFileCount = episodeFileCount;
    }

    public @Nullable Integer getEpisodeCount() {
        return episodeCount;
    }

    public void setEpisodeCount(@Nullable Integer episodeCount) {
        this.episodeCount = episodeCount;
    }

    public @Nullable Integer getTotalEpisodeCount() {
        return totalEpisodeCount;
    }

    public void setTotalEpisodeCount(@Nullable Integer totalEpisodeCount) {
        this.totalEpisodeCount = totalEpisodeCount;
    }
}
