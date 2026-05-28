package report.butt.mediamanager.repository;

import java.util.Optional;

import org.jspecify.annotations.NullMarked;
import org.springframework.data.jpa.repository.JpaRepository;

import report.butt.mediamanager.model.TvChildRequest;

@NullMarked
public interface TvChildRequestRepository extends JpaRepository<TvChildRequest, Long> {

  Optional<TvChildRequest> findByOmbiRequestId(Integer ombiRequestId);

}
