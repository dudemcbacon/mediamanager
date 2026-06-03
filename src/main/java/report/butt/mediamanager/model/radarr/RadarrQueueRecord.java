package report.butt.mediamanager.model.radarr;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class RadarrQueueRecord {

    @JsonProperty("trackedDownloadState")
    private String trackedDownloadState;

    public String getTrackedDownloadState() {
        return trackedDownloadState;
    }

    public void setTrackedDownloadState(String trackedDownloadState) {
        this.trackedDownloadState = trackedDownloadState;
    }
}
