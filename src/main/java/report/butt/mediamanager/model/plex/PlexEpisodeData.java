package report.butt.mediamanager.model.plex;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/** The per-episode fields read from Plex: the media file path and its size in bytes (either may be null). */
@NullMarked
public record PlexEpisodeData(
        @Nullable String path, @Nullable Long size) {}
