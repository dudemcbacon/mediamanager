package report.butt.mediamanager.service;

import com.newrelic.api.agent.Trace;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Locale;
import java.util.Optional;
import net.bramp.ffmpeg.FFprobe;
import net.bramp.ffmpeg.probe.FFmpegFormat;
import net.bramp.ffmpeg.probe.FFmpegProbeResult;
import net.bramp.ffmpeg.probe.FFmpegStream;
import org.apache.commons.lang3.math.Fraction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import report.butt.mediamanager.exceptions.RequestNotFoundException;
import report.butt.mediamanager.model.FfprobeScan;
import report.butt.mediamanager.model.FfprobeStream;
import report.butt.mediamanager.model.MovieRequest;
import report.butt.mediamanager.model.TvEpisodeRequest;
import report.butt.mediamanager.repository.FfprobeScanRepository;
import report.butt.mediamanager.repository.MovieRequestRepository;
import report.butt.mediamanager.repository.TvEpisodeRequestRepository;

/**
 * Runs {@code ffprobe} (via ffmpeg-cli-wrapper) against a request's local media file and stores the
 * container format + per-stream data as a {@link FfprobeScan}. Equivalent to:
 *
 * <pre>ffprobe -v quiet -print_format json -show_format -show_streams &lt;localFilePath&gt;</pre>
 *
 * The local path is the Radarr-reported file path with {@code mediamanager.local-file-system-prefix}
 * prepended (the same resolution {@code MovieRefreshService} uses for its local-file check).
 */
@Service
public class FfprobeScanService {

    private static final Logger log = LoggerFactory.getLogger(FfprobeScanService.class);

    /** Soft-reference discriminators stored on scans (the id refers to a MovieRequest / TvEpisodeRequest). */
    private static final String MOVIE_REQUEST_TYPE = "MOVIE";

    private static final String EPISODE_REQUEST_TYPE = "EPISODE";

    private final FfprobeScanRepository ffprobeScanRepository;
    private final MovieRequestRepository movieRequestRepository;
    private final TvEpisodeRequestRepository tvEpisodeRequestRepository;
    private final String localFileSystemPrefix;
    private final String ffprobePath;

    public FfprobeScanService(
            FfprobeScanRepository ffprobeScanRepository,
            MovieRequestRepository movieRequestRepository,
            TvEpisodeRequestRepository tvEpisodeRequestRepository,
            @Value("${mediamanager.local-file-system-prefix:}") String localFileSystemPrefix,
            @Value("${ffmpeg.ffprobe-path:ffprobe}") String ffprobePath) {
        this.ffprobeScanRepository = ffprobeScanRepository;
        this.movieRequestRepository = movieRequestRepository;
        this.tvEpisodeRequestRepository = tvEpisodeRequestRepository;
        this.localFileSystemPrefix = localFileSystemPrefix;
        this.ffprobePath = ffprobePath;
    }

    /**
     * Probes the movie request's local file and persists the result. Throws {@link RequestNotFoundException} if the
     * request is missing, {@link IllegalStateException} if it has no file path or ffprobe reports an error, and
     * {@link UncheckedIOException} if ffprobe can't be run.
     */
    @Trace
    public FfprobeScan scanMovie(Long movieRequestId) {
        MovieRequest movieRequest = movieRequestRepository
                .findById(movieRequestId)
                .orElseThrow(() -> new RequestNotFoundException(movieRequestId));

        String radarrMovieFilePath = movieRequest.getRadarrMovieFilePath();
        if (radarrMovieFilePath == null || radarrMovieFilePath.isBlank()) {
            throw new IllegalStateException("MovieRequest " + movieRequestId + " has no radarrMovieFilePath to scan");
        }
        return scan(radarrMovieFilePath, movieRequestId, MOVIE_REQUEST_TYPE);
    }

    /**
     * Probes the TV episode's local file (its Sonarr path) and persists the result. Same failure modes as
     * {@link #scanMovie}.
     */
    @Trace
    public FfprobeScan scanEpisode(Long episodeId) {
        TvEpisodeRequest episode = tvEpisodeRequestRepository
                .findById(episodeId)
                .orElseThrow(() -> new RequestNotFoundException(episodeId));

        String sonarrPath = episode.getSonarrPath();
        if (sonarrPath == null || sonarrPath.isBlank()) {
            throw new IllegalStateException("TvEpisodeRequest " + episodeId + " has no sonarrPath to scan");
        }
        return scan(sonarrPath, episodeId, EPISODE_REQUEST_TYPE);
    }

    /** Resolves the local path, runs ffprobe, maps the result, and saves it. */
    private FfprobeScan scan(String reportedPath, Long requestId, String requestType) {
        String localPath = localFileSystemPrefix + reportedPath;
        log.info("Running ffprobe for {} {} against {}", requestType, requestId, localPath);
        FFmpegProbeResult result = probe(localPath);
        if (result.hasError()) {
            throw new IllegalStateException("ffprobe failed for " + localPath + ": " + result.getError());
        }
        FfprobeScan saved = ffprobeScanRepository.save(toScan(result, requestId, requestType));
        log.info("Saved {} ({} streams) for {} {}", saved, saved.getStreams().size(), requestType, requestId);
        return saved;
    }

    /** The most recent ffprobe scan for a movie request (streams eagerly loaded), or empty if never scanned. */
    public Optional<FfprobeScan> getLatestMovieScan(Long movieRequestId) {
        return ffprobeScanRepository.findFirstByRequestIdAndRequestTypeOrderByCreatedAtDesc(
                movieRequestId, MOVIE_REQUEST_TYPE);
    }

    /** The most recent ffprobe scan for a TV episode (streams eagerly loaded), or empty if never scanned. */
    public Optional<FfprobeScan> getLatestEpisodeScan(Long episodeId) {
        return ffprobeScanRepository.findFirstByRequestIdAndRequestTypeOrderByCreatedAtDesc(
                episodeId, EPISODE_REQUEST_TYPE);
    }

    /** Runs the equivalent of {@code ffprobe -v quiet -print_format json -show_format -show_streams <path>}. */
    private FFmpegProbeResult probe(String localPath) {
        try {
            var ffprobe = new FFprobe(ffprobePath);
            // The default builder already adds -v quiet -print_format json -show_format -show_streams;
            // chapters are disabled to match the requested command.
            return ffprobe.probe(ffprobe.builder().setInput(localPath).setShowChapters(false));
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to run ffprobe (" + ffprobePath + ") on " + localPath, e);
        }
    }

    /** Maps an ffprobe result's {@code format} and {@code streams} onto a new {@link FfprobeScan}. */
    static FfprobeScan toScan(FFmpegProbeResult result, Long requestId, String requestType) {
        FfprobeScan scan = new FfprobeScan(requestId, requestType);
        FFmpegFormat format = result.getFormat();
        if (format != null) {
            scan.setFilename(format.getFilename());
            scan.setNbStreams(format.getNbStreams());
            scan.setNbPrograms(format.getNbPrograms());
            scan.setFormatName(format.getFormatName());
            scan.setFormatLongName(format.getFormatLongName());
            scan.setStartTime(format.getStartTime());
            scan.setDuration(format.getDuration());
            scan.setSize(format.getSize());
            scan.setBitRate(format.getBitRate());
            scan.setProbeScore(format.getProbeScore());
        }
        for (FFmpegStream stream : result.getStreams()) {
            scan.addStream(toStream(stream));
        }
        return scan;
    }

    private static FfprobeStream toStream(FFmpegStream stream) {
        var s = new FfprobeStream();
        s.setStreamIndex(stream.getIndex());
        s.setCodecName(stream.getCodecName());
        s.setCodecLongName(stream.getCodecLongName());
        s.setCodecType(
                stream.getCodecType() == null
                        ? null
                        : stream.getCodecType().name().toLowerCase(Locale.ROOT));
        s.setWidth(stream.getWidth());
        s.setHeight(stream.getHeight());
        s.setPixFmt(stream.getPixFmt());
        s.setSampleRate(stream.getSampleRate());
        s.setChannels(stream.getChannels());
        s.setChannelLayout(stream.getChannelLayout());
        s.setBitRate(stream.getBitRate());
        s.setDuration(stream.getDuration());
        s.setNbFrames(stream.getNbFrames());
        s.setRFrameRate(fraction(stream.getRFrameRate()));
        s.setAvgFrameRate(fraction(stream.getAvgFrameRate()));
        return s;
    }

    /** ffprobe frame rates are {@code num/denom}; {@link Fraction#toString()} preserves that form. */
    private static String fraction(Fraction value) {
        return value == null ? null : value.toString();
    }
}
