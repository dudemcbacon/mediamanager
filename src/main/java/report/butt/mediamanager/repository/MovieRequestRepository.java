package report.butt.mediamanager.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.jspecify.annotations.NullMarked;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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

    /**
     * Movies whose Plex media path matches a LIKE pattern, used by the transcode-complete webhook to resolve either a
     * full path (a pattern with no wildcards) or a bare filename (a {@code %/name} suffix pattern). {@code !} is the
     * escape character, so the caller must escape any literal {@code !}, {@code %} or {@code _} in the value — see
     * {@code TdarrUpdateService.likePattern}.
     */
    @Query("SELECT m FROM MovieRequest m WHERE m.plexMediaFilename LIKE :pattern ESCAPE '!'")
    List<MovieRequest> findByPlexMediaFilenameLike(@Param("pattern") String pattern);

    /**
     * Ids of the movies a Tdarr sweep should ask about: available (the same condition as
     * {@code MovieRequest.isAvailable()}) and with a Plex path to search by. Ids rather than entities, so queuing a
     * library-wide sweep doesn't load every movie.
     */
    @Query("""
            SELECT m.id FROM MovieRequest m
            WHERE m.radarrHasFile = true
              AND m.ombiRequestStatus = 'Common.Available'
              AND m.plexMediaFilename IS NOT NULL
              AND m.plexMediaFilename <> ''
            """)
    List<Long> findTdarrRefreshableMovieIds();
}
