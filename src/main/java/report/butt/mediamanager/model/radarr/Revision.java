package report.butt.mediamanager.model.radarr;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.annotation.processing.Generated;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({"version", "real", "isRepack"})
@Generated("jsonschema2pojo")
@NullMarked
public class Revision {

    @JsonProperty("version")
    private @Nullable Integer version;

    @JsonProperty("real")
    private @Nullable Integer real;

    @JsonProperty("isRepack")
    private @Nullable Boolean isRepack;

    @JsonIgnore
    private Map<String, Object> additionalProperties = new LinkedHashMap<String, Object>();

    @JsonProperty("version")
    public @Nullable Integer getVersion() {
        return version;
    }

    @JsonProperty("version")
    public void setVersion(@Nullable Integer version) {
        this.version = version;
    }

    @JsonProperty("real")
    public @Nullable Integer getReal() {
        return real;
    }

    @JsonProperty("real")
    public void setReal(@Nullable Integer real) {
        this.real = real;
    }

    @JsonProperty("isRepack")
    public @Nullable Boolean getIsRepack() {
        return isRepack;
    }

    @JsonProperty("isRepack")
    public void setIsRepack(@Nullable Boolean isRepack) {
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
