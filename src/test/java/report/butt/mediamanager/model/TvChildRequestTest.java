package report.butt.mediamanager.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;

@NullMarked
class TvChildRequestTest {

    private static TvRequest parent() {
        var p = new TvRequest("Show", 12345, false, 5000, "Common.Approved");
        p.setId(1L);
        return p;
    }

    @Test
    void isAvailable_trueWhenOmbiAvailableAndStatusAvailable() {
        var child = new TvChildRequest(parent(), "Show", 12345, true, 6000, "Common.Available");
        assertTrue(child.isAvailable());
    }

    @Test
    void isAvailable_falseWhenOmbiAvailableFalse() {
        var child = new TvChildRequest(parent(), "Show", 12345, false, 6000, "Common.Available");
        assertFalse(child.isAvailable());
    }

    @Test
    void isAvailable_falseWhenOmbiAvailableNull() {
        var child = new TvChildRequest(parent(), "Show", 12345, null, 6000, "Common.Available");
        assertFalse(child.isAvailable());
    }

    @Test
    void isAvailable_falseWhenStatusNotAvailable() {
        var child = new TvChildRequest(parent(), "Show", 12345, true, 6000, "Common.Processing");
        assertFalse(child.isAvailable());
    }

    @Test
    void constructor_setsOmbiParentRequestIdFromParent() {
        TvRequest p = parent();
        p.setOmbiRequestId(999);
        var child = new TvChildRequest(p, "Show", 12345, false, 6000, "Common.Approved");
        assertEquals(999, child.getOmbiParentRequestId());
    }

    @Test
    void setters_roundTrip() {
        var child = new TvChildRequest(parent(), "Show", 12345, false, 6000, "Common.Approved");

        child.setId(10L);
        assertEquals(10L, child.getId());

        child.setTitle("New Title");
        assertEquals("New Title", child.getTitle());

        child.setTvdbId(111);
        assertEquals(111, child.getTvdbId());

        child.setOmbiAvailable(true);
        assertTrue(child.getOmbiAvailable());

        child.setOmbiRequestId(222);
        assertEquals(222, child.getOmbiRequestId());

        child.setOmbiRequestStatus("Common.Available");
        assertEquals("Common.Available", child.getOmbiRequestStatus());

        child.setOmbiUserName("alice");
        assertEquals("alice", child.getOmbiUserName());

        child.setOmbiTotalSeasons(3);
        assertEquals(3, child.getOmbiTotalSeasons());

        child.setOmbiParentRequestId(333);
        assertEquals(333, child.getOmbiParentRequestId());

        child.setSeasonRequests(List.of());
        assertTrue(child.getSeasonRequests().isEmpty());

        var p2 = new TvRequest("Other", 999, false, 1, "S");
        child.setParent(p2);
        assertEquals(p2, child.getParent());
    }

    @Test
    void hashCode_isStable() {
        var child = new TvChildRequest(parent(), "Show", 12345, false, 6000, "Common.Approved");
        int h1 = child.hashCode();
        int h2 = child.hashCode();
        assertEquals(h2, h1);
    }

    @Test
    void toString_isNonNull() {
        var child = new TvChildRequest(parent(), "Show", 12345, false, 6000, "Common.Approved");
        assertNotNull(child.toString());
    }

    @Test
    void toString_withNullParent_doesNotThrow() {
        var child = new TvChildRequest(parent(), "Show", 12345, false, 6000, "Common.Approved");
        child.setParent(null);
        assertNotNull(child.toString());
    }

    @Test
    void timestamps_nullBeforePersist() {
        var child = new TvChildRequest(parent(), "Show", 12345, false, 6000, "S");
        assertEquals(null, child.getCreatedAt());
        assertEquals(null, child.getUpdatedAt());
    }
}
