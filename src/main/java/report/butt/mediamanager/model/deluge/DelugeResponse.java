package report.butt.mediamanager.model.deluge;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/** Envelope for a Deluge JSON-RPC reply: {@code result} on success, {@code error} on failure. */
@JsonIgnoreProperties(ignoreUnknown = true)
public class DelugeResponse<T> {

    @JsonProperty("result")
    private T result;

    @JsonProperty("error")
    private DelugeRpcError error;

    @JsonProperty("id")
    private Integer id;

    public T getResult() {
        return result;
    }

    public void setResult(T result) {
        this.result = result;
    }

    public DelugeRpcError getError() {
        return error;
    }

    public void setError(DelugeRpcError error) {
        this.error = error;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }
}
