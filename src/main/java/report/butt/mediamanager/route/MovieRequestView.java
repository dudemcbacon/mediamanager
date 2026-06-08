package report.butt.mediamanager.route;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.badge.Badge;
import com.vaadin.flow.component.badge.BadgeVariant;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.card.Card;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.dialog.Dialog;
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
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.provider.SortDirection;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.data.renderer.LitRenderer;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.spring.annotation.UIScope;
import com.vaadin.flow.theme.aura.Aura;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
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
import report.butt.mediamanager.service.NotificationService;
import report.butt.mediamanager.validation.Validator;

@Component
@UIScope
@StyleSheet(Aura.STYLESHEET)
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
    private final Set<Long> movieRequestsWithNotes = new HashSet<>();
    private final Checkbox showValidCheckbox = new Checkbox(true);
    private final Checkbox showStaleCheckbox = new Checkbox(false);
    private final Checkbox showWithNotesCheckbox = new Checkbox(true);
    private final Span showValidLabel = RequestViewSupport.coloredLabel("Show valid rows", "#2e8b57");
    private final Span showStaleLabel = RequestViewSupport.coloredLabel("Show stale rows", "#b8860b");
    private final Span showWithNotesLabel = RequestViewSupport.coloredLabel("Show rows with notes", "#1e6fce");
    private final Span totalLabel = RequestViewSupport.coloredLabel("Total movies", "#333");
    private final Span radarrQueueValue = new Span("—");
    private final Card radarrQueueCard = RequestViewSupport.statCard("Radarr Queue", radarrQueueValue);
    private final Tooltip radarrQueueTooltip = Tooltip.forComponent(radarrQueueCard);
    private final Span radarrHealthValue = new Span("—");
    private final Card radarrHealthCard = RequestViewSupport.statCard("Health Issues", radarrHealthValue);
    private final Tooltip radarrHealthTooltip = Tooltip.forComponent(radarrHealthCard);
    private final TextField searchField = new TextField();
    private List<MovieRequest> allRequests = List.of();
    private final String ombiUrl;
    private final String radarrUrl;
    private final String plexUrl;
    private final String plexMachineIdentifier;
    private final PlexClient plexClient;
    private final DelugeClient delugeClient;
    private final SabnzbdClient sabnzbdClient;
    private final NotificationService notificationService;

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
            @Value("${radarr.url}") String radarrUrl) {
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

        GridContextMenu<MovieRequest> contextMenu = grid.addContextMenu();
        RequestViewSupport.suppressGridContextMenuOnLinks(grid);
        contextMenu.addItem("Refresh", e -> e.getItem().ifPresent(mr -> {
            movieController.refresh(mr.getId());
            movieController.validate(mr.getId());
            refreshGrid();
        }));
        contextMenu.addItem("Search", e -> e.getItem().ifPresent(mr -> {
            movieController.searchOne(mr.getId());
            refreshGrid();
        }));
        contextMenu.addItem("Delete Download", e -> e.getItem().ifPresent(mr -> {
            movieController.deleteDownloadAndSearch(mr.getId());
            refreshGrid();
        }));
        GridMenuItem<MovieRequest> markAvailableItem =
                contextMenu.addItem("Mark Available", e -> e.getItem().ifPresent(mr -> {
                    movieController.markAvailable(mr.getId());
                    movieController.refresh(mr.getId());
                    movieController.validate(mr.getId());
                    refreshGrid();
                }));
        contextMenu.addItem("Set Quality Profile to 'Any'", e -> e.getItem().ifPresent(mr -> {
            movieController.setQualityProfileToAny(mr.getId());
            refreshGrid();
        }));
        contextMenu.addItem("Mark as Stale", e -> e.getItem().ifPresent(this::openMarkStaleDialog));
        contextMenu.addItem("Add Note", e -> e.getItem().ifPresent(this::openAddNoteDialog));
        contextMenu.addItem("View Notes", e -> e.getItem().ifPresent(this::openViewNotesDialog));
        contextMenu.addItem("View Plex Query URL", e -> e.getItem().ifPresent(this::openPlexQueryUrlDialog));
        GridMenuItem<MovieRequest> viewOmbiItem =
                contextMenu.addItem("View Ombi", e -> e.getItem().ifPresent(this::openOmbi));
        GridMenuItem<MovieRequest> viewPlexAppItem =
                contextMenu.addItem("View Plex App", e -> e.getItem().ifPresent(this::openPlexApp));
        GridMenuItem<MovieRequest> viewPlexJsonItem =
                contextMenu.addItem("View Plex Json", e -> e.getItem().ifPresent(this::openPlexJson));
        GridMenuItem<MovieRequest> viewTmdbItem =
                contextMenu.addItem("View TMDB", e -> e.getItem().ifPresent(this::openTmdb));
        contextMenu.addItem("Delete Movie Request", e -> e.getItem().ifPresent(mr -> {
            movieController.delete(mr.getId());
            refreshGrid();
        }));
        contextMenu.setDynamicContentHandler(mr -> {
            if (mr == null) {
                return false;
            }
            markAvailableItem.setEnabled(mr.getOmbiRequestId() != null);
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

        refreshGrid();
        grid.sort(List.of(new GridSortOrder<>(titleColumn, SortDirection.ASCENDING)));
        grid.setWidthFull();
        grid.setMinHeight("0");

        Button refreshAll = new Button("Refresh All", e -> {
            movieController.refreshAll();
            movieController.validateAll();
            refreshGrid();
        });
        Button validateAll = new Button("Validate All", e -> {
            movieController.validateAll();
            refreshGrid();
        });
        Button searchAll = new Button("Search All", e -> {
            movieController.searchAll();
            refreshGrid();
        });
        Button testNotifications = new Button(
                "Test Notifications",
                e -> Notification.show(RequestViewSupport.notificationSummary(notificationService.runCheck())));
        showValidCheckbox.addValueChangeListener(e -> refreshGrid());
        showStaleCheckbox.addValueChangeListener(e -> refreshGrid());
        showWithNotesCheckbox.addValueChangeListener(e -> refreshGrid());
        showValidCheckbox.setLabelComponent(showValidLabel);
        showStaleCheckbox.setLabelComponent(showStaleLabel);
        showWithNotesCheckbox.setLabelComponent(showWithNotesLabel);
        searchField.setPlaceholder("Search by title");
        searchField.setClearButtonVisible(true);
        searchField.setValueChangeMode(ValueChangeMode.LAZY);
        searchField.addValueChangeListener(e -> applyFilters());
        HorizontalLayout statsRow = new HorizontalLayout(radarrQueueCard, radarrHealthCard);
        statsRow.setAlignItems(FlexComponent.Alignment.CENTER);
        HorizontalLayout toolbar = new HorizontalLayout(
                searchField,
                refreshAll,
                validateAll,
                searchAll,
                testNotifications,
                showValidCheckbox,
                showStaleCheckbox,
                showWithNotesCheckbox,
                totalLabel);
        toolbar.setAlignItems(FlexComponent.Alignment.CENTER);

        add(statsRow, toolbar, grid);
        setFlexGrow(1, grid);
    }

    private void refreshGrid() {
        latestValidations.clear();
        for (Validation v : validationRepository.findAll()) {
            if (!knownValidatorNames.contains(v.getValidationName())) {
                continue;
            }
            Long movieRequestId = v.getRequest().getId();
            latestValidations
                    .computeIfAbsent(movieRequestId, k -> new HashMap<>())
                    .merge(v.getValidationName(), v, (a, b) -> a.getCreatedAt().isAfter(b.getCreatedAt()) ? a : b);
        }
        movieRequestsWithNotes.clear();
        noteRepository
                .findAll()
                .forEach(n -> movieRequestsWithNotes.add(n.getRequest().getId()));
        List<MovieRequest> all = movieRequestRepository.findAll();
        long validCount = all.stream()
                .filter(mr -> mr.isValid(knownValidatorNames, latestValidations.getOrDefault(mr.getId(), Map.of())))
                .count();
        long staleCount =
                all.stream().filter(mr -> Boolean.TRUE.equals(mr.getStale())).count();
        long withNotesCount = all.stream()
                .filter(mr -> movieRequestsWithNotes.contains(mr.getId()))
                .count();
        showValidLabel.setText("Show valid rows (" + validCount + ")");
        showStaleLabel.setText("Show stale rows (" + staleCount + ")");
        showWithNotesLabel.setText("Show rows with notes (" + withNotesCount + ")");
        totalLabel.setText("Total movies: " + all.size());

        allRequests = all;
        applyFilters();
        triggerStatsLoad();
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        triggerStatsLoad();
    }

    /** Kicks off the Radarr queue + health fetch on the current UI, if the view is attached; a no-op otherwise. */
    private void triggerStatsLoad() {
        getUI().ifPresent(this::loadStatsAsync);
    }

    /**
     * Fetches the Radarr queue and health off the UI thread (both hit remote, VPN-fronted Radarr and can be slow) so
     * the page's initial render isn't blocked; the stat cards show a spinner until the results arrive via
     * {@link UI#access}. The download-status load is chained afterwards because it joins torrents/slots to the queue
     * maps populated here. A guard skips the fetch when one is already in flight.
     */
    private void loadStatsAsync(UI ui) {
        if (!statsLoadInFlight.compareAndSet(false, true)) {
            return;
        }
        RequestViewSupport.showCardLoading(radarrQueueValue);
        RequestViewSupport.showCardLoading(radarrHealthValue);
        CompletableFuture<RadarrQueue> queue = CompletableFuture.supplyAsync(movieController::getRadarrQueue);
        CompletableFuture<List<RadarrHealthItem>> health =
                CompletableFuture.supplyAsync(movieController::getRadarrHealth);
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
                        triggerDownloadLoad();
                    }
                }));
    }

    private record StatsResult(RadarrQueue queue, List<RadarrHealthItem> health) {}

    /** Kicks off the Deluge + SABnzbd fetch on the current UI, if the view is attached; a no-op otherwise. */
    private void triggerDownloadLoad() {
        getUI().ifPresent(this::loadDownloadStatusAsync);
    }

    /**
     * Fetches Deluge torrent and SABnzbd queue status off the UI thread (both hit remote, VPN-fronted services and can
     * be slow). While the fetch is in flight the Status column shows a per-row spinner for queued movies; results are
     * pushed back via {@link UI#access}. Requires server push (see {@code @Push}). A guard skips the fetch when one is
     * already in flight.
     */
    private void loadDownloadStatusAsync(UI ui) {
        if (!downloadLoadInFlight.compareAndSet(false, true)) {
            return;
        }
        // Render the per-row spinners now that the in-flight guard is set.
        grid.getDataProvider().refreshAll();
        CompletableFuture<Map<String, DelugeTorrent>> torrents =
                CompletableFuture.supplyAsync(delugeClient::getTorrentsStatus);
        CompletableFuture<Map<String, SabnzbdSlot>> slots = CompletableFuture.supplyAsync(sabnzbdClient::getQueueSlots);
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
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Mark \"" + mr.getTitle() + "\" as stale");

        TextArea reason = new TextArea("Reason");
        reason.setWidthFull();
        reason.setMinHeight("8em");
        if (mr.getStaleReason() != null) {
            reason.setValue(mr.getStaleReason());
        }

        HorizontalLayout cannedReasons = new HorizontalLayout();
        cannedReasons.getStyle().set("flex-wrap", "wrap");
        for (String canned : CANNED_STALE_REASONS) {
            Button preset = new Button(canned, e -> reason.setValue(canned));
            cannedReasons.add(preset);
        }

        Button submit = new Button("Submit", e -> {
            movieController.markStale(mr.getId(), reason.getValue());
            dialog.close();
            refreshGrid();
        });
        Button cancel = new Button("Cancel", e -> dialog.close());

        dialog.add(cannedReasons, reason);
        dialog.getFooter().add(cancel, submit);
        dialog.open();
    }

    private void openAddNoteDialog(MovieRequest mr) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Add note to \"" + mr.getTitle() + "\"");

        TextArea note = new TextArea("Note");
        note.setWidthFull();
        note.setMinHeight("8em");

        Button submit = new Button("Submit", e -> {
            if (note.getValue() == null || note.getValue().isBlank()) {
                return;
            }
            movieController.addNote(mr.getId(), note.getValue());
            dialog.close();
            refreshGrid();
        });
        Button cancel = new Button("Cancel", e -> dialog.close());

        dialog.add(note);
        dialog.getFooter().add(cancel, submit);
        dialog.open();
    }

    private void openViewNotesDialog(MovieRequest mr) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Notes for \"" + mr.getTitle() + "\"");
        dialog.setWidth("600px");

        VerticalLayout body = new VerticalLayout();
        body.setPadding(false);
        body.setSpacing(true);

        List<Note> notes = noteRepository.findByRequestOrderByCreatedAtDesc(mr);
        if (notes.isEmpty()) {
            body.add(new Span("No notes yet."));
        } else {
            for (Note n : notes) {
                VerticalLayout entry = new VerticalLayout();
                entry.setPadding(false);
                entry.setSpacing(false);
                Span timestamp = new Span(String.valueOf(n.getCreatedAt()));
                timestamp.getStyle().set("font-size", "var(--lumo-font-size-s)");
                timestamp.getStyle().set("color", "var(--lumo-secondary-text-color)");
                Span text = new Span(n.getNotes());
                text.getStyle().set("white-space", "pre-wrap");
                entry.add(timestamp, text);
                body.add(entry);
            }
        }

        Button close = new Button("Close", e -> dialog.close());
        dialog.add(body);
        dialog.getFooter().add(close);
        dialog.open();
    }

    private void openPlexQueryUrlDialog(MovieRequest mr) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Plex query URL for \"" + mr.getTitle() + "\"");
        dialog.setWidth("600px");

        String url = plexClient.movieQueryUrl(mr.getTitle());
        TextArea urlField = new TextArea();
        urlField.setReadOnly(true);
        urlField.setWidthFull();
        urlField.setValue(url == null ? "Plex query URL unavailable" : url);

        Button close = new Button("Close", e -> dialog.close());
        dialog.add(urlField);
        dialog.getFooter().add(close);
        dialog.open();
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

    private static final String IMPORT_BLOCKED_COLOR = "#f44336";
    private static final String IMPORT_PENDING_COLOR = "#ffeb3b";
    private static final String HEALTH_WARNING_COLOR = "#ffeb3b";

    /**
     * Updates the queue card's number, a per-state breakdown tooltip, and a severity background: red when any item is
     * importBlocked (highest priority), yellow when any is importPending.
     */
    private void updateRadarrQueueCard(RadarrQueue queue) {
        if (queue == null) {
            radarrQueueValue.setText("—");
            radarrQueueTooltip.setText("Radarr queue unavailable");
            radarrQueueCard.getStyle().remove("background-color");
            return;
        }

        Integer total = queue.getTotalRecords();
        radarrQueueValue.setText(total == null ? "—" : String.valueOf(total));

        List<RadarrQueueRecord> records = queue.getRecords() == null ? List.of() : queue.getRecords();
        Map<String, Long> byState = records.stream()
                .map(RadarrQueueRecord::getTrackedDownloadState)
                .filter(state -> state != null)
                .collect(Collectors.groupingBy(state -> state, Collectors.counting()));

        String breakdown = byState.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue()
                        .reversed()
                        .thenComparing(Map.Entry.<String, Long>comparingByKey()))
                .map(e -> e.getKey() + ": " + e.getValue())
                .collect(Collectors.joining(", "));
        radarrQueueTooltip.setText(breakdown.isEmpty() ? "No active downloads" : breakdown);

        if (byState.containsKey("importBlocked")) {
            radarrQueueCard.getStyle().set("background-color", IMPORT_BLOCKED_COLOR);
        } else if (byState.containsKey("importPending")) {
            radarrQueueCard.getStyle().set("background-color", IMPORT_PENDING_COLOR);
        } else {
            radarrQueueCard.getStyle().remove("background-color");
        }
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
        if (health == null) {
            radarrHealthValue.setText("—");
            radarrHealthTooltip.setText("Radarr health unavailable");
            radarrHealthCard.getStyle().remove("background-color");
            return;
        }
        radarrHealthValue.setText(String.valueOf(health.size()));
        if (health.isEmpty()) {
            radarrHealthTooltip.setText("No health issues");
            radarrHealthCard.getStyle().remove("background-color");
            return;
        }
        radarrHealthTooltip.setText(health.stream()
                .map(h -> RequestViewSupport.healthIssueLine(h.getType(), h.getMessage()))
                .collect(Collectors.joining("\n")));
        if (health.stream().anyMatch(h -> "warning".equalsIgnoreCase(h.getType()))) {
            radarrHealthCard.getStyle().set("background-color", HEALTH_WARNING_COLOR);
        } else {
            radarrHealthCard.getStyle().remove("background-color");
        }
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
        return LitRenderer.<MovieRequest>of("<span class=\"status-spinner\" ?hidden=\"${!item.loading}\"></span>"
                        + "<span ?hidden=\"${item.loading}\">${item.progress}</span>")
                .withProperty("loading", this::isStatusLoading)
                .withProperty("progress", this::progressText);
    }

    /**
     * Client-side renderer for the Peers cell: a spinner while a torrent's status loads, then the peer/seed counts.
     * Non-torrent (usenet) downloads have no peers, so they show a dash.
     */
    private LitRenderer<MovieRequest> peersRenderer() {
        return LitRenderer.<MovieRequest>of("<span class=\"status-spinner\" ?hidden=\"${!item.loading}\"></span>"
                        + "<span ?hidden=\"${item.loading}\">${item.peers}</span>")
                .withProperty("loading", this::isPeersLoading)
                .withProperty("peers", this::peersText);
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
