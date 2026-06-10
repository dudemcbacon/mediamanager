package report.butt.mediamanager.model;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class TvChildRequestTest {

    private static TvRequest parent() {
        TvRequest p = new TvRequest("Show", 12345, false, 5000, "Common.Approved");
        p.setId(1L);
        return p;
    }

    @Test
    void isAvailable_trueWhenOmbiAvailableAndStatusAvailable() {
        TvChildRequest child = new TvChildRequest(parent(), "Show", 12345, true, 6000, "Common.Available");
        assertTrue(child.isAvailable());
    }

    @Test
    void isAvailable_falseWhenOmbiAvailableFalse() {
        TvChildRequest child = new TvChildRequest(parent(), "Show", 12345, false, 6000, "Common.Available");
        assertFalse(child.isAvailable());
    }

    @Test
    void isAvailable_falseWhenOmbiAvailableNull() {
        TvChildRequest child = new TvChildRequest(parent(), "Show", 12345, null, 6000, "Common.Available");
        assertFalse(child.isAvailable());
    }

    @Test
    void isAvailable_falseWhenStatusNotAvailable() {
        TvChildRequest child = new TvChildRequest(parent(), "Show", 12345, true, 6000, "Common.Processing");
        assertFalse(child.isAvailable());
    }

    @Test
    void constructor_setsOmbiParentRequestIdFromParent() {
        TvRequest p = parent();
        p.setOmbiRequestId(999);
        TvChildRequest child = new TvChildRequest(p, "Show", 12345, false, 6000, "Common.Approved");
        assert child.getOmbiParentRequestId() == 999;
    }

    @Test
    void setters_roundTrip() {
        TvChildRequest child = new TvChildRequest(parent(), "Show", 12345, false, 6000, "Common.Approved");

        child.setId(10L);
        assert child.getId() == 10L;

        child.setTitle("New Title");
        assert "New Title".equals(child.getTitle());

        child.setTvdbId(111);
        assert child.getTvdbId() == 111;

        child.setOmbiAvailable(true);
        assertTrue(child.getOmbiAvailable());

        child.setOmbiRequestId(222);
        assert child.getOmbiRequestId() == 222;

        child.setOmbiRequestStatus("Common.Available");
        assert "Common.Available".equals(child.getOmbiRequestStatus());

        child.setOmbiUserName("alice");
        assert "alice".equals(child.getOmbiUserName());

        child.setOmbiTotalSeasons(3);
        assert child.getOmbiTotalSeasons() == 3;

        child.setOmbiParentRequestId(333);
        assert child.getOmbiParentRequestId() == 333;

        child.setSeasonRequests(List.of());
        assert child.getSeasonRequests().isEmpty();

        TvRequest p2 = new TvRequest("Other", 999, false, 1, "S");
        child.setParent(p2);
        assert child.getParent() == p2;
    }

    @Test
    void hashCode_isStable() {
        TvChildRequest child = new TvChildRequest(parent(), "Show", 12345, false, 6000, "Common.Approved");
        int h1 = child.hashCode();
        int h2 = child.hashCode();
        assert h1 == h2;
    }

    @Test
    void toString_isNonNull() {
        TvChildRequest child = new TvChildRequest(parent(), "Show", 12345, false, 6000, "Common.Approved");
        assertNotNull(child.toString());
    }

    @Test
    void toString_withNullParent_doesNotThrow() {
        TvChildRequest child = new TvChildRequest(parent(), "Show", 12345, false, 6000, "Common.Approved");
        child.setParent(null);
        assertNotNull(child.toString());
    }

    @Test
    void timestamps_nullBeforePersist() {
        TvChildRequest child = new TvChildRequest(parent(), "Show", 12345, false, 6000, "S");
        assert child.getCreatedAt() == null;
        assert child.getUpdatedAt() == null;
    }
}
