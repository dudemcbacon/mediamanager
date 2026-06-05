package report.butt.mediamanager.validation;

import org.springframework.stereotype.Component;
import report.butt.mediamanager.model.MovieRequest;
import report.butt.mediamanager.model.RequestType;

@Component
public class AvailableInPlex implements Validator<MovieRequest> {
    @Override
    public Boolean validate(MovieRequest request) {
        return request.getPlexMediaId() != null;
    }

    @Override
    public RequestType supportedType() {
        return RequestType.MOVIE;
    }

    @Override
    public int sortOrder() {
        return 300;
    }

    @Override
    public String shortName() {
        return "Plex";
    }

    @Override
    public String title() {
        return "Plex?";
    }

    @Override
    public String description() {
        return "Movie has a Plex media ID, meaning it is present in the Plex library.";
    }
}
