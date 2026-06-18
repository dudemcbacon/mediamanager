package report.butt.mediamanager.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import report.butt.mediamanager.client.DelugeClient;
import report.butt.mediamanager.client.RadarrClient;
import report.butt.mediamanager.client.SonarrClient;
import report.butt.mediamanager.model.MovieRequest;
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
import report.butt.mediamanager.service.NotificationService.Category;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class NotificationServiceTest {

    private static final String AVAILABLE_STATUS = "Common.Available";

    @Mock
    private DelugeClient delugeClient;

    @Mock
    private RadarrClient radarrClient;

    @Mock
    private SonarrClient sonarrClient;

    @Mock
    private MovieRequestRepository movieRequestRepository;

    @Mock
    private TvRequestRepository tvRequestRepository;

    @Mock
    private ObjectProvider<JavaMailSender> mailSenderProvider;

    @Mock
    private JavaMailSender mailSender;

    private NotificationService service;

    @BeforeEach
    void setUp() {
        when(delugeClient.getTorrentsStatus()).thenReturn(Map.of());
        when(radarrClient.getQueue()).thenReturn(new RadarrQueue());
        when(radarrClient.getHealth()).thenReturn(List.of());
        when(sonarrClient.getQueue()).thenReturn(new SonarrQueue());
        when(sonarrClient.getHealth()).thenReturn(List.of());
        when(movieRequestRepository.findAll()).thenReturn(List.of());
        when(tvRequestRepository.findAll()).thenReturn(List.of());
        when(mailSenderProvider.getIfAvailable()).thenReturn(mailSender);
        service = new NotificationService(
                delugeClient,
                radarrClient,
                sonarrClient,
                movieRequestRepository,
                tvRequestRepository,
                mailSenderProvider,
                "from@test",
                "to@test",
                14,
                90,
                30,
                24);
    }

    @Test
    void sendsNothingWhenNothingToReport() {
        NotificationService.NotificationResult result = service.runCheck();

        assertEquals(0, result.total());
        assertFalse(result.emailSent());
        verify(mailSender, never()).send(any(SimpleMailMessage.class));
    }

    @Test
    void flagsTorrentStuckBeyondThreshold() {
        DelugeTorrent stuck = torrent("stuck", 50.0, daysAgoEpoch(20), 5);
        DelugeTorrent recent = torrent("recent", 10.0, daysAgoEpoch(3), 5);
        DelugeTorrent finishedButOld = torrent("done", 100.0, daysAgoEpoch(20), 5);
        when(delugeClient.getTorrentsStatus()).thenReturn(Map.of("a", stuck, "b", recent, "c", finishedButOld));

        NotificationService.NotificationResult result = service.runCheck();

        assertEquals(1, result.count(Category.STUCK_DOWNLOAD));
        assertEquals(0, result.count(Category.ZERO_SEED_DOWNLOAD));
        assertEquals(1, result.total());
        assertTrue(result.emailSent());
        verify(mailSender, times(1)).send(any(SimpleMailMessage.class));
    }

    @Test
    void flagsZeroSeedTorrentButNotAsStuckToo() {
        DelugeTorrent zeroSeedRecent = torrent("seedless", 40.0, daysAgoEpoch(2), 0);
        DelugeTorrent zeroSeedOld = torrent("seedless-old", 30.0, daysAgoEpoch(20), 0);
        DelugeTorrent healthy = torrent("healthy", 60.0, daysAgoEpoch(2), 8);
        when(delugeClient.getTorrentsStatus()).thenReturn(Map.of("a", zeroSeedRecent, "b", zeroSeedOld, "c", healthy));

        NotificationService.NotificationResult result = service.runCheck();

        // zeroSeedOld is past the stuck threshold, so it counts as stuck (not double-listed as zero-seed).
        assertEquals(1, result.count(Category.STUCK_DOWNLOAD));
        assertEquals(1, result.count(Category.ZERO_SEED_DOWNLOAD));
        assertEquals(2, result.total());
    }

    @Test
    void linksZeroSeedTorrentToItsMovieRequest() {
        DelugeTorrent seedless = torrent("Some.Movie.2024.1080p", 40.0, daysAgoEpoch(2), 0);
        when(delugeClient.getTorrentsStatus()).thenReturn(Map.of("ABC123HASH", seedless));
        RadarrQueueRecord queued = record(10, "downloading");
        queued.setDownloadId("abc123hash"); // queue downloadId == torrent hash (matched case-insensitively)
        when(radarrClient.getQueue()).thenReturn(queue(queued));
        MovieRequest movie = movieWithRadarrId("The Real Movie", 10);
        movie.setRadarrLastSearchTime(daysAgo(1)); // searched recently, so it's not also flagged as unsearched
        when(movieRequestRepository.findAll()).thenReturn(List.of(movie));

        NotificationService.NotificationResult result = service.runCheck();

        assertEquals(1, result.count(Category.ZERO_SEED_DOWNLOAD));
        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());
        String body = captor.getValue().getText();
        assertTrue(body.contains("Some.Movie.2024.1080p"), body);
        assertTrue(body.contains("Movie: The Real Movie"), body);
    }

    @Test
    void flagsImportBlockedAcrossRadarrAndSonarr() {
        when(radarrClient.getQueue()).thenReturn(queue(record(10, "importBlocked"), record(11, "downloading")));
        when(sonarrClient.getQueue()).thenReturn(sonarrQueue(sonarrRecord(20, 2, "importBlocked")));
        when(movieRequestRepository.findAll()).thenReturn(List.of(movieWithRadarrId("Blocked Movie", 10)));
        when(tvRequestRepository.findAll()).thenReturn(List.of(tvWithSonarrId("Blocked Show", 20)));

        NotificationService.NotificationResult result = service.runCheck();

        assertEquals(2, result.count(Category.IMPORT_BLOCKED));
    }

    @Test
    void flagsHealthIssuesFromBothServices() {
        when(radarrClient.getHealth()).thenReturn(List.of(radarrHealth("warning", "Disk space low")));
        when(sonarrClient.getHealth()).thenReturn(List.of(sonarrHealth("error", "Indexer unavailable")));

        NotificationService.NotificationResult result = service.runCheck();

        assertEquals(2, result.count(Category.HEALTH_ISSUE));
    }

    @Test
    void reportsUnreachableIntegrationAndSkipsItsChecks() {
        when(delugeClient.getTorrentsStatus()).thenThrow(new RuntimeException("connection refused"));

        NotificationService.NotificationResult result = service.runCheck();

        assertEquals(1, result.count(Category.UNREACHABLE));
        assertEquals(0, result.count(Category.STUCK_DOWNLOAD));
        assertEquals(1, result.total());
    }

    @Test
    void flagsOnlyOverdueUnavailableNonStaleRequests() {
        MovieRequest overdue = movie("Overdue", daysAgo(100), false, false);
        MovieRequest available = movie("Available", daysAgo(100), true, false);
        MovieRequest stale = movie("Stale", daysAgo(100), false, true);
        MovieRequest recent = movie("Recent", daysAgo(10), false, false);
        when(movieRequestRepository.findAll()).thenReturn(List.of(overdue, available, stale, recent));

        NotificationService.NotificationResult result = service.runCheck();

        assertEquals(1, result.count(Category.OVERDUE_MOVIE));
        assertEquals(0, result.count(Category.OVERDUE_TV));
    }

    @Test
    void overdueLinesIncludeRequesterSearchQueueAndEpisodeDetail() {
        MovieRequest movie = movie("Overdue Movie", daysAgo(120), false, false);
        movie.setOmbiUserName("alice");
        movie.setRadarrRequestId(10); // never searched (no last-search time set)
        when(movieRequestRepository.findAll()).thenReturn(List.of(movie));
        when(radarrClient.getQueue()).thenReturn(queue(record(10, "downloading"))); // one queued item

        TvRequest show = tvOverdue("Overdue Show", daysAgo(120));
        show.setOmbiUserName("bob");
        show.setSonarrSeriesId(20);
        show.setSonarrLastSearched(daysAgo(3)); // searched recently → not also flagged unsearched
        show.setSonarrEpisodeFileCount(4);
        show.setSonarrTotalEpisodeCount(10);
        when(tvRequestRepository.findAll()).thenReturn(List.of(show));

        NotificationService.NotificationResult result = service.runCheck();

        assertEquals(1, result.count(Category.OVERDUE_MOVIE));
        assertEquals(1, result.count(Category.OVERDUE_TV));
        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());
        String body = captor.getValue().getText();
        assertTrue(body.contains("by alice"), body);
        assertTrue(body.contains("never searched"), body);
        assertTrue(body.contains("1 queued item"), body);
        assertTrue(body.contains("by bob"), body);
        assertTrue(body.contains("no queued items"), body);
        assertTrue(body.contains("4/10 episodes downloaded"), body);
    }

    @Test
    void flagsUnsearchedActionableRequests() {
        // Requested recently (not overdue) but never searched, and present in Radarr → unsearched only.
        MovieRequest neverSearched = movie("Never searched", daysAgo(10), false, false);
        neverSearched.setRadarrRequestId(10);
        MovieRequest searchedRecently = movie("Searched", daysAgo(10), false, false);
        searchedRecently.setRadarrRequestId(11);
        searchedRecently.setRadarrLastSearchTime(daysAgo(1));
        when(movieRequestRepository.findAll()).thenReturn(List.of(neverSearched, searchedRecently));

        NotificationService.NotificationResult result = service.runCheck();

        assertEquals(1, result.count(Category.UNSEARCHED_MOVIE));
        assertEquals(0, result.count(Category.OVERDUE_MOVIE));
    }

    @Test
    void includesRecentlyRequestedItemsInNewRequestDigest() {
        MovieRequest fresh = movie("Fresh", hoursAgo(2), false, false);
        MovieRequest old = movie("Old", daysAgo(5), false, false);
        when(movieRequestRepository.findAll()).thenReturn(List.of(fresh, old));

        NotificationService.NotificationResult result = service.runCheck();

        assertEquals(1, result.count(Category.NEW_REQUEST));
    }

    @Test
    void snapshotReturnsStructuredRowsWithoutSendingEmail() {
        // Zero-seed torrent linked to a movie request.
        DelugeTorrent seedless = torrent("Some.Movie.2024", 40.0, daysAgoEpoch(2), 0);
        when(delugeClient.getTorrentsStatus()).thenReturn(Map.of("ABCHASH", seedless));
        RadarrQueueRecord queued = record(10, "downloading");
        queued.setDownloadId("abchash");
        when(radarrClient.getQueue()).thenReturn(queue(queued));

        MovieRequest overdueMovie = movie("Overdue Movie", daysAgo(120), false, false);
        overdueMovie.setOmbiUserName("alice");
        overdueMovie.setRadarrRequestId(10);
        overdueMovie.setRadarrLastSearchTime(daysAgo(1)); // not also unsearched
        when(movieRequestRepository.findAll()).thenReturn(List.of(overdueMovie));

        TvRequest overdueShow = tvOverdue("Overdue Show", daysAgo(120));
        overdueShow.setOmbiUserName("bob");
        overdueShow.setSonarrSeriesId(20);
        overdueShow.setSonarrLastSearched(daysAgo(2));
        overdueShow.setSonarrEpisodeFileCount(4);
        overdueShow.setSonarrTotalEpisodeCount(10);
        when(tvRequestRepository.findAll()).thenReturn(List.of(overdueShow));

        NotificationService.NotificationSnapshot snapshot = service.snapshot();

        // snapshot() never sends mail.
        verify(mailSender, never()).send(any(SimpleMailMessage.class));

        assertEquals(1, snapshot.zeroSeedDownloads().size());
        assertEquals("Movie: Overdue Movie", snapshot.zeroSeedDownloads().get(0).linkedRequest());

        assertEquals(1, snapshot.overdueMovies().size());
        NotificationService.OverdueMovieRow movieRow = snapshot.overdueMovies().get(0);
        assertEquals("alice", movieRow.requester());

        assertEquals(1, snapshot.overdueTv().size());
        NotificationService.OverdueTvRow tvRow = snapshot.overdueTv().get(0);
        assertEquals("bob", tvRow.requester());
        assertEquals(4, tvRow.downloadedEpisodes());
        assertEquals(10, tvRow.totalEpisodes());
    }

    @Test
    void doesNotSendWhenMailNotConfigured() {
        when(mailSenderProvider.getIfAvailable()).thenReturn(null);
        when(movieRequestRepository.findAll()).thenReturn(List.of(movie("Overdue", daysAgo(100), false, false)));

        NotificationService.NotificationResult result = service.runCheck();

        assertEquals(1, result.total());
        assertFalse(result.emailSent());
        assertFalse(result.mailConfigured());
    }

    // --- builders ---

    private static DelugeTorrent torrent(String name, double progress, double timeAdded, int totalSeeds) {
        var t = new DelugeTorrent();
        t.setName(name);
        t.setProgress(progress);
        t.setTimeAdded(timeAdded);
        t.setTotalSeeds(totalSeeds);
        return t;
    }

    private static RadarrQueue queue(RadarrQueueRecord... records) {
        var q = new RadarrQueue();
        q.setRecords(List.of(records));
        return q;
    }

    private static RadarrQueueRecord record(int movieId, String state) {
        var r = new RadarrQueueRecord();
        r.setMovieId(movieId);
        r.setTrackedDownloadState(state);
        return r;
    }

    private static SonarrQueue sonarrQueue(SonarrQueueRecord... records) {
        var q = new SonarrQueue();
        q.setRecords(List.of(records));
        return q;
    }

    private static SonarrQueueRecord sonarrRecord(int seriesId, int season, String state) {
        var r = new SonarrQueueRecord();
        r.setSeriesId(seriesId);
        r.setSeasonNumber(season);
        r.setTrackedDownloadState(state);
        return r;
    }

    private static RadarrHealthItem radarrHealth(String type, String message) {
        var item = new RadarrHealthItem();
        item.setType(type);
        item.setMessage(message);
        return item;
    }

    private static SonarrHealthItem sonarrHealth(String type, String message) {
        var item = new SonarrHealthItem();
        item.setType(type);
        item.setMessage(message);
        return item;
    }

    private static MovieRequest movieWithRadarrId(String title, int radarrId) {
        MovieRequest m = movie(title, daysAgo(1), false, false);
        m.setRadarrRequestId(radarrId);
        return m;
    }

    private static TvRequest tvWithSonarrId(String title, int sonarrId) {
        var t = new TvRequest(title, 1, false, 1, "Common.ProcessingRequest");
        t.setSonarrSeriesId(sonarrId);
        return t;
    }

    private static TvRequest tvOverdue(String title, Instant requestedDate) {
        var t = new TvRequest(title, 1, false, 1, "Common.ProcessingRequest");
        t.setOmbiRequestedDate(requestedDate);
        t.setStale(false);
        return t;
    }

    private static MovieRequest movie(String title, Instant requestedDate, boolean available, boolean stale) {
        // isAvailable() requires radarrHasFile==true AND ombiRequestStatus==Common.Available.
        var m = new MovieRequest(title, 1, available, 1, available ? AVAILABLE_STATUS : "Common.ProcessingRequest");
        m.setRadarrHasFile(available);
        m.setOmbiRequestedDate(requestedDate);
        m.setStale(stale);
        return m;
    }

    private static Instant daysAgo(int days) {
        return Instant.now().minus(days, ChronoUnit.DAYS);
    }

    private static Instant hoursAgo(int hours) {
        return Instant.now().minus(hours, ChronoUnit.HOURS);
    }

    private static double daysAgoEpoch(int days) {
        return daysAgo(days).getEpochSecond();
    }
}
