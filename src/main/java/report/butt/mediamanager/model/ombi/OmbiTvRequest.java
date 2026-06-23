package report.butt.mediamanager.model.ombi;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@JsonIgnoreProperties(ignoreUnknown = true)
@NullMarked
public class OmbiTvRequest {

    @JsonProperty("id")
    private @Nullable Integer id;

    @JsonProperty("tvDbId")
    private @Nullable Integer tvDbId;

    @JsonProperty("externalProviderId")
    private @Nullable Integer externalProviderId;

    @JsonProperty("imdbId")
    private @Nullable String imdbId;

    @JsonProperty("qualityOverride")
    private @Nullable Integer qualityOverride;

    @JsonProperty("rootFolder")
    private @Nullable Integer rootFolder;

    @JsonProperty("languageProfile")
    private @Nullable Integer languageProfile;

    @JsonProperty("overview")
    private @Nullable String overview;

    @JsonProperty("title")
    private @Nullable String title;

    @JsonProperty("posterPath")
    private @Nullable String posterPath;

    @JsonProperty("background")
    private @Nullable String background;

    @JsonProperty("releaseDate")
    private @Nullable String releaseDate;

    @JsonProperty("status")
    private @Nullable String status;

    @JsonProperty("totalSeasons")
    private @Nullable Integer totalSeasons;

    @JsonProperty("childRequests")
    private @Nullable List<OmbiTvChildRequest> childRequests;

    public @Nullable Integer getId() {
        return id;
    }

    public void setId(@Nullable Integer id) {
        this.id = id;
    }

    public @Nullable Integer getTvDbId() {
        return tvDbId;
    }

    public void setTvDbId(@Nullable Integer tvDbId) {
        this.tvDbId = tvDbId;
    }

    public @Nullable Integer getExternalProviderId() {
        return externalProviderId;
    }

    public void setExternalProviderId(@Nullable Integer externalProviderId) {
        this.externalProviderId = externalProviderId;
    }

    public @Nullable String getImdbId() {
        return imdbId;
    }

    public void setImdbId(@Nullable String imdbId) {
        this.imdbId = imdbId;
    }

    public @Nullable Integer getQualityOverride() {
        return qualityOverride;
    }

    public void setQualityOverride(@Nullable Integer qualityOverride) {
        this.qualityOverride = qualityOverride;
    }

    public @Nullable Integer getRootFolder() {
        return rootFolder;
    }

    public void setRootFolder(@Nullable Integer rootFolder) {
        this.rootFolder = rootFolder;
    }

    public @Nullable Integer getLanguageProfile() {
        return languageProfile;
    }

    public void setLanguageProfile(@Nullable Integer languageProfile) {
        this.languageProfile = languageProfile;
    }

    public @Nullable String getOverview() {
        return overview;
    }

    public void setOverview(@Nullable String overview) {
        this.overview = overview;
    }

    public @Nullable String getTitle() {
        return title;
    }

    public void setTitle(@Nullable String title) {
        this.title = title;
    }

    public @Nullable String getPosterPath() {
        return posterPath;
    }

    public void setPosterPath(@Nullable String posterPath) {
        this.posterPath = posterPath;
    }

    public @Nullable String getBackground() {
        return background;
    }

    public void setBackground(@Nullable String background) {
        this.background = background;
    }

    public @Nullable String getReleaseDate() {
        return releaseDate;
    }

    public void setReleaseDate(@Nullable String releaseDate) {
        this.releaseDate = releaseDate;
    }

    public @Nullable String getStatus() {
        return status;
    }

    public void setStatus(@Nullable String status) {
        this.status = status;
    }

    public @Nullable Integer getTotalSeasons() {
        return totalSeasons;
    }

    public void setTotalSeasons(@Nullable Integer totalSeasons) {
        this.totalSeasons = totalSeasons;
    }

    public @Nullable List<OmbiTvChildRequest> getChildRequests() {
        return childRequests;
    }

    public void setChildRequests(@Nullable List<OmbiTvChildRequest> childRequests) {
        this.childRequests = childRequests;
    }
}
