package report.butt.mediamanager.validation;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.Test;
import report.butt.mediamanager.model.TvEpisodeRequest;
import report.butt.mediamanager.model.TvSeasonRequest;

/** Unit tests for EpisodeValidator implementations: EpisodePathsMatch and EpisodeSearchedRecently. */
class EpisodeValidationRulesTest {

    // --- EpisodePathsMatch ---

    @Test
    void episodePathsMatch_passesWhenPathsEqual() {
        TvEpisodeRequest ep = episode();
        ep.setSonarrPath("/tv/Show/S01E01.mkv");
        ep.setPlexPath("/tv/Show/S01E01.mkv");
        assertTrue(new EpisodePathsMatch().validate(ep));
    }

    @Test
    void episodePathsMatch_passesWhenMntPrefixDiffers() {
        TvEpisodeRequest ep = episode();
        ep.setSonarrPath("/tv/Show/S01E01.mkv");
        ep.setPlexPath("/mnt/tv/Show/S01E01.mkv");
        assertTrue(new EpisodePathsMatch().validate(ep));
    }

    @Test
    void episodePathsMatch_passesWhenBothNull() {
        TvEpisodeRequest ep = episode();
        ep.setSonarrPath(null);
        ep.setPlexPath(null);
        assertTrue(new EpisodePathsMatch().validate(ep));
    }

    @Test
    void episodePathsMatch_failsWhenPathsDiffer() {
        TvEpisodeRequest ep = episode();
        ep.setSonarrPath("/tv/Show/S01E01.mkv");
        ep.setPlexPath("/tv/Show/S01E02.mkv");
        assertFalse(new EpisodePathsMatch().validate(ep));
    }

    @Test
    void episodePathsMatch_failsWhenOnlyOneIsNull() {
        TvEpisodeRequest ep = episode();
        ep.setSonarrPath("/tv/Show/S01E01.mkv");
        ep.setPlexPath(null);
        assertFalse(new EpisodePathsMatch().validate(ep));
    }

    // --- EpisodeSearchedRecently ---

    @Test
    void episodeSearchedRecently_passesWhenOmbiAvailableTrue() {
        TvEpisodeRequest ep = episode();
        ep.setOmbiAvailable(true);
        ep.setSonarrLastSearchTime(null);
        assertTrue(new EpisodeSearchedRecently().validate(ep));
    }

    @Test
    void episodeSearchedRecently_passesWhenSearchedWithinLastWeek() {
        TvEpisodeRequest ep = episode();
        ep.setOmbiAvailable(false);
        ep.setSonarrLastSearchTime(Instant.now().minus(3, ChronoUnit.DAYS));
        assertTrue(new EpisodeSearchedRecently().validate(ep));
    }

    @Test
    void episodeSearchedRecently_failsWhenNotAvailableAndNeverSearched() {
        TvEpisodeRequest ep = episode();
        ep.setOmbiAvailable(false);
        ep.setSonarrLastSearchTime(null);
        assertFalse(new EpisodeSearchedRecently().validate(ep));
    }

    @Test
    void episodeSearchedRecently_failsWhenOmbiAvailableNullAndNeverSearched() {
        TvEpisodeRequest ep = episode();
        ep.setOmbiAvailable(null);
        ep.setSonarrLastSearchTime(null);
        assertFalse(new EpisodeSearchedRecently().validate(ep));
    }

    @Test
    void episodeSearchedRecently_failsWhenSearchedMoreThanWeekAgo() {
        TvEpisodeRequest ep = episode();
        ep.setOmbiAvailable(false);
        ep.setSonarrLastSearchTime(Instant.now().minus(9, ChronoUnit.DAYS));
        assertFalse(new EpisodeSearchedRecently().validate(ep));
    }

    // --- EpisodeLocalFileMatchesPlex ---

    @Test
    void episodeLocalFileMatchesPlex_passesWhenAvailableAndSizesMatch() {
        TvEpisodeRequest ep = episode();
        ep.setLocalFilePathAvailable(true);
        ep.setLocalFileSize(1_000_000L);
        ep.setPlexMediaSize(1_000_000L);
        assertTrue(new EpisodeLocalFileMatchesPlex().validate(ep));
    }

    @Test
    void episodeLocalFileMatchesPlex_failsWhenFileNotAvailable() {
        TvEpisodeRequest ep = episode();
        ep.setLocalFilePathAvailable(false);
        ep.setLocalFileSize(1_000_000L);
        ep.setPlexMediaSize(1_000_000L);
        assertFalse(new EpisodeLocalFileMatchesPlex().validate(ep));
    }

    @Test
    void episodeLocalFileMatchesPlex_failsWhenAvailabilityNull() {
        TvEpisodeRequest ep = episode();
        ep.setLocalFilePathAvailable(null);
        ep.setLocalFileSize(1_000_000L);
        ep.setPlexMediaSize(1_000_000L);
        assertFalse(new EpisodeLocalFileMatchesPlex().validate(ep));
    }

    @Test
    void episodeLocalFileMatchesPlex_failsWhenSizesDiffer() {
        TvEpisodeRequest ep = episode();
        ep.setLocalFilePathAvailable(true);
        ep.setLocalFileSize(1_000_000L);
        ep.setPlexMediaSize(999_999L);
        assertFalse(new EpisodeLocalFileMatchesPlex().validate(ep));
    }

    @Test
    void episodeLocalFileMatchesPlex_failsWhenLocalSizeNull() {
        TvEpisodeRequest ep = episode();
        ep.setLocalFilePathAvailable(true);
        ep.setLocalFileSize(null);
        ep.setPlexMediaSize(1_000_000L);
        assertFalse(new EpisodeLocalFileMatchesPlex().validate(ep));
    }

    @Test
    void episodeLocalFileMatchesPlex_failsWhenPlexSizeNull() {
        TvEpisodeRequest ep = episode();
        ep.setLocalFilePathAvailable(true);
        ep.setLocalFileSize(1_000_000L);
        ep.setPlexMediaSize(null);
        assertFalse(new EpisodeLocalFileMatchesPlex().validate(ep));
    }

    // --- helpers ---

    private static TvEpisodeRequest episode() {
        TvSeasonRequest season = mock(TvSeasonRequest.class);
        return new TvEpisodeRequest(season, 1, 1);
    }
}
