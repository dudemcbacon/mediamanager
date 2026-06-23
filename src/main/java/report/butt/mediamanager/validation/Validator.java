package report.butt.mediamanager.validation;

import org.jspecify.annotations.NullMarked;
import report.butt.mediamanager.model.Request;
import report.butt.mediamanager.model.RequestType;

@NullMarked
public interface Validator<T extends Request> extends ValidationRule<T> {
    RequestType supportedType();
}
