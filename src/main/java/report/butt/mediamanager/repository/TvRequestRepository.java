package report.butt.mediamanager.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.jspecify.annotations.NullMarked;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import report.butt.mediamanager.model.TvRequest;

@NullMarked
public interface TvRequestRepository extends JpaRepository<TvRequest, Long> {

    Optional<TvRequest> findByOmbiRequestId(Integer ombiRequestId);

    List<TvRequest> findByOmbiRequestIdIn(Collection<Integer> ombiRequestIds);

    List<TvRequest> findBySonarrSeriesId(Integer sonarrSeriesId);

    /**
     * Shows marked stale by the automatic re-search that have since become available (the same condition as
     * {@code TvRequest.isAvailable()}), so the flag can be cleared. The reason prefix restricts this to our own
     * auto-set stale — a human's triage decision and their free-text reason must never be cleared automatically.
     */
    @Query("""
            SELECT t FROM TvRequest t
            WHERE t.stale = true
              AND t.staleReason LIKE CONCAT(:autoReasonPrefix, '%')
              AND t.sonarrEpisodeFileCount IS NOT NULL
              AND t.sonarrEpisodeCount IS NOT NULL
              AND t.sonarrEpisodeCount > 0
              AND t.sonarrEpisodeFileCount >= t.sonarrEpisodeCount
              AND t.ombiRequestStatus = 'Common.Available'
            """)
    List<TvRequest> findAutoStaleNowAvailable(@Param("autoReasonPrefix") String autoReasonPrefix);
}
