package report.butt.mediamanager.model.plex;

import com.fasterxml.jackson.annotation.JsonProperty;

public class PlexPart {

    @JsonProperty("id")
    private Integer id;

    @JsonProperty("file")
    private String file;

    @JsonProperty("size")
    private Long size;

    @JsonProperty("duration")
    private Long duration;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getFile() {
        return file;
    }

    public void setFile(String file) {
        this.file = file;
    }

    public Long getSize() {
        return size;
    }

    public void setSize(Long size) {
        this.size = size;
    }

    public Long getDuration() {
        return duration;
    }

    public void setDuration(Long duration) {
        this.duration = duration;
    }
}
