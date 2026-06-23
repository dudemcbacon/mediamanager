package report.butt.mediamanager.model.ombi;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@JsonIgnoreProperties(ignoreUnknown = true)
@NullMarked
public class OmbiTvSearchResult {

    @JsonProperty("seasonRequests")
    private @Nullable List<OmbiTvSeasonRequest> seasonRequests;

    public @Nullable List<OmbiTvSeasonRequest> getSeasonRequests() {
        return seasonRequests;
    }

    public void setSeasonRequests(@Nullable List<OmbiTvSeasonRequest> seasonRequests) {
        this.seasonRequests = seasonRequests;
    }
}
