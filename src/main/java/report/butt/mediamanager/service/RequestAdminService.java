package report.butt.mediamanager.service;

import java.time.Instant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import report.butt.mediamanager.exceptions.RequestNotFoundException;
import report.butt.mediamanager.model.Note;
import report.butt.mediamanager.model.Request;
import report.butt.mediamanager.repository.NoteRepository;
import report.butt.mediamanager.repository.ValidationRepository;

/**
 * Type-agnostic request mutations shared by MovieController and TvController. Each method takes the caller's typed
 * repository, so one implementation serves both MovieRequest and TvRequest.
 */
@Service
public class RequestAdminService {

    private final ValidationRepository validationRepository;
    private final NoteRepository noteRepository;

    public RequestAdminService(ValidationRepository validationRepository, NoteRepository noteRepository) {
        this.validationRepository = validationRepository;
        this.noteRepository = noteRepository;
    }

    /** Flags a request stale with the given reason, stamping the current time. */
    public <T extends Request> void markStale(JpaRepository<T, Long> repository, Long id, String reason) {
        T request = repository.findById(id).orElseThrow(() -> new RequestNotFoundException(id));
        request.setStale(true);
        request.setStaleReason(reason);
        request.setMarkedStaleAt(Instant.now());
        repository.save(request);
    }

    /** Adds a note to a request. */
    public <T extends Request> Note addNote(JpaRepository<T, Long> repository, Long id, String notes) {
        T request = repository.findById(id).orElseThrow(() -> new RequestNotFoundException(id));
        return noteRepository.save(new Note(notes, request));
    }

    /** Deletes a request together with its validations and notes. */
    @Transactional
    public <T extends Request> void delete(JpaRepository<T, Long> repository, Long id) {
        T request = repository.findById(id).orElseThrow(() -> new RequestNotFoundException(id));
        validationRepository.deleteByRequest(request);
        noteRepository.deleteByRequest(request);
        repository.delete(request);
    }
}
