package report.butt.mediamanager.validation;

import report.butt.mediamanager.exceptions.MovieRequestValidationException;
import report.butt.mediamanager.model.MovieRequest;

public interface MovieValidator {
  Boolean validate(MovieRequest request) throws MovieRequestValidationException;

  int sortOrder();

  String shortName();

  String description();
}
