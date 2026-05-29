package report.butt.mediamanager.model.sonarr;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Series {

    @JsonProperty("id")
    private Integer id;

    @JsonProperty("title")
    private String title;

    @JsonProperty("tvdbId")
    private Integer tvdbId;

    @JsonProperty("year")
    private Integer year;

    @JsonProperty("monitored")
    private Boolean monitored;

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

    public Boolean getMonitored() {
        return monitored;
    }

    public void setMonitored(Boolean monitored) {
        this.monitored = monitored;
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
