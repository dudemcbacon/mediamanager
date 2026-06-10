package report.butt.mediamanager.route;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.UI;
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
import report.butt.mediamanager.controller.TvController;
import report.butt.mediamanager.model.Note;
import report.butt.mediamanager.model.TvChildRequest;
import report.butt.mediamanager.model.TvEpisodeRequest;
import report.butt.mediamanager.model.TvRequest;
import report.butt.mediamanager.model.Validation;
import report.butt.mediamanager.model.deluge.DelugeTorrent;
import report.butt.mediamanager.model.plex.EpisodeKey;
import report.butt.mediamanager.model.sabnzbd.SabnzbdSlot;
import report.butt.mediamanager.model.sonarr.SonarrHealthItem;
import report.butt.mediamanager.model.sonarr.SonarrQueue;
import report.butt.mediamanager.model.sonarr.SonarrQueueRecord;
import report.butt.mediamanager.repository.NoteRepository;
import report.butt.mediamanager.repository.TvRequestRepository;
import report.butt.mediamanager.repository.ValidationRepository;
import report.butt.mediamanager.security.SecurityUtils;
import report.butt.mediamanager.service.NotificationService;
import report.butt.mediamanager.service.TvHierarchyService;
import report.butt.mediamanager.validation.EpisodeValidator;
import report.butt.mediamanager.validation.Validator;

@Route("tv")
@PageTitle("TV")
@PermitAll
@Component
@UIScope
@StyleSheet("grid-available.css")
public class TvRequestView extends VerticalLayout {

    private static final Logger log = LoggerFactory.getLogger(TvRequestView.class);

    /** Identifies an episode within the Sonarr queue across all shows. */
    private record QueueKey(Integer seriesId, Integer seasonNumber, Integer episodeNumber) {}

    private final Grid<TvRequest> grid = new Grid<>(TvRequest.class, false);
    private final TvRequestRepository tvRequestRepository;
    private final TvController tvController;
    private final ValidationRepository validationRepository;
    private final NoteRepository noteRepository;
    private final TvHierarchyService tvHierarchyService;
    private final Set<String> knownValidatorNames;
    private final List<EpisodeValidator> episodeValidators;
    private final Set<String> knownEpisodeValidatorNames;
    private final Map<Long, Map<String, Validation>> latestValidations = new HashMap<>();
    private final Map<Long, Map<String, Validation>> latestEpisodeValidations = new HashMap<>();
    private final Map<Long, Boolean> subValidations = new HashMap<>();
    private final Set<Long> tvRequestsWithNotes = new HashSet<>();
    private final Checkbox showValidCheckbox = new Checkbox(true);
    private final Checkbox showStaleCheckbox = new Checkbox(false);
    private final Checkbox showWithNotesCheckbox = new Checkbox(true);
    private final Span showValidLabel = RequestViewSupport.coloredLabel("Show valid rows", "var(--aura-green-text)");
    private final Span showStaleLabel = RequestViewSupport.coloredLabel("Show stale rows", "var(--aura-yellow-text)");
    private final Span showWithNotesLabel =
            RequestViewSupport.coloredLabel("Show rows with notes", "var(--aura-blue-text)");
    private final Span totalLabel = RequestViewSupport.coloredLabel("Total TV shows", "var(--vaadin-text-color)");
    private final Span sonarrQueueValue = new Span("—");
    private final Card sonarrQueueCard = RequestViewSupport.statCard("Sonarr Queue", sonarrQueueValue);
    private final Tooltip sonarrQueueTooltip = Tooltip.forComponent(sonarrQueueCard);
    private final Span sonarrHealthValue = new Span("—");
    private final Card sonarrHealthCard = RequestViewSupport.statCard("Health Issues", sonarrHealthValue);
    private final Tooltip sonarrHealthTooltip = Tooltip.forComponent(sonarrHealthCard);
    private final TextField searchField = new TextField();
    private final List<Button> bulkButtons = new ArrayList<>();
    private List<TvRequest> allRequests = List.of();
    private final String ombiUrl;
    private final String sonarrUrl;
    private final String plexUrl;
    private final String plexMachineIdentifier;
    private final PlexClient plexClient;
    private final DelugeClient delugeClient;
    private final SabnzbdClient sabnzbdClient;
    private final NotificationService notificationService;
    private final TransactionTemplate transactionTemplate;
    private final Map<QueueKey, String> protocolByEpisode = new HashMap<>();
    private final Map<QueueKey, String> downloadIdByEpisode = new HashMap<>();
    private final Map<QueueKey, DelugeTorrent> torrentByEpisode = new HashMap<>();
    private final Map<QueueKey, SabnzbdSlot> slotByEpisode = new HashMap<>();
    private final Map<Integer, Set<String>> protocolsBySeriesId = new HashMap<>();
    private final AtomicBoolean downloadLoadInFlight = new AtomicBoolean(false);
    private final AtomicBoolean statsLoadInFlight = new AtomicBoolean(false);
    private final AtomicBoolean gridLoadInFlight = new AtomicBoolean(false);
    private final ExecutorService uiTaskExecutor;

    /** How often to quietly re-fetch live Sonarr/download status while the view is open. */
    private static final int LIVE_POLL_INTERVAL_MS = 30_000;

    private Registration pollRegistration;

    public TvRequestView(
            TvRequestRepository tvRequestRepository,
            TvController tvController,
            ValidationRepository validationRepository,
            NoteRepository noteRepository,
            List<Validator<TvRequest>> validators,
            List<EpisodeValidator> episodeValidators,
            PlexClient plexClient,
            DelugeClient delugeClient,
            SabnzbdClient sabnzbdClient,
            NotificationService notificationService,
            @Value("${ombi.url}") String ombiUrl,
            @Value("${sonarr.url}") String sonarrUrl,
            TvHierarchyService tvHierarchyService,
            PlatformTransactionManager transactionManager,
            ExecutorService uiTaskExecutor) {
        this.tvRequestRepository = tvRequestRepository;
        this.tvController = tvController;
        this.validationRepository = validationRepository;
        this.noteRepository = noteRepository;
        this.tvHierarchyService = tvHierarchyService;
        this.ombiUrl = ombiUrl;
        this.sonarrUrl = sonarrUrl;
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
        this.episodeValidators = episodeValidators.stream()
                .sorted(Comparator.comparingInt(EpisodeValidator::sortOrder))
                .toList();
        this.knownEpisodeValidatorNames = this.episodeValidators.stream()
                .map(v -> v.getClass().getSimpleName())
                .collect(Collectors.toUnmodifiableSet());
        setSizeFull();

        Grid.Column<TvRequest> titleColumn = grid.addColumn(TvRequest::getTitle)
                .setHeader("Title")
                .setFlexGrow(1)
                .setWidth("10em")
                .setSortable(true)
                .setComparator(
                        Comparator.comparing(TvRequest::getTitle, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)));

        grid.addColumn(RequestViewSupport.linkRenderer(this::sonarrHref))
                .setHeader("Sonarr")
                .setAutoWidth(true);

        grid.addColumn(TvRequestView::episodesAvailable)
                .setHeader("Episodes")
                .setAutoWidth(true)
                .setSortable(true)
                .setComparator(Comparator.comparingDouble(TvRequestView::episodeAvailabilityRatio));

        validators.stream()
                .sorted(Comparator.comparingInt(Validator<TvRequest>::sortOrder))
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

        grid.addColumn(subValidationRenderer())
                .setHeader("Sub")
                .setAutoWidth(true)
                .setSortable(true)
                .setComparator(Comparator.comparing(
                        mr -> subValidations.get(mr.getId()), Comparator.nullsLast(Comparator.naturalOrder())));

        grid.addColumn(qualityBadgeRenderer())
                .setHeader("Quality Profile")
                .setAutoWidth(true)
                .setSortable(true)
                .setComparator(Comparator.comparing(
                        TvRequest::getSonarrQualityProfile, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)));

        grid.addColumn(new ComponentRenderer<>(this::statusBadges))
                .setHeader("Status")
                .setAutoWidth(true);

        grid.setItemDetailsRenderer(new ComponentRenderer<>(this::createDetails));
        grid.setDetailsVisibleOnClick(true);

        boolean admin = SecurityUtils.isAdmin();
        GridContextMenu<TvRequest> contextMenu = grid.addContextMenu();
        RequestViewSupport.suppressGridContextMenuOnLinks(grid);
        contextMenu.addItem("Refresh", e -> e.getItem()
                .ifPresent(mr -> runRowAction(mr, "Refreshing…", () -> {
                    tvController.refresh(mr.getId());
                    tvController.validate(mr.getId());
                })));
        contextMenu.addItem("Search", e -> e.getItem()
                .ifPresent(mr -> runRowAction(mr, "Searching…", () -> tvController.searchOne(mr.getId()))));
        contextMenu.addItem("Search All Seasons", e -> e.getItem()
                .ifPresent(mr -> runRowAction(
                        mr, "Searching all seasons…", () -> tvController.searchAllSeasonsForRequest(mr.getId()))));
        contextMenu.addItem("Search All Episodes", e -> e.getItem()
                .ifPresent(mr -> runRowAction(
                        mr, "Searching all episodes…", () -> tvController.searchAllEpisodesForRequest(mr.getId()))));
        GridMenuItem<TvRequest> markAvailableItem = contextMenu.addItem("Mark Available", e -> e.getItem()
                .ifPresent(mr -> runRowAction(mr, "Marking available…", () -> {
                    tvController.markAvailable(mr.getId());
                    tvController.refresh(mr.getId());
                    tvController.validate(mr.getId());
                })));
        GridMenuItem<TvRequest> qualityProfileItem =
                contextMenu.addItem("Set Quality Profile to 'Any'", e -> e.getItem()
                        .ifPresent(mr -> runRowAction(
                                mr,
                                "Updating quality profile…",
                                () -> tvController.setQualityProfileToAny(mr.getId()))));
        contextMenu.addItem("Mark as Stale", e -> e.getItem().ifPresent(this::openMarkStaleDialog));
        contextMenu.addItem("Add Note", e -> e.getItem().ifPresent(this::openAddNoteDialog));
        contextMenu.addItem("View Notes", e -> e.getItem().ifPresent(this::openViewNotesDialog));
        contextMenu.addItem("View Plex Query URL", e -> e.getItem().ifPresent(this::openPlexQueryUrlDialog));
        GridMenuItem<TvRequest> viewOmbiItem =
                contextMenu.addItem("View Ombi", e -> e.getItem().ifPresent(this::openOmbi));
        GridMenuItem<TvRequest> viewPlexAppItem =
                contextMenu.addItem("View Plex App", e -> e.getItem().ifPresent(this::openPlexApp));
        GridMenuItem<TvRequest> viewPlexJsonItem =
                contextMenu.addItem("View Plex JSON", e -> e.getItem().ifPresent(this::openPlexJson));
        GridMenuItem<TvRequest> viewTvdbItem =
                contextMenu.addItem("View TVDB", e -> e.getItem().ifPresent(this::openTvdb));
        GridMenuItem<TvRequest> deleteRequestItem = contextMenu.addItem("Delete TV Request", e -> e.getItem()
                .ifPresent(mr -> runRowAction(mr, "Deleting request…", () -> tvController.delete(mr.getId()))));
        // USER tier may view, refresh, search, validate, and annotate; mutating Ombi/Sonarr or deleting is ADMIN-only.
        markAvailableItem.setVisible(admin);
        qualityProfileItem.setVisible(admin);
        deleteRequestItem.setVisible(admin);
        contextMenu.setDynamicContentHandler(mr -> {
            if (mr == null) {
                return false;
            }
            markAvailableItem.setEnabled(mr.getOmbiRequestId() != null);
            viewOmbiItem.setEnabled(ombiHref(mr) != null);
            viewPlexAppItem.setEnabled(plexAppHref(mr) != null);
            viewPlexJsonItem.setEnabled(plexHref(mr) != null);
            viewTvdbItem.setEnabled(tvdbHref(mr) != null);
            return true;
        });

        grid.setPartNameGenerator(mr -> {
            if (Boolean.TRUE.equals(mr.getStale())) {
                return "stale";
            }
            if (tvRequestsWithNotes.contains(mr.getId()) && !mr.isAvailable()) {
                return "has_notes";
            }
            Map<String, Validation> latestForRow = latestValidations.getOrDefault(mr.getId(), Map.of());
            boolean valid = mr.isValid(knownValidatorNames, latestForRow)
                    && Boolean.TRUE.equals(subValidations.get(mr.getId()));
            return valid ? "available" : "not_available";
        });

        grid.sort(List.of(new GridSortOrder<>(titleColumn, SortDirection.ASCENDING)));
        grid.setWidthFull();
        grid.setMinHeight("0");

        Button refreshAll = new Button(
                "Refresh All",
                e -> runBulkAction("Refreshing all…", () -> {
                    tvController.refreshAll();
                    tvController.validateAll();
                }));
        Button validateAll =
                new Button("Validate All", e -> runBulkAction("Validating all…", tvController::validateAll));
        Button searchAllSeries = new Button(
                "Search All Series", e -> runBulkAction("Searching all series…", tvController::searchAllSeries));
        Button searchAllSeasons = new Button(
                "Search All Seasons", e -> runBulkAction("Searching all seasons…", tvController::searchAllSeasons));
        Button searchAllEpisodes = new Button(
                "Search All Episodes", e -> runBulkAction("Searching all episodes…", tvController::searchAllEpisodes));
        Button testNotifications = new Button("Test Notifications", e -> runNotificationCheck());
        testNotifications.setVisible(admin);
        bulkButtons.addAll(List.of(
                refreshAll, validateAll, searchAllSeries, searchAllSeasons, searchAllEpisodes, testNotifications));
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
        HorizontalLayout statsRow = new HorizontalLayout(sonarrQueueCard, sonarrHealthCard);
        statsRow.getStyle().set("flex-wrap", "wrap");
        HorizontalLayout toolbar = new HorizontalLayout(
                searchField,
                refreshAll,
                validateAll,
                searchAllSeries,
                searchAllSeasons,
                searchAllEpisodes,
                testNotifications,
                showValidCheckbox,
                showStaleCheckbox,
                showWithNotesCheckbox,
                totalLabel);
        toolbar.setAlignItems(FlexComponent.Alignment.CENTER);
        // Many controls in one row: let them wrap instead of overflowing on narrow screens.
        toolbar.getStyle().set("flex-wrap", "wrap");

        add(statsRow, toolbar, grid);
        setFlexGrow(1, grid);
    }

    /**
     * Runs a blocking per-row controller action (Ombi/Sonarr/Plex) off the UI thread. On completion it refreshes only
     * the affected show's DB state (request- and episode-level validations, notes, sub-roll-up) instead of re-reading
     * the whole library, and reloads the live Sonarr/download status since the action may have changed the queue.
     */
    private void runRowAction(TvRequest mr, String workingMessage, Runnable action) {
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
     * read-only transaction on the worker thread because {@link Validation#getRequest()}/{@code getTvEpisode()} and
     * {@link Note#getRequest()} are lazy and would otherwise fail outside the request-bound session. A no-op until the
     * view is attached; the initial load is kicked off from {@link #onAttach}.
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
                            log.warn("Failed to refresh TV grid", throwable);
                            Notification.show("Failed to load TV shows; see the server log.");
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
            Map<Long, Map<String, Validation>> latestValidations,
            Map<Long, Map<String, Validation>> latestEpisodeValidations,
            Map<Long, Boolean> subValidations,
            Set<Long> withNotes,
            List<TvRequest> all) {}

    /**
     * Reads request- and episode-level validations, the show hierarchies (for the sub-validation roll-up), notes, and
     * requests, building every row index. Runs inside a read-only transaction on the worker thread.
     */
    private GridSnapshot buildSnapshot() {
        Map<Long, Map<String, Validation>> latest = new HashMap<>();
        Map<Long, Map<String, Validation>> latestEpisode = new HashMap<>();
        for (Validation v : validationRepository.findAll()) {
            String name = v.getValidationName();
            if (knownValidatorNames.contains(name)) {
                Long tvRequestId = v.getRequest().getId();
                latest.computeIfAbsent(tvRequestId, k -> new HashMap<>())
                        .merge(name, v, (a, b) -> a.getCreatedAt().isAfter(b.getCreatedAt()) ? a : b);
            } else if (knownEpisodeValidatorNames.contains(name) && v.getTvEpisode() != null) {
                Long episodeId = v.getTvEpisode().getId();
                latestEpisode
                        .computeIfAbsent(episodeId, k -> new HashMap<>())
                        .merge(name, v, (a, b) -> a.getCreatedAt().isAfter(b.getCreatedAt()) ? a : b);
            }
        }
        Map<Long, Boolean> subs = new HashMap<>();
        tvHierarchyService
                .loadAllHierarchies()
                .forEach((tvRequestId, children) -> subs.put(
                        tvRequestId,
                        TvHierarchyTreeGrid.allChildrenValidation(children, episodeValidators, latestEpisode)));
        Set<Long> withNotes = new HashSet<>();
        noteRepository.findAll().forEach(n -> withNotes.add(n.getRequest().getId()));
        List<TvRequest> all = tvRequestRepository.findAll();
        return new GridSnapshot(latest, latestEpisode, subs, withNotes, all);
    }

    /** Applies a freshly loaded snapshot to the view state (on the UI thread) and re-runs the active filters. */
    private void applySnapshot(GridSnapshot snapshot) {
        latestValidations.clear();
        latestValidations.putAll(snapshot.latestValidations());
        latestEpisodeValidations.clear();
        latestEpisodeValidations.putAll(snapshot.latestEpisodeValidations());
        subValidations.clear();
        subValidations.putAll(snapshot.subValidations());
        tvRequestsWithNotes.clear();
        tvRequestsWithNotes.addAll(snapshot.withNotes());
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
                .filter(mr -> tvRequestsWithNotes.contains(mr.getId()))
                .count();
        showValidLabel.setText("Show valid rows (" + valid + ")");
        showStaleLabel.setText("Show stale rows (" + stale + ")");
        showWithNotesLabel.setText("Show rows with notes (" + withNotes + ")");
        totalLabel.setText("Total TV shows: " + allRequests.size());
    }

    private record RowSnapshot(
            TvRequest request,
            Map<String, Validation> validations,
            Map<Long, Map<String, Validation>> episodeValidations,
            Boolean sub,
            boolean hasNotes) {}

    /** Refreshes a single show's DB state off the UI thread — see {@link #runRowAction}. */
    private void refreshRow(Long id) {
        getUI().ifPresent(ui -> CompletableFuture.supplyAsync(
                        () -> transactionTemplate.execute(status -> buildRowSnapshot(id)), uiTaskExecutor)
                .whenComplete((row, throwable) -> ui.access(() -> {
                    if (throwable != null) {
                        log.warn("Failed to refresh TV row {}", id, throwable);
                        Notification.show("Failed to refresh row; see the server log.");
                    } else if (row != null) {
                        applyRowSnapshot(id, row);
                    }
                })));
    }

    /**
     * Reads one show's request- and episode-level validations (the episodes fetched in a single {@code IN} query),
     * notes, and sub-roll-up. Runs inside a read-only transaction; a null request means the show was deleted.
     */
    private RowSnapshot buildRowSnapshot(Long id) {
        TvRequest mr = tvRequestRepository.findById(id).orElse(null);
        if (mr == null) {
            return new RowSnapshot(null, Map.of(), Map.of(), null, false);
        }
        Map<String, Validation> byName = new HashMap<>();
        for (Validation v : validationRepository.findByRequest(mr)) {
            if (knownValidatorNames.contains(v.getValidationName())) {
                byName.merge(
                        v.getValidationName(), v, (a, b) -> a.getCreatedAt().isAfter(b.getCreatedAt()) ? a : b);
            }
        }
        List<TvChildRequest> children = tvHierarchyService.loadHierarchy(mr);
        List<TvEpisodeRequest> episodes = children.stream()
                .flatMap(c -> c.getSeasonRequests().stream())
                .flatMap(s -> s.getEpisodeRequests().stream())
                .toList();
        Map<Long, Map<String, Validation>> latestEpisode = new HashMap<>();
        if (!episodes.isEmpty()) {
            for (Validation v : validationRepository.findByTvEpisodeIn(episodes)) {
                String name = v.getValidationName();
                if (knownEpisodeValidatorNames.contains(name) && v.getTvEpisode() != null) {
                    latestEpisode
                            .computeIfAbsent(v.getTvEpisode().getId(), k -> new HashMap<>())
                            .merge(name, v, (a, b) -> a.getCreatedAt().isAfter(b.getCreatedAt()) ? a : b);
                }
            }
        }
        Boolean sub = TvHierarchyTreeGrid.allChildrenValidation(children, episodeValidators, latestEpisode);
        boolean hasNotes = !noteRepository.findByRequestOrderByCreatedAtDesc(mr).isEmpty();
        return new RowSnapshot(mr, byName, latestEpisode, sub, hasNotes);
    }

    /** Merges a single show's refreshed state into the view (on the UI thread) and re-runs the active filters. */
    private void applyRowSnapshot(Long id, RowSnapshot row) {
        List<TvRequest> updated = new ArrayList<>(allRequests);
        updated.removeIf(r -> id.equals(r.getId()));
        if (row.request() == null) {
            latestValidations.remove(id);
            subValidations.remove(id);
            tvRequestsWithNotes.remove(id);
        } else {
            updated.add(row.request());
            latestValidations.put(id, row.validations());
            latestEpisodeValidations.putAll(row.episodeValidations());
            subValidations.put(id, row.sub());
            if (row.hasNotes()) {
                tvRequestsWithNotes.add(id);
            } else {
                tvRequestsWithNotes.remove(id);
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
        // Quietly refresh just the live Sonarr/download status on each poll (not the whole grid) so the queue card and
        // the expanded detail tree's progress stay current without a manual reload.
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
     * Kicks off the Sonarr queue + health fetch on the current UI, if the view is attached; a no-op otherwise. When
     * {@code showLoading} is set the stat cards show a spinner first (initial/explicit loads); the background poll
     * passes {@code false} so the cards update in place without flashing.
     */
    private void triggerStatsLoad(boolean showLoading) {
        getUI().ifPresent(ui -> loadStatsAsync(ui, showLoading));
    }

    /**
     * Fetches the Sonarr queue and health off the UI thread (both hit remote, VPN-fronted Sonarr and can be slow) so
     * the page's initial render isn't blocked; the stat cards show a spinner until the results arrive via
     * {@link UI#access}. The download-status load is chained afterwards because it joins torrents/slots to the queue
     * maps populated here. A guard skips the fetch when one is already in flight.
     */
    private void loadStatsAsync(UI ui, boolean showLoading) {
        if (!statsLoadInFlight.compareAndSet(false, true)) {
            return;
        }
        if (showLoading) {
            RequestViewSupport.showCardLoading(sonarrQueueValue);
            RequestViewSupport.showCardLoading(sonarrHealthValue);
        }
        CompletableFuture<SonarrQueue> queue =
                CompletableFuture.supplyAsync(tvController::getSonarrQueue, uiTaskExecutor);
        CompletableFuture<List<SonarrHealthItem>> health =
                CompletableFuture.supplyAsync(tvController::getSonarrHealth, uiTaskExecutor);
        queue.thenCombine(health, StatsResult::new)
                .whenComplete((stats, throwable) -> ui.access(() -> {
                    try {
                        if (throwable != null) {
                            log.warn("Failed to load Sonarr stats", throwable);
                        }
                        SonarrQueue sonarrQueue = throwable == null && stats != null ? stats.queue() : null;
                        List<SonarrHealthItem> sonarrHealth =
                                throwable == null && stats != null ? stats.health() : null;
                        updateSonarrQueueCard(sonarrQueue);
                        updateQueueMaps(sonarrQueue);
                        updateSonarrHealthCard(sonarrHealth);
                    } finally {
                        statsLoadInFlight.set(false);
                        triggerDownloadLoad();
                    }
                }));
    }

    private record StatsResult(SonarrQueue queue, List<SonarrHealthItem> health) {}

    /** Kicks off the Deluge + SABnzbd fetch on the current UI, if the view is attached; a no-op otherwise. */
    private void triggerDownloadLoad() {
        getUI().ifPresent(this::loadDownloadStatusAsync);
    }

    /**
     * Fetches Deluge torrent and SABnzbd queue status off the UI thread (both hit remote, VPN-fronted services and can
     * be slow), then joins them to queued episodes. Results are pushed back via {@link UI#access} (requires server
     * push). A guard skips the fetch when one is already in flight.
     */
    private void loadDownloadStatusAsync(UI ui) {
        if (!downloadLoadInFlight.compareAndSet(false, true)) {
            return;
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
                        // Refresh the main grid so an expanded detail's tree picks up the joined progress/peers.
                        grid.getDataProvider().refreshAll();
                    }
                }));
    }

    private record DownloadStatus(Map<String, DelugeTorrent> torrents, Map<String, SabnzbdSlot> slots) {}

    /**
     * Joins Deluge torrents and SABnzbd slots to queued episodes through the Sonarr queue: a queue record's downloadId
     * is the Deluge torrent hash (matched case-insensitively) for torrents or the SABnzbd {@code nzo_id} for usenet,
     * and the record's protocol picks which client to look in.
     */
    private void applyDownloadStatus(Map<String, DelugeTorrent> torrents, Map<String, SabnzbdSlot> slots) {
        torrentByEpisode.clear();
        slotByEpisode.clear();
        Map<String, DelugeTorrent> byLowerHash = new HashMap<>();
        if (torrents != null) {
            torrents.forEach((hash, torrent) -> byLowerHash.put(hash.toLowerCase(), torrent));
        }
        Map<String, SabnzbdSlot> byNzoId = slots == null ? Map.of() : slots;
        downloadIdByEpisode.forEach((key, downloadId) -> {
            if (downloadId == null) {
                return;
            }
            if (RequestViewSupport.isTorrent(protocolByEpisode.get(key))) {
                DelugeTorrent torrent = byLowerHash.get(downloadId.toLowerCase());
                if (torrent != null) {
                    torrentByEpisode.put(key, torrent);
                }
            } else {
                SabnzbdSlot slot = byNzoId.get(downloadId);
                if (slot != null) {
                    slotByEpisode.put(key, slot);
                }
            }
        });
    }

    /**
     * Indexes the current Sonarr queue: per-episode protocol/downloadId (for the expanded tree's Type/Progress/Peers)
     * and per-series protocols (for the main grid's Status badges). The queue is fetched with
     * {@code includeEpisode=true}, so each record carries its episode number.
     */
    private void updateQueueMaps(SonarrQueue queue) {
        protocolByEpisode.clear();
        downloadIdByEpisode.clear();
        protocolsBySeriesId.clear();
        if (queue == null || queue.getRecords() == null) {
            return;
        }
        for (SonarrQueueRecord record : queue.getRecords()) {
            Integer seriesId = record.getSeriesId();
            if (seriesId == null) {
                continue;
            }
            if (record.getProtocol() != null) {
                protocolsBySeriesId
                        .computeIfAbsent(seriesId, k -> new HashSet<>())
                        .add(record.getProtocol());
            }
            Integer episodeNumber =
                    record.getEpisode() == null ? null : record.getEpisode().getEpisodeNumber();
            if (record.getSeasonNumber() == null || episodeNumber == null) {
                continue;
            }
            QueueKey key = new QueueKey(seriesId, record.getSeasonNumber(), episodeNumber);
            protocolByEpisode.put(key, record.getProtocol());
            downloadIdByEpisode.put(key, record.getDownloadId());
        }
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
                .filter(mr -> showWithNotes || !tvRequestsWithNotes.contains(mr.getId()))
                .filter(mr -> showValid
                        || !mr.isValid(knownValidatorNames, latestValidations.getOrDefault(mr.getId(), Map.of())))
                .toList());
    }

    private static final List<String> CANNED_STALE_REASONS = List.of(
            "No search returned from Sonarr",
            "No tvdbId",
            "Not in English",
            "TV show misfiled in Movies folder",
            "Repeated download attempts without success");

    private void openMarkStaleDialog(TvRequest mr) {
        RequestViewSupport.openTextEntryDialog(
                "Mark \"" + mr.getTitle() + "\" as stale",
                "Reason",
                mr.getStaleReason(),
                CANNED_STALE_REASONS,
                false,
                reason -> {
                    tvController.markStale(mr.getId(), reason);
                    refreshRow(mr.getId());
                });
    }

    private void openAddNoteDialog(TvRequest mr) {
        RequestViewSupport.openTextEntryDialog(
                "Add note to \"" + mr.getTitle() + "\"", "Note", null, List.of(), true, note -> {
                    tvController.addNote(mr.getId(), note);
                    refreshRow(mr.getId());
                });
    }

    private void openViewNotesDialog(TvRequest mr) {
        RequestViewSupport.openNotesDialog(mr.getTitle(), noteRepository.findByRequestOrderByCreatedAtDesc(mr));
    }

    private void openPlexQueryUrlDialog(TvRequest mr) {
        String url = plexClient.showQueryUrl(mr.getTitle());
        RequestViewSupport.openTextDialog(
                "Plex query URL for \"" + mr.getTitle() + "\"", url == null ? "Plex query URL unavailable" : url);
    }

    /** Opens the row's Ombi details page in a new browser tab. */
    private void openOmbi(TvRequest mr) {
        String url = ombiHref(mr);
        if (url != null) {
            getUI().ifPresent(ui -> ui.getPage().open(url));
        }
    }

    /** Opens the row's Plex app deep link in a new browser tab. */
    private void openPlexApp(TvRequest mr) {
        String url = plexAppHref(mr);
        if (url != null) {
            getUI().ifPresent(ui -> ui.getPage().open(url));
        }
    }

    /** Opens the row's Plex metadata JSON URL in a new browser tab. */
    private void openPlexJson(TvRequest mr) {
        String url = plexHref(mr);
        if (url != null) {
            getUI().ifPresent(ui -> ui.getPage().open(url));
        }
    }

    /** Opens the row's TVDB page in a new browser tab. */
    private void openTvdb(TvRequest mr) {
        String url = tvdbHref(mr);
        if (url != null) {
            getUI().ifPresent(ui -> ui.getPage().open(url));
        }
    }

    private String ombiHref(TvRequest mr) {
        Integer externalProviderId = mr.getOmbiExternalProviderId();
        return externalProviderId == null ? null : ombiUrl + "/details/tv/" + externalProviderId;
    }

    private String sonarrHref(TvRequest mr) {
        String titleSlug = mr.getSonarrTitleSlug();
        return titleSlug == null || titleSlug.isBlank() ? null : sonarrUrl + "/series/" + titleSlug;
    }

    /** Available (downloaded) episodes over total episodes, per Sonarr — e.g. "52/236". */
    private static String episodesAvailable(TvRequest mr) {
        int available = mr.getSonarrEpisodeFileCount() == null ? 0 : mr.getSonarrEpisodeFileCount();
        int total = mr.getSonarrEpisodeCount() == null ? 0 : mr.getSonarrEpisodeCount();
        return available + "/" + total;
    }

    /** Fraction of episodes available (0.0–1.0), for sorting the Episodes column by completeness. */
    private static double episodeAvailabilityRatio(TvRequest mr) {
        int total = mr.getSonarrEpisodeCount() == null ? 0 : mr.getSonarrEpisodeCount();
        if (total == 0) {
            return 0.0;
        }
        int available = mr.getSonarrEpisodeFileCount() == null ? 0 : mr.getSonarrEpisodeFileCount();
        return (double) available / total;
    }

    private static String plexHref(TvRequest mr) {
        String url = mr.getPlexMetadataUrl();
        return url == null || url.isBlank() ? null : url;
    }

    private String plexAppHref(TvRequest mr) {
        String ratingKey = mr.getPlexMetadataId();
        if (ratingKey == null || ratingKey.isBlank() || plexMachineIdentifier == null) {
            return null;
        }
        return plexUrl + "/web/index.html#!/server/" + plexMachineIdentifier + "/details?key=/library/metadata/"
                + ratingKey;
    }

    private static String tvdbHref(TvRequest mr) {
        Integer tvdbId = mr.getTvdbId();
        return tvdbId == null ? null : "https://www.thetvdb.com/?id=" + tvdbId + "&tab=series";
    }

    /** Updates the queue card's number, per-state breakdown tooltip, and severity background. */
    private void updateSonarrQueueCard(SonarrQueue queue) {
        if (queue == null) {
            RequestViewSupport.updateQueueCard(sonarrQueueCard, sonarrQueueValue, sonarrQueueTooltip, null, null);
            return;
        }
        List<SonarrQueueRecord> records = queue.getRecords() == null ? List.of() : queue.getRecords();
        Map<String, Long> byState = records.stream()
                .map(SonarrQueueRecord::getTrackedDownloadState)
                .filter(state -> state != null)
                .collect(Collectors.groupingBy(state -> state, Collectors.counting()));
        RequestViewSupport.updateQueueCard(
                sonarrQueueCard, sonarrQueueValue, sonarrQueueTooltip, queue.getTotalRecords(), byState);
    }

    /** Sets the health count and a tooltip listing each reported issue. */
    private void updateSonarrHealthCard(List<SonarrHealthItem> health) {
        RequestViewSupport.updateHealthCard(
                sonarrHealthCard,
                sonarrHealthValue,
                sonarrHealthTooltip,
                health,
                SonarrHealthItem::getType,
                SonarrHealthItem::getMessage);
    }

    private LitRenderer<TvRequest> qualityBadgeRenderer() {
        return LitRenderer.<TvRequest>of("<span theme=\"badge\" ?hidden=\"${!item.label}\">${item.label}</span>"
                        + "<span ?hidden=\"${item.label}\">—</span>")
                .withProperty("label", mr -> {
                    String profile = mr.getSonarrQualityProfile();
                    return profile == null ? "" : profile;
                });
    }

    /**
     * Status cell: a torrent and/or usenet {@link Badge} when any episode of the series is in the Sonarr queue — solid
     * blue (default) for torrent, solid green for usenet — or a dash when nothing is queued.
     */
    private com.vaadin.flow.component.Component statusBadges(TvRequest mr) {
        Set<String> protocols = mr.getSonarrSeriesId() == null ? null : protocolsBySeriesId.get(mr.getSonarrSeriesId());
        return RequestViewSupport.protocolBadges(protocols == null ? Set.of() : protocols);
    }

    /**
     * Client-side renderer for a validator result cell. Using {@link LitRenderer} instead of a server-side component
     * column keeps the grid responsive while scrolling: the browser renders the icon from a tiny data payload rather
     * than the server building a component per cell.
     */
    private LitRenderer<TvRequest> validatorResultRenderer(String validationName) {
        return LitRenderer.<TvRequest>of(
                        "<vaadin-icon icon=\"${item.icon}\" style=\"color: ${item.color}\"></vaadin-icon>")
                .withProperty("icon", mr -> RequestViewSupport.resultIconName(latestResultValue(mr, validationName)))
                .withProperty("color", mr -> RequestViewSupport.resultIconColor(latestResultValue(mr, validationName)));
    }

    /** Sub-validation cell; null means no child episodes validated yet (unknown), not a failure. */
    private LitRenderer<TvRequest> subValidationRenderer() {
        return LitRenderer.<TvRequest>of(
                        "<vaadin-icon icon=\"${item.icon}\" style=\"color: ${item.color}\"></vaadin-icon>")
                .withProperty("icon", mr -> RequestViewSupport.resultIconName(subValidations.get(mr.getId())))
                .withProperty("color", mr -> RequestViewSupport.resultIconColor(subValidations.get(mr.getId())));
    }

    private Boolean latestResultValue(TvRequest mr, String validationName) {
        return RequestViewSupport.latestResultValue(latestValidations, mr.getId(), validationName);
    }

    private static final List<String> DETAIL_PRIORITY_FIELDS = List.of("id", "title", "tvdbId");

    /** Builds the per-episode download lookup for one series, keyed by season+episode for the tree grid. */
    private Map<EpisodeKey, TvHierarchyTreeGrid.EpisodeDownload> episodeDownloadsForSeries(Integer seriesId) {
        Map<EpisodeKey, TvHierarchyTreeGrid.EpisodeDownload> downloads = new HashMap<>();
        if (seriesId == null) {
            return downloads;
        }
        protocolByEpisode.forEach((key, protocol) -> {
            if (seriesId.equals(key.seriesId())) {
                downloads.put(
                        new EpisodeKey(key.seasonNumber(), key.episodeNumber()),
                        new TvHierarchyTreeGrid.EpisodeDownload(
                                protocol, torrentByEpisode.get(key), slotByEpisode.get(key)));
            }
        });
        return downloads;
    }

    private com.vaadin.flow.component.Component createDetails(TvRequest mr) {
        FormLayout fields = buildFieldDump(mr);

        List<TvChildRequest> children = tvHierarchyService.loadHierarchy(mr);
        Map<EpisodeKey, TvHierarchyTreeGrid.EpisodeDownload> downloads =
                episodeDownloadsForSeries(mr.getSonarrSeriesId());
        com.vaadin.flow.component.Component hierarchy = children.isEmpty()
                ? TvHierarchyTreeGrid.placeholderWhenEmpty()
                : new TvHierarchyTreeGrid(
                        children, episodeValidators, latestEpisodeValidations, downloads, tvController, uiTaskExecutor);

        VerticalLayout layout = new VerticalLayout(fields, hierarchy);
        layout.setWidthFull();
        layout.setPadding(false);
        return layout;
    }

    private static FormLayout buildFieldDump(TvRequest mr) {
        return RequestViewSupport.fieldDump(TvRequest.class, mr, DETAIL_PRIORITY_FIELDS);
    }
}
