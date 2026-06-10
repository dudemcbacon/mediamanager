package report.butt.mediamanager.route;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.contextmenu.GridContextMenu;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.progressbar.ProgressBar;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.function.ToDoubleFunction;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import report.butt.mediamanager.controller.MovieController;
import report.butt.mediamanager.controller.TvController;
import report.butt.mediamanager.model.Request;
import report.butt.mediamanager.repository.MovieRequestRepository;
import report.butt.mediamanager.repository.TvRequestRepository;
import report.butt.mediamanager.service.DownloadCleanupService;
import report.butt.mediamanager.service.NotificationService;
import report.butt.mediamanager.service.NotificationService.HealthIssue;
import report.butt.mediamanager.service.NotificationService.ImportBlocked;
import report.butt.mediamanager.service.NotificationService.NewRequestRow;
import report.butt.mediamanager.service.NotificationService.NotificationSnapshot;
import report.butt.mediamanager.service.NotificationService.OverdueMovieRow;
import report.butt.mediamanager.service.NotificationService.OverdueTvRow;
import report.butt.mediamanager.service.NotificationService.RequestLink;
import report.butt.mediamanager.service.NotificationService.StuckDownload;
import report.butt.mediamanager.service.NotificationService.UnsearchedRow;
import report.butt.mediamanager.service.NotificationService.ZeroSeedDownload;

/**
 * Statistics dashboard: requester leaderboards plus the same findings the notification email reports, shown as grids
 * with deep links and bulk/per-row actions. Everything loads asynchronously (with progress bars) on attach so the page
 * renders immediately — the notification snapshot in particular hits Deluge/Radarr/Sonarr and can be slow. Server push
 * delivers the results (see {@code @Push}).
 */
@Route("admin")
@RolesAllowed("ADMIN")
public class AdminView extends VerticalLayout {

    private static final Logger log = LoggerFactory.getLogger(AdminView.class);

    private static final int LEADERBOARD_SIZE = 10;

    private static final DateTimeFormatter DATE =
            DateTimeFormatter.ofPattern("yyyy-MM-dd").withZone(ZoneId.systemDefault());

    /** A requester, how many requests they've made, and how many of those are available. */
    public record RequesterCount(String username, long count, long available) {
        public double percentComplete() {
            return count == 0 ? 0.0 : available * 100.0 / count;
        }
    }

    private record Leaderboards(List<RequesterCount> movies, List<RequesterCount> tv) {}

    private final MovieRequestRepository movieRequestRepository;
    private final TvRequestRepository tvRequestRepository;
    private final NotificationService notificationService;
    private final DownloadCleanupService downloadCleanupService;
    private final MovieController movieController;
    private final TvController tvController;
    private final String ombiUrl;
    private final String radarrUrl;
    private final String sonarrUrl;
    private final ExecutorService uiTaskExecutor;

    private final ProgressBar leaderboardProgress = indeterminateBar();
    private final ProgressBar snapshotProgress = indeterminateBar();
    private final AtomicBoolean leaderboardLoading = new AtomicBoolean(false);
    private final AtomicBoolean snapshotLoading = new AtomicBoolean(false);
    private final AtomicBoolean cleanupInFlight = new AtomicBoolean(false);
    private final AtomicBoolean searchInFlight = new AtomicBoolean(false);

    private final Button deleteAllStuck = new Button("Delete all");
    private final Button deleteAllZeroSeed = new Button("Delete all");
    private final Button searchAllUnsearchedMovies = new Button("Search all");
    private final Button searchAllUnsearchedTv = new Button("Search all");

    private List<StuckDownload> currentStuck = List.of();
    private List<ZeroSeedDownload> currentZeroSeed = List.of();
    private List<UnsearchedRow> currentUnsearchedMovies = List.of();
    private List<UnsearchedRow> currentUnsearchedTv = List.of();

    private final Section<RequesterCount> movieBoard;
    private final Section<RequesterCount> tvBoard;
    private final Section<StuckDownload> stuck;
    private final Section<ZeroSeedDownload> zeroSeed;
    private final Section<ImportBlocked> importBlocked;
    private final Section<OverdueMovieRow> overdueMovies;
    private final Section<OverdueTvRow> overdueTv;
    private final Section<UnsearchedRow> unsearchedMovies;
    private final Section<UnsearchedRow> unsearchedTv;
    private final Section<NewRequestRow> newRequests;
    private final Section<HealthIssue> health;
    private final Section<String> unreachable;

    public AdminView(
            MovieRequestRepository movieRequestRepository,
            TvRequestRepository tvRequestRepository,
            NotificationService notificationService,
            DownloadCleanupService downloadCleanupService,
            MovieController movieController,
            TvController tvController,
            @Value("${ombi.url}") String ombiUrl,
            @Value("${radarr.url}") String radarrUrl,
            @Value("${sonarr.url}") String sonarrUrl,
            ExecutorService uiTaskExecutor) {
        this.movieRequestRepository = movieRequestRepository;
        this.tvRequestRepository = tvRequestRepository;
        this.notificationService = notificationService;
        this.downloadCleanupService = downloadCleanupService;
        this.movieController = movieController;
        this.tvController = tvController;
        this.ombiUrl = ombiUrl;
        this.radarrUrl = radarrUrl;
        this.sonarrUrl = sonarrUrl;
        this.uiTaskExecutor = uiTaskExecutor;

        movieBoard = new Section<>("Top movie requesters", leaderboardGrid("Movie requests"));
        tvBoard = new Section<>("Top TV requesters", leaderboardGrid("TV requests"));
        stuck = new Section<>(
                "Stuck downloads",
                downloadGrid(
                        StuckDownload::name,
                        StuckDownload::progress,
                        StuckDownload::added,
                        StuckDownload::linkedRequest,
                        StuckDownload::link));
        zeroSeed = new Section<>(
                "Torrents with no seeds",
                downloadGrid(
                        ZeroSeedDownload::name,
                        ZeroSeedDownload::progress,
                        ZeroSeedDownload::added,
                        ZeroSeedDownload::linkedRequest,
                        ZeroSeedDownload::link));
        importBlocked = new Section<>("Import-blocked downloads", importBlockedGrid());
        overdueMovies = new Section<>("Overdue movie requests", overdueMovieGrid());
        overdueTv = new Section<>("Overdue TV requests", overdueTvGrid());
        unsearchedMovies = new Section<>("Movie requests not searched recently", unsearchedGrid());
        unsearchedTv = new Section<>("TV requests not searched recently", unsearchedGrid());
        newRequests = new Section<>("New requests", newRequestGrid());
        health = new Section<>("Service health warnings", healthGrid());
        unreachable = new Section<>("Unreachable integrations", unreachableGrid());

        deleteAllStuck.addClickListener(
                e -> getUI().ifPresent(ui -> cleanupTorrents(ui, hashesOf(currentStuck, StuckDownload::hash))));
        deleteAllZeroSeed.addClickListener(
                e -> getUI().ifPresent(ui -> cleanupTorrents(ui, hashesOf(currentZeroSeed, ZeroSeedDownload::hash))));
        searchAllUnsearchedMovies.addClickListener(
                e -> getUI().ifPresent(ui -> searchUnsearched(ui, currentUnsearchedMovies, false)));
        searchAllUnsearchedTv.addClickListener(
                e -> getUI().ifPresent(ui -> searchUnsearched(ui, currentUnsearchedTv, true)));
        addDeleteContextMenu(stuck.grid(), StuckDownload::hash);
        addDeleteContextMenu(zeroSeed.grid(), ZeroSeedDownload::hash);

        setWidthFull();

        add(new H2("Admin"));

        add(new H3("Leaderboards (top " + LEADERBOARD_SIZE + ")"));
        add(leaderboardProgress);
        HorizontalLayout boards = new HorizontalLayout(movieBoard.layout(), tvBoard.layout());
        boards.setWidthFull();
        add(boards);

        add(new H3("Notifications"));
        add(snapshotProgress);
        add(
                stuck.layout(deleteAllStuck),
                zeroSeed.layout(deleteAllZeroSeed),
                importBlocked.layout(),
                overdueMovies.layout(),
                overdueTv.layout(),
                unsearchedMovies.layout(searchAllUnsearchedMovies),
                unsearchedTv.layout(searchAllUnsearchedTv),
                newRequests.layout(),
                health.layout(),
                unreachable.layout());
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        getUI().ifPresent(ui -> {
            loadLeaderboards(ui);
            loadSnapshot(ui);
        });
    }

    /** Loads the leaderboards (a cheap DB read) off the UI thread; results pushed back via {@link UI#access}. */
    private void loadLeaderboards(UI ui) {
        if (!leaderboardLoading.compareAndSet(false, true)) {
            return;
        }
        CompletableFuture.supplyAsync(
                        () -> new Leaderboards(
                                leaderboard(movieRequestRepository.findAll()),
                                leaderboard(tvRequestRepository.findAll())),
                        uiTaskExecutor)
                .whenComplete((boards, throwable) -> ui.access(() -> {
                    try {
                        if (throwable != null) {
                            log.warn("Failed to load admin leaderboards", throwable);
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

    /**
     * Loads the notification snapshot (Deluge/Radarr/Sonarr + repo reads) off the UI thread. Uses
     * {@link NotificationService#snapshot()}, which sends no email, then populates every category grid.
     */
    private void loadSnapshot(UI ui) {
        if (!snapshotLoading.compareAndSet(false, true)) {
            return;
        }
        CompletableFuture.supplyAsync(notificationService::snapshot, uiTaskExecutor)
                .whenComplete((snapshot, throwable) -> ui.access(() -> {
                    try {
                        if (throwable != null) {
                            log.warn("Failed to load admin notification snapshot", throwable);
                        } else {
                            applySnapshot(snapshot);
                        }
                    } finally {
                        snapshotLoading.set(false);
                        snapshotProgress.setVisible(false);
                    }
                }));
    }

    private void applySnapshot(NotificationSnapshot snapshot) {
        currentStuck = snapshot.stuckDownloads();
        currentZeroSeed = snapshot.zeroSeedDownloads();
        currentUnsearchedMovies = snapshot.unsearchedMovies();
        currentUnsearchedTv = snapshot.unsearchedTv();
        stuck.set(snapshot.stuckDownloads());
        zeroSeed.set(snapshot.zeroSeedDownloads());
        importBlocked.set(snapshot.importBlocked());
        overdueMovies.set(snapshot.overdueMovies());
        overdueTv.set(snapshot.overdueTv());
        unsearchedMovies.set(snapshot.unsearchedMovies());
        unsearchedTv.set(snapshot.unsearchedTv());
        newRequests.set(snapshot.newRequests());
        health.set(snapshot.healthIssues());
        unreachable.set(snapshot.unreachableIntegrations());
    }

    // --- actions ---

    /**
     * Deletes the given torrents (removing from the client + blocklisting), searches Radarr/Sonarr for the affected
     * requests, and refreshes them — all off the UI thread. Reloads the snapshot afterwards so the grids reflect it.
     */
    private void cleanupTorrents(UI ui, Set<String> hashes) {
        if (hashes.isEmpty()) {
            Notification.show("No matching torrents to delete.");
            return;
        }
        if (!cleanupInFlight.compareAndSet(false, true)) {
            return;
        }
        setDownloadButtonsEnabled(false);
        CompletableFuture.supplyAsync(() -> downloadCleanupService.deleteTorrentsAndReprocess(hashes), uiTaskExecutor)
                .whenComplete((result, throwable) -> ui.access(() -> {
                    try {
                        if (throwable != null) {
                            log.warn("Failed to delete torrents", throwable);
                            Notification.show("Delete failed; see the server log.");
                        } else {
                            Notification.show("Deleted " + result.torrentsDeleted() + " torrent(s); reprocessed "
                                    + result.moviesReprocessed() + " movie(s) and " + result.showsReprocessed()
                                    + " show(s).");
                        }
                    } finally {
                        cleanupInFlight.set(false);
                        setDownloadButtonsEnabled(true);
                        loadSnapshot(ui);
                    }
                }));
    }

    /** Triggers a Radarr/Sonarr search for every entry in an unsearched grid, off the UI thread, then reloads. */
    private void searchUnsearched(UI ui, List<UnsearchedRow> rows, boolean tv) {
        List<Integer> ids = rows.stream()
                .map(UnsearchedRow::searchId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (ids.isEmpty()) {
            Notification.show("Nothing to search.");
            return;
        }
        if (!searchInFlight.compareAndSet(false, true)) {
            return;
        }
        searchAllUnsearchedMovies.setEnabled(false);
        searchAllUnsearchedTv.setEnabled(false);
        CompletableFuture.runAsync(
                        () -> {
                            if (tv) {
                                tvController.searchSeries(ids);
                            } else {
                                movieController.searchMovies(ids);
                            }
                        },
                        uiTaskExecutor)
                .whenComplete((unused, throwable) -> ui.access(() -> {
                    try {
                        if (throwable != null) {
                            log.warn("Failed to search requests", throwable);
                            Notification.show("Search failed; see the server log.");
                        } else {
                            Notification.show(
                                    "Requested a search for " + ids.size() + " " + (tv ? "show(s)." : "movie(s)."));
                        }
                    } finally {
                        searchInFlight.set(false);
                        searchAllUnsearchedMovies.setEnabled(true);
                        searchAllUnsearchedTv.setEnabled(true);
                        loadSnapshot(ui);
                    }
                }));
    }

    private void setDownloadButtonsEnabled(boolean enabled) {
        deleteAllStuck.setEnabled(enabled);
        deleteAllZeroSeed.setEnabled(enabled);
    }

    private static <T> Set<String> hashesOf(List<T> rows, Function<T, String> hashFn) {
        return rows.stream().map(hashFn).filter(Objects::nonNull).collect(Collectors.toSet());
    }

    /** Adds a "Delete Download" context-menu entry that deletes the right-clicked torrent. */
    private <T> void addDeleteContextMenu(Grid<T> grid, Function<T, String> hashFn) {
        GridContextMenu<T> menu = grid.addContextMenu();
        menu.addItem("Delete Download", e -> e.getItem().ifPresent(row -> {
            String hash = hashFn.apply(row);
            if (hash != null) {
                getUI().ifPresent(ui -> cleanupTorrents(ui, Set.of(hash)));
            }
        }));
    }

    /** Counts requests per Ombi username (blank → "unknown") with availability, most-requested first, top N. */
    static List<RequesterCount> leaderboard(List<? extends Request> requests) {
        Map<String, long[]> stats = new HashMap<>(); // [total, available]
        for (Request request : requests) {
            String user = request.getOmbiUserName();
            String key = user == null || user.isBlank() ? "unknown" : user;
            long[] tally = stats.computeIfAbsent(key, k -> new long[2]);
            tally[0]++;
            if (request.isAvailable()) {
                tally[1]++;
            }
        }
        return stats.entrySet().stream()
                .map(e -> new RequesterCount(e.getKey(), e.getValue()[0], e.getValue()[1]))
                .sorted(Comparator.comparingLong(RequesterCount::count)
                        .reversed()
                        .thenComparing(RequesterCount::username, String.CASE_INSENSITIVE_ORDER))
                .limit(LEADERBOARD_SIZE)
                .toList();
    }

    // --- link building ---

    private String ombiHref(RequestLink link) {
        if (link == null) {
            return null;
        }
        if (link.tv()) {
            return link.ombiExternalProviderId() == null
                    ? null
                    : ombiUrl + "/details/tv/" + link.ombiExternalProviderId();
        }
        return link.tmdbId() == null ? null : ombiUrl + "/details/movie/" + link.tmdbId();
    }

    private String arrHref(RequestLink link) {
        if (link == null) {
            return null;
        }
        if (link.tv()) {
            return link.sonarrTitleSlug() == null || link.sonarrTitleSlug().isBlank()
                    ? null
                    : sonarrUrl + "/series/" + link.sonarrTitleSlug();
        }
        return link.tmdbId() == null ? null : radarrUrl + "/movie/" + link.tmdbId();
    }

    // --- grid builders ---

    private static Grid<RequesterCount> leaderboardGrid(String countHeader) {
        Grid<RequesterCount> grid = compactGrid();
        grid.addColumn(RequesterCount::username)
                .setHeader("User")
                .setAutoWidth(true)
                .setFlexGrow(1);
        grid.addColumn(RequesterCount::count).setHeader(countHeader).setAutoWidth(true);
        grid.addColumn(r -> String.format("%.0f%%", r.percentComplete()))
                .setHeader("% Complete")
                .setAutoWidth(true);
        return grid;
    }

    /** Builds a stuck/zero-seed download grid: name, progress, added, request, and Ombi + Radarr/Sonarr links. */
    private <T> Grid<T> downloadGrid(
            Function<T, String> name,
            ToDoubleFunction<T> progress,
            Function<T, Instant> added,
            Function<T, String> request,
            Function<T, RequestLink> link) {
        Grid<T> grid = compactGrid();
        grid.addColumn(name::apply).setHeader("Name").setAutoWidth(true).setFlexGrow(1);
        grid.addColumn(d -> percent(progress.applyAsDouble(d)))
                .setHeader("Progress")
                .setAutoWidth(true);
        grid.addColumn(d -> dateText(added.apply(d))).setHeader("Added").setAutoWidth(true);
        grid.addColumn(d -> orDash(request.apply(d))).setHeader("Request").setAutoWidth(true);
        grid.addColumn(RequestViewSupport.linkRenderer(d -> ombiHref(link.apply(d))))
                .setHeader("Ombi")
                .setAutoWidth(true);
        grid.addColumn(RequestViewSupport.linkRenderer(d -> arrHref(link.apply(d))))
                .setHeader("Radarr/Sonarr")
                .setAutoWidth(true);
        RequestViewSupport.suppressGridContextMenuOnLinks(grid);
        return grid;
    }

    private static Grid<ImportBlocked> importBlockedGrid() {
        Grid<ImportBlocked> grid = compactGrid();
        grid.addColumn(ImportBlocked::source).setHeader("Source").setAutoWidth(true);
        grid.addColumn(ImportBlocked::title)
                .setHeader("Title")
                .setAutoWidth(true)
                .setFlexGrow(1);
        grid.addColumn(b -> b.season() == null ? "—" : "S" + b.season())
                .setHeader("Season")
                .setAutoWidth(true);
        return grid;
    }

    private Grid<OverdueMovieRow> overdueMovieGrid() {
        Grid<OverdueMovieRow> grid = compactGrid();
        grid.addColumn(OverdueMovieRow::title)
                .setHeader("Title")
                .setAutoWidth(true)
                .setFlexGrow(1);
        grid.addColumn(r -> dateText(r.requested())).setHeader("Requested").setAutoWidth(true);
        grid.addColumn(OverdueMovieRow::requester).setHeader("Requester").setAutoWidth(true);
        grid.addColumn(r -> dateText(r.lastSearched()))
                .setHeader("Last searched")
                .setAutoWidth(true);
        grid.addColumn(OverdueMovieRow::queued).setHeader("Queued").setAutoWidth(true);
        grid.addColumn(RequestViewSupport.linkRenderer(r -> ombiHref(r.link())))
                .setHeader("Ombi")
                .setAutoWidth(true);
        grid.addColumn(RequestViewSupport.linkRenderer(r -> arrHref(r.link())))
                .setHeader("Radarr")
                .setAutoWidth(true);
        return grid;
    }

    private Grid<OverdueTvRow> overdueTvGrid() {
        Grid<OverdueTvRow> grid = compactGrid();
        grid.addColumn(OverdueTvRow::title)
                .setHeader("Title")
                .setAutoWidth(true)
                .setFlexGrow(1);
        grid.addColumn(r -> dateText(r.requested())).setHeader("Requested").setAutoWidth(true);
        grid.addColumn(OverdueTvRow::requester).setHeader("Requester").setAutoWidth(true);
        grid.addColumn(r -> dateText(r.lastSearched()))
                .setHeader("Last searched")
                .setAutoWidth(true);
        grid.addColumn(OverdueTvRow::queued).setHeader("Queued").setAutoWidth(true);
        grid.addColumn(r -> r.downloadedEpisodes() + "/" + r.totalEpisodes())
                .setHeader("Episodes")
                .setAutoWidth(true);
        grid.addColumn(RequestViewSupport.linkRenderer(r -> ombiHref(r.link())))
                .setHeader("Ombi")
                .setAutoWidth(true);
        grid.addColumn(RequestViewSupport.linkRenderer(r -> arrHref(r.link())))
                .setHeader("Sonarr")
                .setAutoWidth(true);
        return grid;
    }

    private static Grid<UnsearchedRow> unsearchedGrid() {
        Grid<UnsearchedRow> grid = compactGrid();
        grid.addColumn(UnsearchedRow::title)
                .setHeader("Title")
                .setAutoWidth(true)
                .setFlexGrow(1);
        grid.addColumn(r -> dateText(r.lastSearched()))
                .setHeader("Last searched")
                .setAutoWidth(true);
        return grid;
    }

    private static Grid<NewRequestRow> newRequestGrid() {
        Grid<NewRequestRow> grid = compactGrid();
        grid.addColumn(NewRequestRow::type).setHeader("Type").setAutoWidth(true);
        grid.addColumn(NewRequestRow::title)
                .setHeader("Title")
                .setAutoWidth(true)
                .setFlexGrow(1);
        grid.addColumn(r -> dateText(r.requested())).setHeader("Requested").setAutoWidth(true);
        return grid;
    }

    private static Grid<HealthIssue> healthGrid() {
        Grid<HealthIssue> grid = compactGrid();
        grid.addColumn(HealthIssue::source).setHeader("Source").setAutoWidth(true);
        grid.addColumn(h -> orDash(h.type())).setHeader("Type").setAutoWidth(true);
        grid.addColumn(h -> orDash(h.message()))
                .setHeader("Message")
                .setAutoWidth(true)
                .setFlexGrow(1);
        return grid;
    }

    private static Grid<String> unreachableGrid() {
        Grid<String> grid = compactGrid();
        grid.addColumn(s -> s).setHeader("Integration").setAutoWidth(true).setFlexGrow(1);
        return grid;
    }

    // --- small helpers ---

    private static <T> Grid<T> compactGrid() {
        Grid<T> grid = new Grid<>();
        grid.setAllRowsVisible(true); // size to content so the dashboard shows every grid without inner scrollbars
        grid.setWidthFull();
        return grid;
    }

    private static ProgressBar indeterminateBar() {
        ProgressBar bar = new ProgressBar();
        bar.setIndeterminate(true);
        return bar;
    }

    private static String percent(double progress) {
        return String.format("%.1f%%", progress);
    }

    private static String dateText(Instant instant) {
        return instant == null ? "—" : DATE.format(instant);
    }

    private static String orDash(String value) {
        return value == null || value.isBlank() ? "—" : value;
    }

    /** A titled section (header with live count + grid) whose data arrives asynchronously. */
    private static final class Section<T> {

        private final String title;
        private final Span header;
        private final Grid<T> grid;

        private Section(String title, Grid<T> grid) {
            this.title = title;
            this.header = RequestViewSupport.coloredLabel(title, "var(--vaadin-text-color)");
            this.grid = grid;
        }

        private Grid<T> grid() {
            return grid;
        }

        private Component layout(Component... betweenHeaderAndGrid) {
            VerticalLayout layout = new VerticalLayout();
            layout.setPadding(false);
            layout.setSpacing(false);
            layout.setWidthFull();
            layout.add(header);
            layout.add(betweenHeaderAndGrid);
            layout.add(grid);
            return layout;
        }

        private void set(List<T> items) {
            header.setText(title + " (" + items.size() + ")");
            grid.setItems(items);
        }
    }
}
