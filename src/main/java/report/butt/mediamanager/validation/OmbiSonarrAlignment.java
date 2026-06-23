package report.butt.mediamanager.validation;

import java.util.Objects;
import org.jspecify.annotations.NullMarked;
import org.springframework.stereotype.Component;
import report.butt.mediamanager.model.RequestType;
import report.butt.mediamanager.model.TvRequest;

@Component
@NullMarked
public class OmbiSonarrAlignment implements Validator<TvRequest> {
    @Override
    public Boolean validate(TvRequest request) {
        Integer fileCount = request.getSonarrEpisodeFileCount();
        Integer episodeCount = request.getSonarrEpisodeCount();
        if (fileCount == null || episodeCount == null || episodeCount <= 0) {
            return false;
        }
        return fileCount >= episodeCount && Objects.equals(request.getOmbiRequestStatus(), "Common.Available");
    }

    @Override
    public RequestType supportedType() {
        return RequestType.TV;
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
        return "Sonarr reports all episode files are downloaded and Ombi marks the request as available.";
    }
}
