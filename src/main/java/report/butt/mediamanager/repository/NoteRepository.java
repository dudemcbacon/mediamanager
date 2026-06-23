package report.butt.mediamanager.repository;

import java.util.List;
import org.jspecify.annotations.NullMarked;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;
import report.butt.mediamanager.model.Note;
import report.butt.mediamanager.model.Request;

@NullMarked
public interface NoteRepository extends JpaRepository<Note, Long> {
    List<Note> findByRequestOrderByCreatedAtDesc(Request request);

    @Transactional
    void deleteByRequest(Request request);
}
