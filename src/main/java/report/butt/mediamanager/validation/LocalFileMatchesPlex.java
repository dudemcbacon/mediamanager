package report.butt.mediamanager.validation;

import org.springframework.stereotype.Component;
import report.butt.mediamanager.model.MovieRequest;
import report.butt.mediamanager.model.RequestType;

@Component
public class LocalFileMatchesPlex implements Validator<MovieRequest> {

    @Override
    public Boolean validate(MovieRequest request) {
        return Boolean.TRUE.equals(request.getLocalFilePathAvailable())
                && request.getLocalFileSize() != null
                && request.getLocalFileSize().equals(request.getPlexMediaSize());
    }

    @Override
    public RequestType supportedType() {
        return RequestType.MOVIE;
    }

    @Override
    public int sortOrder() {
        return 460;
    }

    @Override
    public String shortName() {
        return "Local";
    }

    @Override
    public String title() {
        return "Local File OK?";
    }

    @Override
    public String description() {
        return "The local file exists on disk and its size matches the Plex media file size.";
    }
}
