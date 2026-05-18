package report.butt.mediamanager.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import report.butt.mediamanager.model.MovieRequest;

public interface MovieRequestRepository extends JpaRepository<MovieRequest, Long> {

}
