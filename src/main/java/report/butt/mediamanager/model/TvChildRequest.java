package report.butt.mediamanager.model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

@Entity
@Table(name = "tv_child_request")
@DiscriminatorValue("TV_CHILD")
@OnDelete(action = OnDeleteAction.CASCADE)
public class TvChildRequest extends TvRequest {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "parent_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private TvRequest parent;

    private Integer ombiParentRequestId;

    @OneToMany(mappedBy = "tvChildRequest", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TvSeasonRequest> seasonRequests = new ArrayList<>();

    TvChildRequest() {}

    public TvChildRequest(
            TvRequest parent,
            String title,
            Integer tvdbId,
            Boolean ombiAvailable,
            Integer ombiRequestId,
            String ombiRequestStatus) {
        super(title, tvdbId, ombiAvailable, ombiRequestId, ombiRequestStatus);
        this.parent = parent;
        this.ombiParentRequestId = parent.getOmbiRequestId();
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

    public List<TvSeasonRequest> getSeasonRequests() {
        return this.seasonRequests;
    }

    public void setSeasonRequests(List<TvSeasonRequest> seasonRequests) {
        this.seasonRequests = seasonRequests;
    }

    @Override
    public boolean isAvailable() {
        return Boolean.TRUE.equals(getOmbiAvailable()) && OMBI_AVAILABLE_STATUS.equals(getOmbiRequestStatus());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getId(), getTitle(), getTvdbId(), getOmbiRequestId());
    }

    @Override
    public String toString() {
        return String.format(
                "TvChildRequest{id=%s, parentId=%s, title=%s, ombiRequestId=%d, ombiRequestStatus=%s}",
                getId(),
                parent == null ? null : parent.getId(),
                getTitle(),
                getOmbiRequestId(),
                getOmbiRequestStatus());
    }
}
