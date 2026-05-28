package report.butt.mediamanager.validation;

import org.springframework.stereotype.Component;

import report.butt.mediamanager.model.RequestType;
import report.butt.mediamanager.model.TvRequest;

@Component
public class AvailableInPlexTv implements Validator<TvRequest> {
  @Override
  public Boolean validate(TvRequest request) {
    return request.getPlexMediaId() != null;
  }

  @Override
  public RequestType supportedType() {
    return RequestType.TV;
  }

  @Override
  public int sortOrder() {
    return 300;
  }

  @Override
  public String shortName() {
    return "Plex?";
  }

  @Override
  public String description() {
    return "Show has a Plex media ID, meaning it is present in the Plex library.";
  }
}
