package report.butt.mediamanager.model.ombi;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.processing.Generated;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

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
@NullMarked
public class User implements Serializable {

    @JsonProperty("alias")
    private @Nullable String alias;

    @JsonProperty("userType")
    private @Nullable Integer userType;

    @JsonProperty("providerUserId")
    private @Nullable String providerUserId;

    @JsonProperty("lastLoggedIn")
    private @Nullable String lastLoggedIn;

    @JsonProperty("language")
    private @Nullable Object language;

    @JsonProperty("streamingCountry")
    private @Nullable String streamingCountry;

    @JsonProperty("movieRequestLimit")
    private @Nullable Integer movieRequestLimit;

    @JsonProperty("episodeRequestLimit")
    private @Nullable Integer episodeRequestLimit;

    @JsonProperty("musicRequestLimit")
    private @Nullable Integer musicRequestLimit;

    @JsonProperty("movieRequestLimitType")
    private @Nullable Object movieRequestLimitType;

    @JsonProperty("episodeRequestLimitType")
    private @Nullable Object episodeRequestLimitType;

    @JsonProperty("musicRequestLimitType")
    private @Nullable Object musicRequestLimitType;

    @JsonProperty("userAccessToken")
    private @Nullable String userAccessToken;

    @JsonProperty("mediaServerToken")
    private @Nullable Object mediaServerToken;

    @JsonProperty("notificationUserIds")
    private @Nullable List<Object> notificationUserIds;

    @JsonProperty("userNotificationPreferences")
    private @Nullable Object userNotificationPreferences;

    @JsonProperty("isEmbyConnect")
    private @Nullable Boolean isEmbyConnect;

    @JsonProperty("userAlias")
    private @Nullable String userAlias;

    @JsonProperty("emailLogin")
    private @Nullable Boolean emailLogin;

    @JsonProperty("isSystemUser")
    private @Nullable Boolean isSystemUser;

    @JsonProperty("id")
    private @Nullable String id;

    @JsonProperty("userName")
    private @Nullable String userName;

    @JsonProperty("normalizedUserName")
    private @Nullable String normalizedUserName;

    @JsonProperty("email")
    private @Nullable String email;

    @JsonProperty("normalizedEmail")
    private @Nullable String normalizedEmail;

    @JsonProperty("emailConfirmed")
    private @Nullable Boolean emailConfirmed;

    @JsonProperty("phoneNumber")
    private @Nullable Object phoneNumber;

    @JsonProperty("phoneNumberConfirmed")
    private @Nullable Boolean phoneNumberConfirmed;

    @JsonProperty("twoFactorEnabled")
    private @Nullable Boolean twoFactorEnabled;

    @JsonProperty("lockoutEnd")
    private @Nullable Object lockoutEnd;

    @JsonProperty("lockoutEnabled")
    private @Nullable Boolean lockoutEnabled;

    @JsonProperty("accessFailedCount")
    private @Nullable Integer accessFailedCount;

    @JsonIgnore
    private Map<String, Object> additionalProperties = new LinkedHashMap<String, Object>();

    private static final long serialVersionUID = -4038896577704940927L;

    @JsonProperty("alias")
    public @Nullable String getAlias() {
        return alias;
    }

    @JsonProperty("alias")
    public void setAlias(@Nullable String alias) {
        this.alias = alias;
    }

    @JsonProperty("userType")
    public @Nullable Integer getUserType() {
        return userType;
    }

    @JsonProperty("userType")
    public void setUserType(@Nullable Integer userType) {
        this.userType = userType;
    }

    @JsonProperty("providerUserId")
    public @Nullable String getProviderUserId() {
        return providerUserId;
    }

    @JsonProperty("providerUserId")
    public void setProviderUserId(@Nullable String providerUserId) {
        this.providerUserId = providerUserId;
    }

    @JsonProperty("lastLoggedIn")
    public @Nullable String getLastLoggedIn() {
        return lastLoggedIn;
    }

    @JsonProperty("lastLoggedIn")
    public void setLastLoggedIn(@Nullable String lastLoggedIn) {
        this.lastLoggedIn = lastLoggedIn;
    }

    @JsonProperty("language")
    public @Nullable Object getLanguage() {
        return language;
    }

    @JsonProperty("language")
    public void setLanguage(@Nullable Object language) {
        this.language = language;
    }

    @JsonProperty("streamingCountry")
    public @Nullable String getStreamingCountry() {
        return streamingCountry;
    }

    @JsonProperty("streamingCountry")
    public void setStreamingCountry(@Nullable String streamingCountry) {
        this.streamingCountry = streamingCountry;
    }

    @JsonProperty("movieRequestLimit")
    public @Nullable Integer getMovieRequestLimit() {
        return movieRequestLimit;
    }

    @JsonProperty("movieRequestLimit")
    public void setMovieRequestLimit(@Nullable Integer movieRequestLimit) {
        this.movieRequestLimit = movieRequestLimit;
    }

    @JsonProperty("episodeRequestLimit")
    public @Nullable Integer getEpisodeRequestLimit() {
        return episodeRequestLimit;
    }

    @JsonProperty("episodeRequestLimit")
    public void setEpisodeRequestLimit(@Nullable Integer episodeRequestLimit) {
        this.episodeRequestLimit = episodeRequestLimit;
    }

    @JsonProperty("musicRequestLimit")
    public @Nullable Integer getMusicRequestLimit() {
        return musicRequestLimit;
    }

    @JsonProperty("musicRequestLimit")
    public void setMusicRequestLimit(@Nullable Integer musicRequestLimit) {
        this.musicRequestLimit = musicRequestLimit;
    }

    @JsonProperty("movieRequestLimitType")
    public @Nullable Object getMovieRequestLimitType() {
        return movieRequestLimitType;
    }

    @JsonProperty("movieRequestLimitType")
    public void setMovieRequestLimitType(@Nullable Object movieRequestLimitType) {
        this.movieRequestLimitType = movieRequestLimitType;
    }

    @JsonProperty("episodeRequestLimitType")
    public @Nullable Object getEpisodeRequestLimitType() {
        return episodeRequestLimitType;
    }

    @JsonProperty("episodeRequestLimitType")
    public void setEpisodeRequestLimitType(@Nullable Object episodeRequestLimitType) {
        this.episodeRequestLimitType = episodeRequestLimitType;
    }

    @JsonProperty("musicRequestLimitType")
    public @Nullable Object getMusicRequestLimitType() {
        return musicRequestLimitType;
    }

    @JsonProperty("musicRequestLimitType")
    public void setMusicRequestLimitType(@Nullable Object musicRequestLimitType) {
        this.musicRequestLimitType = musicRequestLimitType;
    }

    @JsonProperty("userAccessToken")
    public @Nullable String getUserAccessToken() {
        return userAccessToken;
    }

    @JsonProperty("userAccessToken")
    public void setUserAccessToken(@Nullable String userAccessToken) {
        this.userAccessToken = userAccessToken;
    }

    @JsonProperty("mediaServerToken")
    public @Nullable Object getMediaServerToken() {
        return mediaServerToken;
    }

    @JsonProperty("mediaServerToken")
    public void setMediaServerToken(@Nullable Object mediaServerToken) {
        this.mediaServerToken = mediaServerToken;
    }

    @JsonProperty("notificationUserIds")
    public @Nullable List<Object> getNotificationUserIds() {
        return notificationUserIds;
    }

    @JsonProperty("notificationUserIds")
    public void setNotificationUserIds(@Nullable List<Object> notificationUserIds) {
        this.notificationUserIds = notificationUserIds;
    }

    @JsonProperty("userNotificationPreferences")
    public @Nullable Object getUserNotificationPreferences() {
        return userNotificationPreferences;
    }

    @JsonProperty("userNotificationPreferences")
    public void setUserNotificationPreferences(@Nullable Object userNotificationPreferences) {
        this.userNotificationPreferences = userNotificationPreferences;
    }

    @JsonProperty("isEmbyConnect")
    public @Nullable Boolean getIsEmbyConnect() {
        return isEmbyConnect;
    }

    @JsonProperty("isEmbyConnect")
    public void setIsEmbyConnect(@Nullable Boolean isEmbyConnect) {
        this.isEmbyConnect = isEmbyConnect;
    }

    @JsonProperty("userAlias")
    public @Nullable String getUserAlias() {
        return userAlias;
    }

    @JsonProperty("userAlias")
    public void setUserAlias(@Nullable String userAlias) {
        this.userAlias = userAlias;
    }

    @JsonProperty("emailLogin")
    public @Nullable Boolean getEmailLogin() {
        return emailLogin;
    }

    @JsonProperty("emailLogin")
    public void setEmailLogin(@Nullable Boolean emailLogin) {
        this.emailLogin = emailLogin;
    }

    @JsonProperty("isSystemUser")
    public @Nullable Boolean getIsSystemUser() {
        return isSystemUser;
    }

    @JsonProperty("isSystemUser")
    public void setIsSystemUser(@Nullable Boolean isSystemUser) {
        this.isSystemUser = isSystemUser;
    }

    @JsonProperty("id")
    public @Nullable String getId() {
        return id;
    }

    @JsonProperty("id")
    public void setId(@Nullable String id) {
        this.id = id;
    }

    @JsonProperty("userName")
    public @Nullable String getUserName() {
        return userName;
    }

    @JsonProperty("userName")
    public void setUserName(@Nullable String userName) {
        this.userName = userName;
    }

    @JsonProperty("normalizedUserName")
    public @Nullable String getNormalizedUserName() {
        return normalizedUserName;
    }

    @JsonProperty("normalizedUserName")
    public void setNormalizedUserName(@Nullable String normalizedUserName) {
        this.normalizedUserName = normalizedUserName;
    }

    @JsonProperty("email")
    public @Nullable String getEmail() {
        return email;
    }

    @JsonProperty("email")
    public void setEmail(@Nullable String email) {
        this.email = email;
    }

    @JsonProperty("normalizedEmail")
    public @Nullable String getNormalizedEmail() {
        return normalizedEmail;
    }

    @JsonProperty("normalizedEmail")
    public void setNormalizedEmail(@Nullable String normalizedEmail) {
        this.normalizedEmail = normalizedEmail;
    }

    @JsonProperty("emailConfirmed")
    public @Nullable Boolean getEmailConfirmed() {
        return emailConfirmed;
    }

    @JsonProperty("emailConfirmed")
    public void setEmailConfirmed(@Nullable Boolean emailConfirmed) {
        this.emailConfirmed = emailConfirmed;
    }

    @JsonProperty("phoneNumber")
    public @Nullable Object getPhoneNumber() {
        return phoneNumber;
    }

    @JsonProperty("phoneNumber")
    public void setPhoneNumber(@Nullable Object phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    @JsonProperty("phoneNumberConfirmed")
    public @Nullable Boolean getPhoneNumberConfirmed() {
        return phoneNumberConfirmed;
    }

    @JsonProperty("phoneNumberConfirmed")
    public void setPhoneNumberConfirmed(@Nullable Boolean phoneNumberConfirmed) {
        this.phoneNumberConfirmed = phoneNumberConfirmed;
    }

    @JsonProperty("twoFactorEnabled")
    public @Nullable Boolean getTwoFactorEnabled() {
        return twoFactorEnabled;
    }

    @JsonProperty("twoFactorEnabled")
    public void setTwoFactorEnabled(@Nullable Boolean twoFactorEnabled) {
        this.twoFactorEnabled = twoFactorEnabled;
    }

    @JsonProperty("lockoutEnd")
    public @Nullable Object getLockoutEnd() {
        return lockoutEnd;
    }

    @JsonProperty("lockoutEnd")
    public void setLockoutEnd(@Nullable Object lockoutEnd) {
        this.lockoutEnd = lockoutEnd;
    }

    @JsonProperty("lockoutEnabled")
    public @Nullable Boolean getLockoutEnabled() {
        return lockoutEnabled;
    }

    @JsonProperty("lockoutEnabled")
    public void setLockoutEnabled(@Nullable Boolean lockoutEnabled) {
        this.lockoutEnabled = lockoutEnabled;
    }

    @JsonProperty("accessFailedCount")
    public @Nullable Integer getAccessFailedCount() {
        return accessFailedCount;
    }

    @JsonProperty("accessFailedCount")
    public void setAccessFailedCount(@Nullable Integer accessFailedCount) {
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
