package report.butt.mediamanager.validation;

import org.jspecify.annotations.NullMarked;

@NullMarked
public interface ValidationRule<T> {
    Boolean validate(T target);

    int sortOrder();

    String shortName();

    String title();

    String description();
}
