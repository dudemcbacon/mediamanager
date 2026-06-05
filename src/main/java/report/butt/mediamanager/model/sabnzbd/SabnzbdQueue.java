package report.butt.mediamanager.model.sabnzbd;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class SabnzbdQueue {

    @JsonProperty("slots")
    private List<SabnzbdSlot> slots = List.of();

    public List<SabnzbdSlot> getSlots() {
        return slots;
    }

    public void setSlots(List<SabnzbdSlot> slots) {
        this.slots = slots;
    }
}
