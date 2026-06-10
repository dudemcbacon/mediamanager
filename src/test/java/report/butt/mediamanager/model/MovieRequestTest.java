package report.butt.mediamanager.model;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import org.junit.jupiter.api.Test;

class MovieRequestTest {

    private static MovieRequest available() {
        MovieRequest m = new MovieRequest("Inception", 27205, false, 1, "Common.ProcessingRequest");
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
        assert h1 == h2;
    }

    @Test
    void toString_isNonNull() {
        assertNotNull(available().toString());
    }

    @Test
    void settersAndGetters_roundTrip() {
        MovieRequest m = new MovieRequest("Title", 100, true, 42, "Common.Available");
        m.setTmdbid(200);
        assert m.getTmdbid() == 200;

        m.setPlexTmdbid(300);
        assert m.getPlexTmdbid() == 300;

        m.setRadarrRequestId(400);
        assert m.getRadarrRequestId() == 400;

        m.setRadarrMonitored(true);
        assertTrue(m.getRadarrMonitored());

        m.setRadarrIsAvailable(true);
        assertTrue(m.getRadarrIsAvailable());

        Instant now = Instant.now();
        m.setRadarrLastSearchTime(now);
        assert m.getRadarrLastSearchTime().equals(now);

        m.setRadarrPath("/movies/title");
        assert "/movies/title".equals(m.getRadarrPath());

        m.setRadarrRootFolderPath("/movies");
        assert "/movies".equals(m.getRadarrRootFolderPath());

        m.setRadarrMovieFilePath("/movies/title/title.mkv");
        assert "/movies/title/title.mkv".equals(m.getRadarrMovieFilePath());

        m.setRadarrOriginalLanguage("English");
        assert "English".equals(m.getRadarrOriginalLanguage());

        m.setRadarrQualityProfile("HD-1080p");
        assert "HD-1080p".equals(m.getRadarrQualityProfile());
    }

    @Test
    void requestBaseSettersAndGetters_roundTrip() {
        MovieRequest m = new MovieRequest("T", 1, false, 1, "Common.ProcessingRequest");
        m.setId(99L);
        assert m.getId() == 99L;

        m.setTitle("New Title");
        assert "New Title".equals(m.getTitle());

        m.setOmbiAvailable(true);
        assertTrue(m.getOmbiAvailable());

        m.setOmbiRequestId(10);
        assert m.getOmbiRequestId() == 10;

        m.setOmbiUserName("alice");
        assert "alice".equals(m.getOmbiUserName());

        Instant date = Instant.parse("2024-01-01T00:00:00Z");
        m.setOmbiRequestedDate(date);
        assert m.getOmbiRequestedDate().equals(date);

        m.setStale(true);
        assertTrue(m.getStale());

        m.setStaleReason("Too old");
        assert "Too old".equals(m.getStaleReason());

        m.setMarkedStaleAt(date);
        assert m.getMarkedStaleAt().equals(date);

        m.setPlexMetadataUrl("/plex/123");
        assert "/plex/123".equals(m.getPlexMetadataUrl());

        m.setPlexMetadataId("meta-1");
        assert "meta-1".equals(m.getPlexMetadataId());

        m.setPlexAddedAt(1000L);
        assert m.getPlexAddedAt() == 1000L;

        m.setPlexUpdatedAt(2000L);
        assert m.getPlexUpdatedAt() == 2000L;

        m.setPlexMediaId(55);
        assert m.getPlexMediaId() == 55;

        m.setPlexMediaFilename("/media/file.mkv");
        assert "/media/file.mkv".equals(m.getPlexMediaFilename());

        m.setPlexMediaSize(123456L);
        assert m.getPlexMediaSize() == 123456L;

        m.setPlexMediaDuration(7200L);
        assert m.getPlexMediaDuration() == 7200L;
    }
}
