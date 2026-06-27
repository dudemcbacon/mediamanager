package report.butt.mediamanager.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.jspecify.annotations.NullMarked;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import report.butt.mediamanager.client.DelugeClient;
import report.butt.mediamanager.model.SeedlessTorrent;
import report.butt.mediamanager.model.deluge.DelugeTorrent;
import report.butt.mediamanager.repository.SeedlessTorrentRepository;

/**
 * Maintains the {@link SeedlessTorrent} table so download stuckness can tell how long a torrent has gone without seeds.
 * Each sweep: unfinished torrents with no seeds get a row stamped "seedless since now" (kept across sweeps so the
 * drought accrues), while torrents that regained seeds, finished, or disappeared have their row removed. Run from
 * {@link ScheduledRefreshJob}'s hourly sweep — day-resolution is plenty for a drought measured in months.
 */
@Service
@NullMarked
public class SeedlessTorrentTracker {

    private static final Logger log = LoggerFactory.getLogger(SeedlessTorrentTracker.class);

    private final DelugeClient delugeClient;
    private final SeedlessTorrentRepository repository;

    public SeedlessTorrentTracker(DelugeClient delugeClient, SeedlessTorrentRepository repository) {
        this.delugeClient = delugeClient;
        this.repository = repository;
    }

    public void sweep() {
        Map<String, DelugeTorrent> torrents = delugeClient.getTorrentsStatus();
        // getTorrentsStatus() returns an empty map on failure (not an exception), and an empty result would otherwise
        // look like "every torrent vanished" and clear every drought counter. Skip the sweep rather than risk that.
        if (torrents.isEmpty()) {
            log.debug("Seedless sweep skipped: no torrents returned");
            return;
        }
        Instant now = Instant.now();
        Map<String, SeedlessTorrent> existing =
                repository.findAll().stream().collect(Collectors.toMap(SeedlessTorrent::getHash, Function.identity()));

        List<SeedlessTorrent> newlySeedless = new ArrayList<>();
        Set<String> stillSeedless = new HashSet<>();
        for (Map.Entry<String, DelugeTorrent> entry : torrents.entrySet()) {
            if (!isSeedless(entry.getValue())) {
                continue;
            }
            String hash = entry.getKey().toLowerCase(Locale.ROOT);
            stillSeedless.add(hash);
            if (!existing.containsKey(hash)) {
                newlySeedless.add(new SeedlessTorrent(hash, now)); // start the clock
            }
        }
        if (!newlySeedless.isEmpty()) {
            repository.saveAll(newlySeedless);
        }
        // Drop rows whose torrent regained seeds, finished, or disappeared from the client.
        List<String> recovered = existing.keySet().stream()
                .filter(hash -> !stillSeedless.contains(hash))
                .toList();
        if (!recovered.isEmpty()) {
            repository.deleteAllById(recovered);
        }
        log.debug(
                "Seedless sweep: {} seedless torrent(s), {} newly seedless, {} cleared",
                stillSeedless.size(),
                newlySeedless.size(),
                recovered.size());
    }

    private static boolean isSeedless(DelugeTorrent torrent) {
        return torrent.getProgress() != null
                && torrent.getProgress() < 100.0
                && torrent.getTotalSeeds() != null
                && torrent.getTotalSeeds() == 0;
    }
}
