package report.butt.mediamanager.validation;

import java.util.Objects;
import org.jspecify.annotations.NullMarked;
import org.springframework.stereotype.Component;
import report.butt.mediamanager.model.MovieRequest;
import report.butt.mediamanager.model.RequestType;

@Component
@NullMarked
public class RadarrHasFile implements Validator<MovieRequest> {
    @Override
    public Boolean validate(MovieRequest request) {
        return Objects.equals(request.getRadarrHasFile(), true);
    }

    @Override
    public RequestType supportedType() {
        return RequestType.MOVIE;
    }

    @Override
    public int sortOrder() {
        return 250;
    }

    @Override
    public String shortName() {
        return "File";
    }

    @Override
    public String title() {
        return "Radarr File?";
    }

    @Override
    public String description() {
        return "Movie has a downloaded file in Radarr.";
    }
}
