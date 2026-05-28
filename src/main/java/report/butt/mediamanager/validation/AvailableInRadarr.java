package report.butt.mediamanager.validation;

import org.springframework.stereotype.Component;

import report.butt.mediamanager.model.MovieRequest;
import report.butt.mediamanager.model.RequestType;

@Component
public class AvailableInRadarr implements Validator<MovieRequest> {
  @Override
  public Boolean validate(MovieRequest request) {
    return request.getRadarrRequestId() != null;
  }

  @Override
  public RequestType supportedType() {
    return RequestType.MOVIE;
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
