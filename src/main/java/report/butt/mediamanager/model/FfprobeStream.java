package report.butt.mediamanager.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.hibernate.annotations.UpdateTimestamp;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * One media stream from an ffprobe scan ({@code ffprobe -show_streams}): a representative subset of the
 * codec/video/audio fields. Belongs to a single {@link FfprobeScan}.
 */
@Entity
@Table(name = "ffprobe_streams")
@NullMarked
public class FfprobeStream {

    @Id
    @GeneratedValue
    private @Nullable Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ffprobe_scan_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private FfprobeScan ffprobeScan;

    // "index" is a reserved word in most SQL dialects, so map to stream_index.
    @Column(name = "stream_index")
    private @Nullable Integer streamIndex;

    private @Nullable String codecName;
    private @Nullable String codecLongName;
    private @Nullable String codecType;
    private @Nullable Integer width;
    private @Nullable Integer height;
    private @Nullable String pixFmt;
    private @Nullable Integer sampleRate;
    private @Nullable Integer channels;
    private @Nullable String channelLayout;
    private @Nullable Long bitRate;
    private @Nullable Double duration;
    private @Nullable Long nbFrames;
    private @Nullable String rFrameRate;
    private @Nullable String avgFrameRate;

    @CreationTimestamp
    private @Nullable Instant createdAt;

    @UpdateTimestamp
    private @Nullable Instant updatedAt;

    public FfprobeStream() {}

    public @Nullable Long getId() {
        return this.id;
    }

    public void setId(@Nullable Long id) {
        this.id = id;
    }

    public FfprobeScan getFfprobeScan() {
        return this.ffprobeScan;
    }

    public void setFfprobeScan(FfprobeScan ffprobeScan) {
        this.ffprobeScan = ffprobeScan;
    }

    public @Nullable Integer getStreamIndex() {
        return this.streamIndex;
    }

    public void setStreamIndex(@Nullable Integer streamIndex) {
        this.streamIndex = streamIndex;
    }

    public @Nullable String getCodecName() {
        return this.codecName;
    }

    public void setCodecName(@Nullable String codecName) {
        this.codecName = codecName;
    }

    public @Nullable String getCodecLongName() {
        return this.codecLongName;
    }

    public void setCodecLongName(@Nullable String codecLongName) {
        this.codecLongName = codecLongName;
    }

    public @Nullable String getCodecType() {
        return this.codecType;
    }

    public void setCodecType(@Nullable String codecType) {
        this.codecType = codecType;
    }

    public @Nullable Integer getWidth() {
        return this.width;
    }

    public void setWidth(@Nullable Integer width) {
        this.width = width;
    }

    public @Nullable Integer getHeight() {
        return this.height;
    }

    public void setHeight(@Nullable Integer height) {
        this.height = height;
    }

    public @Nullable String getPixFmt() {
        return this.pixFmt;
    }

    public void setPixFmt(@Nullable String pixFmt) {
        this.pixFmt = pixFmt;
    }

    public @Nullable Integer getSampleRate() {
        return this.sampleRate;
    }

    public void setSampleRate(@Nullable Integer sampleRate) {
        this.sampleRate = sampleRate;
    }

    public @Nullable Integer getChannels() {
        return this.channels;
    }

    public void setChannels(@Nullable Integer channels) {
        this.channels = channels;
    }

    public @Nullable String getChannelLayout() {
        return this.channelLayout;
    }

    public void setChannelLayout(@Nullable String channelLayout) {
        this.channelLayout = channelLayout;
    }

    public @Nullable Long getBitRate() {
        return this.bitRate;
    }

    public void setBitRate(@Nullable Long bitRate) {
        this.bitRate = bitRate;
    }

    public @Nullable Double getDuration() {
        return this.duration;
    }

    public void setDuration(@Nullable Double duration) {
        this.duration = duration;
    }

    public @Nullable Long getNbFrames() {
        return this.nbFrames;
    }

    public void setNbFrames(@Nullable Long nbFrames) {
        this.nbFrames = nbFrames;
    }

    public @Nullable String getRFrameRate() {
        return this.rFrameRate;
    }

    public void setRFrameRate(@Nullable String rFrameRate) {
        this.rFrameRate = rFrameRate;
    }

    public @Nullable String getAvgFrameRate() {
        return this.avgFrameRate;
    }

    public void setAvgFrameRate(@Nullable String avgFrameRate) {
        this.avgFrameRate = avgFrameRate;
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
                "FfprobeStream{id=%s, streamIndex=%s, codecType=%s, codecName=%s}",
                id, streamIndex, codecType, codecName);
    }
}
