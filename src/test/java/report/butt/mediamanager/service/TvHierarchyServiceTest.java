package report.butt.mediamanager.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import jakarta.persistence.EntityManager;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;
import report.butt.mediamanager.client.PlexClient;
import report.butt.mediamanager.model.TvChildRequest;
import report.butt.mediamanager.model.TvEpisodeRequest;
import report.butt.mediamanager.model.TvRequest;
import report.butt.mediamanager.model.TvSeasonRequest;
import report.butt.mediamanager.repository.TvChildRequestRepository;
import report.butt.mediamanager.repository.TvEpisodeRequestRepository;
import report.butt.mediamanager.repository.TvRequestRepository;
import report.butt.mediamanager.repository.TvSeasonRequestRepository;

// Bootstrap credentials have no default in application.properties (fail-fast in production). Supply
// test values here so the context can start without the MEDIAMANAGER_BOOTSTRAP_* env vars.
// Required-config validation is disabled: tests don't supply the real integration/mail config it enforces.
@SpringBootTest(
        properties = {
            "mediamanager.bootstrap.username=test",
            "mediamanager.bootstrap.password=test",
            "mediamanager.validate-required-config=false"
        })
@Transactional
class TvHierarchyServiceTest {

    @Autowired
    private TvHierarchyService service;

    @Autowired
    private TvRequestRepository tvRequestRepository;

    @Autowired
    private TvChildRequestRepository tvChildRequestRepository;

    @Autowired
    private TvSeasonRequestRepository tvSeasonRequestRepository;

    @Autowired
    private TvEpisodeRequestRepository tvEpisodeRequestRepository;

    @Autowired
    private EntityManager entityManager;

    // PlexClient's @PostConstruct methods perform live Plex HTTP calls at startup, which would fail
    // to load the context in environments without a reachable Plex server. This test exercises only
    // the JPA hierarchy load, so a mock bean keeps the context bootable without external services.
    @MockitoBean
    private PlexClient plexClient;

    @Test
    void loadHierarchy_returnsChildrenSeasonsAndEpisodesInOrder() {
        TvRequest parent =
                tvRequestRepository.save(new TvRequest("Hierarchy Test Show", 999001, false, 9001, "Common.Approved"));

        TvChildRequest childA = tvChildRequestRepository.save(
                new TvChildRequest(parent, "Hierarchy Test Show", 999001, false, 9101, "Common.Approved"));
        TvChildRequest childB = tvChildRequestRepository.save(
                new TvChildRequest(parent, "Hierarchy Test Show", 999001, false, 9102, "Common.Approved"));

        // Insert seasons out of order to verify sorting.
        for (TvChildRequest child : List.of(childA, childB)) {
            for (Integer seasonNumber : List.of(3, 1, 2)) {
                TvSeasonRequest season = tvSeasonRequestRepository.save(new TvSeasonRequest(
                        child, 8000 + child.getId().intValue() * 10 + seasonNumber, seasonNumber, false));
                // Insert episodes out of order too.
                for (Integer episodeNumber : List.of(2, 1, 3)) {
                    tvEpisodeRequestRepository.save(new TvEpisodeRequest(
                            season, 7000 + season.getId().intValue() * 10 + episodeNumber, episodeNumber));
                }
            }
        }

        // Flush pending writes and clear the persistence context so the service
        // performs a fresh load of children (and their LAZY collections) rather than
        // returning the stale in-memory parent with empty seasonRequests.
        entityManager.flush();
        entityManager.clear();

        List<TvChildRequest> result = service.loadHierarchy(parent);

        assertEquals(2, result.size(), "expected two children");
        assertEquals(childA.getId(), result.get(0).getId(), "children must be ordered by id ascending");
        assertEquals(childB.getId(), result.get(1).getId(), "children must be ordered by id ascending");

        for (TvChildRequest child : result) {
            List<TvSeasonRequest> seasons = child.getSeasonRequests();
            assertNotNull(seasons, "seasons must be initialized");
            assertEquals(3, seasons.size(), "each child should have 3 seasons");
            assertEquals(
                    List.of(1, 2, 3),
                    seasons.stream().map(TvSeasonRequest::getOmbiSeasonNumber).toList(),
                    "seasons must be ordered by ombiSeasonNumber ascending");

            for (TvSeasonRequest season : seasons) {
                List<TvEpisodeRequest> episodes = season.getEpisodeRequests();
                assertNotNull(episodes, "episodes must be initialized");
                assertEquals(3, episodes.size(), "each season should have 3 episodes");
                assertEquals(
                        List.of(1, 2, 3),
                        episodes.stream()
                                .map(TvEpisodeRequest::getOmbiEpisodeNumber)
                                .toList(),
                        "episodes must be ordered by ombiEpisodeNumber ascending");
            }
        }
    }
}
