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

/**
 * One ffprobe scan of a request's local media file: the container-level {@code format} data from
 * {@code ffprobe -show_format}. The per-stream {@code -show_streams} data hangs off {@link #streams}.
 * {@link #requestId}/{@link #requestType} are a soft reference to the owning {@link Request} (the
 * discriminator value, e.g. {@code MOVIE}) rather than a foreign key, so a scan outlives its request.
 */
@Entity
@Table(name = "ffprobe_scans")
public class FfprobeScan {

    @Id
    @GeneratedValue
    private Long id;

    private Long requestId;
    private String requestType;

    @Column(columnDefinition = "TEXT")
    private String filename;

    private Integer nbStreams;
    private Integer nbPrograms;
    private String formatName;
    private String formatLongName;
    private Double startTime;
    private Double duration;
    private Long size;
    private Long bitRate;
    private Integer probeScore;

    @OneToMany(mappedBy = "ffprobeScan", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<FfprobeStream> streams = new ArrayList<>();

    @CreationTimestamp
    private Instant createdAt;

    @UpdateTimestamp
    private Instant updatedAt;

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

    public Long getId() {
        return this.id;
    }

    public void setId(Long id) {
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

    public String getFilename() {
        return this.filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public Integer getNbStreams() {
        return this.nbStreams;
    }

    public void setNbStreams(Integer nbStreams) {
        this.nbStreams = nbStreams;
    }

    public Integer getNbPrograms() {
        return this.nbPrograms;
    }

    public void setNbPrograms(Integer nbPrograms) {
        this.nbPrograms = nbPrograms;
    }

    public String getFormatName() {
        return this.formatName;
    }

    public void setFormatName(String formatName) {
        this.formatName = formatName;
    }

    public String getFormatLongName() {
        return this.formatLongName;
    }

    public void setFormatLongName(String formatLongName) {
        this.formatLongName = formatLongName;
    }

    public Double getStartTime() {
        return this.startTime;
    }

    public void setStartTime(Double startTime) {
        this.startTime = startTime;
    }

    public Double getDuration() {
        return this.duration;
    }

    public void setDuration(Double duration) {
        this.duration = duration;
    }

    public Long getSize() {
        return this.size;
    }

    public void setSize(Long size) {
        this.size = size;
    }

    public Long getBitRate() {
        return this.bitRate;
    }

    public void setBitRate(Long bitRate) {
        this.bitRate = bitRate;
    }

    public Integer getProbeScore() {
        return this.probeScore;
    }

    public void setProbeScore(Integer probeScore) {
        this.probeScore = probeScore;
    }

    public List<FfprobeStream> getStreams() {
        return this.streams;
    }

    public void setStreams(List<FfprobeStream> streams) {
        this.streams = streams;
    }

    public Instant getCreatedAt() {
        return this.createdAt;
    }

    public Instant getUpdatedAt() {
        return this.updatedAt;
    }

    @Override
    public String toString() {
        return String.format(
                "FfprobeScan{id=%s, requestId=%s, requestType=%s, formatName=%s, streams=%d}",
                id, requestId, requestType, formatName, streams.size());
    }
}
