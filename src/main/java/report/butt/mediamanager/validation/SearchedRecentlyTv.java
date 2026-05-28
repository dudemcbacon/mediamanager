package report.butt.mediamanager.validation;

import java.time.Duration;
import java.time.Instant;

import org.springframework.stereotype.Component;

import report.butt.mediamanager.model.RequestType;
import report.butt.mediamanager.model.TvRequest;

@Component
public class SearchedRecentlyTv implements Validator<TvRequest> {

  private static final Duration ONE_WEEK = Duration.ofDays(7);

  @Override
  public Boolean validate(TvRequest request) {
    if (request.isAvailable()) {
      return true;
    }
    Instant lastSearched = request.getSonarrLastSearched();
    if (lastSearched == null) {
      return false;
    }
    return lastSearched.isAfter(Instant.now().minus(ONE_WEEK));
  }

  @Override
  public RequestType supportedType() {
    return RequestType.TV;
  }

  @Override
  public int sortOrder() {
    return 600;
  }

  @Override
  public String shortName() {
    return "Recent Search?";
  }

  @Override
  public String description() {
    return "Show is available, or Sonarr searched for it within the last week.";
  }
}
