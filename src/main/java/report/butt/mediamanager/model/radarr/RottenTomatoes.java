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
@JsonPropertyOrder({"votes", "value", "type"})
@Generated("jsonschema2pojo")
@NullMarked
public class RottenTomatoes {

    @JsonProperty("votes")
    private @Nullable Integer votes;

    @JsonProperty("value")
    private @Nullable Integer value;

    @JsonProperty("type")
    private @Nullable String type;

    @JsonIgnore
    private Map<String, Object> additionalProperties = new LinkedHashMap<String, Object>();

    @JsonProperty("votes")
    public @Nullable Integer getVotes() {
        return votes;
    }

    @JsonProperty("votes")
    public void setVotes(@Nullable Integer votes) {
        this.votes = votes;
    }

    @JsonProperty("value")
    public @Nullable Integer getValue() {
        return value;
    }

    @JsonProperty("value")
    public void setValue(@Nullable Integer value) {
        this.value = value;
    }

    @JsonProperty("type")
    public @Nullable String getType() {
        return type;
    }

    @JsonProperty("type")
    public void setType(@Nullable String type) {
        this.type = type;
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
