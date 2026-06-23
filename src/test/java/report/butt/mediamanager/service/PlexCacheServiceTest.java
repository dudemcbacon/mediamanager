package report.butt.mediamanager.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

@NullMarked
class PlexCacheServiceTest {

    @TempDir
    Path tempDir;

    private PlexCacheService service;

    @BeforeEach
    void setUp() throws IOException {
        service = new PlexCacheService(tempDir.toString());
        service.init();
    }

    @Test
    void storeWritesFileAndReturnsUrl() throws IOException {
        String url = service.store("movie-123", "{\"data\":1}");

        assertEquals("/plex-cache/movie-123.json", url);
        Path file = tempDir.resolve("movie-123.json");
        assertTrue(Files.exists(file));
        assertEquals("{\"data\":1}", Files.readString(file, StandardCharsets.UTF_8));
    }

    @Test
    void storeOverwritesExistingFile() throws IOException {
        service.store("movie-42", "first");
        service.store("movie-42", "second");

        Path file = tempDir.resolve("movie-42.json");
        assertEquals("second", Files.readString(file, StandardCharsets.UTF_8));
    }

    @Test
    void storeThrowsOnInvalidKey() {
        assertThrows(IllegalArgumentException.class, () -> service.store("bad key!", "body"));
        assertThrows(IllegalArgumentException.class, () -> service.store(null, "body"));
        assertThrows(IllegalArgumentException.class, () -> service.store("../traversal", "body"));
    }

    @Test
    void resolveReturnsPathForExistingFile() throws IOException {
        service.store("tv-99", "{}");

        Path resolved = service.resolve("tv-99.json");
        assertNotNull(resolved);
        assertTrue(Files.exists(resolved));
    }

    @Test
    void resolveReturnsNullForMissingFile() {
        assertNull(service.resolve("nonexistent.json"));
    }

    @Test
    void resolveReturnsNullForNonJsonFilename() {
        assertNull(service.resolve("movie-1.xml"));
        assertNull(service.resolve(null));
    }

    @Test
    void resolveReturnsNullForPathTraversalAttempt() {
        assertNull(service.resolve("../secret.json"));
    }

    @Test
    void resolveReturnsNullForKeyWithSpecialChars() {
        assertNull(service.resolve("bad key!.json"));
    }

    @Test
    void cleanExceptDeletesStaleFilesMatchingPrefix() throws IOException {
        service.store("movie-1", "{}");
        service.store("movie-2", "{}");
        service.store("movie-3", "{}");
        service.store("tv-1", "{}"); // different prefix, must not be deleted

        // Keep movie-1, delete movie-2 and movie-3
        service.cleanExcept("movie-", Set.of("movie-1"));

        assertNull(service.resolve("movie-2.json"));
        assertNull(service.resolve("movie-3.json"));
        assertNotNull(service.resolve("movie-1.json")); // valid key, kept
        assertNotNull(service.resolve("tv-1.json")); // different prefix, kept
    }

    @Test
    void cleanExceptKeepsAllWhenAllKeysValid() throws IOException {
        service.store("movie-10", "{}");
        service.store("movie-11", "{}");

        service.cleanExcept("movie-", Set.of("movie-10", "movie-11"));

        assertNotNull(service.resolve("movie-10.json"));
        assertNotNull(service.resolve("movie-11.json"));
    }

    @Test
    void cleanExceptDoesNothingWhenCacheDirIsNull() {
        // Build a service whose init() was never called (cacheDir = null)
        var noInit = new PlexCacheService("");
        // Should not throw even though cacheDir is null
        noInit.cleanExcept("movie-", Set.of());
    }

    @Test
    void initWithBlankConfiguredDirCreatesTempDir() throws IOException {
        var svc = new PlexCacheService("");
        svc.init();
        // Just verify no exception and store works
        String url = svc.store("test-1", "data");
        assertEquals("/plex-cache/test-1.json", url);
    }
}
