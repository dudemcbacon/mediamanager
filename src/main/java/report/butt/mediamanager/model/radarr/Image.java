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
@JsonPropertyOrder({"coverType", "url", "remoteUrl"})
@Generated("jsonschema2pojo")
@NullMarked
public class Image {

    @JsonProperty("coverType")
    private @Nullable String coverType;

    @JsonProperty("url")
    private @Nullable String url;

    @JsonProperty("remoteUrl")
    private @Nullable String remoteUrl;

    @JsonIgnore
    private Map<String, Object> additionalProperties = new LinkedHashMap<String, Object>();

    @JsonProperty("coverType")
    public @Nullable String getCoverType() {
        return coverType;
    }

    @JsonProperty("coverType")
    public void setCoverType(@Nullable String coverType) {
        this.coverType = coverType;
    }

    @JsonProperty("url")
    public @Nullable String getUrl() {
        return url;
    }

    @JsonProperty("url")
    public void setUrl(@Nullable String url) {
        this.url = url;
    }

    @JsonProperty("remoteUrl")
    public @Nullable String getRemoteUrl() {
        return remoteUrl;
    }

    @JsonProperty("remoteUrl")
    public void setRemoteUrl(@Nullable String remoteUrl) {
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
