package report.butt.mediamanager.validation;

import org.springframework.stereotype.Component;

import report.butt.mediamanager.model.MovieRequest;

@Component
public class NotInTvFolder implements MovieValidator {
  @Override
  public Boolean validate(MovieRequest request) {
    String path = request.getRadarrPath();
    return path == null || !path.contains("/TV/");
  }

  @Override
  public int sortOrder() {
    return 250;
  }

  @Override
  public String shortName() {
    return "NotTV?";
  }

  @Override
  public String description() {
    return "Radarr path does not contain \"/TV/\", meaning the movie is not misfiled under a TV folder.";
  }
}
