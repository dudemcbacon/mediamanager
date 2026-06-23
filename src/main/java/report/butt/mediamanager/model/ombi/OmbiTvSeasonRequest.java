package report.butt.mediamanager.model.ombi;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@JsonIgnoreProperties(ignoreUnknown = true)
@NullMarked
public class OmbiTvSeasonRequest {

    @JsonProperty("id")
    private @Nullable Integer id;

    @JsonProperty("seasonNumber")
    private @Nullable Integer seasonNumber;

    @JsonProperty("overview")
    private @Nullable String overview;

    @JsonProperty("episodes")
    private @Nullable List<OmbiTvEpisode> episodes;

    @JsonProperty("childRequestId")
    private @Nullable Integer childRequestId;

    @JsonProperty("seasonAvailable")
    private @Nullable Boolean seasonAvailable;

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

    public @Nullable String getOverview() {
        return overview;
    }

    public void setOverview(@Nullable String overview) {
        this.overview = overview;
    }

    public @Nullable List<OmbiTvEpisode> getEpisodes() {
        return episodes;
    }

    public void setEpisodes(@Nullable List<OmbiTvEpisode> episodes) {
        this.episodes = episodes;
    }

    public @Nullable Integer getChildRequestId() {
        return childRequestId;
    }

    public void setChildRequestId(@Nullable Integer childRequestId) {
        this.childRequestId = childRequestId;
    }

    public @Nullable Boolean getSeasonAvailable() {
        return seasonAvailable;
    }

    public void setSeasonAvailable(@Nullable Boolean seasonAvailable) {
        this.seasonAvailable = seasonAvailable;
    }
}
