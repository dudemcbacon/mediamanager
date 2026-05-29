package report.butt.mediamanager.validation;

import report.butt.mediamanager.exceptions.RequestValidationException;
import report.butt.mediamanager.model.Request;
import report.butt.mediamanager.model.RequestType;

public interface Validator<T extends Request> {
    Boolean validate(T request) throws RequestValidationException;

    RequestType supportedType();

    int sortOrder();

    String shortName();

    String description();
}
