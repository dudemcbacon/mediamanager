package report.butt.mediamanager.validation;

import java.util.Objects;
import org.springframework.stereotype.Component;
import report.butt.mediamanager.model.RequestType;
import report.butt.mediamanager.model.TvRequest;

@Component
public class TvdbIdsMatch implements Validator<TvRequest> {
    @Override
    public Boolean validate(TvRequest request) {
        return Objects.equals(request.getTvdbId(), request.getPlexTvdbId());
    }

    @Override
    public RequestType supportedType() {
        return RequestType.TV;
    }

    @Override
    public int sortOrder() {
        return 500;
    }

    @Override
    public String shortName() {
        return "TVDB IDs?";
    }

    @Override
    public String description() {
        return "The TVDB ID from the request matches the TVDB ID of the Plex media.";
    }
}
