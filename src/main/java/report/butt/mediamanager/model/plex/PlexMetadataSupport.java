package report.butt.mediamanager.model.plex;

import java.util.OptionalInt;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import report.butt.mediamanager.model.Request;

/**
 * Pure helpers for reading {@link PlexMetadata} into our domain, shared by the movie/TV refresh paths and the client.
 */
@NullMarked
public final class PlexMetadataSupport {

    private PlexMetadataSupport() {}

    /**
     * Parses the numeric id from the first Plex guid with the given {@code prefix} (e.g. {@code "tmdb://"}), or empty
     * if none match. A guid whose value after the prefix is non-numeric throws {@link NumberFormatException}, matching
     * the inline behaviour this replaces.
     */
    public static OptionalInt parseGuidId(PlexMetadata metadata, String prefix) {
        if (metadata.getGuids() == null) {
            return OptionalInt.empty();
        }
        return metadata.getGuids().stream()
                .map(PlexGuid::getId)
                .filter(id -> id != null && id.startsWith(prefix))
                .map(id -> id.substring(prefix.length()))
                .mapToInt(Integer::parseInt)
                .findFirst();
    }

    /** Copies the first media item's id/duration and its first part's filename/size onto the request, if present. */
    public static void applyFirstMedia(Request request, PlexMetadata metadata) {
        if (metadata.getMedia() == null || metadata.getMedia().isEmpty()) {
            return;
        }
        PlexMedia media = metadata.getMedia().get(0);
        request.setPlexMediaId(media.getId());
        request.setPlexMediaDuration(media.getDuration());
        if (media.getPart() != null && !media.getPart().isEmpty()) {
            PlexPart part = media.getPart().get(0);
            request.setPlexMediaFilename(part.getFile());
            request.setPlexMediaSize(part.getSize());
        }
    }

    /** The first part's file path and size for an episode, or null when the episode has no media/part/file. */
    public static @Nullable PlexEpisodeData firstFile(PlexMetadata episode) {
        if (episode.getMedia() == null || episode.getMedia().isEmpty()) {
            return null;
        }
        PlexMedia media = episode.getMedia().get(0);
        if (media.getPart() == null || media.getPart().isEmpty()) {
            return null;
        }
        PlexPart part = media.getPart().get(0);
        if (part.getFile() == null) {
            return null;
        }
        return new PlexEpisodeData(part.getFile(), part.getSize());
    }
}
