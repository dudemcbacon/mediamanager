package report.butt.mediamanager.model;

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
import org.hibernate.annotations.Check;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(
        uniqueConstraints = {
            @UniqueConstraint(
                    name = "uk_validation_name_request",
                    columnNames = {"validation_name", "request_id"}),
            @UniqueConstraint(
                    name = "uk_validation_name_tv_episode",
                    columnNames = {"validation_name", "tv_episode_id"})
        })
// Exactly one of (request_id, tv_episode_id) is set: a validation belongs to either a
// request or a TV episode, never both and never neither. Also enforced in db/migrations.
@Check(
        name = "chk_validation_request_xor_episode",
        constraints = "(request_id IS NULL) <> (tv_episode_id IS NULL)")
public class Validation {
    private @Id @GeneratedValue Long id;
    private String validationName;
    private Boolean result;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "request_id")
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Request request;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tv_episode_id")
    @OnDelete(action = OnDeleteAction.CASCADE)
    private TvEpisodeRequest tvEpisode;

    @CreationTimestamp
    private Instant createdAt;

    @UpdateTimestamp
    private Instant updatedAt;

    Validation() {}

    public Validation(String validationName, Boolean result, Request request) {
        this.validationName = validationName;
        this.result = result;
        this.request = request;
    }

    public Validation(String validationName, Boolean result, TvEpisodeRequest tvEpisode) {
        this.validationName = validationName;
        this.result = result;
        this.tvEpisode = tvEpisode;
    }

    public Long getId() {
        return this.id;
    }

    public String getValidationName() {
        return this.validationName;
    }

    public Boolean getResult() {
        return this.result;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setValidationName(String validationName) {
        this.validationName = validationName;
    }

    public void setResult(Boolean result) {
        this.result = result;
    }

    public Request getRequest() {
        return this.request;
    }

    public TvEpisodeRequest getTvEpisode() {
        return this.tvEpisode;
    }

    public Instant getCreatedAt() {
        return this.createdAt;
    }

    public void setRequest(Request request) {
        this.request = request;
    }

    public void setTvEpisode(TvEpisodeRequest tvEpisode) {
        this.tvEpisode = tvEpisode;
    }

    public int hashCode() {
        return Objects.hash(this.id, this.validationName, this.result);
    }

    public String toString() {
        return String.format(
                "Validation{id=%d, validationName=%s, result=%b}", this.id, this.validationName, this.result);
    }
}
