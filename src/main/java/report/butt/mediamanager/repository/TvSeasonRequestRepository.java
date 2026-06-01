package report.butt.mediamanager.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.jspecify.annotations.NullMarked;
import org.springframework.data.jpa.repository.JpaRepository;
import report.butt.mediamanager.model.TvRequest;
import report.butt.mediamanager.model.TvSeasonRequest;

@NullMarked
public interface TvSeasonRequestRepository extends JpaRepository<TvSeasonRequest, Long> {

    Optional<TvSeasonRequest> findByTvChildRequestIdAndOmbiSeasonNumber(
            Long tvChildRequestId, Integer ombiSeasonNumber);

    List<TvSeasonRequest> findByTvChildRequestIdIn(Collection<Long> tvChildRequestIds);

    long countByTvChildRequestParent(TvRequest parent);
}
