package report.butt.mediamanager.service;

import com.newrelic.api.agent.Trace;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
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
import report.butt.mediamanager.model.Request;
import report.butt.mediamanager.model.TvRequest;
import report.butt.mediamanager.model.deluge.DelugeTorrent;
import report.butt.mediamanager.model.radarr.RadarrHealthItem;
import report.butt.mediamanager.model.radarr.RadarrQueue;
import report.butt.mediamanager.model.radarr.RadarrQueueRecord;
import report.butt.mediamanager.model.sonarr.SonarrHealthItem;
import report.butt.mediamanager.model.sonarr.SonarrQueue;
import report.butt.mediamanager.model.sonarr.SonarrQueueRecord;
import report.butt.mediamanager.repository.MovieRequestRepository;
import report.butt.mediamanager.repository.TvRequestRepository;

/**
 * Detects conditions worth an alert and emails a single daily summary. Detection is done once by {@link #snapshot()},
 * which returns structured per-category rows; {@link #runCheck()} renders the email from that snapshot and the admin
 * page renders grids from it, so the two share one source of truth. A failure reaching one integration is reported (as
 * an "unreachable" finding) and its dependent checks are skipped rather than aborting the whole run. Run daily by
 * {@link ScheduledRefreshJob} and on demand from the movie/TV views ("Test Notifications").
 */
@Service
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
        STUCK_DOWNLOAD,
        ZERO_SEED_DOWNLOAD,
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
    private final ObjectProvider<JavaMailSender> mailSenderProvider;
    private final String from;
    private final String to;
    private final int stuckDownloadDays;
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
            ObjectProvider<JavaMailSender> mailSenderProvider,
            @Value("${notifications.from}") String from,
            @Value("${notifications.to}") String to,
            @Value("${notifications.stuck-download-days}") int stuckDownloadDays,
            @Value("${notifications.overdue-request-days}") int overdueRequestDays,
            @Value("${notifications.unsearched-days}") int unsearchedDays,
            @Value("${notifications.new-request-window-hours}") int newRequestWindowHours) {
        this.delugeClient = delugeClient;
        this.radarrClient = radarrClient;
        this.sonarrClient = sonarrClient;
        this.movieRequestRepository = movieRequestRepository;
        this.tvRequestRepository = tvRequestRepository;
        this.mailSenderProvider = mailSenderProvider;
        this.from = from;
        this.to = to;
        this.stuckDownloadDays = stuckDownloadDays;
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
        NotificationSnapshot snapshot = snapshot();
        Map<Category, Integer> counts = snapshot.counts();
        int total = snapshot.total();
        if (total == 0) {
            log.info("Notification check found nothing to report");
            return new NotificationResult(false, mailConfigured(), counts);
        }
        boolean emailSent = send(total, buildBody(snapshot));
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

        DownloadFindings downloads = findDownloads(now, unreachable, requestByTorrentHash);
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
                downloads.stuck(),
                downloads.zeroSeed(),
                importBlocked,
                overdueMovies,
                overdueTv,
                unsearchedMovies,
                unsearchedTv,
                newRequests,
                health,
                unreachable);
    }

    // --- detection ---

    // Internal data carrier; its collection components are never mutated after construction.
    @SuppressWarnings("ImmutableMemberCollection")
    private record DownloadFindings(List<StuckDownload> stuck, List<ZeroSeedDownload> zeroSeed) {}

    private DownloadFindings findDownloads(
            Instant now, List<String> unreachable, Map<String, TorrentRequest> requestByTorrentHash) {
        Map<String, DelugeTorrent> torrents = tryFetch("Deluge", unreachable, delugeClient::getTorrentsStatus);
        if (torrents == null) {
            return new DownloadFindings(List.of(), List.of());
        }
        Instant stuckThreshold = now.minus(stuckDownloadDays, ChronoUnit.DAYS);
        // Iterate entries so each torrent's hash is available — both to link it to its request and to act on it from
        // the admin page's delete actions.
        List<StuckDownload> stuck = torrents.entrySet().stream()
                .filter(e -> isUnfinished(e.getValue()))
                .filter(e -> e.getValue().getTimeAdded() != null
                        && Instant.ofEpochSecond(e.getValue().getTimeAdded().longValue())
                                .isBefore(stuckThreshold))
                .sorted(Comparator.comparing(e -> e.getValue().getTimeAdded()))
                .map(e -> {
                    TorrentRequest request = requestByTorrentHash.get(e.getKey().toLowerCase(Locale.ROOT));
                    return new StuckDownload(
                            e.getValue().getName(),
                            e.getValue().getProgress(),
                            Instant.ofEpochSecond(e.getValue().getTimeAdded().longValue()),
                            request == null ? null : request.display(),
                            request == null ? null : request.link(),
                            e.getKey());
                })
                .toList();
        Set<String> stuckNames = stuck.stream().map(StuckDownload::name).collect(Collectors.toSet());
        // Zero-seed torrents will never finish — surface them even before the stuck timer fires, but don't double-list
        // ones already reported as stuck.
        List<ZeroSeedDownload> zeroSeed = torrents.entrySet().stream()
                .filter(e -> isUnfinished(e.getValue()))
                .filter(e ->
                        e.getValue().getTotalSeeds() != null && e.getValue().getTotalSeeds() == 0)
                .filter(e -> !stuckNames.contains(e.getValue().getName()))
                .sorted(Comparator.comparing(
                        e -> e.getValue().getName(), Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)))
                .map(e -> {
                    TorrentRequest request = requestByTorrentHash.get(e.getKey().toLowerCase(Locale.ROOT));
                    return new ZeroSeedDownload(
                            e.getValue().getName(),
                            e.getValue().getProgress(),
                            e.getValue().getTimeAdded() == null
                                    ? null
                                    : Instant.ofEpochSecond(
                                            e.getValue().getTimeAdded().longValue()),
                            request == null ? null : request.display(),
                            request == null ? null : request.link(),
                            e.getKey());
                })
                .toList();
        return new DownloadFindings(stuck, zeroSeed);
    }

    private static List<ImportBlocked> findImportBlocked(
            RadarrQueue radarrQueue,
            SonarrQueue sonarrQueue,
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
            RadarrQueue radarrQueue,
            SonarrQueue sonarrQueue,
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
    private static <X> X tryFetch(String integration, List<String> unreachable, Supplier<X> call) {
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

    private static boolean beforeOrNull(Instant value, Instant threshold) {
        return value == null || value.isBefore(threshold);
    }

    private static boolean isUnfinished(DelugeTorrent t) {
        return t.getProgress() != null && t.getProgress() < 100.0;
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

    private static String label(String title, String fallback) {
        if (title != null && !title.isBlank()) {
            return title;
        }
        return fallback == null || fallback.isBlank() ? "(unknown)" : fallback;
    }

    private static String requesterName(Request r) {
        String user = r.getOmbiUserName();
        return user == null || user.isBlank() ? "unknown" : user;
    }

    private static int nz(Integer value) {
        return value == null ? 0 : value;
    }

    // --- email rendering (line text must stay stable: it is asserted in tests) ---

    private static String stuckLine(StuckDownload d) {
        return d.name() + " (" + String.format("%.1f%%", d.progress()) + ", added " + DATE.format(d.added()) + ")";
    }

    private static String zeroSeedLine(ZeroSeedDownload d) {
        String base = d.name() + " (" + String.format("%.1f%%", d.progress()) + ", no seeds)";
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

    static String lastSearchedText(Instant when) {
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
            case STUCK_DOWNLOAD -> "Stuck downloads (added more than " + stuckDownloadDays + " days ago, not finished)";
            case ZERO_SEED_DOWNLOAD -> "Torrents with no seeds (will not finish)";
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
                Category.STUCK_DOWNLOAD,
                s.stuckDownloads().stream().map(NotificationService::stuckLine).toList());
        lines.put(
                Category.ZERO_SEED_DOWNLOAD,
                s.zeroSeedDownloads().stream()
                        .map(NotificationService::zeroSeedLine)
                        .toList());
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

        var sb = new StringBuilder("MediaManager daily summary.\n");
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

    public record StuckDownload(
            String name,
            double progress,
            Instant added,
            @Nullable String linkedRequest,
            @Nullable RequestLink link,
            String hash) {}

    public record ZeroSeedDownload(
            String name,
            double progress,
            @Nullable Instant added,
            @Nullable String linkedRequest,
            @Nullable RequestLink link,
            String hash) {}

    public record ImportBlocked(
            String source, String title, @Nullable Integer season) {}

    public record OverdueMovieRow(
            String title, Instant requested, String requester, Instant lastSearched, int queued, RequestLink link) {}

    public record OverdueTvRow(
            String title,
            Instant requested,
            String requester,
            Instant lastSearched,
            int queued,
            int downloadedEpisodes,
            int totalEpisodes,
            RequestLink link) {}

    public record UnsearchedRow(String title, Instant lastSearched, Integer searchId) {}

    public record NewRequestRow(String type, String title, Instant requested) {}

    public record HealthIssue(String source, String type, String message) {}

    /** All findings from one detection run, in structured form. */
    // Internal data carrier; its collection components are never mutated after construction.
    @SuppressWarnings("ImmutableMemberCollection")
    public record NotificationSnapshot(
            List<StuckDownload> stuckDownloads,
            List<ZeroSeedDownload> zeroSeedDownloads,
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
            counts.put(Category.STUCK_DOWNLOAD, stuckDownloads.size());
            counts.put(Category.ZERO_SEED_DOWNLOAD, zeroSeedDownloads.size());
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
