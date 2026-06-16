package report.butt.mediamanager.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class LocalFileInspectorTest {

    @Test
    void availableWithSizeWhenFileExists(@TempDir Path tempDir) throws IOException {
        // prefix (tempDir) + reported path ("/movies/film.mkv") must resolve to the real file.
        Path moviesDir = Files.createDirectories(tempDir.resolve("movies"));
        Files.write(moviesDir.resolve("film.mkv"), new byte[] {1, 2, 3, 4, 5});

        LocalFileInspector.Result result = LocalFileInspector.inspect(tempDir.toString(), "/movies/film.mkv");

        assertTrue(result.available());
        assertEquals(5L, result.sizeBytes());
    }

    @Test
    void unavailableWhenFileMissing(@TempDir Path tempDir) {
        LocalFileInspector.Result result = LocalFileInspector.inspect(tempDir.toString(), "/movies/missing.mkv");

        assertFalse(result.available());
        assertNull(result.sizeBytes());
    }

    @Test
    void unavailableWhenPathNullOrBlank() {
        assertFalse(LocalFileInspector.inspect("", null).available());
        assertFalse(LocalFileInspector.inspect("", "   ").available());
        assertNull(LocalFileInspector.inspect("", null).sizeBytes());
    }

    @Test
    void emptyPrefixChecksPathAsIs(@TempDir Path tempDir) throws IOException {
        Path file = Files.write(tempDir.resolve("film.mkv"), new byte[] {1, 2, 3});

        LocalFileInspector.Result result = LocalFileInspector.inspect("", file.toString());

        assertTrue(result.available());
        assertEquals(3L, result.sizeBytes());
    }
}
