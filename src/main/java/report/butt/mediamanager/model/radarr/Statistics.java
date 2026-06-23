package report.butt.mediamanager.model.radarr;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.processing.Generated;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({"movieFileCount", "sizeOnDisk", "releaseGroups"})
@Generated("jsonschema2pojo")
@NullMarked
public class Statistics {

    @JsonProperty("movieFileCount")
    private @Nullable Integer movieFileCount;

    @JsonProperty("sizeOnDisk")
    private @Nullable Long sizeOnDisk;

    @JsonProperty("releaseGroups")
    private @Nullable List<Object> releaseGroups;

    @JsonIgnore
    private Map<String, Object> additionalProperties = new LinkedHashMap<String, Object>();

    @JsonProperty("movieFileCount")
    public @Nullable Integer getMovieFileCount() {
        return movieFileCount;
    }

    @JsonProperty("movieFileCount")
    public void setMovieFileCount(@Nullable Integer movieFileCount) {
        this.movieFileCount = movieFileCount;
    }

    @JsonProperty("sizeOnDisk")
    public @Nullable Long getSizeOnDisk() {
        return sizeOnDisk;
    }

    @JsonProperty("sizeOnDisk")
    public void setSizeOnDisk(@Nullable Long sizeOnDisk) {
        this.sizeOnDisk = sizeOnDisk;
    }

    @JsonProperty("releaseGroups")
    public @Nullable List<Object> getReleaseGroups() {
        return releaseGroups;
    }

    @JsonProperty("releaseGroups")
    public void setReleaseGroups(@Nullable List<Object> releaseGroups) {
        this.releaseGroups = releaseGroups;
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
