package report.butt.mediamanager.validation;

import java.util.Objects;
import org.jspecify.annotations.NullMarked;
import org.springframework.stereotype.Component;
import report.butt.mediamanager.model.MovieRequest;
import report.butt.mediamanager.model.RequestType;

@Component
@NullMarked
public class PathsMatch implements Validator<MovieRequest> {

    @Override
    public Boolean validate(MovieRequest request) {
        return Objects.equals(
                ValidationSupport.stripMnt(request.getPlexMediaFilename()),
                ValidationSupport.stripMnt(request.getRadarrMovieFilePath()));
    }

    @Override
    public RequestType supportedType() {
        return RequestType.MOVIE;
    }

    @Override
    public int sortOrder() {
        return 450;
    }

    @Override
    public String shortName() {
        return "Paths";
    }

    @Override
    public String title() {
        return "Paths Match?";
    }

    @Override
    public String description() {
        return "The Plex media filename matches the Radarr path (ignoring a leading /mnt on either side).";
    }
}
