package report.butt.mediamanager.exceptions;

public class MovieRequestNotFoundException extends RuntimeException {

  public MovieRequestNotFoundException(Long id) {
    super("Could not find movie request " + id);
  }
}
