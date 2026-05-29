package report.butt.mediamanager.validation;

import org.springframework.stereotype.Component;
import report.butt.mediamanager.model.MovieRequest;
import report.butt.mediamanager.model.RequestType;

@Component
public class HistoryWithoutAvailability implements Validator<MovieRequest> {
    @Override
    public Boolean validate(MovieRequest request) {
        Integer count = request.getRadarrHistoryCount();
        if (count == null) {
            return true;
        }
        return !(count > 1 && !request.isAvailable());
    }

    @Override
    public RequestType supportedType() {
        return RequestType.MOVIE;
    }

    @Override
    public int sortOrder() {
        return 350;
    }

    @Override
    public String shortName() {
        return "History?";
    }

    @Override
    public String description() {
        return "Radarr history count is not greater than 1 while the movie is unavailable, indicating repeated download attempts without success.";
    }
}
