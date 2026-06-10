package report.butt.mediamanager.validation;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import report.butt.mediamanager.model.TvRequest;
import report.butt.mediamanager.repository.TvSeasonRequestRepository;

/** Unit tests for all TV ValidationRule implementations. */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class TvValidationRulesTest {

    // --- AvailableInOmbiTv ---

    @Test
    void availableInOmbiTv_passesWhenOmbiRequestIdIsSet() {
        TvRequest t = tv();
        t.setOmbiRequestId(5);
        assertTrue(new AvailableInOmbiTv().validate(t));
    }

    @Test
    void availableInOmbiTv_failsWhenOmbiRequestIdIsNull() {
        TvRequest t = tv();
        t.setOmbiRequestId(null);
        assertFalse(new AvailableInOmbiTv().validate(t));
    }

    // --- AvailableInSonarr ---

    @Test
    void availableInSonarr_passesWhenSonarrSeriesIdIsSet() {
        TvRequest t = tv();
        t.setSonarrSeriesId(10);
        assertTrue(new AvailableInSonarr().validate(t));
    }

    @Test
    void availableInSonarr_failsWhenSonarrSeriesIdIsNull() {
        TvRequest t = tv();
        t.setSonarrSeriesId(null);
        assertFalse(new AvailableInSonarr().validate(t));
    }

    // --- AvailableInPlexTv ---

    @Test
    void availableInPlexTv_passesWhenTvdbIdsMatch() {
        TvRequest t = tv();
        t.setTvdbId(123);
        t.setPlexTvdbId(123);
        assertTrue(new AvailableInPlexTv().validate(t));
    }

    @Test
    void availableInPlexTv_failsWhenTvdbIdIsNull() {
        TvRequest t = tv();
        t.setTvdbId(null);
        t.setPlexTvdbId(123);
        assertFalse(new AvailableInPlexTv().validate(t));
    }

    @Test
    void availableInPlexTv_failsWhenIdsDiffer() {
        TvRequest t = tv();
        t.setTvdbId(123);
        t.setPlexTvdbId(456);
        assertFalse(new AvailableInPlexTv().validate(t));
    }

    @Test
    void availableInPlexTv_failsWhenPlexTvdbIdIsNull() {
        TvRequest t = tv();
        t.setTvdbId(123);
        t.setPlexTvdbId(null);
        assertFalse(new AvailableInPlexTv().validate(t));
    }

    // --- TvdbIdsMatch ---

    @Test
    void tvdbIdsMatch_passesWhenBothEqual() {
        TvRequest t = tv();
        t.setTvdbId(77);
        t.setPlexTvdbId(77);
        assertTrue(new TvdbIdsMatch().validate(t));
    }

    @Test
    void tvdbIdsMatch_passesWhenBothNull() {
        TvRequest t = tv();
        t.setTvdbId(null);
        t.setPlexTvdbId(null);
        assertTrue(new TvdbIdsMatch().validate(t));
    }

    @Test
    void tvdbIdsMatch_failsWhenIdsDiffer() {
        TvRequest t = tv();
        t.setTvdbId(77);
        t.setPlexTvdbId(88);
        assertFalse(new TvdbIdsMatch().validate(t));
    }

    @Test
    void tvdbIdsMatch_failsWhenOneIsNull() {
        TvRequest t = tv();
        t.setTvdbId(77);
        t.setPlexTvdbId(null);
        assertFalse(new TvdbIdsMatch().validate(t));
    }

    // --- EnglishOrAvailableTv ---

    @Test
    void englishOrAvailableTv_passesWhenLanguageIsNull() {
        TvRequest t = tv();
        t.setSonarrOriginalLanguage(null);
        assertTrue(new EnglishOrAvailableTv().validate(t));
    }

    @Test
    void englishOrAvailableTv_passesWhenEnglish() {
        TvRequest t = tv();
        t.setSonarrOriginalLanguage("English");
        assertTrue(new EnglishOrAvailableTv().validate(t));
    }

    @Test
    void englishOrAvailableTv_passesWhenEnglishCaseInsensitive() {
        TvRequest t = tv();
        t.setSonarrOriginalLanguage("english");
        assertTrue(new EnglishOrAvailableTv().validate(t));
    }

    @Test
    void englishOrAvailableTv_passesWhenNonEnglishAndAvailable() {
        // isAvailable() for TvRequest: fileCount >= episodeCount > 0 AND status==Common.Available
        TvRequest t = new TvRequest("Show", 1, true, 1, "Common.Available");
        t.setSonarrEpisodeFileCount(5);
        t.setSonarrEpisodeCount(5);
        t.setSonarrOriginalLanguage("Korean");
        assertTrue(new EnglishOrAvailableTv().validate(t));
    }

    @Test
    void englishOrAvailableTv_failsWhenNonEnglishAndNotAvailable() {
        TvRequest t = tv();
        t.setSonarrOriginalLanguage("Korean");
        assertFalse(new EnglishOrAvailableTv().validate(t));
    }

    // --- SonarrMonitored ---

    @Test
    void sonarrMonitored_passesWhenMonitoredAndAll() {
        TvRequest t = tv();
        t.setSonarrMonitored(true);
        t.setSonarrMonitoredAll("all");
        assertTrue(new SonarrMonitored().validate(t));
    }

    @Test
    void sonarrMonitored_failsWhenNotMonitored() {
        TvRequest t = tv();
        t.setSonarrMonitored(false);
        t.setSonarrMonitoredAll("all");
        assertFalse(new SonarrMonitored().validate(t));
    }

    @Test
    void sonarrMonitored_failsWhenMonitoredNullAndAll() {
        TvRequest t = tv();
        t.setSonarrMonitored(null);
        t.setSonarrMonitoredAll("all");
        assertFalse(new SonarrMonitored().validate(t));
    }

    @Test
    void sonarrMonitored_failsWhenMonitoredButNotAll() {
        TvRequest t = tv();
        t.setSonarrMonitored(true);
        t.setSonarrMonitoredAll("future");
        assertFalse(new SonarrMonitored().validate(t));
    }

    @Test
    void sonarrMonitored_failsWhenMonitoredAllIsNull() {
        TvRequest t = tv();
        t.setSonarrMonitored(true);
        t.setSonarrMonitoredAll(null);
        assertFalse(new SonarrMonitored().validate(t));
    }

    // --- SearchedRecentlyTv ---

    @Test
    void searchedRecentlyTv_passesWhenAvailable() {
        TvRequest t = new TvRequest("Show", 1, true, 1, "Common.Available");
        t.setSonarrEpisodeFileCount(3);
        t.setSonarrEpisodeCount(3);
        assertTrue(new SearchedRecentlyTv().validate(t));
    }

    @Test
    void searchedRecentlyTv_passesWhenSearchedWithinLastWeek() {
        TvRequest t = tv();
        t.setSonarrLastSearched(Instant.now().minus(2, ChronoUnit.DAYS));
        assertTrue(new SearchedRecentlyTv().validate(t));
    }

    @Test
    void searchedRecentlyTv_failsWhenNotAvailableAndNeverSearched() {
        TvRequest t = tv();
        t.setSonarrLastSearched(null);
        assertFalse(new SearchedRecentlyTv().validate(t));
    }

    @Test
    void searchedRecentlyTv_failsWhenSearchedMoreThanWeekAgo() {
        TvRequest t = tv();
        t.setSonarrLastSearched(Instant.now().minus(10, ChronoUnit.DAYS));
        assertFalse(new SearchedRecentlyTv().validate(t));
    }

    // --- OmbiSonarrAlignment ---

    @Test
    void ombiSonarrAlignment_passesWhenFilesGeEpisodesAndStatusAvailable() {
        TvRequest t = tv();
        t.setSonarrEpisodeFileCount(10);
        t.setSonarrEpisodeCount(10);
        t.setOmbiRequestStatus("Common.Available");
        assertTrue(new OmbiSonarrAlignment().validate(t));
    }

    @Test
    void ombiSonarrAlignment_passesWhenMoreFilesThanEpisodesAndStatusAvailable() {
        TvRequest t = tv();
        t.setSonarrEpisodeFileCount(12);
        t.setSonarrEpisodeCount(10);
        t.setOmbiRequestStatus("Common.Available");
        assertTrue(new OmbiSonarrAlignment().validate(t));
    }

    @Test
    void ombiSonarrAlignment_failsWhenFileCountIsNull() {
        TvRequest t = tv();
        t.setSonarrEpisodeFileCount(null);
        t.setSonarrEpisodeCount(5);
        t.setOmbiRequestStatus("Common.Available");
        assertFalse(new OmbiSonarrAlignment().validate(t));
    }

    @Test
    void ombiSonarrAlignment_failsWhenEpisodeCountIsNull() {
        TvRequest t = tv();
        t.setSonarrEpisodeFileCount(5);
        t.setSonarrEpisodeCount(null);
        t.setOmbiRequestStatus("Common.Available");
        assertFalse(new OmbiSonarrAlignment().validate(t));
    }

    @Test
    void ombiSonarrAlignment_failsWhenEpisodeCountIsZero() {
        TvRequest t = tv();
        t.setSonarrEpisodeFileCount(0);
        t.setSonarrEpisodeCount(0);
        t.setOmbiRequestStatus("Common.Available");
        assertFalse(new OmbiSonarrAlignment().validate(t));
    }

    @Test
    void ombiSonarrAlignment_failsWhenFilesLessThanEpisodes() {
        TvRequest t = tv();
        t.setSonarrEpisodeFileCount(3);
        t.setSonarrEpisodeCount(10);
        t.setOmbiRequestStatus("Common.Available");
        assertFalse(new OmbiSonarrAlignment().validate(t));
    }

    @Test
    void ombiSonarrAlignment_failsWhenStatusIsNotAvailable() {
        TvRequest t = tv();
        t.setSonarrEpisodeFileCount(10);
        t.setSonarrEpisodeCount(10);
        t.setOmbiRequestStatus("Common.ProcessingRequest");
        assertFalse(new OmbiSonarrAlignment().validate(t));
    }

    // --- OmbiSeasonCountAlignment ---

    @Test
    void ombiSeasonCountAlignment_passesWhenExpectedLteActual() {
        TvSeasonRequestRepository repo = mock(TvSeasonRequestRepository.class);
        TvRequest t = tv();
        t.setOmbiTotalSeasons(2);
        when(repo.countByTvChildRequestParent(t)).thenReturn(3L);

        assertTrue(new OmbiSeasonCountAlignment(repo).validate(t));
    }

    @Test
    void ombiSeasonCountAlignment_passesWhenExpectedEqualsActual() {
        TvSeasonRequestRepository repo = mock(TvSeasonRequestRepository.class);
        TvRequest t = tv();
        t.setOmbiTotalSeasons(3);
        when(repo.countByTvChildRequestParent(t)).thenReturn(3L);

        assertTrue(new OmbiSeasonCountAlignment(repo).validate(t));
    }

    @Test
    void ombiSeasonCountAlignment_failsWhenExpectedIsNull() {
        TvSeasonRequestRepository repo = mock(TvSeasonRequestRepository.class);
        TvRequest t = tv();
        t.setOmbiTotalSeasons(null);

        assertFalse(new OmbiSeasonCountAlignment(repo).validate(t));
    }

    @Test
    void ombiSeasonCountAlignment_failsWhenExpectedGtActual() {
        TvSeasonRequestRepository repo = mock(TvSeasonRequestRepository.class);
        TvRequest t = tv();
        t.setOmbiTotalSeasons(5);
        when(repo.countByTvChildRequestParent(t)).thenReturn(2L);

        assertFalse(new OmbiSeasonCountAlignment(repo).validate(t));
    }

    // --- helpers ---

    private static TvRequest tv() {
        return new TvRequest("Test Show", 1, false, 1, "Common.ProcessingRequest");
    }
}
