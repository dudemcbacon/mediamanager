package report.butt.mediamanager.service;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import org.jobrunr.scheduling.JobRequestScheduler;
import org.jspecify.annotations.NullMarked;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import report.butt.mediamanager.client.TdarrClient;
import report.butt.mediamanager.job.TdarrRefreshJobRequest;
import report.butt.mediamanager.model.MovieRequest;
import report.butt.mediamanager.model.TvEpisodeRequest;
import report.butt.mediamanager.model.tdarr.TdarrFile;
import report.butt.mediamanager.repository.MovieRequestRepository;
import report.butt.mediamanager.repository.TvEpisodeRequestRepository;

/**
 * Reads Tdarr's health-check and transcode verdicts for one movie or episode at a time, looked up by the filename of
 * its Plex path.
 *
 * <p>Deliberately <em>not</em> part of the Ombi/Radarr/Sonarr/Plex refresh: Tdarr has no bulk endpoint, so a
 * library-wide sweep costs one HTTP call per file and takes hours on a large episode library. Every sweep therefore
 * queues one {@code TdarrRefreshJobRequest} per file, so JobRunr's worker-count bounds how hard Tdarr is hit and a slow
 * sweep never occupies a UI thread. Sweeps come from the "Refresh Tdarr" buttons and from the weekly
 * {@code tdarr.sweep-cron} job; the push equivalent is the {@code /api/tdarr/transcode-complete} webhook.
 */
@Service
@NullMarked
public class TdarrRefreshService {

    private static final Logger log = LoggerFactory.getLogger(TdarrRefreshService.class);

    private final MovieRequestRepository movieRequestRepository;
    private final TvEpisodeRequestRepository tvEpisodeRequestRepository;
    private final TdarrClient tdarrClient;
    private final JobRequestScheduler jobRequestScheduler;

    public TdarrRefreshService(
            MovieRequestRepository movieRequestRepository,
            TvEpisodeRequestRepository tvEpisodeRequestRepository,
            TdarrClient tdarrClient,
            JobRequestScheduler jobRequestScheduler) {
        this.movieRequestRepository = movieRequestRepository;
        this.tvEpisodeRequestRepository = tvEpisodeRequestRepository;
        this.tdarrClient = tdarrClient;
        this.jobRequestScheduler = jobRequestScheduler;
    }

    /** Ids of the available movies worth asking Tdarr about, i.e. the ones a sweep enqueues a job for. */
    public List<Long> refreshableMovieIds() {
        return movieRequestRepository.findTdarrRefreshableMovieIds();
    }

    /** Ids of the available episodes worth asking Tdarr about. */
    public List<Long> refreshableEpisodeIds() {
        return tvEpisodeRequestRepository.findTdarrRefreshableEpisodeIds();
    }

    /** Queues one Tdarr refresh job per available movie; returns how many were queued. */
    public int sweepMovies() {
        var ids = refreshableMovieIds();
        for (Long id : ids) {
            jobRequestScheduler.enqueue(new TdarrRefreshJobRequest(TdarrRefreshJobRequest.MediaType.MOVIE, id));
        }
        log.info("Queued Tdarr refresh for {} movie(s)", ids.size());
        return ids.size();
    }

    /** Queues one Tdarr refresh job per available episode; returns how many were queued. */
    public int sweepEpisodes() {
        var ids = refreshableEpisodeIds();
        for (Long id : ids) {
            jobRequestScheduler.enqueue(new TdarrRefreshJobRequest(TdarrRefreshJobRequest.MediaType.EPISODE, id));
        }
        log.info("Queued Tdarr refresh for {} episode(s)", ids.size());
        return ids.size();
    }

    /**
     * Refreshes one movie's Tdarr fields. A movie that has since become unavailable or lost its Plex path is skipped —
     * the id was chosen when the sweep was queued and may be stale by the time the job runs. A miss or an unreachable
     * Tdarr leaves the stored values in place rather than blanking them.
     */
    @Transactional
    public void refreshMovie(Long movieRequestId) {
        MovieRequest movie = movieRequestRepository.findById(movieRequestId).orElse(null);
        if (movie == null) {
            log.warn("Tdarr refresh: movie {} no longer exists", movieRequestId);
            return;
        }
        String plexMediaFilename = movie.getPlexMediaFilename();
        if (!movie.isAvailable() || plexMediaFilename == null || plexMediaFilename.isBlank()) {
            return;
        }
        TdarrFile tdarrFile = tdarrClient.findByPath(plexMediaFilename);
        if (tdarrFile == null) {
            return;
        }
        movie.setTdarrHealthCheck(tdarrFile.getHealthCheck());
        movie.setTdarrTranscodeDecisionMaker(tdarrFile.getTranscodeDecisionMaker());
        movie.setTdarrOldSizeGb(tdarrFile.getOldSize());
        movie.setTdarrNewSizeGb(tdarrFile.getNewSize());
        movie.setTdarrLastUpdated(Instant.now());
        movieRequestRepository.save(movie);
    }

    /** Refreshes one episode's Tdarr fields; same skip and failure behaviour as {@link #refreshMovie}. */
    @Transactional
    public void refreshEpisode(Long episodeId) {
        TvEpisodeRequest episode =
                tvEpisodeRequestRepository.findById(episodeId).orElse(null);
        if (episode == null) {
            log.warn("Tdarr refresh: episode {} no longer exists", episodeId);
            return;
        }
        String plexPath = episode.getPlexPath();
        if (!Objects.equals(episode.getOmbiAvailable(), true) || plexPath == null || plexPath.isBlank()) {
            return;
        }
        TdarrFile tdarrFile = tdarrClient.findByPath(plexPath);
        if (tdarrFile == null) {
            return;
        }
        episode.setTdarrHealthCheck(tdarrFile.getHealthCheck());
        episode.setTdarrTranscodeDecisionMaker(tdarrFile.getTranscodeDecisionMaker());
        episode.setTdarrOldSizeGb(tdarrFile.getOldSize());
        episode.setTdarrNewSizeGb(tdarrFile.getNewSize());
        episode.setTdarrLastUpdated(Instant.now());
        tvEpisodeRequestRepository.save(episode);
    }
}
