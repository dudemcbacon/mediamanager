package report.butt.mediamanager.repository;

import java.util.List;
import java.util.Optional;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import report.butt.mediamanager.model.FfprobeScan;

@NullMarked
public interface FfprobeScanRepository extends JpaRepository<FfprobeScan, Long> {

    /** Scans for a given request (matched by the soft reference), newest first. */
    List<FfprobeScan> findByRequestIdAndRequestTypeOrderByCreatedAtDesc(Long requestId, String requestType);

    /**
     * The most recent scan for a request with its {@code streams} eagerly fetched, so they can be rendered after the
     * persistence session closes (the {@code streams} association is otherwise lazy).
     */
    @EntityGraph(attributePaths = "streams")
    Optional<FfprobeScan> findFirstByRequestIdAndRequestTypeOrderByCreatedAtDesc(Long requestId, String requestType);

    /** Distinct request ids that already have at least one scan of the given type ("MOVIE" or "EPISODE"). */
    @Query("SELECT DISTINCT s.requestId FROM FfprobeScan s WHERE s.requestType = :requestType")
    List<Long> findDistinctRequestIdsByRequestType(@Param("requestType") String requestType);

    /** One request id paired with the codec name of one of its video streams. */
    interface VideoCodecView {
        Long getRequestId();

        @Nullable String getCodecName();
    }

    /**
     * Every scanned video stream for the given request type, newest scan first and lowest stream index first within a
     * scan. Callers keep the first row per request id (see {@code FfprobeScanService#latestMovieVideoCodecs}) to get
     * each request's current primary video codec in one query, rather than a per-request lookup for every grid row.
     */
    @Query("""
            SELECT s.requestId AS requestId, st.codecName AS codecName
            FROM FfprobeScan s JOIN s.streams st
            WHERE s.requestType = :requestType AND st.codecType = 'video'
            ORDER BY s.createdAt DESC, s.id DESC, st.streamIndex ASC
            """)
    List<VideoCodecView> findVideoCodecsByRequestType(@Param("requestType") String requestType);
}
