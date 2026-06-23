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
@JsonPropertyOrder({"sourceType", "movieMetadataId", "title", "id"})
@Generated("jsonschema2pojo")
@NullMarked
public class AlternateTitle {

    @JsonProperty("sourceType")
    private @Nullable String sourceType;

    @JsonProperty("movieMetadataId")
    private @Nullable Integer movieMetadataId;

    @JsonProperty("title")
    private @Nullable String title;

    @JsonProperty("id")
    private @Nullable Integer id;

    @JsonIgnore
    private Map<String, Object> additionalProperties = new LinkedHashMap<String, Object>();

    @JsonProperty("sourceType")
    public @Nullable String getSourceType() {
        return sourceType;
    }

    @JsonProperty("sourceType")
    public void setSourceType(@Nullable String sourceType) {
        this.sourceType = sourceType;
    }

    @JsonProperty("movieMetadataId")
    public @Nullable Integer getMovieMetadataId() {
        return movieMetadataId;
    }

    @JsonProperty("movieMetadataId")
    public void setMovieMetadataId(@Nullable Integer movieMetadataId) {
        this.movieMetadataId = movieMetadataId;
    }

    @JsonProperty("title")
    public @Nullable String getTitle() {
        return title;
    }

    @JsonProperty("title")
    public void setTitle(@Nullable String title) {
        this.title = title;
    }

    @JsonProperty("id")
    public @Nullable Integer getId() {
        return id;
    }

    @JsonProperty("id")
    public void setId(@Nullable Integer id) {
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
