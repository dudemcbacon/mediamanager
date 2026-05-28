package report.butt.mediamanager.model.ombi;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class OmbiTvSeasonRequest {

  @JsonProperty("id")
  private Integer id;

  @JsonProperty("seasonNumber")
  private Integer seasonNumber;

  @JsonProperty("overview")
  private String overview;

  @JsonProperty("episodes")
  private List<OmbiTvEpisode> episodes;

  @JsonProperty("childRequestId")
  private Integer childRequestId;

  @JsonProperty("seasonAvailable")
  private Boolean seasonAvailable;

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

  public String getOverview() {
    return overview;
  }

  public void setOverview(String overview) {
    this.overview = overview;
  }

  public List<OmbiTvEpisode> getEpisodes() {
    return episodes;
  }

  public void setEpisodes(List<OmbiTvEpisode> episodes) {
    this.episodes = episodes;
  }

  public Integer getChildRequestId() {
    return childRequestId;
  }

  public void setChildRequestId(Integer childRequestId) {
    this.childRequestId = childRequestId;
  }

  public Boolean getSeasonAvailable() {
    return seasonAvailable;
  }

  public void setSeasonAvailable(Boolean seasonAvailable) {
    this.seasonAvailable = seasonAvailable;
  }
}
