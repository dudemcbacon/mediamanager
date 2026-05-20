package report.butt.mediamanager.model.plex;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

public class PlexMediaContainer {

    @JsonProperty("Metadata")
    private List<PlexMetadata> metadata;

    public List<PlexMetadata> getMetadata() {
        return metadata;
    }

    public void setMetadata(List<PlexMetadata> metadata) {
        this.metadata = metadata;
    }
}
