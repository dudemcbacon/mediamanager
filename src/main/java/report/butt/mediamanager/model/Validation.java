package report.butt.mediamanager.model;

import jakarta.persistence.CheckConstraint;
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
// Exactly one of (request_id, tv_episode_id) is set: a validation belongs to either a request or a
// TV episode, never both and never neither. Enforced by the check constraint below and in db/migrations.
@Table(
        uniqueConstraints = {
            @UniqueConstraint(
                    name = "uk_validation_name_request",
                    columnNames = {"validation_name", "request_id"}),
            @UniqueConstraint(
                    name = "uk_validation_name_tv_episode",
                    columnNames = {"validation_name", "tv_episode_id"})
        },
        check =
                @CheckConstraint(
                        name = "chk_validation_request_xor_episode",
                        constraint = "(request_id IS NULL) <> (tv_episode_id IS NULL)"))
@NullMarked
public class Validation {
    @Id
    @GeneratedValue
    private @Nullable Long id;

    private String validationName;
    private @Nullable Boolean result;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "request_id")
    @OnDelete(action = OnDeleteAction.CASCADE)
    private @Nullable Request request;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tv_episode_id")
    @OnDelete(action = OnDeleteAction.CASCADE)
    private @Nullable TvEpisodeRequest tvEpisode;

    @CreationTimestamp
    private @Nullable Instant createdAt;

    @UpdateTimestamp
    private @Nullable Instant updatedAt;

    Validation() {}

    public Validation(String validationName, @Nullable Boolean result, Request request) {
        this.validationName = validationName;
        this.result = result;
        this.request = request;
    }

    public Validation(String validationName, @Nullable Boolean result, TvEpisodeRequest tvEpisode) {
        this.validationName = validationName;
        this.result = result;
        this.tvEpisode = tvEpisode;
    }

    public @Nullable Long getId() {
        return this.id;
    }

    public String getValidationName() {
        return this.validationName;
    }

    public @Nullable Boolean getResult() {
        return this.result;
    }

    public void setId(@Nullable Long id) {
        this.id = id;
    }

    public void setValidationName(String validationName) {
        this.validationName = validationName;
    }

    public void setResult(@Nullable Boolean result) {
        this.result = result;
    }

    public @Nullable Request getRequest() {
        return this.request;
    }

    public @Nullable TvEpisodeRequest getTvEpisode() {
        return this.tvEpisode;
    }

    public @Nullable Instant getCreatedAt() {
        return this.createdAt;
    }

    public @Nullable Instant getUpdatedAt() {
        return this.updatedAt;
    }

    public void setRequest(@Nullable Request request) {
        this.request = request;
    }

    public void setTvEpisode(@Nullable TvEpisodeRequest tvEpisode) {
        this.tvEpisode = tvEpisode;
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.id, this.validationName, this.result);
    }

    @Override
    public String toString() {
        return String.format(
                "Validation{id=%d, validationName=%s, result=%b}", this.id, this.validationName, this.result);
    }
}
