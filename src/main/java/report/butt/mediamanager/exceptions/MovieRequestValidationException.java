package report.butt.mediamanager.exceptions;

public class MovieRequestValidationException extends RuntimeException {
  public MovieRequestValidationException(Long id) {
    super("Could not validate movie request " + id);
  }
}
