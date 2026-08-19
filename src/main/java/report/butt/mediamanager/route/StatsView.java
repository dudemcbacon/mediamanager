package report.butt.mediamanager.route;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.progressbar.ProgressBar;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import report.butt.mediamanager.model.MovieRequest;
import report.butt.mediamanager.model.Request;
import report.butt.mediamanager.repository.MovieRequestRepository;
import report.butt.mediamanager.repository.TvEpisodeRequestRepository;
import report.butt.mediamanager.repository.TvRequestRepository;
import report.butt.mediamanager.route.RequestViewSupport.Section;
import report.butt.mediamanager.service.FfprobeScanService;

/**
 * Stats dashboard: requester leaderboards and the video-codec breakdown of the movie and TV episode libraries (and room
 * for any future stats). Everything loads in one pass asynchronously on attach so the page renders immediately; results
 * are pushed back via server push (see {@code @Push}).
 */
@Route("stats")
@RolesAllowed("ADMIN")
// Async-UI view: leaderboard data is loaded off the UI thread via CompletableFuture + whenComplete/UI#access (@Push).
// Each such future handles its own success and failure in the callback (log + toast) and is intentionally not
// awaited — blocking on it would freeze the UI thread — so FutureReturnValueIgnored is suppressed class-wide.
@SuppressWarnings("FutureReturnValueIgnored")
@NullMarked
public class StatsView extends VerticalLayout {

    private static final Logger log = LoggerFactory.getLogger(StatsView.class);

    private static final int LEADERBOARD_SIZE = 10;
    private static final String UNKNOWN_USER = "unknown";

    /** Bucket for movies whose video codec isn't known (never scanned, or the scan found no video stream). */
    static final String NO_CODEC = "No codec";

    /** A requester, how many requests they've made, how many are available, and how many bytes they consume. */
    public record RequesterCount(String username, long count, long available, long bytes) {
        public double percentComplete() {
            return count == 0 ? 0.0 : available * 100.0 / count;
        }
    }

    /** One video codec, how many movies use it, and its share of the library. */
    public record CodecCount(String codec, long count, double percentOfLibrary) {}

    // Internal data carrier; its collection components are never mutated after construction.
    @SuppressWarnings("ImmutableMemberCollection")
    private record Leaderboards(List<RequesterCount> movies, List<RequesterCount> tv) {}

    // Internal data carrier; its collection components are never mutated after construction.
    @SuppressWarnings("ImmutableMemberCollection")
    private record StatsSnapshot(
            Leaderboards boards, List<CodecCount> movieCodecs, List<CodecCount> episodeCodecs) {}

    private final MovieRequestRepository movieRequestRepository;
    private final TvRequestRepository tvRequestRepository;
    private final TvEpisodeRequestRepository tvEpisodeRequestRepository;
    private final FfprobeScanService ffprobeScanService;
    private final ExecutorService uiTaskExecutor;

    private final ProgressBar statsProgress = RequestViewSupport.indeterminateBar();
    private final AtomicBoolean statsLoading = new AtomicBoolean(false);

    private final Section<RequesterCount> movieBoard;
    private final Section<RequesterCount> tvBoard;
    private final Section<CodecCount> movieCodecTable;
    private final Section<CodecCount> episodeCodecTable;

    public StatsView(
            MovieRequestRepository movieRequestRepository,
            TvRequestRepository tvRequestRepository,
            TvEpisodeRequestRepository tvEpisodeRequestRepository,
            FfprobeScanService ffprobeScanService,
            ExecutorService uiTaskExecutor) {
        this.movieRequestRepository = movieRequestRepository;
        this.tvRequestRepository = tvRequestRepository;
        this.tvEpisodeRequestRepository = tvEpisodeRequestRepository;
        this.ffprobeScanService = ffprobeScanService;
        this.uiTaskExecutor = uiTaskExecutor;

        movieBoard = new Section<>("Top movie requesters", leaderboardGrid("Movie requests"));
        tvBoard = new Section<>("Top TV requesters", leaderboardGrid("TV requests"));
        movieCodecTable = new Section<>("Movie codecs", codecGrid("Movies"));
        episodeCodecTable = new Section<>("TV episode codecs", codecGrid("Episodes"));

        setWidthFull();
        add(new H2("Stats"));
        add(new H3("Leaderboards (top " + LEADERBOARD_SIZE + ")"));
        add(statsProgress);
        var boards = new HorizontalLayout(movieBoard.layout(), tvBoard.layout());
        boards.setWidthFull();
        add(boards);
        add(new H3("Codecs"));
        var codecs = new HorizontalLayout(movieCodecTable.layout(), episodeCodecTable.layout());
        codecs.setWidthFull();
        add(codecs);
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        getUI().ifPresent(this::loadStats);
    }

    /** Loads every stat (a cheap DB read) off the UI thread; results pushed back via {@link UI#access}. */
    private void loadStats(UI ui) {
        if (!statsLoading.compareAndSet(false, true)) {
            return;
        }
        CompletableFuture.supplyAsync(this::computeStats, uiTaskExecutor)
                .whenComplete((stats, throwable) -> ui.access(() -> {
                    try {
                        if (throwable != null) {
                            log.warn("Failed to load stats", throwable);
                        } else {
                            movieBoard.set(stats.boards().movies());
                            tvBoard.set(stats.boards().tv());
                            movieCodecTable.set(stats.movieCodecs());
                            episodeCodecTable.set(stats.episodeCodecs());
                        }
                    } finally {
                        statsLoading.set(false);
                        statsProgress.setVisible(false);
                    }
                }));
    }

    private StatsSnapshot computeStats() {
        List<MovieRequest> movies = movieRequestRepository.findAll();
        Map<String, Long> movieBytes = new HashMap<>();
        for (MovieRequest m : movies) {
            Long size = m.getLocalFileSize();
            if (size != null) {
                movieBytes.merge(userKey(m.getOmbiUserName()), size, Long::sum);
            }
        }
        var boards = new Leaderboards(
                leaderboard(movies, movieBytes), leaderboard(tvRequestRepository.findAll(), tvBytesByUser()));
        // Movie ids come from the list already read above, so the movie codec table adds only the scan query.
        // Episodes are counted from an id-only query rather than loading every episode entity.
        List<Long> movieIds = movies.stream().map(MovieRequest::getId).toList();
        return new StatsSnapshot(
                boards,
                codecCounts(movieIds, ffprobeScanService.latestMovieVideoCodecs()),
                codecCounts(
                        tvEpisodeRequestRepository.findAllEpisodeIds(),
                        ffprobeScanService.latestEpisodeVideoCodecs()));
    }

    private Map<String, Long> tvBytesByUser() {
        Map<String, Long> bytes = new HashMap<>();
        for (Object[] row : tvEpisodeRequestRepository.sumLocalFileSizeByTvRequestOmbiUserName()) {
            var sum = (Long) row[1];
            if (sum != null) {
                bytes.merge(userKey((String) row[0]), sum, Long::sum);
            }
        }
        return bytes;
    }

    /**
     * Counts requests per Ombi username (blank → "unknown") with availability and total bytes consumed (looked up in
     * {@code bytesByUser}, defaulting to zero), most-requested first, top N. {@code bytesByUser} keys must already be
     * normalized via {@link #userKey(String)}.
     */
    static List<RequesterCount> leaderboard(List<? extends Request> requests, Map<String, Long> bytesByUser) {
        Map<String, long[]> stats = new HashMap<>(); // [total, available]
        for (Request request : requests) {
            String key = userKey(request.getOmbiUserName());
            long[] tally = stats.computeIfAbsent(key, k -> new long[2]);
            tally[0]++;
            if (request.isAvailable()) {
                tally[1]++;
            }
        }
        return stats.entrySet().stream()
                .map(e -> new RequesterCount(
                        e.getKey(), e.getValue()[0], e.getValue()[1], bytesByUser.getOrDefault(e.getKey(), 0L)))
                .sorted(Comparator.comparingLong(RequesterCount::count)
                        .reversed()
                        .thenComparing(RequesterCount::username, String.CASE_INSENSITIVE_ORDER))
                .limit(LEADERBOARD_SIZE)
                .toList();
    }

    static String userKey(@Nullable String ombiUserName) {
        return ombiUserName == null || ombiUserName.isBlank() ? UNKNOWN_USER : ombiUserName;
    }

    /**
     * Counts requests per video codec (lowercased, so casing can't split a codec across two rows), most-common first.
     * Requests with no codec in {@code codecByRequestId} — never ffprobe-scanned, or scanned with no video stream —
     * bucket into {@value #NO_CODEC}, so the counts add up to the library size and the shares are meaningful.
     *
     * <p>Driven by {@code requestIds} rather than the codec map's keys: an {@code FfprobeScan} holds only a soft
     * reference to its request and outlives it, so the map can carry ids of deleted movies or episodes that must not be
     * counted.
     */
    static List<CodecCount> codecCounts(Collection<Long> requestIds, Map<Long, String> codecByRequestId) {
        Map<String, Long> counts = new HashMap<>();
        for (Long requestId : requestIds) {
            counts.merge(codecKey(codecByRequestId.get(requestId)), 1L, Long::sum);
        }
        int total = requestIds.size();
        return counts.entrySet().stream()
                .map(e -> new CodecCount(e.getKey(), e.getValue(), total == 0 ? 0.0 : e.getValue() * 100.0 / total))
                .sorted(Comparator.comparingLong(CodecCount::count)
                        .reversed()
                        .thenComparing(CodecCount::codec, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    private static String codecKey(@Nullable String codec) {
        return codec == null || codec.isBlank() ? NO_CODEC : codec.trim().toLowerCase(Locale.ROOT);
    }

    private static Grid<RequesterCount> leaderboardGrid(String countHeader) {
        Grid<RequesterCount> grid = RequestViewSupport.compactGrid();
        grid.addColumn(RequesterCount::username)
                .setHeader("User")
                .setAutoWidth(true)
                .setFlexGrow(1);
        grid.addColumn(RequesterCount::count).setHeader(countHeader).setAutoWidth(true);
        grid.addColumn(r -> String.format("%.0f%%", r.percentComplete()))
                .setHeader("% Complete")
                .setAutoWidth(true);
        grid.addColumn(r -> RequestViewSupport.formatBytes(r.bytes()))
                .setHeader("Bytes")
                .setAutoWidth(true);
        return grid;
    }

    private static Grid<CodecCount> codecGrid(String countHeader) {
        Grid<CodecCount> grid = RequestViewSupport.compactGrid();
        grid.addColumn(CodecCount::codec).setHeader("Codec").setAutoWidth(true).setFlexGrow(1);
        grid.addColumn(CodecCount::count).setHeader(countHeader).setAutoWidth(true);
        // One decimal place, unlike the leaderboards' whole percentages: a library spreads across enough codecs
        // that rounding the long tail to 0% would hide it.
        grid.addColumn(c -> String.format("%.1f%%", c.percentOfLibrary()))
                .setHeader("% of Library")
                .setAutoWidth(true);
        return grid;
    }
}
