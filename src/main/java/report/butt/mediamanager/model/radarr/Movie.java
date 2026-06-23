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
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

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
@NullMarked
public class Movie {

    @JsonProperty("title")
    private @Nullable String title;

    @JsonProperty("originalTitle")
    private @Nullable String originalTitle;

    @JsonProperty("originalLanguage")
    private @Nullable OriginalLanguage originalLanguage;

    @JsonProperty("alternateTitles")
    private @Nullable List<AlternateTitle> alternateTitles;

    @JsonProperty("secondaryYearSourceId")
    private @Nullable Integer secondaryYearSourceId;

    @JsonProperty("sortTitle")
    private @Nullable String sortTitle;

    @JsonProperty("sizeOnDisk")
    private @Nullable Long sizeOnDisk;

    @JsonProperty("status")
    private @Nullable String status;

    @JsonProperty("overview")
    private @Nullable String overview;

    @JsonProperty("inCinemas")
    private @Nullable String inCinemas;

    @JsonProperty("physicalRelease")
    private @Nullable String physicalRelease;

    @JsonProperty("digitalRelease")
    private @Nullable String digitalRelease;

    @JsonProperty("releaseDate")
    private @Nullable String releaseDate;

    @JsonProperty("images")
    private @Nullable List<Image> images;

    @JsonProperty("website")
    private @Nullable String website;

    @JsonProperty("year")
    private @Nullable Integer year;

    @JsonProperty("youTubeTrailerId")
    private @Nullable String youTubeTrailerId;

    @JsonProperty("studio")
    private @Nullable String studio;

    @JsonProperty("path")
    private @Nullable String path;

    @JsonProperty("qualityProfileId")
    private @Nullable Integer qualityProfileId;

    @JsonProperty("hasFile")
    private @Nullable Boolean hasFile;

    @JsonProperty("movieFileId")
    private @Nullable Integer movieFileId;

    @JsonProperty("monitored")
    private @Nullable Boolean monitored;

    @JsonProperty("minimumAvailability")
    private @Nullable String minimumAvailability;

    @JsonProperty("isAvailable")
    private @Nullable Boolean isAvailable;

    @JsonProperty("folderName")
    private @Nullable String folderName;

    @JsonProperty("runtime")
    private @Nullable Integer runtime;

    @JsonProperty("cleanTitle")
    private @Nullable String cleanTitle;

    @JsonProperty("imdbId")
    private @Nullable String imdbId;

    @JsonProperty("tmdbId")
    private @Nullable Integer tmdbId;

    @JsonProperty("titleSlug")
    private @Nullable String titleSlug;

    @JsonProperty("rootFolderPath")
    private @Nullable String rootFolderPath;

    @JsonProperty("certification")
    private @Nullable String certification;

    @JsonProperty("genres")
    private @Nullable List<String> genres;

    @JsonProperty("keywords")
    private @Nullable List<String> keywords;

    @JsonProperty("tags")
    private @Nullable List<Object> tags;

    @JsonProperty("added")
    private @Nullable String added;

    @JsonProperty("ratings")
    private @Nullable Ratings ratings;

    @JsonProperty("movieFile")
    private @Nullable Moviefile movieFile;

    @JsonProperty("popularity")
    private @Nullable Double popularity;

    @JsonProperty("statistics")
    private report.butt.mediamanager.model.radarr.@Nullable Statistics statistics;

    @JsonProperty("lastSearchTime")
    private @Nullable String lastSearchTime;

    @JsonProperty("id")
    private @Nullable Integer id;

    @JsonIgnore
    private Map<String, Object> additionalProperties = new LinkedHashMap<String, Object>();

    @JsonProperty("title")
    public @Nullable String getTitle() {
        return title;
    }

    @JsonProperty("title")
    public void setTitle(@Nullable String title) {
        this.title = title;
    }

    @JsonProperty("originalTitle")
    public @Nullable String getOriginalTitle() {
        return originalTitle;
    }

    @JsonProperty("originalTitle")
    public void setOriginalTitle(@Nullable String originalTitle) {
        this.originalTitle = originalTitle;
    }

    @JsonProperty("originalLanguage")
    public @Nullable OriginalLanguage getOriginalLanguage() {
        return originalLanguage;
    }

    @JsonProperty("originalLanguage")
    public void setOriginalLanguage(@Nullable OriginalLanguage originalLanguage) {
        this.originalLanguage = originalLanguage;
    }

    @JsonProperty("alternateTitles")
    public @Nullable List<AlternateTitle> getAlternateTitles() {
        return alternateTitles;
    }

    @JsonProperty("alternateTitles")
    public void setAlternateTitles(@Nullable List<AlternateTitle> alternateTitles) {
        this.alternateTitles = alternateTitles;
    }

    @JsonProperty("secondaryYearSourceId")
    public @Nullable Integer getSecondaryYearSourceId() {
        return secondaryYearSourceId;
    }

    @JsonProperty("secondaryYearSourceId")
    public void setSecondaryYearSourceId(@Nullable Integer secondaryYearSourceId) {
        this.secondaryYearSourceId = secondaryYearSourceId;
    }

    @JsonProperty("sortTitle")
    public @Nullable String getSortTitle() {
        return sortTitle;
    }

    @JsonProperty("sortTitle")
    public void setSortTitle(@Nullable String sortTitle) {
        this.sortTitle = sortTitle;
    }

    @JsonProperty("sizeOnDisk")
    public @Nullable Long getSizeOnDisk() {
        return sizeOnDisk;
    }

    @JsonProperty("sizeOnDisk")
    public void setSizeOnDisk(@Nullable Long sizeOnDisk) {
        this.sizeOnDisk = sizeOnDisk;
    }

    @JsonProperty("status")
    public @Nullable String getStatus() {
        return status;
    }

    @JsonProperty("status")
    public void setStatus(@Nullable String status) {
        this.status = status;
    }

    @JsonProperty("overview")
    public @Nullable String getOverview() {
        return overview;
    }

    @JsonProperty("overview")
    public void setOverview(@Nullable String overview) {
        this.overview = overview;
    }

    @JsonProperty("inCinemas")
    public @Nullable String getInCinemas() {
        return inCinemas;
    }

    @JsonProperty("inCinemas")
    public void setInCinemas(@Nullable String inCinemas) {
        this.inCinemas = inCinemas;
    }

    @JsonProperty("physicalRelease")
    public @Nullable String getPhysicalRelease() {
        return physicalRelease;
    }

    @JsonProperty("physicalRelease")
    public void setPhysicalRelease(@Nullable String physicalRelease) {
        this.physicalRelease = physicalRelease;
    }

    @JsonProperty("digitalRelease")
    public @Nullable String getDigitalRelease() {
        return digitalRelease;
    }

    @JsonProperty("digitalRelease")
    public void setDigitalRelease(@Nullable String digitalRelease) {
        this.digitalRelease = digitalRelease;
    }

    @JsonProperty("releaseDate")
    public @Nullable String getReleaseDate() {
        return releaseDate;
    }

    @JsonProperty("releaseDate")
    public void setReleaseDate(@Nullable String releaseDate) {
        this.releaseDate = releaseDate;
    }

    @JsonProperty("images")
    public @Nullable List<Image> getImages() {
        return images;
    }

    @JsonProperty("images")
    public void setImages(@Nullable List<Image> images) {
        this.images = images;
    }

    @JsonProperty("website")
    public @Nullable String getWebsite() {
        return website;
    }

    @JsonProperty("website")
    public void setWebsite(@Nullable String website) {
        this.website = website;
    }

    @JsonProperty("year")
    public @Nullable Integer getYear() {
        return year;
    }

    @JsonProperty("year")
    public void setYear(@Nullable Integer year) {
        this.year = year;
    }

    @JsonProperty("youTubeTrailerId")
    public @Nullable String getYouTubeTrailerId() {
        return youTubeTrailerId;
    }

    @JsonProperty("youTubeTrailerId")
    public void setYouTubeTrailerId(@Nullable String youTubeTrailerId) {
        this.youTubeTrailerId = youTubeTrailerId;
    }

    @JsonProperty("studio")
    public @Nullable String getStudio() {
        return studio;
    }

    @JsonProperty("studio")
    public void setStudio(@Nullable String studio) {
        this.studio = studio;
    }

    @JsonProperty("path")
    public @Nullable String getPath() {
        return path;
    }

    @JsonProperty("path")
    public void setPath(@Nullable String path) {
        this.path = path;
    }

    @JsonProperty("qualityProfileId")
    public @Nullable Integer getQualityProfileId() {
        return qualityProfileId;
    }

    @JsonProperty("qualityProfileId")
    public void setQualityProfileId(@Nullable Integer qualityProfileId) {
        this.qualityProfileId = qualityProfileId;
    }

    @JsonProperty("hasFile")
    public @Nullable Boolean getHasFile() {
        return hasFile;
    }

    @JsonProperty("hasFile")
    public void setHasFile(@Nullable Boolean hasFile) {
        this.hasFile = hasFile;
    }

    @JsonProperty("movieFileId")
    public @Nullable Integer getMovieFileId() {
        return movieFileId;
    }

    @JsonProperty("movieFileId")
    public void setMovieFileId(@Nullable Integer movieFileId) {
        this.movieFileId = movieFileId;
    }

    @JsonProperty("monitored")
    public @Nullable Boolean getMonitored() {
        return monitored;
    }

    @JsonProperty("monitored")
    public void setMonitored(@Nullable Boolean monitored) {
        this.monitored = monitored;
    }

    @JsonProperty("minimumAvailability")
    public @Nullable String getMinimumAvailability() {
        return minimumAvailability;
    }

    @JsonProperty("minimumAvailability")
    public void setMinimumAvailability(@Nullable String minimumAvailability) {
        this.minimumAvailability = minimumAvailability;
    }

    @JsonProperty("isAvailable")
    public @Nullable Boolean getIsAvailable() {
        return isAvailable;
    }

    @JsonProperty("isAvailable")
    public void setIsAvailable(@Nullable Boolean isAvailable) {
        this.isAvailable = isAvailable;
    }

    @JsonProperty("folderName")
    public @Nullable String getFolderName() {
        return folderName;
    }

    @JsonProperty("folderName")
    public void setFolderName(@Nullable String folderName) {
        this.folderName = folderName;
    }

    @JsonProperty("runtime")
    public @Nullable Integer getRuntime() {
        return runtime;
    }

    @JsonProperty("runtime")
    public void setRuntime(@Nullable Integer runtime) {
        this.runtime = runtime;
    }

    @JsonProperty("cleanTitle")
    public @Nullable String getCleanTitle() {
        return cleanTitle;
    }

    @JsonProperty("cleanTitle")
    public void setCleanTitle(@Nullable String cleanTitle) {
        this.cleanTitle = cleanTitle;
    }

    @JsonProperty("imdbId")
    public @Nullable String getImdbId() {
        return imdbId;
    }

    @JsonProperty("imdbId")
    public void setImdbId(@Nullable String imdbId) {
        this.imdbId = imdbId;
    }

    @JsonProperty("tmdbId")
    public @Nullable Integer getTmdbId() {
        return tmdbId;
    }

    @JsonProperty("tmdbId")
    public void setTmdbId(@Nullable Integer tmdbId) {
        this.tmdbId = tmdbId;
    }

    @JsonProperty("titleSlug")
    public @Nullable String getTitleSlug() {
        return titleSlug;
    }

    @JsonProperty("titleSlug")
    public void setTitleSlug(@Nullable String titleSlug) {
        this.titleSlug = titleSlug;
    }

    @JsonProperty("rootFolderPath")
    public @Nullable String getRootFolderPath() {
        return rootFolderPath;
    }

    @JsonProperty("rootFolderPath")
    public void setRootFolderPath(@Nullable String rootFolderPath) {
        this.rootFolderPath = rootFolderPath;
    }

    @JsonProperty("certification")
    public @Nullable String getCertification() {
        return certification;
    }

    @JsonProperty("certification")
    public void setCertification(@Nullable String certification) {
        this.certification = certification;
    }

    @JsonProperty("genres")
    public @Nullable List<String> getGenres() {
        return genres;
    }

    @JsonProperty("genres")
    public void setGenres(@Nullable List<String> genres) {
        this.genres = genres;
    }

    @JsonProperty("keywords")
    public @Nullable List<String> getKeywords() {
        return keywords;
    }

    @JsonProperty("keywords")
    public void setKeywords(@Nullable List<String> keywords) {
        this.keywords = keywords;
    }

    @JsonProperty("tags")
    public @Nullable List<Object> getTags() {
        return tags;
    }

    @JsonProperty("tags")
    public void setTags(@Nullable List<Object> tags) {
        this.tags = tags;
    }

    @JsonProperty("added")
    public @Nullable String getAdded() {
        return added;
    }

    @JsonProperty("added")
    public void setAdded(@Nullable String added) {
        this.added = added;
    }

    @JsonProperty("ratings")
    public @Nullable Ratings getRatings() {
        return ratings;
    }

    @JsonProperty("ratings")
    public void setRatings(@Nullable Ratings ratings) {
        this.ratings = ratings;
    }

    @JsonProperty("movieFile")
    public @Nullable Moviefile getMovieFile() {
        return movieFile;
    }

    @JsonProperty("movieFile")
    public void setMovieFile(@Nullable Moviefile movieFile) {
        this.movieFile = movieFile;
    }

    @JsonProperty("popularity")
    public @Nullable Double getPopularity() {
        return popularity;
    }

    @JsonProperty("popularity")
    public void setPopularity(@Nullable Double popularity) {
        this.popularity = popularity;
    }

    @JsonProperty("statistics")
    public report.butt.mediamanager.model.radarr.@Nullable Statistics getStatistics() {
        return statistics;
    }

    @JsonProperty("statistics")
    public void setStatistics(report.butt.mediamanager.model.radarr.@Nullable Statistics statistics) {
        this.statistics = statistics;
    }

    @JsonProperty("lastSearchTime")
    public @Nullable String getLastSearchTime() {
        return lastSearchTime;
    }

    @JsonProperty("lastSearchTime")
    public void setLastSearchTime(@Nullable String lastSearchTime) {
        this.lastSearchTime = lastSearchTime;
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
