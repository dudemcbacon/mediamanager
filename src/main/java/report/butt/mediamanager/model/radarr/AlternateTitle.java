package report.butt.mediamanager.model.radarr;

import java.util.LinkedHashMap;
import java.util.Map;

import javax.annotation.processing.Generated;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
    "sourceType",
    "movieMetadataId",
    "title",
    "id"
})
@Generated("jsonschema2pojo")
public class AlternateTitle {

  @JsonProperty("sourceType")
  private String sourceType;
  @JsonProperty("movieMetadataId")
  private Integer movieMetadataId;
  @JsonProperty("title")
  private String title;
  @JsonProperty("id")
  private Integer id;
  @JsonIgnore
  private Map<String, Object> additionalProperties = new LinkedHashMap<String, Object>();

  @JsonProperty("sourceType")
  public String getSourceType() {
    return sourceType;
  }

  @JsonProperty("sourceType")
  public void setSourceType(String sourceType) {
    this.sourceType = sourceType;
  }

  @JsonProperty("movieMetadataId")
  public Integer getMovieMetadataId() {
    return movieMetadataId;
  }

  @JsonProperty("movieMetadataId")
  public void setMovieMetadataId(Integer movieMetadataId) {
    this.movieMetadataId = movieMetadataId;
  }

  @JsonProperty("title")
  public String getTitle() {
    return title;
  }

  @JsonProperty("title")
  public void setTitle(String title) {
    this.title = title;
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
