package report.butt.mediamanager.repository;

import java.util.List;
import java.util.Optional;
import org.jspecify.annotations.NullMarked;
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
}
