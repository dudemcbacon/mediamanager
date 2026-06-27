package report.butt.mediamanager.route;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.contextmenu.GridContextMenu;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.progressbar.ProgressBar;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import report.butt.mediamanager.controller.MovieController;
import report.butt.mediamanager.controller.TvController;
import report.butt.mediamanager.route.RequestViewSupport.Section;
import report.butt.mediamanager.service.DownloadCleanupService;
import report.butt.mediamanager.service.NotificationService;
import report.butt.mediamanager.service.NotificationService.Download;
import report.butt.mediamanager.service.NotificationService.HealthIssue;
import report.butt.mediamanager.service.NotificationService.ImportBlocked;
import report.butt.mediamanager.service.NotificationService.NewRequestRow;
import report.butt.mediamanager.service.NotificationService.NotificationSnapshot;
import report.butt.mediamanager.service.NotificationService.OverdueMovieRow;
import report.butt.mediamanager.service.NotificationService.OverdueTvRow;
import report.butt.mediamanager.service.NotificationService.RemovedDownloadRow;
import report.butt.mediamanager.service.NotificationService.RequestLink;
import report.butt.mediamanager.service.NotificationService.UnsearchedRow;

/**
 * Notifications dashboard: the same findings the notification email reports, shown as grids with deep links and
 * bulk/per-row actions. The snapshot loads asynchronously (with a progress bar) on attach — it hits
 * Deluge/Radarr/Sonarr and can be slow — so the page renders immediately; results are delivered via server push (see
 * {@code @Push}).
 */
@Route("notifications")
@RolesAllowed("ADMIN")
// Async-UI view: remote/DB data is loaded off the UI thread via CompletableFuture + whenComplete/UI#access (@Push).
// Each such future handles its own success and failure in the callback (log + toast) and is intentionally not
// awaited — blocking on it would freeze the UI thread — so FutureReturnValueIgnored is suppressed class-wide.
@SuppressWarnings("FutureReturnValueIgnored")
@NullMarked
public class NotificationsView extends VerticalLayout {

    private static final Logger log = LoggerFactory.getLogger(NotificationsView.class);

    private static final DateTimeFormatter DATE =
            DateTimeFormatter.ofPattern("yyyy-MM-dd").withZone(ZoneId.systemDefault());

    // The downloads list is unbounded (every active torrent). At or below this many rows the grid sizes to its
    // content like the other dashboard sections; above it the grid would exceed Vaadin's single-fetch cap
    // (setAllRowsVisible fetches every row, max 10 pages), so it switches to a fixed-height lazily-loaded scroller.
    private static final int DOWNLOADS_ROW_CAP = 100;

    private final NotificationService notificationService;
    private final DownloadCleanupService downloadCleanupService;
    private final MovieController movieController;
    private final TvController tvController;
    private final String ombiUrl;
    private final String radarrUrl;
    private final String sonarrUrl;
    private final ExecutorService uiTaskExecutor;

    private final ProgressBar snapshotProgress = RequestViewSupport.indeterminateBar();
    private final AtomicBoolean snapshotLoading = new AtomicBoolean(false);
    private final AtomicBoolean cleanupInFlight = new AtomicBoolean(false);
    private final AtomicBoolean searchInFlight = new AtomicBoolean(false);

    private final Button deleteAllDownloads = new Button("Delete all");
    private final Button searchAllUnsearchedMovies = new Button("Search all");
    private final Button searchAllUnsearchedTv = new Button("Search all");

    private List<Download> currentDownloads = List.of();
    private List<UnsearchedRow> currentUnsearchedMovies = List.of();
    private List<UnsearchedRow> currentUnsearchedTv = List.of();

    private final Section<Download> downloads;
    private final Section<RemovedDownloadRow> removedDownloads;
    private final Section<ImportBlocked> importBlocked;
    private final Section<OverdueMovieRow> overdueMovies;
    private final Section<OverdueTvRow> overdueTv;
    private final Section<UnsearchedRow> unsearchedMovies;
    private final Section<UnsearchedRow> unsearchedTv;
    private final Section<NewRequestRow> newRequests;
    private final Section<HealthIssue> health;
    private final Section<String> unreachable;

    public NotificationsView(
            NotificationService notificationService,
            DownloadCleanupService downloadCleanupService,
            MovieController movieController,
            TvController tvController,
            @Value("${ombi.url}") String ombiUrl,
            @Value("${radarr.url}") String radarrUrl,
            @Value("${sonarr.url}") String sonarrUrl,
            ExecutorService uiTaskExecutor) {
        this.notificationService = notificationService;
        this.downloadCleanupService = downloadCleanupService;
        this.movieController = movieController;
        this.tvController = tvController;
        this.ombiUrl = ombiUrl;
        this.radarrUrl = radarrUrl;
        this.sonarrUrl = sonarrUrl;
        this.uiTaskExecutor = uiTaskExecutor;

        downloads = new Section<>("Downloads in progress", downloadGrid());
        removedDownloads = new Section<>("Stuck downloads removed since the last run", removedDownloadGrid());
        importBlocked = new Section<>("Import-blocked downloads", importBlockedGrid());
        overdueMovies = new Section<>("Overdue movie requests", overdueMovieGrid());
        overdueTv = new Section<>("Overdue TV requests", overdueTvGrid());
        unsearchedMovies = new Section<>("Movie requests not searched recently", unsearchedGrid());
        unsearchedTv = new Section<>("TV requests not searched recently", unsearchedGrid());
        newRequests = new Section<>("New requests", newRequestGrid());
        health = new Section<>("Service health warnings", healthGrid());
        unreachable = new Section<>("Unreachable integrations", unreachableGrid());

        deleteAllDownloads.addClickListener(
                e -> getUI().ifPresent(ui -> cleanupTorrents(ui, hashesOf(currentDownloads, Download::hash))));
        searchAllUnsearchedMovies.addClickListener(
                e -> getUI().ifPresent(ui -> searchUnsearched(ui, currentUnsearchedMovies, false)));
        searchAllUnsearchedTv.addClickListener(
                e -> getUI().ifPresent(ui -> searchUnsearched(ui, currentUnsearchedTv, true)));
        addDeleteContextMenu(downloads.grid(), Download::hash);

        setWidthFull();
        add(new H2("Notifications"));
        add(snapshotProgress);
        add(
                downloads.layout(deleteAllDownloads),
                removedDownloads.layout(),
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
        getUI().ifPresent(this::loadSnapshot);
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
                            log.warn("Failed to load notification snapshot", throwable);
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
        currentDownloads = snapshot.downloads();
        currentUnsearchedMovies = snapshot.unsearchedMovies();
        currentUnsearchedTv = snapshot.unsearchedTv();
        sizeDownloadsGrid(snapshot.downloads().size());
        downloads.set(snapshot.downloads());
        removedDownloads.set(snapshot.removedDownloads());
        importBlocked.set(snapshot.importBlocked());
        overdueMovies.set(snapshot.overdueMovies());
        overdueTv.set(snapshot.overdueTv());
        unsearchedMovies.set(snapshot.unsearchedMovies());
        unsearchedTv.set(snapshot.unsearchedTv());
        newRequests.set(snapshot.newRequests());
        health.set(snapshot.healthIssues());
        unreachable.set(snapshot.unreachableIntegrations());
    }

    /**
     * Sizes the downloads grid to its row count: small lists size to content (no scrollbar, like the other sections);
     * a large list switches to a fixed-height, lazily-loaded scroller so it stays under Vaadin's single-fetch page cap
     * and doesn't render thousands of DOM rows. See {@link #DOWNLOADS_ROW_CAP}.
     */
    private void sizeDownloadsGrid(int count) {
        Grid<Download> grid = downloads.grid();
        if (count > DOWNLOADS_ROW_CAP) {
            grid.setAllRowsVisible(false);
            grid.setHeight("600px");
        } else {
            grid.setHeight(null);
            grid.setAllRowsVisible(true);
        }
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
                .whenComplete((_, throwable) -> ui.access(() -> {
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
        deleteAllDownloads.setEnabled(enabled);
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

    // --- link building ---

    private @Nullable String ombiHref(@Nullable RequestLink link) {
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

    private @Nullable String arrHref(@Nullable RequestLink link) {
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

    /** Builds the downloads grid: name, progress, stuckness, added, request, and Ombi + Radarr/Sonarr links. */
    private Grid<Download> downloadGrid() {
        Grid<Download> grid = RequestViewSupport.compactGrid();
        grid.addColumn(Download::name)
                .setHeader("Name")
                .setAutoWidth(true)
                .setFlexGrow(1)
                .setSortable(true)
                .setComparator(Comparator.comparing(Download::name, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)));
        grid.addColumn(d -> percent(d.progress()))
                .setHeader("Progress")
                .setAutoWidth(true)
                .setSortable(true)
                .setComparator(Comparator.comparingDouble(Download::progress));
        grid.addColumn(d -> stuckPercent(d.stuckness()))
                .setHeader("Stuckness")
                .setAutoWidth(true)
                .setSortable(true)
                .setComparator(Comparator.comparingDouble(Download::stuckness));
        grid.addColumn(d -> dateText(d.added()))
                .setHeader("Added")
                .setAutoWidth(true)
                .setSortable(true)
                .setComparator(Comparator.comparing(Download::added, Comparator.nullsLast(Comparator.naturalOrder())));
        grid.addColumn(d -> orDash(d.linkedRequest()))
                .setHeader("Request")
                .setAutoWidth(true)
                .setSortable(true)
                .setComparator(
                        Comparator.comparing(Download::linkedRequest, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)));
        grid.addColumn(RequestViewSupport.linkRenderer(d -> ombiHref(d.link())))
                .setHeader("Ombi")
                .setAutoWidth(true);
        grid.addColumn(RequestViewSupport.linkRenderer(d -> arrHref(d.link())))
                .setHeader("Radarr/Sonarr")
                .setAutoWidth(true);
        RequestViewSupport.suppressGridContextMenuOnLinks(grid);
        return grid;
    }

    /** Builds the removed-downloads grid: name, progress, stuckness, request, and when it was removed. */
    private static Grid<RemovedDownloadRow> removedDownloadGrid() {
        Grid<RemovedDownloadRow> grid = RequestViewSupport.compactGrid();
        grid.addColumn(RemovedDownloadRow::name)
                .setHeader("Name")
                .setAutoWidth(true)
                .setFlexGrow(1)
                .setSortable(true)
                .setComparator(Comparator.comparing(
                        RemovedDownloadRow::name, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)));
        grid.addColumn(d -> percent(d.progress()))
                .setHeader("Progress")
                .setAutoWidth(true)
                .setSortable(true)
                .setComparator(Comparator.comparingDouble(RemovedDownloadRow::progress));
        grid.addColumn(d -> stuckPercent(d.stuckness()))
                .setHeader("Stuckness")
                .setAutoWidth(true)
                .setSortable(true)
                .setComparator(Comparator.comparingDouble(RemovedDownloadRow::stuckness));
        grid.addColumn(d -> orDash(d.linkedRequest()))
                .setHeader("Request")
                .setAutoWidth(true)
                .setSortable(true)
                .setComparator(Comparator.comparing(
                        RemovedDownloadRow::linkedRequest, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)));
        grid.addColumn(d -> dateText(d.removedAt()))
                .setHeader("Removed")
                .setAutoWidth(true)
                .setSortable(true)
                .setComparator(Comparator.comparing(RemovedDownloadRow::removedAt));
        return grid;
    }

    private static Grid<ImportBlocked> importBlockedGrid() {
        Grid<ImportBlocked> grid = RequestViewSupport.compactGrid();
        grid.addColumn(ImportBlocked::source)
                .setHeader("Source")
                .setAutoWidth(true)
                .setSortable(true)
                .setComparator(Comparator.comparing(ImportBlocked::source, String.CASE_INSENSITIVE_ORDER));
        grid.addColumn(ImportBlocked::title)
                .setHeader("Title")
                .setAutoWidth(true)
                .setFlexGrow(1)
                .setSortable(true)
                .setComparator(Comparator.comparing(ImportBlocked::title, String.CASE_INSENSITIVE_ORDER));
        grid.addColumn(b -> b.season() == null ? "—" : "S" + b.season())
                .setHeader("Season")
                .setAutoWidth(true)
                .setSortable(true)
                .setComparator(
                        Comparator.comparing(ImportBlocked::season, Comparator.nullsLast(Comparator.naturalOrder())));
        return grid;
    }

    private Grid<OverdueMovieRow> overdueMovieGrid() {
        Grid<OverdueMovieRow> grid = RequestViewSupport.compactGrid();
        grid.addColumn(OverdueMovieRow::title)
                .setHeader("Title")
                .setAutoWidth(true)
                .setFlexGrow(1)
                .setSortable(true)
                .setComparator(Comparator.comparing(
                        OverdueMovieRow::title, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)));
        grid.addColumn(r -> dateText(r.requested()))
                .setHeader("Requested")
                .setAutoWidth(true)
                .setSortable(true)
                .setComparator(Comparator.comparing(
                        OverdueMovieRow::requested, Comparator.nullsLast(Comparator.naturalOrder())));
        grid.addColumn(OverdueMovieRow::requester)
                .setHeader("Requester")
                .setAutoWidth(true)
                .setSortable(true)
                .setComparator(Comparator.comparing(OverdueMovieRow::requester, String.CASE_INSENSITIVE_ORDER));
        grid.addColumn(r -> dateText(r.lastSearched()))
                .setHeader("Last searched")
                .setAutoWidth(true)
                .setSortable(true)
                .setComparator(Comparator.comparing(
                        OverdueMovieRow::lastSearched, Comparator.nullsLast(Comparator.naturalOrder())));
        grid.addColumn(OverdueMovieRow::queued)
                .setHeader("Queued")
                .setAutoWidth(true)
                .setSortable(true)
                .setComparator(Comparator.comparingInt(OverdueMovieRow::queued));
        grid.addColumn(RequestViewSupport.linkRenderer(r -> ombiHref(r.link())))
                .setHeader("Ombi")
                .setAutoWidth(true);
        grid.addColumn(RequestViewSupport.linkRenderer(r -> arrHref(r.link())))
                .setHeader("Radarr")
                .setAutoWidth(true);
        return grid;
    }

    private Grid<OverdueTvRow> overdueTvGrid() {
        Grid<OverdueTvRow> grid = RequestViewSupport.compactGrid();
        grid.addColumn(OverdueTvRow::title)
                .setHeader("Title")
                .setAutoWidth(true)
                .setFlexGrow(1)
                .setSortable(true)
                .setComparator(
                        Comparator.comparing(OverdueTvRow::title, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)));
        grid.addColumn(r -> dateText(r.requested()))
                .setHeader("Requested")
                .setAutoWidth(true)
                .setSortable(true)
                .setComparator(
                        Comparator.comparing(OverdueTvRow::requested, Comparator.nullsLast(Comparator.naturalOrder())));
        grid.addColumn(OverdueTvRow::requester)
                .setHeader("Requester")
                .setAutoWidth(true)
                .setSortable(true)
                .setComparator(Comparator.comparing(OverdueTvRow::requester, String.CASE_INSENSITIVE_ORDER));
        grid.addColumn(r -> dateText(r.lastSearched()))
                .setHeader("Last searched")
                .setAutoWidth(true)
                .setSortable(true)
                .setComparator(Comparator.comparing(
                        OverdueTvRow::lastSearched, Comparator.nullsLast(Comparator.naturalOrder())));
        grid.addColumn(OverdueTvRow::queued)
                .setHeader("Queued")
                .setAutoWidth(true)
                .setSortable(true)
                .setComparator(Comparator.comparingInt(OverdueTvRow::queued));
        grid.addColumn(r -> r.downloadedEpisodes() + "/" + r.totalEpisodes())
                .setHeader("Episodes")
                .setAutoWidth(true)
                .setSortable(true)
                .setComparator(Comparator.comparingDouble(NotificationsView::episodeRatio));
        grid.addColumn(RequestViewSupport.linkRenderer(r -> ombiHref(r.link())))
                .setHeader("Ombi")
                .setAutoWidth(true);
        grid.addColumn(RequestViewSupport.linkRenderer(r -> arrHref(r.link())))
                .setHeader("Sonarr")
                .setAutoWidth(true);
        return grid;
    }

    private static Grid<UnsearchedRow> unsearchedGrid() {
        Grid<UnsearchedRow> grid = RequestViewSupport.compactGrid();
        grid.addColumn(UnsearchedRow::title)
                .setHeader("Title")
                .setAutoWidth(true)
                .setFlexGrow(1)
                .setSortable(true)
                .setComparator(Comparator.comparing(
                        UnsearchedRow::title, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)));
        grid.addColumn(r -> dateText(r.lastSearched()))
                .setHeader("Last searched")
                .setAutoWidth(true)
                .setSortable(true)
                .setComparator(Comparator.comparing(
                        UnsearchedRow::lastSearched, Comparator.nullsLast(Comparator.naturalOrder())));
        return grid;
    }

    private static Grid<NewRequestRow> newRequestGrid() {
        Grid<NewRequestRow> grid = RequestViewSupport.compactGrid();
        grid.addColumn(NewRequestRow::type)
                .setHeader("Type")
                .setAutoWidth(true)
                .setSortable(true)
                .setComparator(Comparator.comparing(NewRequestRow::type, String.CASE_INSENSITIVE_ORDER));
        grid.addColumn(NewRequestRow::title)
                .setHeader("Title")
                .setAutoWidth(true)
                .setFlexGrow(1)
                .setSortable(true)
                .setComparator(Comparator.comparing(
                        NewRequestRow::title, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)));
        grid.addColumn(r -> dateText(r.requested()))
                .setHeader("Requested")
                .setAutoWidth(true)
                .setSortable(true)
                .setComparator(Comparator.comparing(
                        NewRequestRow::requested, Comparator.nullsLast(Comparator.naturalOrder())));
        return grid;
    }

    private static Grid<HealthIssue> healthGrid() {
        Grid<HealthIssue> grid = RequestViewSupport.compactGrid();
        grid.addColumn(HealthIssue::source)
                .setHeader("Source")
                .setAutoWidth(true)
                .setSortable(true)
                .setComparator(Comparator.comparing(HealthIssue::source, String.CASE_INSENSITIVE_ORDER));
        grid.addColumn(h -> orDash(h.type()))
                .setHeader("Type")
                .setAutoWidth(true)
                .setSortable(true)
                .setComparator(
                        Comparator.comparing(HealthIssue::type, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)));
        grid.addColumn(h -> orDash(h.message()))
                .setHeader("Message")
                .setAutoWidth(true)
                .setFlexGrow(1)
                .setSortable(true)
                .setComparator(Comparator.comparing(
                        HealthIssue::message, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)));
        return grid;
    }

    private static Grid<String> unreachableGrid() {
        Grid<String> grid = RequestViewSupport.compactGrid();
        grid.addColumn(s -> s)
                .setHeader("Integration")
                .setAutoWidth(true)
                .setFlexGrow(1)
                .setSortable(true)
                .setComparator(String.CASE_INSENSITIVE_ORDER);
        return grid;
    }

    // --- small helpers ---

    private static String percent(double progress) {
        return String.format("%.1f%%", progress);
    }

    /** Formats a 0..1 stuckness score as a whole-number percentage. */
    private static String stuckPercent(double score) {
        return String.format("%.0f%%", score * 100);
    }

    /** Fraction of a show's episodes downloaded, so the "Episodes" column sorts by progress (0 when none exist). */
    private static double episodeRatio(OverdueTvRow row) {
        return row.totalEpisodes() == 0 ? 0.0 : (double) row.downloadedEpisodes() / row.totalEpisodes();
    }

    private static String dateText(@Nullable Instant instant) {
        return instant == null ? "—" : DATE.format(instant);
    }

    private static String orDash(@Nullable String value) {
        return value == null || value.isBlank() ? "—" : value;
    }
}
