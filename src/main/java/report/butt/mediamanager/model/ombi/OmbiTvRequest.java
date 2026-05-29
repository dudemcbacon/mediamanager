package report.butt.mediamanager.model.ombi;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class OmbiTvRequest {

    @JsonProperty("id")
    private Integer id;

    @JsonProperty("tvDbId")
    private Integer tvDbId;

    @JsonProperty("externalProviderId")
    private Integer externalProviderId;

    @JsonProperty("imdbId")
    private String imdbId;

    @JsonProperty("qualityOverride")
    private Integer qualityOverride;

    @JsonProperty("rootFolder")
    private Integer rootFolder;

    @JsonProperty("languageProfile")
    private Integer languageProfile;

    @JsonProperty("overview")
    private String overview;

    @JsonProperty("title")
    private String title;

    @JsonProperty("posterPath")
    private String posterPath;

    @JsonProperty("background")
    private String background;

    @JsonProperty("releaseDate")
    private String releaseDate;

    @JsonProperty("status")
    private String status;

    @JsonProperty("totalSeasons")
    private Integer totalSeasons;

    @JsonProperty("childRequests")
    private List<OmbiTvChildRequest> childRequests;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getTvDbId() {
        return tvDbId;
    }

    public void setTvDbId(Integer tvDbId) {
        this.tvDbId = tvDbId;
    }

    public Integer getExternalProviderId() {
        return externalProviderId;
    }

    public void setExternalProviderId(Integer externalProviderId) {
        this.externalProviderId = externalProviderId;
    }

    public String getImdbId() {
        return imdbId;
    }

    public void setImdbId(String imdbId) {
        this.imdbId = imdbId;
    }

    public Integer getQualityOverride() {
        return qualityOverride;
    }

    public void setQualityOverride(Integer qualityOverride) {
        this.qualityOverride = qualityOverride;
    }

    public Integer getRootFolder() {
        return rootFolder;
    }

    public void setRootFolder(Integer rootFolder) {
        this.rootFolder = rootFolder;
    }

    public Integer getLanguageProfile() {
        return languageProfile;
    }

    public void setLanguageProfile(Integer languageProfile) {
        this.languageProfile = languageProfile;
    }

    public String getOverview() {
        return overview;
    }

    public void setOverview(String overview) {
        this.overview = overview;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getPosterPath() {
        return posterPath;
    }

    public void setPosterPath(String posterPath) {
        this.posterPath = posterPath;
    }

    public String getBackground() {
        return background;
    }

    public void setBackground(String background) {
        this.background = background;
    }

    public String getReleaseDate() {
        return releaseDate;
    }

    public void setReleaseDate(String releaseDate) {
        this.releaseDate = releaseDate;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Integer getTotalSeasons() {
        return totalSeasons;
    }

    public void setTotalSeasons(Integer totalSeasons) {
        this.totalSeasons = totalSeasons;
    }

    public List<OmbiTvChildRequest> getChildRequests() {
        return childRequests;
    }

    public void setChildRequests(List<OmbiTvChildRequest> childRequests) {
        this.childRequests = childRequests;
    }
}
