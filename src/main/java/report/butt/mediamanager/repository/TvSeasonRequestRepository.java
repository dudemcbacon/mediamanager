package report.butt.mediamanager.repository;

import java.util.Optional;

import org.jspecify.annotations.NullMarked;
import org.springframework.data.jpa.repository.JpaRepository;

import report.butt.mediamanager.model.TvSeasonRequest;

@NullMarked
public interface TvSeasonRequestRepository extends JpaRepository<TvSeasonRequest, Long> {

  Optional<TvSeasonRequest> findByTvChildRequestIdAndOmbiSeasonNumber(Long tvChildRequestId,
      Integer ombiSeasonNumber);

}
