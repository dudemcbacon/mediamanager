package report.butt.mediamanager.service;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@NullMarked
public class PlexCacheService {

    private static final Logger log = LoggerFactory.getLogger(PlexCacheService.class);
    private static final Pattern KEY_PATTERN = Pattern.compile("[A-Za-z0-9_-]+");
    public static final String URL_PREFIX = "/plex-cache/";

    private final @Nullable String configuredDir;
    private @Nullable Path cacheDir;

    public PlexCacheService(@Value("${plex.cache.dir:}") String configuredDir) {
        this.configuredDir = configuredDir;
    }

    @PostConstruct
    void init() throws IOException {
        if (configuredDir != null && !configuredDir.isBlank()) {
            this.cacheDir = Paths.get(configuredDir).toAbsolutePath().normalize();
            Files.createDirectories(cacheDir);
        } else {
            this.cacheDir = Files.createTempDirectory("mediamanager-plex-cache");
        }
        log.info("Plex response cache directory: {}", cacheDir);
    }

    public String store(String key, String body) {
        validateKey(key);
        Path file = cacheDir.resolve(key + ".json");
        try {
            Files.writeString(
                    file,
                    body,
                    StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING,
                    StandardOpenOption.WRITE);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to write Plex cache file " + file, e);
        }
        return URL_PREFIX + key + ".json";
    }

    public @Nullable Path resolve(@Nullable String filename) {
        if (filename == null || !filename.endsWith(".json")) {
            return null;
        }
        String key = filename.substring(0, filename.length() - ".json".length());
        if (!KEY_PATTERN.matcher(key).matches()) {
            return null;
        }
        Path resolved = cacheDir.resolve(filename).normalize();
        if (!resolved.startsWith(cacheDir)) {
            return null;
        }
        return Files.exists(resolved) ? resolved : null;
    }

    public void cleanExcept(String keyPrefix, Set<String> validKeys) {
        if (cacheDir == null) {
            return;
        }
        try (Stream<Path> files = Files.list(cacheDir)) {
            files.filter(Files::isRegularFile).forEach(path -> {
                String name = path.getFileName().toString();
                if (!name.endsWith(".json")) {
                    return;
                }
                String key = name.substring(0, name.length() - ".json".length());
                if (!key.startsWith(keyPrefix) || validKeys.contains(key)) {
                    return;
                }
                try {
                    Files.deleteIfExists(path);
                } catch (IOException e) {
                    log.warn("Failed to delete stale Plex cache file {}", path, e);
                }
            });
        } catch (IOException e) {
            log.warn("Failed to enumerate Plex cache directory {}", cacheDir, e);
        }
    }

    private static void validateKey(@Nullable String key) {
        if (key == null || !KEY_PATTERN.matcher(key).matches()) {
            throw new IllegalArgumentException("Invalid cache key: " + key);
        }
    }
}
