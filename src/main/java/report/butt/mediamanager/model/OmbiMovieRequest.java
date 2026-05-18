package report.butt.mediamanager.model;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

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
public class OmbiMovieRequest implements Serializable {
  @JsonProperty("theMovieDbId")
  private Integer theMovieDbId;
  @JsonProperty("issueId")
  private Object issueId;
  @JsonProperty("issues")
  private Object issues;
  @JsonProperty("subscribed")
  private Boolean subscribed;
  @JsonProperty("showSubscribe")
  private Boolean showSubscribe;
  @JsonProperty("is4kRequest")
  private Boolean is4kRequest;
  @JsonProperty("rootPathOverride")
  private Integer rootPathOverride;
  @JsonProperty("qualityOverride")
  private Integer qualityOverride;
  @JsonProperty("has4KRequest")
  private Boolean has4KRequest;
  @JsonProperty("approved4K")
  private Boolean approved4K;
  @JsonProperty("markedAsApproved4K")
  private String markedAsApproved4K;
  @JsonProperty("requestedDate4k")
  private String requestedDate4k;
  @JsonProperty("available4K")
  private Boolean available4K;
  @JsonProperty("markedAsAvailable4K")
  private Object markedAsAvailable4K;
  @JsonProperty("denied4K")
  private Object denied4K;
  @JsonProperty("markedAsDenied4K")
  private String markedAsDenied4K;
  @JsonProperty("deniedReason4K")
  private Object deniedReason4K;
  @JsonProperty("requestCombination")
  private Integer requestCombination;
  @JsonProperty("langCode")
  private String langCode;
  @JsonProperty("requestStatus")
  private String requestStatus;
  @JsonProperty("canApprove")
  private Boolean canApprove;
  @JsonProperty("watchedByRequestedUser")
  private Boolean watchedByRequestedUser;
  @JsonProperty("playedByUsersCount")
  private Integer playedByUsersCount;
  @JsonProperty("imdbId")
  private Object imdbId;
  @JsonProperty("overview")
  private String overview;
  @JsonProperty("posterPath")
  private Object posterPath;
  @JsonProperty("releaseDate")
  private String releaseDate;
  @JsonProperty("digitalReleaseDate")
  private Object digitalReleaseDate;
  @JsonProperty("status")
  private String status;
  @JsonProperty("background")
  private Object background;
  @JsonProperty("released")
  private Boolean released;
  @JsonProperty("digitalRelease")
  private Boolean digitalRelease;
  @JsonProperty("title")
  private String title;
  @JsonProperty("approved")
  private Boolean approved;
  @JsonProperty("markedAsApproved")
  private String markedAsApproved;
  @JsonProperty("requestedDate")
  private String requestedDate;
  @JsonProperty("available")
  private Boolean available;
  @JsonProperty("markedAsAvailable")
  private Object markedAsAvailable;
  @JsonProperty("requestedUserId")
  private String requestedUserId;
  @JsonProperty("denied")
  private Boolean denied;
  @JsonProperty("markedAsDenied")
  private String markedAsDenied;
  @JsonProperty("deniedReason")
  private Object deniedReason;
  @JsonProperty("requestType")
  private Integer requestType;
  @JsonProperty("requestedByAlias")
  private Object requestedByAlias;
  @JsonProperty("requestedUser")
  private OmbiUser requestedUser;
  @JsonProperty("source")
  private Integer source;
  @JsonProperty("id")
  private Integer id;
  @JsonIgnore
  private Map<String, Object> additionalProperties = new LinkedHashMap<String, Object>();
  private final static long serialVersionUID = 6356458637734283408L;

  @JsonProperty("theMovieDbId")
  public Integer getTheMovieDbId() {
    return theMovieDbId;
  }

  @JsonProperty("theMovieDbId")
  public void setTheMovieDbId(Integer theMovieDbId) {
    this.theMovieDbId = theMovieDbId;
  }

  @JsonProperty("issueId")
  public Object getIssueId() {
    return issueId;
  }

  @JsonProperty("issueId")
  public void setIssueId(Object issueId) {
    this.issueId = issueId;
  }

  @JsonProperty("issues")
  public Object getIssues() {
    return issues;
  }

  @JsonProperty("issues")
  public void setIssues(Object issues) {
    this.issues = issues;
  }

  @JsonProperty("subscribed")
  public Boolean getSubscribed() {
    return subscribed;
  }

  @JsonProperty("subscribed")
  public void setSubscribed(Boolean subscribed) {
    this.subscribed = subscribed;
  }

  @JsonProperty("showSubscribe")
  public Boolean getShowSubscribe() {
    return showSubscribe;
  }

  @JsonProperty("showSubscribe")
  public void setShowSubscribe(Boolean showSubscribe) {
    this.showSubscribe = showSubscribe;
  }

  @JsonProperty("is4kRequest")
  public Boolean getIs4kRequest() {
    return is4kRequest;
  }

  @JsonProperty("is4kRequest")
  public void setIs4kRequest(Boolean is4kRequest) {
    this.is4kRequest = is4kRequest;
  }

  @JsonProperty("rootPathOverride")
  public Integer getRootPathOverride() {
    return rootPathOverride;
  }

  @JsonProperty("rootPathOverride")
  public void setRootPathOverride(Integer rootPathOverride) {
    this.rootPathOverride = rootPathOverride;
  }

  @JsonProperty("qualityOverride")
  public Integer getQualityOverride() {
    return qualityOverride;
  }

  @JsonProperty("qualityOverride")
  public void setQualityOverride(Integer qualityOverride) {
    this.qualityOverride = qualityOverride;
  }

  @JsonProperty("has4KRequest")
  public Boolean getHas4KRequest() {
    return has4KRequest;
  }

  @JsonProperty("has4KRequest")
  public void setHas4KRequest(Boolean has4KRequest) {
    this.has4KRequest = has4KRequest;
  }

  @JsonProperty("approved4K")
  public Boolean getApproved4K() {
    return approved4K;
  }

  @JsonProperty("approved4K")
  public void setApproved4K(Boolean approved4K) {
    this.approved4K = approved4K;
  }

  @JsonProperty("markedAsApproved4K")
  public String getMarkedAsApproved4K() {
    return markedAsApproved4K;
  }

  @JsonProperty("markedAsApproved4K")
  public void setMarkedAsApproved4K(String markedAsApproved4K) {
    this.markedAsApproved4K = markedAsApproved4K;
  }

  @JsonProperty("requestedDate4k")
  public String getRequestedDate4k() {
    return requestedDate4k;
  }

  @JsonProperty("requestedDate4k")
  public void setRequestedDate4k(String requestedDate4k) {
    this.requestedDate4k = requestedDate4k;
  }

  @JsonProperty("available4K")
  public Boolean getAvailable4K() {
    return available4K;
  }

  @JsonProperty("available4K")
  public void setAvailable4K(Boolean available4K) {
    this.available4K = available4K;
  }

  @JsonProperty("markedAsAvailable4K")
  public Object getMarkedAsAvailable4K() {
    return markedAsAvailable4K;
  }

  @JsonProperty("markedAsAvailable4K")
  public void setMarkedAsAvailable4K(Object markedAsAvailable4K) {
    this.markedAsAvailable4K = markedAsAvailable4K;
  }

  @JsonProperty("denied4K")
  public Object getDenied4K() {
    return denied4K;
  }

  @JsonProperty("denied4K")
  public void setDenied4K(Object denied4K) {
    this.denied4K = denied4K;
  }

  @JsonProperty("markedAsDenied4K")
  public String getMarkedAsDenied4K() {
    return markedAsDenied4K;
  }

  @JsonProperty("markedAsDenied4K")
  public void setMarkedAsDenied4K(String markedAsDenied4K) {
    this.markedAsDenied4K = markedAsDenied4K;
  }

  @JsonProperty("deniedReason4K")
  public Object getDeniedReason4K() {
    return deniedReason4K;
  }

  @JsonProperty("deniedReason4K")
  public void setDeniedReason4K(Object deniedReason4K) {
    this.deniedReason4K = deniedReason4K;
  }

  @JsonProperty("requestCombination")
  public Integer getRequestCombination() {
    return requestCombination;
  }

  @JsonProperty("requestCombination")
  public void setRequestCombination(Integer requestCombination) {
    this.requestCombination = requestCombination;
  }

  @JsonProperty("langCode")
  public String getLangCode() {
    return langCode;
  }

  @JsonProperty("langCode")
  public void setLangCode(String langCode) {
    this.langCode = langCode;
  }

  @JsonProperty("requestStatus")
  public String getRequestStatus() {
    return requestStatus;
  }

  @JsonProperty("requestStatus")
  public void setRequestStatus(String requestStatus) {
    this.requestStatus = requestStatus;
  }

  @JsonProperty("canApprove")
  public Boolean getCanApprove() {
    return canApprove;
  }

  @JsonProperty("canApprove")
  public void setCanApprove(Boolean canApprove) {
    this.canApprove = canApprove;
  }

  @JsonProperty("watchedByRequestedUser")
  public Boolean getWatchedByRequestedUser() {
    return watchedByRequestedUser;
  }

  @JsonProperty("watchedByRequestedUser")
  public void setWatchedByRequestedUser(Boolean watchedByRequestedUser) {
    this.watchedByRequestedUser = watchedByRequestedUser;
  }

  @JsonProperty("playedByUsersCount")
  public Integer getPlayedByUsersCount() {
    return playedByUsersCount;
  }

  @JsonProperty("playedByUsersCount")
  public void setPlayedByUsersCount(Integer playedByUsersCount) {
    this.playedByUsersCount = playedByUsersCount;
  }

  @JsonProperty("imdbId")
  public Object getImdbId() {
    return imdbId;
  }

  @JsonProperty("imdbId")
  public void setImdbId(Object imdbId) {
    this.imdbId = imdbId;
  }

  @JsonProperty("overview")
  public String getOverview() {
    return overview;
  }

  @JsonProperty("overview")
  public void setOverview(String overview) {
    this.overview = overview;
  }

  @JsonProperty("posterPath")
  public Object getPosterPath() {
    return posterPath;
  }

  @JsonProperty("posterPath")
  public void setPosterPath(Object posterPath) {
    this.posterPath = posterPath;
  }

  @JsonProperty("releaseDate")
  public String getReleaseDate() {
    return releaseDate;
  }

  @JsonProperty("releaseDate")
  public void setReleaseDate(String releaseDate) {
    this.releaseDate = releaseDate;
  }

  @JsonProperty("digitalReleaseDate")
  public Object getDigitalReleaseDate() {
    return digitalReleaseDate;
  }

  @JsonProperty("digitalReleaseDate")
  public void setDigitalReleaseDate(Object digitalReleaseDate) {
    this.digitalReleaseDate = digitalReleaseDate;
  }

  @JsonProperty("status")
  public String getStatus() {
    return status;
  }

  @JsonProperty("status")
  public void setStatus(String status) {
    this.status = status;
  }

  @JsonProperty("background")
  public Object getBackground() {
    return background;
  }

  @JsonProperty("background")
  public void setBackground(Object background) {
    this.background = background;
  }

  @JsonProperty("released")
  public Boolean getReleased() {
    return released;
  }

  @JsonProperty("released")
  public void setReleased(Boolean released) {
    this.released = released;
  }

  @JsonProperty("digitalRelease")
  public Boolean getDigitalRelease() {
    return digitalRelease;
  }

  @JsonProperty("digitalRelease")
  public void setDigitalRelease(Boolean digitalRelease) {
    this.digitalRelease = digitalRelease;
  }

  @JsonProperty("title")
  public String getTitle() {
    return title;
  }

  @JsonProperty("title")
  public void setTitle(String title) {
    this.title = title;
  }

  @JsonProperty("approved")
  public Boolean getApproved() {
    return approved;
  }

  @JsonProperty("approved")
  public void setApproved(Boolean approved) {
    this.approved = approved;
  }

  @JsonProperty("markedAsApproved")
  public String getMarkedAsApproved() {
    return markedAsApproved;
  }

  @JsonProperty("markedAsApproved")
  public void setMarkedAsApproved(String markedAsApproved) {
    this.markedAsApproved = markedAsApproved;
  }

  @JsonProperty("requestedDate")
  public String getRequestedDate() {
    return requestedDate;
  }

  @JsonProperty("requestedDate")
  public void setRequestedDate(String requestedDate) {
    this.requestedDate = requestedDate;
  }

  @JsonProperty("available")
  public Boolean getAvailable() {
    return available;
  }

  @JsonProperty("available")
  public void setAvailable(Boolean available) {
    this.available = available;
  }

  @JsonProperty("markedAsAvailable")
  public Object getMarkedAsAvailable() {
    return markedAsAvailable;
  }

  @JsonProperty("markedAsAvailable")
  public void setMarkedAsAvailable(Object markedAsAvailable) {
    this.markedAsAvailable = markedAsAvailable;
  }

  @JsonProperty("requestedUserId")
  public String getRequestedUserId() {
    return requestedUserId;
  }

  @JsonProperty("requestedUserId")
  public void setRequestedUserId(String requestedUserId) {
    this.requestedUserId = requestedUserId;
  }

  @JsonProperty("denied")
  public Boolean getDenied() {
    return denied;
  }

  @JsonProperty("denied")
  public void setDenied(Boolean denied) {
    this.denied = denied;
  }

  @JsonProperty("markedAsDenied")
  public String getMarkedAsDenied() {
    return markedAsDenied;
  }

  @JsonProperty("markedAsDenied")
  public void setMarkedAsDenied(String markedAsDenied) {
    this.markedAsDenied = markedAsDenied;
  }

  @JsonProperty("deniedReason")
  public Object getDeniedReason() {
    return deniedReason;
  }

  @JsonProperty("deniedReason")
  public void setDeniedReason(Object deniedReason) {
    this.deniedReason = deniedReason;
  }

  @JsonProperty("requestType")
  public Integer getRequestType() {
    return requestType;
  }

  @JsonProperty("requestType")
  public void setRequestType(Integer requestType) {
    this.requestType = requestType;
  }

  @JsonProperty("requestedByAlias")
  public Object getRequestedByAlias() {
    return requestedByAlias;
  }

  @JsonProperty("requestedByAlias")
  public void setRequestedByAlias(Object requestedByAlias) {
    this.requestedByAlias = requestedByAlias;
  }

  @JsonProperty("requestedUser")
  public OmbiUser getRequestedUser() {
    return requestedUser;
  }

  @JsonProperty("requestedUser")
  public void setRequestedUser(OmbiUser requestedUser) {
    this.requestedUser = requestedUser;
  }

  @JsonProperty("source")
  public Integer getSource() {
    return source;
  }

  @JsonProperty("source")
  public void setSource(Integer source) {
    this.source = source;
  }

  @JsonProperty("id")
  public Integer getId() {
    return id;
  }

  @JsonProperty("id")
  public void setId(Integer id) {
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
