package report.butt.mediamanager.validation;

import org.jspecify.annotations.NullMarked;
import org.springframework.stereotype.Component;
import report.butt.mediamanager.model.MovieRequest;
import report.butt.mediamanager.model.RequestType;

@Component
@NullMarked
public class EnglishOrAvailable implements Validator<MovieRequest> {
    @Override
    public Boolean validate(MovieRequest request) {
        String language = request.getRadarrOriginalLanguage();
        if (language == null || "English".equalsIgnoreCase(language)) {
            return true;
        }
        return request.isAvailable();
    }

    @Override
    public RequestType supportedType() {
        return RequestType.MOVIE;
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
        return "Movies whose original language is not English must already be available; otherwise they are flagged.";
    }
}
