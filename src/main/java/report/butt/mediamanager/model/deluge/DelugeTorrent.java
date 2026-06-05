package report.butt.mediamanager.model.deluge;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class DelugeTorrent {

    @JsonProperty("name")
    private String name;

    @JsonProperty("progress")
    private Double progress;

    @JsonProperty("state")
    private String state;

    @JsonProperty("num_peers")
    private Integer numPeers;

    @JsonProperty("num_seeds")
    private Integer numSeeds;

    @JsonProperty("total_peers")
    private Integer totalPeers;

    @JsonProperty("total_seeds")
    private Integer totalSeeds;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Double getProgress() {
        return progress;
    }

    public void setProgress(Double progress) {
        this.progress = progress;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public Integer getNumPeers() {
        return numPeers;
    }

    public void setNumPeers(Integer numPeers) {
        this.numPeers = numPeers;
    }

    public Integer getNumSeeds() {
        return numSeeds;
    }

    public void setNumSeeds(Integer numSeeds) {
        this.numSeeds = numSeeds;
    }

    public Integer getTotalPeers() {
        return totalPeers;
    }

    public void setTotalPeers(Integer totalPeers) {
        this.totalPeers = totalPeers;
    }

    public Integer getTotalSeeds() {
        return totalSeeds;
    }

    public void setTotalSeeds(Integer totalSeeds) {
        this.totalSeeds = totalSeeds;
    }
}
