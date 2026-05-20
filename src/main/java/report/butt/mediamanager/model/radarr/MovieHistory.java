package report.butt.mediamanager.model.radarr;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class MovieHistory {

  @JsonProperty("id")
  private Integer id;

  @JsonProperty("movieId")
  private Integer movieId;

  @JsonProperty("sourceTitle")
  private String sourceTitle;

  @JsonProperty("languages")
  private List<Language> languages;

  @JsonProperty("quality")
  private Quality quality;

  @JsonProperty("customFormatScore")
  private Integer customFormatScore;

  @JsonProperty("qualityCutoffNotMet")
  private Boolean qualityCutoffNotMet;

  @JsonProperty("date")
  private String date;

  @JsonProperty("downloadId")
  private String downloadId;

  @JsonProperty("eventType")
  private String eventType;

  @JsonProperty("data")
  private Map<String, Object> data;

  public Integer getId() {
    return id;
  }

  public void setId(Integer id) {
    this.id = id;
  }

  public Integer getMovieId() {
    return movieId;
  }

  public void setMovieId(Integer movieId) {
    this.movieId = movieId;
  }

  public String getSourceTitle() {
    return sourceTitle;
  }

  public void setSourceTitle(String sourceTitle) {
    this.sourceTitle = sourceTitle;
  }

  public List<Language> getLanguages() {
    return languages;
  }

  public void setLanguages(List<Language> languages) {
    this.languages = languages;
  }

  public Quality getQuality() {
    return quality;
  }

  public void setQuality(Quality quality) {
    this.quality = quality;
  }

  public Integer getCustomFormatScore() {
    return customFormatScore;
  }

  public void setCustomFormatScore(Integer customFormatScore) {
    this.customFormatScore = customFormatScore;
  }

  public Boolean getQualityCutoffNotMet() {
    return qualityCutoffNotMet;
  }

  public void setQualityCutoffNotMet(Boolean qualityCutoffNotMet) {
    this.qualityCutoffNotMet = qualityCutoffNotMet;
  }

  public String getDate() {
    return date;
  }

  public void setDate(String date) {
    this.date = date;
  }

  public String getDownloadId() {
    return downloadId;
  }

  public void setDownloadId(String downloadId) {
    this.downloadId = downloadId;
  }

  public String getEventType() {
    return eventType;
  }

  public void setEventType(String eventType) {
    this.eventType = eventType;
  }

  public Map<String, Object> getData() {
    return data;
  }

  public void setData(Map<String, Object> data) {
    this.data = data;
  }
}
