package report.butt.mediamanager.validation;

import org.jspecify.annotations.NullMarked;
import org.springframework.stereotype.Component;
import report.butt.mediamanager.model.RequestType;
import report.butt.mediamanager.model.TvRequest;

@Component
@NullMarked
public class AvailableInOmbiTv implements Validator<TvRequest> {
    @Override
    public Boolean validate(TvRequest request) {
        return request.getOmbiRequestId() != null;
    }

    @Override
    public RequestType supportedType() {
        return RequestType.TV;
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
        return "Show has an Ombi request ID, meaning it is tracked in Ombi.";
    }
}
