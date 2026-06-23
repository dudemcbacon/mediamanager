package report.butt.mediamanager.model;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@Entity
@Table(
        name = "request",
        uniqueConstraints =
                @UniqueConstraint(
                        name = "uk_request_ombi_id_type",
                        columnNames = {"ombi_request_id", "request_type"}))
@Inheritance(strategy = InheritanceType.JOINED)
@DiscriminatorColumn(name = "request_type")
@NullMarked
public abstract class Request {

    protected static final String OMBI_AVAILABLE_STATUS = "Common.Available";

    @Id
    @GeneratedValue
    private @Nullable Long id;

    private @Nullable String title;
    private @Nullable Boolean ombiAvailable;

    private @Nullable Integer ombiRequestId;
    private @Nullable String ombiRequestStatus;
    private @Nullable String ombiUserName;
    private @Nullable Instant ombiRequestedDate;

    private @Nullable Boolean stale;

    @Column(columnDefinition = "TEXT")
    private @Nullable String staleReason;

    private @Nullable Instant markedStaleAt;

    @Column(columnDefinition = "TEXT")
    private @Nullable String plexMetadataUrl;

    private @Nullable String plexMetadataId;
    private @Nullable Long plexAddedAt;
    private @Nullable Long plexUpdatedAt;
    private @Nullable Integer plexMediaId;

    @Column(columnDefinition = "TEXT")
    private @Nullable String plexMediaFilename;

    private @Nullable Long plexMediaSize;
    private @Nullable Long plexMediaDuration;

    @CreationTimestamp
    private @Nullable Instant createdAt;

    @UpdateTimestamp
    private @Nullable Instant updatedAt;

    public abstract boolean isAvailable();

    public @Nullable Long getId() {
        return this.id;
    }

    public @Nullable String getTitle() {
        return this.title;
    }

    public @Nullable Boolean getOmbiAvailable() {
        return this.ombiAvailable;
    }

    public @Nullable Integer getOmbiRequestId() {
        return this.ombiRequestId;
    }

    public @Nullable String getOmbiRequestStatus() {
        return this.ombiRequestStatus;
    }

    public void setId(@Nullable Long id) {
        this.id = id;
    }

    public void setTitle(@Nullable String title) {
        this.title = title;
    }

    public void setOmbiAvailable(@Nullable Boolean ombiAvailable) {
        this.ombiAvailable = ombiAvailable;
    }

    public void setOmbiRequestId(@Nullable Integer ombiRequestId) {
        this.ombiRequestId = ombiRequestId;
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

    public @Nullable Instant getOmbiRequestedDate() {
        return this.ombiRequestedDate;
    }

    public void setOmbiRequestedDate(@Nullable Instant ombiRequestedDate) {
        this.ombiRequestedDate = ombiRequestedDate;
    }

    public @Nullable Boolean getStale() {
        return this.stale;
    }

    public void setStale(@Nullable Boolean stale) {
        this.stale = stale;
    }

    public @Nullable String getStaleReason() {
        return this.staleReason;
    }

    public void setStaleReason(@Nullable String staleReason) {
        this.staleReason = staleReason;
    }

    public @Nullable Instant getMarkedStaleAt() {
        return this.markedStaleAt;
    }

    public void setMarkedStaleAt(@Nullable Instant markedStaleAt) {
        this.markedStaleAt = markedStaleAt;
    }

    public @Nullable String getPlexMetadataUrl() {
        return this.plexMetadataUrl;
    }

    public void setPlexMetadataUrl(@Nullable String plexMetadataUrl) {
        this.plexMetadataUrl = plexMetadataUrl;
    }

    public @Nullable String getPlexMetadataId() {
        return this.plexMetadataId;
    }

    public void setPlexMetadataId(@Nullable String plexMetadataId) {
        this.plexMetadataId = plexMetadataId;
    }

    public @Nullable Long getPlexAddedAt() {
        return this.plexAddedAt;
    }

    public void setPlexAddedAt(@Nullable Long plexAddedAt) {
        this.plexAddedAt = plexAddedAt;
    }

    public @Nullable Long getPlexUpdatedAt() {
        return this.plexUpdatedAt;
    }

    public void setPlexUpdatedAt(@Nullable Long plexUpdatedAt) {
        this.plexUpdatedAt = plexUpdatedAt;
    }

    public @Nullable Integer getPlexMediaId() {
        return this.plexMediaId;
    }

    public void setPlexMediaId(@Nullable Integer plexMediaId) {
        this.plexMediaId = plexMediaId;
    }

    public @Nullable String getPlexMediaFilename() {
        return this.plexMediaFilename;
    }

    public void setPlexMediaFilename(@Nullable String plexMediaFilename) {
        this.plexMediaFilename = plexMediaFilename;
    }

    public @Nullable Long getPlexMediaSize() {
        return this.plexMediaSize;
    }

    public void setPlexMediaSize(@Nullable Long plexMediaSize) {
        this.plexMediaSize = plexMediaSize;
    }

    public @Nullable Long getPlexMediaDuration() {
        return this.plexMediaDuration;
    }

    public void setPlexMediaDuration(@Nullable Long plexMediaDuration) {
        this.plexMediaDuration = plexMediaDuration;
    }

    public @Nullable Instant getCreatedAt() {
        return this.createdAt;
    }

    public @Nullable Instant getUpdatedAt() {
        return this.updatedAt;
    }

    public boolean isValid(Collection<String> validatorNames, Map<String, Validation> latestByName) {
        return validatorNames.stream().allMatch(name -> {
            @Nullable Validation v = latestByName.get(name);
            return v != null && Objects.equals(v.getResult(), true);
        });
    }
}
