package report.butt.mediamanager.model.radarr;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@JsonIgnoreProperties(ignoreUnknown = true)
@NullMarked
public class RadarrHealthItem {

    @JsonProperty("source")
    private @Nullable String source;

    @JsonProperty("type")
    private @Nullable String type;

    @JsonProperty("message")
    private @Nullable String message;

    @JsonProperty("wikiUrl")
    private @Nullable String wikiUrl;

    public @Nullable String getSource() {
        return source;
    }

    public void setSource(@Nullable String source) {
        this.source = source;
    }

    public @Nullable String getType() {
        return type;
    }

    public void setType(@Nullable String type) {
        this.type = type;
    }

    public @Nullable String getMessage() {
        return message;
    }

    public void setMessage(@Nullable String message) {
        this.message = message;
    }

    public @Nullable String getWikiUrl() {
        return wikiUrl;
    }

    public void setWikiUrl(@Nullable String wikiUrl) {
        this.wikiUrl = wikiUrl;
    }
}
