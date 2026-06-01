package report.butt.mediamanager.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.jspecify.annotations.NullMarked;
import org.springframework.data.jpa.repository.JpaRepository;
import report.butt.mediamanager.model.TvChildRequest;
import report.butt.mediamanager.model.TvRequest;

@NullMarked
public interface TvChildRequestRepository extends JpaRepository<TvChildRequest, Long> {

    Optional<TvChildRequest> findByOmbiRequestId(Integer ombiRequestId);

    List<TvChildRequest> findByOmbiRequestIdIn(Collection<Integer> ombiRequestIds);

    List<TvChildRequest> findByParentOrderByIdAsc(TvRequest parent);
}
