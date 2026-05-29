package report.butt.mediamanager.model.radarr;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.processing.Generated;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
    "title",
    "originalTitle",
    "originalLanguage",
    "alternateTitles",
    "secondaryYearSourceId",
    "sortTitle",
    "sizeOnDisk",
    "status",
    "overview",
    "inCinemas",
    "physicalRelease",
    "digitalRelease",
    "releaseDate",
    "images",
    "website",
    "year",
    "youTubeTrailerId",
    "studio",
    "path",
    "qualityProfileId",
    "hasFile",
    "movieFileId",
    "monitored",
    "minimumAvailability",
    "isAvailable",
    "folderName",
    "runtime",
    "cleanTitle",
    "imdbId",
    "tmdbId",
    "titleSlug",
    "rootFolderPath",
    "certification",
    "genres",
    "keywords",
    "tags",
    "added",
    "ratings",
    "movieFile",
    "popularity",
    "statistics",
    "lastSearchTime",
    "id"
})
@Generated("jsonschema2pojo")
public class Movie {

    @JsonProperty("title")
    private String title;

    @JsonProperty("originalTitle")
    private String originalTitle;

    @JsonProperty("originalLanguage")
    private OriginalLanguage originalLanguage;

    @JsonProperty("alternateTitles")
    private List<AlternateTitle> alternateTitles;

    @JsonProperty("secondaryYearSourceId")
    private Integer secondaryYearSourceId;

    @JsonProperty("sortTitle")
    private String sortTitle;

    @JsonProperty("sizeOnDisk")
    private Long sizeOnDisk;

    @JsonProperty("status")
    private String status;

    @JsonProperty("overview")
    private String overview;

    @JsonProperty("inCinemas")
    private String inCinemas;

    @JsonProperty("physicalRelease")
    private String physicalRelease;

    @JsonProperty("digitalRelease")
    private String digitalRelease;

    @JsonProperty("releaseDate")
    private String releaseDate;

    @JsonProperty("images")
    private List<Image> images;

    @JsonProperty("website")
    private String website;

    @JsonProperty("year")
    private Integer year;

    @JsonProperty("youTubeTrailerId")
    private String youTubeTrailerId;

    @JsonProperty("studio")
    private String studio;

    @JsonProperty("path")
    private String path;

    @JsonProperty("qualityProfileId")
    private Integer qualityProfileId;

    @JsonProperty("hasFile")
    private Boolean hasFile;

    @JsonProperty("movieFileId")
    private Integer movieFileId;

    @JsonProperty("monitored")
    private Boolean monitored;

    @JsonProperty("minimumAvailability")
    private String minimumAvailability;

    @JsonProperty("isAvailable")
    private Boolean isAvailable;

    @JsonProperty("folderName")
    private String folderName;

    @JsonProperty("runtime")
    private Integer runtime;

    @JsonProperty("cleanTitle")
    private String cleanTitle;

    @JsonProperty("imdbId")
    private String imdbId;

    @JsonProperty("tmdbId")
    private Integer tmdbId;

    @JsonProperty("titleSlug")
    private String titleSlug;

    @JsonProperty("rootFolderPath")
    private String rootFolderPath;

    @JsonProperty("certification")
    private String certification;

    @JsonProperty("genres")
    private List<String> genres;

    @JsonProperty("keywords")
    private List<String> keywords;

    @JsonProperty("tags")
    private List<Object> tags;

    @JsonProperty("added")
    private String added;

    @JsonProperty("ratings")
    private Ratings ratings;

    @JsonProperty("movieFile")
    private Moviefile movieFile;

    @JsonProperty("popularity")
    private Double popularity;

    @JsonProperty("statistics")
    private report.butt.mediamanager.model.radarr.Statistics statistics;

    @JsonProperty("lastSearchTime")
    private String lastSearchTime;

    @JsonProperty("id")
    private Integer id;

    @JsonIgnore
    private Map<String, Object> additionalProperties = new LinkedHashMap<String, Object>();

    @JsonProperty("title")
    public String getTitle() {
        return title;
    }

    @JsonProperty("title")
    public void setTitle(String title) {
        this.title = title;
    }

    @JsonProperty("originalTitle")
    public String getOriginalTitle() {
        return originalTitle;
    }

    @JsonProperty("originalTitle")
    public void setOriginalTitle(String originalTitle) {
        this.originalTitle = originalTitle;
    }

    @JsonProperty("originalLanguage")
    public OriginalLanguage getOriginalLanguage() {
        return originalLanguage;
    }

    @JsonProperty("originalLanguage")
    public void setOriginalLanguage(OriginalLanguage originalLanguage) {
        this.originalLanguage = originalLanguage;
    }

    @JsonProperty("alternateTitles")
    public List<AlternateTitle> getAlternateTitles() {
        return alternateTitles;
    }

    @JsonProperty("alternateTitles")
    public void setAlternateTitles(List<AlternateTitle> alternateTitles) {
        this.alternateTitles = alternateTitles;
    }

    @JsonProperty("secondaryYearSourceId")
    public Integer getSecondaryYearSourceId() {
        return secondaryYearSourceId;
    }

    @JsonProperty("secondaryYearSourceId")
    public void setSecondaryYearSourceId(Integer secondaryYearSourceId) {
        this.secondaryYearSourceId = secondaryYearSourceId;
    }

    @JsonProperty("sortTitle")
    public String getSortTitle() {
        return sortTitle;
    }

    @JsonProperty("sortTitle")
    public void setSortTitle(String sortTitle) {
        this.sortTitle = sortTitle;
    }

    @JsonProperty("sizeOnDisk")
    public Long getSizeOnDisk() {
        return sizeOnDisk;
    }

    @JsonProperty("sizeOnDisk")
    public void setSizeOnDisk(Long sizeOnDisk) {
        this.sizeOnDisk = sizeOnDisk;
    }

    @JsonProperty("status")
    public String getStatus() {
        return status;
    }

    @JsonProperty("status")
    public void setStatus(String status) {
        this.status = status;
    }

    @JsonProperty("overview")
    public String getOverview() {
        return overview;
    }

    @JsonProperty("overview")
    public void setOverview(String overview) {
        this.overview = overview;
    }

    @JsonProperty("inCinemas")
    public String getInCinemas() {
        return inCinemas;
    }

    @JsonProperty("inCinemas")
    public void setInCinemas(String inCinemas) {
        this.inCinemas = inCinemas;
    }

    @JsonProperty("physicalRelease")
    public String getPhysicalRelease() {
        return physicalRelease;
    }

    @JsonProperty("physicalRelease")
    public void setPhysicalRelease(String physicalRelease) {
        this.physicalRelease = physicalRelease;
    }

    @JsonProperty("digitalRelease")
    public String getDigitalRelease() {
        return digitalRelease;
    }

    @JsonProperty("digitalRelease")
    public void setDigitalRelease(String digitalRelease) {
        this.digitalRelease = digitalRelease;
    }

    @JsonProperty("releaseDate")
    public String getReleaseDate() {
        return releaseDate;
    }

    @JsonProperty("releaseDate")
    public void setReleaseDate(String releaseDate) {
        this.releaseDate = releaseDate;
    }

    @JsonProperty("images")
    public List<Image> getImages() {
        return images;
    }

    @JsonProperty("images")
    public void setImages(List<Image> images) {
        this.images = images;
    }

    @JsonProperty("website")
    public String getWebsite() {
        return website;
    }

    @JsonProperty("website")
    public void setWebsite(String website) {
        this.website = website;
    }

    @JsonProperty("year")
    public Integer getYear() {
        return year;
    }

    @JsonProperty("year")
    public void setYear(Integer year) {
        this.year = year;
    }

    @JsonProperty("youTubeTrailerId")
    public String getYouTubeTrailerId() {
        return youTubeTrailerId;
    }

    @JsonProperty("youTubeTrailerId")
    public void setYouTubeTrailerId(String youTubeTrailerId) {
        this.youTubeTrailerId = youTubeTrailerId;
    }

    @JsonProperty("studio")
    public String getStudio() {
        return studio;
    }

    @JsonProperty("studio")
    public void setStudio(String studio) {
        this.studio = studio;
    }

    @JsonProperty("path")
    public String getPath() {
        return path;
    }

    @JsonProperty("path")
    public void setPath(String path) {
        this.path = path;
    }

    @JsonProperty("qualityProfileId")
    public Integer getQualityProfileId() {
        return qualityProfileId;
    }

    @JsonProperty("qualityProfileId")
    public void setQualityProfileId(Integer qualityProfileId) {
        this.qualityProfileId = qualityProfileId;
    }

    @JsonProperty("hasFile")
    public Boolean getHasFile() {
        return hasFile;
    }

    @JsonProperty("hasFile")
    public void setHasFile(Boolean hasFile) {
        this.hasFile = hasFile;
    }

    @JsonProperty("movieFileId")
    public Integer getMovieFileId() {
        return movieFileId;
    }

    @JsonProperty("movieFileId")
    public void setMovieFileId(Integer movieFileId) {
        this.movieFileId = movieFileId;
    }

    @JsonProperty("monitored")
    public Boolean getMonitored() {
        return monitored;
    }

    @JsonProperty("monitored")
    public void setMonitored(Boolean monitored) {
        this.monitored = monitored;
    }

    @JsonProperty("minimumAvailability")
    public String getMinimumAvailability() {
        return minimumAvailability;
    }

    @JsonProperty("minimumAvailability")
    public void setMinimumAvailability(String minimumAvailability) {
        this.minimumAvailability = minimumAvailability;
    }

    @JsonProperty("isAvailable")
    public Boolean getIsAvailable() {
        return isAvailable;
    }

    @JsonProperty("isAvailable")
    public void setIsAvailable(Boolean isAvailable) {
        this.isAvailable = isAvailable;
    }

    @JsonProperty("folderName")
    public String getFolderName() {
        return folderName;
    }

    @JsonProperty("folderName")
    public void setFolderName(String folderName) {
        this.folderName = folderName;
    }

    @JsonProperty("runtime")
    public Integer getRuntime() {
        return runtime;
    }

    @JsonProperty("runtime")
    public void setRuntime(Integer runtime) {
        this.runtime = runtime;
    }

    @JsonProperty("cleanTitle")
    public String getCleanTitle() {
        return cleanTitle;
    }

    @JsonProperty("cleanTitle")
    public void setCleanTitle(String cleanTitle) {
        this.cleanTitle = cleanTitle;
    }

    @JsonProperty("imdbId")
    public String getImdbId() {
        return imdbId;
    }

    @JsonProperty("imdbId")
    public void setImdbId(String imdbId) {
        this.imdbId = imdbId;
    }

    @JsonProperty("tmdbId")
    public Integer getTmdbId() {
        return tmdbId;
    }

    @JsonProperty("tmdbId")
    public void setTmdbId(Integer tmdbId) {
        this.tmdbId = tmdbId;
    }

    @JsonProperty("titleSlug")
    public String getTitleSlug() {
        return titleSlug;
    }

    @JsonProperty("titleSlug")
    public void setTitleSlug(String titleSlug) {
        this.titleSlug = titleSlug;
    }

    @JsonProperty("rootFolderPath")
    public String getRootFolderPath() {
        return rootFolderPath;
    }

    @JsonProperty("rootFolderPath")
    public void setRootFolderPath(String rootFolderPath) {
        this.rootFolderPath = rootFolderPath;
    }

    @JsonProperty("certification")
    public String getCertification() {
        return certification;
    }

    @JsonProperty("certification")
    public void setCertification(String certification) {
        this.certification = certification;
    }

    @JsonProperty("genres")
    public List<String> getGenres() {
        return genres;
    }

    @JsonProperty("genres")
    public void setGenres(List<String> genres) {
        this.genres = genres;
    }

    @JsonProperty("keywords")
    public List<String> getKeywords() {
        return keywords;
    }

    @JsonProperty("keywords")
    public void setKeywords(List<String> keywords) {
        this.keywords = keywords;
    }

    @JsonProperty("tags")
    public List<Object> getTags() {
        return tags;
    }

    @JsonProperty("tags")
    public void setTags(List<Object> tags) {
        this.tags = tags;
    }

    @JsonProperty("added")
    public String getAdded() {
        return added;
    }

    @JsonProperty("added")
    public void setAdded(String added) {
        this.added = added;
    }

    @JsonProperty("ratings")
    public Ratings getRatings() {
        return ratings;
    }

    @JsonProperty("ratings")
    public void setRatings(Ratings ratings) {
        this.ratings = ratings;
    }

    @JsonProperty("movieFile")
    public Moviefile getMovieFile() {
        return movieFile;
    }

    @JsonProperty("movieFile")
    public void setMovieFile(Moviefile movieFile) {
        this.movieFile = movieFile;
    }

    @JsonProperty("popularity")
    public Double getPopularity() {
        return popularity;
    }

    @JsonProperty("popularity")
    public void setPopularity(Double popularity) {
        this.popularity = popularity;
    }

    @JsonProperty("statistics")
    public report.butt.mediamanager.model.radarr.Statistics getStatistics() {
        return statistics;
    }

    @JsonProperty("statistics")
    public void setStatistics(report.butt.mediamanager.model.radarr.Statistics statistics) {
        this.statistics = statistics;
    }

    @JsonProperty("lastSearchTime")
    public String getLastSearchTime() {
        return lastSearchTime;
    }

    @JsonProperty("lastSearchTime")
    public void setLastSearchTime(String lastSearchTime) {
        this.lastSearchTime = lastSearchTime;
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
