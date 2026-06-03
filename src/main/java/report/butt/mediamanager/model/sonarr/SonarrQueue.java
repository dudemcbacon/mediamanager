package report.butt.mediamanager.model.sonarr;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class SonarrQueue {

    @JsonProperty("totalRecords")
    private Integer totalRecords;

    @JsonProperty("records")
    private List<SonarrQueueRecord> records = List.of();

    public Integer getTotalRecords() {
        return totalRecords;
    }

    public void setTotalRecords(Integer totalRecords) {
        this.totalRecords = totalRecords;
    }

    public List<SonarrQueueRecord> getRecords() {
        return records;
    }

    public void setRecords(List<SonarrQueueRecord> records) {
        this.records = records;
    }
}
