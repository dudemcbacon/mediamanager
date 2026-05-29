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
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(
        name = "tv_season_request",
        uniqueConstraints =
                @UniqueConstraint(
                        name = "uk_tv_season_request_child_season",
                        columnNames = {"tv_child_request_id", "ombi_season_number"}))
public class TvSeasonRequest {

    @Id
    @GeneratedValue
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tv_child_request_id", nullable = false)
    private TvChildRequest tvChildRequest;

    @Column(name = "ombi_season_request_id")
    private Integer ombiSeasonRequestId;

    @Column(name = "ombi_season_number")
    private Integer ombiSeasonNumber;

    private Boolean ombiSeasonAvailable;

    @OneToMany(mappedBy = "tvSeasonRequest", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TvEpisodeRequest> episodeRequests = new ArrayList<>();

    @CreationTimestamp
    private Instant createdAt;

    @UpdateTimestamp
    private Instant updatedAt;

    TvSeasonRequest() {}

    public TvSeasonRequest(
            TvChildRequest tvChildRequest,
            Integer ombiSeasonRequestId,
            Integer ombiSeasonNumber,
            Boolean ombiSeasonAvailable) {
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

    public TvChildRequest getTvChildRequest() {
        return this.tvChildRequest;
    }

    public void setTvChildRequest(TvChildRequest tvChildRequest) {
        this.tvChildRequest = tvChildRequest;
    }

    public Integer getOmbiSeasonRequestId() {
        return this.ombiSeasonRequestId;
    }

    public void setOmbiSeasonRequestId(Integer ombiSeasonRequestId) {
        this.ombiSeasonRequestId = ombiSeasonRequestId;
    }

    public Integer getOmbiSeasonNumber() {
        return this.ombiSeasonNumber;
    }

    public void setOmbiSeasonNumber(Integer ombiSeasonNumber) {
        this.ombiSeasonNumber = ombiSeasonNumber;
    }

    public Boolean getOmbiSeasonAvailable() {
        return this.ombiSeasonAvailable;
    }

    public void setOmbiSeasonAvailable(Boolean ombiSeasonAvailable) {
        this.ombiSeasonAvailable = ombiSeasonAvailable;
    }

    public List<TvEpisodeRequest> getEpisodeRequests() {
        return this.episodeRequests;
    }

    public void setEpisodeRequests(List<TvEpisodeRequest> episodeRequests) {
        this.episodeRequests = episodeRequests;
    }

    public Instant getCreatedAt() {
        return this.createdAt;
    }

    public Instant getUpdatedAt() {
        return this.updatedAt;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, ombiSeasonRequestId, ombiSeasonNumber);
    }

    @Override
    public String toString() {
        return String.format(
                "TvSeasonRequest{id=%s, childId=%s, ombiSeasonNumber=%d, ombiSeasonAvailable=%b}",
                id, tvChildRequest == null ? null : tvChildRequest.getId(), ombiSeasonNumber, ombiSeasonAvailable);
    }
}
