package report.butt.mediamanager.controller;

import java.nio.file.Path;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import report.butt.mediamanager.service.PlexCacheService;

@RestController
public class PlexCacheController {

    private final PlexCacheService plexCacheService;

    public PlexCacheController(PlexCacheService plexCacheService) {
        this.plexCacheService = plexCacheService;
    }

    @GetMapping("/plex-cache/{filename:.+}")
    public ResponseEntity<Resource> get(@PathVariable String filename) {
        Path file = plexCacheService.resolve(filename);
        if (file == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(new FileSystemResource(file));
    }
}
