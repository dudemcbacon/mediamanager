package report.butt.mediamanager.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
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

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@NullMarked
class ValidatorServiceTest {

    private final ValidationRepository validationRepository = mock(ValidationRepository.class);
    private final TvHierarchyService tvHierarchyService = mock(TvHierarchyService.class);
    private final MovieRequestRepository movieRequestRepository = mock(MovieRequestRepository.class);
    private final TvRequestRepository tvRequestRepository = mock(TvRequestRepository.class);

    /**
     * Concrete movie validator with a stable class name so getSimpleName() returns "AlwaysTrueMovieValidator". Using
     * anonymous/mock classes would give Mockito-generated names that break Validation row matching.
     */
    static class AlwaysTrueMovieValidator implements Validator<MovieRequest> {
        @Override
        public RequestType supportedType() {
            return RequestType.MOVIE;
        }

        @Override
        public Boolean validate(MovieRequest target) {
            return true;
        }

        @Override
        public int sortOrder() {
            return 0;
        }

        @Override
        public String shortName() {
            return "atm";
        }

        @Override
        public String title() {
            return "Always True";
        }

        @Override
        public String description() {
            return "";
        }
    }

    /** A validator that always throws, used to verify one bad rule doesn't abort the sweep for an entity. */
    static class ThrowingMovieValidator implements Validator<MovieRequest> {
        @Override
        public RequestType supportedType() {
            return RequestType.MOVIE;
        }

        @Override
        public Boolean validate(MovieRequest target) {
            throw new IllegalStateException("boom");
        }

        @Override
        public int sortOrder() {
            return 0;
        }

        @Override
        public String shortName() {
            return "boom";
        }

        @Override
        public String title() {
            return "Throwing";
        }

        @Override
        public String description() {
            return "";
        }
    }

    static class AlwaysTrueTvValidator implements Validator<TvRequest> {
        @Override
        public RequestType supportedType() {
            return RequestType.TV;
        }

        @Override
        public Boolean validate(TvRequest target) {
            return true;
        }

        @Override
        public int sortOrder() {
            return 0;
        }

        @Override
        public String shortName() {
            return "att";
        }

        @Override
        public String title() {
            return "Always True TV";
        }

        @Override
        public String description() {
            return "";
        }
    }

    static class AlwaysTrueEpisodeValidator implements EpisodeValidator {
        @Override
        public Boolean validate(TvEpisodeRequest target) {
            return true;
        }

        @Override
        public int sortOrder() {
            return 0;
        }

        @Override
        public String shortName() {
            return "ate";
        }

        @Override
        public String title() {
            return "Always True Ep";
        }

        @Override
        public String description() {
            return "";
        }
    }

    private final AlwaysTrueMovieValidator movieValidator = new AlwaysTrueMovieValidator();
    private final AlwaysTrueTvValidator tvValidator = new AlwaysTrueTvValidator();
    private final AlwaysTrueEpisodeValidator episodeValidator = new AlwaysTrueEpisodeValidator();

    // The service keys validations by validator.getClass().getSimpleName(), so the stable inner class
    // names ("AlwaysTrueMovieValidator", "AlwaysTrueTvValidator", "AlwaysTrueEpisodeValidator") must
    // match exactly what we put into any pre-seeded Validation rows.
    private static final String MOVIE_VALIDATOR_NAME = "AlwaysTrueMovieValidator";

    @Test
    void validateRequestCreatesNewValidationWhenNoneExist() {
        when(validationRepository.findByRequest(any())).thenReturn(List.of());

        ValidatorService service = buildService(List.of(movieValidator), List.of());

        MovieRequest movie = movie("Film");
        List<Validation> result = service.validate(movie);

        assertEquals(1, result.size());
        assertTrue(result.get(0).getResult());
        verify(validationRepository).saveAll(anyList());
    }

    @Test
    void validateRequestReusesExistingValidationWhenResultUnchanged() {
        // AlwaysTrueMovieValidator.validate() always returns true; pre-seed a row with the same result
        var existing = new Validation(MOVIE_VALIDATOR_NAME, true, movie("Film"));
        when(validationRepository.findByRequest(any())).thenReturn(List.of(existing));

        ValidatorService service = buildService(List.of(movieValidator), List.of());
        MovieRequest movie = movie("Film");
        List<Validation> result = service.validate(movie);

        // Existing row reused; result unchanged → no save
        assertEquals(1, result.size());
        verify(validationRepository, never()).saveAll(anyList());
    }

    @Test
    void validateRequestUpdatesExistingWhenResultChanges() {
        // Pre-seed a row with result=false; AlwaysTrueMovieValidator returns true → result changed
        var existing = new Validation(MOVIE_VALIDATOR_NAME, false, movie("Film"));
        when(validationRepository.findByRequest(any())).thenReturn(List.of(existing));

        ValidatorService service = buildService(List.of(movieValidator), List.of());
        service.validate(movie("Film"));

        verify(validationRepository).saveAll(anyList());
    }

    @Test
    void validateEpisodeRunsEpisodeValidators() {
        when(validationRepository.findByTvEpisode(any())).thenReturn(List.of());

        ValidatorService service = buildService(List.of(), List.of(episodeValidator));
        TvEpisodeRequest episode = episode();
        List<Validation> result = service.validate(episode);

        assertEquals(1, result.size());
        verify(validationRepository).saveAll(anyList());
    }

    @Test
    void validateAllMoviesIteratesAllMovies() {
        when(validationRepository.findAll()).thenReturn(List.of());

        MovieRequest m1 = movie("M1");
        m1.setId(1L);
        MovieRequest m2 = movie("M2");
        m2.setId(2L);
        when(movieRequestRepository.findAll()).thenReturn(List.of(m1, m2));

        ValidatorService service = buildService(List.of(movieValidator), List.of());
        service.validateAllMovies();

        verify(validationRepository).saveAll(anyList());
    }

    @Test
    void validateAllMoviesDoesNotSaveWhenNothingChanged() {
        // AlwaysTrueMovieValidator always returns true. Pre-seed a row with result=true for the same movie.
        MovieRequest m = movie("M");
        m.setId(1L);
        var existing = new Validation(MOVIE_VALIDATOR_NAME, true, m);
        when(validationRepository.findAll()).thenReturn(List.of(existing));
        when(movieRequestRepository.findAll()).thenReturn(List.of(m));

        ValidatorService service = buildService(List.of(movieValidator), List.of());
        service.validateAllMovies();

        verify(validationRepository, never()).saveAll(anyList());
    }

    @Test
    void validateAllTvIteratesShowsAndEpisodes() {
        when(validationRepository.findAll()).thenReturn(List.of());

        TvRequest show = tvShow("Show");
        show.setId(10L);
        TvEpisodeRequest ep = episode();
        ep.setId(100L);
        when(tvRequestRepository.findAll()).thenReturn(List.of(show));
        when(tvHierarchyService.loadEpisodesByRequestId()).thenReturn(Map.of(10L, List.of(ep)));

        ValidatorService service = buildService(List.of(tvValidator), List.of(episodeValidator));
        service.validateAllTv();

        verify(validationRepository).saveAll(anyList());
    }

    @Test
    void validateWithEpisodesRunsBothRequestAndEpisodeValidation() {
        when(validationRepository.findByRequest(any())).thenReturn(List.of());
        when(validationRepository.findByTvEpisode(any())).thenReturn(List.of());

        TvRequest show = tvShow("Show");
        TvEpisodeRequest ep = episode();
        when(tvHierarchyService.loadEpisodes(show)).thenReturn(List.of(ep));

        ValidatorService service = buildService(List.of(tvValidator), List.of(episodeValidator));
        service.validateWithEpisodes(show);

        // Both request and episode validators ran → saveAll called at least once
        verify(validationRepository, Mockito.atLeast(1)).saveAll(anyList());
    }

    @Test
    void throwingValidatorIsSkippedAndDoesNotBlockOthers() {
        when(validationRepository.findByRequest(any())).thenReturn(List.of());

        ValidatorService service = buildService(List.of(new ThrowingMovieValidator(), movieValidator), List.of());

        MovieRequest movie = movie("Film");
        List<Validation> result = service.validate(movie);

        // The throwing rule is skipped; the healthy rule that runs after it still produces its result.
        assertEquals(1, result.size());
        assertEquals(MOVIE_VALIDATOR_NAME, result.get(0).getValidationName());
        assertTrue(result.get(0).getResult());
        verify(validationRepository).saveAll(anyList());
    }

    @Test
    void validateMovieWithNoValidatorsReturnsEmptyList() {
        when(validationRepository.findByRequest(any())).thenReturn(List.of());

        ValidatorService service = buildService(List.of(), List.of());
        MovieRequest movie = movie("X");
        List<Validation> result = service.validate(movie);
        assertTrue(result.isEmpty());
    }

    // --- helpers ---

    private ValidatorService buildService(
            List<Validator<? extends Request>> validators, List<EpisodeValidator> episodeValidators) {
        return new ValidatorService(
                validators,
                episodeValidators,
                validationRepository,
                tvHierarchyService,
                movieRequestRepository,
                tvRequestRepository);
    }

    private static MovieRequest movie(String title) {
        var m = new MovieRequest(title, 1, false, 1, "Common.ProcessingRequest");
        return m;
    }

    private static TvRequest tvShow(String title) {
        return new TvRequest(title, 100, false, 1, "Common.ProcessingRequest");
    }

    private static TvEpisodeRequest episode() {
        return new TvEpisodeRequest(null, 1, 1);
    }
}
