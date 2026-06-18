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
import org.jobrunr.scheduling.JobRequestScheduler;
import org.junit.jupiter.api.Test;
import org.springframework.ui.ConcurrentModel;
import report.butt.mediamanager.client.OmbiClient;
import report.butt.mediamanager.client.RadarrClient;
import report.butt.mediamanager.exceptions.RequestNotFoundException;
import report.butt.mediamanager.job.FfprobeScanJobRequest;
import report.butt.mediamanager.model.FfprobeScan;
import report.butt.mediamanager.model.MovieRequest;
import report.butt.mediamanager.model.Note;
import report.butt.mediamanager.model.ombi.OmbiReprocessResponse;
import report.butt.mediamanager.model.radarr.RadarrCommand;
import report.butt.mediamanager.model.radarr.RadarrHealthItem;
import report.butt.mediamanager.model.radarr.RadarrQueue;
import report.butt.mediamanager.model.radarr.RadarrQueueRecord;
import report.butt.mediamanager.repository.MovieRequestRepository;
import report.butt.mediamanager.service.FfprobeScanService;
import report.butt.mediamanager.service.MovieRefreshService;
import report.butt.mediamanager.service.RequestAdminService;
import report.butt.mediamanager.service.ValidatorService;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

class MovieControllerTest {

    private final MovieRequestRepository movieRequestRepository = mock(MovieRequestRepository.class);
    private final RadarrClient radarrClient = mock(RadarrClient.class);
    private final OmbiClient ombiClient = mock(OmbiClient.class);
    private final ObjectMapper objectMapper = mock(ObjectMapper.class);
    private final MovieRefreshService movieRefreshService = mock(MovieRefreshService.class);
    private final ValidatorService validatorService = mock(ValidatorService.class);
    private final RequestAdminService requestAdminService = mock(RequestAdminService.class);
    private final FfprobeScanService ffprobeScanService = mock(FfprobeScanService.class);
    private final JobRequestScheduler jobRequestScheduler = mock(JobRequestScheduler.class);

    private final MovieController controller = new MovieController(
            movieRequestRepository,
            ombiClient,
            radarrClient,
            objectMapper,
            movieRefreshService,
            validatorService,
            requestAdminService,
            ffprobeScanService,
            jobRequestScheduler);

    // ---- getRadarrQueue ----

    @Test
    void getRadarrQueue_returnsQueueOnSuccess() {
        var queue = new RadarrQueue();
        when(radarrClient.getQueue()).thenReturn(queue);

        RadarrQueue result = controller.getRadarrQueue();

        assertNotNull(result);
    }

    @Test
    void getRadarrQueue_returnsNullOnException() {
        when(radarrClient.getQueue()).thenThrow(new RuntimeException("unreachable"));

        assertNull(controller.getRadarrQueue());
    }

    // ---- getRadarrHealth ----

    @Test
    void getRadarrHealth_returnsListOnSuccess() {
        var health = List.of(new RadarrHealthItem());
        when(radarrClient.getHealth()).thenReturn(health);

        List<RadarrHealthItem> result = controller.getRadarrHealth();

        assertEquals(health, result);
    }

    @Test
    void getRadarrHealth_returnsNullOnException() {
        when(radarrClient.getHealth()).thenThrow(new RuntimeException("unreachable"));

        assertNull(controller.getRadarrHealth());
    }

    // ---- refreshAll ----

    @Test
    void refreshAll_callsRefreshAllAndRedirects() {
        assertEquals("redirect:/movies", controller.refreshAll());
        verify(movieRefreshService).refreshAll();
    }

    // ---- searchMissing ----

    @Test
    void searchMissing_withNoMatchingMovies_redirectsWithoutCallingRadarr() {
        when(movieRequestRepository.findAll()).thenReturn(List.of());

        assertEquals("redirect:/movies", controller.searchMissing());
        verify(radarrClient, never()).searchMovies(any());
    }

    @Test
    void searchMissing_withMatchingMovies_triggersSearchAndStampsTime() {
        MovieRequest m = processingMovieWithFile(42);
        when(movieRequestRepository.findAll()).thenReturn(List.of(m));
        when(radarrClient.searchMovies(List.of(42))).thenReturn(new RadarrCommand());

        assertEquals("redirect:/movies", controller.searchMissing());
        verify(radarrClient).searchMovies(List.of(42));
        verify(movieRequestRepository).saveAll(List.of(m));
        assertNotNull(m.getRadarrLastSearchTime());
    }

    // ---- movies ----

    @Test
    void movies_addsMoviesAttributeAndReturnsView() {
        var m = new MovieRequest("A", 1, false, 1, "status");
        when(movieRequestRepository.findAll()).thenReturn(List.of(m));
        var model = new ConcurrentModel();

        String view = controller.movies(model);

        assertEquals("movies", view);
        assertNotNull(model.getAttribute("movies"));
    }

    // ---- search ----

    @Test
    void search_stampsSingleMovieAndRedirects() {
        var m = new MovieRequest("Movie", 1, false, 10, "status");
        m.setRadarrRequestId(7);
        when(movieRequestRepository.findAll()).thenReturn(List.of(m));
        when(radarrClient.searchMovies(List.of(7))).thenReturn(new RadarrCommand());

        assertEquals("redirect:/movies", controller.search(7));
        verify(movieRequestRepository).save(m);
        assertNotNull(m.getRadarrLastSearchTime());
    }

    // ---- refresh ----

    @Test
    void refresh_callsRefreshOneAndRedirects() {
        assertEquals("redirect:/movies", controller.refresh(1L));
        verify(movieRefreshService).refreshOne(1L);
    }

    // ---- validate ----

    @Test
    void validate_foundRequest_callsValidateAndRedirects() {
        var m = new MovieRequest("M", 1, false, 1, "s");
        when(movieRequestRepository.findById(1L)).thenReturn(Optional.of(m));

        assertEquals("redirect:/movies", controller.validate(1L));
        verify(validatorService).validate(m);
    }

    @Test
    void validate_notFound_throwsRequestNotFoundException() {
        when(movieRequestRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(RequestNotFoundException.class, () -> controller.validate(99L));
    }

    // ---- validateAll ----

    @Test
    void validateAll_callsValidateAllMoviesAndRedirects() {
        assertEquals("redirect:/movies", controller.validateAll());
        verify(validatorService).validateAllMovies();
    }

    // ---- searchOne ----

    @Test
    void searchOne_withNullRadarrRequestId_redirectsWithoutSearch() {
        var m = new MovieRequest("M", 1, false, 1, "s");
        when(movieRequestRepository.findById(1L)).thenReturn(Optional.of(m));

        assertEquals("redirect:/movies", controller.searchOne(1L));
        verify(radarrClient, never()).searchMovies(any());
    }

    @Test
    void searchOne_withRadarrRequestId_triggersSearchAndStampsTime() {
        var m = new MovieRequest("M", 1, false, 1, "s");
        m.setId(2L);
        m.setRadarrRequestId(55);
        when(movieRequestRepository.findById(2L)).thenReturn(Optional.of(m));
        when(radarrClient.searchMovies(List.of(55))).thenReturn(new RadarrCommand());

        assertEquals("redirect:/movies", controller.searchOne(2L));
        verify(radarrClient).searchMovies(List.of(55));
        verify(movieRequestRepository).save(m);
        assertNotNull(m.getRadarrLastSearchTime());
    }

    @Test
    void searchOne_notFound_throwsRequestNotFoundException() {
        when(movieRequestRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(RequestNotFoundException.class, () -> controller.searchOne(99L));
    }

    // ---- searchAll ----

    @Test
    void searchAll_withNoMoviesHavingRadarrId_redirectsWithoutSearch() {
        var m = new MovieRequest("M", 1, false, 1, "s");
        when(movieRequestRepository.findAll()).thenReturn(List.of(m));

        assertEquals("redirect:/movies", controller.searchAll());
        verify(radarrClient, never()).searchMovies(any());
    }

    @Test
    void searchAll_withMovies_triggersSearchAndStampsTime() {
        var m = new MovieRequest("M", 1, false, 1, "s");
        m.setRadarrRequestId(77);
        when(movieRequestRepository.findAll()).thenReturn(List.of(m));
        when(radarrClient.searchMovies(List.of(77))).thenReturn(new RadarrCommand());

        assertEquals("redirect:/movies", controller.searchAll());
        verify(radarrClient).searchMovies(List.of(77));
        verify(movieRequestRepository).saveAll(List.of(m));
        assertNotNull(m.getRadarrLastSearchTime());
    }

    // ---- searchMovies ----

    @Test
    void searchMovies_withEmptyCollection_doesNotCallRadarr() {
        controller.searchMovies(List.of());

        verify(radarrClient, never()).searchMovies(any());
    }

    @Test
    void searchMovies_withNullEntries_deduplicatesAndCallsRadarr() {
        var m = new MovieRequest("M", 1, false, 1, "s");
        m.setRadarrRequestId(10);
        when(movieRequestRepository.findAll()).thenReturn(List.of(m));
        when(radarrClient.searchMovies(List.of(10))).thenReturn(new RadarrCommand());

        controller.searchMovies(Arrays.asList(10, null, 10));

        verify(radarrClient).searchMovies(List.of(10));
        verify(movieRequestRepository).saveAll(List.of(m));
    }

    // ---- setQualityProfileToAny ----

    @Test
    void setQualityProfileToAny_notFound_throwsRequestNotFoundException() {
        when(movieRequestRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(RequestNotFoundException.class, () -> controller.setQualityProfileToAny(99L));
    }

    @Test
    void setQualityProfileToAny_withNullRadarrId_redirectsWithoutChangingProfile() {
        var m = new MovieRequest("M", 1, false, 1, "s");
        when(movieRequestRepository.findById(1L)).thenReturn(Optional.of(m));

        assertEquals("redirect:/movies", controller.setQualityProfileToAny(1L));
        verify(radarrClient, never()).getQualityProfileIdByName(any());
    }

    @Test
    void setQualityProfileToAny_withNullProfileId_redirectsWithoutUpdating() {
        var m = new MovieRequest("M", 1, false, 1, "s");
        m.setRadarrRequestId(33);
        when(movieRequestRepository.findById(1L)).thenReturn(Optional.of(m));
        when(radarrClient.getQualityProfileIdByName("Any")).thenReturn(null);

        assertEquals("redirect:/movies", controller.setQualityProfileToAny(1L));
        verify(radarrClient, never()).updateMovieQualityProfile(any(), any());
    }

    @Test
    void setQualityProfileToAny_withValidProfile_updatesAndRefreshes() {
        var m = new MovieRequest("M", 1, false, 1, "s");
        m.setId(3L);
        m.setRadarrRequestId(33);
        when(movieRequestRepository.findById(3L)).thenReturn(Optional.of(m));
        when(radarrClient.getQualityProfileIdByName("Any")).thenReturn(5);

        assertEquals("redirect:/movies", controller.setQualityProfileToAny(3L));
        verify(radarrClient).updateMovieQualityProfile(33, 5);
        verify(movieRefreshService).refreshOne(3L);
    }

    // ---- markAvailable ----

    @Test
    void markAvailable_notFound_throwsRequestNotFoundException() {
        when(movieRequestRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(RequestNotFoundException.class, () -> controller.markAvailable(99L));
    }

    @Test
    void markAvailable_callsOmbiAndRedirects() throws Exception {
        var m = new MovieRequest("M", 1, false, 50, "s");
        m.setId(4L);
        when(movieRequestRepository.findById(4L)).thenReturn(Optional.of(m));
        when(ombiClient.markMovieAvailable(50)).thenReturn(new OmbiReprocessResponse());
        when(objectMapper.writeValueAsString(any())).thenReturn("{}");

        assertEquals("redirect:/movies", controller.markAvailable(4L));
        verify(ombiClient).markMovieAvailable(50);
    }

    @Test
    void markAvailable_jacksonExceptionOnLogging_stillRedirects() throws Exception {
        var m = new MovieRequest("M", 1, false, 50, "s");
        m.setId(4L);
        when(movieRequestRepository.findById(4L)).thenReturn(Optional.of(m));
        when(ombiClient.markMovieAvailable(50)).thenReturn(new OmbiReprocessResponse());
        when(objectMapper.writeValueAsString(any())).thenThrow(mock(JacksonException.class));

        assertEquals("redirect:/movies", controller.markAvailable(4L));
    }

    // ---- delete ----

    @Test
    void delete_callsAdminServiceAndRedirects() {
        assertEquals("redirect:/movies", controller.delete(1L));
        verify(requestAdminService).delete(movieRequestRepository, 1L);
    }

    // ---- addNote ----

    @Test
    void addNote_delegatesToAdminServiceAndReturnsNote() {
        var note = new Note("text", null);
        when(requestAdminService.addNote(movieRequestRepository, 1L, "text")).thenReturn(note);

        Note result = controller.addNote(1L, "text");

        assertEquals(note, result);
    }

    // ---- markStale ----

    @Test
    void markStale_delegatesToAdminServiceAndRedirects() {
        assertEquals("redirect:/movies", controller.markStale(1L, "reason"));
        verify(requestAdminService).markStale(movieRequestRepository, 1L, "reason");
    }

    // ---- reprocess ----

    @Test
    void reprocess_callsOmbiAndRefreshesAndRedirects() throws Exception {
        var m = new MovieRequest("M", 1, false, 50, "s");
        m.setId(5L);
        when(movieRequestRepository.findById(5L)).thenReturn(Optional.of(m));
        when(ombiClient.reprocessMovieRequest(50)).thenReturn(new OmbiReprocessResponse());
        when(objectMapper.writeValueAsString(any())).thenReturn("{}");

        assertEquals("redirect:/movies", controller.reprocess(5L));
        verify(ombiClient).reprocessMovieRequest(50);
        verify(movieRefreshService).refreshOne(5L);
    }

    @Test
    void reprocess_jacksonExceptionOnLogging_stillRedirects() throws Exception {
        var m = new MovieRequest("M", 1, false, 50, "s");
        m.setId(5L);
        when(movieRequestRepository.findById(5L)).thenReturn(Optional.of(m));
        when(ombiClient.reprocessMovieRequest(50)).thenReturn(new OmbiReprocessResponse());
        when(objectMapper.writeValueAsString(any())).thenThrow(mock(JacksonException.class));

        assertEquals("redirect:/movies", controller.reprocess(5L));
        verify(movieRefreshService).refreshOne(5L);
    }

    @Test
    void reprocess_notFound_throwsRequestNotFoundException() {
        when(movieRequestRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(RequestNotFoundException.class, () -> controller.reprocess(99L));
    }

    // ---- deleteDownloadAndSearch ----

    @Test
    void deleteDownloadAndSearch_notFound_throwsRequestNotFoundException() {
        when(movieRequestRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(RequestNotFoundException.class, () -> controller.deleteDownloadAndSearch(99L));
    }

    @Test
    void deleteDownloadAndSearch_deletesOnlyThisMoviesQueueItemsThenSearches() {
        var movie = new MovieRequest("The Movie", 1, false, 100, "Common.ProcessingRequest");
        movie.setId(5L);
        movie.setRadarrRequestId(42);

        when(movieRequestRepository.findById(5L)).thenReturn(Optional.of(movie));
        // One queue item for this movie (id 777), one for a different movie (id 778) that must be left alone.
        when(radarrClient.getQueue()).thenReturn(queueOf(queueRecord(777, 42), queueRecord(778, 99)));
        when(radarrClient.searchMovies(List.of(42))).thenReturn(new RadarrCommand());

        controller.deleteDownloadAndSearch(5L);

        verify(radarrClient).getQueue();
        verify(radarrClient).deleteQueueItem(777);
        verify(radarrClient).searchMovies(List.of(42));
        verifyNoMoreInteractions(radarrClient);
        verify(movieRequestRepository).save(movie);
    }

    @Test
    void deleteDownloadAndSearch_withoutRadarrRequestId_doesNothing() {
        var movie = new MovieRequest("No Radarr", 1, false, 100, "Common.ProcessingRequest");
        movie.setId(6L);

        when(movieRequestRepository.findById(6L)).thenReturn(Optional.of(movie));

        controller.deleteDownloadAndSearch(6L);

        verifyNoMoreInteractions(radarrClient);
    }

    @Test
    void deleteDownloadAndSearch_queueFetchThrows_skipsDeleteAndStillSearches() {
        var movie = new MovieRequest("M", 1, false, 100, "s");
        movie.setId(7L);
        movie.setRadarrRequestId(42);

        when(movieRequestRepository.findById(7L)).thenReturn(Optional.of(movie));
        when(radarrClient.getQueue()).thenThrow(new RuntimeException("queue down"));
        when(radarrClient.searchMovies(List.of(42))).thenReturn(new RadarrCommand());

        controller.deleteDownloadAndSearch(7L);

        verify(radarrClient).getQueue();
        verify(radarrClient).searchMovies(List.of(42));
        verify(radarrClient, never()).deleteQueueItem(any());
    }

    // ---- scanWithFfprobe ----

    @Test
    void scanWithFfprobe_enqueuesJob() {
        controller.scanWithFfprobe(8L);

        verify(jobRequestScheduler).enqueue(new FfprobeScanJobRequest(FfprobeScanJobRequest.MediaType.MOVIE, 8L));
        verify(ffprobeScanService, never()).scanMovie(any());
    }

    @Test
    void getLatestFfprobeScan_delegatesToService() {
        var scan = new FfprobeScan(8L, "MOVIE");
        when(ffprobeScanService.getLatestMovieScan(8L)).thenReturn(Optional.of(scan));

        assertEquals(Optional.of(scan), controller.getLatestFfprobeScan(8L));
    }

    // ---- helpers ----

    /** A movie that matches the searchMissing filter. */
    private static MovieRequest processingMovieWithFile(Integer radarrId) {
        var m = new MovieRequest("M", 1, false, 1, "Common.ProcessingRequest");
        m.setRadarrHasFile(false);
        m.setRadarrRequestId(radarrId);
        return m;
    }

    private static RadarrQueueRecord queueRecord(Integer id, Integer movieId) {
        var record = new RadarrQueueRecord();
        record.setId(id);
        record.setMovieId(movieId);
        return record;
    }

    private static RadarrQueue queueOf(RadarrQueueRecord... records) {
        var queue = new RadarrQueue();
        queue.setRecords(List.of(records));
        return queue;
    }
}
