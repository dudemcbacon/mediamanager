package report.butt.mediamanager.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.ui.ConcurrentModel;
import report.butt.mediamanager.client.OmbiClient;
import report.butt.mediamanager.client.SonarrClient;
import report.butt.mediamanager.exceptions.RequestNotFoundException;
import report.butt.mediamanager.model.Note;
import report.butt.mediamanager.model.TvChildRequest;
import report.butt.mediamanager.model.TvEpisodeRequest;
import report.butt.mediamanager.model.TvRequest;
import report.butt.mediamanager.model.TvSeasonRequest;
import report.butt.mediamanager.model.ombi.OmbiReprocessResponse;
import report.butt.mediamanager.model.sonarr.Episode;
import report.butt.mediamanager.model.sonarr.SonarrCommand;
import report.butt.mediamanager.model.sonarr.SonarrHealthItem;
import report.butt.mediamanager.model.sonarr.SonarrQueue;
import report.butt.mediamanager.model.sonarr.SonarrQueueRecord;
import report.butt.mediamanager.repository.TvChildRequestRepository;
import report.butt.mediamanager.repository.TvEpisodeRequestRepository;
import report.butt.mediamanager.repository.TvRequestRepository;
import report.butt.mediamanager.repository.TvSeasonRequestRepository;
import report.butt.mediamanager.service.RequestAdminService;
import report.butt.mediamanager.service.TvRefreshService;
import report.butt.mediamanager.service.ValidatorService;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

class TvControllerTest {

    private final TvRequestRepository tvRequestRepository = mock(TvRequestRepository.class);
    private final TvChildRequestRepository tvChildRequestRepository = mock(TvChildRequestRepository.class);
    private final TvSeasonRequestRepository tvSeasonRequestRepository = mock(TvSeasonRequestRepository.class);
    private final TvEpisodeRequestRepository tvEpisodeRequestRepository = mock(TvEpisodeRequestRepository.class);
    private final OmbiClient ombiClient = mock(OmbiClient.class);
    private final SonarrClient sonarrClient = mock(SonarrClient.class);
    private final ObjectMapper objectMapper = mock(ObjectMapper.class);
    private final TvRefreshService tvRefreshService = mock(TvRefreshService.class);
    private final ValidatorService validatorService = mock(ValidatorService.class);
    private final RequestAdminService requestAdminService = mock(RequestAdminService.class);

    private final TvController controller = new TvController(
            tvRequestRepository,
            tvChildRequestRepository,
            tvSeasonRequestRepository,
            tvEpisodeRequestRepository,
            ombiClient,
            sonarrClient,
            objectMapper,
            tvRefreshService,
            validatorService,
            requestAdminService);

    @Test
    void markAvailable_callsOmbiOncePerChildRequestId() {
        TvRequest parent = new TvRequest("Show", 1, false, 100, "Common.Approved");
        parent.setId(7L);
        TvChildRequest childA = new TvChildRequest(parent, "Show", 1, false, 201, "Common.Approved");
        TvChildRequest childB = new TvChildRequest(parent, "Show", 1, false, 202, "Common.Approved");

        when(tvRequestRepository.findById(7L)).thenReturn(Optional.of(parent));
        when(tvChildRequestRepository.findByParentOrderByIdAsc(parent)).thenReturn(List.of(childA, childB));
        when(objectMapper.writeValueAsString(any())).thenReturn("{}");

        controller.markAvailable(7L);

        // The parent's own ombi_request_id (100) must NOT be used; each child's id is.
        verify(ombiClient).markTvAvailable(201);
        verify(ombiClient).markTvAvailable(202);
        verifyNoMoreInteractions(ombiClient);
    }

    @Test
    void searchSeason_triggersSeasonSearchForThatSeason() {
        TvRequest parent = new TvRequest("Show", 1, false, 100, "Common.Approved");
        parent.setSonarrSeriesId(55);
        TvChildRequest child = new TvChildRequest(parent, "Show", 1, false, 201, "Common.Approved");
        TvSeasonRequest season = new TvSeasonRequest(child, 1, 2, false);
        season.setId(20L);

        when(tvSeasonRequestRepository.findById(20L)).thenReturn(Optional.of(season));
        when(sonarrClient.searchSeason(55, 2)).thenReturn(new SonarrCommand());

        controller.searchSeason(20L);

        verify(sonarrClient).searchSeason(55, 2);
        verifyNoMoreInteractions(sonarrClient);
    }

    @Test
    void searchAllSeasonsForChild_triggersSeasonSearchPerSeason() {
        TvRequest parent = new TvRequest("Show", 1, false, 100, "Common.Approved");
        parent.setSonarrSeriesId(55);
        TvChildRequest child = new TvChildRequest(parent, "Show", 1, false, 201, "Common.Approved");
        child.setId(10L);
        child.setSeasonRequests(
                List.of(new TvSeasonRequest(child, 1, 1, false), new TvSeasonRequest(child, 2, 2, false)));

        when(tvChildRequestRepository.findById(10L)).thenReturn(Optional.of(child));
        when(sonarrClient.searchSeason(55, 1)).thenReturn(new SonarrCommand());
        when(sonarrClient.searchSeason(55, 2)).thenReturn(new SonarrCommand());

        controller.searchAllSeasonsForChild(10L);

        verify(sonarrClient).searchSeason(55, 1);
        verify(sonarrClient).searchSeason(55, 2);
        verifyNoMoreInteractions(sonarrClient);
    }

    @Test
    void searchEpisode_resolvesSonarrEpisodeIdAndTriggersEpisodeSearch() {
        TvRequest parent = new TvRequest("Show", 1, false, 100, "Common.Approved");
        parent.setSonarrSeriesId(55);
        TvChildRequest child = new TvChildRequest(parent, "Show", 1, false, 201, "Common.Approved");
        TvSeasonRequest season = new TvSeasonRequest(child, 1, 2, false);
        TvEpisodeRequest episode = new TvEpisodeRequest(season, 100, 3);
        episode.setId(30L);

        when(tvEpisodeRequestRepository.findById(30L)).thenReturn(Optional.of(episode));
        when(sonarrClient.getEpisodes(55)).thenReturn(List.of(sonarrEpisode(999, 2, 3), sonarrEpisode(998, 2, 4)));
        when(sonarrClient.searchEpisodes(List.of(999))).thenReturn(new SonarrCommand());

        controller.searchEpisode(30L);

        verify(sonarrClient).getEpisodes(55);
        verify(sonarrClient).searchEpisodes(List.of(999));
        verifyNoMoreInteractions(sonarrClient);
    }

    @Test
    void searchEpisode_withoutSonarrSeriesId_doesNotCallSonarr() {
        TvRequest parent = new TvRequest("Show", 1, false, 100, "Common.Approved");
        TvChildRequest child = new TvChildRequest(parent, "Show", 1, false, 201, "Common.Approved");
        TvSeasonRequest season = new TvSeasonRequest(child, 1, 2, false);
        TvEpisodeRequest episode = new TvEpisodeRequest(season, 100, 3);
        episode.setId(31L);

        when(tvEpisodeRequestRepository.findById(31L)).thenReturn(Optional.of(episode));

        controller.searchEpisode(31L);

        verifyNoMoreInteractions(sonarrClient);
    }

    @Test
    void deleteEpisodeDownloadAndSearch_deletesMatchingQueueItemThenSearches() {
        TvRequest parent = new TvRequest("Show", 1, false, 100, "Common.Approved");
        parent.setSonarrSeriesId(55);
        TvChildRequest child = new TvChildRequest(parent, "Show", 1, false, 201, "Common.Approved");
        TvSeasonRequest season = new TvSeasonRequest(child, 1, 2, false);
        TvEpisodeRequest episode = new TvEpisodeRequest(season, 100, 3);
        episode.setId(30L);

        when(tvEpisodeRequestRepository.findById(30L)).thenReturn(Optional.of(episode));
        // One queue item for this episode (id 777), one for a different episode (id 778) that must be left alone.
        when(sonarrClient.getQueue()).thenReturn(queueOf(queueRecord(777, 55, 2, 3), queueRecord(778, 55, 2, 4)));
        when(sonarrClient.getEpisodes(55)).thenReturn(List.of(sonarrEpisode(999, 2, 3)));
        when(sonarrClient.searchEpisodes(List.of(999))).thenReturn(new SonarrCommand());

        controller.deleteEpisodeDownloadAndSearch(30L);

        verify(sonarrClient).getQueue();
        verify(sonarrClient).deleteQueueItem(777);
        verify(sonarrClient).getEpisodes(55);
        verify(sonarrClient).searchEpisodes(List.of(999));
        verifyNoMoreInteractions(sonarrClient);
    }

    // ---- getSonarrQueue ----

    @Test
    void getSonarrQueue_returnsQueueOnSuccess() {
        SonarrQueue queue = new SonarrQueue();
        when(sonarrClient.getQueue()).thenReturn(queue);

        assertNotNull(controller.getSonarrQueue());
    }

    @Test
    void getSonarrQueue_returnsNullOnException() {
        when(sonarrClient.getQueue()).thenThrow(new RuntimeException("unreachable"));

        assertNull(controller.getSonarrQueue());
    }

    // ---- getSonarrHealth ----

    @Test
    void getSonarrHealth_returnsListOnSuccess() {
        List<SonarrHealthItem> health = List.of(new SonarrHealthItem());
        when(sonarrClient.getHealth()).thenReturn(health);

        assertEquals(health, controller.getSonarrHealth());
    }

    @Test
    void getSonarrHealth_returnsNullOnException() {
        when(sonarrClient.getHealth()).thenThrow(new RuntimeException("unreachable"));

        assertNull(controller.getSonarrHealth());
    }

    // ---- refreshAll ----

    @Test
    void refreshAll_callsRefreshAllAndRedirects() {
        assertEquals("redirect:/tv", controller.refreshAll());
        verify(tvRefreshService).refreshAll();
    }

    // ---- searchMissing ----

    @Test
    void searchMissing_withNoMatchingSeries_redirectsWithoutCallingSearch() {
        when(tvRequestRepository.findAll()).thenReturn(List.of());

        assertEquals("redirect:/tv", controller.searchMissing());
        verify(sonarrClient, never()).searchSeries(any());
    }

    @Test
    void searchMissing_withMatchingSeries_triggersSearchAndStampsTime() {
        TvRequest tv = processingTvWithEpisodes(55);
        when(tvRequestRepository.findAll()).thenReturn(List.of(tv));
        when(sonarrClient.searchSeries(List.of(55))).thenReturn(new SonarrCommand());

        assertEquals("redirect:/tv", controller.searchMissing());
        verify(sonarrClient).searchSeries(List.of(55));
        verify(tvRequestRepository).saveAll(List.of(tv));
        assertNotNull(tv.getSonarrLastSearched());
    }

    // ---- tv ----

    @Test
    void tv_addsTvRequestsAttributeAndReturnsView() {
        TvRequest tv = new TvRequest("Show", 1, false, 1, "s");
        when(tvRequestRepository.findAll()).thenReturn(List.of(tv));
        ConcurrentModel model = new ConcurrentModel();

        String view = controller.tv(model);

        assertEquals("tv", view);
        assertNotNull(model.getAttribute("tvRequests"));
    }

    // ---- search ----

    @Test
    void search_stampsLastSearchedAndRedirects() {
        TvRequest tv = new TvRequest("Show", 1, false, 1, "s");
        tv.setSonarrSeriesId(55);
        when(tvRequestRepository.findAll()).thenReturn(List.of(tv));
        when(sonarrClient.searchSeries(List.of(55))).thenReturn(new SonarrCommand());

        assertEquals("redirect:/tv", controller.search(55));
        verify(tvRequestRepository).save(tv);
        assertNotNull(tv.getSonarrLastSearched());
    }

    // ---- refresh ----

    @Test
    void refresh_callsRefreshOneAndRedirects() {
        assertEquals("redirect:/tv", controller.refresh(1L));
        verify(tvRefreshService).refreshOne(1L);
    }

    // ---- setQualityProfileToAny ----

    @Test
    void setQualityProfileToAny_notFound_throwsRequestNotFoundException() {
        when(tvRequestRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(RequestNotFoundException.class, () -> controller.setQualityProfileToAny(99L));
    }

    @Test
    void setQualityProfileToAny_withNullSonarrId_redirectsWithoutChangingProfile() {
        TvRequest tv = new TvRequest("Show", 1, false, 1, "s");
        when(tvRequestRepository.findById(1L)).thenReturn(Optional.of(tv));

        assertEquals("redirect:/tv", controller.setQualityProfileToAny(1L));
        verify(sonarrClient, never()).getQualityProfileIdByName(any());
    }

    @Test
    void setQualityProfileToAny_withNullProfileId_redirectsWithoutUpdating() {
        TvRequest tv = new TvRequest("Show", 1, false, 1, "s");
        tv.setSonarrSeriesId(55);
        when(tvRequestRepository.findById(1L)).thenReturn(Optional.of(tv));
        when(sonarrClient.getQualityProfileIdByName("Any")).thenReturn(null);

        assertEquals("redirect:/tv", controller.setQualityProfileToAny(1L));
        verify(sonarrClient, never()).updateSeriesQualityProfile(any(), any());
    }

    @Test
    void setQualityProfileToAny_withValidProfile_updatesAndRefreshes() {
        TvRequest tv = new TvRequest("Show", 1, false, 1, "s");
        tv.setId(3L);
        tv.setSonarrSeriesId(55);
        when(tvRequestRepository.findById(3L)).thenReturn(Optional.of(tv));
        when(sonarrClient.getQualityProfileIdByName("Any")).thenReturn(5);

        assertEquals("redirect:/tv", controller.setQualityProfileToAny(3L));
        verify(sonarrClient).updateSeriesQualityProfile(55, 5);
        verify(tvRefreshService).refreshOne(3L);
    }

    // ---- validate ----

    @Test
    void validate_foundRequest_callsValidateWithEpisodesAndRedirects() {
        TvRequest tv = new TvRequest("Show", 1, false, 1, "s");
        when(tvRequestRepository.findById(1L)).thenReturn(Optional.of(tv));

        assertEquals("redirect:/tv", controller.validate(1L));
        verify(validatorService).validateWithEpisodes(tv);
    }

    @Test
    void validate_notFound_throwsRequestNotFoundException() {
        when(tvRequestRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(RequestNotFoundException.class, () -> controller.validate(99L));
    }

    // ---- validateAll ----

    @Test
    void validateAll_callsValidateAllTvAndRedirects() {
        assertEquals("redirect:/tv", controller.validateAll());
        verify(validatorService).validateAllTv();
    }

    // ---- searchOne ----

    @Test
    void searchOne_withNullSonarrId_redirectsWithoutSearch() {
        TvRequest tv = new TvRequest("Show", 1, false, 1, "s");
        when(tvRequestRepository.findById(1L)).thenReturn(Optional.of(tv));

        assertEquals("redirect:/tv", controller.searchOne(1L));
        verify(sonarrClient, never()).searchSeries(any());
    }

    @Test
    void searchOne_withSonarrId_triggersSearchAndStampsTime() {
        TvRequest tv = new TvRequest("Show", 1, false, 1, "s");
        tv.setId(2L);
        tv.setSonarrSeriesId(55);
        when(tvRequestRepository.findById(2L)).thenReturn(Optional.of(tv));
        when(sonarrClient.searchSeries(List.of(55))).thenReturn(new SonarrCommand());

        assertEquals("redirect:/tv", controller.searchOne(2L));
        verify(sonarrClient).searchSeries(List.of(55));
        verify(tvRequestRepository).save(tv);
        assertNotNull(tv.getSonarrLastSearched());
    }

    @Test
    void searchOne_notFound_throwsRequestNotFoundException() {
        when(tvRequestRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(RequestNotFoundException.class, () -> controller.searchOne(99L));
    }

    // ---- searchAllSeries ----

    @Test
    void searchAllSeries_withNoUnavailableSeries_redirectsWithoutSearch() {
        // A fully-available show (sonarr counts equal) should be skipped
        TvRequest tv = new TvRequest("Show", 1, false, 1, "Common.Available");
        tv.setSonarrSeriesId(55);
        tv.setSonarrEpisodeFileCount(5);
        tv.setSonarrEpisodeCount(5);
        when(tvRequestRepository.findAll()).thenReturn(List.of(tv));

        assertEquals("redirect:/tv", controller.searchAllSeries());
        verify(sonarrClient, never()).searchSeries(any());
    }

    @Test
    void searchAllSeries_withUnavailableSeries_triggersSearchAndStampsTime() {
        TvRequest tv = new TvRequest("Show", 1, false, 1, "s");
        tv.setSonarrSeriesId(55);
        when(tvRequestRepository.findAll()).thenReturn(List.of(tv));
        when(sonarrClient.searchSeries(List.of(55))).thenReturn(new SonarrCommand());

        assertEquals("redirect:/tv", controller.searchAllSeries());
        verify(sonarrClient).searchSeries(List.of(55));
        verify(tvRequestRepository).saveAll(List.of(tv));
        assertNotNull(tv.getSonarrLastSearched());
    }

    // ---- searchAllSeasons ----

    @Test
    void searchAllSeasons_withNoSonarrId_skipsRequest() {
        TvRequest tv = new TvRequest("Show", 1, false, 1, "s");
        tv.setId(1L);
        when(tvRequestRepository.findAll()).thenReturn(List.of(tv));

        controller.searchAllSeasons();

        verify(sonarrClient, never()).searchSeason(any(), any());
    }

    @Test
    void searchAllSeasons_withAvailableSeason_skipsSearch() {
        TvRequest parent = new TvRequest("Show", 1, false, 100, "s");
        parent.setId(1L);
        parent.setSonarrSeriesId(55);
        TvChildRequest child = new TvChildRequest(parent, "Show", 1, false, 201, "s");
        TvSeasonRequest season = new TvSeasonRequest(child, 1, 2, true); // available

        when(tvRequestRepository.findAll()).thenReturn(List.of(parent));
        when(tvChildRequestRepository.findByParentOrderByIdAsc(parent)).thenReturn(List.of(child));
        child.setSeasonRequests(List.of(season));

        controller.searchAllSeasons();

        verify(sonarrClient, never()).searchSeason(any(), any());
    }

    @Test
    void searchAllSeasons_withUnavailableSeason_triggersSeasonSearch() {
        TvRequest parent = new TvRequest("Show", 1, false, 100, "s");
        parent.setId(1L);
        parent.setSonarrSeriesId(55);
        TvChildRequest child = new TvChildRequest(parent, "Show", 1, false, 201, "s");
        TvSeasonRequest season = new TvSeasonRequest(child, 1, 2, false); // not available

        when(tvRequestRepository.findAll()).thenReturn(List.of(parent));
        when(tvChildRequestRepository.findByParentOrderByIdAsc(parent)).thenReturn(List.of(child));
        child.setSeasonRequests(List.of(season));
        when(sonarrClient.searchSeason(55, 2)).thenReturn(new SonarrCommand());

        controller.searchAllSeasons();

        verify(sonarrClient).searchSeason(55, 2);
    }

    // ---- searchAllEpisodes ----

    @Test
    void searchAllEpisodes_withNoSonarrId_skipsRequest() {
        TvRequest tv = new TvRequest("Show", 1, false, 1, "s");
        tv.setId(1L);
        when(tvRequestRepository.findAll()).thenReturn(List.of(tv));

        controller.searchAllEpisodes();

        verify(sonarrClient, never()).searchEpisodes(any());
    }

    @Test
    void searchAllEpisodes_withUnavailableEpisode_triggersEpisodeSearch() {
        TvRequest parent = new TvRequest("Show", 1, false, 100, "s");
        parent.setId(1L);
        parent.setSonarrSeriesId(55);
        TvChildRequest child = new TvChildRequest(parent, "Show", 1, false, 201, "s");
        TvSeasonRequest season = new TvSeasonRequest(child, 1, 2, false);
        TvEpisodeRequest episode = new TvEpisodeRequest(season, 100, 3);
        episode.setOmbiAvailable(false);
        season.setEpisodeRequests(List.of(episode));
        child.setSeasonRequests(List.of(season));

        when(tvRequestRepository.findAll()).thenReturn(List.of(parent));
        when(tvChildRequestRepository.findByParentOrderByIdAsc(parent)).thenReturn(List.of(child));
        when(sonarrClient.getEpisodes(55)).thenReturn(List.of(sonarrEpisode(999, 2, 3)));
        when(sonarrClient.searchEpisodes(List.of(999))).thenReturn(new SonarrCommand());

        controller.searchAllEpisodes();

        verify(sonarrClient).searchEpisodes(List.of(999));
    }

    // ---- searchAllSeasonsForRequest ----

    @Test
    void searchAllSeasonsForRequest_triggersSeasonSearchForAllSeasons() {
        TvRequest parent = new TvRequest("Show", 1, false, 100, "s");
        parent.setId(1L);
        parent.setSonarrSeriesId(55);
        TvChildRequest child = new TvChildRequest(parent, "Show", 1, false, 201, "s");
        TvSeasonRequest season = new TvSeasonRequest(child, 1, 2, false);
        child.setSeasonRequests(List.of(season));

        when(tvRequestRepository.findById(1L)).thenReturn(Optional.of(parent));
        when(tvChildRequestRepository.findByParentOrderByIdAsc(parent)).thenReturn(List.of(child));
        when(sonarrClient.searchSeason(55, 2)).thenReturn(new SonarrCommand());

        controller.searchAllSeasonsForRequest(1L);

        verify(sonarrClient).searchSeason(55, 2);
    }

    @Test
    void searchAllSeasonsForRequest_notFound_throwsRequestNotFoundException() {
        when(tvRequestRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(RequestNotFoundException.class, () -> controller.searchAllSeasonsForRequest(99L));
    }

    // ---- searchAllEpisodesForRequest ----

    @Test
    void searchAllEpisodesForRequest_triggersEpisodeSearch() {
        TvRequest parent = new TvRequest("Show", 1, false, 100, "s");
        parent.setId(1L);
        parent.setSonarrSeriesId(55);
        TvChildRequest child = new TvChildRequest(parent, "Show", 1, false, 201, "s");
        TvSeasonRequest season = new TvSeasonRequest(child, 1, 2, false);
        TvEpisodeRequest episode = new TvEpisodeRequest(season, 100, 3);
        season.setEpisodeRequests(List.of(episode));
        child.setSeasonRequests(List.of(season));

        when(tvRequestRepository.findById(1L)).thenReturn(Optional.of(parent));
        when(tvChildRequestRepository.findByParentOrderByIdAsc(parent)).thenReturn(List.of(child));
        when(sonarrClient.getEpisodes(55)).thenReturn(List.of(sonarrEpisode(999, 2, 3)));
        when(sonarrClient.searchEpisodes(List.of(999))).thenReturn(new SonarrCommand());

        controller.searchAllEpisodesForRequest(1L);

        verify(sonarrClient).searchEpisodes(List.of(999));
    }

    // ---- searchAllEpisodesForChild ----

    @Test
    void searchAllEpisodesForChild_triggersEpisodeSearch() {
        TvRequest parent = new TvRequest("Show", 1, false, 100, "s");
        parent.setSonarrSeriesId(55);
        TvChildRequest child = new TvChildRequest(parent, "Show", 1, false, 201, "s");
        child.setId(10L);
        TvSeasonRequest season = new TvSeasonRequest(child, 1, 2, false);
        TvEpisodeRequest episode = new TvEpisodeRequest(season, 100, 3);
        season.setEpisodeRequests(List.of(episode));
        child.setSeasonRequests(List.of(season));

        when(tvChildRequestRepository.findById(10L)).thenReturn(Optional.of(child));
        when(sonarrClient.getEpisodes(55)).thenReturn(List.of(sonarrEpisode(999, 2, 3)));
        when(sonarrClient.searchEpisodes(List.of(999))).thenReturn(new SonarrCommand());

        controller.searchAllEpisodesForChild(10L);

        verify(sonarrClient).searchEpisodes(List.of(999));
    }

    // ---- searchAllEpisodesForSeason ----

    @Test
    void searchAllEpisodesForSeason_triggersEpisodeSearch() {
        TvRequest parent = new TvRequest("Show", 1, false, 100, "s");
        parent.setSonarrSeriesId(55);
        TvChildRequest child = new TvChildRequest(parent, "Show", 1, false, 201, "s");
        TvSeasonRequest season = new TvSeasonRequest(child, 1, 2, false);
        season.setId(20L);
        TvEpisodeRequest episode = new TvEpisodeRequest(season, 100, 3);
        season.setEpisodeRequests(List.of(episode));

        when(tvSeasonRequestRepository.findById(20L)).thenReturn(Optional.of(season));
        when(sonarrClient.getEpisodes(55)).thenReturn(List.of(sonarrEpisode(999, 2, 3)));
        when(sonarrClient.searchEpisodes(List.of(999))).thenReturn(new SonarrCommand());

        controller.searchAllEpisodesForSeason(20L);

        verify(sonarrClient).searchEpisodes(List.of(999));
    }

    // ---- searchSeries ----

    @Test
    void searchSeries_withEmptyCollection_doesNotCallSonarr() {
        controller.searchSeries(List.of());

        verify(sonarrClient, never()).searchSeries(any());
    }

    @Test
    void searchSeries_withNullEntries_deduplicatesAndCallsSonarr() {
        TvRequest tv = new TvRequest("Show", 1, false, 1, "s");
        tv.setSonarrSeriesId(55);
        when(tvRequestRepository.findAll()).thenReturn(List.of(tv));
        when(sonarrClient.searchSeries(List.of(55))).thenReturn(new SonarrCommand());

        controller.searchSeries(Arrays.asList(55, null, 55));

        verify(sonarrClient).searchSeries(List.of(55));
        verify(tvRequestRepository).saveAll(List.of(tv));
    }

    // ---- markAvailable ----

    @Test
    void markAvailable_notFound_throwsRequestNotFoundException() {
        when(tvRequestRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(RequestNotFoundException.class, () -> controller.markAvailable(99L));
    }

    @Test
    void markAvailable_withNoChildren_redirectsWithoutCallingOmbi() {
        TvRequest parent = new TvRequest("Show", 1, false, 100, "s");
        parent.setId(7L);
        when(tvRequestRepository.findById(7L)).thenReturn(Optional.of(parent));
        when(tvChildRequestRepository.findByParentOrderByIdAsc(parent)).thenReturn(List.of());

        assertEquals("redirect:/tv", controller.markAvailable(7L));
        verify(ombiClient, never()).markTvAvailable(any());
    }

    @Test
    void markAvailable_withChildHavingNullOmbiId_skipsChild() {
        TvRequest parent = new TvRequest("Show", 1, false, 100, "s");
        parent.setId(7L);
        TvChildRequest child = new TvChildRequest(parent, "Show", 1, false, 201, "s");
        child.setOmbiRequestId(null);
        when(tvRequestRepository.findById(7L)).thenReturn(Optional.of(parent));
        when(tvChildRequestRepository.findByParentOrderByIdAsc(parent)).thenReturn(List.of(child));

        assertEquals("redirect:/tv", controller.markAvailable(7L));
        verify(ombiClient, never()).markTvAvailable(any());
    }

    @Test
    void markAvailable_jacksonExceptionOnLogging_stillRedirects() throws Exception {
        TvRequest parent = new TvRequest("Show", 1, false, 100, "s");
        parent.setId(7L);
        TvChildRequest child = new TvChildRequest(parent, "Show", 1, false, 201, "s");
        when(tvRequestRepository.findById(7L)).thenReturn(Optional.of(parent));
        when(tvChildRequestRepository.findByParentOrderByIdAsc(parent)).thenReturn(List.of(child));
        when(ombiClient.markTvAvailable(201)).thenReturn(new OmbiReprocessResponse());
        when(objectMapper.writeValueAsString(any())).thenThrow(mock(JacksonException.class));

        assertEquals("redirect:/tv", controller.markAvailable(7L));
    }

    // ---- delete ----

    @Test
    void delete_callsAdminServiceAndRedirects() {
        assertEquals("redirect:/tv", controller.delete(1L));
        verify(requestAdminService).delete(tvRequestRepository, 1L);
    }

    // ---- addNote ----

    @Test
    void addNote_delegatesToAdminServiceAndReturnsNote() {
        Note note = new Note("text", null);
        when(requestAdminService.addNote(tvRequestRepository, 1L, "text")).thenReturn(note);

        Note result = controller.addNote(1L, "text");

        assertEquals(note, result);
    }

    // ---- markStale ----

    @Test
    void markStale_delegatesToAdminServiceAndRedirects() {
        assertEquals("redirect:/tv", controller.markStale(1L, "reason"));
        verify(requestAdminService).markStale(tvRequestRepository, 1L, "reason");
    }

    // ---- searchSeasons and searchEpisodes null-seriesId and empty-keys branches ----

    @Test
    void searchAllSeasonsForRequest_withNullSonarrSeriesId_skipsSearch() {
        // TvRequest with null sonarrSeriesId → searchSeasons logs warn and returns
        TvRequest tv = new TvRequest("Show", 1, false, 100, "s");
        tv.setId(1L);
        // sonarrSeriesId is null by default
        TvChildRequest child = new TvChildRequest(tv, "Show", 1, false, 201, "s");
        TvSeasonRequest season = new TvSeasonRequest(child, 1, 2, false);
        child.setSeasonRequests(List.of(season));

        when(tvRequestRepository.findById(1L)).thenReturn(Optional.of(tv));
        when(tvChildRequestRepository.findByParentOrderByIdAsc(tv)).thenReturn(List.of(child));

        controller.searchAllSeasonsForRequest(1L);

        verify(sonarrClient, never()).searchSeason(any(), any());
    }

    @Test
    void searchAllEpisodesForRequest_withEmptyEpisodes_skipsSearch() {
        // Seasons with no episodes → episodeKeysOfSeasons returns empty set → searchEpisodes logs and returns
        TvRequest tv = new TvRequest("Show", 1, false, 100, "s");
        tv.setId(1L);
        tv.setSonarrSeriesId(55);
        TvChildRequest child = new TvChildRequest(tv, "Show", 1, false, 201, "s");
        TvSeasonRequest season = new TvSeasonRequest(child, 1, 2, false);
        season.setEpisodeRequests(List.of()); // no episodes
        child.setSeasonRequests(List.of(season));

        when(tvRequestRepository.findById(1L)).thenReturn(Optional.of(tv));
        when(tvChildRequestRepository.findByParentOrderByIdAsc(tv)).thenReturn(List.of(child));

        controller.searchAllEpisodesForRequest(1L);

        verify(sonarrClient, never()).searchEpisodes(any());
    }

    @Test
    void searchAllEpisodesForRequest_withNullSonarrSeriesId_skipsSearch() {
        // TvRequest with null sonarrSeriesId → searchEpisodes logs warn and returns
        TvRequest tv = new TvRequest("Show", 1, false, 100, "s");
        tv.setId(1L);
        // sonarrSeriesId is null
        TvChildRequest child = new TvChildRequest(tv, "Show", 1, false, 201, "s");
        TvSeasonRequest season = new TvSeasonRequest(child, 1, 2, false);
        TvEpisodeRequest episode = new TvEpisodeRequest(season, 100, 3);
        season.setEpisodeRequests(List.of(episode));
        child.setSeasonRequests(List.of(season));

        when(tvRequestRepository.findById(1L)).thenReturn(Optional.of(tv));
        when(tvChildRequestRepository.findByParentOrderByIdAsc(tv)).thenReturn(List.of(child));

        controller.searchAllEpisodesForRequest(1L);

        verify(sonarrClient, never()).searchEpisodes(any());
    }

    @Test
    void searchAllEpisodesForSeason_withNullSeasonNumber_skipsEpisode() {
        // episode with no season → episodeKeysOfEpisodes skips it → empty keys → no search
        TvRequest parent = new TvRequest("Show", 1, false, 100, "s");
        parent.setSonarrSeriesId(55);
        TvChildRequest child = new TvChildRequest(parent, "Show", 1, false, 201, "s");
        TvSeasonRequest season = new TvSeasonRequest(child, 1, 2, false);
        season.setId(20L);
        // episode with null tvSeasonRequest → seasonNumber = null → skipped
        TvEpisodeRequest episode = new TvEpisodeRequest(null, 100, 3);
        season.setEpisodeRequests(List.of(episode));

        when(tvSeasonRequestRepository.findById(20L)).thenReturn(Optional.of(season));

        controller.searchAllEpisodesForSeason(20L);

        verify(sonarrClient, never()).searchEpisodes(any());
    }

    @Test
    void searchAllEpisodes_withSeasonHavingNullSeasonNumber_skipsEpisodes() {
        // Season with null ombiSeasonNumber → unavailableEpisodeKeys skips it
        TvRequest parent = new TvRequest("Show", 1, false, 100, "s");
        parent.setId(1L);
        parent.setSonarrSeriesId(55);
        TvChildRequest child = new TvChildRequest(parent, "Show", 1, false, 201, "s");
        TvSeasonRequest season = new TvSeasonRequest(child, 1, null, false); // null season number
        TvEpisodeRequest episode = new TvEpisodeRequest(season, 100, 3);
        season.setEpisodeRequests(List.of(episode));
        child.setSeasonRequests(List.of(season));

        when(tvRequestRepository.findAll()).thenReturn(List.of(parent));
        when(tvChildRequestRepository.findByParentOrderByIdAsc(parent)).thenReturn(List.of(child));

        controller.searchAllEpisodes();

        verify(sonarrClient, never()).searchEpisodes(any());
    }

    // ---- not-found paths for orElseThrow lambdas ----

    @Test
    void searchAllSeasonsForRequest_notFoundVariant_throwsRequestNotFoundException() {
        when(tvRequestRepository.findById(88L)).thenReturn(Optional.empty());

        assertThrows(RequestNotFoundException.class, () -> controller.searchAllSeasonsForRequest(88L));
    }

    @Test
    void searchAllEpisodesForRequest_notFound_throwsRequestNotFoundException() {
        when(tvRequestRepository.findById(88L)).thenReturn(Optional.empty());

        assertThrows(RequestNotFoundException.class, () -> controller.searchAllEpisodesForRequest(88L));
    }

    @Test
    void searchAllSeasonsForChild_notFound_throwsRequestNotFoundException() {
        when(tvChildRequestRepository.findById(88L)).thenReturn(Optional.empty());

        assertThrows(RequestNotFoundException.class, () -> controller.searchAllSeasonsForChild(88L));
    }

    @Test
    void searchAllEpisodesForChild_notFound_throwsRequestNotFoundException() {
        when(tvChildRequestRepository.findById(88L)).thenReturn(Optional.empty());

        assertThrows(RequestNotFoundException.class, () -> controller.searchAllEpisodesForChild(88L));
    }

    @Test
    void searchSeason_notFound_throwsRequestNotFoundException() {
        when(tvSeasonRequestRepository.findById(88L)).thenReturn(Optional.empty());

        assertThrows(RequestNotFoundException.class, () -> controller.searchSeason(88L));
    }

    @Test
    void searchAllEpisodesForSeason_notFound_throwsRequestNotFoundException() {
        when(tvSeasonRequestRepository.findById(88L)).thenReturn(Optional.empty());

        assertThrows(RequestNotFoundException.class, () -> controller.searchAllEpisodesForSeason(88L));
    }

    @Test
    void searchEpisode_notFound_throwsRequestNotFoundException() {
        when(tvEpisodeRequestRepository.findById(88L)).thenReturn(Optional.empty());

        assertThrows(RequestNotFoundException.class, () -> controller.searchEpisode(88L));
    }

    @Test
    void deleteEpisodeDownloadAndSearch_notFound_throwsRequestNotFoundException() {
        when(tvEpisodeRequestRepository.findById(88L)).thenReturn(Optional.empty());

        assertThrows(RequestNotFoundException.class, () -> controller.deleteEpisodeDownloadAndSearch(88L));
    }

    // ---- searchSeasons null seriesId branch (covers private method early return) ----

    @Test
    void searchAllSeasonsForChild_withNullSeriesId_skipsSearch() {
        // parent has no sonarrSeriesId → seriesId = null → searchSeasons logs and returns
        TvRequest parent = new TvRequest("Show", 1, false, 100, "s");
        // sonarrSeriesId is null by default
        TvChildRequest child = new TvChildRequest(parent, "Show", 1, false, 201, "s");
        child.setId(99L);
        child.setSeasonRequests(List.of(new TvSeasonRequest(child, 1, 2, false)));

        when(tvChildRequestRepository.findById(99L)).thenReturn(Optional.of(child));

        // searchSeasons with null seriesId → logs warn, no sonarrClient call
        controller.searchAllSeasonsForChild(99L);

        verify(sonarrClient, never()).searchSeason(any(), any());
    }

    // ---- deleteEpisodeDownloadAndSearch edge cases ----

    @Test
    void deleteEpisodeDownloadAndSearch_missingSonarrInfo_skipsDeleteButStillSearches() {
        // episode with no season (null tvSeasonRequest) → seriesId = null
        TvRequest parent = new TvRequest("Show", 1, false, 100, "s");
        parent.setSonarrSeriesId(55);
        TvChildRequest child = new TvChildRequest(parent, "Show", 1, false, 201, "s");
        TvSeasonRequest season = new TvSeasonRequest(child, 1, 2, false);
        TvEpisodeRequest episode = new TvEpisodeRequest(season, 100, 3);
        episode.setId(40L);
        // Override season so seriesId lookup returns null by clearing the parent chain
        TvEpisodeRequest episodeNoSeason = new TvEpisodeRequest(null, 100, 3);
        episodeNoSeason.setId(40L);

        when(tvEpisodeRequestRepository.findById(40L)).thenReturn(Optional.of(episodeNoSeason));
        // searchEpisode will call sonarrClient.getEpisodes with null seriesId — but since seriesId
        // is null, searchEpisodes is skipped entirely.

        controller.deleteEpisodeDownloadAndSearch(40L);

        verify(sonarrClient, never()).getQueue();
        verify(sonarrClient, never()).searchEpisodes(any());
    }

    @Test
    void deleteEpisodeDownloadAndSearch_queueFetchThrows_skipsDeleteAndStillSearches() {
        TvRequest parent = new TvRequest("Show", 1, false, 100, "s");
        parent.setSonarrSeriesId(55);
        TvChildRequest child = new TvChildRequest(parent, "Show", 1, false, 201, "s");
        TvSeasonRequest season = new TvSeasonRequest(child, 1, 2, false);
        TvEpisodeRequest episode = new TvEpisodeRequest(season, 100, 3);
        episode.setId(30L);

        when(tvEpisodeRequestRepository.findById(30L)).thenReturn(Optional.of(episode));
        when(sonarrClient.getQueue()).thenThrow(new RuntimeException("queue down"));
        when(sonarrClient.getEpisodes(55)).thenReturn(List.of(sonarrEpisode(999, 2, 3)));
        when(sonarrClient.searchEpisodes(List.of(999))).thenReturn(new SonarrCommand());

        controller.deleteEpisodeDownloadAndSearch(30L);

        verify(sonarrClient).getQueue();
        verify(sonarrClient, never()).deleteQueueItem(any());
        verify(sonarrClient).searchEpisodes(List.of(999));
    }

    // ---- helpers ----

    /** A TvRequest matching the searchMissing filter. */
    private static TvRequest processingTvWithEpisodes(Integer sonarrSeriesId) {
        TvRequest tv = new TvRequest("Show", 1, false, 1, "Common.ProcessingRequest");
        tv.setSonarrSeriesId(sonarrSeriesId);
        tv.setSonarrEpisodeFileCount(1);
        tv.setSonarrTotalEpisodeCount(5);
        return tv;
    }

    private static Episode sonarrEpisode(Integer id, Integer seasonNumber, Integer episodeNumber) {
        Episode episode = new Episode();
        episode.setId(id);
        episode.setSeasonNumber(seasonNumber);
        episode.setEpisodeNumber(episodeNumber);
        return episode;
    }

    private static SonarrQueueRecord queueRecord(Integer id, Integer seriesId, Integer season, Integer episodeNumber) {
        SonarrQueueRecord record = new SonarrQueueRecord();
        record.setId(id);
        record.setSeriesId(seriesId);
        record.setSeasonNumber(season);
        record.setEpisode(sonarrEpisode(null, season, episodeNumber));
        return record;
    }

    private static SonarrQueue queueOf(SonarrQueueRecord... records) {
        SonarrQueue queue = new SonarrQueue();
        queue.setRecords(List.of(records));
        return queue;
    }
}
