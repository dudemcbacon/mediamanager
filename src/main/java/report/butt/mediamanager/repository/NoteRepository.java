package report.butt.mediamanager.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import report.butt.mediamanager.model.MovieRequest;
import report.butt.mediamanager.model.Note;

public interface NoteRepository extends JpaRepository<Note, Long> {
  List<Note> findByMovieRequestOrderByCreatedAtDesc(MovieRequest movieRequest);

  @Transactional
  void deleteByMovieRequest(MovieRequest movieRequest);
}
