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

/**
 * One media stream from an ffprobe scan ({@code ffprobe -show_streams}): a representative subset of the
 * codec/video/audio fields. Belongs to a single {@link FfprobeScan}.
 */
@Entity
@Table(name = "ffprobe_streams")
public class FfprobeStream {

    @Id
    @GeneratedValue
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ffprobe_scan_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private FfprobeScan ffprobeScan;

    // "index" is a reserved word in most SQL dialects, so map to stream_index.
    @Column(name = "stream_index")
    private Integer streamIndex;

    private String codecName;
    private String codecLongName;
    private String codecType;
    private Integer width;
    private Integer height;
    private String pixFmt;
    private Integer sampleRate;
    private Integer channels;
    private String channelLayout;
    private Long bitRate;
    private Double duration;
    private Long nbFrames;
    private String rFrameRate;
    private String avgFrameRate;

    @CreationTimestamp
    private Instant createdAt;

    @UpdateTimestamp
    private Instant updatedAt;

    public FfprobeStream() {}

    public Long getId() {
        return this.id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public FfprobeScan getFfprobeScan() {
        return this.ffprobeScan;
    }

    public void setFfprobeScan(FfprobeScan ffprobeScan) {
        this.ffprobeScan = ffprobeScan;
    }

    public Integer getStreamIndex() {
        return this.streamIndex;
    }

    public void setStreamIndex(Integer streamIndex) {
        this.streamIndex = streamIndex;
    }

    public String getCodecName() {
        return this.codecName;
    }

    public void setCodecName(String codecName) {
        this.codecName = codecName;
    }

    public String getCodecLongName() {
        return this.codecLongName;
    }

    public void setCodecLongName(String codecLongName) {
        this.codecLongName = codecLongName;
    }

    public String getCodecType() {
        return this.codecType;
    }

    public void setCodecType(String codecType) {
        this.codecType = codecType;
    }

    public Integer getWidth() {
        return this.width;
    }

    public void setWidth(Integer width) {
        this.width = width;
    }

    public Integer getHeight() {
        return this.height;
    }

    public void setHeight(Integer height) {
        this.height = height;
    }

    public String getPixFmt() {
        return this.pixFmt;
    }

    public void setPixFmt(String pixFmt) {
        this.pixFmt = pixFmt;
    }

    public Integer getSampleRate() {
        return this.sampleRate;
    }

    public void setSampleRate(Integer sampleRate) {
        this.sampleRate = sampleRate;
    }

    public Integer getChannels() {
        return this.channels;
    }

    public void setChannels(Integer channels) {
        this.channels = channels;
    }

    public String getChannelLayout() {
        return this.channelLayout;
    }

    public void setChannelLayout(String channelLayout) {
        this.channelLayout = channelLayout;
    }

    public Long getBitRate() {
        return this.bitRate;
    }

    public void setBitRate(Long bitRate) {
        this.bitRate = bitRate;
    }

    public Double getDuration() {
        return this.duration;
    }

    public void setDuration(Double duration) {
        this.duration = duration;
    }

    public Long getNbFrames() {
        return this.nbFrames;
    }

    public void setNbFrames(Long nbFrames) {
        this.nbFrames = nbFrames;
    }

    public String getRFrameRate() {
        return this.rFrameRate;
    }

    public void setRFrameRate(String rFrameRate) {
        this.rFrameRate = rFrameRate;
    }

    public String getAvgFrameRate() {
        return this.avgFrameRate;
    }

    public void setAvgFrameRate(String avgFrameRate) {
        this.avgFrameRate = avgFrameRate;
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
                "FfprobeStream{id=%s, streamIndex=%s, codecType=%s, codecName=%s}",
                id, streamIndex, codecType, codecName);
    }
}
