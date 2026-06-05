package report.butt.mediamanager.validation;

import org.springframework.stereotype.Component;
import report.butt.mediamanager.model.MovieRequest;
import report.butt.mediamanager.model.RequestType;

@Component
public class NotInTvFolder implements Validator<MovieRequest> {
    @Override
    public Boolean validate(MovieRequest request) {
        String path = request.getRadarrPath();
        return path == null || !path.contains("/TV/");
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
        return "NotTV";
    }

    @Override
    public String title() {
        return "NotTV?";
    }

    @Override
    public String description() {
        return "Radarr path does not contain \"/TV/\", meaning the movie is not misfiled under a TV folder.";
    }
}
