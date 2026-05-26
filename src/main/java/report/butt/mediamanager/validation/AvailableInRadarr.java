package report.butt.mediamanager.validation;

import org.springframework.stereotype.Component;

import report.butt.mediamanager.model.MovieRequest;

@Component
public class AvailableInRadarr implements MovieValidator {
  @Override
  public Boolean validate(MovieRequest request) {
    return request.getRadarrRequestId() != null;
  }

  @Override
  public int sortOrder() {
    return 200;
  }

  @Override
  public String shortName() {
    return "Radarr?";
  }

  @Override
  public String description() {
    return "Movie has a Radarr request ID, meaning it is tracked in Radarr.";
  }
}
