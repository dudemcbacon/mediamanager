package report.butt.mediamanager.model.plex;

/** The per-episode fields read from Plex: the media file path and its size in bytes (either may be null). */
public record PlexEpisodeData(String path, Long size) {}
