package report.butt.mediamanager.model.sabnzbd;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/** Envelope for SABnzbd's {@code mode=queue} reply; the queue's slots are the active downloads. */
@JsonIgnoreProperties(ignoreUnknown = true)
@NullMarked
public class SabnzbdResponse {

    @JsonProperty("queue")
    private @Nullable SabnzbdQueue queue;

    public @Nullable SabnzbdQueue getQueue() {
        return queue;
    }

    public void setQueue(@Nullable SabnzbdQueue queue) {
        this.queue = queue;
    }
}
