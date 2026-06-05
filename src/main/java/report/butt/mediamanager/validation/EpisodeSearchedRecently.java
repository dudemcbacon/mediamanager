package report.butt.mediamanager.validation;

import java.time.Duration;
import java.time.Instant;
import org.springframework.stereotype.Component;
import report.butt.mediamanager.model.TvEpisodeRequest;

@Component
public class EpisodeSearchedRecently implements EpisodeValidator {

    private static final Duration ONE_WEEK = Duration.ofDays(7);

    @Override
    public Boolean validate(TvEpisodeRequest episode) {
        if (Boolean.TRUE.equals(episode.getOmbiAvailable())) {
            return true;
        }
        Instant lastSearched = episode.getSonarrLastSearchTime();
        if (lastSearched == null) {
            return false;
        }
        return lastSearched.isAfter(Instant.now().minus(ONE_WEEK));
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
