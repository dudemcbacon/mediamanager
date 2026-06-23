package report.butt.mediamanager.exceptions;

import org.jspecify.annotations.NullMarked;

@NullMarked
public class RequestValidationException extends RuntimeException {
    public RequestValidationException(Long id) {
        super("Could not validate request " + id);
    }
}
