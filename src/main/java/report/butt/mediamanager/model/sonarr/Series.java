package report.butt.mediamanager.model.sonarr;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Series {

    @JsonProperty("id")
    private Integer id;

    @JsonProperty("title")
    private String title;

    @JsonProperty("titleSlug")
    private String titleSlug;

    @JsonProperty("tvdbId")
    private Integer tvdbId;

    @JsonProperty("year")
    private Integer year;

    @JsonProperty("qualityProfileId")
    private Integer qualityProfileId;

    @JsonProperty("monitored")
    private Boolean monitored;

    @JsonProperty("monitorNewItems")
    private String monitorNewItems;

    @JsonProperty("path")
    private String path;

    @JsonProperty("rootFolderPath")
    private String rootFolderPath;

    @JsonProperty("originalLanguage")
    private SonarrLanguage originalLanguage;

    @JsonProperty("statistics")
    private SeriesStatistics statistics;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getTitleSlug() {
        return titleSlug;
    }

    public void setTitleSlug(String titleSlug) {
        this.titleSlug = titleSlug;
    }

    public Integer getTvdbId() {
        return tvdbId;
    }

    public void setTvdbId(Integer tvdbId) {
        this.tvdbId = tvdbId;
    }

    public Integer getYear() {
        return year;
    }

    public void setYear(Integer year) {
        this.year = year;
    }

    public Integer getQualityProfileId() {
        return qualityProfileId;
    }

    public void setQualityProfileId(Integer qualityProfileId) {
        this.qualityProfileId = qualityProfileId;
    }

    public Boolean getMonitored() {
        return monitored;
    }

    public void setMonitored(Boolean monitored) {
        this.monitored = monitored;
    }

    public String getMonitorNewItems() {
        return monitorNewItems;
    }

    public void setMonitorNewItems(String monitorNewItems) {
        this.monitorNewItems = monitorNewItems;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getRootFolderPath() {
        return rootFolderPath;
    }

    public void setRootFolderPath(String rootFolderPath) {
        this.rootFolderPath = rootFolderPath;
    }

    public SonarrLanguage getOriginalLanguage() {
        return originalLanguage;
    }

    public void setOriginalLanguage(SonarrLanguage originalLanguage) {
        this.originalLanguage = originalLanguage;
    }

    public SeriesStatistics getStatistics() {
        return statistics;
    }

    public void setStatistics(SeriesStatistics statistics) {
        this.statistics = statistics;
    }
}
