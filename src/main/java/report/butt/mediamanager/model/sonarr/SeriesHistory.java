package report.butt.mediamanager.model.sonarr;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@JsonIgnoreProperties(ignoreUnknown = true)
@NullMarked
public class SeriesHistory {

    @JsonProperty("id")
    private @Nullable Integer id;

    @JsonProperty("seriesId")
    private @Nullable Integer seriesId;

    @JsonProperty("episodeId")
    private @Nullable Integer episodeId;

    @JsonProperty("sourceTitle")
    private @Nullable String sourceTitle;

    @JsonProperty("date")
    private @Nullable String date;

    @JsonProperty("eventType")
    private @Nullable String eventType;

    public @Nullable Integer getId() {
        return id;
    }

    public void setId(@Nullable Integer id) {
        this.id = id;
    }

    public @Nullable Integer getSeriesId() {
        return seriesId;
    }

    public void setSeriesId(@Nullable Integer seriesId) {
        this.seriesId = seriesId;
    }

    public @Nullable Integer getEpisodeId() {
        return episodeId;
    }

    public void setEpisodeId(@Nullable Integer episodeId) {
        this.episodeId = episodeId;
    }

    public @Nullable String getSourceTitle() {
        return sourceTitle;
    }

    public void setSourceTitle(@Nullable String sourceTitle) {
        this.sourceTitle = sourceTitle;
    }

    public @Nullable String getDate() {
        return date;
    }

    public void setDate(@Nullable String date) {
        this.date = date;
    }

    public @Nullable String getEventType() {
        return eventType;
    }

    public void setEventType(@Nullable String eventType) {
        this.eventType = eventType;
    }
}
