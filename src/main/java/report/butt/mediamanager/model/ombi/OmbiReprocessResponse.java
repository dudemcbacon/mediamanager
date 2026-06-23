package report.butt.mediamanager.model.ombi;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@JsonIgnoreProperties(ignoreUnknown = true)
@NullMarked
public class OmbiReprocessResponse {

    private @Nullable Boolean result;
    private @Nullable String message;
    private @Nullable Boolean isError;
    private @Nullable String errorMessage;
    private @Nullable String errorCode;
    private @Nullable Integer requestId;

    public @Nullable Boolean getResult() {
        return result;
    }

    public void setResult(@Nullable Boolean result) {
        this.result = result;
    }

    public @Nullable String getMessage() {
        return message;
    }

    public void setMessage(@Nullable String message) {
        this.message = message;
    }

    public @Nullable Boolean getIsError() {
        return isError;
    }

    public void setIsError(@Nullable Boolean isError) {
        this.isError = isError;
    }

    public @Nullable String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(@Nullable String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public @Nullable String getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(@Nullable String errorCode) {
        this.errorCode = errorCode;
    }

    public @Nullable Integer getRequestId() {
        return requestId;
    }

    public void setRequestId(@Nullable Integer requestId) {
        this.requestId = requestId;
    }
}
