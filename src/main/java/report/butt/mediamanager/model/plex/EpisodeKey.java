package report.butt.mediamanager.model.plex;

/** Identifies an episode within a show by its season and episode number. */
public record EpisodeKey(int seasonNumber, int episodeNumber) {}
