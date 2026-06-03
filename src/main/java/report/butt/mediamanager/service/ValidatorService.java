package report.butt.mediamanager.service;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import report.butt.mediamanager.model.MovieRequest;
import report.butt.mediamanager.model.Request;
import report.butt.mediamanager.model.RequestType;
import report.butt.mediamanager.model.TvEpisodeRequest;
import report.butt.mediamanager.model.TvRequest;
import report.butt.mediamanager.model.Validation;
import report.butt.mediamanager.repository.ValidationRepository;
import report.butt.mediamanager.validation.EpisodeValidator;
import report.butt.mediamanager.validation.Validator;

@Service
public class ValidatorService {

    private final Map<RequestType, List<Validator<? extends Request>>> validatorsByType;
    private final List<EpisodeValidator> episodeValidators;
    private final ValidationRepository validationRepository;
    private final TvHierarchyService tvHierarchyService;

    public ValidatorService(
            List<Validator<? extends Request>> validators,
            List<EpisodeValidator> episodeValidators,
            ValidationRepository validationRepository,
            TvHierarchyService tvHierarchyService) {
        this.validationRepository = validationRepository;
        this.validatorsByType = validators.stream()
                .collect(Collectors.groupingBy(
                        Validator::supportedType, () -> new EnumMap<>(RequestType.class), Collectors.toList()));
        this.episodeValidators = episodeValidators;
        this.tvHierarchyService = tvHierarchyService;
    }

    /** Validates a TV show and every episode beneath it (children → seasons → episodes). */
    public void validateWithEpisodes(TvRequest tvRequest) {
        validate(tvRequest);
        tvHierarchyService.loadEpisodes(tvRequest).forEach(this::validate);
    }

    public List<Validation> validate(Request request) {
        RequestType type = typeOf(request);
        List<Validator<? extends Request>> applicable = validatorsByType.getOrDefault(type, List.of());
        return applicable.stream()
                .map(validator -> {
                    String name = validator.getClass().getSimpleName();
                    Boolean result = runUnchecked(validator, request);
                    Validation validation = validationRepository
                            .findByRequestAndValidationName(request, name)
                            .orElseGet(() -> new Validation(name, result, request));
                    validation.setResult(result);
                    return validationRepository.save(validation);
                })
                .toList();
    }

    public List<Validation> validate(TvEpisodeRequest episode) {
        return episodeValidators.stream()
                .map(validator -> {
                    String name = validator.getClass().getSimpleName();
                    Boolean result = validator.validate(episode);
                    Validation validation = validationRepository
                            .findByTvEpisodeAndValidationName(episode, name)
                            .orElseGet(() -> new Validation(name, result, episode));
                    validation.setResult(result);
                    return validationRepository.save(validation);
                })
                .toList();
    }

    private RequestType typeOf(Request request) {
        if (request instanceof MovieRequest) {
            return RequestType.MOVIE;
        }
        if (request instanceof TvRequest) {
            return RequestType.TV;
        }
        throw new IllegalArgumentException(
                "Unknown Request subtype: " + request.getClass().getName());
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static Boolean runUnchecked(Validator validator, Request request) {
        return (Boolean) validator.validate(request);
    }
}
