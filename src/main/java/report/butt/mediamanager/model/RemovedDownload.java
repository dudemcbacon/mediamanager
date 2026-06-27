package report.butt.mediamanager.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * Records that a stuck torrent (by info hash) was auto-removed by a notification run, capturing what it was so the
 * summary email and the Notifications page can show the cleanup. The whole table is rewritten each run (see
 * {@code NotificationService}), so it always holds exactly the most recent run's removals — "removed since the last
 * run".
 */
@Entity
@Table(name = "removed_download")
@NullMarked
public class RemovedDownload {

    @Id
    private String hash;

    @Column(columnDefinition = "text")
    private @Nullable String name;

    private double progress;

    private double stuckness;

    @Column(columnDefinition = "text")
    private @Nullable String linkedRequest;

    private Instant removedAt;

    RemovedDownload() {}

    public RemovedDownload(
            String hash,
            @Nullable String name,
            double progress,
            double stuckness,
            @Nullable String linkedRequest,
            Instant removedAt) {
        this.hash = hash;
        this.name = name;
        this.progress = progress;
        this.stuckness = stuckness;
        this.linkedRequest = linkedRequest;
        this.removedAt = removedAt;
    }

    public String getHash() {
        return hash;
    }

    public @Nullable String getName() {
        return name;
    }

    public double getProgress() {
        return progress;
    }

    public double getStuckness() {
        return stuckness;
    }

    public @Nullable String getLinkedRequest() {
        return linkedRequest;
    }

    public Instant getRemovedAt() {
        return removedAt;
    }
}
