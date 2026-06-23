package report.butt.mediamanager.model.radarr;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.annotation.processing.Generated;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
    "audioBitrate",
    "audioChannels",
    "audioCodec",
    "audioLanguages",
    "audioStreamCount",
    "videoBitDepth",
    "videoBitrate",
    "videoCodec",
    "videoFps",
    "videoDynamicRange",
    "videoDynamicRangeType",
    "resolution",
    "runTime",
    "scanType",
    "subtitles"
})
@Generated("jsonschema2pojo")
@NullMarked
public class MediaInfo {

    @JsonProperty("audioBitrate")
    private @Nullable Integer audioBitrate;

    @JsonProperty("audioChannels")
    private @Nullable Double audioChannels;

    @JsonProperty("audioCodec")
    private @Nullable String audioCodec;

    @JsonProperty("audioLanguages")
    private @Nullable String audioLanguages;

    @JsonProperty("audioStreamCount")
    private @Nullable Integer audioStreamCount;

    @JsonProperty("videoBitDepth")
    private @Nullable Integer videoBitDepth;

    @JsonProperty("videoBitrate")
    private @Nullable Integer videoBitrate;

    @JsonProperty("videoCodec")
    private @Nullable String videoCodec;

    @JsonProperty("videoFps")
    private @Nullable Double videoFps;

    @JsonProperty("videoDynamicRange")
    private @Nullable String videoDynamicRange;

    @JsonProperty("videoDynamicRangeType")
    private @Nullable String videoDynamicRangeType;

    @JsonProperty("resolution")
    private @Nullable String resolution;

    @JsonProperty("runTime")
    private @Nullable String runTime;

    @JsonProperty("scanType")
    private @Nullable String scanType;

    @JsonProperty("subtitles")
    private @Nullable String subtitles;

    @JsonIgnore
    private Map<String, Object> additionalProperties = new LinkedHashMap<String, Object>();

    @JsonProperty("audioBitrate")
    public @Nullable Integer getAudioBitrate() {
        return audioBitrate;
    }

    @JsonProperty("audioBitrate")
    public void setAudioBitrate(@Nullable Integer audioBitrate) {
        this.audioBitrate = audioBitrate;
    }

    @JsonProperty("audioChannels")
    public @Nullable Double getAudioChannels() {
        return audioChannels;
    }

    @JsonProperty("audioChannels")
    public void setAudioChannels(@Nullable Double audioChannels) {
        this.audioChannels = audioChannels;
    }

    @JsonProperty("audioCodec")
    public @Nullable String getAudioCodec() {
        return audioCodec;
    }

    @JsonProperty("audioCodec")
    public void setAudioCodec(@Nullable String audioCodec) {
        this.audioCodec = audioCodec;
    }

    @JsonProperty("audioLanguages")
    public @Nullable String getAudioLanguages() {
        return audioLanguages;
    }

    @JsonProperty("audioLanguages")
    public void setAudioLanguages(@Nullable String audioLanguages) {
        this.audioLanguages = audioLanguages;
    }

    @JsonProperty("audioStreamCount")
    public @Nullable Integer getAudioStreamCount() {
        return audioStreamCount;
    }

    @JsonProperty("audioStreamCount")
    public void setAudioStreamCount(@Nullable Integer audioStreamCount) {
        this.audioStreamCount = audioStreamCount;
    }

    @JsonProperty("videoBitDepth")
    public @Nullable Integer getVideoBitDepth() {
        return videoBitDepth;
    }

    @JsonProperty("videoBitDepth")
    public void setVideoBitDepth(@Nullable Integer videoBitDepth) {
        this.videoBitDepth = videoBitDepth;
    }

    @JsonProperty("videoBitrate")
    public @Nullable Integer getVideoBitrate() {
        return videoBitrate;
    }

    @JsonProperty("videoBitrate")
    public void setVideoBitrate(@Nullable Integer videoBitrate) {
        this.videoBitrate = videoBitrate;
    }

    @JsonProperty("videoCodec")
    public @Nullable String getVideoCodec() {
        return videoCodec;
    }

    @JsonProperty("videoCodec")
    public void setVideoCodec(@Nullable String videoCodec) {
        this.videoCodec = videoCodec;
    }

    @JsonProperty("videoFps")
    public @Nullable Double getVideoFps() {
        return videoFps;
    }

    @JsonProperty("videoFps")
    public void setVideoFps(@Nullable Double videoFps) {
        this.videoFps = videoFps;
    }

    @JsonProperty("videoDynamicRange")
    public @Nullable String getVideoDynamicRange() {
        return videoDynamicRange;
    }

    @JsonProperty("videoDynamicRange")
    public void setVideoDynamicRange(@Nullable String videoDynamicRange) {
        this.videoDynamicRange = videoDynamicRange;
    }

    @JsonProperty("videoDynamicRangeType")
    public @Nullable String getVideoDynamicRangeType() {
        return videoDynamicRangeType;
    }

    @JsonProperty("videoDynamicRangeType")
    public void setVideoDynamicRangeType(@Nullable String videoDynamicRangeType) {
        this.videoDynamicRangeType = videoDynamicRangeType;
    }

    @JsonProperty("resolution")
    public @Nullable String getResolution() {
        return resolution;
    }

    @JsonProperty("resolution")
    public void setResolution(@Nullable String resolution) {
        this.resolution = resolution;
    }

    @JsonProperty("runTime")
    public @Nullable String getRunTime() {
        return runTime;
    }

    @JsonProperty("runTime")
    public void setRunTime(@Nullable String runTime) {
        this.runTime = runTime;
    }

    @JsonProperty("scanType")
    public @Nullable String getScanType() {
        return scanType;
    }

    @JsonProperty("scanType")
    public void setScanType(@Nullable String scanType) {
        this.scanType = scanType;
    }

    @JsonProperty("subtitles")
    public @Nullable String getSubtitles() {
        return subtitles;
    }

    @JsonProperty("subtitles")
    public void setSubtitles(@Nullable String subtitles) {
        this.subtitles = subtitles;
    }

    @JsonAnyGetter
    public Map<String, Object> getAdditionalProperties() {
        return this.additionalProperties;
    }

    @JsonAnySetter
    public void setAdditionalProperty(String name, Object value) {
        this.additionalProperties.put(name, value);
    }
}
