package report.butt.mediamanager.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import report.butt.mediamanager.model.Request;
import report.butt.mediamanager.model.Validation;

public interface ValidationRepository extends JpaRepository<Validation, Long> {
  Optional<Validation> findByRequestAndValidationName(Request request, String validationName);

  @Transactional
  void deleteByRequest(Request request);
}
