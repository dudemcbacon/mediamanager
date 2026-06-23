package report.butt.mediamanager.model.sonarr;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@JsonIgnoreProperties(ignoreUnknown = true)
@NullMarked
public class Series {

    @JsonProperty("id")
    private @Nullable Integer id;

    @JsonProperty("title")
    private @Nullable String title;

    @JsonProperty("titleSlug")
    private @Nullable String titleSlug;

    @JsonProperty("tvdbId")
    private @Nullable Integer tvdbId;

    @JsonProperty("year")
    private @Nullable Integer year;

    @JsonProperty("qualityProfileId")
    private @Nullable Integer qualityProfileId;

    @JsonProperty("monitored")
    private @Nullable Boolean monitored;

    @JsonProperty("monitorNewItems")
    private @Nullable String monitorNewItems;

    @JsonProperty("path")
    private @Nullable String path;

    @JsonProperty("rootFolderPath")
    private @Nullable String rootFolderPath;

    @JsonProperty("originalLanguage")
    private @Nullable SonarrLanguage originalLanguage;

    @JsonProperty("statistics")
    private @Nullable SeriesStatistics statistics;

    public @Nullable Integer getId() {
        return id;
    }

    public void setId(@Nullable Integer id) {
        this.id = id;
    }

    public @Nullable String getTitle() {
        return title;
    }

    public void setTitle(@Nullable String title) {
        this.title = title;
    }

    public @Nullable String getTitleSlug() {
        return titleSlug;
    }

    public void setTitleSlug(@Nullable String titleSlug) {
        this.titleSlug = titleSlug;
    }

    public @Nullable Integer getTvdbId() {
        return tvdbId;
    }

    public void setTvdbId(@Nullable Integer tvdbId) {
        this.tvdbId = tvdbId;
    }

    public @Nullable Integer getYear() {
        return year;
    }

    public void setYear(@Nullable Integer year) {
        this.year = year;
    }

    public @Nullable Integer getQualityProfileId() {
        return qualityProfileId;
    }

    public void setQualityProfileId(@Nullable Integer qualityProfileId) {
        this.qualityProfileId = qualityProfileId;
    }

    public @Nullable Boolean getMonitored() {
        return monitored;
    }

    public void setMonitored(@Nullable Boolean monitored) {
        this.monitored = monitored;
    }

    public @Nullable String getMonitorNewItems() {
        return monitorNewItems;
    }

    public void setMonitorNewItems(@Nullable String monitorNewItems) {
        this.monitorNewItems = monitorNewItems;
    }

    public @Nullable String getPath() {
        return path;
    }

    public void setPath(@Nullable String path) {
        this.path = path;
    }

    public @Nullable String getRootFolderPath() {
        return rootFolderPath;
    }

    public void setRootFolderPath(@Nullable String rootFolderPath) {
        this.rootFolderPath = rootFolderPath;
    }

    public @Nullable SonarrLanguage getOriginalLanguage() {
        return originalLanguage;
    }

    public void setOriginalLanguage(@Nullable SonarrLanguage originalLanguage) {
        this.originalLanguage = originalLanguage;
    }

    public @Nullable SeriesStatistics getStatistics() {
        return statistics;
    }

    public void setStatistics(@Nullable SeriesStatistics statistics) {
        this.statistics = statistics;
    }
}
