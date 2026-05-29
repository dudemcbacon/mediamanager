package report.butt.mediamanager.model.ombi;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class OmbiTvChildRequest {

    @JsonProperty("id")
    private Integer id;

    @JsonProperty("parentRequestId")
    private Integer parentRequestId;

    @JsonProperty("title")
    private String title;

    @JsonProperty("issueId")
    private Integer issueId;

    @JsonProperty("issues")
    private Object issues;

    @JsonProperty("seriesType")
    private Integer seriesType;

    @JsonProperty("subscribed")
    private Boolean subscribed;

    @JsonProperty("showSubscribe")
    private Boolean showSubscribe;

    @JsonProperty("releaseYear")
    private String releaseYear;

    @JsonProperty("seasonRequests")
    private List<OmbiTvSeasonRequest> seasonRequests;

    @JsonProperty("approved")
    private Boolean approved;

    @JsonProperty("markedAsApproved")
    private String markedAsApproved;

    @JsonProperty("requestedDate")
    private String requestedDate;

    @JsonProperty("available")
    private Boolean available;

    @JsonProperty("markedAsAvailable")
    private String markedAsAvailable;

    @JsonProperty("requestedUserId")
    private String requestedUserId;

    @JsonProperty("denied")
    private Boolean denied;

    @JsonProperty("markedAsDenied")
    private String markedAsDenied;

    @JsonProperty("deniedReason")
    private String deniedReason;

    @JsonProperty("requestType")
    private Integer requestType;

    @JsonProperty("requestedByAlias")
    private String requestedByAlias;

    @JsonProperty("requestedUser")
    private User requestedUser;

    @JsonProperty("source")
    private Integer source;

    @JsonProperty("canApprove")
    private Boolean canApprove;

    @JsonProperty("requestedUserPlayedProgress")
    private Integer requestedUserPlayedProgress;

    @JsonProperty("requestStatus")
    private String requestStatus;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getParentRequestId() {
        return parentRequestId;
    }

    public void setParentRequestId(Integer parentRequestId) {
        this.parentRequestId = parentRequestId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Integer getIssueId() {
        return issueId;
    }

    public void setIssueId(Integer issueId) {
        this.issueId = issueId;
    }

    public Object getIssues() {
        return issues;
    }

    public void setIssues(Object issues) {
        this.issues = issues;
    }

    public Integer getSeriesType() {
        return seriesType;
    }

    public void setSeriesType(Integer seriesType) {
        this.seriesType = seriesType;
    }

    public Boolean getSubscribed() {
        return subscribed;
    }

    public void setSubscribed(Boolean subscribed) {
        this.subscribed = subscribed;
    }

    public Boolean getShowSubscribe() {
        return showSubscribe;
    }

    public void setShowSubscribe(Boolean showSubscribe) {
        this.showSubscribe = showSubscribe;
    }

    public String getReleaseYear() {
        return releaseYear;
    }

    public void setReleaseYear(String releaseYear) {
        this.releaseYear = releaseYear;
    }

    public List<OmbiTvSeasonRequest> getSeasonRequests() {
        return seasonRequests;
    }

    public void setSeasonRequests(List<OmbiTvSeasonRequest> seasonRequests) {
        this.seasonRequests = seasonRequests;
    }

    public Boolean getApproved() {
        return approved;
    }

    public void setApproved(Boolean approved) {
        this.approved = approved;
    }

    public String getMarkedAsApproved() {
        return markedAsApproved;
    }

    public void setMarkedAsApproved(String markedAsApproved) {
        this.markedAsApproved = markedAsApproved;
    }

    public String getRequestedDate() {
        return requestedDate;
    }

    public void setRequestedDate(String requestedDate) {
        this.requestedDate = requestedDate;
    }

    public Boolean getAvailable() {
        return available;
    }

    public void setAvailable(Boolean available) {
        this.available = available;
    }

    public String getMarkedAsAvailable() {
        return markedAsAvailable;
    }

    public void setMarkedAsAvailable(String markedAsAvailable) {
        this.markedAsAvailable = markedAsAvailable;
    }

    public String getRequestedUserId() {
        return requestedUserId;
    }

    public void setRequestedUserId(String requestedUserId) {
        this.requestedUserId = requestedUserId;
    }

    public Boolean getDenied() {
        return denied;
    }

    public void setDenied(Boolean denied) {
        this.denied = denied;
    }

    public String getMarkedAsDenied() {
        return markedAsDenied;
    }

    public void setMarkedAsDenied(String markedAsDenied) {
        this.markedAsDenied = markedAsDenied;
    }

    public String getDeniedReason() {
        return deniedReason;
    }

    public void setDeniedReason(String deniedReason) {
        this.deniedReason = deniedReason;
    }

    public Integer getRequestType() {
        return requestType;
    }

    public void setRequestType(Integer requestType) {
        this.requestType = requestType;
    }

    public String getRequestedByAlias() {
        return requestedByAlias;
    }

    public void setRequestedByAlias(String requestedByAlias) {
        this.requestedByAlias = requestedByAlias;
    }

    public User getRequestedUser() {
        return requestedUser;
    }

    public void setRequestedUser(User requestedUser) {
        this.requestedUser = requestedUser;
    }

    public Integer getSource() {
        return source;
    }

    public void setSource(Integer source) {
        this.source = source;
    }

    public Boolean getCanApprove() {
        return canApprove;
    }

    public void setCanApprove(Boolean canApprove) {
        this.canApprove = canApprove;
    }

    public Integer getRequestedUserPlayedProgress() {
        return requestedUserPlayedProgress;
    }

    public void setRequestedUserPlayedProgress(Integer requestedUserPlayedProgress) {
        this.requestedUserPlayedProgress = requestedUserPlayedProgress;
    }

    public String getRequestStatus() {
        return requestStatus;
    }

    public void setRequestStatus(String requestStatus) {
        this.requestStatus = requestStatus;
    }
}
