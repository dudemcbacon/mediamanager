package report.butt.mediamanager.client;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import report.butt.mediamanager.model.plex.PlexMetadata;

@NullMarked
public record MetadataResult(String url, @Nullable PlexMetadata metadata) {}
