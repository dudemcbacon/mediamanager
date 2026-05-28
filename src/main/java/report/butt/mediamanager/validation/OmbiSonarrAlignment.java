package report.butt.mediamanager.validation;

import org.springframework.stereotype.Component;

import report.butt.mediamanager.model.RequestType;
import report.butt.mediamanager.model.TvRequest;

@Component
public class OmbiSonarrAlignment implements Validator<TvRequest> {
  @Override
  public Boolean validate(TvRequest request) {
    Integer fileCount = request.getSonarrEpisodeFileCount();
    Integer totalCount = request.getSonarrTotalEpisodeCount();
    if (fileCount == null || totalCount == null || totalCount <= 0) {
      return false;
    }
    return fileCount >= totalCount
        && "Common.Available".equals(request.getOmbiRequestStatus());
  }

  @Override
  public RequestType supportedType() {
    return RequestType.TV;
  }

  @Override
  public int sortOrder() {
    return 400;
  }

  @Override
  public String shortName() {
    return "Alignment?";
  }

  @Override
  public String description() {
    return "Sonarr reports all episode files are downloaded and Ombi marks the request as available.";
  }
}
