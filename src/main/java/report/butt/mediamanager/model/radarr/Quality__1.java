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
@JsonPropertyOrder({"id", "name", "source", "resolution", "modifier"})
@Generated("jsonschema2pojo")
@NullMarked
public class Quality__1 {

    @JsonProperty("id")
    private @Nullable Integer id;

    @JsonProperty("name")
    private @Nullable String name;

    @JsonProperty("source")
    private @Nullable String source;

    @JsonProperty("resolution")
    private @Nullable Integer resolution;

    @JsonProperty("modifier")
    private @Nullable String modifier;

    @JsonIgnore
    private Map<String, Object> additionalProperties = new LinkedHashMap<String, Object>();

    @JsonProperty("id")
    public @Nullable Integer getId() {
        return id;
    }

    @JsonProperty("id")
    public void setId(@Nullable Integer id) {
        this.id = id;
    }

    @JsonProperty("name")
    public @Nullable String getName() {
        return name;
    }

    @JsonProperty("name")
    public void setName(@Nullable String name) {
        this.name = name;
    }

    @JsonProperty("source")
    public @Nullable String getSource() {
        return source;
    }

    @JsonProperty("source")
    public void setSource(@Nullable String source) {
        this.source = source;
    }

    @JsonProperty("resolution")
    public @Nullable Integer getResolution() {
        return resolution;
    }

    @JsonProperty("resolution")
    public void setResolution(@Nullable Integer resolution) {
        this.resolution = resolution;
    }

    @JsonProperty("modifier")
    public @Nullable String getModifier() {
        return modifier;
    }

    @JsonProperty("modifier")
    public void setModifier(@Nullable String modifier) {
        this.modifier = modifier;
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
