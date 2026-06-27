package report.butt.mediamanager.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import org.hibernate.annotations.UpdateTimestamp;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * Records that a torrent (by info hash) has had no seeds since {@link #seedlessSince}. A row exists only while the
 * torrent is currently seedless: it is created the first sweep its seed count hits zero and removed once seeds return
 * or the torrent disappears (see {@code SeedlessTorrentTracker}), so the table stays small. The elapsed time since
 * {@link #seedlessSince} feeds the drought term of a download's {@link report.butt.mediamanager.service.Stuckness}.
 */
@Entity
@Table(name = "seedless_torrent")
@NullMarked
public class SeedlessTorrent {

    @Id
    private String hash;

    private Instant seedlessSince;

    @UpdateTimestamp
    private @Nullable Instant updatedAt;

    SeedlessTorrent() {}

    public SeedlessTorrent(String hash, Instant seedlessSince) {
        this.hash = hash;
        this.seedlessSince = seedlessSince;
    }

    public String getHash() {
        return hash;
    }

    public void setHash(String hash) {
        this.hash = hash;
    }

    public Instant getSeedlessSince() {
        return seedlessSince;
    }

    public void setSeedlessSince(Instant seedlessSince) {
        this.seedlessSince = seedlessSince;
    }

    public @Nullable Instant getUpdatedAt() {
        return updatedAt;
    }
}
