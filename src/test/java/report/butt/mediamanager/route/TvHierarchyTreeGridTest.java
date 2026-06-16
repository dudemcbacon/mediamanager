package report.butt.mediamanager.route;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import report.butt.mediamanager.model.TvChildRequest;
import report.butt.mediamanager.model.TvEpisodeRequest;
import report.butt.mediamanager.model.TvRequest;
import report.butt.mediamanager.model.TvSeasonRequest;
import report.butt.mediamanager.model.Validation;
import report.butt.mediamanager.validation.EpisodePathsMatch;
import report.butt.mediamanager.validation.EpisodeSearchedRecently;
import report.butt.mediamanager.validation.EpisodeValidator;

class TvHierarchyTreeGridTest {

    private static final List<EpisodeValidator> VALIDATORS =
            List.of(new EpisodePathsMatch(), new EpisodeSearchedRecently());

    @Test
    void unvalidatedSeasonsAreIgnoredAtChildLevel() {
        // Season 1's episode is validated and valid; season 2's episode has no validation yet ("—").
        // The child should be valid because the only unknown row is ignored, not counted as a failure.
        var child = childWithEpisodes(1L, 2L);
        var validations = episodeValidations(Map.of(1L, true));

        Boolean result = TvHierarchyTreeGrid.allChildrenValidation(List.of(child), VALIDATORS, validations);

        assertEquals(true, result, "Child should be valid when its only known season is valid");
    }

    @Test
    void knownFailureStillFailsDespiteUnknownRows() {
        var child = childWithEpisodes(1L, 2L);
        var validations = episodeValidations(Map.of(1L, false));

        Boolean result = TvHierarchyTreeGrid.allChildrenValidation(List.of(child), VALIDATORS, validations);

        assertEquals(false, result, "A known failing season must still fail the child");
    }

    @Test
    void allRowsUnknownStaysUnknown() {
        var child = childWithEpisodes(1L, 2L);

        Boolean result =
                TvHierarchyTreeGrid.allChildrenValidation(List.of(child), VALIDATORS, new HashMap<>());

        assertNull(result, "A child with nothing validated is unknown, not a failure");
    }

    /** A single child request with one episode per season, season numbers 1..N, episode ids as given. */
    private static TvChildRequest childWithEpisodes(Long... episodeIds) {
        var parent = new TvRequest("Show", 100, false, 1, "Common.ProcessingRequest");
        var child = new TvChildRequest(parent, "Show", 100, false, 1, "Common.ProcessingRequest");
        List<TvSeasonRequest> seasons = new ArrayList<>();
        for (int i = 0; i < episodeIds.length; i++) {
            int seasonNumber = i + 1;
            var season = new TvSeasonRequest(child, seasonNumber, seasonNumber, false);
            var episode = new TvEpisodeRequest(season, episodeIds[i].intValue(), 1);
            episode.setId(episodeIds[i]);
            season.setEpisodeRequests(new ArrayList<>(List.of(episode)));
            seasons.add(season);
        }
        child.setSeasonRequests(seasons);
        return child;
    }

    /** Marks each listed episode id as having both episode validators recorded with the given result. */
    private static Map<Long, Map<String, Validation>> episodeValidations(Map<Long, Boolean> resultsByEpisodeId) {
        Map<Long, Map<String, Validation>> latest = new HashMap<>();
        resultsByEpisodeId.forEach((episodeId, valid) -> {
            Map<String, Validation> byName = new HashMap<>();
            for (EpisodeValidator validator : VALIDATORS) {
                String name = validator.getClass().getSimpleName();
                byName.put(name, new Validation(name, valid, (TvEpisodeRequest) null));
            }
            latest.put(episodeId, byName);
        });
        return latest;
    }
}
