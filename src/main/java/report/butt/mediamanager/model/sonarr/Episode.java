package report.butt.mediamanager.model.sonarr;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@JsonIgnoreProperties(ignoreUnknown = true)
@NullMarked
public class Episode {

    @JsonProperty("id")
    private @Nullable Integer id;

    @JsonProperty("seasonNumber")
    private @Nullable Integer seasonNumber;

    @JsonProperty("episodeNumber")
    private @Nullable Integer episodeNumber;

    @JsonProperty("hasFile")
    private @Nullable Boolean hasFile;

    @JsonProperty("lastSearchTime")
    private @Nullable String lastSearchTime;

    @JsonProperty("episodeFile")
    private @Nullable EpisodeFile episodeFile;

    public @Nullable Integer getId() {
        return id;
    }

    public void setId(@Nullable Integer id) {
        this.id = id;
    }

    public @Nullable Integer getSeasonNumber() {
        return seasonNumber;
    }

    public void setSeasonNumber(@Nullable Integer seasonNumber) {
        this.seasonNumber = seasonNumber;
    }

    public @Nullable Integer getEpisodeNumber() {
        return episodeNumber;
    }

    public void setEpisodeNumber(@Nullable Integer episodeNumber) {
        this.episodeNumber = episodeNumber;
    }

    public @Nullable Boolean getHasFile() {
        return hasFile;
    }

    public void setHasFile(@Nullable Boolean hasFile) {
        this.hasFile = hasFile;
    }

    public @Nullable String getLastSearchTime() {
        return lastSearchTime;
    }

    public void setLastSearchTime(@Nullable String lastSearchTime) {
        this.lastSearchTime = lastSearchTime;
    }

    public @Nullable EpisodeFile getEpisodeFile() {
        return episodeFile;
    }

    public void setEpisodeFile(@Nullable EpisodeFile episodeFile) {
        this.episodeFile = episodeFile;
    }
}
