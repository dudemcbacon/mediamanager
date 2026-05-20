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
    "version",
    "real",
    "isRepack"
})
@Generated("jsonschema2pojo")
public class Revision {

  @JsonProperty("version")
  private Integer version;
  @JsonProperty("real")
  private Integer real;
  @JsonProperty("isRepack")
  private Boolean isRepack;
  @JsonIgnore
  private Map<String, Object> additionalProperties = new LinkedHashMap<String, Object>();

  @JsonProperty("version")
  public Integer getVersion() {
    return version;
  }

  @JsonProperty("version")
  public void setVersion(Integer version) {
    this.version = version;
  }

  @JsonProperty("real")
  public Integer getReal() {
    return real;
  }

  @JsonProperty("real")
  public void setReal(Integer real) {
    this.real = real;
  }

  @JsonProperty("isRepack")
  public Boolean getIsRepack() {
    return isRepack;
  }

  @JsonProperty("isRepack")
  public void setIsRepack(Boolean isRepack) {
    this.isRepack = isRepack;
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
