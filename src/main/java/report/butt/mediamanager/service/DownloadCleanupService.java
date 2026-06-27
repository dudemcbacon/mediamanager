package report.butt.mediamanager.service;

import com.google.errorprone.annotations.Var;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import report.butt.mediamanager.client.RadarrClient;
import report.butt.mediamanager.client.SonarrClient;
import report.butt.mediamanager.model.TvRequest;
import report.butt.mediamanager.model.radarr.RadarrQueue;
import report.butt.mediamanager.model.radarr.RadarrQueueRecord;
import report.butt.mediamanager.model.sonarr.SonarrQueue;
import report.butt.mediamanager.model.sonarr.SonarrQueueRecord;
import report.butt.mediamanager.repository.MovieRequestRepository;
import report.butt.mediamanager.repository.TvRequestRepository;

/**
 * Bulk cleanup for the admin page's "Delete all" action: given a set of torrent hashes (the zero-seed torrents), finds
 * the matching Radarr/Sonarr queue items, deletes them (removing from the download client and blocklisting the release,
 * matching the per-item "Delete Download" action), triggers a fresh search, and refreshes each affected request.
 */
@Service
@NullMarked
public class DownloadCleanupService {

    private static final Logger log = LoggerFactory.getLogger(DownloadCleanupService.class);

    private final RadarrClient radarrClient;
    private final SonarrClient sonarrClient;
    private final MovieRequestRepository movieRequestRepository;
    private final TvRequestRepository tvRequestRepository;
    private final MovieRefreshService movieRefreshService;
    private final TvRefreshService tvRefreshService;

    public DownloadCleanupService(
            RadarrClient radarrClient,
            SonarrClient sonarrClient,
            MovieRequestRepository movieRequestRepository,
            TvRequestRepository tvRequestRepository,
            MovieRefreshService movieRefreshService,
            TvRefreshService tvRefreshService) {
        this.radarrClient = radarrClient;
        this.sonarrClient = sonarrClient;
        this.movieRequestRepository = movieRequestRepository;
        this.tvRequestRepository = tvRequestRepository;
        this.movieRefreshService = movieRefreshService;
        this.tvRefreshService = tvRefreshService;
    }

    /** Outcome of a bulk cleanup, for reporting back to the UI. */
    // Internal data carrier; its hash set is never mutated after construction.
    @SuppressWarnings("ImmutableMemberCollection")
    public record CleanupResult(
            int torrentsDeleted, int moviesReprocessed, int showsReprocessed, Set<String> deletedHashes) {}

    /**
     * Deletes every Radarr/Sonarr queue item whose download id matches one of {@code torrentHashes}, then searches and
     * refreshes the affected movies/shows. Matching is case-insensitive (download id == torrent hash).
     */
    public CleanupResult deleteTorrentsAndReprocess(@Nullable Set<String> torrentHashes) {
        if (torrentHashes == null || torrentHashes.isEmpty()) {
            return new CleanupResult(0, 0, 0, Set.of());
        }
        Set<String> hashes = torrentHashes.stream()
                .filter(Objects::nonNull)
                .map(s -> s.toLowerCase(Locale.ROOT))
                .collect(Collectors.toSet());

        // The download ids (== torrent hashes, lowercased) actually deleted, so callers can record precisely what was
        // removed rather than what they asked to remove (a hash with no matching queue record is a no-op).
        Set<String> deletedHashes = new LinkedHashSet<>();
        @Var int torrentsDeleted = 0;

        Set<Integer> movieIds = new LinkedHashSet<>();
        RadarrQueue radarrQueue = fetch(radarrClient::getQueue, "Radarr queue");
        if (radarrQueue != null && radarrQueue.getRecords() != null) {
            for (RadarrQueueRecord record : radarrQueue.getRecords()) {
                if (matches(record.getId(), record.getDownloadId(), hashes)) {
                    log.info("Deleting Radarr queue item {} ({})", record.getId(), record.getDownloadId());
                    radarrClient.deleteQueueItem(Objects.requireNonNull(record.getId()));
                    torrentsDeleted++;
                    deletedHashes.add(Objects.requireNonNull(record.getDownloadId()).toLowerCase(Locale.ROOT));
                    if (record.getMovieId() != null) {
                        movieIds.add(record.getMovieId());
                    }
                }
            }
        }

        Set<Integer> seriesIds = new LinkedHashSet<>();
        List<Integer> episodeIds = new ArrayList<>();
        SonarrQueue sonarrQueue = fetch(sonarrClient::getQueue, "Sonarr queue");
        if (sonarrQueue != null && sonarrQueue.getRecords() != null) {
            for (SonarrQueueRecord record : sonarrQueue.getRecords()) {
                if (matches(record.getId(), record.getDownloadId(), hashes)) {
                    log.info("Deleting Sonarr queue item {} ({})", record.getId(), record.getDownloadId());
                    sonarrClient.deleteQueueItem(Objects.requireNonNull(record.getId()));
                    torrentsDeleted++;
                    deletedHashes.add(Objects.requireNonNull(record.getDownloadId()).toLowerCase(Locale.ROOT));
                    if (record.getSeriesId() != null) {
                        seriesIds.add(record.getSeriesId());
                    }
                    if (record.getEpisode() != null && record.getEpisode().getId() != null) {
                        episodeIds.add(record.getEpisode().getId());
                    }
                }
            }
        }

        if (!movieIds.isEmpty()) {
            radarrClient.searchMovies(List.copyOf(movieIds));
        }
        if (!episodeIds.isEmpty()) {
            sonarrClient.searchEpisodes(episodeIds);
        }

        @Var int moviesReprocessed = 0;
        for (Integer movieId : movieIds) {
            var request = movieRequestRepository.findByRadarrRequestId(movieId);
            if (request.isPresent()) {
                movieRefreshService.refreshOne(request.get().getId());
                moviesReprocessed++;
            }
        }
        @Var int showsReprocessed = 0;
        for (Integer seriesId : seriesIds) {
            for (TvRequest request : tvRequestRepository.findBySonarrSeriesId(seriesId)) {
                tvRefreshService.refreshOne(request.getId());
                showsReprocessed++;
            }
        }

        log.info(
                "Zero-seed cleanup: deleted {} torrent(s), reprocessed {} movie(s) and {} show(s)",
                torrentsDeleted,
                moviesReprocessed,
                showsReprocessed);
        return new CleanupResult(torrentsDeleted, moviesReprocessed, showsReprocessed, deletedHashes);
    }

    private static boolean matches(@Nullable Integer queueId, @Nullable String downloadId, Set<String> hashes) {
        return queueId != null && downloadId != null && hashes.contains(downloadId.toLowerCase(Locale.ROOT));
    }

    private static <X> @Nullable X fetch(Supplier<X> call, String what) {
        try {
            return call.get();
        } catch (RuntimeException e) {
            log.warn("Failed to fetch {}; skipping its deletions", what, e);
            return null;
        }
    }
}
