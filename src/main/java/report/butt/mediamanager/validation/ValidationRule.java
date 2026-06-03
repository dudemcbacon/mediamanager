package report.butt.mediamanager.validation;

import report.butt.mediamanager.exceptions.RequestValidationException;

public interface ValidationRule<T> {
    Boolean validate(T target) throws RequestValidationException;

    int sortOrder();

    String shortName();

    String description();
}
