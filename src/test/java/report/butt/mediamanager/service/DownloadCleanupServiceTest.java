package report.butt.mediamanager.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import report.butt.mediamanager.client.RadarrClient;
import report.butt.mediamanager.client.SonarrClient;
import report.butt.mediamanager.model.MovieRequest;
import report.butt.mediamanager.model.TvRequest;
import report.butt.mediamanager.model.radarr.RadarrQueue;
import report.butt.mediamanager.model.radarr.RadarrQueueRecord;
import report.butt.mediamanager.model.sonarr.Episode;
import report.butt.mediamanager.model.sonarr.SonarrQueue;
import report.butt.mediamanager.model.sonarr.SonarrQueueRecord;
import report.butt.mediamanager.repository.MovieRequestRepository;
import report.butt.mediamanager.repository.TvRequestRepository;

class DownloadCleanupServiceTest {

    private final RadarrClient radarrClient = mock(RadarrClient.class);
    private final SonarrClient sonarrClient = mock(SonarrClient.class);
    private final MovieRequestRepository movieRequestRepository = mock(MovieRequestRepository.class);
    private final TvRequestRepository tvRequestRepository = mock(TvRequestRepository.class);
    private final MovieRefreshService movieRefreshService = mock(MovieRefreshService.class);
    private final TvRefreshService tvRefreshService = mock(TvRefreshService.class);

    private final DownloadCleanupService service = new DownloadCleanupService(
            radarrClient,
            sonarrClient,
            movieRequestRepository,
            tvRequestRepository,
            movieRefreshService,
            tvRefreshService);

    @Test
    void deletesMatchingQueueItemsThenSearchesAndRefreshes() {
        // Radarr: one queued item matches a zero-seed hash, one doesn't.
        when(radarrClient.getQueue())
                .thenReturn(radarrQueue(radarrRecord(101, 10, "ABCHASH"), radarrRecord(102, 11, "OTHER")));
        // Sonarr: one queued item matches.
        when(sonarrClient.getQueue()).thenReturn(sonarrQueue(sonarrRecord(201, 20, 999, "DEFHASH")));

        var movie = new MovieRequest("Movie", 1, false, 1, "Common.ProcessingRequest");
        movie.setId(5L);
        when(movieRequestRepository.findByRadarrRequestId(10)).thenReturn(Optional.of(movie));
        var show = new TvRequest("Show", 1, false, 1, "Common.ProcessingRequest");
        show.setId(7L);
        when(tvRequestRepository.findBySonarrSeriesId(20)).thenReturn(List.of(show));

        // Hashes matched case-insensitively.
        DownloadCleanupService.CleanupResult result = service.deleteTorrentsAndReprocess(Set.of("abchash", "defhash"));

        assertEquals(2, result.torrentsDeleted());
        assertEquals(1, result.moviesReprocessed());
        assertEquals(1, result.showsReprocessed());

        verify(radarrClient).deleteQueueItem(101);
        verify(radarrClient, never()).deleteQueueItem(102); // non-matching download id untouched
        verify(sonarrClient).deleteQueueItem(201);
        verify(radarrClient).searchMovies(List.of(10));
        verify(sonarrClient).searchEpisodes(List.of(999));
        verify(movieRefreshService).refreshOne(5L);
        verify(tvRefreshService).refreshOne(7L);
    }

    @Test
    void emptyHashesIsANoOp() {
        DownloadCleanupService.CleanupResult result = service.deleteTorrentsAndReprocess(Set.of());

        assertEquals(0, result.torrentsDeleted());
        verify(radarrClient, never()).getQueue();
        verify(sonarrClient, never()).getQueue();
    }

    private static RadarrQueue radarrQueue(RadarrQueueRecord... records) {
        var queue = new RadarrQueue();
        queue.setRecords(List.of(records));
        return queue;
    }

    private static RadarrQueueRecord radarrRecord(Integer id, Integer movieId, String downloadId) {
        var record = new RadarrQueueRecord();
        record.setId(id);
        record.setMovieId(movieId);
        record.setDownloadId(downloadId);
        return record;
    }

    private static SonarrQueue sonarrQueue(SonarrQueueRecord... records) {
        var queue = new SonarrQueue();
        queue.setRecords(List.of(records));
        return queue;
    }

    private static SonarrQueueRecord sonarrRecord(Integer id, Integer seriesId, Integer episodeId, String downloadId) {
        var record = new SonarrQueueRecord();
        record.setId(id);
        record.setSeriesId(seriesId);
        record.setDownloadId(downloadId);
        var episode = new Episode();
        episode.setId(episodeId);
        record.setEpisode(episode);
        return record;
    }
}
