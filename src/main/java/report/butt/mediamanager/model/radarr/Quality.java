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
    "quality",
    "revision"
})
@Generated("jsonschema2pojo")
public class Quality {

  @JsonProperty("quality")
  private Quality__1 quality;
  @JsonProperty("revision")
  private report.butt.mediamanager.model.radarr.Revision revision;
  @JsonIgnore
  private Map<String, Object> additionalProperties = new LinkedHashMap<String, Object>();

  @JsonProperty("quality")
  public Quality__1 getQuality() {
    return quality;
  }

  @JsonProperty("quality")
  public void setQuality(Quality__1 quality) {
    this.quality = quality;
  }

  @JsonProperty("revision")
  public report.butt.mediamanager.model.radarr.Revision getRevision() {
    return revision;
  }

  @JsonProperty("revision")
  public void setRevision(report.butt.mediamanager.model.radarr.Revision revision) {
    this.revision = revision;
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
