package report.butt.mediamanager.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.jobrunr.scheduling.JobRequestScheduler;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import report.butt.mediamanager.client.MetadataResult;
import report.butt.mediamanager.client.OmbiClient;
import report.butt.mediamanager.client.PlexClient;
import report.butt.mediamanager.client.RadarrClient;
import report.butt.mediamanager.exceptions.RequestNotFoundException;
import report.butt.mediamanager.model.MovieRequest;
import report.butt.mediamanager.model.ombi.OmbiMovieRequest;
import report.butt.mediamanager.model.radarr.Movie;
import report.butt.mediamanager.model.radarr.Moviefile;
import report.butt.mediamanager.repository.MovieRequestRepository;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@NullMarked
class MovieRefreshServiceTest {

    private final MovieRequestRepository repository = mock(MovieRequestRepository.class);
    private final OmbiClient ombiClient = mock(OmbiClient.class);
    private final RadarrClient radarrClient = mock(RadarrClient.class);
    private final PlexClient plexClient = mock(PlexClient.class);
    private final PlexCacheService plexCacheService = mock(PlexCacheService.class);
    private final JobRequestScheduler jobRequestScheduler = mock(JobRequestScheduler.class);
    private final FfprobeScanService ffprobeScanService = mock(FfprobeScanService.class);

    private final MovieRefreshService service = new MovieRefreshService(
            repository,
            ombiClient,
            radarrClient,
            plexClient,
            plexCacheService,
            "",
            jobRequestScheduler,
            ffprobeScanService);

    // --- refreshAll ---

    @Test
    void refreshAllSavesNewMovies() {
        OmbiMovieRequest ombi = ombiMovie(1, "New Movie", 100);
        when(ombiClient.getMovies()).thenReturn(List.of(ombi));
        when(radarrClient.getMovies()).thenReturn(List.of());
        when(radarrClient.getQualityProfilesById()).thenReturn(Map.of());
        when(plexClient.getAllMoviesIndexedByTmdb()).thenReturn(Map.of());
        when(repository.findByOmbiRequestIdIn(any())).thenReturn(List.of());
        when(plexClient.cacheMovieMetadata(any(Integer.class), any())).thenReturn("/plex-cache/movie-100.json");

        service.refreshAll();

        verify(repository).saveAll(anyList());
        verify(plexCacheService).cleanExcept(any(), any());
    }

    @Test
    void refreshAllSkipsSaveWhenExistingMovieUnchanged() {
        OmbiMovieRequest ombi = ombiMovie(1, "Existing Movie", 100);
        var existing = new MovieRequest("Existing Movie", 100, false, 1, "Common.ProcessingRequest");
        existing.setId(5L);
        // Set ombi fields that applyUpdates will set — so hash won't change
        existing.setTitle("Existing Movie");
        existing.setTmdbid(100);
        existing.setOmbiAvailable(false);
        existing.setOmbiRequestStatus("Common.ProcessingRequest");

        when(ombiClient.getMovies()).thenReturn(List.of(ombi));
        when(radarrClient.getMovies()).thenReturn(List.of());
        when(radarrClient.getQualityProfilesById()).thenReturn(Map.of());
        when(plexClient.getAllMoviesIndexedByTmdb()).thenReturn(Map.of());
        when(repository.findByOmbiRequestIdIn(any())).thenReturn(List.of(existing));

        service.refreshAll();

        // No radarr movie → no plex lookup; existing row unchanged → saveAll with empty list or not called
        // The service only calls saveAll when toSave is non-empty
        verify(repository, never()).saveAll(anyList());
        verify(plexCacheService).cleanExcept(any(), any());
    }

    @Test
    void refreshAllWithEmptyOmbiResponseDoesNothingExceptClean() {
        when(ombiClient.getMovies()).thenReturn(List.of());
        when(radarrClient.getMovies()).thenReturn(List.of());
        when(radarrClient.getQualityProfilesById()).thenReturn(Map.of());
        when(plexClient.getAllMoviesIndexedByTmdb()).thenReturn(Map.of());
        when(repository.findByOmbiRequestIdIn(any())).thenReturn(List.of());

        service.refreshAll();

        verify(repository, never()).saveAll(anyList());
        verify(plexCacheService).cleanExcept("movie-", Set.of());
    }

    @Test
    void refreshAllReCachesRadarrQualityProfiles() {
        when(ombiClient.getMovies()).thenReturn(List.of());
        when(radarrClient.getMovies()).thenReturn(List.of());
        when(radarrClient.getQualityProfilesById()).thenReturn(Map.of());
        when(plexClient.getAllMoviesIndexedByTmdb()).thenReturn(Map.of());
        when(repository.findByOmbiRequestIdIn(any())).thenReturn(List.of());

        service.refreshAll();

        // Profiles are re-cached each refresh so renamed/added Radarr profiles self-heal without a restart.
        verify(radarrClient).cacheQualityProfiles();
    }

    @Test
    void refreshAllWithRadarrMatchAppliesRadarrFields() {
        OmbiMovieRequest ombi = ombiMovie(1, "Radarr Movie", 200);
        Movie radarrMovie = radarrMovie(200, 55, "/movies/radarr-movie");

        when(ombiClient.getMovies()).thenReturn(List.of(ombi));
        when(radarrClient.getMovies()).thenReturn(List.of(radarrMovie));
        when(radarrClient.getQualityProfilesById()).thenReturn(Map.of(1, "HD-1080p"));
        when(plexClient.getAllMoviesIndexedByTmdb()).thenReturn(Map.of());
        when(repository.findByOmbiRequestIdIn(any())).thenReturn(List.of());
        when(plexClient.cacheMovieMetadata(any(Integer.class), any())).thenReturn("/plex-cache/movie-200.json");

        service.refreshAll();

        verify(repository).saveAll(anyList());
    }

    @Test
    void refreshAllPlexExceptionIsSwallowed() {
        OmbiMovieRequest ombi = ombiMovie(1, "Movie", 300);
        Movie radarrMovie = radarrMovie(300, 66, "/movies/movie");

        when(ombiClient.getMovies()).thenReturn(List.of(ombi));
        when(radarrClient.getMovies()).thenReturn(List.of(radarrMovie));
        when(radarrClient.getQualityProfilesById()).thenReturn(Map.of());
        when(plexClient.getAllMoviesIndexedByTmdb()).thenReturn(Map.of());
        when(repository.findByOmbiRequestIdIn(any())).thenReturn(List.of());
        when(plexClient.cacheMovieMetadata(any(Integer.class), any())).thenThrow(new RuntimeException("Plex down"));

        // Should not propagate — plex errors are swallowed in applyPlexUpdates
        service.refreshAll();

        verify(repository).saveAll(anyList());
    }

    @Test
    void refreshAllCollapsesDuplicateOmbiRequestsForSameRadarrMovie() {
        // Two Ombi requests for the same film (same tmdb -> same Radarr movie). radarr_request_id is unique, so only
        // one row may be saved rather than a second insert that violates the constraint.
        OmbiMovieRequest first = ombiMovie(1, "Inside", 958196);
        OmbiMovieRequest second = ombiMovie(2, "Inside", 958196);
        Movie radarrMovie = radarrMovie(958196, 3956, "/movies/inside");

        when(ombiClient.getMovies()).thenReturn(List.of(first, second));
        when(radarrClient.getMovies()).thenReturn(List.of(radarrMovie));
        when(radarrClient.getQualityProfilesById()).thenReturn(Map.of());
        when(plexClient.getAllMoviesIndexedByTmdb()).thenReturn(Map.of());
        when(repository.findByOmbiRequestIdIn(any())).thenReturn(List.of());
        when(repository.findByRadarrRequestIdIn(any())).thenReturn(List.of());
        when(plexClient.cacheMovieMetadata(any(Integer.class), any())).thenReturn("/plex-cache/movie-958196.json");

        service.refreshAll();

        List<MovieRequest> saved = captureSavedAll();
        assertEquals(1, saved.size());
        assertEquals(3956, saved.get(0).getRadarrRequestId());
    }

    @Test
    void refreshAllReusesExistingRowForReRequestedMovie() {
        // The movie's row already exists (under an old Ombi id no longer in the response); the current Ombi request has
        // a new id, so it must update that row in place — keyed by Radarr movie — rather than insert a colliding row.
        var existing = new MovieRequest("Inside", 958196, false, 1, "Common.ProcessingRequest");
        existing.setId(42L);
        existing.setRadarrRequestId(3956);
        OmbiMovieRequest reRequest = ombiMovie(2, "Inside", 958196);
        Movie radarrMovie = radarrMovie(958196, 3956, "/movies/inside");

        when(ombiClient.getMovies()).thenReturn(List.of(reRequest));
        when(radarrClient.getMovies()).thenReturn(List.of(radarrMovie));
        when(radarrClient.getQualityProfilesById()).thenReturn(Map.of());
        when(plexClient.getAllMoviesIndexedByTmdb()).thenReturn(Map.of());
        when(repository.findByOmbiRequestIdIn(any())).thenReturn(List.of()); // old Ombi id not in the current response
        when(repository.findByRadarrRequestIdIn(any())).thenReturn(List.of(existing));
        when(plexClient.cacheMovieMetadata(any(Integer.class), any())).thenReturn("/plex-cache/movie-958196.json");

        service.refreshAll();

        List<MovieRequest> saved = captureSavedAll();
        assertEquals(1, saved.size());
        assertEquals(42L, saved.get(0).getId()); // reused the existing row in place, not a colliding new insert
        assertEquals(3956, saved.get(0).getRadarrRequestId());
    }

    private List<MovieRequest> captureSavedAll() {
        @SuppressWarnings("unchecked") // Mockito's forClass can't carry the List<MovieRequest> generic; the cast is safe
        ArgumentCaptor<List<MovieRequest>> captor = ArgumentCaptor.forClass(List.class);
        verify(repository).saveAll(captor.capture());
        return captor.getValue();
    }

    // --- refreshOne ---

    @Test
    void refreshOneThrowsWhenNotFound() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(RequestNotFoundException.class, () -> service.refreshOne(99L));
    }

    @Test
    void refreshOneWithNullOmbiIdSkipsOmbiLookup() {
        var existing = new MovieRequest("My Movie", 400, false, null, "Common.ProcessingRequest");
        existing.setId(1L);
        when(repository.findById(1L)).thenReturn(Optional.of(existing));
        when(radarrClient.getQualityProfilesById()).thenReturn(Map.of());
        when(radarrClient.getMoviesByTmdbId(400)).thenReturn(List.of());

        service.refreshOne(1L);

        verify(repository).save(existing);
    }

    @Test
    void refreshOneWithOmbiIdFetchesOmbiAndRadarr() {
        var existing = new MovieRequest("Some Movie", 500, false, 10, "Common.ProcessingRequest");
        existing.setId(2L);
        OmbiMovieRequest ombi = ombiMovie(10, "Some Movie", 500);
        Movie radarrMovie = radarrMovie(500, 77, "/movies/some-movie");

        when(repository.findById(2L)).thenReturn(Optional.of(existing));
        when(ombiClient.getMovies()).thenReturn(List.of(ombi));
        when(radarrClient.getMoviesByTmdbId(500)).thenReturn(List.of(radarrMovie));
        when(radarrClient.getQualityProfilesById()).thenReturn(Map.of());
        when(plexClient.getMovieByTmdbId(any(Integer.class), any(), any(Integer.class)))
                .thenReturn(new MetadataResult("/plex-cache/movie-500.json", null));

        service.refreshOne(2L);

        verify(repository).save(existing);
    }

    @Test
    void refreshOneWithOmbiIdNotFoundInOmbiResponse() {
        var existing = new MovieRequest("Orphan Movie", 600, false, 20, "Common.ProcessingRequest");
        existing.setId(3L);

        when(repository.findById(3L)).thenReturn(Optional.of(existing));
        when(ombiClient.getMovies()).thenReturn(List.of()); // 20 not in the list
        when(radarrClient.getQualityProfilesById()).thenReturn(Map.of());
        when(radarrClient.getMoviesByTmdbId(600)).thenReturn(List.of());

        service.refreshOne(3L);

        verify(repository).save(existing);
    }

    // --- local file status ---

    @Test
    void refreshOneRecordsLocalFileWhenItExists(@TempDir Path tempDir) throws IOException {
        // localFileSystemPrefix (tempDir) + radarr path ("/movies/movie.mkv") must resolve to the real file.
        Path moviesDir = Files.createDirectories(tempDir.resolve("movies"));
        Files.write(moviesDir.resolve("movie.mkv"), new byte[] {1, 2, 3, 4, 5});

        var prefixed = new MovieRefreshService(
                repository,
                ombiClient,
                radarrClient,
                plexClient,
                plexCacheService,
                tempDir.toString(),
                jobRequestScheduler,
                ffprobeScanService);

        var existing = new MovieRequest("Some Movie", 500, false, null, "Common.ProcessingRequest");
        existing.setId(2L);
        Movie radarrMovie = radarrMovie(500, 77, "/movies/some-movie");
        radarrMovie.setMovieFile(movieFile("/movies/movie.mkv"));

        when(repository.findById(2L)).thenReturn(Optional.of(existing));
        when(radarrClient.getMoviesByTmdbId(500)).thenReturn(List.of(radarrMovie));
        when(radarrClient.getQualityProfilesById()).thenReturn(Map.of());
        when(plexClient.getMovieByTmdbId(any(Integer.class), any(), any(Integer.class)))
                .thenReturn(new MetadataResult(null, null));

        prefixed.refreshOne(2L);

        MovieRequest saved = captureSaved();
        assertEquals(true, saved.getLocalFilePathAvailable());
        assertEquals(5L, saved.getLocalFileSize());
    }

    @Test
    void refreshOneRecordsLocalFileUnavailableWhenMissing(@TempDir Path tempDir) {
        var prefixed = new MovieRefreshService(
                repository,
                ombiClient,
                radarrClient,
                plexClient,
                plexCacheService,
                tempDir.toString(),
                jobRequestScheduler,
                ffprobeScanService);

        var existing = new MovieRequest("Some Movie", 500, false, null, "Common.ProcessingRequest");
        existing.setId(2L);
        Movie radarrMovie = radarrMovie(500, 77, "/movies/some-movie");
        radarrMovie.setMovieFile(movieFile("/movies/does-not-exist.mkv"));

        when(repository.findById(2L)).thenReturn(Optional.of(existing));
        when(radarrClient.getMoviesByTmdbId(500)).thenReturn(List.of(radarrMovie));
        when(radarrClient.getQualityProfilesById()).thenReturn(Map.of());
        when(plexClient.getMovieByTmdbId(any(Integer.class), any(), any(Integer.class)))
                .thenReturn(new MetadataResult(null, null));

        prefixed.refreshOne(2L);

        MovieRequest saved = captureSaved();
        assertEquals(false, saved.getLocalFilePathAvailable());
        assertNull(saved.getLocalFileSize());
    }

    @Test
    void refreshOneMarksLocalFileUnavailableWhenRadarrReportsNoFile() {
        var existing = new MovieRequest("Some Movie", 500, false, null, "Common.ProcessingRequest");
        existing.setId(2L);
        Movie radarrMovie = radarrMovie(500, 77, "/movies/some-movie"); // no movie file

        when(repository.findById(2L)).thenReturn(Optional.of(existing));
        when(radarrClient.getMoviesByTmdbId(500)).thenReturn(List.of(radarrMovie));
        when(radarrClient.getQualityProfilesById()).thenReturn(Map.of());
        when(plexClient.getMovieByTmdbId(any(Integer.class), any(), any(Integer.class)))
                .thenReturn(new MetadataResult(null, null));

        service.refreshOne(2L);

        MovieRequest saved = captureSaved();
        assertEquals(false, saved.getLocalFilePathAvailable());
        assertNull(saved.getLocalFileSize());
    }

    private MovieRequest captureSaved() {
        ArgumentCaptor<MovieRequest> captor = ArgumentCaptor.forClass(MovieRequest.class);
        verify(repository).save(captor.capture());
        return captor.getValue();
    }

    // --- fixtures ---

    private static Moviefile movieFile(String path) {
        var f = new Moviefile();
        f.setPath(path);
        return f;
    }

    private static OmbiMovieRequest ombiMovie(Integer id, String title, Integer tmdbId) {
        var m = new OmbiMovieRequest();
        m.setId(id);
        m.setTitle(title);
        m.setTheMovieDbId(tmdbId);
        m.setAvailable(false);
        m.setRequestStatus("Common.ProcessingRequest");
        return m;
    }

    private static Movie radarrMovie(Integer tmdbId, Integer radarrId, String path) {
        var m = new Movie();
        m.setTmdbId(tmdbId);
        m.setId(radarrId);
        m.setPath(path);
        m.setHasFile(false);
        m.setMonitored(true);
        m.setIsAvailable(false);
        m.setQualityProfileId(1);
        return m;
    }
}
