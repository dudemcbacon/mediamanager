package report.butt.mediamanager.model.plex;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public class PlexSearchResponse {

    @JsonProperty("MediaContainer")
    private @Nullable PlexMediaContainer mediaContainer;

    public @Nullable PlexMediaContainer getMediaContainer() {
        return mediaContainer;
    }

    public void setMediaContainer(@Nullable PlexMediaContainer mediaContainer) {
        this.mediaContainer = mediaContainer;
    }
}
