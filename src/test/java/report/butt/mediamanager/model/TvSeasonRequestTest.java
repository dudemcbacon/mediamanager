package report.butt.mediamanager.model;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class TvSeasonRequestTest {

    private static TvChildRequest child() {
        TvRequest parent = new TvRequest("Show", 100, false, 5, "Common.Approved");
        parent.setId(1L);
        TvChildRequest c = new TvChildRequest(parent, "Show", 100, false, 6, "Common.Approved");
        c.setId(2L);
        return c;
    }

    @Test
    void constructor_setsAllFields() {
        TvChildRequest c = child();
        TvSeasonRequest season = new TvSeasonRequest(c, 7000, 1, true);

        assert season.getTvChildRequest() == c;
        assert season.getOmbiSeasonRequestId() == 7000;
        assert season.getOmbiSeasonNumber() == 1;
        assertTrue(season.getOmbiSeasonAvailable());
    }

    @Test
    void setters_roundTrip() {
        TvSeasonRequest season = new TvSeasonRequest(child(), 7000, 1, false);

        season.setId(3L);
        assert season.getId() == 3L;

        season.setOmbiSeasonRequestId(8000);
        assert season.getOmbiSeasonRequestId() == 8000;

        season.setOmbiSeasonNumber(2);
        assert season.getOmbiSeasonNumber() == 2;

        season.setOmbiSeasonAvailable(true);
        assertTrue(season.getOmbiSeasonAvailable());

        TvChildRequest c2 = child();
        season.setTvChildRequest(c2);
        assert season.getTvChildRequest() == c2;

        season.setEpisodeRequests(List.of());
        assert season.getEpisodeRequests().isEmpty();
    }

    @Test
    void hashCode_isStable() {
        TvSeasonRequest season = new TvSeasonRequest(child(), 7000, 1, false);
        int h1 = season.hashCode();
        int h2 = season.hashCode();
        assert h1 == h2;
    }

    @Test
    void toString_isNonNull() {
        TvSeasonRequest season = new TvSeasonRequest(child(), 7000, 1, false);
        assertNotNull(season.toString());
    }

    @Test
    void toString_withNullChild_doesNotThrow() {
        TvSeasonRequest season = new TvSeasonRequest(child(), 7000, 1, false);
        season.setTvChildRequest(null);
        assertNotNull(season.toString());
    }

    @Test
    void timestamps_nullBeforePersist() {
        TvSeasonRequest season = new TvSeasonRequest(child(), 7000, 1, false);
        assertNull(season.getCreatedAt());
        assertNull(season.getUpdatedAt());
    }
}
