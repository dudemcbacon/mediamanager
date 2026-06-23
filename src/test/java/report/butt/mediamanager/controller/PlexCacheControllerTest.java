package report.butt.mediamanager.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import report.butt.mediamanager.service.PlexCacheService;

@NullMarked
class PlexCacheControllerTest {

    private final PlexCacheService plexCacheService = mock(PlexCacheService.class);
    private final PlexCacheController controller = new PlexCacheController(plexCacheService);

    @Test
    void get_fileNotFound_returns404() {
        when(plexCacheService.resolve("missing.json")).thenReturn(null);

        ResponseEntity<Resource> response = controller.get("missing.json");

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    void get_fileExists_returns200WithJsonBody() throws IOException {
        Path tempFile = Files.createTempFile("plex-test", ".json");
        Files.writeString(tempFile, "{}");
        try {
            when(plexCacheService.resolve("data.json")).thenReturn(tempFile);

            ResponseEntity<Resource> response = controller.get("data.json");

            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertNotNull(response.getBody());
        } finally {
            Files.deleteIfExists(tempFile);
        }
    }
}
