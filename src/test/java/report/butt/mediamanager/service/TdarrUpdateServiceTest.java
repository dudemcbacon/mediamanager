package report.butt.mediamanager.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import report.butt.mediamanager.model.MovieRequest;
import report.butt.mediamanager.model.TvChildRequest;
import report.butt.mediamanager.model.TvEpisodeRequest;
import report.butt.mediamanager.model.TvRequest;
import report.butt.mediamanager.model.TvSeasonRequest;
import report.butt.mediamanager.model.tdarr.TdarrTranscodeUpdate;
import report.butt.mediamanager.repository.MovieRequestRepository;
import report.butt.mediamanager.repository.TvEpisodeRequestRepository;

@NullMarked
class TdarrUpdateServiceTest {

    private static final String FULL_PATH = "/media/TV/Absolutely Fabulous/Season 3/S03E03.mkv";

    private final MovieRequestRepository movieRepository = mock(MovieRequestRepository.class);
    private final TvEpisodeRequestRepository episodeRepository = mock(TvEpisodeRequestRepository.class);
    private final TdarrUpdateService service = new TdarrUpdateService(movieRepository, episodeRepository);

    private static TdarrTranscodeUpdate update(@Nullable String filename) {
        var update = new TdarrTranscodeUpdate();
        update.setFilename(filename);
        update.setHealthCheck("Success");
        update.setTranscodeDecisionMaker("Transcode success");
        update.setOldSize(0.2834);
        update.setNewSize(0.1351);
        return update;
    }

    private static MovieRequest movie(Long id, String plexPath) {
        var movie = new MovieRequest("Title", 100, true, 1, "Common.Available");
        movie.setId(id);
        movie.setPlexMediaFilename(plexPath);
        return movie;
    }

    private static TvEpisodeRequest episode(Long id, String plexPath) {
        var parent = new TvRequest("Show", 100, true, 5000, "Common.Available");
        var child = new TvChildRequest(parent, "Show", 100, true, 6000, "Common.Available");
        var season = new TvSeasonRequest(child, 7000, 3, true);
        var episode = new TvEpisodeRequest(season, 8000, 3);
        episode.setId(id);
        episode.setPlexPath(plexPath);
        return episode;
    }

    private void noMatches() {
        when(movieRepository.findByPlexMediaFilenameLike(any())).thenReturn(List.of());
        when(episodeRepository.findByPlexPathLike(any())).thenReturn(List.of());
    }

    // --- resolution ---

    @Test
    void aBareFilenameBecomesASuffixPattern() {
        noMatches();

        service.apply(update("S03E03.mkv"));

        verify(movieRepository).findByPlexMediaFilenameLike("%/S03E03.mkv");
        verify(episodeRepository).findByPlexPathLike("%/S03E03.mkv");
    }

    /** A caller that supplies a precise path must not have it widened into "any file with this name". */
    @Test
    void aFullPathIsMatchedExactly() {
        noMatches();

        service.apply(update(FULL_PATH));

        verify(movieRepository).findByPlexMediaFilenameLike(FULL_PATH);
    }

    @Test
    void likeMetacharactersAreEscapedSoTheyMatchLiterally() {
        assertEquals("%/my!_movie !%20 (2024).mkv", TdarrUpdateService.likePattern("my_movie %20 (2024).mkv"));
        assertEquals("%/bang!!.mkv", TdarrUpdateService.likePattern("bang!.mkv"));
    }

    @Test
    void surroundingWhitespaceIsTrimmed() {
        noMatches();

        var result = service.apply(update("  S03E03.mkv  "));

        assertEquals("S03E03.mkv", result.filename());
        verify(episodeRepository).findByPlexPathLike("%/S03E03.mkv");
    }

    @Test
    void aMissingFilenameIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> service.apply(update(null)));
        assertThrows(IllegalArgumentException.class, () -> service.apply(update("   ")));
    }

    // --- applying values ---

    @Test
    void writesEveryTdarrFieldOntoAMatchedMovie() {
        MovieRequest movie = movie(42L, "/media/Movies/1992 (2024)/1992 (2024).mkv");
        when(movieRepository.findByPlexMediaFilenameLike(any())).thenReturn(List.of(movie));
        when(episodeRepository.findByPlexPathLike(any())).thenReturn(List.of());

        var result = service.apply(update("1992 (2024).mkv"));

        assertEquals("Success", movie.getTdarrHealthCheck());
        assertEquals("Transcode success", movie.getTdarrTranscodeDecisionMaker());
        assertEquals(0.2834, movie.getTdarrOldSizeGb());
        assertEquals(0.1351, movie.getTdarrNewSizeGb());
        assertEquals(1, result.movies());
        assertEquals(0, result.episodes());
        assertEquals(1, result.matched());
        assertNotNull(movie.getTdarrLastUpdated());
        verify(movieRepository).saveAll(List.of(movie));
    }

    @Test
    void writesEveryTdarrFieldOntoAMatchedEpisode() {
        TvEpisodeRequest episode = episode(7L, FULL_PATH);
        when(movieRepository.findByPlexMediaFilenameLike(any())).thenReturn(List.of());
        when(episodeRepository.findByPlexPathLike(any())).thenReturn(List.of(episode));

        var result = service.apply(update("S03E03.mkv"));

        assertEquals("Success", episode.getTdarrHealthCheck());
        assertEquals("Transcode success", episode.getTdarrTranscodeDecisionMaker());
        assertEquals(0.2834, episode.getTdarrOldSizeGb());
        assertEquals(0.1351, episode.getTdarrNewSizeGb());
        assertEquals(0, result.movies());
        assertEquals(1, result.episodes());
        assertNotNull(episode.getTdarrLastUpdated());
        verify(episodeRepository).saveAll(List.of(episode));
    }

    /** The chosen behaviour for an ambiguous bare filename: update them all rather than refusing. */
    @Test
    void everyMatchIsUpdatedWhenAFilenameIsAmbiguous() {
        TvEpisodeRequest showA = episode(1L, "/media/TV/Show A/Season 1/S01E01.mkv");
        TvEpisodeRequest showB = episode(2L, "/media/TV/Show B/Season 1/S01E01.mkv");
        when(movieRepository.findByPlexMediaFilenameLike(any())).thenReturn(List.of());
        when(episodeRepository.findByPlexPathLike(any())).thenReturn(List.of(showA, showB));

        var result = service.apply(update("S01E01.mkv"));

        assertEquals(2, result.episodes());
        assertEquals(2, result.matched());
        assertEquals("Transcode success", showA.getTdarrTranscodeDecisionMaker());
        assertEquals("Transcode success", showB.getTdarrTranscodeDecisionMaker());
        assertEquals(2, result.updated().size());
        assertTrue(result.updated().get(0).contains("Show A"), result.updated().toString());
    }

    @Test
    void aFilenameMatchingBothAMovieAndAnEpisodeUpdatesBoth() {
        when(movieRepository.findByPlexMediaFilenameLike(any())).thenReturn(List.of(movie(42L, "/media/Movies/x.mkv")));
        when(episodeRepository.findByPlexPathLike(any())).thenReturn(List.of(episode(7L, "/media/TV/Show/x.mkv")));

        var result = service.apply(update("x.mkv"));

        assertEquals(1, result.movies());
        assertEquals(1, result.episodes());
        assertEquals(2, result.matched());
    }

    /** A partial payload must not blank out values a refresh already recorded. */
    @Test
    void nullFieldsLeaveStoredValuesAlone() {
        MovieRequest movie = movie(42L, "/media/Movies/x.mkv");
        movie.setTdarrHealthCheck("Success");
        movie.setTdarrOldSizeGb(9.9);
        when(movieRepository.findByPlexMediaFilenameLike(any())).thenReturn(List.of(movie));
        when(episodeRepository.findByPlexPathLike(any())).thenReturn(List.of());

        var sparse = new TdarrTranscodeUpdate();
        sparse.setFilename("x.mkv");
        sparse.setTranscodeDecisionMaker("Not required");
        service.apply(sparse);

        assertEquals("Not required", movie.getTdarrTranscodeDecisionMaker());
        assertEquals("Success", movie.getTdarrHealthCheck(), "health check should be untouched");
        assertEquals(9.9, movie.getTdarrOldSizeGb(), "old size should be untouched");
        assertNull(movie.getTdarrNewSizeGb());
        assertNotNull(movie.getTdarrLastUpdated(), "a matched row is stamped even for a partial payload");
    }

    /** Every row touched by one call shares a single timestamp rather than drifting microseconds apart. */
    @Test
    void allRowsInOneCallShareTheSameTimestamp() {
        MovieRequest movie = movie(42L, "/media/Movies/x.mkv");
        TvEpisodeRequest episode = episode(7L, "/media/TV/Show/x.mkv");
        when(movieRepository.findByPlexMediaFilenameLike(any())).thenReturn(List.of(movie));
        when(episodeRepository.findByPlexPathLike(any())).thenReturn(List.of(episode));

        service.apply(update("x.mkv"));

        assertEquals(movie.getTdarrLastUpdated(), episode.getTdarrLastUpdated());
    }

    @Test
    void nothingIsStampedWhenNothingMatches() {
        MovieRequest untouched = movie(42L, "/media/Movies/other.mkv");
        noMatches();

        service.apply(update("nope.mkv"));

        assertNull(untouched.getTdarrLastUpdated());
    }

    @Test
    void anUnknownFilenameMatchesNothing() {
        noMatches();

        var result = service.apply(update("nope.mkv"));

        assertEquals(0, result.matched());
        assertTrue(result.updated().isEmpty());
    }

    @Test
    void updatedDescriptorsNameTheTypeIdAndPath() {
        when(movieRepository.findByPlexMediaFilenameLike(any())).thenReturn(List.of(movie(42L, "/media/Movies/x.mkv")));
        when(episodeRepository.findByPlexPathLike(any())).thenReturn(List.of(episode(7L, "/media/TV/Show/x.mkv")));

        var result = service.apply(update("x.mkv"));

        assertEquals(List.of("MOVIE 42 /media/Movies/x.mkv", "EPISODE 7 /media/TV/Show/x.mkv"), result.updated());
    }

    @Test
    void bothRepositoriesAreSavedEvenWhenOneHasNoMatches() {
        noMatches();

        service.apply(update("nope.mkv"));

        ArgumentCaptor<List<MovieRequest>> movies = ArgumentCaptor.captor();
        verify(movieRepository).saveAll(movies.capture());
        assertTrue(movies.getValue().isEmpty());
    }
}
