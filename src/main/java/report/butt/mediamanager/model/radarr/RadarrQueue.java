package report.butt.mediamanager.model.radarr;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class RadarrQueue {

    @JsonProperty("totalRecords")
    private Integer totalRecords;

    @JsonProperty("records")
    private List<RadarrQueueRecord> records = List.of();

    public Integer getTotalRecords() {
        return totalRecords;
    }

    public void setTotalRecords(Integer totalRecords) {
        this.totalRecords = totalRecords;
    }

    public List<RadarrQueueRecord> getRecords() {
        return records;
    }

    public void setRecords(List<RadarrQueueRecord> records) {
        this.records = records;
    }
}
