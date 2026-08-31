package report.butt.mediamanager.service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Objects;
import org.jspecify.annotations.NullMarked;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Service;
import report.butt.mediamanager.client.RadarrClient;
import report.butt.mediamanager.client.SonarrClient;
import report.butt.mediamanager.model.MovieRequest;
import report.butt.mediamanager.model.TvEpisodeRequest;
import report.butt.mediamanager.repository.MovieRequestRepository;
import report.butt.mediamanager.repository.TvEpisodeRequestRepository;

/**
 * Asks Radarr/Sonarr to search again for things that are still missing. Runs as a step of the hourly refresh, so it
 * always works off freshly-refreshed rows, and records every search it requests through {@link SearchTrackingService}.
 *
 * <p>An item qualifies only when <em>both</em> the *arr's own last-search time and ours are older than
 * {@code search.retry-after-days}. The *arr value is the authoritative one — it moves for searches Radarr and Sonarr run
 * themselves, so honouring it stops us duplicating work they have just done — but on its own it is not enough: if a
 * search fails, or the *arr never records it, that timestamp never moves and the item would be re-searched every hour.
 * Our own stamp is what bounds the retry rate in that case.
 *
 * <p>Each run is capped at {@code search.max-per-run} of each type, longest-unsearched first, so a first run against a
 * large library (or the backlog after an outage) drains over several hours instead of firing hundreds of indexer queries
 * at once. Both types are issued as a single batched command per service.
 */
@Service
@NullMarked
public class AutoSearchService {

    private static final Logger log = LoggerFactory.getLogger(AutoSearchService.class);

    private final MovieRequestRepository movieRequestRepository;
    private final TvEpisodeRequestRepository tvEpisodeRequestRepository;
    private final RadarrClient radarrClient;
    private final SonarrClient sonarrClient;
    private final SearchTrackingService searchTrackingService;
    private final int retryAfterDays;
    private final int maxPerRun;

    // Spring constructor injection; the parameter count reflects injected collaborators, not a design smell.
    @SuppressWarnings("TooManyParameters")
    public AutoSearchService(
            MovieRequestRepository movieRequestRepository,
            TvEpisodeRequestRepository tvEpisodeRequestRepository,
            RadarrClient radarrClient,
            SonarrClient sonarrClient,
            SearchTrackingService searchTrackingService,
            @Value("${search.retry-after-days}") int retryAfterDays,
            @Value("${search.max-per-run}") int maxPerRun) {
        this.movieRequestRepository = movieRequestRepository;
        this.tvEpisodeRequestRepository = tvEpisodeRequestRepository;
        this.radarrClient = radarrClient;
        this.sonarrClient = sonarrClient;
        this.searchTrackingService = searchTrackingService;
        this.retryAfterDays = retryAfterDays;
        this.maxPerRun = maxPerRun;
    }

    /** Clears stale flags that no longer apply, then searches for the longest-unsearched missing movies and episodes. */
    public void run() {
        searchTrackingService.clearAutoStaleNowAvailable();
        Instant threshold = Instant.now().minus(retryAfterDays, ChronoUnit.DAYS);
        int movies = searchMovies(threshold);
        int episodes = searchEpisodes(threshold);
        log.info("Auto search requested {} movie search(es) and {} episode search(es)", movies, episodes);
    }

    /** Requests one batched Radarr search for the eligible movies; returns how many were searched for. */
    private int searchMovies(Instant threshold) {
        List<MovieRequest> movies = movieRequestRepository.findSearchable(threshold, Limit.of(maxPerRun));
        List<Integer> radarrIds = movies.stream()
                .map(MovieRequest::getRadarrRequestId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (radarrIds.isEmpty()) {
            return 0;
        }
        log.info("Auto search: requesting Radarr MoviesSearch for {} movie(s): {}", radarrIds.size(), radarrIds);
        radarrClient.searchMovies(radarrIds);
        searchTrackingService.recordMovieSearches(radarrIds);
        return radarrIds.size();
    }

    /**
     * Requests one batched Sonarr search for the eligible episodes; returns how many were searched for. Sonarr's
     * EpisodeSearch keys off its own episode ids, which the refresh now persists, so no lookup call is needed here.
     */
    private int searchEpisodes(Instant threshold) {
        // Only episodes Sonarr has an id for are searchable. The query already requires one, but filter here too so
        // what gets counted can never drift from what got searched.
        List<TvEpisodeRequest> searchable = tvEpisodeRequestRepository.findSearchable(threshold, Limit.of(maxPerRun))
                .stream()
                .filter(e -> e.getSonarrEpisodeId() != null)
                .toList();
        if (searchable.isEmpty()) {
            return 0;
        }
        List<Integer> sonarrEpisodeIds = searchable.stream()
                .map(TvEpisodeRequest::getSonarrEpisodeId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        log.info(
                "Auto search: requesting Sonarr EpisodeSearch for {} episode(s): {}",
                sonarrEpisodeIds.size(),
                sonarrEpisodeIds);
        sonarrClient.searchEpisodes(sonarrEpisodeIds);
        // Recorded by our own row ids, not Sonarr's, since the tracking columns live on TvEpisodeRequest.
        searchTrackingService.recordEpisodeSearches(
                searchable.stream().map(TvEpisodeRequest::getId).toList());
        return sonarrEpisodeIds.size();
    }
}
