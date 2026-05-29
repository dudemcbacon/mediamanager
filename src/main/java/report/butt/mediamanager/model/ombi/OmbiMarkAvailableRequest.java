package report.butt.mediamanager.model.ombi;

import com.fasterxml.jackson.annotation.JsonProperty;

public class OmbiMarkAvailableRequest {

    @JsonProperty("id")
    private Integer id;

    @JsonProperty("is4K")
    private Boolean is4K = false;

    public OmbiMarkAvailableRequest(Integer id) {
        this.id = id;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Boolean getIs4K() {
        return is4K;
    }

    public void setIs4K(Boolean is4K) {
        this.is4K = is4K;
    }
}
