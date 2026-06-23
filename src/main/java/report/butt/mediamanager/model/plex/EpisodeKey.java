package report.butt.mediamanager.model.plex;

import org.jspecify.annotations.NullMarked;

/** Identifies an episode within a show by its season and episode number. */
@NullMarked
public record EpisodeKey(int seasonNumber, int episodeNumber) {}
