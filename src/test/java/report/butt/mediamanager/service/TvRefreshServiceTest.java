package report.butt.mediamanager.service;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import org.jobrunr.scheduling.JobRequestScheduler;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import report.butt.mediamanager.client.MetadataResult;
import report.butt.mediamanager.client.OmbiClient;
import report.butt.mediamanager.client.PlexClient;
import report.butt.mediamanager.client.SonarrClient;
import report.butt.mediamanager.exceptions.RequestNotFoundException;
import report.butt.mediamanager.model.TvRequest;
import report.butt.mediamanager.model.TvSeasonRequest;
import report.butt.mediamanager.model.ombi.OmbiTvChildRequest;
import report.butt.mediamanager.model.ombi.OmbiTvEpisode;
import report.butt.mediamanager.model.ombi.OmbiTvRequest;
import report.butt.mediamanager.model.ombi.OmbiTvSeasonRequest;
import report.butt.mediamanager.model.sonarr.Series;
import report.butt.mediamanager.repository.TvChildRequestRepository;
import report.butt.mediamanager.repository.TvEpisodeRequestRepository;
import report.butt.mediamanager.repository.TvRequestRepository;
import report.butt.mediamanager.repository.TvSeasonRequestRepository;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@NullMarked
class TvRefreshServiceTest {

    private final TvRequestRepository repository = mock(TvRequestRepository.class);
    private final TvChildRequestRepository childRepository = mock(TvChildRequestRepository.class);
    private final TvSeasonRequestRepository seasonRepository = mock(TvSeasonRequestRepository.class);
    private final TvEpisodeRequestRepository episodeRepository = mock(TvEpisodeRequestRepository.class);
    private final OmbiClient ombiClient = mock(OmbiClient.class);
    private final SonarrClient sonarrClient = mock(SonarrClient.class);
    private final PlexClient plexClient = mock(PlexClient.class);
    private final PlexCacheService plexCacheService = mock(PlexCacheService.class);
    private final JobRequestScheduler jobRequestScheduler = mock(JobRequestScheduler.class);
    private final FfprobeScanService ffprobeScanService = mock(FfprobeScanService.class);

    private final TvRefreshService service = new TvRefreshService(
            repository,
            childRepository,
            seasonRepository,
            episodeRepository,
            ombiClient,
            sonarrClient,
            plexClient,
            plexCacheService,
            "",
            jobRequestScheduler,
            ffprobeScanService);

    // --- refreshAll ---

    @Test
    void refreshAllWithEmptyOmbiResponseDoesNothingExceptClean() {
        when(ombiClient.getTvRequests()).thenReturn(List.of());
        when(sonarrClient.getAllSeries()).thenReturn(List.of());
        when(sonarrClient.getQualityProfilesById()).thenReturn(Map.of());
        when(plexClient.getAllShowsIndexedByTvdb()).thenReturn(Map.of());
        when(plexClient.getAllEpisodesIndexedByShow()).thenReturn(Map.of());
        when(repository.findByOmbiRequestIdIn(any())).thenReturn(List.of());
        when(childRepository.findByOmbiRequestIdIn(any())).thenReturn(List.of());
        when(seasonRepository.findByTvChildRequestIdIn(any())).thenReturn(List.of());
        when(episodeRepository.findByTvSeasonRequestIdIn(any())).thenReturn(List.of());

        service.refreshAll();

        verify(repository, never()).saveAll(anyList());
        verify(plexCacheService).cleanExcept("tv-", Set.of());
    }

    @Test
    void refreshAllReCachesSonarrQualityProfiles() {
        when(ombiClient.getTvRequests()).thenReturn(List.of());
        when(sonarrClient.getAllSeries()).thenReturn(List.of());
        when(sonarrClient.getQualityProfilesById()).thenReturn(Map.of());
        when(plexClient.getAllShowsIndexedByTvdb()).thenReturn(Map.of());
        when(plexClient.getAllEpisodesIndexedByShow()).thenReturn(Map.of());
        when(repository.findByOmbiRequestIdIn(any())).thenReturn(List.of());
        when(childRepository.findByOmbiRequestIdIn(any())).thenReturn(List.of());
        when(seasonRepository.findByTvChildRequestIdIn(any())).thenReturn(List.of());
        when(episodeRepository.findByTvSeasonRequestIdIn(any())).thenReturn(List.of());

        service.refreshAll();

        // Profiles are re-cached each refresh so renamed/added Sonarr profiles self-heal without a restart.
        verify(sonarrClient).cacheQualityProfiles();
    }

    @Test
    void refreshAllSavesNewShowWhenNotInDb() {
        OmbiTvRequest ombiTv = ombiTv(1, "New Show", 500);
        when(ombiClient.getTvRequests()).thenReturn(List.of(ombiTv));
        when(sonarrClient.getAllSeries()).thenReturn(List.of());
        when(sonarrClient.getQualityProfilesById()).thenReturn(Map.of());
        when(plexClient.getAllShowsIndexedByTvdb()).thenReturn(Map.of());
        when(plexClient.getAllEpisodesIndexedByShow()).thenReturn(Map.of());
        when(repository.findByOmbiRequestIdIn(any())).thenReturn(List.of());
        when(childRepository.findByOmbiRequestIdIn(any())).thenReturn(List.of());
        when(seasonRepository.findByTvChildRequestIdIn(any())).thenReturn(List.of());
        when(episodeRepository.findByTvSeasonRequestIdIn(any())).thenReturn(List.of());

        service.refreshAll();

        verify(repository).saveAll(anyList());
    }

    @Test
    void refreshAllWithSonarrMatchSavesSonarrFields() {
        OmbiTvRequest ombiTv = ombiTv(1, "Matched Show", 600);
        Series series = series(10, 600, "matched-show");

        when(ombiClient.getTvRequests()).thenReturn(List.of(ombiTv));
        when(sonarrClient.getAllSeries()).thenReturn(List.of(series));
        when(sonarrClient.getQualityProfilesById()).thenReturn(Map.of());
        when(sonarrClient.getEpisodes(10)).thenReturn(List.of());
        when(plexClient.getAllShowsIndexedByTvdb()).thenReturn(Map.of());
        when(plexClient.getAllEpisodesIndexedByShow()).thenReturn(Map.of());
        when(repository.findByOmbiRequestIdIn(any())).thenReturn(List.of());
        when(childRepository.findByOmbiRequestIdIn(any())).thenReturn(List.of());
        when(seasonRepository.findByTvChildRequestIdIn(any())).thenReturn(List.of());
        when(episodeRepository.findByTvSeasonRequestIdIn(any())).thenReturn(List.of());
        when(plexClient.cacheTvMetadata(any(Integer.class), any())).thenReturn("/plex-cache/tv-600.json");

        service.refreshAll();

        verify(repository).saveAll(anyList());
        verify(plexCacheService).cleanExcept(any(), any());
    }

    @Test
    void refreshAllSonarrEpisodeFetchExceptionIsSwallowed() {
        OmbiTvRequest ombiTv = ombiTv(1, "Show", 700);
        Series series = series(20, 700, "show");

        when(ombiClient.getTvRequests()).thenReturn(List.of(ombiTv));
        when(sonarrClient.getAllSeries()).thenReturn(List.of(series));
        when(sonarrClient.getQualityProfilesById()).thenReturn(Map.of());
        when(sonarrClient.getEpisodes(20)).thenThrow(new RuntimeException("Sonarr down"));
        when(plexClient.getAllShowsIndexedByTvdb()).thenReturn(Map.of());
        when(plexClient.getAllEpisodesIndexedByShow()).thenReturn(Map.of());
        when(repository.findByOmbiRequestIdIn(any())).thenReturn(List.of());
        when(childRepository.findByOmbiRequestIdIn(any())).thenReturn(List.of());
        when(seasonRepository.findByTvChildRequestIdIn(any())).thenReturn(List.of());
        when(episodeRepository.findByTvSeasonRequestIdIn(any())).thenReturn(List.of());
        when(plexClient.cacheTvMetadata(any(Integer.class), any())).thenReturn("/plex-cache/tv-700.json");

        // Should not propagate
        service.refreshAll();

        verify(repository).saveAll(anyList());
    }

    @Test
    void refreshAllExistingUnchangedShowIsNotSaved() {
        // Build an OmbiTvRequest with no child requests (firstChild=null → ombiAvailable/status=null)
        OmbiTvRequest ombiTv = ombiTv(1, "Existing Show", 800);
        ombiTv.setChildRequests(null);
        ombiTv.setTotalSeasons(1);
        ombiTv.setExternalProviderId(null); // backfillTotalSeasons won't run (totalSeasons != 0)

        var existing = new TvRequest("Existing Show", 800, null, 1, null);
        existing.setId(10L);
        // Pre-set every field that applyUpdates will overwrite with the exact same values
        existing.setTitle("Existing Show");
        existing.setTvdbId(800);
        existing.setOmbiAvailable(null);
        existing.setOmbiRequestStatus(null);
        existing.setOmbiUserName(null);
        existing.setOmbiRequestedDate(null);
        existing.setOmbiExternalProviderId(null);
        existing.setOmbiTotalSeasons(1); // ombiTv.getTotalSeasons() == 1

        when(ombiClient.getTvRequests()).thenReturn(List.of(ombiTv));
        when(sonarrClient.getAllSeries()).thenReturn(List.of());
        when(sonarrClient.getQualityProfilesById()).thenReturn(Map.of());
        when(plexClient.getAllShowsIndexedByTvdb()).thenReturn(Map.of());
        when(plexClient.getAllEpisodesIndexedByShow()).thenReturn(Map.of());
        when(repository.findByOmbiRequestIdIn(any())).thenReturn(List.of(existing));
        when(childRepository.findByOmbiRequestIdIn(any())).thenReturn(List.of());
        when(seasonRepository.findByTvChildRequestIdIn(any())).thenReturn(List.of());
        when(episodeRepository.findByTvSeasonRequestIdIn(any())).thenReturn(List.of());

        service.refreshAll();

        verify(repository, never()).saveAll(anyList());
    }

    @Test
    void refreshAllWithChildRequestsSavesSeasonsAndEpisodes() {
        // Build an OmbiTvRequest with a child request, season, and episode
        OmbiTvRequest ombiTv = ombiTv(1, "Child Show", 900);
        OmbiTvChildRequest child = ombiChild(1, 11);
        OmbiTvSeasonRequest season = ombiSeason(100, 1);
        OmbiTvEpisode episode = ombiEpisode(1000, 1);
        season.setEpisodes(List.of(episode));
        child.setSeasonRequests(List.of(season));
        ombiTv.setChildRequests(List.of(child));

        when(ombiClient.getTvRequests()).thenReturn(List.of(ombiTv));
        when(sonarrClient.getAllSeries()).thenReturn(List.of());
        when(sonarrClient.getQualityProfilesById()).thenReturn(Map.of());
        when(plexClient.getAllShowsIndexedByTvdb()).thenReturn(Map.of());
        when(plexClient.getAllEpisodesIndexedByShow()).thenReturn(Map.of());
        when(repository.findByOmbiRequestIdIn(any())).thenReturn(List.of());
        when(childRepository.findByOmbiRequestIdIn(any())).thenReturn(List.of());
        when(seasonRepository.findByTvChildRequestIdIn(any())).thenReturn(List.of());
        when(episodeRepository.findByTvSeasonRequestIdIn(any())).thenReturn(List.of());

        service.refreshAll();

        // Show, child, season, and episode all saved
        verify(repository).saveAll(anyList());
        verify(childRepository).saveAll(anyList());
        verify(seasonRepository).saveAll(anyList());
        verify(episodeRepository).saveAll(anyList());
    }

    @Test
    void refreshAllAllEpisodesAvailableSetsSeasonAvailable() {
        OmbiTvRequest ombiTv = ombiTv(2, "Available Show", 901);
        OmbiTvChildRequest child = ombiChild(2, 12);
        OmbiTvSeasonRequest season = ombiSeason(200, 1);
        OmbiTvEpisode ep1 = ombiEpisode(2001, 1);
        ep1.setAvailable(true);
        OmbiTvEpisode ep2 = ombiEpisode(2002, 2);
        ep2.setAvailable(true);
        season.setEpisodes(List.of(ep1, ep2));
        child.setSeasonRequests(List.of(season));
        ombiTv.setChildRequests(List.of(child));

        when(ombiClient.getTvRequests()).thenReturn(List.of(ombiTv));
        when(sonarrClient.getAllSeries()).thenReturn(List.of());
        when(sonarrClient.getQualityProfilesById()).thenReturn(Map.of());
        when(plexClient.getAllShowsIndexedByTvdb()).thenReturn(Map.of());
        when(plexClient.getAllEpisodesIndexedByShow()).thenReturn(Map.of());
        when(repository.findByOmbiRequestIdIn(any())).thenReturn(List.of());
        when(childRepository.findByOmbiRequestIdIn(any())).thenReturn(List.of());
        when(seasonRepository.findByTvChildRequestIdIn(any())).thenReturn(List.of());
        when(episodeRepository.findByTvSeasonRequestIdIn(any())).thenReturn(List.of());

        // Should not throw; season availability calculated from episodes
        service.refreshAll();

        verify(seasonRepository).saveAll(anyList());
    }

    @Test
    @SuppressWarnings("unchecked") // Safe: seasonRepository.saveAll is always invoked with a List<TvSeasonRequest>.
    void refreshAllSeasonWithNoEpisodesIsMarkedAvailable() {
        OmbiTvRequest ombiTv = ombiTv(3, "Empty Season Show", 902);
        OmbiTvChildRequest child = ombiChild(3, 13);
        OmbiTvSeasonRequest season = ombiSeason(300, 1);
        season.setEpisodes(List.of()); // zero episodes associated
        child.setSeasonRequests(List.of(season));
        ombiTv.setChildRequests(List.of(child));

        when(ombiClient.getTvRequests()).thenReturn(List.of(ombiTv));
        when(sonarrClient.getAllSeries()).thenReturn(List.of());
        when(sonarrClient.getQualityProfilesById()).thenReturn(Map.of());
        when(plexClient.getAllShowsIndexedByTvdb()).thenReturn(Map.of());
        when(plexClient.getAllEpisodesIndexedByShow()).thenReturn(Map.of());
        when(repository.findByOmbiRequestIdIn(any())).thenReturn(List.of());
        when(childRepository.findByOmbiRequestIdIn(any())).thenReturn(List.of());
        when(seasonRepository.findByTvChildRequestIdIn(any())).thenReturn(List.of());
        when(episodeRepository.findByTvSeasonRequestIdIn(any())).thenReturn(List.of());

        service.refreshAll();

        ArgumentCaptor<List<TvSeasonRequest>> captor = ArgumentCaptor.forClass(List.class);
        verify(seasonRepository).saveAll(captor.capture());
        assertTrue(
                captor.getValue().stream().allMatch(s -> Objects.equals(s.getOmbiSeasonAvailable(), true)),
                "A season with no episodes should be marked available");
    }

    // --- refreshOne ---

    @Test
    void refreshOneThrowsWhenNotFound() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(RequestNotFoundException.class, () -> service.refreshOne(99L));
    }

    @Test
    void refreshOneWithNullOmbiIdSkipsOmbiLookup() {
        var existing = new TvRequest("Orphan Show", 900, false, null, "Common.ProcessingRequest");
        existing.setId(1L);
        when(repository.findById(1L)).thenReturn(Optional.of(existing));
        when(sonarrClient.getQualityProfilesById()).thenReturn(Map.of());
        when(repository.save(any())).thenReturn(existing);

        service.refreshOne(1L);

        verify(repository).save(existing);
    }

    @Test
    void refreshOneWithOmbiIdNotFoundInOmbiResponse() {
        var existing = new TvRequest("Orphan Show", 1000, false, 50, "Common.ProcessingRequest");
        existing.setId(2L);
        when(repository.findById(2L)).thenReturn(Optional.of(existing));
        when(ombiClient.getTvRequests()).thenReturn(List.of()); // id 50 not present
        when(sonarrClient.getQualityProfilesById()).thenReturn(Map.of());
        when(repository.save(any())).thenReturn(existing);

        service.refreshOne(2L);

        verify(repository).save(existing);
    }

    @Test
    void refreshOneWithOmbiMatchAndSonarrSeries() {
        var existing = new TvRequest("My Show", 1100, false, 60, "Common.ProcessingRequest");
        existing.setId(3L);
        OmbiTvRequest ombiTv = ombiTv(60, "My Show", 1100);
        OmbiTvChildRequest child = ombiChild(60, 1);
        ombiTv.setChildRequests(List.of(child));
        Series series = series(30, 1100, "my-show");

        when(repository.findById(3L)).thenReturn(Optional.of(existing));
        when(ombiClient.getTvRequests()).thenReturn(List.of(ombiTv));
        when(sonarrClient.getSeriesByTvdbId(1100)).thenReturn(List.of(series));
        when(sonarrClient.getQualityProfilesById()).thenReturn(Map.of());
        when(sonarrClient.getEpisodes(30)).thenReturn(List.of());
        when(repository.save(any())).thenReturn(existing);
        when(childRepository.findByOmbiRequestId(1)).thenReturn(Optional.empty());
        when(childRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(plexClient.getShowByTvdbId(any(Integer.class), any(), any(Integer.class)))
                .thenReturn(new MetadataResult(null, null));

        service.refreshOne(3L);

        verify(repository).save(existing);
    }

    // --- fixtures ---

    private static OmbiTvRequest ombiTv(Integer id, String title, Integer tvdbId) {
        var req = new OmbiTvRequest();
        req.setId(id);
        req.setTitle(title);
        req.setTvDbId(tvdbId);
        req.setTotalSeasons(1);
        return req;
    }

    private static OmbiTvChildRequest ombiChild(Integer parentId, Integer id) {
        var child = new OmbiTvChildRequest();
        child.setId(id);
        child.setParentRequestId(parentId);
        child.setTitle("Child");
        child.setAvailable(false);
        child.setRequestStatus("Common.ProcessingRequest");
        return child;
    }

    private static Series series(Integer id, Integer tvdbId, String titleSlug) {
        var s = new Series();
        s.setId(id);
        s.setTvdbId(tvdbId);
        s.setTitleSlug(titleSlug);
        s.setTitle("Series Title");
        s.setYear(2020);
        s.setQualityProfileId(1); // must be non-null: Map.of() throws NPE on null key lookup
        return s;
    }

    private static OmbiTvSeasonRequest ombiSeason(Integer id, Integer seasonNumber) {
        var s = new OmbiTvSeasonRequest();
        s.setId(id);
        s.setSeasonNumber(seasonNumber);
        return s;
    }

    private static OmbiTvEpisode ombiEpisode(Integer id, Integer episodeNumber) {
        var e = new OmbiTvEpisode();
        e.setId(id);
        e.setEpisodeNumber(episodeNumber);
        e.setAvailable(false);
        return e;
    }
}
