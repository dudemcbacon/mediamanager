package report.butt.mediamanager.model.plex;

import com.fasterxml.jackson.annotation.JsonProperty;

public class PlexGuid {

    @JsonProperty("id")
    private String id;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }
}
