package report.butt.mediamanager.repository;

import java.util.Optional;

import org.jspecify.annotations.NullMarked;
import org.springframework.data.jpa.repository.JpaRepository;

import report.butt.mediamanager.model.MovieRequest;

@NullMarked
public interface MovieRequestRepository extends JpaRepository<MovieRequest, Long> {

  Optional<MovieRequest> findByOmbiRequestId(Integer ombiRequestId);

}
