package report.butt.mediamanager.validation;

import java.util.Objects;
import org.springframework.stereotype.Component;
import report.butt.mediamanager.model.TvEpisodeRequest;

@Component
public class EpisodePathsMatch implements EpisodeValidator {

    @Override
    public Boolean validate(TvEpisodeRequest episode) {
        return Objects.equals(
                ValidationSupport.stripMnt(episode.getPlexPath()), ValidationSupport.stripMnt(episode.getSonarrPath()));
    }

    @Override
    public int sortOrder() {
        return 450;
    }

    @Override
    public String shortName() {
        return "Paths";
    }

    @Override
    public String title() {
        return "Paths Match?";
    }

    @Override
    public String description() {
        return "The Plex path matches the Sonarr path (ignoring a leading /mnt on either side).";
    }
}
