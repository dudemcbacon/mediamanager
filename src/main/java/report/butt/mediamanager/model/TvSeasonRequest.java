package report.butt.mediamanager.model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
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
        name = "tv_season_request",
        uniqueConstraints =
                @UniqueConstraint(
                        name = "uk_tv_season_request_child_season",
                        columnNames = {"tv_child_request_id", "ombi_season_number"}))
@NullMarked
public class TvSeasonRequest {

    @Id
    @GeneratedValue
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tv_child_request_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private @Nullable TvChildRequest tvChildRequest;

    @Column(name = "ombi_season_request_id")
    private @Nullable Integer ombiSeasonRequestId;

    @Column(name = "ombi_season_number")
    private @Nullable Integer ombiSeasonNumber;

    private @Nullable Boolean ombiSeasonAvailable;

    @OneToMany(mappedBy = "tvSeasonRequest", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TvEpisodeRequest> episodeRequests = new ArrayList<>();

    @CreationTimestamp
    private @Nullable Instant createdAt;

    @UpdateTimestamp
    private @Nullable Instant updatedAt;

    TvSeasonRequest() {}

    public TvSeasonRequest(
            TvChildRequest tvChildRequest,
            @Nullable Integer ombiSeasonRequestId,
            @Nullable Integer ombiSeasonNumber,
            @Nullable Boolean ombiSeasonAvailable) {
        this.tvChildRequest = tvChildRequest;
        this.ombiSeasonRequestId = ombiSeasonRequestId;
        this.ombiSeasonNumber = ombiSeasonNumber;
        this.ombiSeasonAvailable = ombiSeasonAvailable;
    }

    public Long getId() {
        return this.id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public @Nullable TvChildRequest getTvChildRequest() {
        return this.tvChildRequest;
    }

    public void setTvChildRequest(@Nullable TvChildRequest tvChildRequest) {
        this.tvChildRequest = tvChildRequest;
    }

    public @Nullable Integer getOmbiSeasonRequestId() {
        return this.ombiSeasonRequestId;
    }

    public void setOmbiSeasonRequestId(@Nullable Integer ombiSeasonRequestId) {
        this.ombiSeasonRequestId = ombiSeasonRequestId;
    }

    public @Nullable Integer getOmbiSeasonNumber() {
        return this.ombiSeasonNumber;
    }

    public void setOmbiSeasonNumber(@Nullable Integer ombiSeasonNumber) {
        this.ombiSeasonNumber = ombiSeasonNumber;
    }

    public @Nullable Boolean getOmbiSeasonAvailable() {
        return this.ombiSeasonAvailable;
    }

    public void setOmbiSeasonAvailable(@Nullable Boolean ombiSeasonAvailable) {
        this.ombiSeasonAvailable = ombiSeasonAvailable;
    }

    public List<TvEpisodeRequest> getEpisodeRequests() {
        return this.episodeRequests;
    }

    public void setEpisodeRequests(List<TvEpisodeRequest> episodeRequests) {
        this.episodeRequests = episodeRequests;
    }

    public @Nullable Instant getCreatedAt() {
        return this.createdAt;
    }

    public @Nullable Instant getUpdatedAt() {
        return this.updatedAt;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, ombiSeasonRequestId, ombiSeasonNumber, ombiSeasonAvailable);
    }

    @Override
    public String toString() {
        return String.format(
                "TvSeasonRequest{id=%s, childId=%s, ombiSeasonNumber=%d, ombiSeasonAvailable=%b}",
                id, tvChildRequest == null ? null : tvChildRequest.getId(), ombiSeasonNumber, ombiSeasonAvailable);
    }
}
