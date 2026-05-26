package report.butt.mediamanager.validation;

import java.util.Objects;

import org.springframework.stereotype.Component;

import report.butt.mediamanager.model.MovieRequest;

@Component
public class TmdbIdsMatch implements MovieValidator {
  @Override
  public Boolean validate(MovieRequest request) {
    return Objects.equals(request.getTmdbid(), request.getPlexTmdbid());
  }

  @Override
  public int sortOrder() {
    return 500;
  }

  @Override
  public String shortName() {
    return "TMDB IDs?";
  }

  @Override
  public String description() {
    return "The TMDB ID from the request matches the TMDB ID of the Plex media.";
  }
}
