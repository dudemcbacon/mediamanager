package report.butt.mediamanager.model.plex;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@JsonIgnoreProperties(ignoreUnknown = true)
@NullMarked
public class PlexDirectory {

    @JsonProperty("key")
    private @Nullable String key;

    @JsonProperty("type")
    private @Nullable String type;

    @JsonProperty("title")
    private @Nullable String title;

    public @Nullable String getKey() {
        return key;
    }

    public void setKey(@Nullable String key) {
        this.key = key;
    }

    public @Nullable String getType() {
        return type;
    }

    public void setType(@Nullable String type) {
        this.type = type;
    }

    public @Nullable String getTitle() {
        return title;
    }

    public void setTitle(@Nullable String title) {
        this.title = title;
    }
}
