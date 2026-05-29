package report.butt.mediamanager.model.plex;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public class PlexMedia {

    @JsonProperty("id")
    private Integer id;

    @JsonProperty("duration")
    private Long duration;

    @JsonProperty("Part")
    private List<PlexPart> part;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Long getDuration() {
        return duration;
    }

    public void setDuration(Long duration) {
        this.duration = duration;
    }

    public List<PlexPart> getPart() {
        return part;
    }

    public void setPart(List<PlexPart> part) {
        this.part = part;
    }
}
