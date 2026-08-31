package report.butt.mediamanager.service;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import report.butt.mediamanager.model.MovieRequest;
import report.butt.mediamanager.model.TvEpisodeRequest;
import report.butt.mediamanager.model.TvRequest;
import report.butt.mediamanager.model.TvSeasonRequest;
import report.butt.mediamanager.repository.MovieRequestRepository;
import report.butt.mediamanager.repository.TvEpisodeRequestRepository;
import report.butt.mediamanager.repository.TvRequestRepository;

/**
 * Records the searches this app requests, so "how long have we been searching for this without success" is answerable.
 * Every search path calls in here after asking Radarr/Sonarr, which keeps one definition of a search count and one
 * place that decides when fruitless searching has gone on long enough to mark a request stale.
 *
 * <p>Tracking is per movie and per episode, the two things actually searched for. Series- and season-level searches are
 * attributed to the unavailable episodes underneath them, since that is what such a search really covers. Available
 * items are left out of that fan-out: a series search does re-scan them, but they need nothing further and counting
 * them would inflate their history.
 *
 * <p>Episodes carry no stale flag of their own — {@code TvEpisodeRequest} sits outside the {@code Request} hierarchy —
 * so an episode whose searching has gone on too long escalates to its parent {@link TvRequest}, naming the episode.
 */
@Service
@NullMarked
public class SearchTrackingService {

    private static final Logger log = LoggerFactory.getLogger(SearchTrackingService.class);

    /**
     * Marks a stale reason as written by this service rather than by a person. Only stale flags carrying it are ever
     * cleared automatically — a human's triage decision and their free-text reason must survive untouched.
     */
    public static final String AUTO_REASON_PREFIX = "Auto: ";

    private final MovieRequestRepository movieRequestRepository;
    private final TvRequestRepository tvRequestRepository;
    private final TvEpisodeRequestRepository tvEpisodeRequestRepository;
    private final int staleAfterDays;

    public SearchTrackingService(
            MovieRequestRepository movieRequestRepository,
            TvRequestRepository tvRequestRepository,
            TvEpisodeRequestRepository tvEpisodeRequestRepository,
            @Value("${search.stale-after-days}") int staleAfterDays) {
        this.movieRequestRepository = movieRequestRepository;
        this.tvRequestRepository = tvRequestRepository;
        this.tvEpisodeRequestRepository = tvEpisodeRequestRepository;
        this.staleAfterDays = staleAfterDays;
    }

    /**
     * Records a search of the given Radarr movie ids: bumps each movie's count, stamps the first and latest search, and
     * marks the movie stale once the span between them exceeds {@code search.stale-after-days}. Also applies the
     * {@code radarrLastSearchTime} stamp the callers used to set by hand, so a search shows immediately rather than only
     * once the next refresh reads it back from Radarr.
     */
    @Transactional
    public void recordMovieSearches(Collection<Integer> radarrIds) {
        Set<Integer> ids = radarrIds.stream().filter(Objects::nonNull).collect(Collectors.toSet());
        if (ids.isEmpty()) {
            return;
        }
        List<MovieRequest> movies = movieRequestRepository.findByRadarrRequestIdIn(ids);
        if (movies.isEmpty()) {
            return;
        }
        var now = Instant.now();
        for (MovieRequest movie : movies) {
            movie.setRadarrLastSearchTime(now);
            movie.setSearchCount(nextCount(movie.getSearchCount()));
            if (movie.getSearchFirstAt() == null) {
                movie.setSearchFirstAt(now);
            }
            movie.setSearchLastAt(now);

            if (spanExceedsLimit(movie.getSearchFirstAt(), now) && !Objects.equals(movie.getStale(), true)) {
                movie.setStale(true);
                movie.setStaleReason(reason(movie.getSearchCount(), movie.getSearchFirstAt(), now, null));
                movie.setMarkedStaleAt(now);
                log.info(
                        "Marked movie '{}' stale after {} fruitless search(es)",
                        movie.getTitle(),
                        movie.getSearchCount());
            }
        }
        movieRequestRepository.saveAll(movies);
    }

    /**
     * Records a search of the given episode request ids. When an episode's search span exceeds the limit its parent
     * series is marked stale, episodes having no stale flag of their own.
     */
    @Transactional
    public void recordEpisodeSearches(Collection<Long> episodeRequestIds) {
        Set<Long> ids = episodeRequestIds.stream().filter(Objects::nonNull).collect(Collectors.toSet());
        if (ids.isEmpty()) {
            return;
        }
        recordEpisodes(tvEpisodeRequestRepository.findAllById(ids));
    }

    /**
     * Records a series-level search, attributed to every unavailable episode of those series. Callers keep their own
     * {@code TvRequest.sonarrLastSearched} stamping; this adds the per-episode history.
     */
    @Transactional
    public void recordSeriesSearches(Collection<Integer> sonarrSeriesIds) {
        Set<Integer> ids = sonarrSeriesIds.stream().filter(Objects::nonNull).collect(Collectors.toSet());
        if (ids.isEmpty()) {
            return;
        }
        recordEpisodes(tvEpisodeRequestRepository.findUnavailableBySonarrSeriesIdIn(ids));
    }

    /** Records a season-level search, attributed to every unavailable episode of those seasons of one series. */
    @Transactional
    public void recordSeasonSearches(@Nullable Integer sonarrSeriesId, Collection<Integer> seasonNumbers) {
        Set<Integer> seasons = seasonNumbers.stream().filter(Objects::nonNull).collect(Collectors.toSet());
        if (sonarrSeriesId == null || seasons.isEmpty()) {
            return;
        }
        recordEpisodes(
                tvEpisodeRequestRepository.findUnavailableBySonarrSeriesIdAndSeasonNumbers(sonarrSeriesId, seasons));
    }

    /**
     * Clears the stale flag on requests this service marked that have since become available, so the flag means "still
     * missing after prolonged searching" rather than a permanent scar. Only reasons carrying
     * {@link #AUTO_REASON_PREFIX} are touched. Returns how many were cleared.
     */
    @Transactional
    public int clearAutoStaleNowAvailable() {
        List<MovieRequest> movies = movieRequestRepository.findAutoStaleNowAvailable(AUTO_REASON_PREFIX);
        List<TvRequest> shows = tvRequestRepository.findAutoStaleNowAvailable(AUTO_REASON_PREFIX);
        for (MovieRequest movie : movies) {
            movie.setStale(false);
            movie.setStaleReason(null);
            movie.setMarkedStaleAt(null);
        }
        for (TvRequest show : shows) {
            show.setStale(false);
            show.setStaleReason(null);
            show.setMarkedStaleAt(null);
        }
        if (!movies.isEmpty()) {
            movieRequestRepository.saveAll(movies);
        }
        if (!shows.isEmpty()) {
            tvRequestRepository.saveAll(shows);
        }
        int cleared = movies.size() + shows.size();
        if (cleared > 0) {
            log.info("Cleared auto-set stale on {} now-available request(s)", cleared);
        }
        return cleared;
    }

    /** Applies the search stamps to a batch of episodes, then escalates any that have gone stale to their series. */
    private void recordEpisodes(List<TvEpisodeRequest> episodes) {
        if (episodes.isEmpty()) {
            return;
        }
        var now = Instant.now();
        List<TvEpisodeRequest> newlyStale = new ArrayList<>();
        for (TvEpisodeRequest episode : episodes) {
            episode.setSonarrLastSearchTime(now);
            episode.setSearchCount(nextCount(episode.getSearchCount()));
            if (episode.getSearchFirstAt() == null) {
                episode.setSearchFirstAt(now);
            }
            episode.setSearchLastAt(now);

            if (spanExceedsLimit(episode.getSearchFirstAt(), now)) {
                newlyStale.add(episode);
            }
        }
        tvEpisodeRequestRepository.saveAll(episodes);
        escalateToSeries(newlyStale, now);
    }

    /**
     * Marks the parent series of each stale-eligible episode stale, naming the episode that triggered it. A series that
     * is already stale is left alone, so an existing reason (possibly a person's) is preserved.
     */
    private void escalateToSeries(List<TvEpisodeRequest> staleEpisodes, Instant now) {
        if (staleEpisodes.isEmpty()) {
            return;
        }
        Map<Long, Long> seriesIdByEpisodeId = new HashMap<>();
        List<Long> episodeIds =
                staleEpisodes.stream().map(TvEpisodeRequest::getId).filter(Objects::nonNull).toList();
        for (Object[] row : tvEpisodeRequestRepository.findParentTvRequestIds(episodeIds)) {
            seriesIdByEpisodeId.put((Long) row[0], (Long) row[1]);
        }
        // One reason per series even when several of its episodes cross the line together; the first one names it.
        Map<Long, TvEpisodeRequest> triggerBySeriesId = new HashMap<>();
        for (TvEpisodeRequest episode : staleEpisodes) {
            Long seriesId = seriesIdByEpisodeId.get(episode.getId());
            if (seriesId != null) {
                triggerBySeriesId.putIfAbsent(seriesId, episode);
            }
        }
        List<TvRequest> toSave = new ArrayList<>();
        for (TvRequest show : tvRequestRepository.findAllById(triggerBySeriesId.keySet())) {
            TvEpisodeRequest trigger = triggerBySeriesId.get(show.getId());
            if (trigger == null || Objects.equals(show.getStale(), true)) {
                continue;
            }
            show.setStale(true);
            show.setStaleReason(reason(trigger.getSearchCount(), trigger.getSearchFirstAt(), now, describe(trigger)));
            show.setMarkedStaleAt(now);
            toSave.add(show);
            log.info(
                    "Marked show '{}' stale after {} fruitless search(es) for {}",
                    show.getTitle(),
                    trigger.getSearchCount(),
                    describe(trigger));
        }
        if (!toSave.isEmpty()) {
            tvRequestRepository.saveAll(toSave);
        }
    }

    /** Null means never searched, so the first search lands on 1 rather than on an ambiguous 0. */
    private static Integer nextCount(@Nullable Integer current) {
        return current == null ? 1 : current + 1;
    }

    /** True once the span from the first search to now exceeds the configured limit. */
    private boolean spanExceedsLimit(@Nullable Instant firstAt, Instant now) {
        return firstAt != null
                && Duration.between(firstAt, now).compareTo(Duration.of(staleAfterDays, ChronoUnit.DAYS)) > 0;
    }

    private static String reason(
            @Nullable Integer searchCount, @Nullable Instant firstAt, Instant now, @Nullable String subject) {
        long days = firstAt == null ? 0 : Duration.between(firstAt, now).toDays();
        return AUTO_REASON_PREFIX + "still unavailable after "
                + Objects.requireNonNullElse(searchCount, 0) + " search(es) over " + days + " days"
                + (subject == null ? "" : " for " + subject);
    }

    /** Season/episode label for a stale reason, e.g. {@code S02E05}; unknown numbers render as {@code ?}. */
    private static String describe(TvEpisodeRequest episode) {
        TvSeasonRequest season = episode.getTvSeasonRequest();
        Integer seasonNumber = season == null ? null : season.getOmbiSeasonNumber();
        return "S" + label(seasonNumber) + "E" + label(episode.getOmbiEpisodeNumber());
    }

    private static String label(@Nullable Integer number) {
        return number == null ? "?" : String.format("%02d", number);
    }
}
