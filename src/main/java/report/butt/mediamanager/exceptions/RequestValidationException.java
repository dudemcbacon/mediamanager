package report.butt.mediamanager.exceptions;

public class RequestValidationException extends RuntimeException {
    public RequestValidationException(Long id) {
        super("Could not validate request " + id);
    }
}
