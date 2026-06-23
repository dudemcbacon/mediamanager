package report.butt.mediamanager.model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.hibernate.annotations.UpdateTimestamp;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@Entity
@Table(
        name = "tv_child_request",
        uniqueConstraints =
                @UniqueConstraint(
                        name = "uk_tv_child_request_ombi_id",
                        columnNames = {"ombi_request_id"}))
@NullMarked
public class TvChildRequest {

    private static final String OMBI_AVAILABLE_STATUS = "Common.Available";

    @Id
    @GeneratedValue
    private @Nullable Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "parent_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private @Nullable TvRequest parent;

    private @Nullable Integer ombiParentRequestId;

    private @Nullable String title;
    private @Nullable Integer tvdbId;
    private @Nullable Boolean ombiAvailable;
    private @Nullable Integer ombiRequestId;
    private @Nullable String ombiRequestStatus;
    private @Nullable String ombiUserName;
    private @Nullable Integer ombiTotalSeasons;

    @OneToMany(mappedBy = "tvChildRequest", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TvSeasonRequest> seasonRequests = new ArrayList<>();

    @CreationTimestamp
    private @Nullable Instant createdAt;

    @UpdateTimestamp
    private @Nullable Instant updatedAt;

    TvChildRequest() {}

    public TvChildRequest(
            TvRequest parent,
            @Nullable String title,
            @Nullable Integer tvdbId,
            @Nullable Boolean ombiAvailable,
            @Nullable Integer ombiRequestId,
            @Nullable String ombiRequestStatus) {
        this.parent = parent;
        this.title = title;
        this.tvdbId = tvdbId;
        this.ombiAvailable = ombiAvailable;
        this.ombiRequestId = ombiRequestId;
        this.ombiRequestStatus = ombiRequestStatus;
        this.ombiParentRequestId = parent.getOmbiRequestId();
    }

    public @Nullable Long getId() {
        return this.id;
    }

    public void setId(@Nullable Long id) {
        this.id = id;
    }

    public @Nullable TvRequest getParent() {
        return this.parent;
    }

    public void setParent(@Nullable TvRequest parent) {
        this.parent = parent;
    }

    public @Nullable Integer getOmbiParentRequestId() {
        return this.ombiParentRequestId;
    }

    public void setOmbiParentRequestId(@Nullable Integer ombiParentRequestId) {
        this.ombiParentRequestId = ombiParentRequestId;
    }

    public @Nullable String getTitle() {
        return this.title;
    }

    public void setTitle(@Nullable String title) {
        this.title = title;
    }

    public @Nullable Integer getTvdbId() {
        return this.tvdbId;
    }

    public void setTvdbId(@Nullable Integer tvdbId) {
        this.tvdbId = tvdbId;
    }

    public @Nullable Boolean getOmbiAvailable() {
        return this.ombiAvailable;
    }

    public void setOmbiAvailable(@Nullable Boolean ombiAvailable) {
        this.ombiAvailable = ombiAvailable;
    }

    public @Nullable Integer getOmbiRequestId() {
        return this.ombiRequestId;
    }

    public void setOmbiRequestId(@Nullable Integer ombiRequestId) {
        this.ombiRequestId = ombiRequestId;
    }

    public @Nullable String getOmbiRequestStatus() {
        return this.ombiRequestStatus;
    }

    public void setOmbiRequestStatus(@Nullable String ombiRequestStatus) {
        this.ombiRequestStatus = ombiRequestStatus;
    }

    public @Nullable String getOmbiUserName() {
        return this.ombiUserName;
    }

    public void setOmbiUserName(@Nullable String ombiUserName) {
        this.ombiUserName = ombiUserName;
    }

    public @Nullable Integer getOmbiTotalSeasons() {
        return this.ombiTotalSeasons;
    }

    public void setOmbiTotalSeasons(@Nullable Integer ombiTotalSeasons) {
        this.ombiTotalSeasons = ombiTotalSeasons;
    }

    public List<TvSeasonRequest> getSeasonRequests() {
        return this.seasonRequests;
    }

    public void setSeasonRequests(List<TvSeasonRequest> seasonRequests) {
        this.seasonRequests = seasonRequests;
    }

    public @Nullable Instant getCreatedAt() {
        return this.createdAt;
    }

    public @Nullable Instant getUpdatedAt() {
        return this.updatedAt;
    }

    public boolean isAvailable() {
        return Objects.equals(ombiAvailable, true) && Objects.equals(ombiRequestStatus, OMBI_AVAILABLE_STATUS);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                id,
                title,
                tvdbId,
                ombiAvailable,
                ombiRequestId,
                ombiRequestStatus,
                ombiUserName,
                ombiTotalSeasons,
                ombiParentRequestId);
    }

    @Override
    public String toString() {
        return String.format(
                "TvChildRequest{id=%s, parentId=%s, title=%s, ombiRequestId=%d, ombiRequestStatus=%s}",
                id, parent == null ? null : parent.getId(), title, ombiRequestId, ombiRequestStatus);
    }
}
