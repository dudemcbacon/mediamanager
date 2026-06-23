package report.butt.mediamanager.model.plex;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public class PlexMedia {

    @JsonProperty("id")
    private @Nullable Integer id;

    @JsonProperty("duration")
    private @Nullable Long duration;

    @JsonProperty("Part")
    private @Nullable List<PlexPart> part;

    public @Nullable Integer getId() {
        return id;
    }

    public void setId(@Nullable Integer id) {
        this.id = id;
    }

    public @Nullable Long getDuration() {
        return duration;
    }

    public void setDuration(@Nullable Long duration) {
        this.duration = duration;
    }

    public @Nullable List<PlexPart> getPart() {
        return part;
    }

    public void setPart(@Nullable List<PlexPart> part) {
        this.part = part;
    }
}
