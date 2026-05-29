package report.butt.mediamanager.validation;

import org.springframework.stereotype.Component;
import report.butt.mediamanager.model.RequestType;
import report.butt.mediamanager.model.TvRequest;
import report.butt.mediamanager.repository.TvSeasonRequestRepository;

@Component
public class OmbiSeasonCountAlignment implements Validator<TvRequest> {

    private final TvSeasonRequestRepository seasonRepository;

    public OmbiSeasonCountAlignment(TvSeasonRequestRepository seasonRepository) {
        this.seasonRepository = seasonRepository;
    }

    @Override
    public Boolean validate(TvRequest request) {
        Integer expected = request.getOmbiTotalSeasons();
        if (expected == null) {
            return false;
        }
        long actual = seasonRepository.countByTvChildRequestParent(request);
        return expected.longValue() <= actual;
    }

    @Override
    public RequestType supportedType() {
        return RequestType.TV;
    }

    @Override
    public int sortOrder() {
        return 700;
    }

    @Override
    public String shortName() {
        return "Seasons?";
    }

    @Override
    public String description() {
        return "Ombi's reported total seasons matches the number of TvSeasonRequests recorded for the show.";
    }
}
