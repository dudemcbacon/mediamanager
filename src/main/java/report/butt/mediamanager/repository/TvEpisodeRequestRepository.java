package report.butt.mediamanager.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.jspecify.annotations.NullMarked;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import report.butt.mediamanager.model.TvEpisodeRequest;

@NullMarked
public interface TvEpisodeRequestRepository extends JpaRepository<TvEpisodeRequest, Long> {

    Optional<TvEpisodeRequest> findByTvSeasonRequestIdAndOmbiEpisodeNumber(
            Long tvSeasonRequestId, Integer ombiEpisodeNumber);

    List<TvEpisodeRequest> findByTvSeasonRequestIdIn(Collection<Long> tvSeasonRequestIds);

    /**
     * Sum of {@code localFileSize} per show-level Ombi user (the {@link report.butt.mediamanager.model.TvRequest}'s
     * {@code ombiUserName}), skipping episodes whose size is unknown. Each row is {@code [String username, Long bytes]};
     * a null username represents shows with no recorded requester. Keyed against the parent show to match the
     * leaderboard's per-{@code TvRequest} count attribution.
     */
    @Query(
            """
            SELECT tr.ombiUserName, SUM(e.localFileSize)
            FROM TvEpisodeRequest e
              JOIN e.tvSeasonRequest s
              JOIN s.tvChildRequest c
              JOIN c.parent tr
            WHERE e.localFileSize IS NOT NULL
            GROUP BY tr.ombiUserName
            """)
    List<Object[]> sumLocalFileSizeByTvRequestOmbiUserName();
}
