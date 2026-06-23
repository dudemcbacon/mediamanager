package report.butt.mediamanager.validation;

import org.jspecify.annotations.NullMarked;
import org.springframework.stereotype.Component;
import report.butt.mediamanager.model.MovieRequest;
import report.butt.mediamanager.model.RequestType;

@Component
@NullMarked
public class AvailableInOmbi implements Validator<MovieRequest> {
    @Override
    public Boolean validate(MovieRequest request) {
        return request.getOmbiRequestId() != null;
    }

    @Override
    public RequestType supportedType() {
        return RequestType.MOVIE;
    }

    @Override
    public int sortOrder() {
        return 100;
    }

    @Override
    public String shortName() {
        return "Ombi";
    }

    @Override
    public String title() {
        return "Ombi?";
    }

    @Override
    public String description() {
        return "Movie has an Ombi request ID, meaning it is tracked in Ombi.";
    }
}
