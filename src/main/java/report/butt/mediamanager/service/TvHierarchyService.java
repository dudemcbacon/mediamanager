package report.butt.mediamanager.service;

import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import report.butt.mediamanager.model.TvChildRequest;
import report.butt.mediamanager.model.TvEpisodeRequest;
import report.butt.mediamanager.model.TvRequest;
import report.butt.mediamanager.model.TvSeasonRequest;
import report.butt.mediamanager.repository.TvChildRequestRepository;

@Service
public class TvHierarchyService {

    private static final Comparator<TvSeasonRequest> SEASON_ORDER =
            Comparator.comparing(TvSeasonRequest::getOmbiSeasonNumber, Comparator.nullsLast(Integer::compare));

    private static final Comparator<TvEpisodeRequest> EPISODE_ORDER =
            Comparator.comparing(TvEpisodeRequest::getOmbiEpisodeNumber, Comparator.nullsLast(Integer::compare));

    private final TvChildRequestRepository tvChildRequestRepository;

    public TvHierarchyService(TvChildRequestRepository tvChildRequestRepository) {
        this.tvChildRequestRepository = tvChildRequestRepository;
    }

    @Transactional(readOnly = true)
    public List<TvChildRequest> loadHierarchy(TvRequest parent) {
        List<TvChildRequest> children = tvChildRequestRepository.findByParentOrderByIdAsc(parent);
        for (TvChildRequest child : children) {
            List<TvSeasonRequest> seasons = child.getSeasonRequests();
            seasons.sort(SEASON_ORDER);
            for (TvSeasonRequest season : seasons) {
                season.getEpisodeRequests().sort(EPISODE_ORDER);
            }
        }
        return children;
    }
}
