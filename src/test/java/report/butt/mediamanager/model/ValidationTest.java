package report.butt.mediamanager.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Objects;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;

@NullMarked
class ValidationTest {

    @Test
    void constructor_withRequest_setsFields() {
        var req = new MovieRequest("T", 1, false, 1, "Common.ProcessingRequest");
        var v = new Validation("PathsMatch", true, req);

        assertEquals("PathsMatch", v.getValidationName());
        assertTrue(v.getResult());
        assertEquals(req, v.getRequest());
        assertNull(v.getTvEpisode());
    }

    @Test
    void constructor_withTvEpisode_setsFields() {
        var parent = new TvRequest("Show", 100, false, 5, "Common.Available");
        var child = new TvChildRequest(parent, "Show", 100, false, 6, "Common.Available");
        var season = new TvSeasonRequest(child, 7, 1, false);
        var ep = new TvEpisodeRequest(season, 8, 1);

        var v = new Validation("EpisodePaths", false, ep);

        assertEquals("EpisodePaths", v.getValidationName());
        assertNull(v.getResult() == null ? null : v.getResult() ? true : null);
        assertFalse(Objects.equals(v.getResult(), true));
        assertEquals(ep, v.getTvEpisode());
        assertNull(v.getRequest());
    }

    @Test
    void setters_roundTrip() {
        var req = new MovieRequest("T", 1, false, 1, "S");
        var v = new Validation("Name", true, req);

        v.setId(10L);
        assertEquals(10L, v.getId());

        v.setValidationName("NewName");
        assertEquals("NewName", v.getValidationName());

        v.setResult(false);
        assertFalse(Objects.equals(v.getResult(), true));

        var req2 = new MovieRequest("T2", 2, false, 2, "S");
        v.setRequest(req2);
        assertEquals(req2, v.getRequest());

        var parent = new TvRequest("P", 1, false, 1, "S");
        var child = new TvChildRequest(parent, "C", 1, false, 2, "S");
        var season = new TvSeasonRequest(child, 3, 1, false);
        var ep = new TvEpisodeRequest(season, 4, 1);
        v.setTvEpisode(ep);
        assertEquals(ep, v.getTvEpisode());
    }

    @Test
    void hashCode_isStable() {
        var req = new MovieRequest("T", 1, false, 1, "S");
        var v = new Validation("Name", true, req);
        int h1 = v.hashCode();
        int h2 = v.hashCode();
        assertEquals(h2, h1);
    }

    @Test
    void toString_isNonNull() {
        var req = new MovieRequest("T", 1, false, 1, "S");
        var v = new Validation("Name", true, req);
        assertNotNull(v.toString());
    }

    @Test
    void createdAt_updatedAt_nullBeforePersist() {
        var req = new MovieRequest("T", 1, false, 1, "S");
        var v = new Validation("Name", true, req);
        assertNull(v.getCreatedAt());
        assertNull(v.getUpdatedAt());
    }
}
