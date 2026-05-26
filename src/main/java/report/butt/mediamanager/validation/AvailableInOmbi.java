package report.butt.mediamanager.validation;

import org.springframework.stereotype.Component;

import report.butt.mediamanager.model.MovieRequest;

@Component
public class AvailableInOmbi implements MovieValidator {
  @Override
  public Boolean validate(MovieRequest request) {
    return request.getOmbiRequestId() != null;
  }

  @Override
  public int sortOrder() {
    return 100;
  }

  @Override
  public String shortName() {
    return "Ombi?";
  }

  @Override
  public String description() {
    return "Movie has an Ombi request ID, meaning it is tracked in Ombi.";
  }
}
