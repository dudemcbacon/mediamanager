package report.butt.mediamanager.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import report.butt.mediamanager.exceptions.RequestNotFoundException;
import report.butt.mediamanager.model.MovieRequest;
import report.butt.mediamanager.repository.MovieRequestRepository;

class MovieRequestControllerTest {

    private final MovieRequestRepository repository = mock(MovieRequestRepository.class);
    private final MovieRequestController controller = new MovieRequestController(repository);

    // ---- all ----

    @Test
    void all_returnsAllFromRepository() {
        MovieRequest m = new MovieRequest("A", 1, false, 1, "s");
        when(repository.findAll()).thenReturn(List.of(m));

        List<MovieRequest> result = controller.all();

        assertEquals(List.of(m), result);
    }

    // ---- newMovieRequest ----

    @Test
    void newMovieRequest_savesAndReturns() {
        MovieRequest m = new MovieRequest("A", 1, false, 1, "s");
        when(repository.save(m)).thenReturn(m);

        MovieRequest result = controller.newMovieRequest(m);

        assertEquals(m, result);
        verify(repository).save(m);
    }

    // ---- one ----

    @Test
    void one_found_returnsRequest() {
        MovieRequest m = new MovieRequest("A", 1, false, 1, "s");
        when(repository.findById(1L)).thenReturn(Optional.of(m));

        assertEquals(m, controller.one(1L));
    }

    @Test
    void one_notFound_throwsRequestNotFoundException() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(RequestNotFoundException.class, () -> controller.one(99L));
    }

    // ---- replaceMovieRequest ----

    @Test
    void replaceMovieRequest_found_updatesFieldsAndSaves() {
        MovieRequest existing = new MovieRequest("Old", 1, false, 10, "oldStatus");
        MovieRequest incoming = new MovieRequest("New", 2, true, 20, "newStatus");
        when(repository.findById(1L)).thenReturn(Optional.of(existing));
        when(repository.save(existing)).thenReturn(existing);

        MovieRequest result = controller.replaceMovieRequest(incoming, 1L);

        assertEquals("New", result.getTitle());
        assertEquals(2, result.getTmdbid());
        verify(repository).save(existing);
    }

    @Test
    void replaceMovieRequest_notFound_savesIncomingAsNew() {
        MovieRequest incoming = new MovieRequest("New", 2, true, 20, "newStatus");
        when(repository.findById(1L)).thenReturn(Optional.empty());
        when(repository.save(incoming)).thenReturn(incoming);

        MovieRequest result = controller.replaceMovieRequest(incoming, 1L);

        assertEquals(incoming, result);
        verify(repository).save(incoming);
    }

    // ---- deleteMovieRequest ----

    @Test
    void deleteMovieRequest_callsDeleteById() {
        controller.deleteMovieRequest(5L);

        verify(repository).deleteById(5L);
    }
}
