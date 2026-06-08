package report.butt.mediamanager.model.sonarr;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class SonarrQueueRecord {

    @JsonProperty("id")
    private Integer id;

    @JsonProperty("seriesId")
    private Integer seriesId;

    @JsonProperty("seasonNumber")
    private Integer seasonNumber;

    @JsonProperty("trackedDownloadState")
    private String trackedDownloadState;

    @JsonProperty("downloadId")
    private String downloadId;

    @JsonProperty("protocol")
    private String protocol;

    /** Populated only when the queue is fetched with {@code includeEpisode=true}; carries episodeNumber. */
    @JsonProperty("episode")
    private Episode episode;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getSeriesId() {
        return seriesId;
    }

    public void setSeriesId(Integer seriesId) {
        this.seriesId = seriesId;
    }

    public Integer getSeasonNumber() {
        return seasonNumber;
    }

    public void setSeasonNumber(Integer seasonNumber) {
        this.seasonNumber = seasonNumber;
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

    public Episode getEpisode() {
        return episode;
    }

    public void setEpisode(Episode episode) {
        this.episode = episode;
    }
}
