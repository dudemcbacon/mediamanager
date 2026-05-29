package report.butt.mediamanager.validation;

import java.time.Duration;
import java.time.Instant;
import org.springframework.stereotype.Component;
import report.butt.mediamanager.model.MovieRequest;
import report.butt.mediamanager.model.RequestType;

@Component
public class SearchedRecently implements Validator<MovieRequest> {

    private static final Duration ONE_WEEK = Duration.ofDays(7);

    @Override
    public Boolean validate(MovieRequest request) {
        if (request.isAvailable()) {
            return true;
        }
        Instant lastSearched = request.getRadarrLastSearchTime();
        if (lastSearched == null) {
            return false;
        }
        return lastSearched.isAfter(Instant.now().minus(ONE_WEEK));
    }

    @Override
    public RequestType supportedType() {
        return RequestType.MOVIE;
    }

    @Override
    public int sortOrder() {
        return 600;
    }

    @Override
    public String shortName() {
        return "Recent Search?";
    }

    @Override
    public String description() {
        return "Movie is available, or Radarr searched for it within the last week.";
    }
}
