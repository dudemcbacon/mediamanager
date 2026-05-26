package report.butt.mediamanager.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import report.butt.mediamanager.model.MovieRequest;
import report.butt.mediamanager.model.Validation;

public interface ValidationRepository extends JpaRepository<Validation, Long> {
  Optional<Validation> findByMovieRequestAndValidationName(MovieRequest movieRequest, String validationName);
}
