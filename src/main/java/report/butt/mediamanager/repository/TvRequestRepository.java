package report.butt.mediamanager.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.jspecify.annotations.NullMarked;
import org.springframework.data.jpa.repository.JpaRepository;
import report.butt.mediamanager.model.TvRequest;

@NullMarked
public interface TvRequestRepository extends JpaRepository<TvRequest, Long> {

    Optional<TvRequest> findByOmbiRequestId(Integer ombiRequestId);

    List<TvRequest> findByOmbiRequestIdIn(Collection<Integer> ombiRequestIds);
}
