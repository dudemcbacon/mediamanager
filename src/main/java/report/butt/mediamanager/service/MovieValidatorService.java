package report.butt.mediamanager.service;

import java.util.List;

import org.springframework.stereotype.Service;

import report.butt.mediamanager.model.MovieRequest;
import report.butt.mediamanager.model.Validation;
import report.butt.mediamanager.repository.ValidationRepository;
import report.butt.mediamanager.validation.MovieValidator;

@Service
public class MovieValidatorService {

  private final List<MovieValidator> validators;
  private final ValidationRepository validationRepository;

  public MovieValidatorService(List<MovieValidator> validators, ValidationRepository validationRepository) {
    this.validators = validators;
    this.validationRepository = validationRepository;
  }

  public List<Validation> validate(MovieRequest movieRequest) {
    return validators.stream()
        .map(validator -> {
          String name = validator.getClass().getSimpleName();
          Boolean result = validator.validate(movieRequest);
          Validation validation = validationRepository
              .findByMovieRequestAndValidationName(movieRequest, name)
              .orElseGet(() -> new Validation(name, result, movieRequest));
          validation.setResult(result);
          return validationRepository.save(validation);
        })
        .toList();
  }
}
