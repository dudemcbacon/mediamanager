package report.butt.mediamanager.model;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ValidationTest {

    @Test
    void constructor_withRequest_setsFields() {
        MovieRequest req = new MovieRequest("T", 1, false, 1, "Common.ProcessingRequest");
        Validation v = new Validation("PathsMatch", true, req);

        assert "PathsMatch".equals(v.getValidationName());
        assertTrue(v.getResult());
        assert v.getRequest() == req;
        assertNull(v.getTvEpisode());
    }

    @Test
    void constructor_withTvEpisode_setsFields() {
        TvRequest parent = new TvRequest("Show", 100, false, 5, "Common.Available");
        TvChildRequest child = new TvChildRequest(parent, "Show", 100, false, 6, "Common.Available");
        TvSeasonRequest season = new TvSeasonRequest(child, 7, 1, false);
        TvEpisodeRequest ep = new TvEpisodeRequest(season, 8, 1);

        Validation v = new Validation("EpisodePaths", false, ep);

        assert "EpisodePaths".equals(v.getValidationName());
        assertNull(v.getResult() == null ? null : v.getResult() ? true : null);
        assert !Boolean.TRUE.equals(v.getResult());
        assert v.getTvEpisode() == ep;
        assertNull(v.getRequest());
    }

    @Test
    void setters_roundTrip() {
        MovieRequest req = new MovieRequest("T", 1, false, 1, "S");
        Validation v = new Validation("Name", true, req);

        v.setId(10L);
        assert v.getId() == 10L;

        v.setValidationName("NewName");
        assert "NewName".equals(v.getValidationName());

        v.setResult(false);
        assert !Boolean.TRUE.equals(v.getResult());

        MovieRequest req2 = new MovieRequest("T2", 2, false, 2, "S");
        v.setRequest(req2);
        assert v.getRequest() == req2;

        TvRequest parent = new TvRequest("P", 1, false, 1, "S");
        TvChildRequest child = new TvChildRequest(parent, "C", 1, false, 2, "S");
        TvSeasonRequest season = new TvSeasonRequest(child, 3, 1, false);
        TvEpisodeRequest ep = new TvEpisodeRequest(season, 4, 1);
        v.setTvEpisode(ep);
        assert v.getTvEpisode() == ep;
    }

    @Test
    void hashCode_isStable() {
        MovieRequest req = new MovieRequest("T", 1, false, 1, "S");
        Validation v = new Validation("Name", true, req);
        int h1 = v.hashCode();
        int h2 = v.hashCode();
        assert h1 == h2;
    }

    @Test
    void toString_isNonNull() {
        MovieRequest req = new MovieRequest("T", 1, false, 1, "S");
        Validation v = new Validation("Name", true, req);
        assertNotNull(v.toString());
    }

    @Test
    void createdAt_updatedAt_nullBeforePersist() {
        MovieRequest req = new MovieRequest("T", 1, false, 1, "S");
        Validation v = new Validation("Name", true, req);
        assertNull(v.getCreatedAt());
        assertNull(v.getUpdatedAt());
    }
}
