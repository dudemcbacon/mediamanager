package report.butt.mediamanager.model.sabnzbd;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@JsonIgnoreProperties(ignoreUnknown = true)
@NullMarked
public class SabnzbdSlot {

    @JsonProperty("nzo_id")
    private @Nullable String nzoId;

    @JsonProperty("status")
    private @Nullable String status;

    @JsonProperty("percentage")
    private @Nullable String percentage;

    public @Nullable String getNzoId() {
        return nzoId;
    }

    public void setNzoId(@Nullable String nzoId) {
        this.nzoId = nzoId;
    }

    public @Nullable String getStatus() {
        return status;
    }

    public void setStatus(@Nullable String status) {
        this.status = status;
    }

    public @Nullable String getPercentage() {
        return percentage;
    }

    public void setPercentage(@Nullable String percentage) {
        this.percentage = percentage;
    }
}
