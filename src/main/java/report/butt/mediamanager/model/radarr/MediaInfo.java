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
public class MediaInfo {

    @JsonProperty("audioBitrate")
    private Integer audioBitrate;

    @JsonProperty("audioChannels")
    private Double audioChannels;

    @JsonProperty("audioCodec")
    private String audioCodec;

    @JsonProperty("audioLanguages")
    private String audioLanguages;

    @JsonProperty("audioStreamCount")
    private Integer audioStreamCount;

    @JsonProperty("videoBitDepth")
    private Integer videoBitDepth;

    @JsonProperty("videoBitrate")
    private Integer videoBitrate;

    @JsonProperty("videoCodec")
    private String videoCodec;

    @JsonProperty("videoFps")
    private Double videoFps;

    @JsonProperty("videoDynamicRange")
    private String videoDynamicRange;

    @JsonProperty("videoDynamicRangeType")
    private String videoDynamicRangeType;

    @JsonProperty("resolution")
    private String resolution;

    @JsonProperty("runTime")
    private String runTime;

    @JsonProperty("scanType")
    private String scanType;

    @JsonProperty("subtitles")
    private String subtitles;

    @JsonIgnore
    private Map<String, Object> additionalProperties = new LinkedHashMap<String, Object>();

    @JsonProperty("audioBitrate")
    public Integer getAudioBitrate() {
        return audioBitrate;
    }

    @JsonProperty("audioBitrate")
    public void setAudioBitrate(Integer audioBitrate) {
        this.audioBitrate = audioBitrate;
    }

    @JsonProperty("audioChannels")
    public Double getAudioChannels() {
        return audioChannels;
    }

    @JsonProperty("audioChannels")
    public void setAudioChannels(Double audioChannels) {
        this.audioChannels = audioChannels;
    }

    @JsonProperty("audioCodec")
    public String getAudioCodec() {
        return audioCodec;
    }

    @JsonProperty("audioCodec")
    public void setAudioCodec(String audioCodec) {
        this.audioCodec = audioCodec;
    }

    @JsonProperty("audioLanguages")
    public String getAudioLanguages() {
        return audioLanguages;
    }

    @JsonProperty("audioLanguages")
    public void setAudioLanguages(String audioLanguages) {
        this.audioLanguages = audioLanguages;
    }

    @JsonProperty("audioStreamCount")
    public Integer getAudioStreamCount() {
        return audioStreamCount;
    }

    @JsonProperty("audioStreamCount")
    public void setAudioStreamCount(Integer audioStreamCount) {
        this.audioStreamCount = audioStreamCount;
    }

    @JsonProperty("videoBitDepth")
    public Integer getVideoBitDepth() {
        return videoBitDepth;
    }

    @JsonProperty("videoBitDepth")
    public void setVideoBitDepth(Integer videoBitDepth) {
        this.videoBitDepth = videoBitDepth;
    }

    @JsonProperty("videoBitrate")
    public Integer getVideoBitrate() {
        return videoBitrate;
    }

    @JsonProperty("videoBitrate")
    public void setVideoBitrate(Integer videoBitrate) {
        this.videoBitrate = videoBitrate;
    }

    @JsonProperty("videoCodec")
    public String getVideoCodec() {
        return videoCodec;
    }

    @JsonProperty("videoCodec")
    public void setVideoCodec(String videoCodec) {
        this.videoCodec = videoCodec;
    }

    @JsonProperty("videoFps")
    public Double getVideoFps() {
        return videoFps;
    }

    @JsonProperty("videoFps")
    public void setVideoFps(Double videoFps) {
        this.videoFps = videoFps;
    }

    @JsonProperty("videoDynamicRange")
    public String getVideoDynamicRange() {
        return videoDynamicRange;
    }

    @JsonProperty("videoDynamicRange")
    public void setVideoDynamicRange(String videoDynamicRange) {
        this.videoDynamicRange = videoDynamicRange;
    }

    @JsonProperty("videoDynamicRangeType")
    public String getVideoDynamicRangeType() {
        return videoDynamicRangeType;
    }

    @JsonProperty("videoDynamicRangeType")
    public void setVideoDynamicRangeType(String videoDynamicRangeType) {
        this.videoDynamicRangeType = videoDynamicRangeType;
    }

    @JsonProperty("resolution")
    public String getResolution() {
        return resolution;
    }

    @JsonProperty("resolution")
    public void setResolution(String resolution) {
        this.resolution = resolution;
    }

    @JsonProperty("runTime")
    public String getRunTime() {
        return runTime;
    }

    @JsonProperty("runTime")
    public void setRunTime(String runTime) {
        this.runTime = runTime;
    }

    @JsonProperty("scanType")
    public String getScanType() {
        return scanType;
    }

    @JsonProperty("scanType")
    public void setScanType(String scanType) {
        this.scanType = scanType;
    }

    @JsonProperty("subtitles")
    public String getSubtitles() {
        return subtitles;
    }

    @JsonProperty("subtitles")
    public void setSubtitles(String subtitles) {
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
