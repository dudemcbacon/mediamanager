package report.butt.mediamanager.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import java.util.Objects;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.hibernate.annotations.UpdateTimestamp;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@Entity
@Table(
        name = "tv_episode_request",
        uniqueConstraints =
                @UniqueConstraint(
                        name = "uk_tv_episode_request_season_episode",
                        columnNames = {"tv_season_request_id", "ombi_episode_number"}))
@NullMarked
public class TvEpisodeRequest {

    @Id
    @GeneratedValue
    private @Nullable Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tv_season_request_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private @Nullable TvSeasonRequest tvSeasonRequest;

    @Column(name = "ombi_episode_id")
    private @Nullable Integer ombiEpisodeId;

    @Column(name = "ombi_episode_number")
    private @Nullable Integer ombiEpisodeNumber;

    private @Nullable String ombiTitle;
    private @Nullable Boolean ombiAvailable;
    private @Nullable Boolean ombiApproved;
    private @Nullable Boolean ombiRequested;
    private @Nullable String ombiRequestStatus;

    @Column(columnDefinition = "TEXT")
    private @Nullable String sonarrPath;

    @Column(columnDefinition = "TEXT")
    private @Nullable String plexPath;

    /** Size in bytes of the episode's Plex media file, when known. */
    private @Nullable Long plexMediaSize;

    /** Whether {@link #sonarrPath} resolves to an existing file on the local filesystem (prefix applied). */
    private @Nullable Boolean localFilePathAvailable;

    /** Size in bytes of the local file, when {@link #localFilePathAvailable} is true; otherwise null. */
    private @Nullable Long localFileSize;

    private @Nullable Instant sonarrLastSearchTime;

    @CreationTimestamp
    private @Nullable Instant createdAt;

    @UpdateTimestamp
    private @Nullable Instant updatedAt;

    TvEpisodeRequest() {}

    public TvEpisodeRequest(
            TvSeasonRequest tvSeasonRequest, @Nullable Integer ombiEpisodeId, @Nullable Integer ombiEpisodeNumber) {
        this.tvSeasonRequest = tvSeasonRequest;
        this.ombiEpisodeId = ombiEpisodeId;
        this.ombiEpisodeNumber = ombiEpisodeNumber;
    }

    public @Nullable Long getId() {
        return this.id;
    }

    public void setId(@Nullable Long id) {
        this.id = id;
    }

    public @Nullable TvSeasonRequest getTvSeasonRequest() {
        return this.tvSeasonRequest;
    }

    public void setTvSeasonRequest(@Nullable TvSeasonRequest tvSeasonRequest) {
        this.tvSeasonRequest = tvSeasonRequest;
    }

    public @Nullable Integer getOmbiEpisodeId() {
        return this.ombiEpisodeId;
    }

    public void setOmbiEpisodeId(@Nullable Integer ombiEpisodeId) {
        this.ombiEpisodeId = ombiEpisodeId;
    }

    public @Nullable Integer getOmbiEpisodeNumber() {
        return this.ombiEpisodeNumber;
    }

    public void setOmbiEpisodeNumber(@Nullable Integer ombiEpisodeNumber) {
        this.ombiEpisodeNumber = ombiEpisodeNumber;
    }

    public @Nullable String getOmbiTitle() {
        return this.ombiTitle;
    }

    public void setOmbiTitle(@Nullable String ombiTitle) {
        this.ombiTitle = ombiTitle;
    }

    public @Nullable Boolean getOmbiAvailable() {
        return this.ombiAvailable;
    }

    public void setOmbiAvailable(@Nullable Boolean ombiAvailable) {
        this.ombiAvailable = ombiAvailable;
    }

    public @Nullable Boolean getOmbiApproved() {
        return this.ombiApproved;
    }

    public void setOmbiApproved(@Nullable Boolean ombiApproved) {
        this.ombiApproved = ombiApproved;
    }

    public @Nullable Boolean getOmbiRequested() {
        return this.ombiRequested;
    }

    public void setOmbiRequested(@Nullable Boolean ombiRequested) {
        this.ombiRequested = ombiRequested;
    }

    public @Nullable String getOmbiRequestStatus() {
        return this.ombiRequestStatus;
    }

    public void setOmbiRequestStatus(@Nullable String ombiRequestStatus) {
        this.ombiRequestStatus = ombiRequestStatus;
    }

    public @Nullable String getSonarrPath() {
        return this.sonarrPath;
    }

    public void setSonarrPath(@Nullable String sonarrPath) {
        this.sonarrPath = sonarrPath;
    }

    public @Nullable String getPlexPath() {
        return this.plexPath;
    }

    public void setPlexPath(@Nullable String plexPath) {
        this.plexPath = plexPath;
    }

    public @Nullable Long getPlexMediaSize() {
        return this.plexMediaSize;
    }

    public void setPlexMediaSize(@Nullable Long plexMediaSize) {
        this.plexMediaSize = plexMediaSize;
    }

    public @Nullable Boolean getLocalFilePathAvailable() {
        return this.localFilePathAvailable;
    }

    public void setLocalFilePathAvailable(@Nullable Boolean localFilePathAvailable) {
        this.localFilePathAvailable = localFilePathAvailable;
    }

    public @Nullable Long getLocalFileSize() {
        return this.localFileSize;
    }

    public void setLocalFileSize(@Nullable Long localFileSize) {
        this.localFileSize = localFileSize;
    }

    public @Nullable Instant getSonarrLastSearchTime() {
        return this.sonarrLastSearchTime;
    }

    public void setSonarrLastSearchTime(@Nullable Instant sonarrLastSearchTime) {
        this.sonarrLastSearchTime = sonarrLastSearchTime;
    }

    public @Nullable Instant getCreatedAt() {
        return this.createdAt;
    }

    public @Nullable Instant getUpdatedAt() {
        return this.updatedAt;
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                id,
                ombiEpisodeId,
                ombiEpisodeNumber,
                ombiTitle,
                ombiAvailable,
                ombiApproved,
                ombiRequested,
                ombiRequestStatus,
                sonarrPath,
                plexPath,
                plexMediaSize,
                localFilePathAvailable,
                localFileSize,
                sonarrLastSearchTime);
    }

    @Override
    public String toString() {
        return String.format(
                "TvEpisodeRequest{id=%s, seasonId=%s, ombiEpisodeNumber=%d, ombiAvailable=%b}",
                id, tvSeasonRequest == null ? null : tvSeasonRequest.getId(), ombiEpisodeNumber, ombiAvailable);
    }
}
