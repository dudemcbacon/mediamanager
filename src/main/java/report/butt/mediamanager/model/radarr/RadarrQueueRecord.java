package report.butt.mediamanager.model.radarr;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class RadarrQueueRecord {

    @JsonProperty("id")
    private Integer id;

    @JsonProperty("movieId")
    private Integer movieId;

    @JsonProperty("trackedDownloadState")
    private String trackedDownloadState;

    @JsonProperty("downloadId")
    private String downloadId;

    @JsonProperty("protocol")
    private String protocol;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getMovieId() {
        return movieId;
    }

    public void setMovieId(Integer movieId) {
        this.movieId = movieId;
    }

    public String getTrackedDownloadState() {
        return trackedDownloadState;
    }

    public void setTrackedDownloadState(String trackedDownloadState) {
        this.trackedDownloadState = trackedDownloadState;
    }

    public String getDownloadId() {
        return downloadId;
    }

    public void setDownloadId(String downloadId) {
        this.downloadId = downloadId;
    }

    public String getProtocol() {
        return protocol;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }
}
