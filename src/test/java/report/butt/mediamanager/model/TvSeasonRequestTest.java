package report.butt.mediamanager.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;

@NullMarked
class TvSeasonRequestTest {

    private static TvChildRequest child() {
        var parent = new TvRequest("Show", 100, false, 5, "Common.Approved");
        parent.setId(1L);
        var c = new TvChildRequest(parent, "Show", 100, false, 6, "Common.Approved");
        c.setId(2L);
        return c;
    }

    @Test
    void constructor_setsAllFields() {
        TvChildRequest c = child();
        var season = new TvSeasonRequest(c, 7000, 1, true);

        assertEquals(c, season.getTvChildRequest());
        assertEquals(7000, season.getOmbiSeasonRequestId());
        assertEquals(1, season.getOmbiSeasonNumber());
        assertTrue(season.getOmbiSeasonAvailable());
    }

    @Test
    void setters_roundTrip() {
        var season = new TvSeasonRequest(child(), 7000, 1, false);

        season.setId(3L);
        assertEquals(3L, season.getId());

        season.setOmbiSeasonRequestId(8000);
        assertEquals(8000, season.getOmbiSeasonRequestId());

        season.setOmbiSeasonNumber(2);
        assertEquals(2, season.getOmbiSeasonNumber());

        season.setOmbiSeasonAvailable(true);
        assertTrue(season.getOmbiSeasonAvailable());

        TvChildRequest c2 = child();
        season.setTvChildRequest(c2);
        assertEquals(c2, season.getTvChildRequest());

        season.setEpisodeRequests(List.of());
        assertTrue(season.getEpisodeRequests().isEmpty());
    }

    @Test
    void hashCode_isStable() {
        var season = new TvSeasonRequest(child(), 7000, 1, false);
        int h1 = season.hashCode();
        int h2 = season.hashCode();
        assertEquals(h2, h1);
    }

    @Test
    void toString_isNonNull() {
        var season = new TvSeasonRequest(child(), 7000, 1, false);
        assertNotNull(season.toString());
    }

    @Test
    void toString_withNullChild_doesNotThrow() {
        var season = new TvSeasonRequest(child(), 7000, 1, false);
        season.setTvChildRequest(null);
        assertNotNull(season.toString());
    }

    @Test
    void timestamps_nullBeforePersist() {
        var season = new TvSeasonRequest(child(), 7000, 1, false);
        assertNull(season.getCreatedAt());
        assertNull(season.getUpdatedAt());
    }
}
