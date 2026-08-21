package report.butt.mediamanager.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import report.butt.mediamanager.client.TdarrClient;
import report.butt.mediamanager.model.MovieRequest;
import report.butt.mediamanager.model.TvChildRequest;
import report.butt.mediamanager.model.TvEpisodeRequest;
import report.butt.mediamanager.model.TvRequest;
import report.butt.mediamanager.model.TvSeasonRequest;
import report.butt.mediamanager.model.tdarr.TdarrFile;
import report.butt.mediamanager.repository.MovieRequestRepository;
import report.butt.mediamanager.repository.TvEpisodeRequestRepository;

@NullMarked
class TdarrRefreshServiceTest {

    private final MovieRequestRepository movieRepository = mock(MovieRequestRepository.class);
    private final TvEpisodeRequestRepository episodeRepository = mock(TvEpisodeRequestRepository.class);
    private final TdarrClient tdarrClient = mock(TdarrClient.class);
    private final TdarrRefreshService service =
            new TdarrRefreshService(movieRepository, episodeRepository, tdarrClient);

    private static TdarrFile tdarrFile() {
        var file = new TdarrFile();
        file.setFile("/media/Movies/x.mkv");
        file.setHealthCheck("Success");
        file.setTranscodeDecisionMaker("Transcode success");
        file.setOldSize(0.2834);
        file.setNewSize(0.1351);
        return file;
    }

    /** Available = has a Radarr file and Ombi reports it available; see {@code MovieRequest.isAvailable()}. */
    private static MovieRequest availableMovie(@Nullable String plexMediaFilename) {
        var movie = new MovieRequest("Title", 100, true, 1, "Common.Available");
        movie.setId(42L);
        movie.setRadarrHasFile(true);
        movie.setPlexMediaFilename(plexMediaFilename);
        return movie;
    }

    private static TvEpisodeRequest episode(@Nullable Boolean available, @Nullable String plexPath) {
        var parent = new TvRequest("Show", 100, true, 5000, "Common.Available");
        var child = new TvChildRequest(parent, "Show", 100, true, 6000, "Common.Available");
        var season = new TvSeasonRequest(child, 7000, 3, true);
        var episode = new TvEpisodeRequest(season, 8000, 3);
        episode.setId(7L);
        episode.setOmbiAvailable(available);
        episode.setPlexPath(plexPath);
        return episode;
    }

    // --- candidate ids ---

    @Test
    void refreshableIdsComeFromTheRepositories() {
        when(movieRepository.findTdarrRefreshableMovieIds()).thenReturn(List.of(1L, 2L));
        when(episodeRepository.findTdarrRefreshableEpisodeIds()).thenReturn(List.of(3L));

        assertEquals(List.of(1L, 2L), service.refreshableMovieIds());
        assertEquals(List.of(3L), service.refreshableEpisodeIds());
        verifyNoInteractions(tdarrClient);
    }

    // --- movies ---

    @Test
    void writesTdarrFieldsOntoAnAvailableMovie() {
        MovieRequest movie = availableMovie("/media/Movies/x.mkv");
        when(movieRepository.findById(42L)).thenReturn(Optional.of(movie));
        when(tdarrClient.findByPath("/media/Movies/x.mkv")).thenReturn(tdarrFile());

        service.refreshMovie(42L);

        assertEquals("Success", movie.getTdarrHealthCheck());
        assertEquals("Transcode success", movie.getTdarrTranscodeDecisionMaker());
        assertEquals(0.2834, movie.getTdarrOldSizeGb());
        assertEquals(0.1351, movie.getTdarrNewSizeGb());
        assertNotNull(movie.getTdarrLastUpdated());
        verify(movieRepository).save(movie);
    }

    /** The id was chosen when the sweep was queued, so by run time the movie may be gone or no longer available. */
    @Test
    void skipsAMovieThatNoLongerExists() {
        when(movieRepository.findById(42L)).thenReturn(Optional.empty());

        service.refreshMovie(42L);

        verifyNoInteractions(tdarrClient);
        verify(movieRepository, never()).save(any());
    }

    @Test
    void skipsAMovieThatIsNoLongerAvailable() {
        MovieRequest movie = availableMovie("/media/Movies/x.mkv");
        movie.setRadarrHasFile(false);
        when(movieRepository.findById(42L)).thenReturn(Optional.of(movie));

        service.refreshMovie(42L);

        verifyNoInteractions(tdarrClient);
        verify(movieRepository, never()).save(any());
    }

    @Test
    void skipsAMovieWithNoPlexPath() {
        when(movieRepository.findById(42L)).thenReturn(Optional.of(availableMovie(null)));

        service.refreshMovie(42L);

        verifyNoInteractions(tdarrClient);
        verify(movieRepository, never()).save(any());
    }

    /** A miss or an unreachable Tdarr must leave what is stored alone rather than blanking it. */
    @Test
    void leavesStoredValuesAloneWhenTdarrHasNoMatch() {
        MovieRequest movie = availableMovie("/media/Movies/x.mkv");
        movie.setTdarrHealthCheck("Success");
        when(movieRepository.findById(42L)).thenReturn(Optional.of(movie));
        when(tdarrClient.findByPath(any())).thenReturn(null);

        service.refreshMovie(42L);

        assertEquals("Success", movie.getTdarrHealthCheck());
        assertNull(movie.getTdarrLastUpdated(), "a miss should not count as a successful contact");
        verify(movieRepository, never()).save(any());
    }

    // --- episodes ---

    @Test
    void writesTdarrFieldsOntoAnAvailableEpisode() {
        TvEpisodeRequest episode = episode(true, "/media/TV/Show/x.mkv");
        when(episodeRepository.findById(7L)).thenReturn(Optional.of(episode));
        when(tdarrClient.findByPath("/media/TV/Show/x.mkv")).thenReturn(tdarrFile());

        service.refreshEpisode(7L);

        assertEquals("Success", episode.getTdarrHealthCheck());
        assertEquals("Transcode success", episode.getTdarrTranscodeDecisionMaker());
        assertEquals(0.2834, episode.getTdarrOldSizeGb());
        assertEquals(0.1351, episode.getTdarrNewSizeGb());
        assertNotNull(episode.getTdarrLastUpdated());
        verify(episodeRepository).save(episode);
    }

    @Test
    void skipsAnUnavailableEpisode() {
        when(episodeRepository.findById(7L)).thenReturn(Optional.of(episode(false, "/media/TV/Show/x.mkv")));

        service.refreshEpisode(7L);

        verifyNoInteractions(tdarrClient);
        verify(episodeRepository, never()).save(any());
    }

    @Test
    void skipsAnEpisodeWithNoPlexPath() {
        when(episodeRepository.findById(7L)).thenReturn(Optional.of(episode(true, null)));

        service.refreshEpisode(7L);

        verifyNoInteractions(tdarrClient);
        verify(episodeRepository, never()).save(any());
    }

    @Test
    void skipsAnEpisodeThatNoLongerExists() {
        when(episodeRepository.findById(7L)).thenReturn(Optional.empty());

        service.refreshEpisode(7L);

        verifyNoInteractions(tdarrClient);
        verify(episodeRepository, never()).save(any());
    }
}
