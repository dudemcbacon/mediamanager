package report.butt.mediamanager.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import jakarta.persistence.EntityManager;
import java.util.List;
import org.jspecify.annotations.NullMarked;
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
            // Must differ from the username: AppUserBootstrap rejects equal bootstrap creds on a clean DB (CI).
            "mediamanager.bootstrap.password=test-admin-pw",
            "mediamanager.validate-required-config=false",
            // JobRunr's background server + dashboard are runtime concerns, not part of these service tests;
            // disabling them keeps the test from starting worker threads or binding the dashboard port.
            "jobrunr.background-job-server.enabled=false",
            "jobrunr.dashboard.enabled=false"
        })
@Transactional
@NullMarked
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
    void loadAllHierarchies_returnsHierarchyGroupedByParentId() {
        TvRequest parent =
                tvRequestRepository.save(new TvRequest("All Hierarchies Show", 888001, false, 8801, "Common.Approved"));

        TvChildRequest child = tvChildRequestRepository.save(
                new TvChildRequest(parent, "All Hierarchies Show", 888001, false, 8901, "Common.Approved"));

        TvSeasonRequest season = tvSeasonRequestRepository.save(new TvSeasonRequest(child, 8800, 1, false));
        tvEpisodeRequestRepository.save(new TvEpisodeRequest(season, 8900, 1));
        tvEpisodeRequestRepository.save(new TvEpisodeRequest(season, 8901, 2));

        entityManager.flush();
        entityManager.clear();

        var result = service.loadAllHierarchies();

        // Result may contain entries from other tests; verify our parent's entry is correct
        assertNotNull(result.get(parent.getId()), "parent entry must be present");
        List<TvChildRequest> children = result.get(parent.getId());
        assertEquals(1, children.size());
        assertEquals(1, children.get(0).getSeasonRequests().size());
        assertEquals(
                2,
                children.get(0).getSeasonRequests().get(0).getEpisodeRequests().size());
    }

    @Test
    void loadEpisodesByRequestId_returnsEpisodesGroupedByParentId() {
        TvRequest parent =
                tvRequestRepository.save(new TvRequest("Eps By Id Show", 777001, false, 7701, "Common.Approved"));

        TvChildRequest child = tvChildRequestRepository.save(
                new TvChildRequest(parent, "Eps By Id Show", 777001, false, 7801, "Common.Approved"));

        TvSeasonRequest season = tvSeasonRequestRepository.save(new TvSeasonRequest(child, 7700, 1, false));
        tvEpisodeRequestRepository.save(new TvEpisodeRequest(season, 7800, 1));
        tvEpisodeRequestRepository.save(new TvEpisodeRequest(season, 7801, 2));

        entityManager.flush();
        entityManager.clear();

        var result = service.loadEpisodesByRequestId();

        // Result may contain entries from other tests; verify our parent's entry
        List<TvEpisodeRequest> episodes = result.get(parent.getId());
        assertNotNull(episodes, "episodes for our parent must be present");
        assertEquals(2, episodes.size());
    }

    @Test
    void loadEpisodes_returnsAllEpisodesForShow() {
        TvRequest parent =
                tvRequestRepository.save(new TvRequest("Load Episodes Show", 666001, false, 6601, "Common.Approved"));

        TvChildRequest child = tvChildRequestRepository.save(
                new TvChildRequest(parent, "Load Episodes Show", 666001, false, 6701, "Common.Approved"));

        TvSeasonRequest season = tvSeasonRequestRepository.save(new TvSeasonRequest(child, 6600, 1, false));
        tvEpisodeRequestRepository.save(new TvEpisodeRequest(season, 6700, 1));
        tvEpisodeRequestRepository.save(new TvEpisodeRequest(season, 6701, 2));

        entityManager.flush();
        entityManager.clear();

        List<TvEpisodeRequest> episodes = service.loadEpisodes(parent);
        assertEquals(2, episodes.size());
    }

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
