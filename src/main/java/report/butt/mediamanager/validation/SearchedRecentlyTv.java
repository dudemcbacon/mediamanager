package report.butt.mediamanager.validation;

import org.springframework.stereotype.Component;
import report.butt.mediamanager.model.RequestType;
import report.butt.mediamanager.model.TvRequest;

@Component
public class SearchedRecentlyTv implements Validator<TvRequest> {

    @Override
    public Boolean validate(TvRequest request) {
        return request.isAvailable() || ValidationSupport.searchedWithinLastWeek(request.getSonarrLastSearched());
    }

    @Override
    public RequestType supportedType() {
        return RequestType.TV;
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
        return "Show is available, or Sonarr searched for it within the last week.";
    }
}
