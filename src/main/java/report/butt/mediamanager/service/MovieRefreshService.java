package report.butt.mediamanager.service;

import com.google.errorprone.annotations.Var;
import com.newrelic.api.agent.Trace;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.jobrunr.scheduling.JobRequestScheduler;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import report.butt.mediamanager.client.MetadataResult;
import report.butt.mediamanager.client.OmbiClient;
import report.butt.mediamanager.client.PlexClient;
import report.butt.mediamanager.client.RadarrClient;
import report.butt.mediamanager.exceptions.RequestNotFoundException;
import report.butt.mediamanager.job.FfprobeScanJobRequest;
import report.butt.mediamanager.model.MovieRequest;
import report.butt.mediamanager.model.ombi.OmbiMovieRequest;
import report.butt.mediamanager.model.plex.PlexMetadata;
import report.butt.mediamanager.model.plex.PlexMetadataSupport;
import report.butt.mediamanager.model.radarr.Movie;
import report.butt.mediamanager.repository.MovieRequestRepository;
import report.butt.mediamanager.util.DateTimeUtils;
import report.butt.mediamanager.util.LocalFileInspector;

@Service
@NullMarked
public class MovieRefreshService {

    private static final Logger log = LoggerFactory.getLogger(MovieRefreshService.class);

    private final MovieRequestRepository repository;
    private final OmbiClient ombiClient;
    private final RadarrClient radarrClient;
    private final PlexClient plexClient;
    private final PlexCacheService plexCacheService;

    /** Prepended to Radarr file paths before the local-filesystem existence/size check. Empty = check as-is. */
    private final String localFileSystemPrefix;

    private final JobRequestScheduler jobRequestScheduler;
    private final FfprobeScanService ffprobeScanService;

    public MovieRefreshService(
            MovieRequestRepository repository,
            OmbiClient ombiClient,
            RadarrClient radarrClient,
            PlexClient plexClient,
            PlexCacheService plexCacheService,
            @Value("${mediamanager.local-file-system-prefix:}") String localFileSystemPrefix,
            JobRequestScheduler jobRequestScheduler,
            FfprobeScanService ffprobeScanService) {
        this.repository = repository;
        this.ombiClient = ombiClient;
        this.radarrClient = radarrClient;
        this.plexClient = plexClient;
        this.plexCacheService = plexCacheService;
        this.localFileSystemPrefix = localFileSystemPrefix;
        this.jobRequestScheduler = jobRequestScheduler;
        this.ffprobeScanService = ffprobeScanService;
    }

    @Trace
    public void refreshAll() {
        List<OmbiMovieRequest> ombiMovies = Objects.requireNonNullElse(ombiClient.getMovies(), List.of());
        List<Movie> radarrMovies = Objects.requireNonNullElse(radarrClient.getMovies(), List.of());
        Map<Integer, Movie> radarrByTmdb = radarrMovies.stream()
                .filter(m -> m.getTmdbId() != null)
                .collect(Collectors.toMap(Movie::getTmdbId, Function.identity(), (a, b) -> a));
        // Re-cache quality profiles so renamed/added Radarr profiles are reflected without an app restart.
        radarrClient.cacheQualityProfiles();
        Map<Integer, String> qualityProfilesById = radarrClient.getQualityProfilesById();
        Map<Integer, PlexMetadata> plexByTmdb = plexClient.getAllMoviesIndexedByTmdb();
        Set<String> validCacheKeys = new HashSet<>();

        Set<Integer> ombiRequestIds = ombiMovies.stream()
                .map(OmbiMovieRequest::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Integer, MovieRequest> existingByOmbiId = repository.findByOmbiRequestIdIn(ombiRequestIds).stream()
                .collect(Collectors.toMap(MovieRequest::getOmbiRequestId, Function.identity(), (a, b) -> a));

        List<MovieRequest> toSave = new ArrayList<>();
        @Var int unchanged = 0;
        for (OmbiMovieRequest ombiMovie : ombiMovies) {
            MovieRequest existing = existingByOmbiId.get(ombiMovie.getId());
            MovieRequest movieRequest = existing != null
                    ? existing
                    : new MovieRequest(
                            ombiMovie.getTitle(),
                            ombiMovie.getTheMovieDbId(),
                            ombiMovie.getAvailable(),
                            ombiMovie.getId(),
                            ombiMovie.getRequestStatus());

            Movie radarrMovie =
                    ombiMovie.getTheMovieDbId() == null ? null : radarrByTmdb.get(ombiMovie.getTheMovieDbId());

            Integer beforeHash = existing == null ? null : movieRequest.hashCode();
            applyUpdates(movieRequest, ombiMovie, radarrMovie, plexByTmdb, qualityProfilesById);

            if (radarrMovie != null && radarrMovie.getTmdbId() != null) {
                validCacheKeys.add(PlexClient.movieCacheKey(radarrMovie.getTmdbId()));
            }

            if (beforeHash == null || beforeHash != movieRequest.hashCode()) {
                toSave.add(movieRequest);
                log.info("Refreshed {}", movieRequest);
            } else {
                unchanged++;
            }
        }

        if (!toSave.isEmpty()) {
            repository.saveAll(toSave);
        }
        log.info("Refresh complete: {} saved, {} unchanged", toSave.size(), unchanged);

        plexCacheService.cleanExcept("movie-", validCacheKeys);

        queueMissingMovieScans();
    }

    /** Queues an ffprobe scan for the given movie request. */
    private void queueMovieScan(Long movieRequestId) {
        jobRequestScheduler.enqueue(new FfprobeScanJobRequest(FfprobeScanJobRequest.MediaType.MOVIE, movieRequestId));
    }

    /**
     * Bulk-refresh follow-up: queues a scan for every movie that has a local file but no ffprobe scan yet, so a full
     * refresh backfills missing scans without re-scanning the whole library each run.
     */
    private void queueMissingMovieScans() {
        var scanned = ffprobeScanService.scannedMovieRequestIds();
        for (Long movieRequestId : repository.findScannableMovieRequestIds()) {
            if (!scanned.contains(movieRequestId)) {
                queueMovieScan(movieRequestId);
            }
        }
    }

    public void refreshOne(Long id) {
        MovieRequest movieRequest = repository.findById(id).orElseThrow(() -> new RequestNotFoundException(id));

        Integer ombiRequestId = movieRequest.getOmbiRequestId();
        OmbiMovieRequest ombiMovie = ombiRequestId == null
                ? null
                : Objects.requireNonNullElse(ombiClient.getMovies(), List.<OmbiMovieRequest>of()).stream()
                        .filter(m -> ombiRequestId.equals(m.getId()))
                        .findFirst()
                        .orElse(null);

        Integer tmdbid = ombiMovie != null ? ombiMovie.getTheMovieDbId() : movieRequest.getTmdbid();
        @Var Movie radarrMovie = null;
        if (tmdbid != null) {
            List<Movie> radarrMovies = Objects.requireNonNullElse(radarrClient.getMoviesByTmdbId(tmdbid), List.of());
            if (!radarrMovies.isEmpty()) {
                radarrMovie = radarrMovies.get(0);
            }
        }

        applyUpdates(movieRequest, ombiMovie, radarrMovie, null, radarrClient.getQualityProfilesById());
        repository.save(movieRequest);
        var filePath = movieRequest.getRadarrMovieFilePath();
        if (filePath != null && !filePath.isBlank()) {
            queueMovieScan(id);
        }
        log.info("Refreshed {} ({})", id, movieRequest.getTitle());
    }

    private void applyUpdates(
            MovieRequest movieRequest,
            @Nullable OmbiMovieRequest ombiMovie,
            @Nullable Movie radarrMovie,
            @Nullable Map<Integer, PlexMetadata> plexByTmdb,
            @Nullable Map<Integer, String> qualityProfilesById) {
        if (ombiMovie != null) {
            String ombiUserName = ombiMovie.getRequestedUser() == null
                    ? null
                    : ombiMovie.getRequestedUser().getUserName();
            movieRequest.setTitle(ombiMovie.getTitle());
            movieRequest.setTmdbid(ombiMovie.getTheMovieDbId());
            movieRequest.setOmbiAvailable(ombiMovie.getAvailable());
            movieRequest.setOmbiRequestStatus(ombiMovie.getRequestStatus());
            movieRequest.setOmbiUserName(ombiUserName);
            movieRequest.setOmbiRequestedDate(DateTimeUtils.parseInstant(ombiMovie.getRequestedDate(), "Ombi"));
        }

        if (radarrMovie != null) {
            movieRequest.setRadarrRequestId(radarrMovie.getId());
            movieRequest.setRadarrHasFile(radarrMovie.getHasFile());
            movieRequest.setRadarrMonitored(radarrMovie.getMonitored());
            movieRequest.setRadarrIsAvailable(radarrMovie.getIsAvailable());
            movieRequest.setRadarrPath(radarrMovie.getPath());
            movieRequest.setRadarrRootFolderPath(radarrMovie.getRootFolderPath());
            movieRequest.setRadarrQualityProfile(
                    qualityProfilesById == null ? null : qualityProfilesById.get(radarrMovie.getQualityProfileId()));
            movieRequest.setRadarrOriginalLanguage(
                    radarrMovie.getOriginalLanguage() == null
                            ? null
                            : radarrMovie.getOriginalLanguage().getName());
            movieRequest.setRadarrLastSearchTime(DateTimeUtils.parseInstant(radarrMovie.getLastSearchTime(), "Radarr"));
            movieRequest.setRadarrMovieFilePath(
                    radarrMovie.getMovieFile() == null
                            ? null
                            : radarrMovie.getMovieFile().getPath());
            applyLocalFileStatus(movieRequest);
            applyPlexUpdates(movieRequest, radarrMovie, plexByTmdb);
        }
    }

    /**
     * Checks whether the Radarr-reported movie file exists on the local filesystem (with {@link #localFileSystemPrefix}
     * prepended) and records availability plus size in bytes. A missing path, a non-existent file, or any I/O error
     * leaves the request marked unavailable with no size.
     */
    private void applyLocalFileStatus(MovieRequest movieRequest) {
        LocalFileInspector.Result result =
                LocalFileInspector.inspect(localFileSystemPrefix, movieRequest.getRadarrMovieFilePath());
        movieRequest.setLocalFilePathAvailable(result.available());
        movieRequest.setLocalFileSize(result.sizeBytes());
    }

    private void applyPlexUpdates(
            MovieRequest movieRequest, Movie radarrMovie, @Nullable Map<Integer, PlexMetadata> plexByTmdb) {
        try {
            Integer tmdbId = radarrMovie.getTmdbId();
            if (tmdbId == null) {
                return;
            }
            MetadataResult plexResult;
            if (plexByTmdb != null) {
                PlexMetadata prefetched = plexByTmdb.get(tmdbId);
                String cacheUrl = plexClient.cacheMovieMetadata(tmdbId, prefetched);
                plexResult = new MetadataResult(cacheUrl, prefetched);
            } else {
                plexResult = plexClient.getMovieByTmdbId(tmdbId, radarrMovie.getTitle(), radarrMovie.getYear());
            }
            movieRequest.setPlexMetadataUrl(plexResult.url());
            @Nullable PlexMetadata plexMetadata = plexResult.metadata();
            if (plexMetadata != null) {
                log.info("Plex match found for tmdbId {}: {}", radarrMovie.getTmdbId(), plexMetadata.getTitle());
                movieRequest.setPlexMetadataId(plexMetadata.getRatingKey());
                movieRequest.setPlexAddedAt(plexMetadata.getAddedAt());
                movieRequest.setPlexUpdatedAt(plexMetadata.getUpdatedAt());
                PlexMetadataSupport.parseGuidId(plexMetadata, "tmdb://").ifPresent(movieRequest::setPlexTmdbid);
                PlexMetadataSupport.applyFirstMedia(movieRequest, plexMetadata);
            } else {
                log.info("No Plex match found for tmdbId {}", radarrMovie.getTmdbId());
            }
        } catch (RuntimeException e) {
            log.warn("Plex lookup failed for tmdbId {}", radarrMovie.getTmdbId(), e);
        }
    }
}
