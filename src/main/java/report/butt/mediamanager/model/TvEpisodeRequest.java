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

@Entity
@Table(
        name = "tv_episode_request",
        uniqueConstraints =
                @UniqueConstraint(
                        name = "uk_tv_episode_request_season_episode",
                        columnNames = {"tv_season_request_id", "ombi_episode_number"}))
public class TvEpisodeRequest {

    @Id
    @GeneratedValue
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tv_season_request_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private TvSeasonRequest tvSeasonRequest;

    @Column(name = "ombi_episode_id")
    private Integer ombiEpisodeId;

    @Column(name = "ombi_episode_number")
    private Integer ombiEpisodeNumber;

    private String ombiTitle;
    private Boolean ombiAvailable;
    private Boolean ombiApproved;
    private Boolean ombiRequested;
    private String ombiRequestStatus;

    @Column(columnDefinition = "TEXT")
    private String sonarrPath;

    @Column(columnDefinition = "TEXT")
    private String plexPath;

    private Instant sonarrLastSearchTime;

    @CreationTimestamp
    private Instant createdAt;

    @UpdateTimestamp
    private Instant updatedAt;

    TvEpisodeRequest() {}

    public TvEpisodeRequest(TvSeasonRequest tvSeasonRequest, Integer ombiEpisodeId, Integer ombiEpisodeNumber) {
        this.tvSeasonRequest = tvSeasonRequest;
        this.ombiEpisodeId = ombiEpisodeId;
        this.ombiEpisodeNumber = ombiEpisodeNumber;
    }

    public Long getId() {
        return this.id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public TvSeasonRequest getTvSeasonRequest() {
        return this.tvSeasonRequest;
    }

    public void setTvSeasonRequest(TvSeasonRequest tvSeasonRequest) {
        this.tvSeasonRequest = tvSeasonRequest;
    }

    public Integer getOmbiEpisodeId() {
        return this.ombiEpisodeId;
    }

    public void setOmbiEpisodeId(Integer ombiEpisodeId) {
        this.ombiEpisodeId = ombiEpisodeId;
    }

    public Integer getOmbiEpisodeNumber() {
        return this.ombiEpisodeNumber;
    }

    public void setOmbiEpisodeNumber(Integer ombiEpisodeNumber) {
        this.ombiEpisodeNumber = ombiEpisodeNumber;
    }

    public String getOmbiTitle() {
        return this.ombiTitle;
    }

    public void setOmbiTitle(String ombiTitle) {
        this.ombiTitle = ombiTitle;
    }

    public Boolean getOmbiAvailable() {
        return this.ombiAvailable;
    }

    public void setOmbiAvailable(Boolean ombiAvailable) {
        this.ombiAvailable = ombiAvailable;
    }

    public Boolean getOmbiApproved() {
        return this.ombiApproved;
    }

    public void setOmbiApproved(Boolean ombiApproved) {
        this.ombiApproved = ombiApproved;
    }

    public Boolean getOmbiRequested() {
        return this.ombiRequested;
    }

    public void setOmbiRequested(Boolean ombiRequested) {
        this.ombiRequested = ombiRequested;
    }

    public String getOmbiRequestStatus() {
        return this.ombiRequestStatus;
    }

    public void setOmbiRequestStatus(String ombiRequestStatus) {
        this.ombiRequestStatus = ombiRequestStatus;
    }

    public String getSonarrPath() {
        return this.sonarrPath;
    }

    public void setSonarrPath(String sonarrPath) {
        this.sonarrPath = sonarrPath;
    }

    public String getPlexPath() {
        return this.plexPath;
    }

    public void setPlexPath(String plexPath) {
        this.plexPath = plexPath;
    }

    public Instant getSonarrLastSearchTime() {
        return this.sonarrLastSearchTime;
    }

    public void setSonarrLastSearchTime(Instant sonarrLastSearchTime) {
        this.sonarrLastSearchTime = sonarrLastSearchTime;
    }

    public Instant getCreatedAt() {
        return this.createdAt;
    }

    public Instant getUpdatedAt() {
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
                sonarrLastSearchTime);
    }

    @Override
    public String toString() {
        return String.format(
                "TvEpisodeRequest{id=%s, seasonId=%s, ombiEpisodeNumber=%d, ombiAvailable=%b}",
                id, tvSeasonRequest == null ? null : tvSeasonRequest.getId(), ombiEpisodeNumber, ombiAvailable);
    }
}
