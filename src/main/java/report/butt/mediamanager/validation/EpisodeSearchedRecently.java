package report.butt.mediamanager.validation;

import org.springframework.stereotype.Component;
import report.butt.mediamanager.model.TvEpisodeRequest;

@Component
public class EpisodeSearchedRecently implements EpisodeValidator {

    @Override
    public Boolean validate(TvEpisodeRequest episode) {
        return Boolean.TRUE.equals(episode.getOmbiAvailable())
                || ValidationSupport.searchedWithinLastWeek(episode.getSonarrLastSearchTime());
    }

    @Override
    public int sortOrder() {
        return 600;
    }

    @Override
    public String shortName() {
        return "Search";
    }

    @Override
    public String title() {
        return "Recent Search?";
    }

    @Override
    public String description() {
        return "Episode is available, or Sonarr searched for it within the last week.";
    }
}
