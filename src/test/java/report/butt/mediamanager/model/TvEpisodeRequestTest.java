package report.butt.mediamanager.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;

@NullMarked
class TvEpisodeRequestTest {

    private static TvSeasonRequest season() {
        var parent = new TvRequest("Show", 100, false, 5, "S");
        parent.setId(1L);
        var child = new TvChildRequest(parent, "Show", 100, false, 6, "S");
        child.setId(2L);
        var s = new TvSeasonRequest(child, 7, 1, false);
        s.setId(3L);
        return s;
    }

    @Test
    void constructor_setsFields() {
        TvSeasonRequest s = season();
        var ep = new TvEpisodeRequest(s, 8000, 1);

        assertEquals(s, ep.getTvSeasonRequest());
        assertEquals(8000, ep.getOmbiEpisodeId());
        assertEquals(1, ep.getOmbiEpisodeNumber());
    }

    @Test
    void setters_roundTrip() {
        var ep = new TvEpisodeRequest(season(), 8000, 1);

        ep.setId(4L);
        assertEquals(4L, ep.getId());

        ep.setOmbiEpisodeId(9000);
        assertEquals(9000, ep.getOmbiEpisodeId());

        ep.setOmbiEpisodeNumber(2);
        assertEquals(2, ep.getOmbiEpisodeNumber());

        ep.setOmbiTitle("Pilot");
        assertEquals("Pilot", ep.getOmbiTitle());

        ep.setOmbiAvailable(true);
        assertTrue(ep.getOmbiAvailable());

        ep.setOmbiApproved(true);
        assertTrue(ep.getOmbiApproved());

        ep.setOmbiRequested(true);
        assertTrue(ep.getOmbiRequested());

        ep.setOmbiRequestStatus("Common.Available");
        assertEquals("Common.Available", ep.getOmbiRequestStatus());

        ep.setSonarrPath("/tv/show/s01e01.mkv");
        assertEquals("/tv/show/s01e01.mkv", ep.getSonarrPath());

        ep.setPlexPath("/plex/show/s01e01.mkv");
        assertEquals("/plex/show/s01e01.mkv", ep.getPlexPath());

        Instant t = Instant.parse("2024-06-01T00:00:00Z");
        ep.setSonarrLastSearchTime(t);
        assertEquals(t, ep.getSonarrLastSearchTime());

        TvSeasonRequest s2 = season();
        ep.setTvSeasonRequest(s2);
        assertEquals(s2, ep.getTvSeasonRequest());
    }

    @Test
    void hashCode_isStable() {
        var ep = new TvEpisodeRequest(season(), 8000, 1);
        int h1 = ep.hashCode();
        int h2 = ep.hashCode();
        assertEquals(h2, h1);
    }

    @Test
    void toString_isNonNull() {
        var ep = new TvEpisodeRequest(season(), 8000, 1);
        assertNotNull(ep.toString());
    }

    @Test
    void toString_withNullSeason_doesNotThrow() {
        var ep = new TvEpisodeRequest(season(), 8000, 1);
        ep.setTvSeasonRequest(null);
        assertNotNull(ep.toString());
    }

    @Test
    void timestamps_nullBeforePersist() {
        var ep = new TvEpisodeRequest(season(), 8000, 1);
        assertNull(ep.getCreatedAt());
        assertNull(ep.getUpdatedAt());
    }
}
