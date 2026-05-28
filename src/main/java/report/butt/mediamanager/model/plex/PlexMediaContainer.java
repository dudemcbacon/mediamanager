package report.butt.mediamanager.model.plex;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

public class PlexMediaContainer {

    @JsonProperty("Metadata")
    private List<PlexMetadata> metadata;

    @JsonProperty("Directory")
    private List<PlexDirectory> directory;

    @JsonProperty("machineIdentifier")
    private String machineIdentifier;

    public List<PlexMetadata> getMetadata() {
        return metadata;
    }

    public void setMetadata(List<PlexMetadata> metadata) {
        this.metadata = metadata;
    }

    public List<PlexDirectory> getDirectory() {
        return directory;
    }

    public void setDirectory(List<PlexDirectory> directory) {
        this.directory = directory;
    }

    public String getMachineIdentifier() {
        return machineIdentifier;
    }

    public void setMachineIdentifier(String machineIdentifier) {
        this.machineIdentifier = machineIdentifier;
    }
}
