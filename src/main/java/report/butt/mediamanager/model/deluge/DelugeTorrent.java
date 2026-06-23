package report.butt.mediamanager.model.deluge;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@JsonIgnoreProperties(ignoreUnknown = true)
@NullMarked
public class DelugeTorrent {

    @JsonProperty("name")
    private @Nullable String name;

    @JsonProperty("progress")
    private @Nullable Double progress;

    @JsonProperty("state")
    private @Nullable String state;

    @JsonProperty("time_added")
    private @Nullable Double timeAdded;

    @JsonProperty("num_peers")
    private @Nullable Integer numPeers;

    @JsonProperty("num_seeds")
    private @Nullable Integer numSeeds;

    @JsonProperty("total_peers")
    private @Nullable Integer totalPeers;

    @JsonProperty("total_seeds")
    private @Nullable Integer totalSeeds;

    public @Nullable String getName() {
        return name;
    }

    public void setName(@Nullable String name) {
        this.name = name;
    }

    public @Nullable Double getProgress() {
        return progress;
    }

    public void setProgress(@Nullable Double progress) {
        this.progress = progress;
    }

    public @Nullable String getState() {
        return state;
    }

    public void setState(@Nullable String state) {
        this.state = state;
    }

    public @Nullable Double getTimeAdded() {
        return timeAdded;
    }

    public void setTimeAdded(@Nullable Double timeAdded) {
        this.timeAdded = timeAdded;
    }

    public @Nullable Integer getNumPeers() {
        return numPeers;
    }

    public void setNumPeers(@Nullable Integer numPeers) {
        this.numPeers = numPeers;
    }

    public @Nullable Integer getNumSeeds() {
        return numSeeds;
    }

    public void setNumSeeds(@Nullable Integer numSeeds) {
        this.numSeeds = numSeeds;
    }

    public @Nullable Integer getTotalPeers() {
        return totalPeers;
    }

    public void setTotalPeers(@Nullable Integer totalPeers) {
        this.totalPeers = totalPeers;
    }

    public @Nullable Integer getTotalSeeds() {
        return totalSeeds;
    }

    public void setTotalSeeds(@Nullable Integer totalSeeds) {
        this.totalSeeds = totalSeeds;
    }
}
