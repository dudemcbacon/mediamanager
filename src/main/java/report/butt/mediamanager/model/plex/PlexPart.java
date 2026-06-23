package report.butt.mediamanager.model.plex;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public class PlexPart {

    @JsonProperty("id")
    private @Nullable Integer id;

    @JsonProperty("file")
    private @Nullable String file;

    @JsonProperty("size")
    private @Nullable Long size;

    @JsonProperty("duration")
    private @Nullable Long duration;

    public @Nullable Integer getId() {
        return id;
    }

    public void setId(@Nullable Integer id) {
        this.id = id;
    }

    public @Nullable String getFile() {
        return file;
    }

    public void setFile(@Nullable String file) {
        this.file = file;
    }

    public @Nullable Long getSize() {
        return size;
    }

    public void setSize(@Nullable Long size) {
        this.size = size;
    }

    public @Nullable Long getDuration() {
        return duration;
    }

    public void setDuration(@Nullable Long duration) {
        this.duration = duration;
    }
}
