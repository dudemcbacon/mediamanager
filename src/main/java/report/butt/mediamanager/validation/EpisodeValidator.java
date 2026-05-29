package report.butt.mediamanager.validation;

import report.butt.mediamanager.exceptions.RequestValidationException;
import report.butt.mediamanager.model.TvEpisodeRequest;

public interface EpisodeValidator {
    Boolean validate(TvEpisodeRequest episode) throws RequestValidationException;

    int sortOrder();

    String shortName();

    String description();
}
