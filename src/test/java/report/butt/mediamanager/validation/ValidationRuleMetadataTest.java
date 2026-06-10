package report.butt.mediamanager.validation;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import java.util.List;
import org.junit.jupiter.api.Test;
import report.butt.mediamanager.model.RequestType;
import report.butt.mediamanager.repository.TvSeasonRequestRepository;

/**
 * Exercises the metadata methods (sortOrder, shortName, title, description, supportedType) on every
 * ValidationRule/Validator implementation to drive instruction coverage for those methods.
 */
class ValidationRuleMetadataTest {

    @Test
    void allMovieValidatorsExposeConsistentMetadata() {
        TvSeasonRequestRepository repo = mock(TvSeasonRequestRepository.class);

        List<Validator<?>> movieValidators = List.of(
                new AvailableInOmbi(),
                new AvailableInPlex(),
                new AvailableInRadarr(),
                new RadarrHasFile(),
                new EnglishOrAvailable(),
                new PathsMatch(),
                new TmdbIdsMatch(),
                new OmbiRadarrAlignment(),
                new QualityProfileAnyOrAvailable(),
                new SearchedRecently(),
                new NotInTvFolder());

        for (Validator<?> rule : movieValidators) {
            assertTrue(rule.sortOrder() > 0, rule.getClass().getSimpleName() + ".sortOrder()");
            assertNotNull(rule.shortName(), rule.getClass().getSimpleName() + ".shortName()");
            assertNotNull(rule.title(), rule.getClass().getSimpleName() + ".title()");
            assertNotNull(rule.description(), rule.getClass().getSimpleName() + ".description()");
            assertTrue(rule.shortName().length() > 0);
            assertTrue(rule.title().length() > 0);
            assertTrue(rule.description().length() > 0);
            assertNotNull(rule.supportedType());
        }
    }

    @Test
    void allTvValidatorsExposeConsistentMetadata() {
        TvSeasonRequestRepository repo = mock(TvSeasonRequestRepository.class);

        List<Validator<?>> tvValidators = List.of(
                new AvailableInOmbiTv(),
                new AvailableInSonarr(),
                new AvailableInPlexTv(),
                new TvdbIdsMatch(),
                new EnglishOrAvailableTv(),
                new SonarrMonitored(),
                new SearchedRecentlyTv(),
                new OmbiSonarrAlignment(),
                new OmbiSeasonCountAlignment(repo));

        for (Validator<?> rule : tvValidators) {
            assertTrue(rule.sortOrder() > 0, rule.getClass().getSimpleName() + ".sortOrder()");
            assertNotNull(rule.shortName(), rule.getClass().getSimpleName() + ".shortName()");
            assertNotNull(rule.title(), rule.getClass().getSimpleName() + ".title()");
            assertNotNull(rule.description(), rule.getClass().getSimpleName() + ".description()");
            assertTrue(rule.shortName().length() > 0);
            assertTrue(rule.title().length() > 0);
            assertTrue(rule.description().length() > 0);
            assertNotNull(rule.supportedType());
        }
    }

    @Test
    void allEpisodeValidatorsExposeConsistentMetadata() {
        List<EpisodeValidator> episodeValidators = List.of(new EpisodePathsMatch(), new EpisodeSearchedRecently());

        for (EpisodeValidator rule : episodeValidators) {
            assertTrue(rule.sortOrder() > 0, rule.getClass().getSimpleName() + ".sortOrder()");
            assertNotNull(rule.shortName(), rule.getClass().getSimpleName() + ".shortName()");
            assertNotNull(rule.title(), rule.getClass().getSimpleName() + ".title()");
            assertNotNull(rule.description(), rule.getClass().getSimpleName() + ".description()");
        }
    }

    @Test
    void movieValidatorsSupportedTypeIsMovie() {
        TvSeasonRequestRepository repo = mock(TvSeasonRequestRepository.class);
        List<Validator<?>> movieValidators = List.of(
                new AvailableInOmbi(),
                new AvailableInPlex(),
                new AvailableInRadarr(),
                new RadarrHasFile(),
                new EnglishOrAvailable(),
                new PathsMatch(),
                new TmdbIdsMatch(),
                new OmbiRadarrAlignment(),
                new QualityProfileAnyOrAvailable(),
                new SearchedRecently(),
                new NotInTvFolder());

        for (Validator<?> rule : movieValidators) {
            assertTrue(
                    rule.supportedType() == RequestType.MOVIE,
                    rule.getClass().getSimpleName() + " should support MOVIE");
        }
    }

    @Test
    void tvValidatorsSupportedTypeIsTv() {
        TvSeasonRequestRepository repo = mock(TvSeasonRequestRepository.class);
        List<Validator<?>> tvValidators = List.of(
                new AvailableInOmbiTv(),
                new AvailableInSonarr(),
                new AvailableInPlexTv(),
                new TvdbIdsMatch(),
                new EnglishOrAvailableTv(),
                new SonarrMonitored(),
                new SearchedRecentlyTv(),
                new OmbiSonarrAlignment(),
                new OmbiSeasonCountAlignment(repo));

        for (Validator<?> rule : tvValidators) {
            assertTrue(rule.supportedType() == RequestType.TV, rule.getClass().getSimpleName() + " should support TV");
        }
    }
}
