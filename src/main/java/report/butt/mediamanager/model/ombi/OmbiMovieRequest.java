package report.butt.mediamanager.model.ombi;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Map;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
    "theMovieDbId",
    "issueId",
    "issues",
    "subscribed",
    "showSubscribe",
    "is4kRequest",
    "rootPathOverride",
    "qualityOverride",
    "has4KRequest",
    "approved4K",
    "markedAsApproved4K",
    "requestedDate4k",
    "available4K",
    "markedAsAvailable4K",
    "denied4K",
    "markedAsDenied4K",
    "deniedReason4K",
    "requestCombination",
    "langCode",
    "requestStatus",
    "canApprove",
    "watchedByRequestedUser",
    "playedByUsersCount",
    "imdbId",
    "overview",
    "posterPath",
    "releaseDate",
    "digitalReleaseDate",
    "status",
    "background",
    "released",
    "digitalRelease",
    "title",
    "approved",
    "markedAsApproved",
    "requestedDate",
    "available",
    "markedAsAvailable",
    "requestedUserId",
    "denied",
    "markedAsDenied",
    "deniedReason",
    "requestType",
    "requestedByAlias",
    "requestedUser",
    "source",
    "id"
})
@NullMarked
public class OmbiMovieRequest implements Serializable {
    @JsonProperty("theMovieDbId")
    private @Nullable Integer theMovieDbId;

    @JsonProperty("issueId")
    private @Nullable Object issueId;

    @JsonProperty("issues")
    private @Nullable Object issues;

    @JsonProperty("subscribed")
    private @Nullable Boolean subscribed;

    @JsonProperty("showSubscribe")
    private @Nullable Boolean showSubscribe;

    @JsonProperty("is4kRequest")
    private @Nullable Boolean is4kRequest;

    @JsonProperty("rootPathOverride")
    private @Nullable Integer rootPathOverride;

    @JsonProperty("qualityOverride")
    private @Nullable Integer qualityOverride;

    @JsonProperty("has4KRequest")
    private @Nullable Boolean has4KRequest;

    @JsonProperty("approved4K")
    private @Nullable Boolean approved4K;

    @JsonProperty("markedAsApproved4K")
    private @Nullable String markedAsApproved4K;

    @JsonProperty("requestedDate4k")
    private @Nullable String requestedDate4k;

    @JsonProperty("available4K")
    private @Nullable Boolean available4K;

    @JsonProperty("markedAsAvailable4K")
    private @Nullable Object markedAsAvailable4K;

    @JsonProperty("denied4K")
    private @Nullable Object denied4K;

    @JsonProperty("markedAsDenied4K")
    private @Nullable String markedAsDenied4K;

    @JsonProperty("deniedReason4K")
    private @Nullable Object deniedReason4K;

    @JsonProperty("requestCombination")
    private @Nullable Integer requestCombination;

    @JsonProperty("langCode")
    private @Nullable String langCode;

    @JsonProperty("requestStatus")
    private @Nullable String requestStatus;

    @JsonProperty("canApprove")
    private @Nullable Boolean canApprove;

    @JsonProperty("watchedByRequestedUser")
    private @Nullable Boolean watchedByRequestedUser;

    @JsonProperty("playedByUsersCount")
    private @Nullable Integer playedByUsersCount;

    @JsonProperty("imdbId")
    private @Nullable Object imdbId;

    @JsonProperty("overview")
    private @Nullable String overview;

    @JsonProperty("posterPath")
    private @Nullable Object posterPath;

    @JsonProperty("releaseDate")
    private @Nullable String releaseDate;

    @JsonProperty("digitalReleaseDate")
    private @Nullable Object digitalReleaseDate;

    @JsonProperty("status")
    private @Nullable String status;

    @JsonProperty("background")
    private @Nullable Object background;

    @JsonProperty("released")
    private @Nullable Boolean released;

    @JsonProperty("digitalRelease")
    private @Nullable Boolean digitalRelease;

    @JsonProperty("title")
    private @Nullable String title;

    @JsonProperty("approved")
    private @Nullable Boolean approved;

    @JsonProperty("markedAsApproved")
    private @Nullable String markedAsApproved;

    @JsonProperty("requestedDate")
    private @Nullable String requestedDate;

    @JsonProperty("available")
    private @Nullable Boolean available;

    @JsonProperty("markedAsAvailable")
    private @Nullable Object markedAsAvailable;

    @JsonProperty("requestedUserId")
    private @Nullable String requestedUserId;

    @JsonProperty("denied")
    private @Nullable Boolean denied;

    @JsonProperty("markedAsDenied")
    private @Nullable String markedAsDenied;

    @JsonProperty("deniedReason")
    private @Nullable Object deniedReason;

    @JsonProperty("requestType")
    private @Nullable Integer requestType;

    @JsonProperty("requestedByAlias")
    private @Nullable Object requestedByAlias;

    @JsonProperty("requestedUser")
    private @Nullable User requestedUser;

    @JsonProperty("source")
    private @Nullable Integer source;

    @JsonProperty("id")
    private @Nullable Integer id;

    @JsonIgnore
    private final Map<String, Object> additionalProperties = new LinkedHashMap<String, Object>();

    private static final long serialVersionUID = 6356458637734283408L;

    @JsonProperty("theMovieDbId")
    public @Nullable Integer getTheMovieDbId() {
        return theMovieDbId;
    }

    @JsonProperty("theMovieDbId")
    public void setTheMovieDbId(@Nullable Integer theMovieDbId) {
        this.theMovieDbId = theMovieDbId;
    }

    @JsonProperty("issueId")
    public @Nullable Object getIssueId() {
        return issueId;
    }

    @JsonProperty("issueId")
    public void setIssueId(@Nullable Object issueId) {
        this.issueId = issueId;
    }

    @JsonProperty("issues")
    public @Nullable Object getIssues() {
        return issues;
    }

    @JsonProperty("issues")
    public void setIssues(@Nullable Object issues) {
        this.issues = issues;
    }

    @JsonProperty("subscribed")
    public @Nullable Boolean getSubscribed() {
        return subscribed;
    }

    @JsonProperty("subscribed")
    public void setSubscribed(@Nullable Boolean subscribed) {
        this.subscribed = subscribed;
    }

    @JsonProperty("showSubscribe")
    public @Nullable Boolean getShowSubscribe() {
        return showSubscribe;
    }

    @JsonProperty("showSubscribe")
    public void setShowSubscribe(@Nullable Boolean showSubscribe) {
        this.showSubscribe = showSubscribe;
    }

    @JsonProperty("is4kRequest")
    public @Nullable Boolean getIs4kRequest() {
        return is4kRequest;
    }

    @JsonProperty("is4kRequest")
    public void setIs4kRequest(@Nullable Boolean is4kRequest) {
        this.is4kRequest = is4kRequest;
    }

    @JsonProperty("rootPathOverride")
    public @Nullable Integer getRootPathOverride() {
        return rootPathOverride;
    }

    @JsonProperty("rootPathOverride")
    public void setRootPathOverride(@Nullable Integer rootPathOverride) {
        this.rootPathOverride = rootPathOverride;
    }

    @JsonProperty("qualityOverride")
    public @Nullable Integer getQualityOverride() {
        return qualityOverride;
    }

    @JsonProperty("qualityOverride")
    public void setQualityOverride(@Nullable Integer qualityOverride) {
        this.qualityOverride = qualityOverride;
    }

    @JsonProperty("has4KRequest")
    public @Nullable Boolean getHas4KRequest() {
        return has4KRequest;
    }

    @JsonProperty("has4KRequest")
    public void setHas4KRequest(@Nullable Boolean has4KRequest) {
        this.has4KRequest = has4KRequest;
    }

    @JsonProperty("approved4K")
    public @Nullable Boolean getApproved4K() {
        return approved4K;
    }

    @JsonProperty("approved4K")
    public void setApproved4K(@Nullable Boolean approved4K) {
        this.approved4K = approved4K;
    }

    @JsonProperty("markedAsApproved4K")
    public @Nullable String getMarkedAsApproved4K() {
        return markedAsApproved4K;
    }

    @JsonProperty("markedAsApproved4K")
    public void setMarkedAsApproved4K(@Nullable String markedAsApproved4K) {
        this.markedAsApproved4K = markedAsApproved4K;
    }

    @JsonProperty("requestedDate4k")
    public @Nullable String getRequestedDate4k() {
        return requestedDate4k;
    }

    @JsonProperty("requestedDate4k")
    public void setRequestedDate4k(@Nullable String requestedDate4k) {
        this.requestedDate4k = requestedDate4k;
    }

    @JsonProperty("available4K")
    public @Nullable Boolean getAvailable4K() {
        return available4K;
    }

    @JsonProperty("available4K")
    public void setAvailable4K(@Nullable Boolean available4K) {
        this.available4K = available4K;
    }

    @JsonProperty("markedAsAvailable4K")
    public @Nullable Object getMarkedAsAvailable4K() {
        return markedAsAvailable4K;
    }

    @JsonProperty("markedAsAvailable4K")
    public void setMarkedAsAvailable4K(@Nullable Object markedAsAvailable4K) {
        this.markedAsAvailable4K = markedAsAvailable4K;
    }

    @JsonProperty("denied4K")
    public @Nullable Object getDenied4K() {
        return denied4K;
    }

    @JsonProperty("denied4K")
    public void setDenied4K(@Nullable Object denied4K) {
        this.denied4K = denied4K;
    }

    @JsonProperty("markedAsDenied4K")
    public @Nullable String getMarkedAsDenied4K() {
        return markedAsDenied4K;
    }

    @JsonProperty("markedAsDenied4K")
    public void setMarkedAsDenied4K(@Nullable String markedAsDenied4K) {
        this.markedAsDenied4K = markedAsDenied4K;
    }

    @JsonProperty("deniedReason4K")
    public @Nullable Object getDeniedReason4K() {
        return deniedReason4K;
    }

    @JsonProperty("deniedReason4K")
    public void setDeniedReason4K(@Nullable Object deniedReason4K) {
        this.deniedReason4K = deniedReason4K;
    }

    @JsonProperty("requestCombination")
    public @Nullable Integer getRequestCombination() {
        return requestCombination;
    }

    @JsonProperty("requestCombination")
    public void setRequestCombination(@Nullable Integer requestCombination) {
        this.requestCombination = requestCombination;
    }

    @JsonProperty("langCode")
    public @Nullable String getLangCode() {
        return langCode;
    }

    @JsonProperty("langCode")
    public void setLangCode(@Nullable String langCode) {
        this.langCode = langCode;
    }

    @JsonProperty("requestStatus")
    public @Nullable String getRequestStatus() {
        return requestStatus;
    }

    @JsonProperty("requestStatus")
    public void setRequestStatus(@Nullable String requestStatus) {
        this.requestStatus = requestStatus;
    }

    @JsonProperty("canApprove")
    public @Nullable Boolean getCanApprove() {
        return canApprove;
    }

    @JsonProperty("canApprove")
    public void setCanApprove(@Nullable Boolean canApprove) {
        this.canApprove = canApprove;
    }

    @JsonProperty("watchedByRequestedUser")
    public @Nullable Boolean getWatchedByRequestedUser() {
        return watchedByRequestedUser;
    }

    @JsonProperty("watchedByRequestedUser")
    public void setWatchedByRequestedUser(@Nullable Boolean watchedByRequestedUser) {
        this.watchedByRequestedUser = watchedByRequestedUser;
    }

    @JsonProperty("playedByUsersCount")
    public @Nullable Integer getPlayedByUsersCount() {
        return playedByUsersCount;
    }

    @JsonProperty("playedByUsersCount")
    public void setPlayedByUsersCount(@Nullable Integer playedByUsersCount) {
        this.playedByUsersCount = playedByUsersCount;
    }

    @JsonProperty("imdbId")
    public @Nullable Object getImdbId() {
        return imdbId;
    }

    @JsonProperty("imdbId")
    public void setImdbId(@Nullable Object imdbId) {
        this.imdbId = imdbId;
    }

    @JsonProperty("overview")
    public @Nullable String getOverview() {
        return overview;
    }

    @JsonProperty("overview")
    public void setOverview(@Nullable String overview) {
        this.overview = overview;
    }

    @JsonProperty("posterPath")
    public @Nullable Object getPosterPath() {
        return posterPath;
    }

    @JsonProperty("posterPath")
    public void setPosterPath(@Nullable Object posterPath) {
        this.posterPath = posterPath;
    }

    @JsonProperty("releaseDate")
    public @Nullable String getReleaseDate() {
        return releaseDate;
    }

    @JsonProperty("releaseDate")
    public void setReleaseDate(@Nullable String releaseDate) {
        this.releaseDate = releaseDate;
    }

    @JsonProperty("digitalReleaseDate")
    public @Nullable Object getDigitalReleaseDate() {
        return digitalReleaseDate;
    }

    @JsonProperty("digitalReleaseDate")
    public void setDigitalReleaseDate(@Nullable Object digitalReleaseDate) {
        this.digitalReleaseDate = digitalReleaseDate;
    }

    @JsonProperty("status")
    public @Nullable String getStatus() {
        return status;
    }

    @JsonProperty("status")
    public void setStatus(@Nullable String status) {
        this.status = status;
    }

    @JsonProperty("background")
    public @Nullable Object getBackground() {
        return background;
    }

    @JsonProperty("background")
    public void setBackground(@Nullable Object background) {
        this.background = background;
    }

    @JsonProperty("released")
    public @Nullable Boolean getReleased() {
        return released;
    }

    @JsonProperty("released")
    public void setReleased(@Nullable Boolean released) {
        this.released = released;
    }

    @JsonProperty("digitalRelease")
    public @Nullable Boolean getDigitalRelease() {
        return digitalRelease;
    }

    @JsonProperty("digitalRelease")
    public void setDigitalRelease(@Nullable Boolean digitalRelease) {
        this.digitalRelease = digitalRelease;
    }

    @JsonProperty("title")
    public @Nullable String getTitle() {
        return title;
    }

    @JsonProperty("title")
    public void setTitle(@Nullable String title) {
        this.title = title;
    }

    @JsonProperty("approved")
    public @Nullable Boolean getApproved() {
        return approved;
    }

    @JsonProperty("approved")
    public void setApproved(@Nullable Boolean approved) {
        this.approved = approved;
    }

    @JsonProperty("markedAsApproved")
    public @Nullable String getMarkedAsApproved() {
        return markedAsApproved;
    }

    @JsonProperty("markedAsApproved")
    public void setMarkedAsApproved(@Nullable String markedAsApproved) {
        this.markedAsApproved = markedAsApproved;
    }

    @JsonProperty("requestedDate")
    public @Nullable String getRequestedDate() {
        return requestedDate;
    }

    @JsonProperty("requestedDate")
    public void setRequestedDate(@Nullable String requestedDate) {
        this.requestedDate = requestedDate;
    }

    @JsonProperty("available")
    public @Nullable Boolean getAvailable() {
        return available;
    }

    @JsonProperty("available")
    public void setAvailable(@Nullable Boolean available) {
        this.available = available;
    }

    @JsonProperty("markedAsAvailable")
    public @Nullable Object getMarkedAsAvailable() {
        return markedAsAvailable;
    }

    @JsonProperty("markedAsAvailable")
    public void setMarkedAsAvailable(@Nullable Object markedAsAvailable) {
        this.markedAsAvailable = markedAsAvailable;
    }

    @JsonProperty("requestedUserId")
    public @Nullable String getRequestedUserId() {
        return requestedUserId;
    }

    @JsonProperty("requestedUserId")
    public void setRequestedUserId(@Nullable String requestedUserId) {
        this.requestedUserId = requestedUserId;
    }

    @JsonProperty("denied")
    public @Nullable Boolean getDenied() {
        return denied;
    }

    @JsonProperty("denied")
    public void setDenied(@Nullable Boolean denied) {
        this.denied = denied;
    }

    @JsonProperty("markedAsDenied")
    public @Nullable String getMarkedAsDenied() {
        return markedAsDenied;
    }

    @JsonProperty("markedAsDenied")
    public void setMarkedAsDenied(@Nullable String markedAsDenied) {
        this.markedAsDenied = markedAsDenied;
    }

    @JsonProperty("deniedReason")
    public @Nullable Object getDeniedReason() {
        return deniedReason;
    }

    @JsonProperty("deniedReason")
    public void setDeniedReason(@Nullable Object deniedReason) {
        this.deniedReason = deniedReason;
    }

    @JsonProperty("requestType")
    public @Nullable Integer getRequestType() {
        return requestType;
    }

    @JsonProperty("requestType")
    public void setRequestType(@Nullable Integer requestType) {
        this.requestType = requestType;
    }

    @JsonProperty("requestedByAlias")
    public @Nullable Object getRequestedByAlias() {
        return requestedByAlias;
    }

    @JsonProperty("requestedByAlias")
    public void setRequestedByAlias(@Nullable Object requestedByAlias) {
        this.requestedByAlias = requestedByAlias;
    }

    @JsonProperty("requestedUser")
    public @Nullable User getRequestedUser() {
        return requestedUser;
    }

    @JsonProperty("requestedUser")
    public void setRequestedUser(@Nullable User requestedUser) {
        this.requestedUser = requestedUser;
    }

    @JsonProperty("source")
    public @Nullable Integer getSource() {
        return source;
    }

    @JsonProperty("source")
    public void setSource(@Nullable Integer source) {
        this.source = source;
    }

    @JsonProperty("id")
    public @Nullable Integer getId() {
        return id;
    }

    @JsonProperty("id")
    public void setId(@Nullable Integer id) {
        this.id = id;
    }

    @JsonAnyGetter
    public Map<String, Object> getAdditionalProperties() {
        return this.additionalProperties;
    }

    @JsonAnySetter
    public void setAdditionalProperty(String name, Object value) {
        this.additionalProperties.put(name, value);
    }
}
