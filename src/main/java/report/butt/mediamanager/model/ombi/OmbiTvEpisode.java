package report.butt.mediamanager.model.ombi;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@JsonIgnoreProperties(ignoreUnknown = true)
@NullMarked
public class OmbiTvEpisode {

    @JsonProperty("id")
    private @Nullable Integer id;

    @JsonProperty("episodeNumber")
    private @Nullable Integer episodeNumber;

    @JsonProperty("title")
    private @Nullable String title;

    @JsonProperty("airDate")
    private @Nullable String airDate;

    @JsonProperty("airDateDisplay")
    private @Nullable String airDateDisplay;

    @JsonProperty("url")
    private @Nullable String url;

    @JsonProperty("available")
    private @Nullable Boolean available;

    @JsonProperty("approved")
    private @Nullable Boolean approved;

    @JsonProperty("requested")
    private @Nullable Boolean requested;

    @JsonProperty("denied")
    private @Nullable Boolean denied;

    @JsonProperty("deniedReason")
    private @Nullable String deniedReason;

    @JsonProperty("seasonId")
    private @Nullable Integer seasonId;

    @JsonProperty("requestStatus")
    private @Nullable String requestStatus;

    public @Nullable Integer getId() {
        return id;
    }

    public void setId(@Nullable Integer id) {
        this.id = id;
    }

    public @Nullable Integer getEpisodeNumber() {
        return episodeNumber;
    }

    public void setEpisodeNumber(@Nullable Integer episodeNumber) {
        this.episodeNumber = episodeNumber;
    }

    public @Nullable String getTitle() {
        return title;
    }

    public void setTitle(@Nullable String title) {
        this.title = title;
    }

    public @Nullable String getAirDate() {
        return airDate;
    }

    public void setAirDate(@Nullable String airDate) {
        this.airDate = airDate;
    }

    public @Nullable String getAirDateDisplay() {
        return airDateDisplay;
    }

    public void setAirDateDisplay(@Nullable String airDateDisplay) {
        this.airDateDisplay = airDateDisplay;
    }

    public @Nullable String getUrl() {
        return url;
    }

    public void setUrl(@Nullable String url) {
        this.url = url;
    }

    public @Nullable Boolean getAvailable() {
        return available;
    }

    public void setAvailable(@Nullable Boolean available) {
        this.available = available;
    }

    public @Nullable Boolean getApproved() {
        return approved;
    }

    public void setApproved(@Nullable Boolean approved) {
        this.approved = approved;
    }

    public @Nullable Boolean getRequested() {
        return requested;
    }

    public void setRequested(@Nullable Boolean requested) {
        this.requested = requested;
    }

    public @Nullable Boolean getDenied() {
        return denied;
    }

    public void setDenied(@Nullable Boolean denied) {
        this.denied = denied;
    }

    public @Nullable String getDeniedReason() {
        return deniedReason;
    }

    public void setDeniedReason(@Nullable String deniedReason) {
        this.deniedReason = deniedReason;
    }

    public @Nullable Integer getSeasonId() {
        return seasonId;
    }

    public void setSeasonId(@Nullable Integer seasonId) {
        this.seasonId = seasonId;
    }

    public @Nullable String getRequestStatus() {
        return requestStatus;
    }

    public void setRequestStatus(@Nullable String requestStatus) {
        this.requestStatus = requestStatus;
    }
}
