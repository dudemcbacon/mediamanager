package report.butt.mediamanager.model.plex;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public class PlexMetadata {

    @JsonProperty("ratingKey")
    private String ratingKey;

    @JsonProperty("key")
    private String key;

    @JsonProperty("title")
    private String title;

    @JsonProperty("year")
    private Integer year;

    @JsonProperty("index")
    private Integer index;

    @JsonProperty("parentIndex")
    private Integer parentIndex;

    @JsonProperty("grandparentRatingKey")
    private String grandparentRatingKey;

    @JsonProperty("addedAt")
    private Long addedAt;

    @JsonProperty("updatedAt")
    private Long updatedAt;

    @JsonProperty("Guid")
    private List<PlexGuid> guids;

    @JsonProperty("Media")
    private List<PlexMedia> media;

    public String getRatingKey() {
        return ratingKey;
    }

    public void setRatingKey(String ratingKey) {
        this.ratingKey = ratingKey;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Integer getYear() {
        return year;
    }

    public void setYear(Integer year) {
        this.year = year;
    }

    public Integer getIndex() {
        return index;
    }

    public void setIndex(Integer index) {
        this.index = index;
    }

    public Integer getParentIndex() {
        return parentIndex;
    }

    public void setParentIndex(Integer parentIndex) {
        this.parentIndex = parentIndex;
    }

    public String getGrandparentRatingKey() {
        return grandparentRatingKey;
    }

    public void setGrandparentRatingKey(String grandparentRatingKey) {
        this.grandparentRatingKey = grandparentRatingKey;
    }

    public Long getAddedAt() {
        return addedAt;
    }

    public void setAddedAt(Long addedAt) {
        this.addedAt = addedAt;
    }

    public Long getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Long updatedAt) {
        this.updatedAt = updatedAt;
    }

    public List<PlexGuid> getGuids() {
        return guids;
    }

    public void setGuids(List<PlexGuid> guids) {
        this.guids = guids;
    }

    public List<PlexMedia> getMedia() {
        return media;
    }

    public void setMedia(List<PlexMedia> media) {
        this.media = media;
    }
}
