package report.butt.mediamanager.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.Limit;
import report.butt.mediamanager.client.RadarrClient;
import report.butt.mediamanager.client.SonarrClient;
import report.butt.mediamanager.model.MovieRequest;
import report.butt.mediamanager.model.TvChildRequest;
import report.butt.mediamanager.model.TvEpisodeRequest;
import report.butt.mediamanager.model.TvRequest;
import report.butt.mediamanager.model.TvSeasonRequest;
import report.butt.mediamanager.repository.MovieRequestRepository;
import report.butt.mediamanager.repository.TvEpisodeRequestRepository;

/**
 * The eligibility rules themselves live in the repository queries and so are only exercised end-to-end against a
 * database; these cover the parts this service decides — the cap it passes down, batching into one call per service,
 * un-staling first, and that it never issues a command with nothing to search for.
 */
@NullMarked
class AutoSearchServiceTest {

    private static final int RETRY_AFTER_DAYS = 7;
    private static final int MAX_PER_RUN = 25;

    private final MovieRequestRepository movieRepository = mock(MovieRequestRepository.class);
    private final TvEpisodeRequestRepository episodeRepository = mock(TvEpisodeRequestRepository.class);
    private final RadarrClient radarrClient = mock(RadarrClient.class);
    private final SonarrClient sonarrClient = mock(SonarrClient.class);
    private final SearchTrackingService searchTrackingService = mock(SearchTrackingService.class);

    private final AutoSearchService service = new AutoSearchService(
            movieRepository,
            episodeRepository,
            radarrClient,
            sonarrClient,
            searchTrackingService,
            RETRY_AFTER_DAYS,
            MAX_PER_RUN);

    private static MovieRequest movie(Long id, @Nullable Integer radarrId) {
        var movie = new MovieRequest("Title " + id, 100, false, 1, "Common.ProcessingRequest");
        movie.setId(id);
        movie.setRadarrRequestId(radarrId);
        return movie;
    }

    private static TvEpisodeRequest episode(Long id, @Nullable Integer sonarrEpisodeId) {
        var parent = new TvRequest("Show", 200, false, 5000, "Common.ProcessingRequest");
        parent.setId(10L);
        var child = new TvChildRequest(parent, "Show", 200, false, 6000, "Common.ProcessingRequest");
        child.setId(11L);
        var season = new TvSeasonRequest(child, 7000, 1, false);
        season.setId(12L);
        var episode = new TvEpisodeRequest(season, 8000, 1);
        episode.setId(id);
        episode.setSonarrEpisodeId(sonarrEpisodeId);
        return episode;
    }

    private void noEligibleItems() {
        when(movieRepository.findSearchable(any(), any())).thenReturn(List.of());
        when(episodeRepository.findSearchable(any(), any())).thenReturn(List.of());
    }

    @Test
    void searchesEligibleMoviesAndEpisodesInOneBatchEach() {
        when(movieRepository.findSearchable(any(), any())).thenReturn(List.of(movie(1L, 55), movie(2L, 56)));
        when(episodeRepository.findSearchable(any(), any())).thenReturn(List.of(episode(4L, 900), episode(5L, 901)));

        service.run();

        verify(radarrClient).searchMovies(List.of(55, 56));
        verify(sonarrClient).searchEpisodes(List.of(900, 901));
        verify(searchTrackingService).recordMovieSearches(List.of(55, 56));
        // Episodes are recorded by our own row ids, since the tracking columns live on TvEpisodeRequest.
        verify(searchTrackingService).recordEpisodeSearches(List.of(4L, 5L));
    }

    @Test
    void clearsStaleThatNoLongerAppliesBeforeSearching() {
        noEligibleItems();

        service.run();

        verify(searchTrackingService).clearAutoStaleNowAvailable();
    }

    @Test
    void issuesNoCommandWhenNothingIsEligible() {
        noEligibleItems();

        service.run();

        verify(radarrClient, never()).searchMovies(anyList());
        verify(sonarrClient, never()).searchEpisodes(anyList());
        verify(searchTrackingService, never()).recordMovieSearches(anyCollection());
        verify(searchTrackingService, never()).recordEpisodeSearches(anyCollection());
    }

    @Test
    void passesTheConfiguredCapToBothQueries() {
        noEligibleItems();

        service.run();

        var movieLimit = ArgumentCaptor.forClass(Limit.class);
        verify(movieRepository).findSearchable(any(), movieLimit.capture());
        assertEquals(MAX_PER_RUN, movieLimit.getValue().max());

        var episodeLimit = ArgumentCaptor.forClass(Limit.class);
        verify(episodeRepository).findSearchable(any(), episodeLimit.capture());
        assertEquals(MAX_PER_RUN, episodeLimit.getValue().max());
    }

    @Test
    void queriesWithARetryThresholdInThePast() {
        noEligibleItems();
        Instant before = Instant.now();

        service.run();

        var threshold = ArgumentCaptor.forClass(Instant.class);
        verify(movieRepository).findSearchable(threshold.capture(), any());
        // retry-after-days back from now, so anything searched more recently than that is left alone.
        assertEquals(true, threshold.getValue().isBefore(before));
    }

    @Test
    void skipsItemsMissingTheIdTheSearchCommandNeeds() {
        when(movieRepository.findSearchable(any(), any())).thenReturn(List.of(movie(1L, null)));
        when(episodeRepository.findSearchable(any(), any())).thenReturn(List.of(episode(4L, null)));

        service.run();

        verify(radarrClient, never()).searchMovies(anyList());
        verify(sonarrClient, never()).searchEpisodes(anyList());
    }

    @Test
    void countsOnlyTheEpisodesItActuallySearched() {
        // A row with no Sonarr id can't be searched, so it must not have its search history bumped either.
        when(movieRepository.findSearchable(any(), any())).thenReturn(List.of());
        when(episodeRepository.findSearchable(any(), any())).thenReturn(List.of(episode(4L, 900), episode(5L, null)));

        service.run();

        verify(sonarrClient).searchEpisodes(List.of(900));
        verify(searchTrackingService).recordEpisodeSearches(List.of(4L));
    }

    @Test
    void deduplicatesRepeatedIds() {
        when(movieRepository.findSearchable(any(), any())).thenReturn(List.of(movie(1L, 55), movie(2L, 55)));
        when(episodeRepository.findSearchable(any(), any())).thenReturn(List.of());

        service.run();

        verify(radarrClient).searchMovies(List.of(55));
    }
}
