package report.butt.mediamanager.model.sabnzbd;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class SabnzbdSlot {

    @JsonProperty("nzo_id")
    private String nzoId;

    @JsonProperty("status")
    private String status;

    @JsonProperty("percentage")
    private String percentage;

    public String getNzoId() {
        return nzoId;
    }

    public void setNzoId(String nzoId) {
        this.nzoId = nzoId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getPercentage() {
        return percentage;
    }

    public void setPercentage(String percentage) {
        this.percentage = percentage;
    }
}
