package report.butt.mediamanager.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;

@NullMarked
class MovieRequestTest {

    private static MovieRequest available() {
        var m = new MovieRequest("Inception", 27205, false, 1, "Common.ProcessingRequest");
        m.setRadarrHasFile(true);
        m.setOmbiRequestStatus("Common.Available");
        return m;
    }

    @Test
    void isAvailable_trueWhenRadarrHasFileAndOmbiAvailable() {
        assertTrue(available().isAvailable());
    }

    @Test
    void isAvailable_falseWhenRadarrHasFileFalse() {
        MovieRequest m = available();
        m.setRadarrHasFile(false);
        assertFalse(m.isAvailable());
    }

    @Test
    void isAvailable_falseWhenRadarrHasFileNull() {
        MovieRequest m = available();
        m.setRadarrHasFile(null);
        assertFalse(m.isAvailable());
    }

    @Test
    void isAvailable_falseWhenOmbiStatusNotAvailable() {
        MovieRequest m = available();
        m.setOmbiRequestStatus("Common.ProcessingRequest");
        assertFalse(m.isAvailable());
    }

    @Test
    void isAvailable_falseWhenOmbiStatusNull() {
        MovieRequest m = available();
        m.setOmbiRequestStatus(null);
        assertFalse(m.isAvailable());
    }

    @Test
    void hashCode_isStable() {
        MovieRequest m = available();
        int h1 = m.hashCode();
        int h2 = m.hashCode();
        assertEquals(h2, h1);
    }

    @Test
    void toString_isNonNull() {
        assertNotNull(available().toString());
    }

    @Test
    void settersAndGetters_roundTrip() {
        var m = new MovieRequest("Title", 100, true, 42, "Common.Available");
        m.setTmdbid(200);
        assertEquals(200, m.getTmdbid());

        m.setPlexTmdbid(300);
        assertEquals(300, m.getPlexTmdbid());

        m.setRadarrRequestId(400);
        assertEquals(400, m.getRadarrRequestId());

        m.setRadarrMonitored(true);
        assertTrue(m.getRadarrMonitored());

        m.setRadarrIsAvailable(true);
        assertTrue(m.getRadarrIsAvailable());

        Instant now = Instant.now();
        m.setRadarrLastSearchTime(now);
        assertEquals(now, m.getRadarrLastSearchTime());

        m.setRadarrPath("/movies/title");
        assertEquals("/movies/title", m.getRadarrPath());

        m.setRadarrRootFolderPath("/movies");
        assertEquals("/movies", m.getRadarrRootFolderPath());

        m.setRadarrMovieFilePath("/movies/title/title.mkv");
        assertEquals("/movies/title/title.mkv", m.getRadarrMovieFilePath());

        m.setRadarrOriginalLanguage("English");
        assertEquals("English", m.getRadarrOriginalLanguage());

        m.setRadarrQualityProfile("HD-1080p");
        assertEquals("HD-1080p", m.getRadarrQualityProfile());
    }

    @Test
    void requestBaseSettersAndGetters_roundTrip() {
        var m = new MovieRequest("T", 1, false, 1, "Common.ProcessingRequest");
        m.setId(99L);
        assertEquals(99L, m.getId());

        m.setTitle("New Title");
        assertEquals("New Title", m.getTitle());

        m.setOmbiAvailable(true);
        assertTrue(m.getOmbiAvailable());

        m.setOmbiRequestId(10);
        assertEquals(10, m.getOmbiRequestId());

        m.setOmbiUserName("alice");
        assertEquals("alice", m.getOmbiUserName());

        Instant date = Instant.parse("2024-01-01T00:00:00Z");
        m.setOmbiRequestedDate(date);
        assertEquals(date, m.getOmbiRequestedDate());

        m.setStale(true);
        assertTrue(m.getStale());

        m.setStaleReason("Too old");
        assertEquals("Too old", m.getStaleReason());

        m.setMarkedStaleAt(date);
        assertEquals(date, m.getMarkedStaleAt());

        m.setPlexMetadataUrl("/plex/123");
        assertEquals("/plex/123", m.getPlexMetadataUrl());

        m.setPlexMetadataId("meta-1");
        assertEquals("meta-1", m.getPlexMetadataId());

        m.setPlexAddedAt(1000L);
        assertEquals(1000L, m.getPlexAddedAt());

        m.setPlexUpdatedAt(2000L);
        assertEquals(2000L, m.getPlexUpdatedAt());

        m.setPlexMediaId(55);
        assertEquals(55, m.getPlexMediaId());

        m.setPlexMediaFilename("/media/file.mkv");
        assertEquals("/media/file.mkv", m.getPlexMediaFilename());

        m.setPlexMediaSize(123456L);
        assertEquals(123456L, m.getPlexMediaSize());

        m.setPlexMediaDuration(7200L);
        assertEquals(7200L, m.getPlexMediaDuration());
    }
}
