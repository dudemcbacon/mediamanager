package report.butt.mediamanager.validation;

public interface ValidationRule<T> {
    Boolean validate(T target);

    int sortOrder();

    String shortName();

    String title();

    String description();
}
