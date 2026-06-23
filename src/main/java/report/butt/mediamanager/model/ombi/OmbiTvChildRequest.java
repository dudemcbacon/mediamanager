package report.butt.mediamanager.model.ombi;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@JsonIgnoreProperties(ignoreUnknown = true)
@NullMarked
public class OmbiTvChildRequest {

    @JsonProperty("id")
    private @Nullable Integer id;

    @JsonProperty("parentRequestId")
    private @Nullable Integer parentRequestId;

    @JsonProperty("title")
    private @Nullable String title;

    @JsonProperty("issueId")
    private @Nullable Integer issueId;

    @JsonProperty("issues")
    private @Nullable Object issues;

    @JsonProperty("seriesType")
    private @Nullable Integer seriesType;

    @JsonProperty("subscribed")
    private @Nullable Boolean subscribed;

    @JsonProperty("showSubscribe")
    private @Nullable Boolean showSubscribe;

    @JsonProperty("releaseYear")
    private @Nullable String releaseYear;

    @JsonProperty("seasonRequests")
    private @Nullable List<OmbiTvSeasonRequest> seasonRequests;

    @JsonProperty("approved")
    private @Nullable Boolean approved;

    @JsonProperty("markedAsApproved")
    private @Nullable String markedAsApproved;

    @JsonProperty("requestedDate")
    private @Nullable String requestedDate;

    @JsonProperty("available")
    private @Nullable Boolean available;

    @JsonProperty("markedAsAvailable")
    private @Nullable String markedAsAvailable;

    @JsonProperty("requestedUserId")
    private @Nullable String requestedUserId;

    @JsonProperty("denied")
    private @Nullable Boolean denied;

    @JsonProperty("markedAsDenied")
    private @Nullable String markedAsDenied;

    @JsonProperty("deniedReason")
    private @Nullable String deniedReason;

    @JsonProperty("requestType")
    private @Nullable Integer requestType;

    @JsonProperty("requestedByAlias")
    private @Nullable String requestedByAlias;

    @JsonProperty("requestedUser")
    private @Nullable User requestedUser;

    @JsonProperty("source")
    private @Nullable Integer source;

    @JsonProperty("canApprove")
    private @Nullable Boolean canApprove;

    @JsonProperty("requestedUserPlayedProgress")
    private @Nullable Integer requestedUserPlayedProgress;

    @JsonProperty("requestStatus")
    private @Nullable String requestStatus;

    public @Nullable Integer getId() {
        return id;
    }

    public void setId(@Nullable Integer id) {
        this.id = id;
    }

    public @Nullable Integer getParentRequestId() {
        return parentRequestId;
    }

    public void setParentRequestId(@Nullable Integer parentRequestId) {
        this.parentRequestId = parentRequestId;
    }

    public @Nullable String getTitle() {
        return title;
    }

    public void setTitle(@Nullable String title) {
        this.title = title;
    }

    public @Nullable Integer getIssueId() {
        return issueId;
    }

    public void setIssueId(@Nullable Integer issueId) {
        this.issueId = issueId;
    }

    public @Nullable Object getIssues() {
        return issues;
    }

    public void setIssues(@Nullable Object issues) {
        this.issues = issues;
    }

    public @Nullable Integer getSeriesType() {
        return seriesType;
    }

    public void setSeriesType(@Nullable Integer seriesType) {
        this.seriesType = seriesType;
    }

    public @Nullable Boolean getSubscribed() {
        return subscribed;
    }

    public void setSubscribed(@Nullable Boolean subscribed) {
        this.subscribed = subscribed;
    }

    public @Nullable Boolean getShowSubscribe() {
        return showSubscribe;
    }

    public void setShowSubscribe(@Nullable Boolean showSubscribe) {
        this.showSubscribe = showSubscribe;
    }

    public @Nullable String getReleaseYear() {
        return releaseYear;
    }

    public void setReleaseYear(@Nullable String releaseYear) {
        this.releaseYear = releaseYear;
    }

    public @Nullable List<OmbiTvSeasonRequest> getSeasonRequests() {
        return seasonRequests;
    }

    public void setSeasonRequests(@Nullable List<OmbiTvSeasonRequest> seasonRequests) {
        this.seasonRequests = seasonRequests;
    }

    public @Nullable Boolean getApproved() {
        return approved;
    }

    public void setApproved(@Nullable Boolean approved) {
        this.approved = approved;
    }

    public @Nullable String getMarkedAsApproved() {
        return markedAsApproved;
    }

    public void setMarkedAsApproved(@Nullable String markedAsApproved) {
        this.markedAsApproved = markedAsApproved;
    }

    public @Nullable String getRequestedDate() {
        return requestedDate;
    }

    public void setRequestedDate(@Nullable String requestedDate) {
        this.requestedDate = requestedDate;
    }

    public @Nullable Boolean getAvailable() {
        return available;
    }

    public void setAvailable(@Nullable Boolean available) {
        this.available = available;
    }

    public @Nullable String getMarkedAsAvailable() {
        return markedAsAvailable;
    }

    public void setMarkedAsAvailable(@Nullable String markedAsAvailable) {
        this.markedAsAvailable = markedAsAvailable;
    }

    public @Nullable String getRequestedUserId() {
        return requestedUserId;
    }

    public void setRequestedUserId(@Nullable String requestedUserId) {
        this.requestedUserId = requestedUserId;
    }

    public @Nullable Boolean getDenied() {
        return denied;
    }

    public void setDenied(@Nullable Boolean denied) {
        this.denied = denied;
    }

    public @Nullable String getMarkedAsDenied() {
        return markedAsDenied;
    }

    public void setMarkedAsDenied(@Nullable String markedAsDenied) {
        this.markedAsDenied = markedAsDenied;
    }

    public @Nullable String getDeniedReason() {
        return deniedReason;
    }

    public void setDeniedReason(@Nullable String deniedReason) {
        this.deniedReason = deniedReason;
    }

    public @Nullable Integer getRequestType() {
        return requestType;
    }

    public void setRequestType(@Nullable Integer requestType) {
        this.requestType = requestType;
    }

    public @Nullable String getRequestedByAlias() {
        return requestedByAlias;
    }

    public void setRequestedByAlias(@Nullable String requestedByAlias) {
        this.requestedByAlias = requestedByAlias;
    }

    public @Nullable User getRequestedUser() {
        return requestedUser;
    }

    public void setRequestedUser(@Nullable User requestedUser) {
        this.requestedUser = requestedUser;
    }

    public @Nullable Integer getSource() {
        return source;
    }

    public void setSource(@Nullable Integer source) {
        this.source = source;
    }

    public @Nullable Boolean getCanApprove() {
        return canApprove;
    }

    public void setCanApprove(@Nullable Boolean canApprove) {
        this.canApprove = canApprove;
    }

    public @Nullable Integer getRequestedUserPlayedProgress() {
        return requestedUserPlayedProgress;
    }

    public void setRequestedUserPlayedProgress(@Nullable Integer requestedUserPlayedProgress) {
        this.requestedUserPlayedProgress = requestedUserPlayedProgress;
    }

    public @Nullable String getRequestStatus() {
        return requestStatus;
    }

    public void setRequestStatus(@Nullable String requestStatus) {
        this.requestStatus = requestStatus;
    }
}
