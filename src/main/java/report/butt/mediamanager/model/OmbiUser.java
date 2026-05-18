package report.butt.mediamanager.model;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.processing.Generated;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
    "alias",
    "userType",
    "providerUserId",
    "lastLoggedIn",
    "language",
    "streamingCountry",
    "movieRequestLimit",
    "episodeRequestLimit",
    "musicRequestLimit",
    "movieRequestLimitType",
    "episodeRequestLimitType",
    "musicRequestLimitType",
    "userAccessToken",
    "mediaServerToken",
    "notificationUserIds",
    "userNotificationPreferences",
    "isEmbyConnect",
    "userAlias",
    "emailLogin",
    "isSystemUser",
    "id",
    "userName",
    "normalizedUserName",
    "email",
    "normalizedEmail",
    "emailConfirmed",
    "phoneNumber",
    "phoneNumberConfirmed",
    "twoFactorEnabled",
    "lockoutEnd",
    "lockoutEnabled",
    "accessFailedCount"
})
@Generated("jsonschema2pojo")
public class OmbiUser implements Serializable {

  @JsonProperty("alias")
  private String alias;
  @JsonProperty("userType")
  private Integer userType;
  @JsonProperty("providerUserId")
  private String providerUserId;
  @JsonProperty("lastLoggedIn")
  private String lastLoggedIn;
  @JsonProperty("language")
  private Object language;
  @JsonProperty("streamingCountry")
  private String streamingCountry;
  @JsonProperty("movieRequestLimit")
  private Integer movieRequestLimit;
  @JsonProperty("episodeRequestLimit")
  private Integer episodeRequestLimit;
  @JsonProperty("musicRequestLimit")
  private Integer musicRequestLimit;
  @JsonProperty("movieRequestLimitType")
  private Object movieRequestLimitType;
  @JsonProperty("episodeRequestLimitType")
  private Object episodeRequestLimitType;
  @JsonProperty("musicRequestLimitType")
  private Object musicRequestLimitType;
  @JsonProperty("userAccessToken")
  private String userAccessToken;
  @JsonProperty("mediaServerToken")
  private Object mediaServerToken;
  @JsonProperty("notificationUserIds")
  private List<Object> notificationUserIds;
  @JsonProperty("userNotificationPreferences")
  private Object userNotificationPreferences;
  @JsonProperty("isEmbyConnect")
  private Boolean isEmbyConnect;
  @JsonProperty("userAlias")
  private String userAlias;
  @JsonProperty("emailLogin")
  private Boolean emailLogin;
  @JsonProperty("isSystemUser")
  private Boolean isSystemUser;
  @JsonProperty("id")
  private String id;
  @JsonProperty("userName")
  private String userName;
  @JsonProperty("normalizedUserName")
  private String normalizedUserName;
  @JsonProperty("email")
  private String email;
  @JsonProperty("normalizedEmail")
  private String normalizedEmail;
  @JsonProperty("emailConfirmed")
  private Boolean emailConfirmed;
  @JsonProperty("phoneNumber")
  private Object phoneNumber;
  @JsonProperty("phoneNumberConfirmed")
  private Boolean phoneNumberConfirmed;
  @JsonProperty("twoFactorEnabled")
  private Boolean twoFactorEnabled;
  @JsonProperty("lockoutEnd")
  private Object lockoutEnd;
  @JsonProperty("lockoutEnabled")
  private Boolean lockoutEnabled;
  @JsonProperty("accessFailedCount")
  private Integer accessFailedCount;
  @JsonIgnore
  private Map<String, Object> additionalProperties = new LinkedHashMap<String, Object>();
  private final static long serialVersionUID = -4038896577704940927L;

  @JsonProperty("alias")
  public String getAlias() {
    return alias;
  }

  @JsonProperty("alias")
  public void setAlias(String alias) {
    this.alias = alias;
  }

  @JsonProperty("userType")
  public Integer getUserType() {
    return userType;
  }

  @JsonProperty("userType")
  public void setUserType(Integer userType) {
    this.userType = userType;
  }

  @JsonProperty("providerUserId")
  public String getProviderUserId() {
    return providerUserId;
  }

  @JsonProperty("providerUserId")
  public void setProviderUserId(String providerUserId) {
    this.providerUserId = providerUserId;
  }

  @JsonProperty("lastLoggedIn")
  public String getLastLoggedIn() {
    return lastLoggedIn;
  }

  @JsonProperty("lastLoggedIn")
  public void setLastLoggedIn(String lastLoggedIn) {
    this.lastLoggedIn = lastLoggedIn;
  }

  @JsonProperty("language")
  public Object getLanguage() {
    return language;
  }

  @JsonProperty("language")
  public void setLanguage(Object language) {
    this.language = language;
  }

  @JsonProperty("streamingCountry")
  public String getStreamingCountry() {
    return streamingCountry;
  }

  @JsonProperty("streamingCountry")
  public void setStreamingCountry(String streamingCountry) {
    this.streamingCountry = streamingCountry;
  }

  @JsonProperty("movieRequestLimit")
  public Integer getMovieRequestLimit() {
    return movieRequestLimit;
  }

  @JsonProperty("movieRequestLimit")
  public void setMovieRequestLimit(Integer movieRequestLimit) {
    this.movieRequestLimit = movieRequestLimit;
  }

  @JsonProperty("episodeRequestLimit")
  public Integer getEpisodeRequestLimit() {
    return episodeRequestLimit;
  }

  @JsonProperty("episodeRequestLimit")
  public void setEpisodeRequestLimit(Integer episodeRequestLimit) {
    this.episodeRequestLimit = episodeRequestLimit;
  }

  @JsonProperty("musicRequestLimit")
  public Integer getMusicRequestLimit() {
    return musicRequestLimit;
  }

  @JsonProperty("musicRequestLimit")
  public void setMusicRequestLimit(Integer musicRequestLimit) {
    this.musicRequestLimit = musicRequestLimit;
  }

  @JsonProperty("movieRequestLimitType")
  public Object getMovieRequestLimitType() {
    return movieRequestLimitType;
  }

  @JsonProperty("movieRequestLimitType")
  public void setMovieRequestLimitType(Object movieRequestLimitType) {
    this.movieRequestLimitType = movieRequestLimitType;
  }

  @JsonProperty("episodeRequestLimitType")
  public Object getEpisodeRequestLimitType() {
    return episodeRequestLimitType;
  }

  @JsonProperty("episodeRequestLimitType")
  public void setEpisodeRequestLimitType(Object episodeRequestLimitType) {
    this.episodeRequestLimitType = episodeRequestLimitType;
  }

  @JsonProperty("musicRequestLimitType")
  public Object getMusicRequestLimitType() {
    return musicRequestLimitType;
  }

  @JsonProperty("musicRequestLimitType")
  public void setMusicRequestLimitType(Object musicRequestLimitType) {
    this.musicRequestLimitType = musicRequestLimitType;
  }

  @JsonProperty("userAccessToken")
  public String getUserAccessToken() {
    return userAccessToken;
  }

  @JsonProperty("userAccessToken")
  public void setUserAccessToken(String userAccessToken) {
    this.userAccessToken = userAccessToken;
  }

  @JsonProperty("mediaServerToken")
  public Object getMediaServerToken() {
    return mediaServerToken;
  }

  @JsonProperty("mediaServerToken")
  public void setMediaServerToken(Object mediaServerToken) {
    this.mediaServerToken = mediaServerToken;
  }

  @JsonProperty("notificationUserIds")
  public List<Object> getNotificationUserIds() {
    return notificationUserIds;
  }

  @JsonProperty("notificationUserIds")
  public void setNotificationUserIds(List<Object> notificationUserIds) {
    this.notificationUserIds = notificationUserIds;
  }

  @JsonProperty("userNotificationPreferences")
  public Object getUserNotificationPreferences() {
    return userNotificationPreferences;
  }

  @JsonProperty("userNotificationPreferences")
  public void setUserNotificationPreferences(Object userNotificationPreferences) {
    this.userNotificationPreferences = userNotificationPreferences;
  }

  @JsonProperty("isEmbyConnect")
  public Boolean getIsEmbyConnect() {
    return isEmbyConnect;
  }

  @JsonProperty("isEmbyConnect")
  public void setIsEmbyConnect(Boolean isEmbyConnect) {
    this.isEmbyConnect = isEmbyConnect;
  }

  @JsonProperty("userAlias")
  public String getUserAlias() {
    return userAlias;
  }

  @JsonProperty("userAlias")
  public void setUserAlias(String userAlias) {
    this.userAlias = userAlias;
  }

  @JsonProperty("emailLogin")
  public Boolean getEmailLogin() {
    return emailLogin;
  }

  @JsonProperty("emailLogin")
  public void setEmailLogin(Boolean emailLogin) {
    this.emailLogin = emailLogin;
  }

  @JsonProperty("isSystemUser")
  public Boolean getIsSystemUser() {
    return isSystemUser;
  }

  @JsonProperty("isSystemUser")
  public void setIsSystemUser(Boolean isSystemUser) {
    this.isSystemUser = isSystemUser;
  }

  @JsonProperty("id")
  public String getId() {
    return id;
  }

  @JsonProperty("id")
  public void setId(String id) {
    this.id = id;
  }

  @JsonProperty("userName")
  public String getUserName() {
    return userName;
  }

  @JsonProperty("userName")
  public void setUserName(String userName) {
    this.userName = userName;
  }

  @JsonProperty("normalizedUserName")
  public String getNormalizedUserName() {
    return normalizedUserName;
  }

  @JsonProperty("normalizedUserName")
  public void setNormalizedUserName(String normalizedUserName) {
    this.normalizedUserName = normalizedUserName;
  }

  @JsonProperty("email")
  public String getEmail() {
    return email;
  }

  @JsonProperty("email")
  public void setEmail(String email) {
    this.email = email;
  }

  @JsonProperty("normalizedEmail")
  public String getNormalizedEmail() {
    return normalizedEmail;
  }

  @JsonProperty("normalizedEmail")
  public void setNormalizedEmail(String normalizedEmail) {
    this.normalizedEmail = normalizedEmail;
  }

  @JsonProperty("emailConfirmed")
  public Boolean getEmailConfirmed() {
    return emailConfirmed;
  }

  @JsonProperty("emailConfirmed")
  public void setEmailConfirmed(Boolean emailConfirmed) {
    this.emailConfirmed = emailConfirmed;
  }

  @JsonProperty("phoneNumber")
  public Object getPhoneNumber() {
    return phoneNumber;
  }

  @JsonProperty("phoneNumber")
  public void setPhoneNumber(Object phoneNumber) {
    this.phoneNumber = phoneNumber;
  }

  @JsonProperty("phoneNumberConfirmed")
  public Boolean getPhoneNumberConfirmed() {
    return phoneNumberConfirmed;
  }

  @JsonProperty("phoneNumberConfirmed")
  public void setPhoneNumberConfirmed(Boolean phoneNumberConfirmed) {
    this.phoneNumberConfirmed = phoneNumberConfirmed;
  }

  @JsonProperty("twoFactorEnabled")
  public Boolean getTwoFactorEnabled() {
    return twoFactorEnabled;
  }

  @JsonProperty("twoFactorEnabled")
  public void setTwoFactorEnabled(Boolean twoFactorEnabled) {
    this.twoFactorEnabled = twoFactorEnabled;
  }

  @JsonProperty("lockoutEnd")
  public Object getLockoutEnd() {
    return lockoutEnd;
  }

  @JsonProperty("lockoutEnd")
  public void setLockoutEnd(Object lockoutEnd) {
    this.lockoutEnd = lockoutEnd;
  }

  @JsonProperty("lockoutEnabled")
  public Boolean getLockoutEnabled() {
    return lockoutEnabled;
  }

  @JsonProperty("lockoutEnabled")
  public void setLockoutEnabled(Boolean lockoutEnabled) {
    this.lockoutEnabled = lockoutEnabled;
  }

  @JsonProperty("accessFailedCount")
  public Integer getAccessFailedCount() {
    return accessFailedCount;
  }

  @JsonProperty("accessFailedCount")
  public void setAccessFailedCount(Integer accessFailedCount) {
    this.accessFailedCount = accessFailedCount;
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
