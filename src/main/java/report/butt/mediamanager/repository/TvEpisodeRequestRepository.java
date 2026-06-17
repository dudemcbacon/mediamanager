package report.butt.mediamanager.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.jspecify.annotations.NullMarked;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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

    /**
     * Ids of every episode under the given show that has a local file path, i.e. can be ffprobe-scanned. Used to fan
     * out one scan job per episode for a whole series. Joins TvEpisodeRequest → season → child → parent show.
     */
    @Query("""
            SELECT e.id
            FROM TvEpisodeRequest e
              JOIN e.tvSeasonRequest s
              JOIN s.tvChildRequest c
              JOIN c.parent tr
            WHERE tr.id = :tvRequestId AND e.sonarrPath IS NOT NULL AND e.sonarrPath <> ''
            """)
    List<Long> findScannableEpisodeIdsByTvRequestId(@Param("tvRequestId") Long tvRequestId);

    /** Ids of every episode in the library that has a local file path (so it can be ffprobe-scanned). */
    @Query("SELECT e.id FROM TvEpisodeRequest e WHERE e.sonarrPath IS NOT NULL AND e.sonarrPath <> ''")
    List<Long> findAllScannableEpisodeIds();
}
