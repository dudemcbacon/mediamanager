package report.butt.mediamanager.model.sabnzbd;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/** Envelope for SABnzbd's {@code mode=queue} reply; the queue's slots are the active downloads. */
@JsonIgnoreProperties(ignoreUnknown = true)
public class SabnzbdResponse {

    @JsonProperty("queue")
    private SabnzbdQueue queue;

    public SabnzbdQueue getQueue() {
        return queue;
    }

    public void setQueue(SabnzbdQueue queue) {
        this.queue = queue;
    }
}
