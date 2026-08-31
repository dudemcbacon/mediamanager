package report.butt.mediamanager.repository;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.jspecify.annotations.NullMarked;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import report.butt.mediamanager.model.TvEpisodeRequest;

@NullMarked
public interface TvEpisodeRequestRepository extends JpaRepository<TvEpisodeRequest, Long> {

    Optional<TvEpisodeRequest> findByTvSeasonRequestIdAndOmbiEpisodeNumber(
            Long tvSeasonRequestId, Integer ombiEpisodeNumber);

    List<TvEpisodeRequest> findByTvSeasonRequestIdIn(Collection<Long> tvSeasonRequestIds);

    /** Our episode rows for Sonarr's own episode ids, for attributing a search issued with Sonarr ids back to them. */
    List<TvEpisodeRequest> findBySonarrEpisodeIdIn(Collection<Integer> sonarrEpisodeIds);

    /**
     * Sum of {@code localFileSize} per show-level Ombi user (the {@link report.butt.mediamanager.model.TvRequest}'s
     * {@code ombiUserName}), skipping episodes whose size is unknown. Each row is {@code [String username, Long
     * bytes]}; a null username represents shows with no recorded requester. Keyed against the parent show to match the
     * leaderboard's per-{@code TvRequest} count attribution.
     */
    @Query("""
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

    /** Ids of every episode in the library, for stats that count episodes without loading the entities. */
    @Query("SELECT e.id FROM TvEpisodeRequest e")
    List<Long> findAllEpisodeIds();

    /**
     * Episode counts grouped by Tdarr transcode verdict, for the stats table. Each row is {@code [String verdict, Long
     * count]}; a null verdict means Tdarr hasn't reported on that episode's file. Aggregated in the database rather
     * than by loading every episode entity, for the same reason {@link #findAllEpisodeIds()} exists.
     */
    @Query("""
            SELECT e.tdarrTranscodeDecisionMaker, COUNT(e)
            FROM TvEpisodeRequest e
            GROUP BY e.tdarrTranscodeDecisionMaker
            """)
    List<Object[]> countEpisodesByTdarrTranscodeDecisionMaker();

    /**
     * Episodes whose Plex path matches a LIKE pattern, used by the transcode-complete webhook to resolve either a full
     * path (a pattern with no wildcards) or a bare filename (a {@code %/name} suffix pattern). {@code !} is the escape
     * character, so the caller must escape any literal {@code !}, {@code %} or {@code _} in the value — see
     * {@code TdarrUpdateService.likePattern}.
     */
    @Query("SELECT e FROM TvEpisodeRequest e WHERE e.plexPath LIKE :pattern ESCAPE '!'")
    List<TvEpisodeRequest> findByPlexPathLike(@Param("pattern") String pattern);

    /**
     * Ids of the episodes a Tdarr sweep should ask about: available, and with a Plex path to search by. Ids rather than
     * entities, so queuing a library-wide sweep doesn't load every episode.
     */
    @Query("""
            SELECT e.id FROM TvEpisodeRequest e
            WHERE e.ombiAvailable = true
              AND e.plexPath IS NOT NULL
              AND e.plexPath <> ''
            """)
    List<Long> findTdarrRefreshableEpisodeIds();

    /**
     * Episodes the automatic re-search should ask Sonarr about: not available, known to Sonarr (so EpisodeSearch has an
     * id to key off), whose parent series has not already been triaged stale, and last searched before
     * {@code threshold} by both Sonarr's reckoning and ours. Null timestamps count as never searched and sort first.
     */
    @Query("""
            SELECT e FROM TvEpisodeRequest e
              JOIN e.tvSeasonRequest s
              JOIN s.tvChildRequest c
              JOIN c.parent tr
            WHERE (e.ombiAvailable IS NULL OR e.ombiAvailable = false)
              AND e.sonarrEpisodeId IS NOT NULL
              AND (tr.stale IS NULL OR tr.stale = false)
              AND (e.sonarrLastSearchTime IS NULL OR e.sonarrLastSearchTime < :threshold)
              AND (e.searchLastAt IS NULL OR e.searchLastAt < :threshold)
            ORDER BY e.searchLastAt ASC NULLS FIRST, e.id ASC
            """)
    List<TvEpisodeRequest> findSearchable(@Param("threshold") Instant threshold, Limit limit);

    /**
     * The unavailable episodes of the given Sonarr series, for attributing a series-level SeriesSearch to the episodes
     * it actually searches. Available episodes are excluded: a series search does re-scan them, but they need no
     * further searching and counting them would inflate their search history.
     */
    @Query("""
            SELECT e FROM TvEpisodeRequest e
              JOIN e.tvSeasonRequest s
              JOIN s.tvChildRequest c
              JOIN c.parent tr
            WHERE tr.sonarrSeriesId IN :sonarrSeriesIds
              AND (e.ombiAvailable IS NULL OR e.ombiAvailable = false)
            """)
    List<TvEpisodeRequest> findUnavailableBySonarrSeriesIdIn(
            @Param("sonarrSeriesIds") Collection<Integer> sonarrSeriesIds);

    /** The unavailable episodes of the given seasons of one Sonarr series, for attributing a SeasonSearch. */
    @Query("""
            SELECT e FROM TvEpisodeRequest e
              JOIN e.tvSeasonRequest s
              JOIN s.tvChildRequest c
              JOIN c.parent tr
            WHERE tr.sonarrSeriesId = :sonarrSeriesId
              AND s.ombiSeasonNumber IN :seasonNumbers
              AND (e.ombiAvailable IS NULL OR e.ombiAvailable = false)
            """)
    List<TvEpisodeRequest> findUnavailableBySonarrSeriesIdAndSeasonNumbers(
            @Param("sonarrSeriesId") Integer sonarrSeriesId,
            @Param("seasonNumbers") Collection<Integer> seasonNumbers);

    /** The parent {@code TvRequest} id for each of the given episodes, as {@code [Long episodeId, Long tvRequestId]}. */
    @Query("""
            SELECT e.id, tr.id FROM TvEpisodeRequest e
              JOIN e.tvSeasonRequest s
              JOIN s.tvChildRequest c
              JOIN c.parent tr
            WHERE e.id IN :episodeIds
            """)
    List<Object[]> findParentTvRequestIds(@Param("episodeIds") Collection<Long> episodeIds);
}
