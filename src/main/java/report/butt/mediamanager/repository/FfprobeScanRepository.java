package report.butt.mediamanager.repository;

import java.util.List;
import java.util.Optional;
import org.jspecify.annotations.NullMarked;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
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
}
