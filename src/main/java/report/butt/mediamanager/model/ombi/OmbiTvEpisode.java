package report.butt.mediamanager.model.ombi;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class OmbiTvEpisode {

    @JsonProperty("id")
    private Integer id;

    @JsonProperty("episodeNumber")
    private Integer episodeNumber;

    @JsonProperty("title")
    private String title;

    @JsonProperty("airDate")
    private String airDate;

    @JsonProperty("airDateDisplay")
    private String airDateDisplay;

    @JsonProperty("url")
    private String url;

    @JsonProperty("available")
    private Boolean available;

    @JsonProperty("approved")
    private Boolean approved;

    @JsonProperty("requested")
    private Boolean requested;

    @JsonProperty("denied")
    private Boolean denied;

    @JsonProperty("deniedReason")
    private String deniedReason;

    @JsonProperty("seasonId")
    private Integer seasonId;

    @JsonProperty("requestStatus")
    private String requestStatus;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getEpisodeNumber() {
        return episodeNumber;
    }

    public void setEpisodeNumber(Integer episodeNumber) {
        this.episodeNumber = episodeNumber;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getAirDate() {
        return airDate;
    }

    public void setAirDate(String airDate) {
        this.airDate = airDate;
    }

    public String getAirDateDisplay() {
        return airDateDisplay;
    }

    public void setAirDateDisplay(String airDateDisplay) {
        this.airDateDisplay = airDateDisplay;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public Boolean getAvailable() {
        return available;
    }

    public void setAvailable(Boolean available) {
        this.available = available;
    }

    public Boolean getApproved() {
        return approved;
    }

    public void setApproved(Boolean approved) {
        this.approved = approved;
    }

    public Boolean getRequested() {
        return requested;
    }

    public void setRequested(Boolean requested) {
        this.requested = requested;
    }

    public Boolean getDenied() {
        return denied;
    }

    public void setDenied(Boolean denied) {
        this.denied = denied;
    }

    public String getDeniedReason() {
        return deniedReason;
    }

    public void setDeniedReason(String deniedReason) {
        this.deniedReason = deniedReason;
    }

    public Integer getSeasonId() {
        return seasonId;
    }

    public void setSeasonId(Integer seasonId) {
        this.seasonId = seasonId;
    }

    public String getRequestStatus() {
        return requestStatus;
    }

    public void setRequestStatus(String requestStatus) {
        this.requestStatus = requestStatus;
    }
}
