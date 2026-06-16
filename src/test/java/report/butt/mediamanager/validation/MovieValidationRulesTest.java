package report.butt.mediamanager.validation;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.Test;
import report.butt.mediamanager.model.MovieRequest;

/**
 * Unit tests for all movie ValidationRule implementations. Each rule is exercised for its passing path, failing path,
 * and edge branches.
 */
class MovieValidationRulesTest {

    // --- AvailableInOmbi ---

    @Test
    void availableInOmbi_passesWhenOmbiRequestIdIsSet() {
        MovieRequest m = movie();
        m.setOmbiRequestId(42);
        assertTrue(new AvailableInOmbi().validate(m));
    }

    @Test
    void availableInOmbi_failsWhenOmbiRequestIdIsNull() {
        MovieRequest m = movie();
        m.setOmbiRequestId(null);
        assertFalse(new AvailableInOmbi().validate(m));
    }

    // --- AvailableInPlex ---

    @Test
    void availableInPlex_passesWhenPlexMediaIdIsSet() {
        MovieRequest m = movie();
        m.setPlexMediaId(99);
        assertTrue(new AvailableInPlex().validate(m));
    }

    @Test
    void availableInPlex_failsWhenPlexMediaIdIsNull() {
        MovieRequest m = movie();
        m.setPlexMediaId(null);
        assertFalse(new AvailableInPlex().validate(m));
    }

    // --- AvailableInRadarr ---

    @Test
    void availableInRadarr_passesWhenRadarrRequestIdIsSet() {
        MovieRequest m = movie();
        m.setRadarrRequestId(7);
        assertTrue(new AvailableInRadarr().validate(m));
    }

    @Test
    void availableInRadarr_failsWhenRadarrRequestIdIsNull() {
        MovieRequest m = movie();
        m.setRadarrRequestId(null);
        assertFalse(new AvailableInRadarr().validate(m));
    }

    // --- RadarrHasFile ---

    @Test
    void radarrHasFile_passesWhenTrue() {
        MovieRequest m = movie();
        m.setRadarrHasFile(true);
        assertTrue(new RadarrHasFile().validate(m));
    }

    @Test
    void radarrHasFile_failsWhenFalse() {
        MovieRequest m = movie();
        m.setRadarrHasFile(false);
        assertFalse(new RadarrHasFile().validate(m));
    }

    @Test
    void radarrHasFile_failsWhenNull() {
        MovieRequest m = movie();
        m.setRadarrHasFile(null);
        assertFalse(new RadarrHasFile().validate(m));
    }

    // --- EnglishOrAvailable ---

    @Test
    void englishOrAvailable_passesWhenLanguageIsNull() {
        MovieRequest m = movie();
        m.setRadarrOriginalLanguage(null);
        assertTrue(new EnglishOrAvailable().validate(m));
    }

    @Test
    void englishOrAvailable_passesWhenLanguageIsEnglish() {
        MovieRequest m = movie();
        m.setRadarrOriginalLanguage("English");
        assertTrue(new EnglishOrAvailable().validate(m));
    }

    @Test
    void englishOrAvailable_passesWhenLanguageIsEnglishCaseInsensitive() {
        MovieRequest m = movie();
        m.setRadarrOriginalLanguage("ENGLISH");
        assertTrue(new EnglishOrAvailable().validate(m));
    }

    @Test
    void englishOrAvailable_passesWhenNonEnglishAndAvailable() {
        // isAvailable() == radarrHasFile && ombiRequestStatus == Common.Available
        MovieRequest m = new MovieRequest("Title", 1, true, 1, "Common.Available");
        m.setRadarrHasFile(true);
        m.setRadarrOriginalLanguage("French");
        assertTrue(new EnglishOrAvailable().validate(m));
    }

    @Test
    void englishOrAvailable_failsWhenNonEnglishAndNotAvailable() {
        MovieRequest m = movie();
        m.setRadarrOriginalLanguage("French");
        m.setRadarrHasFile(false);
        assertFalse(new EnglishOrAvailable().validate(m));
    }

    // --- PathsMatch ---

    @Test
    void pathsMatch_passesWhenBothPathsEqual() {
        MovieRequest m = movie();
        m.setPlexMediaFilename("/movies/film.mkv");
        m.setRadarrMovieFilePath("/movies/film.mkv");
        assertTrue(new PathsMatch().validate(m));
    }

    @Test
    void pathsMatch_passesWhenMntPrefixDiffers() {
        MovieRequest m = movie();
        m.setPlexMediaFilename("/mnt/movies/film.mkv");
        m.setRadarrMovieFilePath("/movies/film.mkv");
        assertTrue(new PathsMatch().validate(m));
    }

    @Test
    void pathsMatch_passesWhenBothNull() {
        MovieRequest m = movie();
        m.setPlexMediaFilename(null);
        m.setRadarrMovieFilePath(null);
        assertTrue(new PathsMatch().validate(m));
    }

    @Test
    void pathsMatch_failsWhenPathsDiffer() {
        MovieRequest m = movie();
        m.setPlexMediaFilename("/movies/film.mkv");
        m.setRadarrMovieFilePath("/movies/other.mkv");
        assertFalse(new PathsMatch().validate(m));
    }

    @Test
    void pathsMatch_failsWhenOnlyOneIsNull() {
        MovieRequest m = movie();
        m.setPlexMediaFilename("/movies/film.mkv");
        m.setRadarrMovieFilePath(null);
        assertFalse(new PathsMatch().validate(m));
    }

    // --- TmdbIdsMatch ---

    @Test
    void tmdbIdsMatch_passesWhenBothIdsEqual() {
        MovieRequest m = movie();
        m.setTmdbid(100);
        m.setPlexTmdbid(100);
        assertTrue(new TmdbIdsMatch().validate(m));
    }

    @Test
    void tmdbIdsMatch_passesWhenBothNull() {
        MovieRequest m = movie();
        m.setTmdbid(null);
        m.setPlexTmdbid(null);
        assertTrue(new TmdbIdsMatch().validate(m));
    }

    @Test
    void tmdbIdsMatch_failsWhenIdsDiffer() {
        MovieRequest m = movie();
        m.setTmdbid(100);
        m.setPlexTmdbid(200);
        assertFalse(new TmdbIdsMatch().validate(m));
    }

    @Test
    void tmdbIdsMatch_failsWhenOneIsNull() {
        MovieRequest m = movie();
        m.setTmdbid(100);
        m.setPlexTmdbid(null);
        assertFalse(new TmdbIdsMatch().validate(m));
    }

    // --- OmbiRadarrAlignment ---

    @Test
    void ombiRadarrAlignment_passesWhenFileAndAvailableStatus() {
        MovieRequest m = movie();
        m.setRadarrHasFile(true);
        m.setOmbiRequestStatus("Common.Available");
        assertTrue(new OmbiRadarrAlignment().validate(m));
    }

    @Test
    void ombiRadarrAlignment_failsWhenNoFile() {
        MovieRequest m = movie();
        m.setRadarrHasFile(false);
        m.setOmbiRequestStatus("Common.Available");
        assertFalse(new OmbiRadarrAlignment().validate(m));
    }

    @Test
    void ombiRadarrAlignment_failsWhenFileNullAndStatusAvailable() {
        MovieRequest m = movie();
        m.setRadarrHasFile(null);
        m.setOmbiRequestStatus("Common.Available");
        assertFalse(new OmbiRadarrAlignment().validate(m));
    }

    @Test
    void ombiRadarrAlignment_failsWhenFileButWrongStatus() {
        MovieRequest m = movie();
        m.setRadarrHasFile(true);
        m.setOmbiRequestStatus("Common.ProcessingRequest");
        assertFalse(new OmbiRadarrAlignment().validate(m));
    }

    // --- QualityProfileAnyOrAvailable ---

    @Test
    void qualityProfileAnyOrAvailable_passesWhenAvailable() {
        // isAvailable() = radarrHasFile==true AND ombiRequestStatus==Common.Available
        MovieRequest m = new MovieRequest("Title", 1, true, 1, "Common.Available");
        m.setRadarrHasFile(true);
        m.setRadarrQualityProfile("HD-1080p");
        assertTrue(new QualityProfileAnyOrAvailable().validate(m));
    }

    @Test
    void qualityProfileAnyOrAvailable_passesWhenProfileIsAny() {
        MovieRequest m = movie();
        m.setRadarrQualityProfile("Any");
        assertFalse(m.isAvailable()); // confirm not available
        assertTrue(new QualityProfileAnyOrAvailable().validate(m));
    }

    @Test
    void qualityProfileAnyOrAvailable_passesWhenProfileIsAnyCaseInsensitive() {
        MovieRequest m = movie();
        m.setRadarrQualityProfile("ANY");
        assertTrue(new QualityProfileAnyOrAvailable().validate(m));
    }

    @Test
    void qualityProfileAnyOrAvailable_failsWhenNotAvailableAndProfileNotAny() {
        MovieRequest m = movie();
        m.setRadarrQualityProfile("HD-1080p");
        assertFalse(new QualityProfileAnyOrAvailable().validate(m));
    }

    @Test
    void qualityProfileAnyOrAvailable_failsWhenProfileIsNull() {
        MovieRequest m = movie();
        m.setRadarrQualityProfile(null);
        assertFalse(new QualityProfileAnyOrAvailable().validate(m));
    }

    // --- SearchedRecently ---

    @Test
    void searchedRecently_passesWhenAvailable() {
        MovieRequest m = new MovieRequest("Title", 1, true, 1, "Common.Available");
        m.setRadarrHasFile(true);
        assertTrue(new SearchedRecently().validate(m));
    }

    @Test
    void searchedRecently_passesWhenSearchedWithinLastWeek() {
        MovieRequest m = movie();
        m.setRadarrLastSearchTime(Instant.now().minus(3, ChronoUnit.DAYS));
        assertTrue(new SearchedRecently().validate(m));
    }

    @Test
    void searchedRecently_failsWhenNotAvailableAndNeverSearched() {
        MovieRequest m = movie();
        m.setRadarrLastSearchTime(null);
        assertFalse(new SearchedRecently().validate(m));
    }

    @Test
    void searchedRecently_failsWhenSearchedMoreThanWeekAgo() {
        MovieRequest m = movie();
        m.setRadarrLastSearchTime(Instant.now().minus(8, ChronoUnit.DAYS));
        assertFalse(new SearchedRecently().validate(m));
    }

    // --- NotInTvFolder ---

    @Test
    void notInTvFolder_passesWhenPathIsNull() {
        MovieRequest m = movie();
        m.setRadarrPath(null);
        assertTrue(new NotInTvFolder().validate(m));
    }

    @Test
    void notInTvFolder_passesWhenPathDoesNotContainTvFolder() {
        MovieRequest m = movie();
        m.setRadarrPath("/movies/Action/Film (2024)");
        assertTrue(new NotInTvFolder().validate(m));
    }

    @Test
    void notInTvFolder_failsWhenPathContainsTvFolder() {
        MovieRequest m = movie();
        m.setRadarrPath("/data/TV/SomeShow");
        assertFalse(new NotInTvFolder().validate(m));
    }

    // --- LocalFileMatchesPlex ---

    @Test
    void localFileMatchesPlex_passesWhenAvailableAndSizesMatch() {
        MovieRequest m = movie();
        m.setLocalFilePathAvailable(true);
        m.setLocalFileSize(1_000_000L);
        m.setPlexMediaSize(1_000_000L);
        assertTrue(new LocalFileMatchesPlex().validate(m));
    }

    @Test
    void localFileMatchesPlex_failsWhenFileNotAvailable() {
        MovieRequest m = movie();
        m.setLocalFilePathAvailable(false);
        m.setLocalFileSize(1_000_000L);
        m.setPlexMediaSize(1_000_000L);
        assertFalse(new LocalFileMatchesPlex().validate(m));
    }

    @Test
    void localFileMatchesPlex_failsWhenAvailabilityIsNull() {
        MovieRequest m = movie();
        m.setLocalFilePathAvailable(null);
        m.setLocalFileSize(1_000_000L);
        m.setPlexMediaSize(1_000_000L);
        assertFalse(new LocalFileMatchesPlex().validate(m));
    }

    @Test
    void localFileMatchesPlex_failsWhenSizesDiffer() {
        MovieRequest m = movie();
        m.setLocalFilePathAvailable(true);
        m.setLocalFileSize(1_000_000L);
        m.setPlexMediaSize(999_999L);
        assertFalse(new LocalFileMatchesPlex().validate(m));
    }

    @Test
    void localFileMatchesPlex_failsWhenLocalSizeIsNull() {
        MovieRequest m = movie();
        m.setLocalFilePathAvailable(true);
        m.setLocalFileSize(null);
        m.setPlexMediaSize(1_000_000L);
        assertFalse(new LocalFileMatchesPlex().validate(m));
    }

    @Test
    void localFileMatchesPlex_failsWhenPlexSizeIsNull() {
        MovieRequest m = movie();
        m.setLocalFilePathAvailable(true);
        m.setLocalFileSize(1_000_000L);
        m.setPlexMediaSize(null);
        assertFalse(new LocalFileMatchesPlex().validate(m));
    }

    // --- ValidationRule metadata ---

    @Test
    void availableInOmbi_metadataIsConsistent() {
        AvailableInOmbi rule = new AvailableInOmbi();
        assertTrue(rule.sortOrder() > 0);
        assertTrue(rule.shortName() != null && !rule.shortName().isBlank());
        assertTrue(rule.title() != null && !rule.title().isBlank());
        assertTrue(rule.description() != null && !rule.description().isBlank());
    }

    // --- helpers ---

    /** A plain MovieRequest with no fields set that trigger isAvailable(). */
    private static MovieRequest movie() {
        return new MovieRequest("Test Movie", 1, false, 1, "Common.ProcessingRequest");
    }
}
