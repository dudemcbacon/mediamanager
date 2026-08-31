package report.butt.mediamanager.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Objects;
import java.util.List;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import report.butt.mediamanager.model.MovieRequest;
import report.butt.mediamanager.model.TvChildRequest;
import report.butt.mediamanager.model.TvEpisodeRequest;
import report.butt.mediamanager.model.TvRequest;
import report.butt.mediamanager.model.TvSeasonRequest;
import report.butt.mediamanager.repository.MovieRequestRepository;
import report.butt.mediamanager.repository.TvEpisodeRequestRepository;
import report.butt.mediamanager.repository.TvRequestRepository;

@NullMarked
class SearchTrackingServiceTest {

    private static final int STALE_AFTER_DAYS = 90;

    private final MovieRequestRepository movieRepository = mock(MovieRequestRepository.class);
    private final TvRequestRepository tvRequestRepository = mock(TvRequestRepository.class);
    private final TvEpisodeRequestRepository episodeRepository = mock(TvEpisodeRequestRepository.class);
    private final SearchTrackingService service =
            new SearchTrackingService(movieRepository, tvRequestRepository, episodeRepository, STALE_AFTER_DAYS);

    private static MovieRequest movie(@Nullable Instant firstAt, @Nullable Integer count) {
        var movie = new MovieRequest("Title", 100, false, 1, "Common.ProcessingRequest");
        movie.setId(1L);
        movie.setRadarrRequestId(55);
        movie.setSearchFirstAt(firstAt);
        movie.setSearchCount(count);
        return movie;
    }

    private static TvEpisodeRequest episode(Long id, @Nullable Instant firstAt, @Nullable Integer count) {
        var parent = new TvRequest("Show", 200, false, 5000, "Common.ProcessingRequest");
        parent.setId(10L);
        parent.setSonarrSeriesId(4);
        var child = new TvChildRequest(parent, "Show", 200, false, 6000, "Common.ProcessingRequest");
        child.setId(11L);
        var season = new TvSeasonRequest(child, 7000, 2, false);
        season.setId(12L);
        var episode = new TvEpisodeRequest(season, 8000, 5);
        episode.setId(id);
        episode.setSearchFirstAt(firstAt);
        episode.setSearchCount(count);
        return episode;
    }

    private static Instant daysAgo(int days) {
        return Instant.now().minus(days, ChronoUnit.DAYS);
    }

    // ---- movies ----

    @Test
    void firstMovieSearchSetsCountAndBothTimestamps() {
        MovieRequest movie = movie(null, null);
        when(movieRepository.findByRadarrRequestIdIn(any())).thenReturn(List.of(movie));

        service.recordMovieSearches(List.of(55));

        assertEquals(1, movie.getSearchCount());
        assertNotNull(movie.getSearchFirstAt());
        assertEquals(movie.getSearchFirstAt(), movie.getSearchLastAt());
        // The radarr stamp is kept so a search shows before the next refresh reads it back from Radarr.
        assertNotNull(movie.getRadarrLastSearchTime());
        verify(movieRepository).saveAll(List.of(movie));
    }

    @Test
    void laterMovieSearchIncrementsCountAndMovesOnlyTheLastTimestamp() {
        Instant firstAt = daysAgo(10);
        MovieRequest movie = movie(firstAt, 3);

        when(movieRepository.findByRadarrRequestIdIn(any())).thenReturn(List.of(movie));

        service.recordMovieSearches(List.of(55));

        assertEquals(4, movie.getSearchCount());
        assertEquals(firstAt, movie.getSearchFirstAt());
        assertTrue(assertNonNull(movie.getSearchLastAt()).isAfter(firstAt));
    }

    @Test
    void movieSearchingLongerThanTheLimitMarksItStale() {
        MovieRequest movie = movie(daysAgo(STALE_AFTER_DAYS + 1), 12);
        when(movieRepository.findByRadarrRequestIdIn(any())).thenReturn(List.of(movie));

        service.recordMovieSearches(List.of(55));

        assertTrue(assertNonNull(movie.getStale()));
        assertTrue(assertNonNull(movie.getStaleReason()).startsWith(SearchTrackingService.AUTO_REASON_PREFIX));
        assertNotNull(movie.getMarkedStaleAt());
    }

    @Test
    void movieWithinTheLimitIsNotMarkedStale() {
        MovieRequest movie = movie(daysAgo(STALE_AFTER_DAYS - 1), 12);
        when(movieRepository.findByRadarrRequestIdIn(any())).thenReturn(List.of(movie));

        service.recordMovieSearches(List.of(55));

        assertNull(movie.getStale());
        assertNull(movie.getStaleReason());
    }

    @Test
    void aMovieAlreadyStaleKeepsItsExistingReason() {
        MovieRequest movie = movie(daysAgo(STALE_AFTER_DAYS + 1), 12);
        movie.setStale(true);
        movie.setStaleReason("duplicate request");
        when(movieRepository.findByRadarrRequestIdIn(any())).thenReturn(List.of(movie));

        service.recordMovieSearches(List.of(55));

        assertEquals("duplicate request", movie.getStaleReason());
    }

    @Test
    void noMovieIdsIsANoOp() {
        service.recordMovieSearches(List.of());

        verify(movieRepository, never()).findByRadarrRequestIdIn(anyCollection());
        verify(movieRepository, never()).saveAll(any());
    }

    // ---- episodes ----

    @Test
    void firstEpisodeSearchSetsCountAndBothTimestamps() {
        TvEpisodeRequest episode = episode(4L, null, null);
        when(episodeRepository.findAllById(any())).thenReturn(List.of(episode));

        service.recordEpisodeSearches(List.of(4L));

        assertEquals(1, episode.getSearchCount());
        assertNotNull(episode.getSearchFirstAt());
        assertEquals(episode.getSearchFirstAt(), episode.getSearchLastAt());
        assertNotNull(episode.getSonarrLastSearchTime());
        verify(episodeRepository).saveAll(List.of(episode));
    }

    @Test
    void episodeSearchingLongerThanTheLimitMarksTheParentSeriesStale() {
        TvEpisodeRequest episode = episode(4L, daysAgo(STALE_AFTER_DAYS + 1), 20);
        var show = new TvRequest("Show", 200, false, 5000, "Common.ProcessingRequest");
        show.setId(10L);

        when(episodeRepository.findAllById(any())).thenReturn(List.of(episode));
        when(episodeRepository.findParentTvRequestIds(any())).thenReturn(List.<Object[]>of(new Object[] {4L, 10L}));
        when(tvRequestRepository.findAllById(any())).thenReturn(List.of(show));

        service.recordEpisodeSearches(List.of(4L));

        assertTrue(assertNonNull(show.getStale()));
        String reason = assertNonNull(show.getStaleReason());
        assertTrue(reason.startsWith(SearchTrackingService.AUTO_REASON_PREFIX));
        // The reason names the episode that triggered it, so a stale show is traceable to a cause.
        assertTrue(reason.contains("S02E05"), "expected the episode label in: " + reason);
        assertNotNull(show.getMarkedStaleAt());
        verify(tvRequestRepository).saveAll(List.of(show));
    }

    @Test
    void anEpisodeWithinTheLimitLeavesTheSeriesAlone() {
        TvEpisodeRequest episode = episode(4L, daysAgo(STALE_AFTER_DAYS - 1), 20);
        when(episodeRepository.findAllById(any())).thenReturn(List.of(episode));

        service.recordEpisodeSearches(List.of(4L));

        verify(episodeRepository, never()).findParentTvRequestIds(anyCollection());
        verify(tvRequestRepository, never()).saveAll(any());
    }

    @Test
    void aSeriesAlreadyStaleKeepsItsExistingReason() {
        TvEpisodeRequest episode = episode(4L, daysAgo(STALE_AFTER_DAYS + 1), 20);
        var show = new TvRequest("Show", 200, false, 5000, "Common.ProcessingRequest");
        show.setId(10L);
        show.setStale(true);
        show.setStaleReason("cancelled by requester");

        when(episodeRepository.findAllById(any())).thenReturn(List.of(episode));
        when(episodeRepository.findParentTvRequestIds(any())).thenReturn(List.<Object[]>of(new Object[] {4L, 10L}));
        when(tvRequestRepository.findAllById(any())).thenReturn(List.of(show));

        service.recordEpisodeSearches(List.of(4L));

        assertEquals("cancelled by requester", show.getStaleReason());
    }

    // ---- fan-out ----

    @Test
    void aSeriesSearchIsAttributedToItsUnavailableEpisodes() {
        TvEpisodeRequest episode = episode(4L, null, null);
        when(episodeRepository.findUnavailableBySonarrSeriesIdIn(any())).thenReturn(List.of(episode));

        service.recordSeriesSearches(List.of(4));

        assertEquals(1, episode.getSearchCount());
        verify(episodeRepository).saveAll(List.of(episode));
    }

    @Test
    void aSeasonSearchIsAttributedToThatSeasonsUnavailableEpisodes() {
        TvEpisodeRequest episode = episode(4L, null, null);
        when(episodeRepository.findUnavailableBySonarrSeriesIdAndSeasonNumbers(any(), any()))
                .thenReturn(List.of(episode));

        service.recordSeasonSearches(4, List.of(2));

        assertEquals(1, episode.getSearchCount());
        verify(episodeRepository).saveAll(List.of(episode));
    }

    @Test
    void aSeasonSearchWithNoSeriesIdIsANoOp() {
        service.recordSeasonSearches(null, List.of(2));

        verify(episodeRepository, never()).findUnavailableBySonarrSeriesIdAndSeasonNumbers(any(), anyCollection());
    }

    // ---- un-stale ----

    @Test
    void anAutoStaleRequestThatBecameAvailableIsCleared() {
        MovieRequest movie = movie(daysAgo(100), 12);
        movie.setStale(true);
        movie.setStaleReason(SearchTrackingService.AUTO_REASON_PREFIX + "still unavailable after 12 search(es)");
        movie.setMarkedStaleAt(daysAgo(1));
        when(movieRepository.findAutoStaleNowAvailable(SearchTrackingService.AUTO_REASON_PREFIX))
                .thenReturn(List.of(movie));
        when(tvRequestRepository.findAutoStaleNowAvailable(SearchTrackingService.AUTO_REASON_PREFIX))
                .thenReturn(List.of());

        assertEquals(1, service.clearAutoStaleNowAvailable());

        assertFalse(assertNonNull(movie.getStale()));
        assertNull(movie.getStaleReason());
        assertNull(movie.getMarkedStaleAt());
        verify(movieRepository).saveAll(List.of(movie));
    }

    @Test
    void nothingToClearSavesNothing() {
        when(movieRepository.findAutoStaleNowAvailable(any())).thenReturn(List.of());
        when(tvRequestRepository.findAutoStaleNowAvailable(any())).thenReturn(List.of());

        assertEquals(0, service.clearAutoStaleNowAvailable());

        verify(movieRepository, never()).saveAll(any());
        verify(tvRequestRepository, never()).saveAll(any());
    }

    /** Asserts non-null and returns the value, so NullAway-clean assertions read as one line. */
    private static <T> T assertNonNull(@Nullable T value) {
        assertNotNull(value);
        return Objects.requireNonNull(value);
    }
}
