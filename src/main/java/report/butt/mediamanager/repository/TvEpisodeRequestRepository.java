package report.butt.mediamanager.repository;

import java.util.Optional;

import org.jspecify.annotations.NullMarked;
import org.springframework.data.jpa.repository.JpaRepository;

import report.butt.mediamanager.model.TvEpisodeRequest;

@NullMarked
public interface TvEpisodeRequestRepository extends JpaRepository<TvEpisodeRequest, Long> {

  Optional<TvEpisodeRequest> findByTvSeasonRequestIdAndOmbiEpisodeNumber(Long tvSeasonRequestId,
      Integer ombiEpisodeNumber);

}
