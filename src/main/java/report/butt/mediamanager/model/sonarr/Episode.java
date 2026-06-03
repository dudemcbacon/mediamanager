package report.butt.mediamanager.model.sonarr;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Episode {

    @JsonProperty("id")
    private Integer id;

    @JsonProperty("seasonNumber")
    private Integer seasonNumber;

    @JsonProperty("episodeNumber")
    private Integer episodeNumber;

    @JsonProperty("hasFile")
    private Boolean hasFile;

    @JsonProperty("lastSearchTime")
    private String lastSearchTime;

    @JsonProperty("episodeFile")
    private EpisodeFile episodeFile;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getSeasonNumber() {
        return seasonNumber;
    }

    public void setSeasonNumber(Integer seasonNumber) {
        this.seasonNumber = seasonNumber;
    }

    public Integer getEpisodeNumber() {
        return episodeNumber;
    }

    public void setEpisodeNumber(Integer episodeNumber) {
        this.episodeNumber = episodeNumber;
    }

    public Boolean getHasFile() {
        return hasFile;
    }

    public void setHasFile(Boolean hasFile) {
        this.hasFile = hasFile;
    }

    public String getLastSearchTime() {
        return lastSearchTime;
    }

    public void setLastSearchTime(String lastSearchTime) {
        this.lastSearchTime = lastSearchTime;
    }

    public EpisodeFile getEpisodeFile() {
        return episodeFile;
    }

    public void setEpisodeFile(EpisodeFile episodeFile) {
        this.episodeFile = episodeFile;
    }
}
