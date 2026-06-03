package report.butt.mediamanager.validation;

import java.util.Objects;

import org.springframework.stereotype.Component;

import report.butt.mediamanager.model.TvEpisodeRequest;

@Component
public class EpisodePathsMatch implements EpisodeValidator {

    private static final String MNT_PREFIX = "/mnt";

    @Override
    public Boolean validate(TvEpisodeRequest episode) {
        return Objects.equals(stripMnt(episode.getPlexPath()), stripMnt(episode.getSonarrPath()));
    }

    private static String stripMnt(String path) {
        if (path == null) {
            return null;
        }
        if (path.startsWith(MNT_PREFIX)) {
            return path.substring(MNT_PREFIX.length());
        }
        return path;
    }

    @Override
    public int sortOrder() {
        return 450;
    }

    @Override
    public String shortName() {
        return "Paths Match?";
    }

    @Override
    public String description() {
        return "The Plex path matches the Sonarr path (ignoring a leading /mnt on either side).";
    }
}
