package report.butt.mediamanager.model.radarr;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@JsonIgnoreProperties(ignoreUnknown = true)
@NullMarked
public class RadarrQueue {

    @JsonProperty("totalRecords")
    private @Nullable Integer totalRecords;

    @JsonProperty("records")
    private @Nullable List<RadarrQueueRecord> records = List.of();

    public @Nullable Integer getTotalRecords() {
        return totalRecords;
    }

    public void setTotalRecords(@Nullable Integer totalRecords) {
        this.totalRecords = totalRecords;
    }

    public @Nullable List<RadarrQueueRecord> getRecords() {
        return records;
    }

    public void setRecords(@Nullable List<RadarrQueueRecord> records) {
        this.records = records;
    }
}
