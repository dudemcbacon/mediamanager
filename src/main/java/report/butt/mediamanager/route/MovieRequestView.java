package report.butt.mediamanager.route;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.badge.Badge;
import com.vaadin.flow.component.badge.BadgeVariant;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.card.Card;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridSortOrder;
import com.vaadin.flow.component.grid.contextmenu.GridContextMenu;
import com.vaadin.flow.component.grid.contextmenu.GridMenuItem;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.shared.Tooltip;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.provider.SortDirection;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.data.renderer.LitRenderer;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.shared.Registration;
import com.vaadin.flow.spring.annotation.UIScope;
import jakarta.annotation.security.PermitAll;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import report.butt.mediamanager.client.DelugeClient;
import report.butt.mediamanager.client.PlexClient;
import report.butt.mediamanager.client.SabnzbdClient;
import report.butt.mediamanager.controller.MovieController;
import report.butt.mediamanager.model.MovieRequest;
import report.butt.mediamanager.model.Note;
import report.butt.mediamanager.model.Validation;
import report.butt.mediamanager.model.deluge.DelugeTorrent;
import report.butt.mediamanager.model.radarr.RadarrHealthItem;
import report.butt.mediamanager.model.radarr.RadarrQueue;
import report.butt.mediamanager.model.radarr.RadarrQueueRecord;
import report.butt.mediamanager.model.sabnzbd.SabnzbdSlot;
import report.butt.mediamanager.repository.MovieRequestRepository;
import report.butt.mediamanager.repository.NoteRepository;
import report.butt.mediamanager.repository.ValidationRepository;
import report.butt.mediamanager.security.SecurityUtils;
import report.butt.mediamanager.service.NotificationService;
import report.butt.mediamanager.validation.Validator;

@Route("")
@PageTitle("Movies")
@PermitAll
@Component
@UIScope
@StyleSheet("grid-available.css")
public class MovieRequestView extends VerticalLayout {

    private static final Logger log = LoggerFactory.getLogger(MovieRequestView.class);

    private final Grid<MovieRequest> grid = new Grid<>(MovieRequest.class, false);
    private final MovieRequestRepository movieRequestRepository;
    private final MovieController movieController;
    private final ValidationRepository validationRepository;
    private final NoteRepository noteRepository;
    private final Set<String> knownValidatorNames;
    private final Map<Long, Map<String, Validation>> latestValidations = new HashMap<>();
    private final Map<Integer, String> queueStateByMovieId = new HashMap<>();
    private final Map<Integer, String> downloadIdByMovieId = new HashMap<>();
    private final Map<Integer, String> protocolByMovieId = new HashMap<>();
    private final Map<Integer, DelugeTorrent> torrentByMovieId = new HashMap<>();
    private final Map<Integer, SabnzbdSlot> slotByMovieId = new HashMap<>();
    private final AtomicBoolean downloadLoadInFlight = new AtomicBoolean(false);
    private final AtomicBoolean statsLoadInFlight = new AtomicBoolean(false);
    private final AtomicBoolean gridLoadInFlight = new AtomicBoolean(false);
    private final Set<Long> movieRequestsWithNotes = new HashSet<>();
    private final Checkbox showValidCheckbox = new Checkbox(true);
    private final Checkbox showStaleCheckbox = new Checkbox(false);
    private final Checkbox showWithNotesCheckbox = new Checkbox(true);
    private final Span showValidLabel = RequestViewSupport.coloredLabel("Show valid rows", "var(--aura-green-text)");
    private final Span showStaleLabel = RequestViewSupport.coloredLabel("Show stale rows", "var(--aura-yellow-text)");
    private final Span showWithNotesLabel =
            RequestViewSupport.coloredLabel("Show rows with notes", "var(--aura-blue-text)");
    private final Span totalLabel = RequestViewSupport.coloredLabel("Total movies", "var(--vaadin-text-color)");
    private final Span radarrQueueValue = new Span("—");
    private final Card radarrQueueCard = RequestViewSupport.statCard("Radarr Queue", radarrQueueValue);
    private final Tooltip radarrQueueTooltip = Tooltip.forComponent(radarrQueueCard);
    private final Span radarrHealthValue = new Span("—");
    private final Card radarrHealthCard = RequestViewSupport.statCard("Health Issues", radarrHealthValue);
    private final Tooltip radarrHealthTooltip = Tooltip.forComponent(radarrHealthCard);
    private final TextField searchField = new TextField();
    private final List<Button> bulkButtons = new ArrayList<>();
    private List<MovieRequest> allRequests = List.of();
    private final String ombiUrl;
    private final String radarrUrl;
    private final String plexUrl;
    private final String plexMachineIdentifier;
    private final PlexClient plexClient;
    private final DelugeClient delugeClient;
    private final SabnzbdClient sabnzbdClient;
    private final NotificationService notificationService;
    private final TransactionTemplate transactionTemplate;
    private final ExecutorService uiTaskExecutor;

    /** How often to quietly re-fetch live Radarr/download status while the view is open. */
    private static final int LIVE_POLL_INTERVAL_MS = 30_000;

    private Registration pollRegistration;

    public MovieRequestView(
            MovieRequestRepository movieRequestRepository,
            MovieController movieController,
            ValidationRepository validationRepository,
            NoteRepository noteRepository,
            List<Validator<MovieRequest>> validators,
            PlexClient plexClient,
            DelugeClient delugeClient,
            SabnzbdClient sabnzbdClient,
            NotificationService notificationService,
            @Value("${ombi.url}") String ombiUrl,
            @Value("${radarr.url}") String radarrUrl,
            PlatformTransactionManager transactionManager,
            ExecutorService uiTaskExecutor) {
        this.movieRequestRepository = movieRequestRepository;
        this.movieController = movieController;
        this.validationRepository = validationRepository;
        this.noteRepository = noteRepository;
        this.ombiUrl = ombiUrl;
        this.radarrUrl = radarrUrl;
        this.plexUrl = plexClient.getPlexUrl();
        this.plexMachineIdentifier = plexClient.getMachineIdentifier();
        this.plexClient = plexClient;
        this.delugeClient = delugeClient;
        this.sabnzbdClient = sabnzbdClient;
        this.notificationService = notificationService;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
        this.transactionTemplate.setReadOnly(true);
        this.uiTaskExecutor = uiTaskExecutor;
        this.knownValidatorNames =
                validators.stream().map(v -> v.getClass().getSimpleName()).collect(Collectors.toUnmodifiableSet());
        setSizeFull();

        Grid.Column<MovieRequest> titleColumn = grid.addColumn(MovieRequest::getTitle)
                .setHeader("Title")
                .setFlexGrow(1)
                .setWidth("10em")
                .setSortable(true)
                .setComparator(Comparator.comparing(
                        MovieRequest::getTitle, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)));

        grid.addColumn(RequestViewSupport.linkRenderer(this::radarrHref))
                .setHeader("Radarr")
                .setAutoWidth(true);

        validators.stream()
                .sorted(Comparator.comparingInt(Validator<MovieRequest>::sortOrder))
                .forEach(validator -> {
                    String name = validator.getClass().getSimpleName();
                    grid.addColumn(validatorResultRenderer(name))
                            .setHeader(RequestViewSupport.headerWithTooltip(
                                    validator.shortName(), validator.title(), validator.description()))
                            .setAutoWidth(true)
                            .setSortable(true)
                            .setComparator(Comparator.comparing(
                                    mr -> latestResultValue(mr, name),
                                    Comparator.nullsLast(Comparator.naturalOrder())));
                });

        grid.addColumn(new ComponentRenderer<>(this::typeBadge))
                .setHeader("Type")
                .setAutoWidth(true);
        grid.addColumn(progressRenderer()).setHeader("Progress").setAutoWidth(true);
        grid.addColumn(peersRenderer()).setHeader("Peers").setAutoWidth(true);

        grid.setItemDetailsRenderer(new ComponentRenderer<>(MovieRequestView::createDetails));
        grid.setDetailsVisibleOnClick(true);

        boolean admin = SecurityUtils.isAdmin();
        GridContextMenu<MovieRequest> contextMenu = grid.addContextMenu();
        RequestViewSupport.suppressGridContextMenuOnLinks(grid);
        contextMenu.addItem("Refresh", e -> e.getItem()
                .ifPresent(mr -> runRowAction(mr, "Refreshing…", () -> {
                    movieController.refresh(mr.getId());
                    movieController.validate(mr.getId());
                })));
        contextMenu.addItem("Search", e -> e.getItem()
                .ifPresent(mr -> runRowAction(mr, "Searching…", () -> movieController.searchOne(mr.getId()))));
        GridMenuItem<MovieRequest> deleteDownloadItem = contextMenu.addItem("Delete Download", e -> e.getItem()
                .ifPresent(mr -> runRowAction(
                        mr, "Deleting download…", () -> movieController.deleteDownloadAndSearch(mr.getId()))));
        GridMenuItem<MovieRequest> markAvailableItem = contextMenu.addItem("Mark Available", e -> e.getItem()
                .ifPresent(mr -> runRowAction(mr, "Marking available…", () -> {
                    movieController.markAvailable(mr.getId());
                    movieController.refresh(mr.getId());
                    movieController.validate(mr.getId());
                })));
        GridMenuItem<MovieRequest> qualityProfileItem =
                contextMenu.addItem("Set Quality Profile to 'Any'", e -> e.getItem()
                        .ifPresent(mr -> runRowAction(
                                mr,
                                "Updating quality profile…",
                                () -> movieController.setQualityProfileToAny(mr.getId()))));
        GridMenuItem<MovieRequest> scanFfprobeItem = contextMenu.addItem("Scan with FFprobe", e -> e.getItem()
                .ifPresent(mr -> {
                    movieController.scanWithFfprobe(mr.getId());
                    Notification.show("FFprobe scan queued for \"" + mr.getTitle() + "\".");
                }));
        contextMenu.addItem("Mark as Stale", e -> e.getItem().ifPresent(this::openMarkStaleDialog));
        contextMenu.addItem("Add Note", e -> e.getItem().ifPresent(this::openAddNoteDialog));
        contextMenu.addItem("View Notes", e -> e.getItem().ifPresent(this::openViewNotesDialog));
        contextMenu.addItem("View FFprobe Results", e -> e.getItem().ifPresent(this::openFfprobeResultsDialog));
        contextMenu.addItem("View Plex Query URL", e -> e.getItem().ifPresent(this::openPlexQueryUrlDialog));
        GridMenuItem<MovieRequest> viewOmbiItem =
                contextMenu.addItem("View Ombi", e -> e.getItem().ifPresent(this::openOmbi));
        GridMenuItem<MovieRequest> viewPlexAppItem =
                contextMenu.addItem("View Plex App", e -> e.getItem().ifPresent(this::openPlexApp));
        GridMenuItem<MovieRequest> viewPlexJsonItem =
                contextMenu.addItem("View Plex Json", e -> e.getItem().ifPresent(this::openPlexJson));
        GridMenuItem<MovieRequest> viewTmdbItem =
                contextMenu.addItem("View TMDB", e -> e.getItem().ifPresent(this::openTmdb));
        GridMenuItem<MovieRequest> deleteRequestItem = contextMenu.addItem("Delete Movie Request", e -> e.getItem()
                .ifPresent(mr -> runRowAction(mr, "Deleting request…", () -> movieController.delete(mr.getId()))));
        // USER tier may view, refresh, search, validate, and annotate; mutating Ombi/Radarr or deleting is ADMIN-only.
        deleteDownloadItem.setVisible(admin);
        markAvailableItem.setVisible(admin);
        qualityProfileItem.setVisible(admin);
        scanFfprobeItem.setVisible(admin);
        deleteRequestItem.setVisible(admin);
        contextMenu.setDynamicContentHandler(mr -> {
            if (mr == null) {
                return false;
            }
            markAvailableItem.setEnabled(mr.getOmbiRequestId() != null);
            scanFfprobeItem.setEnabled(
                    mr.getRadarrMovieFilePath() != null && !mr.getRadarrMovieFilePath().isBlank());
            viewOmbiItem.setEnabled(ombiHref(mr) != null);
            viewPlexAppItem.setEnabled(plexAppHref(mr) != null);
            viewPlexJsonItem.setEnabled(plexHref(mr) != null);
            viewTmdbItem.setEnabled(tmdbHref(mr) != null);
            return true;
        });

        grid.setPartNameGenerator(mr -> {
            if (Boolean.TRUE.equals(mr.getStale())) {
                return "stale";
            }
            if (movieRequestsWithNotes.contains(mr.getId()) && !mr.isAvailable()) {
                return "has_notes";
            }
            Map<String, Validation> latestForRow = latestValidations.getOrDefault(mr.getId(), Map.of());
            return mr.isValid(knownValidatorNames, latestForRow) ? "available" : "not_available";
        });

        grid.sort(List.of(new GridSortOrder<>(titleColumn, SortDirection.ASCENDING)));
        grid.setWidthFull();
        grid.setMinHeight("0");

        Button refreshAll = new Button(
                "Refresh All",
                e -> runBulkAction("Refreshing all…", () -> {
                    movieController.refreshAll();
                    movieController.validateAll();
                }));
        Button validateAll =
                new Button("Validate All", e -> runBulkAction("Validating all…", movieController::validateAll));
        Button searchAll = new Button("Search All", e -> runBulkAction("Searching all…", movieController::searchAll));
        Button testNotifications = new Button("Test Notifications", e -> runNotificationCheck());
        testNotifications.setVisible(admin);
        bulkButtons.addAll(List.of(refreshAll, validateAll, searchAll, testNotifications));
        showValidCheckbox.addValueChangeListener(e -> applyFilters());
        showStaleCheckbox.addValueChangeListener(e -> applyFilters());
        showWithNotesCheckbox.addValueChangeListener(e -> applyFilters());
        showValidCheckbox.setLabelComponent(showValidLabel);
        showStaleCheckbox.setLabelComponent(showStaleLabel);
        showWithNotesCheckbox.setLabelComponent(showWithNotesLabel);
        searchField.setPlaceholder("Search by title");
        searchField.setClearButtonVisible(true);
        searchField.setValueChangeMode(ValueChangeMode.LAZY);
        searchField.addValueChangeListener(e -> applyFilters());
        HorizontalLayout statsRow = new HorizontalLayout(radarrQueueCard, radarrHealthCard);
        statsRow.setAlignItems(FlexComponent.Alignment.CENTER);
        statsRow.getStyle().set("flex-wrap", "wrap");
        HorizontalLayout toolbar =
                new HorizontalLayout(searchField, refreshAll, validateAll, searchAll, testNotifications, totalLabel);
        toolbar.setAlignItems(FlexComponent.Alignment.CENTER);
        // Many controls in one row: let them wrap instead of overflowing on narrow screens.
        toolbar.getStyle().set("flex-wrap", "wrap");
        HorizontalLayout filterRow =
                new HorizontalLayout(showValidCheckbox, showStaleCheckbox, showWithNotesCheckbox);
        filterRow.setAlignItems(FlexComponent.Alignment.CENTER);
        filterRow.getStyle().set("flex-wrap", "wrap");

        add(statsRow, toolbar, filterRow, grid, RequestViewSupport.iconsetLoader());
        setFlexGrow(1, grid);
    }

    /**
     * Runs a blocking per-row controller action (Ombi/Radarr/Plex) off the UI thread. On completion it refreshes only
     * the affected row's DB state — instead of re-reading every validation/note/request — and reloads the live
     * Radarr/download status, since the action may have changed the queue.
     */
    private void runRowAction(MovieRequest mr, String workingMessage, Runnable action) {
        getUI().ifPresent(ui -> RequestViewSupport.runAsync(
                ui,
                log,
                workingMessage,
                action,
                () -> {
                    refreshRow(mr.getId());
                    triggerStatsLoad(true);
                },
                uiTaskExecutor));
    }

    private void setBulkButtonsEnabled(boolean enabled) {
        bulkButtons.forEach(b -> b.setEnabled(enabled));
    }

    /**
     * Like {@link #runAction}, but for the toolbar's library-wide buttons: disables all of them for the duration so a
     * long operation can't be double-fired or overlapped with another bulk action, re-enabling them on completion.
     */
    private void runBulkAction(String workingMessage, Runnable action) {
        getUI().ifPresent(ui -> {
            setBulkButtonsEnabled(false);
            RequestViewSupport.runAsync(
                    ui,
                    log,
                    workingMessage,
                    action,
                    () -> {
                        setBulkButtonsEnabled(true);
                        refreshGrid();
                    },
                    uiTaskExecutor);
        });
    }

    /** Runs the notification check off the UI thread and shows its summary toast. */
    private void runNotificationCheck() {
        getUI().ifPresent(ui -> RequestViewSupport.runNotificationCheck(
                ui, log, notificationService, uiTaskExecutor, this::setBulkButtonsEnabled));
    }

    /**
     * Reloads the grid's data off the UI thread and, when it lands, refreshes the stat cards. The DB reads run inside a
     * read-only transaction on the worker thread because {@link Validation#getRequest()} / {@link Note#getRequest()}
     * are lazy and would otherwise fail outside the request-bound session. A no-op until the view is attached (a UI is
     * needed for {@link UI#access}); the initial load is kicked off from {@link #onAttach}.
     */
    private void refreshGrid() {
        getUI().ifPresent(this::loadGridAsync);
    }

    private void loadGridAsync(UI ui) {
        if (!gridLoadInFlight.compareAndSet(false, true)) {
            return;
        }
        CompletableFuture.supplyAsync(() -> transactionTemplate.execute(status -> buildSnapshot()), uiTaskExecutor)
                .whenComplete((snapshot, throwable) -> ui.access(() -> {
                    try {
                        if (throwable != null) {
                            log.warn("Failed to refresh movie grid", throwable);
                            Notification.show("Failed to load movies; see the server log.");
                        } else if (snapshot != null) {
                            applySnapshot(snapshot);
                        }
                    } finally {
                        gridLoadInFlight.set(false);
                        triggerStatsLoad(true);
                    }
                }));
    }

    private record GridSnapshot(
            Map<Long, Map<String, Validation>> latestValidations, Set<Long> withNotes, List<MovieRequest> all) {}

    /** Reads validations, notes, and requests and builds the row indexes. Runs inside a read-only transaction. */
    private GridSnapshot buildSnapshot() {
        // Load the concrete MovieRequests up front so they are managed in the persistence context before we navigate
        // Validation#getRequest()/Note#getRequest() (declared as the base Request). Otherwise Hibernate creates
        // base-Request proxies for those ids first, then narrows each to MovieRequest, logging
        // "Narrowing proxy to ... MovieRequest - this operation breaks ==" once per movie on every refresh.
        List<MovieRequest> all = movieRequestRepository.findAll();
        Map<Long, Map<String, Validation>> latest = new HashMap<>();
        for (Validation v : validationRepository.findAll()) {
            if (!knownValidatorNames.contains(v.getValidationName())) {
                continue;
            }
            Long movieRequestId = v.getRequest().getId();
            latest.computeIfAbsent(movieRequestId, k -> new HashMap<>())
                    .merge(v.getValidationName(), v, (a, b) -> a.getCreatedAt().isAfter(b.getCreatedAt()) ? a : b);
        }
        Set<Long> withNotes = new HashSet<>();
        noteRepository.findAll().forEach(n -> withNotes.add(n.getRequest().getId()));
        return new GridSnapshot(latest, withNotes, all);
    }

    /** Applies a freshly loaded snapshot to the view state (on the UI thread) and re-runs the active filters. */
    private void applySnapshot(GridSnapshot snapshot) {
        latestValidations.clear();
        latestValidations.putAll(snapshot.latestValidations());
        movieRequestsWithNotes.clear();
        movieRequestsWithNotes.addAll(snapshot.withNotes());
        allRequests = snapshot.all();
        updateCountLabels();
        applyFilters();
    }

    /** Recomputes the toolbar count labels from the in-memory row state (no DB read). */
    private void updateCountLabels() {
        long valid = allRequests.stream()
                .filter(mr -> mr.isValid(knownValidatorNames, latestValidations.getOrDefault(mr.getId(), Map.of())))
                .count();
        long stale = allRequests.stream()
                .filter(mr -> Boolean.TRUE.equals(mr.getStale()))
                .count();
        long withNotes = allRequests.stream()
                .filter(mr -> movieRequestsWithNotes.contains(mr.getId()))
                .count();
        showValidLabel.setText("Show valid rows (" + valid + ")");
        showStaleLabel.setText("Show stale rows (" + stale + ")");
        showWithNotesLabel.setText("Show rows with notes (" + withNotes + ")");
        totalLabel.setText("Total movies: " + allRequests.size());
    }

    private record RowSnapshot(MovieRequest request, Map<String, Validation> validations, boolean hasNotes) {}

    /** Refreshes a single row's DB state (validations, notes, entity) off the UI thread — see {@link #runRowAction}. */
    private void refreshRow(Long id) {
        getUI().ifPresent(ui -> CompletableFuture.supplyAsync(
                        () -> transactionTemplate.execute(status -> buildRowSnapshot(id)), uiTaskExecutor)
                .whenComplete((row, throwable) -> ui.access(() -> {
                    if (throwable != null) {
                        log.warn("Failed to refresh movie row {}", id, throwable);
                        Notification.show("Failed to refresh row; see the server log.");
                    } else if (row != null) {
                        applyRowSnapshot(id, row);
                    }
                })));
    }

    /** Reads one request's validations and notes. Runs inside a read-only transaction; null request means deleted. */
    private RowSnapshot buildRowSnapshot(Long id) {
        MovieRequest mr = movieRequestRepository.findById(id).orElse(null);
        if (mr == null) {
            return new RowSnapshot(null, Map.of(), false);
        }
        Map<String, Validation> byName = new HashMap<>();
        for (Validation v : validationRepository.findByRequest(mr)) {
            if (knownValidatorNames.contains(v.getValidationName())) {
                byName.merge(
                        v.getValidationName(), v, (a, b) -> a.getCreatedAt().isAfter(b.getCreatedAt()) ? a : b);
            }
        }
        boolean hasNotes = !noteRepository.findByRequestOrderByCreatedAtDesc(mr).isEmpty();
        return new RowSnapshot(mr, byName, hasNotes);
    }

    /** Merges a single row's refreshed state into the view (on the UI thread) and re-runs the active filters. */
    private void applyRowSnapshot(Long id, RowSnapshot row) {
        List<MovieRequest> updated = new ArrayList<>(allRequests);
        updated.removeIf(r -> id.equals(r.getId()));
        if (row.request() == null) {
            latestValidations.remove(id);
            movieRequestsWithNotes.remove(id);
        } else {
            updated.add(row.request());
            latestValidations.put(id, row.validations());
            if (row.hasNotes()) {
                movieRequestsWithNotes.add(id);
            } else {
                movieRequestsWithNotes.remove(id);
            }
        }
        allRequests = updated;
        updateCountLabels();
        applyFilters();
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        UI ui = attachEvent.getUI();
        ui.setPollInterval(LIVE_POLL_INTERVAL_MS);
        // Quietly refresh just the live Radarr/download status on each poll (not the whole grid) so progress, peers,
        // and the queue card stay current without a manual reload.
        pollRegistration = ui.addPollListener(e -> triggerStatsLoad(false));
        refreshGrid();
    }

    @Override
    protected void onDetach(DetachEvent detachEvent) {
        if (pollRegistration != null) {
            pollRegistration.remove();
            pollRegistration = null;
        }
        detachEvent.getUI().setPollInterval(-1);
        super.onDetach(detachEvent);
    }

    /**
     * Kicks off the Radarr queue + health fetch on the current UI, if the view is attached; a no-op otherwise. When
     * {@code showLoading} is set the stat cards show a spinner first (initial/explicit loads); the background poll
     * passes {@code false} so the cards update in place without flashing.
     */
    private void triggerStatsLoad(boolean showLoading) {
        getUI().ifPresent(ui -> loadStatsAsync(ui, showLoading));
    }

    /**
     * Fetches the Radarr queue and health off the UI thread (both hit remote, VPN-fronted Radarr and can be slow) so
     * the page's initial render isn't blocked; the stat cards show a spinner until the results arrive via
     * {@link UI#access}. The download-status load is chained afterwards because it joins torrents/slots to the queue
     * maps populated here. A guard skips the fetch when one is already in flight.
     */
    private void loadStatsAsync(UI ui, boolean showLoading) {
        if (!statsLoadInFlight.compareAndSet(false, true)) {
            return;
        }
        if (showLoading) {
            RequestViewSupport.showCardLoading(radarrQueueValue);
            RequestViewSupport.showCardLoading(radarrHealthValue);
        }
        CompletableFuture<RadarrQueue> queue =
                CompletableFuture.supplyAsync(movieController::getRadarrQueue, uiTaskExecutor);
        CompletableFuture<List<RadarrHealthItem>> health =
                CompletableFuture.supplyAsync(movieController::getRadarrHealth, uiTaskExecutor);
        queue.thenCombine(health, StatsResult::new)
                .whenComplete((stats, throwable) -> ui.access(() -> {
                    try {
                        if (throwable != null) {
                            log.warn("Failed to load Radarr stats", throwable);
                        }
                        RadarrQueue radarrQueue = throwable == null && stats != null ? stats.queue() : null;
                        List<RadarrHealthItem> radarrHealth =
                                throwable == null && stats != null ? stats.health() : null;
                        updateRadarrQueueCard(radarrQueue);
                        updateQueueMaps(radarrQueue);
                        updateRadarrHealthCard(radarrHealth);
                    } finally {
                        statsLoadInFlight.set(false);
                        triggerDownloadLoad(showLoading);
                    }
                }));
    }

    private record StatsResult(RadarrQueue queue, List<RadarrHealthItem> health) {}

    /** Kicks off the Deluge + SABnzbd fetch on the current UI, if the view is attached; a no-op otherwise. */
    private void triggerDownloadLoad(boolean showLoading) {
        getUI().ifPresent(ui -> loadDownloadStatusAsync(ui, showLoading));
    }

    /**
     * Fetches Deluge torrent and SABnzbd queue status off the UI thread (both hit remote, VPN-fronted services and can
     * be slow). While the fetch is in flight the Status column shows a per-row spinner for queued movies; results are
     * pushed back via {@link UI#access}. Requires server push (see {@code @Push}). A guard skips the fetch when one is
     * already in flight.
     */
    private void loadDownloadStatusAsync(UI ui, boolean showLoading) {
        if (!downloadLoadInFlight.compareAndSet(false, true)) {
            return;
        }
        // Render the per-row spinners now that the in-flight guard is set. Skipped for the background poll so rows
        // don't flash a spinner every interval — they just update in place when the new status lands.
        if (showLoading) {
            grid.getDataProvider().refreshAll();
        }
        CompletableFuture<Map<String, DelugeTorrent>> torrents =
                CompletableFuture.supplyAsync(delugeClient::getTorrentsStatus, uiTaskExecutor);
        CompletableFuture<Map<String, SabnzbdSlot>> slots =
                CompletableFuture.supplyAsync(sabnzbdClient::getQueueSlots, uiTaskExecutor);
        torrents.thenCombine(slots, DownloadStatus::new)
                .whenComplete((status, throwable) -> ui.access(() -> {
                    try {
                        if (throwable != null) {
                            log.warn("Failed to load download status", throwable);
                        } else {
                            applyDownloadStatus(status.torrents(), status.slots());
                        }
                    } finally {
                        downloadLoadInFlight.set(false);
                        grid.getDataProvider().refreshAll();
                        // The Status column is auto-width, but its detail text only arrives now (async), after
                        // the initial width was measured from the shorter spinner content — recompute so it fits.
                        grid.recalculateColumnWidths();
                    }
                }));
    }

    private record DownloadStatus(Map<String, DelugeTorrent> torrents, Map<String, SabnzbdSlot> slots) {}

    /**
     * Joins Deluge torrents and SABnzbd slots to movies through the Radarr queue: a queue record's downloadId is the
     * Deluge torrent hash (matched case-insensitively) for torrents or the SABnzbd {@code nzo_id} for usenet, and the
     * record's protocol picks which client to look in. Rebuilds the movie-to-download indexes from scratch; the caller
     * re-renders so the Status column updates.
     */
    private void applyDownloadStatus(Map<String, DelugeTorrent> torrents, Map<String, SabnzbdSlot> slots) {
        torrentByMovieId.clear();
        slotByMovieId.clear();
        Map<String, DelugeTorrent> byLowerHash = new HashMap<>();
        if (torrents != null) {
            torrents.forEach((hash, torrent) -> byLowerHash.put(hash.toLowerCase(), torrent));
        }
        Map<String, SabnzbdSlot> byNzoId = slots == null ? Map.of() : slots;
        downloadIdByMovieId.forEach((movieId, downloadId) -> {
            if (downloadId == null) {
                return;
            }
            if (RequestViewSupport.isTorrent(protocolByMovieId.get(movieId))) {
                DelugeTorrent torrent = byLowerHash.get(downloadId.toLowerCase());
                if (torrent != null) {
                    torrentByMovieId.put(movieId, torrent);
                }
            } else {
                SabnzbdSlot slot = byNzoId.get(downloadId);
                if (slot != null) {
                    slotByMovieId.put(movieId, slot);
                }
            }
        });
    }

    /**
     * Applies the toolbar filters to the already-loaded {@link #allRequests} without re-querying. A non-blank search
     * matches rows by title and ignores the show/stale/notes toggles, so matching rows surface even when those filters
     * would otherwise hide them.
     */
    private void applyFilters() {
        String term =
                searchField.getValue() == null ? "" : searchField.getValue().trim();
        if (!term.isBlank()) {
            String lower = term.toLowerCase();
            grid.setItems(allRequests.stream()
                    .filter(mr ->
                            mr.getTitle() != null && mr.getTitle().toLowerCase().contains(lower))
                    .toList());
            return;
        }

        boolean showValid = Boolean.TRUE.equals(showValidCheckbox.getValue());
        boolean showStale = Boolean.TRUE.equals(showStaleCheckbox.getValue());
        boolean showWithNotes = Boolean.TRUE.equals(showWithNotesCheckbox.getValue());
        grid.setItems(allRequests.stream()
                .filter(mr -> showStale || !Boolean.TRUE.equals(mr.getStale()))
                .filter(mr -> showWithNotes || !movieRequestsWithNotes.contains(mr.getId()))
                .filter(mr -> showValid
                        || !mr.isValid(knownValidatorNames, latestValidations.getOrDefault(mr.getId(), Map.of())))
                .toList());
    }

    private static final List<String> CANNED_STALE_REASONS = List.of(
            "No search returned from Radarr",
            "No tmdbid",
            "Not in English",
            "Movie misfiled in TV folder",
            "Repeated download attempts without success");

    private void openMarkStaleDialog(MovieRequest mr) {
        RequestViewSupport.openTextEntryDialog(
                "Mark \"" + mr.getTitle() + "\" as stale",
                "Reason",
                mr.getStaleReason(),
                CANNED_STALE_REASONS,
                false,
                reason -> {
                    movieController.markStale(mr.getId(), reason);
                    refreshRow(mr.getId());
                });
    }

    private void openAddNoteDialog(MovieRequest mr) {
        RequestViewSupport.openTextEntryDialog(
                "Add note to \"" + mr.getTitle() + "\"", "Note", null, List.of(), true, note -> {
                    movieController.addNote(mr.getId(), note);
                    refreshRow(mr.getId());
                });
    }

    private void openViewNotesDialog(MovieRequest mr) {
        RequestViewSupport.openNotesDialog(mr.getTitle(), noteRepository.findByRequestOrderByCreatedAtDesc(mr));
    }

    /**
     * Displays the most recent ffprobe scan for the movie. The scan's streams are eagerly fetched by the repository,
     * so this read is safe to run on the UI thread; rendering is shared with the TV view via {@link RequestViewSupport}.
     */
    private void openFfprobeResultsDialog(MovieRequest mr) {
        RequestViewSupport.openFfprobeResultsDialog(
                "FFprobe results for \"" + mr.getTitle() + "\"",
                movieController.getLatestFfprobeScan(mr.getId()).orElse(null));
    }

    private void openPlexQueryUrlDialog(MovieRequest mr) {
        String url = plexClient.movieQueryUrl(mr.getTitle());
        RequestViewSupport.openTextDialog(
                "Plex query URL for \"" + mr.getTitle() + "\"", url == null ? "Plex query URL unavailable" : url);
    }

    /** Opens the row's Ombi details page in a new browser tab. */
    private void openOmbi(MovieRequest mr) {
        String url = ombiHref(mr);
        if (url != null) {
            getUI().ifPresent(ui -> ui.getPage().open(url));
        }
    }

    /** Opens the row's Plex app deep link in a new browser tab. */
    private void openPlexApp(MovieRequest mr) {
        String url = plexAppHref(mr);
        if (url != null) {
            getUI().ifPresent(ui -> ui.getPage().open(url));
        }
    }

    /** Opens the row's Plex metadata JSON URL in a new browser tab. */
    private void openPlexJson(MovieRequest mr) {
        String url = plexHref(mr);
        if (url != null) {
            getUI().ifPresent(ui -> ui.getPage().open(url));
        }
    }

    /** Opens the row's TMDB page in a new browser tab. */
    private void openTmdb(MovieRequest mr) {
        String url = tmdbHref(mr);
        if (url != null) {
            getUI().ifPresent(ui -> ui.getPage().open(url));
        }
    }

    private String ombiHref(MovieRequest mr) {
        Integer tmdbid = mr.getTmdbid();
        return tmdbid == null ? null : ombiUrl + "/details/movie/" + tmdbid;
    }

    private String radarrHref(MovieRequest mr) {
        Integer tmdbid = mr.getTmdbid();
        return tmdbid == null ? null : radarrUrl + "/movie/" + tmdbid;
    }

    private static String plexHref(MovieRequest mr) {
        String url = mr.getPlexMetadataUrl();
        return url == null || url.isBlank() ? null : url;
    }

    private String plexAppHref(MovieRequest mr) {
        String ratingKey = mr.getPlexMetadataId();
        if (ratingKey == null || ratingKey.isBlank() || plexMachineIdentifier == null) {
            return null;
        }
        return plexUrl + "/web/index.html#!/server/" + plexMachineIdentifier + "/details?key=/library/metadata/"
                + ratingKey;
    }

    private static String tmdbHref(MovieRequest mr) {
        Integer tmdbid = mr.getTmdbid();
        return tmdbid == null ? null : "https://www.themoviedb.org/movie/" + tmdbid;
    }

    /** Updates the queue card's number, per-state breakdown tooltip, and severity background. */
    private void updateRadarrQueueCard(RadarrQueue queue) {
        if (queue == null) {
            RequestViewSupport.updateQueueCard(radarrQueueCard, radarrQueueValue, radarrQueueTooltip, null, null);
            return;
        }
        List<RadarrQueueRecord> records = queue.getRecords() == null ? List.of() : queue.getRecords();
        Map<String, Long> byState = records.stream()
                .map(RadarrQueueRecord::getTrackedDownloadState)
                .filter(state -> state != null)
                .collect(Collectors.groupingBy(state -> state, Collectors.counting()));
        RequestViewSupport.updateQueueCard(
                radarrQueueCard, radarrQueueValue, radarrQueueTooltip, queue.getTotalRecords(), byState);
    }

    /**
     * Indexes the current queue by Radarr movie id: the download state drives the Status icon, and the download id
     * links a movie to its Deluge torrent (downloadId == Deluge torrent hash).
     */
    private void updateQueueMaps(RadarrQueue queue) {
        queueStateByMovieId.clear();
        downloadIdByMovieId.clear();
        protocolByMovieId.clear();
        if (queue == null || queue.getRecords() == null) {
            return;
        }
        for (RadarrQueueRecord record : queue.getRecords()) {
            if (record.getMovieId() != null) {
                queueStateByMovieId.put(record.getMovieId(), record.getTrackedDownloadState());
                downloadIdByMovieId.put(record.getMovieId(), record.getDownloadId());
                protocolByMovieId.put(record.getMovieId(), record.getProtocol());
            }
        }
    }

    /** Sets the health count and a tooltip listing each reported issue. */
    private void updateRadarrHealthCard(List<RadarrHealthItem> health) {
        RequestViewSupport.updateHealthCard(
                radarrHealthCard,
                radarrHealthValue,
                radarrHealthTooltip,
                health,
                RadarrHealthItem::getType,
                RadarrHealthItem::getMessage);
    }

    /**
     * Type cell: a Vaadin {@link Badge} showing the raw Radarr protocol text — solid blue (the badge's default accent,
     * {@code FILLED}) for torrent, solid green ({@code SUCCESS FILLED}) for usenet — or a dash when the movie isn't in
     * the Radarr queue. The protocol comes straight from the queue, so this needs no loading state.
     */
    private com.vaadin.flow.component.Component typeBadge(MovieRequest mr) {
        String protocol = protocolByMovieId.get(mr.getRadarrRequestId());
        if (protocol == null) {
            return new Span("—");
        }
        Badge badge = new Badge(protocol);
        if (RequestViewSupport.isTorrent(protocol)) {
            badge.addThemeVariants(BadgeVariant.FILLED);
        } else {
            badge.addThemeVariants(BadgeVariant.SUCCESS, BadgeVariant.FILLED);
        }
        return badge;
    }

    /**
     * Client-side renderer for the Progress cell: a spinner while the download fetch is in flight for a queued movie,
     * then the progress (torrent progress or SABnzbd percentage), or a dash.
     */
    private LitRenderer<MovieRequest> progressRenderer() {
        return LitRenderer.<MovieRequest>of(RequestViewSupport.progressCellTemplate(true))
                .withProperty("loading", this::isStatusLoading)
                .withProperty("pct", this::progressPercent)
                .withProperty("label", this::progressText);
    }

    /**
     * Client-side renderer for the Peers cell: a spinner while a torrent's status loads, then the peer/seed counts.
     * Non-torrent (usenet) downloads have no peers, so they show a dash.
     */
    private LitRenderer<MovieRequest> peersRenderer() {
        return LitRenderer.<MovieRequest>of("<span class=\"status-spinner\" role=\"status\" aria-label=\"Loading\""
                        + " ?hidden=\"${!item.loading}\"></span>"
                        + "<span ?hidden=\"${item.loading}\">${item.peers}</span>")
                .withProperty("loading", this::isPeersLoading)
                .withProperty("peers", this::peersText);
    }

    /** Numeric download percentage (0–100) for the Progress bar, or -1 when there's nothing to show. */
    private double progressPercent(MovieRequest mr) {
        Integer movieId = mr.getRadarrRequestId();
        return RequestViewSupport.progressPercentOf(
                protocolByMovieId.get(movieId), torrentByMovieId.get(movieId), slotByMovieId.get(movieId));
    }

    /** Loading for the Progress cell: any queued movie whose download status is still in flight. */
    private boolean isStatusLoading(MovieRequest mr) {
        return downloadLoadInFlight.get() && protocolByMovieId.get(mr.getRadarrRequestId()) != null;
    }

    /** Loading for the Peers cell: only torrents, the one protocol with peer counts. */
    private boolean isPeersLoading(MovieRequest mr) {
        return downloadLoadInFlight.get()
                && RequestViewSupport.isTorrent(protocolByMovieId.get(mr.getRadarrRequestId()));
    }

    /** Download progress for a queued movie: torrent progress or SABnzbd percentage, else a dash. */
    private String progressText(MovieRequest mr) {
        Integer movieId = mr.getRadarrRequestId();
        String protocol = protocolByMovieId.get(movieId);
        if (protocol == null) {
            return "—";
        }
        if (RequestViewSupport.isTorrent(protocol)) {
            DelugeTorrent torrent = torrentByMovieId.get(movieId);
            return torrent == null ? "—" : RequestViewSupport.formatProgress(torrent.getProgress());
        }
        SabnzbdSlot slot = slotByMovieId.get(movieId);
        return slot == null ? "—" : RequestViewSupport.formatPercentage(slot.getPercentage());
    }

    /** Peer/seed counts for a torrent, or a dash for usenet or movies not in the queue. */
    private String peersText(MovieRequest mr) {
        Integer movieId = mr.getRadarrRequestId();
        if (!RequestViewSupport.isTorrent(protocolByMovieId.get(movieId))) {
            return "—";
        }
        DelugeTorrent torrent = torrentByMovieId.get(movieId);
        return torrent == null ? "—" : RequestViewSupport.formatPeers(torrent);
    }

    /**
     * Client-side renderer for a validator result cell. Using {@link LitRenderer} instead of a server-side component
     * column keeps the grid responsive while scrolling: the browser renders the icon from a tiny data payload rather
     * than the server building a component per cell.
     */
    private LitRenderer<MovieRequest> validatorResultRenderer(String validationName) {
        return LitRenderer.<MovieRequest>of(
                        "<vaadin-icon icon=\"${item.icon}\" style=\"color: ${item.color}\"></vaadin-icon>")
                .withProperty("icon", mr -> RequestViewSupport.resultIconName(latestResultValue(mr, validationName)))
                .withProperty("color", mr -> RequestViewSupport.resultIconColor(latestResultValue(mr, validationName)));
    }

    private Boolean latestResultValue(MovieRequest mr, String validationName) {
        return RequestViewSupport.latestResultValue(latestValidations, mr.getId(), validationName);
    }

    private static final List<String> DETAIL_PRIORITY_FIELDS = List.of("id", "title", "tmdbid");

    private static FormLayout createDetails(MovieRequest mr) {
        return RequestViewSupport.fieldDump(MovieRequest.class, mr, DETAIL_PRIORITY_FIELDS);
    }
}
