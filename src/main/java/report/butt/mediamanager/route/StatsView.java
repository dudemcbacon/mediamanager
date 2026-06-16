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
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import report.butt.mediamanager.model.MovieRequest;
import report.butt.mediamanager.model.Request;
import report.butt.mediamanager.repository.MovieRequestRepository;
import report.butt.mediamanager.repository.TvEpisodeRequestRepository;
import report.butt.mediamanager.repository.TvRequestRepository;
import report.butt.mediamanager.route.RequestViewSupport.Section;

/**
 * Stats dashboard: requester leaderboards (and room for any future stats). The leaderboards load asynchronously on
 * attach so the page renders immediately; results are pushed back via server push (see {@code @Push}).
 */
@Route("stats")
@RolesAllowed("ADMIN")
public class StatsView extends VerticalLayout {

    private static final Logger log = LoggerFactory.getLogger(StatsView.class);

    private static final int LEADERBOARD_SIZE = 10;
    private static final String UNKNOWN_USER = "unknown";

    /** A requester, how many requests they've made, how many are available, and how many bytes they consume. */
    public record RequesterCount(String username, long count, long available, long bytes) {
        public double percentComplete() {
            return count == 0 ? 0.0 : available * 100.0 / count;
        }
    }

    private record Leaderboards(List<RequesterCount> movies, List<RequesterCount> tv) {}

    private final MovieRequestRepository movieRequestRepository;
    private final TvRequestRepository tvRequestRepository;
    private final TvEpisodeRequestRepository tvEpisodeRequestRepository;
    private final ExecutorService uiTaskExecutor;

    private final ProgressBar leaderboardProgress = RequestViewSupport.indeterminateBar();
    private final AtomicBoolean leaderboardLoading = new AtomicBoolean(false);

    private final Section<RequesterCount> movieBoard;
    private final Section<RequesterCount> tvBoard;

    public StatsView(
            MovieRequestRepository movieRequestRepository,
            TvRequestRepository tvRequestRepository,
            TvEpisodeRequestRepository tvEpisodeRequestRepository,
            ExecutorService uiTaskExecutor) {
        this.movieRequestRepository = movieRequestRepository;
        this.tvRequestRepository = tvRequestRepository;
        this.tvEpisodeRequestRepository = tvEpisodeRequestRepository;
        this.uiTaskExecutor = uiTaskExecutor;

        movieBoard = new Section<>("Top movie requesters", leaderboardGrid("Movie requests"));
        tvBoard = new Section<>("Top TV requesters", leaderboardGrid("TV requests"));

        setWidthFull();
        add(new H2("Stats"));
        add(new H3("Leaderboards (top " + LEADERBOARD_SIZE + ")"));
        add(leaderboardProgress);
        HorizontalLayout boards = new HorizontalLayout(movieBoard.layout(), tvBoard.layout());
        boards.setWidthFull();
        add(boards);
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        getUI().ifPresent(this::loadLeaderboards);
    }

    /** Loads the leaderboards (a cheap DB read) off the UI thread; results pushed back via {@link UI#access}. */
    private void loadLeaderboards(UI ui) {
        if (!leaderboardLoading.compareAndSet(false, true)) {
            return;
        }
        CompletableFuture.supplyAsync(this::computeLeaderboards, uiTaskExecutor)
                .whenComplete((boards, throwable) -> ui.access(() -> {
                    try {
                        if (throwable != null) {
                            log.warn("Failed to load stats leaderboards", throwable);
                        } else {
                            movieBoard.set(boards.movies());
                            tvBoard.set(boards.tv());
                        }
                    } finally {
                        leaderboardLoading.set(false);
                        leaderboardProgress.setVisible(false);
                    }
                }));
    }

    private Leaderboards computeLeaderboards() {
        List<MovieRequest> movies = movieRequestRepository.findAll();
        Map<String, Long> movieBytes = new HashMap<>();
        for (MovieRequest m : movies) {
            Long size = m.getLocalFileSize();
            if (size != null) {
                movieBytes.merge(userKey(m.getOmbiUserName()), size, Long::sum);
            }
        }
        return new Leaderboards(
                leaderboard(movies, movieBytes),
                leaderboard(tvRequestRepository.findAll(), tvBytesByUser()));
    }

    private Map<String, Long> tvBytesByUser() {
        Map<String, Long> bytes = new HashMap<>();
        for (Object[] row : tvEpisodeRequestRepository.sumLocalFileSizeByTvRequestOmbiUserName()) {
            Long sum = (Long) row[1];
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
                        e.getKey(),
                        e.getValue()[0],
                        e.getValue()[1],
                        bytesByUser.getOrDefault(e.getKey(), 0L)))
                .sorted(Comparator.comparingLong(RequesterCount::count)
                        .reversed()
                        .thenComparing(RequesterCount::username, String.CASE_INSENSITIVE_ORDER))
                .limit(LEADERBOARD_SIZE)
                .toList();
    }

    static String userKey(String ombiUserName) {
        return ombiUserName == null || ombiUserName.isBlank() ? UNKNOWN_USER : ombiUserName;
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
}
