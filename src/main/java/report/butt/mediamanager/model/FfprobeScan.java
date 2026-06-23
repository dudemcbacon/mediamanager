package report.butt.mediamanager.model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * One ffprobe scan of a request's local media file: the container-level {@code format} data from {@code ffprobe
 * -show_format}. The per-stream {@code -show_streams} data hangs off {@link #streams}.
 * {@link #requestId}/{@link #requestType} are a soft reference to the owning {@link Request} (the discriminator value,
 * e.g. {@code MOVIE}) rather than a foreign key, so a scan outlives its request.
 */
@Entity
@Table(name = "ffprobe_scans")
@NullMarked
public class FfprobeScan {

    @Id
    @GeneratedValue
    private @Nullable Long id;

    private Long requestId;
    private String requestType;

    @Column(columnDefinition = "TEXT")
    private @Nullable String filename;

    private @Nullable Integer nbStreams;
    private @Nullable Integer nbPrograms;
    private @Nullable String formatName;
    private @Nullable String formatLongName;
    private @Nullable Double startTime;
    private @Nullable Double duration;
    private @Nullable Long size;
    private @Nullable Long bitRate;
    private @Nullable Integer probeScore;

    @OneToMany(mappedBy = "ffprobeScan", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<FfprobeStream> streams = new ArrayList<>();

    @CreationTimestamp
    private @Nullable Instant createdAt;

    @UpdateTimestamp
    private @Nullable Instant updatedAt;

    FfprobeScan() {}

    public FfprobeScan(Long requestId, String requestType) {
        this.requestId = requestId;
        this.requestType = requestType;
    }

    /** Adds a stream and sets its back-reference so the cascade persists it against this scan. */
    public void addStream(FfprobeStream stream) {
        stream.setFfprobeScan(this);
        this.streams.add(stream);
    }

    public @Nullable Long getId() {
        return this.id;
    }

    public void setId(@Nullable Long id) {
        this.id = id;
    }

    public Long getRequestId() {
        return this.requestId;
    }

    public void setRequestId(Long requestId) {
        this.requestId = requestId;
    }

    public String getRequestType() {
        return this.requestType;
    }

    public void setRequestType(String requestType) {
        this.requestType = requestType;
    }

    public @Nullable String getFilename() {
        return this.filename;
    }

    public void setFilename(@Nullable String filename) {
        this.filename = filename;
    }

    public @Nullable Integer getNbStreams() {
        return this.nbStreams;
    }

    public void setNbStreams(@Nullable Integer nbStreams) {
        this.nbStreams = nbStreams;
    }

    public @Nullable Integer getNbPrograms() {
        return this.nbPrograms;
    }

    public void setNbPrograms(@Nullable Integer nbPrograms) {
        this.nbPrograms = nbPrograms;
    }

    public @Nullable String getFormatName() {
        return this.formatName;
    }

    public void setFormatName(@Nullable String formatName) {
        this.formatName = formatName;
    }

    public @Nullable String getFormatLongName() {
        return this.formatLongName;
    }

    public void setFormatLongName(@Nullable String formatLongName) {
        this.formatLongName = formatLongName;
    }

    public @Nullable Double getStartTime() {
        return this.startTime;
    }

    public void setStartTime(@Nullable Double startTime) {
        this.startTime = startTime;
    }

    public @Nullable Double getDuration() {
        return this.duration;
    }

    public void setDuration(@Nullable Double duration) {
        this.duration = duration;
    }

    public @Nullable Long getSize() {
        return this.size;
    }

    public void setSize(@Nullable Long size) {
        this.size = size;
    }

    public @Nullable Long getBitRate() {
        return this.bitRate;
    }

    public void setBitRate(@Nullable Long bitRate) {
        this.bitRate = bitRate;
    }

    public @Nullable Integer getProbeScore() {
        return this.probeScore;
    }

    public void setProbeScore(@Nullable Integer probeScore) {
        this.probeScore = probeScore;
    }

    public List<FfprobeStream> getStreams() {
        return this.streams;
    }

    public void setStreams(List<FfprobeStream> streams) {
        this.streams = streams;
    }

    public @Nullable Instant getCreatedAt() {
        return this.createdAt;
    }

    public @Nullable Instant getUpdatedAt() {
        return this.updatedAt;
    }

    @Override
    public String toString() {
        return String.format(
                "FfprobeScan{id=%s, requestId=%s, requestType=%s, formatName=%s, streams=%d}",
                id, requestId, requestType, formatName, streams.size());
    }
}
