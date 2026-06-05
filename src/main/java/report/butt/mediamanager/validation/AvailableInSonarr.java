package report.butt.mediamanager.validation;

import org.springframework.stereotype.Component;
import report.butt.mediamanager.model.RequestType;
import report.butt.mediamanager.model.TvRequest;

@Component
public class AvailableInSonarr implements Validator<TvRequest> {
    @Override
    public Boolean validate(TvRequest request) {
        return request.getSonarrSeriesId() != null;
    }

    @Override
    public RequestType supportedType() {
        return RequestType.TV;
    }

    @Override
    public int sortOrder() {
        return 200;
    }

    @Override
    public String shortName() {
        return "Sonarr";
    }

    @Override
    public String title() {
        return "Sonarr?";
    }

    @Override
    public String description() {
        return "Show has a Sonarr series ID, meaning it is tracked in Sonarr.";
    }
}
