package report.butt.mediamanager.validation;

import java.util.Objects;

import org.springframework.stereotype.Component;

import report.butt.mediamanager.model.MovieRequest;
import report.butt.mediamanager.model.RequestType;

@Component
public class PathsMatch implements Validator<MovieRequest> {

    private static final String MNT_PREFIX = "/mnt";

    @Override
    public Boolean validate(MovieRequest request) {
        return Objects.equals(stripMnt(request.getPlexMediaFilename()), stripMnt(request.getRadarrMovieFilePath()));
    }

    private static String stripMnt(String path) {
        if (path == null) {
            return null;
        }
        if (path.startsWith(MNT_PREFIX)) {
            return path.substring(MNT_PREFIX.length());
        }
        return path;
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
        return "Paths Match?";
    }

    @Override
    public String description() {
        return "The Plex media filename matches the Radarr path (ignoring a leading /mnt on either side).";
    }
}
