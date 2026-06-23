package report.butt.mediamanager.model.plex;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public class PlexMetadata {

    @JsonProperty("ratingKey")
    private @Nullable String ratingKey;

    @JsonProperty("key")
    private @Nullable String key;

    @JsonProperty("title")
    private @Nullable String title;

    @JsonProperty("year")
    private @Nullable Integer year;

    @JsonProperty("index")
    private @Nullable Integer index;

    @JsonProperty("parentIndex")
    private @Nullable Integer parentIndex;

    @JsonProperty("grandparentRatingKey")
    private @Nullable String grandparentRatingKey;

    @JsonProperty("addedAt")
    private @Nullable Long addedAt;

    @JsonProperty("updatedAt")
    private @Nullable Long updatedAt;

    @JsonProperty("Guid")
    private @Nullable List<PlexGuid> guids;

    @JsonProperty("Media")
    private @Nullable List<PlexMedia> media;

    public @Nullable String getRatingKey() {
        return ratingKey;
    }

    public void setRatingKey(@Nullable String ratingKey) {
        this.ratingKey = ratingKey;
    }

    public @Nullable String getKey() {
        return key;
    }

    public void setKey(@Nullable String key) {
        this.key = key;
    }

    public @Nullable String getTitle() {
        return title;
    }

    public void setTitle(@Nullable String title) {
        this.title = title;
    }

    public @Nullable Integer getYear() {
        return year;
    }

    public void setYear(@Nullable Integer year) {
        this.year = year;
    }

    public @Nullable Integer getIndex() {
        return index;
    }

    public void setIndex(@Nullable Integer index) {
        this.index = index;
    }

    public @Nullable Integer getParentIndex() {
        return parentIndex;
    }

    public void setParentIndex(@Nullable Integer parentIndex) {
        this.parentIndex = parentIndex;
    }

    public @Nullable String getGrandparentRatingKey() {
        return grandparentRatingKey;
    }

    public void setGrandparentRatingKey(@Nullable String grandparentRatingKey) {
        this.grandparentRatingKey = grandparentRatingKey;
    }

    public @Nullable Long getAddedAt() {
        return addedAt;
    }

    public void setAddedAt(@Nullable Long addedAt) {
        this.addedAt = addedAt;
    }

    public @Nullable Long getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(@Nullable Long updatedAt) {
        this.updatedAt = updatedAt;
    }

    public @Nullable List<PlexGuid> getGuids() {
        return guids;
    }

    public void setGuids(@Nullable List<PlexGuid> guids) {
        this.guids = guids;
    }

    public @Nullable List<PlexMedia> getMedia() {
        return media;
    }

    public void setMedia(@Nullable List<PlexMedia> media) {
        this.media = media;
    }
}
