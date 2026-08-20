package report.butt.mediamanager.model.tdarr;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/** The envelope Tdarr wraps search hits in: {@code {"array": [...], "totalCount": n}}. */
@JsonIgnoreProperties(ignoreUnknown = true)
@NullMarked
public class TdarrSearchResponse {

    @JsonProperty("array")
    private @Nullable List<TdarrFile> array;

    @JsonProperty("totalCount")
    private @Nullable Integer totalCount;

    public @Nullable List<TdarrFile> getArray() {
        return array;
    }

    public void setArray(@Nullable List<TdarrFile> array) {
        this.array = array;
    }

    public @Nullable Integer getTotalCount() {
        return totalCount;
    }

    public void setTotalCount(@Nullable Integer totalCount) {
        this.totalCount = totalCount;
    }

    /** The hits, or an empty list when Tdarr returned no {@code array}. */
    public List<TdarrFile> files() {
        return array == null ? List.of() : array;
    }
}
