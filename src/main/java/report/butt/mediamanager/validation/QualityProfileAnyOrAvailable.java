package report.butt.mediamanager.validation;

import org.springframework.stereotype.Component;
import report.butt.mediamanager.model.MovieRequest;
import report.butt.mediamanager.model.RequestType;

@Component
public class QualityProfileAnyOrAvailable implements Validator<MovieRequest> {
    @Override
    public Boolean validate(MovieRequest request) {
        if (request.isAvailable()) {
            return true;
        }
        return "Any".equalsIgnoreCase(request.getRadarrQualityProfile());
    }

    @Override
    public RequestType supportedType() {
        return RequestType.MOVIE;
    }

    @Override
    public int sortOrder() {
        return 380;
    }

    @Override
    public String shortName() {
        return "Qual";
    }

    @Override
    public String title() {
        return "Quality?";
    }

    @Override
    public String description() {
        return "Available movies pass; otherwise the Radarr quality profile must be set to 'Any'.";
    }
}
