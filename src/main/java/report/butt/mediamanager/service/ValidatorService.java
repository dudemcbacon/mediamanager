package report.butt.mediamanager.service;

import com.newrelic.api.agent.Trace;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import report.butt.mediamanager.model.MovieRequest;
import report.butt.mediamanager.model.Request;
import report.butt.mediamanager.model.RequestType;
import report.butt.mediamanager.model.TvEpisodeRequest;
import report.butt.mediamanager.model.TvRequest;
import report.butt.mediamanager.model.Validation;
import report.butt.mediamanager.repository.MovieRequestRepository;
import report.butt.mediamanager.repository.TvRequestRepository;
import report.butt.mediamanager.repository.ValidationRepository;
import report.butt.mediamanager.validation.EpisodeValidator;
import report.butt.mediamanager.validation.Validator;

@Service
// Member collections are populated once at construction and never mutated; immutable types are unnecessary.
@SuppressWarnings("ImmutableMemberCollection")
@NullMarked
public class ValidatorService {

    private static final Logger log = LoggerFactory.getLogger(ValidatorService.class);

    private final Map<RequestType, List<Validator<? extends Request>>> validatorsByType;
    private final List<EpisodeValidator> episodeValidators;
    private final ValidationRepository validationRepository;
    private final TvHierarchyService tvHierarchyService;
    private final MovieRequestRepository movieRequestRepository;
    private final TvRequestRepository tvRequestRepository;

    public ValidatorService(
            List<Validator<? extends Request>> validators,
            List<EpisodeValidator> episodeValidators,
            ValidationRepository validationRepository,
            TvHierarchyService tvHierarchyService,
            MovieRequestRepository movieRequestRepository,
            TvRequestRepository tvRequestRepository) {
        this.validationRepository = validationRepository;
        this.validatorsByType = validators.stream()
                .collect(Collectors.groupingBy(
                        Validator::supportedType, () -> new EnumMap<>(RequestType.class), Collectors.toList()));
        this.episodeValidators = episodeValidators;
        this.tvHierarchyService = tvHierarchyService;
        this.movieRequestRepository = movieRequestRepository;
        this.tvRequestRepository = tvRequestRepository;
    }

    /** Validates a TV show and every episode beneath it (children → seasons → episodes). */
    @Transactional
    public void validateWithEpisodes(TvRequest tvRequest) {
        validate(tvRequest);
        tvHierarchyService.loadEpisodes(tvRequest).forEach(this::validate);
    }

    /**
     * Validates every movie request in one transaction, preloading all existing validations up front so the whole sweep
     * costs one read of the validation table plus a single batched write of only the rows whose result changed —
     * instead of a read and a write per (movie × validator).
     */
    @Transactional
    @Trace
    public void validateAllMovies() {
        List<MovieRequest> movies = movieRequestRepository.findAll();
        Map<Long, Map<String, Validation>> existingByRequestId = new HashMap<>();
        indexValidations(existingByRequestId, new HashMap<>());

        List<Validation> toSave = new ArrayList<>();
        for (MovieRequest movie : movies) {
            collectRequestValidations(movie, existingByRequestId.getOrDefault(movie.getId(), Map.of()), toSave);
        }
        if (!toSave.isEmpty()) {
            validationRepository.saveAll(toSave);
        }
        log.info("Validated {} movies: {} validations changed", movies.size(), toSave.size());
    }

    /**
     * Validates every TV show and every episode beneath it in one transaction. The episode hierarchy is loaded in three
     * bulk queries (not one lazy load per show) and all existing validations are preloaded once, so the sweep costs two
     * reads plus a single batched write of only the changed rows — instead of a read and a write per (show/episode ×
     * validator).
     */
    @Transactional
    @Trace
    public void validateAllTv() {
        List<TvRequest> tvRequests = tvRequestRepository.findAll();
        Map<Long, List<TvEpisodeRequest>> episodesByRequestId = tvHierarchyService.loadEpisodesByRequestId();
        Map<Long, Map<String, Validation>> existingByRequestId = new HashMap<>();
        Map<Long, Map<String, Validation>> existingByEpisodeId = new HashMap<>();
        indexValidations(existingByRequestId, existingByEpisodeId);

        List<Validation> toSave = new ArrayList<>();
        for (TvRequest tvRequest : tvRequests) {
            collectRequestValidations(tvRequest, existingByRequestId.getOrDefault(tvRequest.getId(), Map.of()), toSave);
            for (TvEpisodeRequest episode : episodesByRequestId.getOrDefault(tvRequest.getId(), List.of())) {
                collectEpisodeValidations(episode, existingByEpisodeId.getOrDefault(episode.getId(), Map.of()), toSave);
            }
        }
        if (!toSave.isEmpty()) {
            validationRepository.saveAll(toSave);
        }
        log.info("Validated {} TV shows: {} validations changed", tvRequests.size(), toSave.size());
    }

    @Transactional
    public List<Validation> validate(Request request) {
        Map<String, Validation> existing = indexByName(validationRepository.findByRequest(request));
        List<Validation> toSave = new ArrayList<>();
        List<Validation> all = collectRequestValidations(request, existing, toSave);
        if (!toSave.isEmpty()) {
            validationRepository.saveAll(toSave);
        }
        return all;
    }

    @Transactional
    public List<Validation> validate(TvEpisodeRequest episode) {
        Map<String, Validation> existing = indexByName(validationRepository.findByTvEpisode(episode));
        List<Validation> toSave = new ArrayList<>();
        List<Validation> all = collectEpisodeValidations(episode, existing, toSave);
        if (!toSave.isEmpty()) {
            validationRepository.saveAll(toSave);
        }
        return all;
    }

    /**
     * Runs every applicable validator against the request, reusing the preloaded {@code existingByName} rows. Appends
     * only new or result-changed validations to {@code toSave} (unchanged results are left untouched, saving a write),
     * and returns the full set of validations for the request.
     */
    private List<Validation> collectRequestValidations(
            Request request, Map<String, Validation> existingByName, List<Validation> toSave) {
        RequestType type = typeOf(request);
        List<Validator<? extends Request>> applicable = validatorsByType.getOrDefault(type, List.of());
        List<Validation> all = new ArrayList<>(applicable.size());
        for (Validator<? extends Request> validator : applicable) {
            String name = validator.getClass().getSimpleName();
            Boolean result = runUnchecked(validator, request);
            all.add(reconcile(existingByName.get(name), result, toSave, () -> new Validation(name, result, request)));
        }
        return all;
    }

    private List<Validation> collectEpisodeValidations(
            TvEpisodeRequest episode, Map<String, Validation> existingByName, List<Validation> toSave) {
        List<Validation> all = new ArrayList<>(episodeValidators.size());
        for (EpisodeValidator validator : episodeValidators) {
            String name = validator.getClass().getSimpleName();
            Boolean result = validator.validate(episode);
            all.add(reconcile(existingByName.get(name), result, toSave, () -> new Validation(name, result, episode)));
        }
        return all;
    }

    /**
     * Returns the validation to record for one (entity, validator): the existing row when present (updating its result
     * and queuing a write only if the result changed) or a freshly created row (always queued). The supplier defers
     * construction so it only runs when there is no existing row.
     */
    private static Validation reconcile(
            @Nullable Validation existing, Boolean result, List<Validation> toSave, Supplier<Validation> create) {
        if (existing == null) {
            Validation created = create.get();
            toSave.add(created);
            return created;
        }
        if (!Objects.equals(existing.getResult(), result)) {
            existing.setResult(result);
            toSave.add(existing);
        }
        return existing;
    }

    /** Partitions every persisted validation into request-owned and episode-owned, keyed by owner id then name. */
    private void indexValidations(
            Map<Long, Map<String, Validation>> byRequestId, Map<Long, Map<String, Validation>> byEpisodeId) {
        for (Validation v : validationRepository.findAll()) {
            if (v.getRequest() != null) {
                byRequestId
                        .computeIfAbsent(v.getRequest().getId(), k -> new HashMap<>())
                        .put(v.getValidationName(), v);
            } else if (v.getTvEpisode() != null) {
                byEpisodeId
                        .computeIfAbsent(v.getTvEpisode().getId(), k -> new HashMap<>())
                        .put(v.getValidationName(), v);
            }
        }
    }

    private static Map<String, Validation> indexByName(List<Validation> validations) {
        Map<String, Validation> byName = new HashMap<>(validations.size());
        for (Validation v : validations) {
            byName.put(v.getValidationName(), v);
        }
        return byName;
    }

    private static RequestType typeOf(Request request) {
        if (request instanceof MovieRequest) {
            return RequestType.MOVIE;
        }
        if (request instanceof TvRequest) {
            return RequestType.TV;
        }
        throw new IllegalArgumentException(
                "Unknown Request subtype: " + request.getClass().getName());
    }

    // Safe: validators are grouped by supportedType() in validatorsByType, so each Validator is only ever invoked here
    // with a Request of the type it declares it supports; the raw validate(...) call and Boolean cast cannot mismatch.
    @SuppressWarnings({"unchecked", "rawtypes"})
    private static Boolean runUnchecked(Validator validator, Request request) {
        return (Boolean) validator.validate(request);
    }
}
