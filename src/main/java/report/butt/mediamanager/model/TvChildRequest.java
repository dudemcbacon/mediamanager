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

@Entity
@Table(
        name = "tv_child_request",
        uniqueConstraints =
                @UniqueConstraint(
                        name = "uk_tv_child_request_ombi_id",
                        columnNames = {"ombi_request_id"}))
public class TvChildRequest {

    private static final String OMBI_AVAILABLE_STATUS = "Common.Available";

    @Id
    @GeneratedValue
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "parent_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private TvRequest parent;

    private Integer ombiParentRequestId;

    private String title;
    private Integer tvdbId;
    private Boolean ombiAvailable;
    private Integer ombiRequestId;
    private String ombiRequestStatus;
    private String ombiUserName;
    private Integer ombiTotalSeasons;

    @OneToMany(mappedBy = "tvChildRequest", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TvSeasonRequest> seasonRequests = new ArrayList<>();

    @CreationTimestamp
    private Instant createdAt;

    @UpdateTimestamp
    private Instant updatedAt;

    TvChildRequest() {}

    public TvChildRequest(
            TvRequest parent,
            String title,
            Integer tvdbId,
            Boolean ombiAvailable,
            Integer ombiRequestId,
            String ombiRequestStatus) {
        this.parent = parent;
        this.title = title;
        this.tvdbId = tvdbId;
        this.ombiAvailable = ombiAvailable;
        this.ombiRequestId = ombiRequestId;
        this.ombiRequestStatus = ombiRequestStatus;
        this.ombiParentRequestId = parent.getOmbiRequestId();
    }

    public Long getId() {
        return this.id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public TvRequest getParent() {
        return this.parent;
    }

    public void setParent(TvRequest parent) {
        this.parent = parent;
    }

    public Integer getOmbiParentRequestId() {
        return this.ombiParentRequestId;
    }

    public void setOmbiParentRequestId(Integer ombiParentRequestId) {
        this.ombiParentRequestId = ombiParentRequestId;
    }

    public String getTitle() {
        return this.title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Integer getTvdbId() {
        return this.tvdbId;
    }

    public void setTvdbId(Integer tvdbId) {
        this.tvdbId = tvdbId;
    }

    public Boolean getOmbiAvailable() {
        return this.ombiAvailable;
    }

    public void setOmbiAvailable(Boolean ombiAvailable) {
        this.ombiAvailable = ombiAvailable;
    }

    public Integer getOmbiRequestId() {
        return this.ombiRequestId;
    }

    public void setOmbiRequestId(Integer ombiRequestId) {
        this.ombiRequestId = ombiRequestId;
    }

    public String getOmbiRequestStatus() {
        return this.ombiRequestStatus;
    }

    public void setOmbiRequestStatus(String ombiRequestStatus) {
        this.ombiRequestStatus = ombiRequestStatus;
    }

    public String getOmbiUserName() {
        return this.ombiUserName;
    }

    public void setOmbiUserName(String ombiUserName) {
        this.ombiUserName = ombiUserName;
    }

    public Integer getOmbiTotalSeasons() {
        return this.ombiTotalSeasons;
    }

    public void setOmbiTotalSeasons(Integer ombiTotalSeasons) {
        this.ombiTotalSeasons = ombiTotalSeasons;
    }

    public List<TvSeasonRequest> getSeasonRequests() {
        return this.seasonRequests;
    }

    public void setSeasonRequests(List<TvSeasonRequest> seasonRequests) {
        this.seasonRequests = seasonRequests;
    }

    public Instant getCreatedAt() {
        return this.createdAt;
    }

    public Instant getUpdatedAt() {
        return this.updatedAt;
    }

    public boolean isAvailable() {
        return Boolean.TRUE.equals(ombiAvailable) && OMBI_AVAILABLE_STATUS.equals(ombiRequestStatus);
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
