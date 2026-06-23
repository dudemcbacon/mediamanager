package report.butt.mediamanager.model.radarr;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.processing.Generated;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
    "movieId",
    "relativePath",
    "path",
    "size",
    "dateAdded",
    "edition",
    "languages",
    "quality",
    "indexerFlags",
    "mediaInfo",
    "qualityCutoffNotMet",
    "id"
})
@Generated("jsonschema2pojo")
@NullMarked
public class Moviefile {

    @JsonProperty("movieId")
    private @Nullable Integer movieId;

    @JsonProperty("relativePath")
    private @Nullable String relativePath;

    @JsonProperty("path")
    private @Nullable String path;

    @JsonProperty("size")
    private @Nullable Long size;

    @JsonProperty("dateAdded")
    private @Nullable String dateAdded;

    @JsonProperty("edition")
    private @Nullable String edition;

    @JsonProperty("languages")
    private @Nullable List<Language> languages;

    @JsonProperty("quality")
    private @Nullable Quality quality;

    @JsonProperty("indexerFlags")
    private @Nullable Integer indexerFlags;

    @JsonProperty("mediaInfo")
    private @Nullable MediaInfo mediaInfo;

    @JsonProperty("qualityCutoffNotMet")
    private @Nullable Boolean qualityCutoffNotMet;

    @JsonProperty("id")
    private @Nullable Integer id;

    @JsonIgnore
    private Map<String, Object> additionalProperties = new LinkedHashMap<String, Object>();

    @JsonProperty("movieId")
    public @Nullable Integer getMovieId() {
        return movieId;
    }

    @JsonProperty("movieId")
    public void setMovieId(@Nullable Integer movieId) {
        this.movieId = movieId;
    }

    @JsonProperty("relativePath")
    public @Nullable String getRelativePath() {
        return relativePath;
    }

    @JsonProperty("relativePath")
    public void setRelativePath(@Nullable String relativePath) {
        this.relativePath = relativePath;
    }

    @JsonProperty("path")
    public @Nullable String getPath() {
        return path;
    }

    @JsonProperty("path")
    public void setPath(@Nullable String path) {
        this.path = path;
    }

    @JsonProperty("size")
    public @Nullable Long getSize() {
        return size;
    }

    @JsonProperty("size")
    public void setSize(@Nullable Long size) {
        this.size = size;
    }

    @JsonProperty("dateAdded")
    public @Nullable String getDateAdded() {
        return dateAdded;
    }

    @JsonProperty("dateAdded")
    public void setDateAdded(@Nullable String dateAdded) {
        this.dateAdded = dateAdded;
    }

    @JsonProperty("edition")
    public @Nullable String getEdition() {
        return edition;
    }

    @JsonProperty("edition")
    public void setEdition(@Nullable String edition) {
        this.edition = edition;
    }

    @JsonProperty("languages")
    public @Nullable List<Language> getLanguages() {
        return languages;
    }

    @JsonProperty("languages")
    public void setLanguages(@Nullable List<Language> languages) {
        this.languages = languages;
    }

    @JsonProperty("quality")
    public @Nullable Quality getQuality() {
        return quality;
    }

    @JsonProperty("quality")
    public void setQuality(@Nullable Quality quality) {
        this.quality = quality;
    }

    @JsonProperty("indexerFlags")
    public @Nullable Integer getIndexerFlags() {
        return indexerFlags;
    }

    @JsonProperty("indexerFlags")
    public void setIndexerFlags(@Nullable Integer indexerFlags) {
        this.indexerFlags = indexerFlags;
    }

    @JsonProperty("mediaInfo")
    public @Nullable MediaInfo getMediaInfo() {
        return mediaInfo;
    }

    @JsonProperty("mediaInfo")
    public void setMediaInfo(@Nullable MediaInfo mediaInfo) {
        this.mediaInfo = mediaInfo;
    }

    @JsonProperty("qualityCutoffNotMet")
    public @Nullable Boolean getQualityCutoffNotMet() {
        return qualityCutoffNotMet;
    }

    @JsonProperty("qualityCutoffNotMet")
    public void setQualityCutoffNotMet(@Nullable Boolean qualityCutoffNotMet) {
        this.qualityCutoffNotMet = qualityCutoffNotMet;
    }

    @JsonProperty("id")
    public @Nullable Integer getId() {
        return id;
    }

    @JsonProperty("id")
    public void setId(@Nullable Integer id) {
        this.id = id;
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
