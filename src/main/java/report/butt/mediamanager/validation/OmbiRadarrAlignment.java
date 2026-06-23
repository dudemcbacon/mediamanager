package report.butt.mediamanager.validation;

import java.util.Objects;
import org.jspecify.annotations.NullMarked;
import org.springframework.stereotype.Component;
import report.butt.mediamanager.model.MovieRequest;
import report.butt.mediamanager.model.RequestType;

@Component
@NullMarked
public class OmbiRadarrAlignment implements Validator<MovieRequest> {
    @Override
    public Boolean validate(MovieRequest request) {
        return Objects.equals(request.getRadarrHasFile(), true)
                && Objects.equals(request.getOmbiRequestStatus(), "Common.Available");
    }

    @Override
    public RequestType supportedType() {
        return RequestType.MOVIE;
    }

    @Override
    public int sortOrder() {
        return 400;
    }

    @Override
    public String shortName() {
        return "Align";
    }

    @Override
    public String title() {
        return "Alignment?";
    }

    @Override
    public String description() {
        return "Radarr reports the file is downloaded and Ombi marks the request as available.";
    }
}
