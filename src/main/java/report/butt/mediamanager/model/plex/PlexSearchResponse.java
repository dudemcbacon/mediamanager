package report.butt.mediamanager.model.plex;

import com.fasterxml.jackson.annotation.JsonProperty;

public class PlexSearchResponse {

    @JsonProperty("MediaContainer")
    private PlexMediaContainer mediaContainer;

    public PlexMediaContainer getMediaContainer() {
        return mediaContainer;
    }

    public void setMediaContainer(PlexMediaContainer mediaContainer) {
        this.mediaContainer = mediaContainer;
    }
}
