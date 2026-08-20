package report.butt.mediamanager.model.tdarr;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * One file document from Tdarr's file table ({@code POST /api/v2/client/search}). Tdarr returns a large document per
 * file — the full ffprobe, MediaInfo and exiftool output — of which only the health/transcode verdict, the reported
 * sizes and the path (used to confirm the match) are mapped here; everything else is ignored.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@NullMarked
public class TdarrFile {

    /** The document key, which is the file's full path (identical to {@link #file} in practice). */
    @JsonProperty("_id")
    private @Nullable String id;

    /** The file's full path as Tdarr sees it, e.g. {@code /media/TV/Show/Season 1/Show - S01E01.mkv}. */
    @JsonProperty("file")
    private @Nullable String file;

    /** Tdarr's health-check verdict, e.g. {@code Success}, {@code Queued}, {@code Not attempted}. */
    @JsonProperty("HealthCheck")
    private @Nullable String healthCheck;

    /** Tdarr's transcode verdict, e.g. {@code Transcode success}, {@code Queued}, {@code Not required}. */
    @JsonProperty("TranscodeDecisionMaker")
    private @Nullable String transcodeDecisionMaker;

    /**
     * Pre-transcode size in GiB. Tdarr reports this as a fractional GiB value, not bytes — a 145,165,214-byte file
     * comes back as {@code 0.135...}.
     */
    @JsonProperty("oldSize")
    private @Nullable Double oldSize;

    /** Post-transcode size in GiB; same unit as {@link #oldSize}. */
    @JsonProperty("newSize")
    private @Nullable Double newSize;

    public @Nullable String getId() {
        return id;
    }

    public void setId(@Nullable String id) {
        this.id = id;
    }

    public @Nullable String getFile() {
        return file;
    }

    public void setFile(@Nullable String file) {
        this.file = file;
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

    /** The path Tdarr reports for this file, preferring {@link #file} and falling back to the {@code _id} key. */
    public @Nullable String path() {
        return file != null ? file : id;
    }

    @Override
    public String toString() {
        return String.format(
                "TdarrFile{file=%s, healthCheck=%s, transcodeDecisionMaker=%s, oldSize=%s, newSize=%s}",
                path(), healthCheck, transcodeDecisionMaker, oldSize, newSize);
    }
}
