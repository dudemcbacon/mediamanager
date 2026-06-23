package report.butt.mediamanager.model.sonarr;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@JsonIgnoreProperties(ignoreUnknown = true)
@NullMarked
public class EpisodeFile {

    @JsonProperty("path")
    private @Nullable String path;

    public @Nullable String getPath() {
        return path;
    }

    public void setPath(@Nullable String path) {
        this.path = path;
    }
}
