package report.butt.mediamanager.model.sonarr;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@JsonIgnoreProperties(ignoreUnknown = true)
@NullMarked
public class SonarrQueueRecord {

    @JsonProperty("id")
    private @Nullable Integer id;

    @JsonProperty("seriesId")
    private @Nullable Integer seriesId;

    @JsonProperty("seasonNumber")
    private @Nullable Integer seasonNumber;

    @JsonProperty("trackedDownloadState")
    private @Nullable String trackedDownloadState;

    @JsonProperty("downloadId")
    private @Nullable String downloadId;

    @JsonProperty("protocol")
    private @Nullable String protocol;

    /** Populated only when the queue is fetched with {@code includeEpisode=true}; carries episodeNumber. */
    @JsonProperty("episode")
    private @Nullable Episode episode;

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

    public @Nullable Integer getSeasonNumber() {
        return seasonNumber;
    }

    public void setSeasonNumber(@Nullable Integer seasonNumber) {
        this.seasonNumber = seasonNumber;
    }

    public @Nullable String getTrackedDownloadState() {
        return trackedDownloadState;
    }

    public void setTrackedDownloadState(@Nullable String trackedDownloadState) {
        this.trackedDownloadState = trackedDownloadState;
    }

    public @Nullable String getDownloadId() {
        return downloadId;
    }

    public void setDownloadId(@Nullable String downloadId) {
        this.downloadId = downloadId;
    }

    public @Nullable String getProtocol() {
        return protocol;
    }

    public void setProtocol(@Nullable String protocol) {
        this.protocol = protocol;
    }

    public @Nullable Episode getEpisode() {
        return episode;
    }

    public void setEpisode(@Nullable Episode episode) {
        this.episode = episode;
    }
}
