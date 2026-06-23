package report.butt.mediamanager.model.radarr;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@JsonIgnoreProperties(ignoreUnknown = true)
@NullMarked
public class RadarrQueueRecord {

    @JsonProperty("id")
    private @Nullable Integer id;

    @JsonProperty("movieId")
    private @Nullable Integer movieId;

    @JsonProperty("trackedDownloadState")
    private @Nullable String trackedDownloadState;

    @JsonProperty("downloadId")
    private @Nullable String downloadId;

    @JsonProperty("protocol")
    private @Nullable String protocol;

    public @Nullable Integer getId() {
        return id;
    }

    public void setId(@Nullable Integer id) {
        this.id = id;
    }

    public @Nullable Integer getMovieId() {
        return movieId;
    }

    public void setMovieId(@Nullable Integer movieId) {
        this.movieId = movieId;
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
}
