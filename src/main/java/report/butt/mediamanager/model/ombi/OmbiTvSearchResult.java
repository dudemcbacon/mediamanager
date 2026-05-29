package report.butt.mediamanager.model.ombi;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class OmbiTvSearchResult {

    @JsonProperty("seasonRequests")
    private List<OmbiTvSeasonRequest> seasonRequests;

    public List<OmbiTvSeasonRequest> getSeasonRequests() {
        return seasonRequests;
    }

    public void setSeasonRequests(List<OmbiTvSeasonRequest> seasonRequests) {
        this.seasonRequests = seasonRequests;
    }
}
