package report.butt.mediamanager.validation;

import java.util.Objects;
import org.springframework.stereotype.Component;
import report.butt.mediamanager.model.RequestType;
import report.butt.mediamanager.model.TvRequest;

@Component
public class AvailableInPlexTv implements Validator<TvRequest> {
    @Override
    public Boolean validate(TvRequest request) {
        Integer tvdbId = request.getTvdbId();
        Integer plexTvdbId = request.getPlexTvdbId();
        return tvdbId != null && Objects.equals(tvdbId, plexTvdbId);
    }

    @Override
    public RequestType supportedType() {
        return RequestType.TV;
    }

    @Override
    public int sortOrder() {
        return 300;
    }

    @Override
    public String shortName() {
        return "Plex?";
    }

    @Override
    public String description() {
        return "Show's Plex TVDB id matches its Ombi TVDB id, meaning it is present in the Plex library.";
    }
}
