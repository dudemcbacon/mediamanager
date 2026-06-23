package report.butt.mediamanager.validation;

import org.jspecify.annotations.NullMarked;
import org.springframework.stereotype.Component;
import report.butt.mediamanager.model.RequestType;
import report.butt.mediamanager.model.TvRequest;

@Component
@NullMarked
public class EnglishOrAvailableTv implements Validator<TvRequest> {
    @Override
    public Boolean validate(TvRequest request) {
        String language = request.getSonarrOriginalLanguage();
        if (language == null || "English".equalsIgnoreCase(language)) {
            return true;
        }
        return request.isAvailable();
    }

    @Override
    public RequestType supportedType() {
        return RequestType.TV;
    }

    @Override
    public int sortOrder() {
        return 360;
    }

    @Override
    public String shortName() {
        return "Lang";
    }

    @Override
    public String title() {
        return "English?";
    }

    @Override
    public String description() {
        return "Shows whose original language is not English must already be available; otherwise they are flagged.";
    }
}
