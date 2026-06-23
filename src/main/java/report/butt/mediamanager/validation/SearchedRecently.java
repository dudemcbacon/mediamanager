package report.butt.mediamanager.validation;

import org.jspecify.annotations.NullMarked;
import org.springframework.stereotype.Component;
import report.butt.mediamanager.model.MovieRequest;
import report.butt.mediamanager.model.RequestType;

@Component
@NullMarked
public class SearchedRecently implements Validator<MovieRequest> {

    @Override
    public Boolean validate(MovieRequest request) {
        return request.isAvailable() || ValidationSupport.searchedWithinLastWeek(request.getRadarrLastSearchTime());
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
        return "Search";
    }

    @Override
    public String title() {
        return "Recent Search?";
    }

    @Override
    public String description() {
        return "Movie is available, or Radarr searched for it within the last week.";
    }
}
