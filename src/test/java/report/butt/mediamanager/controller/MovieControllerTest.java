package report.butt.mediamanager.controller;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import report.butt.mediamanager.client.OmbiClient;
import report.butt.mediamanager.client.RadarrClient;
import report.butt.mediamanager.model.MovieRequest;
import report.butt.mediamanager.model.radarr.RadarrCommand;
import report.butt.mediamanager.model.radarr.RadarrQueue;
import report.butt.mediamanager.model.radarr.RadarrQueueRecord;
import report.butt.mediamanager.repository.MovieRequestRepository;
import report.butt.mediamanager.service.MovieRefreshService;
import report.butt.mediamanager.service.RequestAdminService;
import report.butt.mediamanager.service.ValidatorService;
import tools.jackson.databind.ObjectMapper;

class MovieControllerTest {

    private final MovieRequestRepository movieRequestRepository = mock(MovieRequestRepository.class);
    private final RadarrClient radarrClient = mock(RadarrClient.class);

    private final MovieController controller = new MovieController(
            movieRequestRepository,
            mock(OmbiClient.class),
            radarrClient,
            mock(ObjectMapper.class),
            mock(MovieRefreshService.class),
            mock(ValidatorService.class),
            mock(RequestAdminService.class));

    @Test
    void deleteDownloadAndSearch_deletesOnlyThisMoviesQueueItemsThenSearches() {
        MovieRequest movie = new MovieRequest("The Movie", 1, false, 100, "Common.ProcessingRequest");
        movie.setId(5L);
        movie.setRadarrRequestId(42);

        when(movieRequestRepository.findById(5L)).thenReturn(Optional.of(movie));
        // One queue item for this movie (id 777), one for a different movie (id 778) that must be left alone.
        when(radarrClient.getQueue()).thenReturn(queueOf(queueRecord(777, 42), queueRecord(778, 99)));
        when(radarrClient.searchMovies(List.of(42))).thenReturn(new RadarrCommand());

        controller.deleteDownloadAndSearch(5L);

        verify(radarrClient).getQueue();
        verify(radarrClient).deleteQueueItem(777);
        verify(radarrClient).searchMovies(List.of(42));
        verifyNoMoreInteractions(radarrClient);
        verify(movieRequestRepository).save(movie);
    }

    @Test
    void deleteDownloadAndSearch_withoutRadarrRequestId_doesNothing() {
        MovieRequest movie = new MovieRequest("No Radarr", 1, false, 100, "Common.ProcessingRequest");
        movie.setId(6L);

        when(movieRequestRepository.findById(6L)).thenReturn(Optional.of(movie));

        controller.deleteDownloadAndSearch(6L);

        verifyNoMoreInteractions(radarrClient);
    }

    private static RadarrQueueRecord queueRecord(Integer id, Integer movieId) {
        RadarrQueueRecord record = new RadarrQueueRecord();
        record.setId(id);
        record.setMovieId(movieId);
        return record;
    }

    private static RadarrQueue queueOf(RadarrQueueRecord... records) {
        RadarrQueue queue = new RadarrQueue();
        queue.setRecords(List.of(records));
        return queue;
    }
}
