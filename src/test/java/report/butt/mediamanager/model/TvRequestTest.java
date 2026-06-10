package report.butt.mediamanager.model;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import org.junit.jupiter.api.Test;

class TvRequestTest {

    private static TvRequest available() {
        TvRequest tv = new TvRequest("Breaking Bad", 81189, false, 1, "Common.Available");
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
        assert h1 == h2;
    }

    @Test
    void toString_isNonNull() {
        assertNotNull(available().toString());
    }

    @Test
    void settersAndGetters_roundTrip() {
        TvRequest tv = new TvRequest("Show", 12345, true, 5, "Common.Available");

        tv.setTvdbId(999);
        assert tv.getTvdbId() == 999;

        tv.setPlexTvdbId(888);
        assert tv.getPlexTvdbId() == 888;

        tv.setSonarrSeriesId(77);
        assert tv.getSonarrSeriesId() == 77;

        tv.setSonarrTitleSlug("show-slug");
        assert "show-slug".equals(tv.getSonarrTitleSlug());

        tv.setSonarrMonitored(true);
        assertTrue(tv.getSonarrMonitored());

        tv.setSonarrMonitoredAll("all");
        assert "all".equals(tv.getSonarrMonitoredAll());

        tv.setSonarrPath("/tv/show");
        assert "/tv/show".equals(tv.getSonarrPath());

        tv.setSonarrRootFolderPath("/tv");
        assert "/tv".equals(tv.getSonarrRootFolderPath());

        tv.setSonarrEpisodeFileCount(10);
        assert tv.getSonarrEpisodeFileCount() == 10;

        tv.setSonarrEpisodeCount(12);
        assert tv.getSonarrEpisodeCount() == 12;

        tv.setSonarrTotalEpisodeCount(50);
        assert tv.getSonarrTotalEpisodeCount() == 50;

        Instant t = Instant.parse("2024-06-01T00:00:00Z");
        tv.setSonarrLastSearched(t);
        assert tv.getSonarrLastSearched().equals(t);

        tv.setSonarrOriginalLanguage("English");
        assert "English".equals(tv.getSonarrOriginalLanguage());

        tv.setSonarrQualityProfile("HD");
        assert "HD".equals(tv.getSonarrQualityProfile());

        tv.setOmbiTotalSeasons(5);
        assert tv.getOmbiTotalSeasons() == 5;

        tv.setOmbiExternalProviderId(42);
        assert tv.getOmbiExternalProviderId() == 42;
    }
}
