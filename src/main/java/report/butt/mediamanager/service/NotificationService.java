package report.butt.mediamanager.service;

import com.newrelic.api.agent.Trace;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.stereotype.Service;
import report.butt.mediamanager.client.DelugeClient;
import report.butt.mediamanager.client.RadarrClient;
import report.butt.mediamanager.client.SonarrClient;
import report.butt.mediamanager.model.MovieRequest;
import report.butt.mediamanager.model.RemovedDownload;
import report.butt.mediamanager.model.Request;
import report.butt.mediamanager.model.SeedlessTorrent;
import report.butt.mediamanager.model.TvRequest;
import report.butt.mediamanager.model.deluge.DelugeTorrent;
import report.butt.mediamanager.model.radarr.RadarrHealthItem;
import report.butt.mediamanager.model.radarr.RadarrQueue;
import report.butt.mediamanager.model.radarr.RadarrQueueRecord;
import report.butt.mediamanager.model.sonarr.SonarrHealthItem;
import report.butt.mediamanager.model.sonarr.SonarrQueue;
import report.butt.mediamanager.model.sonarr.SonarrQueueRecord;
import report.butt.mediamanager.repository.MovieRequestRepository;
import report.butt.mediamanager.repository.RemovedDownloadRepository;
import report.butt.mediamanager.repository.SeedlessTorrentRepository;
import report.butt.mediamanager.repository.TvRequestRepository;

/**
 * Detects conditions worth an alert and emails a single weekly summary. Detection is done once by {@link #snapshot()},
 * which returns structured per-category rows; {@link #runCheck()} renders the email from that snapshot and the admin
 * page renders grids from it, so the two share one source of truth. A failure reaching one integration is reported (as
 * an "unreachable" finding) and its dependent checks are skipped rather than aborting the whole run. Run weekly by
 * {@link ScheduledRefreshJob} and on demand from the movie/TV views ("Test Notifications").
 */
@Service
@NullMarked
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private static final DateTimeFormatter DATE =
            DateTimeFormatter.ofPattern("yyyy-MM-dd").withZone(ZoneId.systemDefault());

    private static final String IMPORT_BLOCKED_STATE = "importBlocked";

    /** Summary sections, in the order they appear in the email. */
    public enum Category {
        UNREACHABLE,
        HEALTH_ISSUE,
        IMPORT_BLOCKED,
        DOWNLOAD,
        REMOVED_DOWNLOAD,
        OVERDUE_MOVIE,
        OVERDUE_TV,
        UNSEARCHED_MOVIE,
        UNSEARCHED_TV,
        NEW_REQUEST
    }

    private final DelugeClient delugeClient;
    private final RadarrClient radarrClient;
    private final SonarrClient sonarrClient;
    private final MovieRequestRepository movieRequestRepository;
    private final TvRequestRepository tvRequestRepository;
    private final SeedlessTorrentRepository seedlessTorrentRepository;
    private final RemovedDownloadRepository removedDownloadRepository;
    private final DownloadCleanupService downloadCleanupService;
    private final ObjectProvider<JavaMailSender> mailSenderProvider;
    private final @Nullable String from;
    private final @Nullable String to;
    private final double stucknessRemovalThreshold;
    private final int overdueRequestDays;
    private final int unsearchedDays;
    private final int newRequestWindowHours;

    // Spring constructor injection; the parameter count reflects injected collaborators, not a design smell.
    @SuppressWarnings("TooManyParameters")
    public NotificationService(
            DelugeClient delugeClient,
            RadarrClient radarrClient,
            SonarrClient sonarrClient,
            MovieRequestRepository movieRequestRepository,
            TvRequestRepository tvRequestRepository,
            SeedlessTorrentRepository seedlessTorrentRepository,
            RemovedDownloadRepository removedDownloadRepository,
            DownloadCleanupService downloadCleanupService,
            ObjectProvider<JavaMailSender> mailSenderProvider,
            @Value("${notifications.from}") String from,
            @Value("${notifications.to}") String to,
            @Value("${notifications.stuckness-removal-threshold}") double stucknessRemovalThreshold,
            @Value("${notifications.overdue-request-days}") int overdueRequestDays,
            @Value("${notifications.unsearched-days}") int unsearchedDays,
            @Value("${notifications.new-request-window-hours}") int newRequestWindowHours) {
        this.delugeClient = delugeClient;
        this.radarrClient = radarrClient;
        this.sonarrClient = sonarrClient;
        this.movieRequestRepository = movieRequestRepository;
        this.tvRequestRepository = tvRequestRepository;
        this.seedlessTorrentRepository = seedlessTorrentRepository;
        this.removedDownloadRepository = removedDownloadRepository;
        this.downloadCleanupService = downloadCleanupService;
        this.mailSenderProvider = mailSenderProvider;
        this.from = from;
        this.to = to;
        this.stucknessRemovalThreshold = stucknessRemovalThreshold;
        this.overdueRequestDays = overdueRequestDays;
        this.unsearchedDays = unsearchedDays;
        this.newRequestWindowHours = newRequestWindowHours;
    }

    /**
     * Runs the detection once and, if anything turned up, sends one summary email. Always returns per-category counts
     * so callers (e.g. the "Test Notifications" button) can report what happened even when no mail was sent.
     */
    @Trace
    public NotificationResult runCheck() {
        NotificationSnapshot detected = snapshot();
        // Auto-remove the dead downloads (stuckness >= threshold), then report off a snapshot that reflects the
        // cleanup: the removed items move into their own section instead of being double-listed as still stuck.
        NotificationSnapshot reported = autoRemoveStuck(detected);
        Map<Category, Integer> counts = reported.counts();
        int total = reported.total();
        if (total == 0) {
            log.info("Notification check found nothing to report");
            return new NotificationResult(false, mailConfigured(), counts);
        }
        boolean emailSent = send(total, buildBody(reported));
        return new NotificationResult(emailSent, mailConfigured(), counts);
    }

    /**
     * Runs every check and returns the structured findings, without sending any email. Safe to call on demand (e.g. the
     * admin page) since it only reads from the integrations.
     */
    public NotificationSnapshot snapshot() {
        Instant now = Instant.now();
        List<MovieRequest> movies = movieRequestRepository.findAll();
        List<TvRequest> tvShows = tvRequestRepository.findAll();
        List<String> unreachable = new ArrayList<>();

        // Fetch the download-client queues once: they drive import-blocked findings, link torrents (hash == queue
        // record downloadId) to their request, and give per-request queued-item counts.
        RadarrQueue radarrQueue = tryFetch("Radarr", unreachable, radarrClient::getQueue);
        SonarrQueue sonarrQueue = tryFetch("Sonarr", unreachable, sonarrClient::getQueue);
        Map<Integer, String> movieTitleByRadarrId = titleMap(movies, MovieRequest::getRadarrRequestId);
        Map<Integer, String> tvTitleBySonarrId = titleMap(tvShows, TvRequest::getSonarrSeriesId);
        Map<Integer, MovieRequest> movieByRadarrId = byId(movies, MovieRequest::getRadarrRequestId);
        Map<Integer, TvRequest> tvBySeriesId = byId(tvShows, TvRequest::getSonarrSeriesId);
        Map<String, TorrentRequest> requestByTorrentHash =
                mapTorrentsToRequests(radarrQueue, sonarrQueue, movieByRadarrId, tvBySeriesId);
        Map<Integer, Integer> queuedByMovieId =
                queueCountsById(radarrQueue == null ? null : radarrQueue.getRecords(), RadarrQueueRecord::getMovieId);
        Map<Integer, Integer> queuedBySeriesId =
                queueCountsById(sonarrQueue == null ? null : sonarrQueue.getRecords(), SonarrQueueRecord::getSeriesId);

        List<Download> downloads = findDownloads(now, unreachable, requestByTorrentHash);
        List<ImportBlocked> importBlocked =
                findImportBlocked(radarrQueue, sonarrQueue, movieTitleByRadarrId, tvTitleBySonarrId);
        List<HealthIssue> health = findHealth(unreachable);

        Instant overdueThreshold = now.minus(overdueRequestDays, ChronoUnit.DAYS);
        List<OverdueMovieRow> overdueMovies = overdue(movies, overdueThreshold).stream()
                .map(m -> new OverdueMovieRow(
                        m.getTitle(),
                        m.getOmbiRequestedDate(),
                        requesterName(m),
                        m.getRadarrLastSearchTime(),
                        queuedByMovieId.getOrDefault(m.getRadarrRequestId(), 0),
                        linkOf(m)))
                .toList();
        List<OverdueTvRow> overdueTv = overdue(tvShows, overdueThreshold).stream()
                .map(t -> new OverdueTvRow(
                        t.getTitle(),
                        t.getOmbiRequestedDate(),
                        requesterName(t),
                        t.getSonarrLastSearched(),
                        queuedBySeriesId.getOrDefault(t.getSonarrSeriesId(), 0),
                        nz(t.getSonarrEpisodeFileCount()),
                        nz(t.getSonarrTotalEpisodeCount()),
                        linkOf(t)))
                .toList();

        Instant unsearchedThreshold = now.minus(unsearchedDays, ChronoUnit.DAYS);
        List<UnsearchedRow> unsearchedMovies = movies.stream()
                .filter(NotificationService::actionableUnavailable)
                .filter(m -> m.getRadarrRequestId() != null)
                .filter(m -> beforeOrNull(m.getRadarrLastSearchTime(), unsearchedThreshold))
                .sorted(byTitle())
                .map(m -> new UnsearchedRow(m.getTitle(), m.getRadarrLastSearchTime(), m.getRadarrRequestId()))
                .toList();
        List<UnsearchedRow> unsearchedTv = tvShows.stream()
                .filter(NotificationService::actionableUnavailable)
                .filter(t -> t.getSonarrSeriesId() != null)
                .filter(t -> beforeOrNull(t.getSonarrLastSearched(), unsearchedThreshold))
                .sorted(byTitle())
                .map(t -> new UnsearchedRow(t.getTitle(), t.getSonarrLastSearched(), t.getSonarrSeriesId()))
                .toList();

        Instant newThreshold = now.minus(newRequestWindowHours, ChronoUnit.HOURS);
        List<NewRequestRow> newRequests = new ArrayList<>();
        movies.stream()
                .filter(m -> recentlyRequested(m, newThreshold))
                .forEach(m -> newRequests.add(new NewRequestRow("Movie", m.getTitle(), m.getOmbiRequestedDate())));
        tvShows.stream()
                .filter(t -> recentlyRequested(t, newThreshold))
                .forEach(t -> newRequests.add(new NewRequestRow("TV", t.getTitle(), t.getOmbiRequestedDate())));

        return new NotificationSnapshot(
                downloads,
                removedDownloads(),
                importBlocked,
                overdueMovies,
                overdueTv,
                unsearchedMovies,
                unsearchedTv,
                newRequests,
                health,
                unreachable);
    }

    /** The downloads the most recent notification run auto-removed (newest first), read read-only from the table. */
    private List<RemovedDownloadRow> removedDownloads() {
        return removedDownloadRepository.findAll().stream()
                .sorted(Comparator.comparing(RemovedDownload::getRemovedAt).reversed())
                .map(NotificationService::toRow)
                .toList();
    }

    /**
     * Removes every detected download (stuck or zero-seed) whose stuckness is at or above the configured threshold via
     * {@link DownloadCleanupService} (delete + blocklist + re-search), records exactly the ones it actually deleted —
     * replacing the table so it always holds just this run's removals — and returns a snapshot adjusted for the
     * cleanup: removed items are pulled out of the downloads list into {@code removedDownloads}.
     */
    private NotificationSnapshot autoRemoveStuck(NotificationSnapshot detected) {
        var candidates = new LinkedHashMap<String, Removable>();
        for (Download d : detected.downloads()) {
            if (d.stuckness() >= stucknessRemovalThreshold) {
                candidates.put(
                        d.hash().toLowerCase(Locale.ROOT),
                        new Removable(d.hash(), d.name(), d.progress(), d.stuckness(), d.linkedRequest()));
            }
        }
        if (candidates.isEmpty()) {
            // Nothing to remove this run; clear any leftover rows so "removed since the last run" reflects that.
            if (!detected.removedDownloads().isEmpty()) {
                removedDownloadRepository.deleteAllInBatch();
            }
            return detected;
        }

        DownloadCleanupService.CleanupResult result =
                downloadCleanupService.deleteTorrentsAndReprocess(candidates.keySet());
        Set<String> deleted = result.deletedHashes().stream()
                .map(h -> h.toLowerCase(Locale.ROOT))
                .collect(Collectors.toSet());
        Instant removedAt = Instant.now();
        List<RemovedDownload> removed = candidates.values().stream()
                .filter(c -> deleted.contains(c.hash().toLowerCase(Locale.ROOT)))
                .map(c -> new RemovedDownload(
                        c.hash(), c.name(), c.progress(), c.stuckness(), c.linkedRequest(), removedAt))
                .toList();
        removedDownloadRepository.deleteAllInBatch();
        removedDownloadRepository.saveAll(removed);
        log.info("Auto-removed {} stuck download(s) at or above stuckness {}", removed.size(), stucknessRemovalThreshold);

        return new NotificationSnapshot(
                detected.downloads().stream()
                        .filter(d -> !deleted.contains(d.hash().toLowerCase(Locale.ROOT)))
                        .toList(),
                removed.stream().map(NotificationService::toRow).toList(),
                detected.importBlocked(),
                detected.overdueMovies(),
                detected.overdueTv(),
                detected.unsearchedMovies(),
                detected.unsearchedTv(),
                detected.newRequests(),
                detected.healthIssues(),
                detected.unreachableIntegrations());
    }

    private static RemovedDownloadRow toRow(RemovedDownload r) {
        return new RemovedDownloadRow(
                r.getName(), r.getProgress(), r.getStuckness(), r.getLinkedRequest(), r.getHash(), r.getRemovedAt());
    }

    /** A download eligible for auto-removal: the fields needed to delete it and record what was removed. */
    private record Removable(
            String hash, @Nullable String name, double progress, double stuckness, @Nullable String linkedRequest) {}

    // --- detection ---

    private List<Download> findDownloads(
            Instant now, List<String> unreachable, Map<String, TorrentRequest> requestByTorrentHash) {
        Map<String, DelugeTorrent> torrents = tryFetch("Deluge", unreachable, delugeClient::getTorrentsStatus);
        if (torrents == null) {
            return List.of();
        }
        // When each torrent went seedless (lowercased hash → timestamp), kept up to date by SeedlessTorrentTracker;
        // the elapsed time drives the drought term of each download's stuckness.
        Map<String, Instant> seedlessSinceByHash = seedlessTorrentRepository.findAll().stream()
                .collect(Collectors.toMap(
                        s -> s.getHash().toLowerCase(Locale.ROOT), SeedlessTorrent::getSeedlessSince, (a, b) -> a));
        // Every unfinished torrent, most-stuck first so the worst offenders (and the auto-removal candidates) lead.
        // Iterate entries so each torrent's hash is available — both to link it to its request and to act on it from
        // the admin page's delete actions.
        return torrents.entrySet().stream()
                .filter(e -> isUnfinished(e.getValue()))
                .map(e -> {
                    DelugeTorrent t = e.getValue();
                    TorrentRequest request = requestByTorrentHash.get(e.getKey().toLowerCase(Locale.ROOT));
                    return new Download(
                            t.getName(),
                            Objects.requireNonNullElse(t.getProgress(), 0.0),
                            t.getTimeAdded() == null
                                    ? null
                                    : Instant.ofEpochSecond(t.getTimeAdded().longValue()),
                            request == null ? null : request.display(),
                            request == null ? null : request.link(),
                            e.getKey(),
                            stucknessOf(t, now, seedlessSinceByHash.get(e.getKey().toLowerCase(Locale.ROOT))));
                })
                .sorted(Comparator.comparingDouble(Download::stuckness)
                        .reversed()
                        .thenComparing(Download::name, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)))
                .toList();
    }

    private static List<ImportBlocked> findImportBlocked(
            @Nullable RadarrQueue radarrQueue,
            @Nullable SonarrQueue sonarrQueue,
            Map<Integer, String> movieTitleByRadarrId,
            Map<Integer, String> tvTitleBySonarrId) {
        List<ImportBlocked> blocked = new ArrayList<>();
        if (radarrQueue != null && radarrQueue.getRecords() != null) {
            for (RadarrQueueRecord record : radarrQueue.getRecords()) {
                if (Objects.equals(record.getTrackedDownloadState(), IMPORT_BLOCKED_STATE)) {
                    blocked.add(new ImportBlocked(
                            "Radarr",
                            label(movieTitleByRadarrId.get(record.getMovieId()), record.getDownloadId()),
                            null));
                }
            }
        }
        if (sonarrQueue != null && sonarrQueue.getRecords() != null) {
            for (SonarrQueueRecord record : sonarrQueue.getRecords()) {
                if (Objects.equals(record.getTrackedDownloadState(), IMPORT_BLOCKED_STATE)) {
                    blocked.add(new ImportBlocked(
                            "Sonarr",
                            label(tvTitleBySonarrId.get(record.getSeriesId()), record.getDownloadId()),
                            record.getSeasonNumber()));
                }
            }
        }
        return blocked;
    }

    private List<HealthIssue> findHealth(List<String> unreachable) {
        List<HealthIssue> health = new ArrayList<>();
        List<RadarrHealthItem> radarrHealth = tryFetch("Radarr", unreachable, radarrClient::getHealth);
        if (radarrHealth != null) {
            radarrHealth.forEach(h -> health.add(new HealthIssue("Radarr", h.getType(), h.getMessage())));
        }
        List<SonarrHealthItem> sonarrHealth = tryFetch("Sonarr", unreachable, sonarrClient::getHealth);
        if (sonarrHealth != null) {
            sonarrHealth.forEach(h -> health.add(new HealthIssue("Sonarr", h.getType(), h.getMessage())));
        }
        return health;
    }

    /**
     * Maps a Deluge torrent hash (lowercased) to the request that queued it, by matching the hash against each queue
     * record's {@code downloadId}. Only records whose request is known to us get an entry; usenet records (whose
     * downloadId is a SABnzbd nzo id) simply never match a torrent hash.
     */
    /** A torrent's owning request: a display label (for the email/grid) and the ids for its deep links. */
    private record TorrentRequest(String display, RequestLink link) {}

    private static Map<String, TorrentRequest> mapTorrentsToRequests(
            @Nullable RadarrQueue radarrQueue,
            @Nullable SonarrQueue sonarrQueue,
            Map<Integer, MovieRequest> movieByRadarrId,
            Map<Integer, TvRequest> tvBySeriesId) {
        Map<String, TorrentRequest> byHash = new HashMap<>();
        if (radarrQueue != null && radarrQueue.getRecords() != null) {
            for (RadarrQueueRecord record : radarrQueue.getRecords()) {
                MovieRequest movie = movieByRadarrId.get(record.getMovieId());
                if (record.getDownloadId() != null && movie != null) {
                    byHash.put(
                            record.getDownloadId().toLowerCase(Locale.ROOT),
                            new TorrentRequest("Movie: " + movie.getTitle(), linkOf(movie)));
                }
            }
        }
        if (sonarrQueue != null && sonarrQueue.getRecords() != null) {
            for (SonarrQueueRecord record : sonarrQueue.getRecords()) {
                TvRequest show = tvBySeriesId.get(record.getSeriesId());
                if (record.getDownloadId() != null && show != null) {
                    String season = record.getSeasonNumber() == null ? "" : " (S" + record.getSeasonNumber() + ")";
                    byHash.put(
                            record.getDownloadId().toLowerCase(Locale.ROOT),
                            new TorrentRequest("TV: " + show.getTitle() + season, linkOf(show)));
                }
            }
        }
        return byHash;
    }

    private static RequestLink linkOf(MovieRequest movie) {
        return new RequestLink(false, movie.getTmdbid(), null, null);
    }

    private static RequestLink linkOf(TvRequest show) {
        return new RequestLink(true, null, show.getOmbiExternalProviderId(), show.getSonarrTitleSlug());
    }

    private static <T extends Request> Map<Integer, T> byId(List<T> requests, Function<T, Integer> idFn) {
        return requests.stream()
                .filter(r -> idFn.apply(r) != null)
                .collect(Collectors.toMap(idFn, Function.identity(), (a, b) -> a));
    }

    /** Overdue = not stale (already triaged), not available, and requested before the threshold. */
    private static <T extends Request> List<T> overdue(List<T> all, Instant threshold) {
        return all.stream()
                .filter(NotificationService::actionableUnavailable)
                .filter(r -> r.getOmbiRequestedDate() != null
                        && r.getOmbiRequestedDate().isBefore(threshold))
                .sorted(Comparator.comparing(Request::getOmbiRequestedDate))
                .toList();
    }

    private static boolean recentlyRequested(Request r, Instant threshold) {
        return r.getOmbiRequestedDate() != null && r.getOmbiRequestedDate().isAfter(threshold);
    }

    // --- helpers ---

    /** Runs an external fetch, recording the integration as unreachable (once) and returning null if it throws. */
    private static <X> @Nullable X tryFetch(String integration, List<String> unreachable, Supplier<X> call) {
        try {
            return call.get();
        } catch (RuntimeException e) {
            log.warn("{} unreachable during notification check", integration, e);
            if (!unreachable.contains(integration)) {
                unreachable.add(integration);
            }
            return null;
        }
    }

    private static boolean actionableUnavailable(Request r) {
        return !Objects.equals(r.getStale(), true) && !r.isAvailable();
    }

    private static boolean beforeOrNull(@Nullable Instant value, Instant threshold) {
        return value == null || value.isBefore(threshold);
    }

    private static boolean isUnfinished(DelugeTorrent t) {
        return t.getProgress() != null && t.getProgress() < 100.0;
    }

    /** Stuckness (0..1) for a torrent, combining seeds, age, progress, and how long it has been seedless. */
    private static double stucknessOf(DelugeTorrent torrent, Instant now, @Nullable Instant seedlessSince) {
        Duration age = torrent.getTimeAdded() == null
                ? Duration.ZERO
                : Duration.between(Instant.ofEpochSecond(torrent.getTimeAdded().longValue()), now);
        int seeds = Objects.requireNonNullElse(torrent.getTotalSeeds(), 0);
        double progress = Objects.requireNonNullElse(torrent.getProgress(), 0.0);
        double daysWithoutSeeds = seedlessSince == null
                ? 0.0
                : Math.max(0.0, (double) Duration.between(seedlessSince, now).toSeconds() / 86_400.0);
        return Stuckness.score(seeds, age, progress, daysWithoutSeeds);
    }

    private static Comparator<Request> byTitle() {
        return Comparator.comparing(Request::getTitle, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER));
    }

    private static <T extends Request> Map<Integer, String> titleMap(List<T> requests, Function<T, Integer> idFn) {
        return requests.stream()
                .filter(r -> idFn.apply(r) != null)
                .collect(Collectors.toMap(idFn, Request::getTitle, (a, b) -> a));
    }

    /** Counts queue records by the id that ties them to a request (Radarr movie id / Sonarr series id). */
    private static <R> Map<Integer, Integer> queueCountsById(@Nullable List<R> records, Function<R, Integer> idFn) {
        Map<Integer, Integer> counts = new HashMap<>();
        if (records != null) {
            for (R record : records) {
                Integer id = idFn.apply(record);
                if (id != null) {
                    counts.merge(id, 1, Integer::sum);
                }
            }
        }
        return counts;
    }

    private static String label(@Nullable String title, @Nullable String fallback) {
        if (title != null && !title.isBlank()) {
            return title;
        }
        return fallback == null || fallback.isBlank() ? "(unknown)" : fallback;
    }

    private static String requesterName(Request r) {
        String user = r.getOmbiUserName();
        return user == null || user.isBlank() ? "unknown" : user;
    }

    private static int nz(@Nullable Integer value) {
        return value == null ? 0 : value;
    }

    // --- email rendering (line text must stay stable: it is asserted in tests) ---

    private static String downloadLine(Download d) {
        String base = d.name() + " (" + String.format("%.1f%%", d.progress()) + ", "
                + String.format("%.0f%%", d.stuckness() * 100) + " stuck)";
        return d.linkedRequest() == null ? base : base + " → " + d.linkedRequest();
    }

    private static String removedLine(RemovedDownloadRow d) {
        String base = d.name() + " (" + String.format("%.1f%%", d.progress()) + ", "
                + String.format("%.0f%%", d.stuckness() * 100) + " stuck)";
        return d.linkedRequest() == null ? base : base + " → " + d.linkedRequest();
    }

    private static String importBlockedLine(ImportBlocked b) {
        String season = b.season() == null ? "" : " (S" + b.season() + ")";
        return "[" + b.source() + "] " + b.title() + season;
    }

    private static String overdueMovieLine(OverdueMovieRow r) {
        return r.title()
                + " (requested " + DATE.format(r.requested())
                + ", by " + r.requester()
                + ", " + lastSearchedText(r.lastSearched())
                + ", " + queuedItemsText(r.queued())
                + ")";
    }

    private static String overdueTvLine(OverdueTvRow r) {
        return r.title()
                + " (requested " + DATE.format(r.requested())
                + ", by " + r.requester()
                + ", " + lastSearchedText(r.lastSearched())
                + ", " + queuedItemsText(r.queued())
                + ", " + r.downloadedEpisodes() + "/" + r.totalEpisodes() + " episodes downloaded)";
    }

    private static String unsearchedLine(UnsearchedRow r) {
        return r.title()
                + (r.lastSearched() == null
                        ? " (never searched)"
                        : " (last searched " + DATE.format(r.lastSearched()) + ")");
    }

    private static String newRequestLine(NewRequestRow r) {
        return "[" + r.type() + "] " + r.title();
    }

    private static String healthLine(HealthIssue h) {
        String typePart = h.type() == null || h.type().isBlank() ? "" : "[" + h.type() + "] ";
        return "[" + h.source() + "] " + typePart + (h.message() == null ? "" : h.message());
    }

    static String lastSearchedText(@Nullable Instant when) {
        return when == null ? "never searched" : "last searched " + DATE.format(when);
    }

    static String queuedItemsText(int count) {
        if (count == 0) {
            return "no queued items";
        }
        return count + (count == 1 ? " queued item" : " queued items");
    }

    private String header(Category category) {
        return switch (category) {
            case UNREACHABLE -> "Unreachable integrations";
            case HEALTH_ISSUE -> "Service health warnings";
            case IMPORT_BLOCKED -> "Import-blocked downloads (finished, need manual import)";
            case DOWNLOAD -> "Downloads in progress";
            case REMOVED_DOWNLOAD -> "Stuck downloads removed since the last run";
            case OVERDUE_MOVIE ->
                "Overdue movie requests (requested more than " + overdueRequestDays + " days ago, unavailable)";
            case OVERDUE_TV ->
                "Overdue TV requests (requested more than " + overdueRequestDays + " days ago, unavailable)";
            case UNSEARCHED_MOVIE -> "Movie requests not searched in " + unsearchedDays + "+ days (unavailable)";
            case UNSEARCHED_TV -> "TV requests not searched in " + unsearchedDays + "+ days (unavailable)";
            case NEW_REQUEST -> "New requests (last " + newRequestWindowHours + "h)";
        };
    }

    private String buildBody(NotificationSnapshot s) {
        EnumMap<Category, List<String>> lines = new EnumMap<>(Category.class);
        lines.put(Category.UNREACHABLE, s.unreachableIntegrations());
        lines.put(
                Category.HEALTH_ISSUE,
                s.healthIssues().stream().map(NotificationService::healthLine).toList());
        lines.put(
                Category.IMPORT_BLOCKED,
                s.importBlocked().stream()
                        .map(NotificationService::importBlockedLine)
                        .toList());
        lines.put(
                Category.DOWNLOAD,
                s.downloads().stream().map(NotificationService::downloadLine).toList());
        lines.put(
                Category.REMOVED_DOWNLOAD,
                s.removedDownloads().stream().map(NotificationService::removedLine).toList());
        lines.put(
                Category.OVERDUE_MOVIE,
                s.overdueMovies().stream()
                        .map(NotificationService::overdueMovieLine)
                        .toList());
        lines.put(
                Category.OVERDUE_TV,
                s.overdueTv().stream().map(NotificationService::overdueTvLine).toList());
        lines.put(
                Category.UNSEARCHED_MOVIE,
                s.unsearchedMovies().stream()
                        .map(NotificationService::unsearchedLine)
                        .toList());
        lines.put(
                Category.UNSEARCHED_TV,
                s.unsearchedTv().stream()
                        .map(NotificationService::unsearchedLine)
                        .toList());
        lines.put(
                Category.NEW_REQUEST,
                s.newRequests().stream()
                        .map(NotificationService::newRequestLine)
                        .toList());

        var sb = new StringBuilder("MediaManager weekly summary.\n");
        for (Category category : Category.values()) {
            List<String> categoryLines = lines.getOrDefault(category, List.of());
            if (categoryLines.isEmpty()) {
                continue;
            }
            sb.append('\n')
                    .append(header(category))
                    .append(": ")
                    .append(categoryLines.size())
                    .append('\n');
            for (String line : categoryLines) {
                sb.append("  - ").append(line).append('\n');
            }
        }
        return sb.toString();
    }

    private boolean mailConfigured() {
        return mailSenderProvider.getIfAvailable() != null && to != null && !to.isBlank();
    }

    private boolean send(int total, String body) {
        JavaMailSender sender = mailSenderProvider.getIfAvailable();
        if (sender == null || to == null || to.isBlank()) {
            log.warn(
                    "Notification summary has {} item(s) but mail is not configured (host/recipient); skipping send",
                    total);
            return false;
        }
        try {
            var message = new SimpleMailMessage();
            if (from != null && !from.isBlank()) {
                message.setFrom(from);
            }
            message.setTo(to.split("\\s*,\\s*"));
            message.setSubject("MediaManager summary: " + total + " item(s)");
            message.setText(body);
            sender.send(message);
            log.info("Sent notification summary email with {} item(s) to {}", total, to);
            return true;
        } catch (RuntimeException e) {
            logSendFailure(sender, e);
            return false;
        }
    }

    /**
     * Logs the connection details actually in use (never the password itself, only its length) so an auth failure can
     * be diagnosed: a wrong password length points at the config pipeline, a correct one points at the server.
     */
    private static void logSendFailure(JavaMailSender sender, Exception e) {
        if (sender instanceof JavaMailSenderImpl impl) {
            String password = impl.getPassword();
            log.warn(
                    "Failed to send notification email via {}:{} as user '{}' (password length={}, "
                            + "mail.smtp.auth={}, mail.smtp.starttls.enable={})",
                    impl.getHost(),
                    impl.getPort(),
                    impl.getUsername(),
                    password == null ? 0 : password.length(),
                    impl.getJavaMailProperties().getProperty("mail.smtp.auth"),
                    impl.getJavaMailProperties().getProperty("mail.smtp.starttls.enable"),
                    e);
        } else {
            log.warn("Failed to send notification email", e);
        }
    }

    // --- structured findings (shared by the email and the admin page) ---

    /** Identifiers needed to build Ombi/Radarr/Sonarr deep links for a request. */
    public record RequestLink(
            boolean tv,
            @Nullable Integer tmdbId,
            @Nullable Integer ombiExternalProviderId,
            @Nullable String sonarrTitleSlug) {}

    /** A currently-downloading (unfinished) torrent and its computed stuckness, shown in the single downloads list. */
    public record Download(
            @Nullable String name,
            double progress,
            @Nullable Instant added,
            @Nullable String linkedRequest,
            @Nullable RequestLink link,
            String hash,
            double stuckness) {}

    public record RemovedDownloadRow(
            @Nullable String name,
            double progress,
            double stuckness,
            @Nullable String linkedRequest,
            String hash,
            Instant removedAt) {}

    public record ImportBlocked(
            String source, String title, @Nullable Integer season) {}

    public record OverdueMovieRow(
            @Nullable String title,
            @Nullable Instant requested,
            String requester,
            @Nullable Instant lastSearched,
            int queued,
            RequestLink link) {}

    public record OverdueTvRow(
            @Nullable String title,
            @Nullable Instant requested,
            String requester,
            @Nullable Instant lastSearched,
            int queued,
            int downloadedEpisodes,
            int totalEpisodes,
            RequestLink link) {}

    public record UnsearchedRow(
            @Nullable String title,
            @Nullable Instant lastSearched,
            @Nullable Integer searchId) {}

    public record NewRequestRow(
            String type, @Nullable String title, @Nullable Instant requested) {}

    public record HealthIssue(
            String source, @Nullable String type, @Nullable String message) {}

    /** All findings from one detection run, in structured form. */
    // Internal data carrier; its collection components are never mutated after construction.
    @SuppressWarnings("ImmutableMemberCollection")
    public record NotificationSnapshot(
            List<Download> downloads,
            List<RemovedDownloadRow> removedDownloads,
            List<ImportBlocked> importBlocked,
            List<OverdueMovieRow> overdueMovies,
            List<OverdueTvRow> overdueTv,
            List<UnsearchedRow> unsearchedMovies,
            List<UnsearchedRow> unsearchedTv,
            List<NewRequestRow> newRequests,
            List<HealthIssue> healthIssues,
            List<String> unreachableIntegrations) {

        public Map<Category, Integer> counts() {
            EnumMap<Category, Integer> counts = new EnumMap<>(Category.class);
            counts.put(Category.UNREACHABLE, unreachableIntegrations.size());
            counts.put(Category.HEALTH_ISSUE, healthIssues.size());
            counts.put(Category.IMPORT_BLOCKED, importBlocked.size());
            counts.put(Category.DOWNLOAD, downloads.size());
            counts.put(Category.REMOVED_DOWNLOAD, removedDownloads.size());
            counts.put(Category.OVERDUE_MOVIE, overdueMovies.size());
            counts.put(Category.OVERDUE_TV, overdueTv.size());
            counts.put(Category.UNSEARCHED_MOVIE, unsearchedMovies.size());
            counts.put(Category.UNSEARCHED_TV, unsearchedTv.size());
            counts.put(Category.NEW_REQUEST, newRequests.size());
            return counts;
        }

        public int total() {
            return counts().values().stream().mapToInt(Integer::intValue).sum();
        }
    }

    /** Outcome of a {@link #runCheck()} run, for reporting back to the UI. */
    // Internal data carrier; its count map is never mutated after construction.
    @SuppressWarnings("ImmutableMemberCollection")
    public record NotificationResult(boolean emailSent, boolean mailConfigured, Map<Category, Integer> counts) {

        public int total() {
            return counts.values().stream().mapToInt(Integer::intValue).sum();
        }

        public int count(Category category) {
            return counts.getOrDefault(category, 0);
        }
    }
}
