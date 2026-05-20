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
    "coverType",
    "url",
    "remoteUrl"
})
@Generated("jsonschema2pojo")
public class Image {

  @JsonProperty("coverType")
  private String coverType;
  @JsonProperty("url")
  private String url;
  @JsonProperty("remoteUrl")
  private String remoteUrl;
  @JsonIgnore
  private Map<String, Object> additionalProperties = new LinkedHashMap<String, Object>();

  @JsonProperty("coverType")
  public String getCoverType() {
    return coverType;
  }

  @JsonProperty("coverType")
  public void setCoverType(String coverType) {
    this.coverType = coverType;
  }

  @JsonProperty("url")
  public String getUrl() {
    return url;
  }

  @JsonProperty("url")
  public void setUrl(String url) {
    this.url = url;
  }

  @JsonProperty("remoteUrl")
  public String getRemoteUrl() {
    return remoteUrl;
  }

  @JsonProperty("remoteUrl")
  public void setRemoteUrl(String remoteUrl) {
    this.remoteUrl = remoteUrl;
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
