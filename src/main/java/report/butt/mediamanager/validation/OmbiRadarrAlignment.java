package report.butt.mediamanager.validation;

import org.springframework.stereotype.Component;

import report.butt.mediamanager.model.MovieRequest;

@Component
public class OmbiRadarrAlignment implements MovieValidator {
  @Override
  public Boolean validate(MovieRequest request) {
    return request.getRadarrHasFile() && request.getOmbiRequestStatus().equals("Common.Available");
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
    return "Radarr reports the file is downloaded and Ombi marks the request as available.";
  }
}
