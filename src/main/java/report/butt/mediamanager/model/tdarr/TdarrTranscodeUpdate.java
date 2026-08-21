package report.butt.mediamanager.model.tdarr;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * Body of the transcode-complete webhook: the same four values a refresh would read from Tdarr, plus the file they
 * belong to. Field names match Tdarr's own JSON keys so a flow's "Send Web Request" body can pass its variables
 * straight through.
 *
 * <p>{@code filename} may be a full path ({@code /media/TV/Show/Season 1/S01E01.mkv}) or a bare filename
 * ({@code S01E01.mkv}) — see {@code TdarrUpdateService} for how each is resolved. A null field is left unchanged on the
 * matched rows rather than clearing the stored value, so a partial payload can't wipe data.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@NullMarked
public class TdarrTranscodeUpdate {

    @JsonProperty("filename")
    private @Nullable String filename;

    @JsonProperty("HealthCheck")
    private @Nullable String healthCheck;

    @JsonProperty("TranscodeDecisionMaker")
    private @Nullable String transcodeDecisionMaker;

    /** Pre-transcode size in GiB, in Tdarr's own unit (not bytes). */
    @JsonProperty("oldSize")
    private @Nullable Double oldSize;

    /** Post-transcode size in GiB; same unit as {@link #oldSize}. */
    @JsonProperty("newSize")
    private @Nullable Double newSize;

    public @Nullable String getFilename() {
        return filename;
    }

    public void setFilename(@Nullable String filename) {
        this.filename = filename;
    }

    public @Nullable String getHealthCheck() {
        return healthCheck;
    }

    public void setHealthCheck(@Nullable String healthCheck) {
        this.healthCheck = healthCheck;
    }

    public @Nullable String getTranscodeDecisionMaker() {
        return transcodeDecisionMaker;
    }

    public void setTranscodeDecisionMaker(@Nullable String transcodeDecisionMaker) {
        this.transcodeDecisionMaker = transcodeDecisionMaker;
    }

    public @Nullable Double getOldSize() {
        return oldSize;
    }

    public void setOldSize(@Nullable Double oldSize) {
        this.oldSize = oldSize;
    }

    public @Nullable Double getNewSize() {
        return newSize;
    }

    public void setNewSize(@Nullable Double newSize) {
        this.newSize = newSize;
    }

    @Override
    public String toString() {
        return String.format(
                "TdarrTranscodeUpdate{filename=%s, healthCheck=%s, transcodeDecisionMaker=%s, oldSize=%s, newSize=%s}",
                filename, healthCheck, transcodeDecisionMaker, oldSize, newSize);
    }
}
