package report.butt.mediamanager.validation;

import org.springframework.stereotype.Component;
import report.butt.mediamanager.model.MovieRequest;
import report.butt.mediamanager.model.RequestType;

@Component
public class OmbiRadarrAlignment implements Validator<MovieRequest> {
    @Override
    public Boolean validate(MovieRequest request) {
        return Boolean.TRUE.equals(request.getRadarrHasFile())
                && "Common.Available".equals(request.getOmbiRequestStatus());
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
        return "Alignment?";
    }

    @Override
    public String description() {
        return "Radarr reports the file is downloaded and Ombi marks the request as available.";
    }
}
