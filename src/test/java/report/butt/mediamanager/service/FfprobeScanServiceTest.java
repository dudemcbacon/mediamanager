package report.butt.mediamanager.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import net.bramp.ffmpeg.probe.FFmpegFormat;
import net.bramp.ffmpeg.probe.FFmpegProbeResult;
import net.bramp.ffmpeg.probe.FFmpegStream;
import net.bramp.ffmpeg.shared.CodecType;
import org.apache.commons.lang3.math.Fraction;
import org.junit.jupiter.api.Test;
import report.butt.mediamanager.exceptions.RequestNotFoundException;
import report.butt.mediamanager.model.FfprobeScan;
import report.butt.mediamanager.model.FfprobeStream;
import report.butt.mediamanager.model.MovieRequest;
import report.butt.mediamanager.model.TvEpisodeRequest;
import report.butt.mediamanager.repository.FfprobeScanRepository;
import report.butt.mediamanager.repository.MovieRequestRepository;
import report.butt.mediamanager.repository.TvEpisodeRequestRepository;

class FfprobeScanServiceTest {

    private final FfprobeScanRepository ffprobeScanRepository = mock(FfprobeScanRepository.class);
    private final MovieRequestRepository movieRequestRepository = mock(MovieRequestRepository.class);
    private final TvEpisodeRequestRepository tvEpisodeRequestRepository = mock(TvEpisodeRequestRepository.class);

    private final FfprobeScanService service = new FfprobeScanService(
            ffprobeScanRepository, movieRequestRepository, tvEpisodeRequestRepository, "", "ffprobe");

    // --- toScan mapping ---

    @Test
    void toScanMapsFormatAndStreams() {
        var result = new FFmpegProbeResult();
        result.format = format();
        result.streams = List.of(videoStream(), audioStream());

        FfprobeScan scan = FfprobeScanService.toScan(result, 42L, "MOVIE");

        assertEquals(42L, scan.getRequestId());
        assertEquals("MOVIE", scan.getRequestType());
        assertEquals("/movies/the-movie/the-movie.mkv", scan.getFilename());
        assertEquals("matroska,webm", scan.getFormatName());
        assertEquals(2, scan.getNbStreams());
        assertEquals(7_516_192_768L, scan.getSize());
        assertEquals(8_000_000L, scan.getBitRate());
        assertEquals(100, scan.getProbeScore());
        assertEquals(2, scan.getStreams().size());

        FfprobeStream video = scan.getStreams().get(0);
        assertEquals(0, video.getStreamIndex());
        assertEquals("h264", video.getCodecName());
        assertEquals("video", video.getCodecType()); // enum lowercased
        assertEquals(1920, video.getWidth());
        assertEquals(1080, video.getHeight());
        assertEquals("24/1", video.getRFrameRate()); // Fraction -> num/denom
        assertSame(scan, video.getFfprobeScan()); // back-reference wired by addStream

        FfprobeStream audio = scan.getStreams().get(1);
        assertEquals("audio", audio.getCodecType());
        assertEquals(48_000, audio.getSampleRate());
        assertEquals(6, audio.getChannels());
        assertEquals("5.1", audio.getChannelLayout());
    }

    @Test
    void toScanWithNoStreamsProducesEmptyList() {
        var result = new FFmpegProbeResult();
        result.format = format();

        FfprobeScan scan = FfprobeScanService.toScan(result, 1L, "MOVIE");

        assertEquals(0, scan.getStreams().size());
        assertEquals("matroska,webm", scan.getFormatName());
    }

    @Test
    void toScanWithNullCodecTypeLeavesItNull() {
        var stream = new FFmpegStream();
        stream.codec_type = null;
        var result = new FFmpegProbeResult();
        result.streams = List.of(stream);

        FfprobeScan scan = FfprobeScanService.toScan(result, 1L, "MOVIE");

        assertNull(scan.getStreams().get(0).getCodecType());
    }

    // --- scanMovie guards ---

    @Test
    void scanMovieThrowsWhenRequestMissing() {
        when(movieRequestRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(RequestNotFoundException.class, () -> service.scanMovie(99L));
        verify(ffprobeScanRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void scanMovieThrowsWhenNoFilePath() {
        var movie = new MovieRequest("Movie", 1, false, 1, "Common.ProcessingRequest");
        movie.setId(5L);
        // radarrMovieFilePath left null
        when(movieRequestRepository.findById(5L)).thenReturn(Optional.of(movie));

        assertThrows(IllegalStateException.class, () -> service.scanMovie(5L));
        verify(ffprobeScanRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void getLatestMovieScanDelegatesToRepository() {
        var scan = new FfprobeScan(5L, "MOVIE");
        when(ffprobeScanRepository.findFirstByRequestIdAndRequestTypeOrderByCreatedAtDesc(5L, "MOVIE"))
                .thenReturn(Optional.of(scan));

        assertEquals(Optional.of(scan), service.getLatestMovieScan(5L));
    }

    // --- scanEpisode guards ---

    @Test
    void scanEpisodeThrowsWhenEpisodeMissing() {
        when(tvEpisodeRequestRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(RequestNotFoundException.class, () -> service.scanEpisode(99L));
        verify(ffprobeScanRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void scanEpisodeThrowsWhenNoSonarrPath() {
        var episode = new TvEpisodeRequest(null, 1, 1); // sonarrPath left null
        episode.setId(7L);
        when(tvEpisodeRequestRepository.findById(7L)).thenReturn(Optional.of(episode));

        assertThrows(IllegalStateException.class, () -> service.scanEpisode(7L));
        verify(ffprobeScanRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void getLatestEpisodeScanDelegatesToRepository() {
        var scan = new FfprobeScan(7L, "EPISODE");
        when(ffprobeScanRepository.findFirstByRequestIdAndRequestTypeOrderByCreatedAtDesc(7L, "EPISODE"))
                .thenReturn(Optional.of(scan));

        assertEquals(Optional.of(scan), service.getLatestEpisodeScan(7L));
    }

    // --- fixtures ---

    private static FFmpegFormat format() {
        var f = new FFmpegFormat();
        f.filename = "/movies/the-movie/the-movie.mkv";
        f.nb_streams = 2;
        f.nb_programs = 0;
        f.format_name = "matroska,webm";
        f.format_long_name = "Matroska / WebM";
        f.start_time = 0.0;
        f.duration = 7200.0;
        f.size = 7_516_192_768L;
        f.bit_rate = 8_000_000L;
        f.probe_score = 100;
        return f;
    }

    private static FFmpegStream videoStream() {
        var s = new FFmpegStream();
        s.index = 0;
        s.codec_name = "h264";
        s.codec_long_name = "H.264 / AVC";
        s.codec_type = CodecType.VIDEO;
        s.width = 1920;
        s.height = 1080;
        s.pix_fmt = "yuv420p";
        s.bit_rate = 7_000_000L;
        s.duration = 7200.0;
        s.nb_frames = 172_800L;
        s.r_frame_rate = Fraction.getFraction(24, 1);
        s.avg_frame_rate = Fraction.getFraction(24, 1);
        return s;
    }

    private static FFmpegStream audioStream() {
        var s = new FFmpegStream();
        s.index = 1;
        s.codec_name = "aac";
        s.codec_type = CodecType.AUDIO;
        s.sample_rate = 48_000;
        s.channels = 6;
        s.channel_layout = "5.1";
        s.bit_rate = 640_000L;
        return s;
    }
}
