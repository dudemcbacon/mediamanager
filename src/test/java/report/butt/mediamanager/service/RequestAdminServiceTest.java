package report.butt.mediamanager.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.jpa.repository.JpaRepository;
import report.butt.mediamanager.exceptions.RequestNotFoundException;
import report.butt.mediamanager.model.MovieRequest;
import report.butt.mediamanager.model.Note;
import report.butt.mediamanager.repository.NoteRepository;
import report.butt.mediamanager.repository.ValidationRepository;

@SuppressWarnings("unchecked")
class RequestAdminServiceTest {

    private final ValidationRepository validationRepository = mock(ValidationRepository.class);
    private final NoteRepository noteRepository = mock(NoteRepository.class);
    private final RequestAdminService service = new RequestAdminService(validationRepository, noteRepository);

    // typed mock used across tests
    private final JpaRepository<MovieRequest, Long> repo = mock(JpaRepository.class);

    @Test
    void markStaleUpdatesFieldsAndSaves() {
        MovieRequest movie = movie("Test Movie");
        movie.setId(1L);
        when(repo.findById(1L)).thenReturn(Optional.of(movie));

        service.markStale(repo, 1L, "duplicate");

        assertEquals(true, movie.getStale());
        assertEquals("duplicate", movie.getStaleReason());
        assertNotNull(movie.getMarkedStaleAt());
        verify(repo).save(movie);
    }

    @Test
    void markStaleThrowsWhenNotFound() {
        when(repo.findById(99L)).thenReturn(Optional.empty());

        assertThrows(RequestNotFoundException.class, () -> service.markStale(repo, 99L, "reason"));
    }

    @Test
    void addNoteSavesNoteForRequest() {
        MovieRequest movie = movie("Note Movie");
        movie.setId(2L);
        when(repo.findById(2L)).thenReturn(Optional.of(movie));
        Note savedNote = new Note("my note", movie);
        when(noteRepository.save(any(Note.class))).thenReturn(savedNote);

        Note result = service.addNote(repo, 2L, "my note");

        assertEquals(savedNote, result);
        ArgumentCaptor<Note> captor = ArgumentCaptor.forClass(Note.class);
        verify(noteRepository).save(captor.capture());
        assertEquals("my note", captor.getValue().getNotes());
        assertEquals(movie, captor.getValue().getRequest());
    }

    @Test
    void addNoteThrowsWhenNotFound() {
        when(repo.findById(77L)).thenReturn(Optional.empty());

        assertThrows(RequestNotFoundException.class, () -> service.addNote(repo, 77L, "note"));
    }

    @Test
    void deleteRemovesValidationsNotesAndRequest() {
        MovieRequest movie = movie("Delete Me");
        movie.setId(3L);
        when(repo.findById(3L)).thenReturn(Optional.of(movie));

        service.delete(repo, 3L);

        verify(validationRepository).deleteByRequest(movie);
        verify(noteRepository).deleteByRequest(movie);
        verify(repo).delete(movie);
    }

    @Test
    void deleteThrowsWhenNotFound() {
        when(repo.findById(55L)).thenReturn(Optional.empty());

        assertThrows(RequestNotFoundException.class, () -> service.delete(repo, 55L));
    }

    private static MovieRequest movie(String title) {
        return new MovieRequest(title, 1, false, 1, "Common.ProcessingRequest");
    }
}
