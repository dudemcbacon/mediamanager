package report.butt.mediamanager.model.deluge;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@JsonIgnoreProperties(ignoreUnknown = true)
@NullMarked
public class DelugeRpcError {

    @JsonProperty("message")
    private @Nullable String message;

    @JsonProperty("code")
    private @Nullable Integer code;

    public @Nullable String getMessage() {
        return message;
    }

    public void setMessage(@Nullable String message) {
        this.message = message;
    }

    public @Nullable Integer getCode() {
        return code;
    }

    public void setCode(@Nullable Integer code) {
        this.code = code;
    }
}
