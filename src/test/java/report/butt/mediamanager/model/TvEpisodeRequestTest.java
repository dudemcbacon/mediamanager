package report.butt.mediamanager.model;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import org.junit.jupiter.api.Test;

class TvEpisodeRequestTest {

    private static TvSeasonRequest season() {
        TvRequest parent = new TvRequest("Show", 100, false, 5, "S");
        parent.setId(1L);
        TvChildRequest child = new TvChildRequest(parent, "Show", 100, false, 6, "S");
        child.setId(2L);
        TvSeasonRequest s = new TvSeasonRequest(child, 7, 1, false);
        s.setId(3L);
        return s;
    }

    @Test
    void constructor_setsFields() {
        TvSeasonRequest s = season();
        TvEpisodeRequest ep = new TvEpisodeRequest(s, 8000, 1);

        assert ep.getTvSeasonRequest() == s;
        assert ep.getOmbiEpisodeId() == 8000;
        assert ep.getOmbiEpisodeNumber() == 1;
    }

    @Test
    void setters_roundTrip() {
        TvEpisodeRequest ep = new TvEpisodeRequest(season(), 8000, 1);

        ep.setId(4L);
        assert ep.getId() == 4L;

        ep.setOmbiEpisodeId(9000);
        assert ep.getOmbiEpisodeId() == 9000;

        ep.setOmbiEpisodeNumber(2);
        assert ep.getOmbiEpisodeNumber() == 2;

        ep.setOmbiTitle("Pilot");
        assert "Pilot".equals(ep.getOmbiTitle());

        ep.setOmbiAvailable(true);
        assertTrue(ep.getOmbiAvailable());

        ep.setOmbiApproved(true);
        assertTrue(ep.getOmbiApproved());

        ep.setOmbiRequested(true);
        assertTrue(ep.getOmbiRequested());

        ep.setOmbiRequestStatus("Common.Available");
        assert "Common.Available".equals(ep.getOmbiRequestStatus());

        ep.setSonarrPath("/tv/show/s01e01.mkv");
        assert "/tv/show/s01e01.mkv".equals(ep.getSonarrPath());

        ep.setPlexPath("/plex/show/s01e01.mkv");
        assert "/plex/show/s01e01.mkv".equals(ep.getPlexPath());

        Instant t = Instant.parse("2024-06-01T00:00:00Z");
        ep.setSonarrLastSearchTime(t);
        assert ep.getSonarrLastSearchTime().equals(t);

        TvSeasonRequest s2 = season();
        ep.setTvSeasonRequest(s2);
        assert ep.getTvSeasonRequest() == s2;
    }

    @Test
    void hashCode_isStable() {
        TvEpisodeRequest ep = new TvEpisodeRequest(season(), 8000, 1);
        int h1 = ep.hashCode();
        int h2 = ep.hashCode();
        assert h1 == h2;
    }

    @Test
    void toString_isNonNull() {
        TvEpisodeRequest ep = new TvEpisodeRequest(season(), 8000, 1);
        assertNotNull(ep.toString());
    }

    @Test
    void toString_withNullSeason_doesNotThrow() {
        TvEpisodeRequest ep = new TvEpisodeRequest(season(), 8000, 1);
        ep.setTvSeasonRequest(null);
        assertNotNull(ep.toString());
    }

    @Test
    void timestamps_nullBeforePersist() {
        TvEpisodeRequest ep = new TvEpisodeRequest(season(), 8000, 1);
        assertNull(ep.getCreatedAt());
        assertNull(ep.getUpdatedAt());
    }
}
