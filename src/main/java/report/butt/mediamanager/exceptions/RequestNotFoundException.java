package report.butt.mediamanager.exceptions;

import org.jspecify.annotations.NullMarked;

@NullMarked
public class RequestNotFoundException extends RuntimeException {

    public RequestNotFoundException(Long id) {
        super("Could not find request " + id);
    }
}
