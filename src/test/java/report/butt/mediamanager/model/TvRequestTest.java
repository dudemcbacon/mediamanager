package report.butt.mediamanager.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import org.junit.jupiter.api.Test;

class TvRequestTest {

    private static TvRequest available() {
        var tv = new TvRequest("Breaking Bad", 81189, false, 1, "Common.Available");
        tv.setSonarrEpisodeFileCount(62);
        tv.setSonarrEpisodeCount(62);
        return tv;
    }

    @Test
    void isAvailable_trueWhenFilesEqualCountAndOmbiAvailable() {
        assertTrue(available().isAvailable());
    }

    @Test
    void isAvailable_trueWhenFilesExceedCount() {
        TvRequest tv = available();
        tv.setSonarrEpisodeFileCount(65);
        assertTrue(tv.isAvailable());
    }

    @Test
    void isAvailable_falseWhenFileCountLessThanEpisodeCount() {
        TvRequest tv = available();
        tv.setSonarrEpisodeFileCount(10);
        assertFalse(tv.isAvailable());
    }

    @Test
    void isAvailable_falseWhenEpisodeCountZero() {
        TvRequest tv = available();
        tv.setSonarrEpisodeCount(0);
        assertFalse(tv.isAvailable());
    }

    @Test
    void isAvailable_falseWhenEpisodeCountNull() {
        TvRequest tv = available();
        tv.setSonarrEpisodeCount(null);
        assertFalse(tv.isAvailable());
    }

    @Test
    void isAvailable_falseWhenFileCountNull() {
        TvRequest tv = available();
        tv.setSonarrEpisodeFileCount(null);
        assertFalse(tv.isAvailable());
    }

    @Test
    void isAvailable_falseWhenOmbiStatusNotAvailable() {
        TvRequest tv = available();
        tv.setOmbiRequestStatus("Common.Processing");
        assertFalse(tv.isAvailable());
    }

    @Test
    void hashCode_isStable() {
        TvRequest tv = available();
        int h1 = tv.hashCode();
        int h2 = tv.hashCode();
        assertEquals(h2, h1);
    }

    @Test
    void toString_isNonNull() {
        assertNotNull(available().toString());
    }

    @Test
    void settersAndGetters_roundTrip() {
        var tv = new TvRequest("Show", 12345, true, 5, "Common.Available");

        tv.setTvdbId(999);
        assertEquals(999, tv.getTvdbId());

        tv.setPlexTvdbId(888);
        assertEquals(888, tv.getPlexTvdbId());

        tv.setSonarrSeriesId(77);
        assertEquals(77, tv.getSonarrSeriesId());

        tv.setSonarrTitleSlug("show-slug");
        assertEquals("show-slug", tv.getSonarrTitleSlug());

        tv.setSonarrMonitored(true);
        assertTrue(tv.getSonarrMonitored());

        tv.setSonarrMonitoredAll("all");
        assertEquals("all", tv.getSonarrMonitoredAll());

        tv.setSonarrPath("/tv/show");
        assertEquals("/tv/show", tv.getSonarrPath());

        tv.setSonarrRootFolderPath("/tv");
        assertEquals("/tv", tv.getSonarrRootFolderPath());

        tv.setSonarrEpisodeFileCount(10);
        assertEquals(10, tv.getSonarrEpisodeFileCount());

        tv.setSonarrEpisodeCount(12);
        assertEquals(12, tv.getSonarrEpisodeCount());

        tv.setSonarrTotalEpisodeCount(50);
        assertEquals(50, tv.getSonarrTotalEpisodeCount());

        Instant t = Instant.parse("2024-06-01T00:00:00Z");
        tv.setSonarrLastSearched(t);
        assertEquals(t, tv.getSonarrLastSearched());

        tv.setSonarrOriginalLanguage("English");
        assertEquals("English", tv.getSonarrOriginalLanguage());

        tv.setSonarrQualityProfile("HD");
        assertEquals("HD", tv.getSonarrQualityProfile());

        tv.setOmbiTotalSeasons(5);
        assertEquals(5, tv.getOmbiTotalSeasons());

        tv.setOmbiExternalProviderId(42);
        assertEquals(42, tv.getOmbiExternalProviderId());
    }
}
