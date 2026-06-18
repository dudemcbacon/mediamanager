package report.butt.mediamanager.validation;

import java.util.Objects;
import org.springframework.stereotype.Component;
import report.butt.mediamanager.model.TvEpisodeRequest;

@Component
public class EpisodeLocalFileMatchesPlex implements EpisodeValidator {

    @Override
    public Boolean validate(TvEpisodeRequest episode) {
        return Objects.equals(episode.getLocalFilePathAvailable(), true)
                && episode.getLocalFileSize() != null
                && episode.getLocalFileSize().equals(episode.getPlexMediaSize());
    }

    @Override
    public int sortOrder() {
        return 460;
    }

    @Override
    public String shortName() {
        return "Local";
    }

    @Override
    public String title() {
        return "Local File OK?";
    }

    @Override
    public String description() {
        return "The local file exists on disk and its size matches the Plex media file size.";
    }
}
