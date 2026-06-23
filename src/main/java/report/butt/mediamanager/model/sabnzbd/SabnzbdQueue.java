package report.butt.mediamanager.model.sabnzbd;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@JsonIgnoreProperties(ignoreUnknown = true)
@NullMarked
public class SabnzbdQueue {

    @JsonProperty("slots")
    private @Nullable List<SabnzbdSlot> slots = List.of();

    public @Nullable List<SabnzbdSlot> getSlots() {
        return slots;
    }

    public void setSlots(@Nullable List<SabnzbdSlot> slots) {
        this.slots = slots;
    }
}
