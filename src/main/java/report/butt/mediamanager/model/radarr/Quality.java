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
@JsonPropertyOrder({"quality", "revision"})
@Generated("jsonschema2pojo")
@NullMarked
public class Quality {

    @JsonProperty("quality")
    private @Nullable Quality__1 quality;

    @JsonProperty("revision")
    private report.butt.mediamanager.model.radarr.@Nullable Revision revision;

    @JsonIgnore
    private Map<String, Object> additionalProperties = new LinkedHashMap<String, Object>();

    @JsonProperty("quality")
    public @Nullable Quality__1 getQuality() {
        return quality;
    }

    @JsonProperty("quality")
    public void setQuality(@Nullable Quality__1 quality) {
        this.quality = quality;
    }

    @JsonProperty("revision")
    public report.butt.mediamanager.model.radarr.@Nullable Revision getRevision() {
        return revision;
    }

    @JsonProperty("revision")
    public void setRevision(report.butt.mediamanager.model.radarr.@Nullable Revision revision) {
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
