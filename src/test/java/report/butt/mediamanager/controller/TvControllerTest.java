package report.butt.mediamanager.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import report.butt.mediamanager.client.OmbiClient;
import report.butt.mediamanager.client.SonarrClient;
import report.butt.mediamanager.model.TvChildRequest;
import report.butt.mediamanager.model.TvEpisodeRequest;
import report.butt.mediamanager.model.TvRequest;
import report.butt.mediamanager.model.TvSeasonRequest;
import report.butt.mediamanager.model.sonarr.Episode;
import report.butt.mediamanager.model.sonarr.SonarrCommand;
import report.butt.mediamanager.repository.NoteRepository;
import report.butt.mediamanager.repository.TvChildRequestRepository;
import report.butt.mediamanager.repository.TvEpisodeRequestRepository;
import report.butt.mediamanager.repository.TvRequestRepository;
import report.butt.mediamanager.repository.TvSeasonRequestRepository;
import report.butt.mediamanager.repository.ValidationRepository;
import report.butt.mediamanager.service.TvRefreshService;
import report.butt.mediamanager.service.ValidatorService;
import tools.jackson.databind.ObjectMapper;

class TvControllerTest {

    private final TvRequestRepository tvRequestRepository = mock(TvRequestRepository.class);
    private final TvChildRequestRepository tvChildRequestRepository = mock(TvChildRequestRepository.class);
    private final TvSeasonRequestRepository tvSeasonRequestRepository = mock(TvSeasonRequestRepository.class);
    private final TvEpisodeRequestRepository tvEpisodeRequestRepository = mock(TvEpisodeRequestRepository.class);
    private final OmbiClient ombiClient = mock(OmbiClient.class);
    private final SonarrClient sonarrClient = mock(SonarrClient.class);
    private final ObjectMapper objectMapper = mock(ObjectMapper.class);

    private final TvController controller = new TvController(
            tvRequestRepository,
            tvChildRequestRepository,
            tvSeasonRequestRepository,
            tvEpisodeRequestRepository,
            mock(ValidationRepository.class),
            mock(NoteRepository.class),
            ombiClient,
            sonarrClient,
            objectMapper,
            mock(TvRefreshService.class),
            mock(ValidatorService.class));

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
        child.setSeasonRequests(List.of(new TvSeasonRequest(child, 1, 1, false), new TvSeasonRequest(child, 2, 2, false)));

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

    private static Episode sonarrEpisode(Integer id, Integer seasonNumber, Integer episodeNumber) {
        Episode episode = new Episode();
        episode.setId(id);
        episode.setSeasonNumber(seasonNumber);
        episode.setEpisodeNumber(episodeNumber);
        return episode;
    }
}
