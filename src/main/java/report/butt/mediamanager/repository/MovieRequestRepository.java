package report.butt.mediamanager.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.jspecify.annotations.NullMarked;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import report.butt.mediamanager.model.MovieRequest;

@NullMarked
public interface MovieRequestRepository extends JpaRepository<MovieRequest, Long> {

    Optional<MovieRequest> findByOmbiRequestId(Integer ombiRequestId);

    List<MovieRequest> findByOmbiRequestIdIn(Collection<Integer> ombiRequestIds);

    Optional<MovieRequest> findByRadarrRequestId(Integer radarrRequestId);

    List<MovieRequest> findByRadarrRequestIdIn(Collection<Integer> radarrRequestIds);

    /** Ids of every movie request that has a local file path (so it can be ffprobe-scanned). */
    @Query("SELECT m.id FROM MovieRequest m WHERE m.radarrMovieFilePath IS NOT NULL AND m.radarrMovieFilePath <> ''")
    List<Long> findScannableMovieRequestIds();
}
