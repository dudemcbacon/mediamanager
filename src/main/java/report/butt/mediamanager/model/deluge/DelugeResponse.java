package report.butt.mediamanager.model.deluge;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/** Envelope for a Deluge JSON-RPC reply: {@code result} on success, {@code error} on failure. */
@JsonIgnoreProperties(ignoreUnknown = true)
@NullMarked
public class DelugeResponse<T> {

    @JsonProperty("result")
    private @Nullable T result;

    @JsonProperty("error")
    private @Nullable DelugeRpcError error;

    @JsonProperty("id")
    private @Nullable Integer id;

    public @Nullable T getResult() {
        return result;
    }

    public void setResult(@Nullable T result) {
        this.result = result;
    }

    public @Nullable DelugeRpcError getError() {
        return error;
    }

    public void setError(@Nullable DelugeRpcError error) {
        this.error = error;
    }

    public @Nullable Integer getId() {
        return id;
    }

    public void setId(@Nullable Integer id) {
        this.id = id;
    }
}
