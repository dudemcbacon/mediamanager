package report.butt.mediamanager.model.ombi;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public class OmbiMarkAvailableRequest {

    @JsonProperty("id")
    private @Nullable Integer id;

    @JsonProperty("is4K")
    private Boolean is4K = false;

    public OmbiMarkAvailableRequest(Integer id) {
        this.id = id;
    }

    public @Nullable Integer getId() {
        return id;
    }

    public void setId(@Nullable Integer id) {
        this.id = id;
    }

    public Boolean getIs4K() {
        return is4K;
    }

    public void setIs4K(Boolean is4K) {
        this.is4K = is4K;
    }
}
