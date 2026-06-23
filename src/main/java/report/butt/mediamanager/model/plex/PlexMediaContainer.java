package report.butt.mediamanager.model.plex;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public class PlexMediaContainer {

    @JsonProperty("Metadata")
    private @Nullable List<PlexMetadata> metadata;

    @JsonProperty("Directory")
    private @Nullable List<PlexDirectory> directory;

    @JsonProperty("machineIdentifier")
    private @Nullable String machineIdentifier;

    public @Nullable List<PlexMetadata> getMetadata() {
        return metadata;
    }

    public void setMetadata(@Nullable List<PlexMetadata> metadata) {
        this.metadata = metadata;
    }

    public @Nullable List<PlexDirectory> getDirectory() {
        return directory;
    }

    public void setDirectory(@Nullable List<PlexDirectory> directory) {
        this.directory = directory;
    }

    public @Nullable String getMachineIdentifier() {
        return machineIdentifier;
    }

    public void setMachineIdentifier(@Nullable String machineIdentifier) {
        this.machineIdentifier = machineIdentifier;
    }
}
