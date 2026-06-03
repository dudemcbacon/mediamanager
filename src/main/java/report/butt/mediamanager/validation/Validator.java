package report.butt.mediamanager.validation;

import report.butt.mediamanager.model.Request;
import report.butt.mediamanager.model.RequestType;

public interface Validator<T extends Request> extends ValidationRule<T> {
    RequestType supportedType();
}
