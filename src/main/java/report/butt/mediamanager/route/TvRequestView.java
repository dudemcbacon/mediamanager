package report.butt.mediamanager.route;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.UI;
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
import report.butt.mediamanager.controller.TvController;
import report.butt.mediamanager.model.Note;
import report.butt.mediamanager.model.TvChildRequest;
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
import report.butt.mediamanager.service.TvHierarchyService;
import report.butt.mediamanager.validation.EpisodeValidator;
import report.butt.mediamanager.validation.Validator;

@Component
@UIScope
@StyleSheet(Aura.STYLESHEET)
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
    private final Span showValidLabel = RequestViewSupport.coloredLabel("Show valid rows", "#2e8b57");
    private final Span showStaleLabel = RequestViewSupport.coloredLabel("Show stale rows", "#b8860b");
    private final Span showWithNotesLabel = RequestViewSupport.coloredLabel("Show rows with notes", "#1e6fce");
    private final Span totalLabel = RequestViewSupport.coloredLabel("Total TV shows", "#333");
    private final Span sonarrQueueValue = new Span("—");
    private final Card sonarrQueueCard = RequestViewSupport.statCard("Sonarr Queue", sonarrQueueValue);
    private final Tooltip sonarrQueueTooltip = Tooltip.forComponent(sonarrQueueCard);
    private final Span sonarrHealthValue = new Span("—");
    private final Card sonarrHealthCard = RequestViewSupport.statCard("Health Issues", sonarrHealthValue);
    private final Tooltip sonarrHealthTooltip = Tooltip.forComponent(sonarrHealthCard);
    private final TextField searchField = new TextField();
    private List<TvRequest> allRequests = List.of();
    private final String ombiUrl;
    private final String sonarrUrl;
    private final String plexUrl;
    private final String plexMachineIdentifier;
    private final PlexClient plexClient;
    private final DelugeClient delugeClient;
    private final SabnzbdClient sabnzbdClient;
    private final Map<QueueKey, String> protocolByEpisode = new HashMap<>();
    private final Map<QueueKey, String> downloadIdByEpisode = new HashMap<>();
    private final Map<QueueKey, DelugeTorrent> torrentByEpisode = new HashMap<>();
    private final Map<QueueKey, SabnzbdSlot> slotByEpisode = new HashMap<>();
    private final Map<Integer, Set<String>> protocolsBySeriesId = new HashMap<>();
    private final AtomicBoolean downloadLoadInFlight = new AtomicBoolean(false);

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
            @Value("${ombi.url}") String ombiUrl,
            @Value("${sonarr.url}") String sonarrUrl,
            TvHierarchyService tvHierarchyService) {
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

        GridContextMenu<TvRequest> contextMenu = grid.addContextMenu();
        RequestViewSupport.suppressGridContextMenuOnLinks(grid);
        contextMenu.addItem("Refresh", e -> e.getItem().ifPresent(mr -> {
            tvController.refresh(mr.getId());
            tvController.validate(mr.getId());
            refreshGrid();
        }));
        contextMenu.addItem("Search", e -> e.getItem().ifPresent(mr -> {
            tvController.searchOne(mr.getId());
            refreshGrid();
        }));
        contextMenu.addItem("Search All Seasons", e -> e.getItem()
                .ifPresent(mr -> tvController.searchAllSeasonsForRequest(mr.getId())));
        contextMenu.addItem("Search All Episodes", e -> e.getItem()
                .ifPresent(mr -> tvController.searchAllEpisodesForRequest(mr.getId())));
        GridMenuItem<TvRequest> markAvailableItem =
                contextMenu.addItem("Mark Available", e -> e.getItem().ifPresent(mr -> {
                    tvController.markAvailable(mr.getId());
                    tvController.refresh(mr.getId());
                    tvController.validate(mr.getId());
                    refreshGrid();
                }));
        contextMenu.addItem("Set Quality Profile to 'Any'", e -> e.getItem().ifPresent(mr -> {
            tvController.setQualityProfileToAny(mr.getId());
            refreshGrid();
        }));
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
        contextMenu.addItem("Delete TV Request", e -> e.getItem().ifPresent(mr -> {
            tvController.delete(mr.getId());
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

        refreshGrid();
        grid.sort(List.of(new GridSortOrder<>(titleColumn, SortDirection.ASCENDING)));
        grid.setWidthFull();
        grid.setMinHeight("0");

        Button refreshAll = new Button("Refresh All", e -> {
            tvController.refreshAll();
            tvController.validateAll();
            refreshGrid();
        });
        Button validateAll = new Button("Validate All", e -> {
            tvController.validateAll();
            refreshGrid();
        });
        Button searchAllSeries = new Button("Search All Series", e -> {
            tvController.searchAllSeries();
            refreshGrid();
        });
        Button searchAllSeasons = new Button("Search All Seasons", e -> {
            tvController.searchAllSeasons();
            refreshGrid();
        });
        Button searchAllEpisodes = new Button("Search All Episodes", e -> {
            tvController.searchAllEpisodes();
            refreshGrid();
        });
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
        HorizontalLayout statsRow = new HorizontalLayout(sonarrQueueCard, sonarrHealthCard);
        HorizontalLayout toolbar = new HorizontalLayout(
                searchField,
                refreshAll,
                validateAll,
                searchAllSeries,
                searchAllSeasons,
                searchAllEpisodes,
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
        latestEpisodeValidations.clear();
        for (Validation v : validationRepository.findAll()) {
            String name = v.getValidationName();
            if (knownValidatorNames.contains(name)) {
                Long tvRequestId = v.getRequest().getId();
                latestValidations
                        .computeIfAbsent(tvRequestId, k -> new HashMap<>())
                        .merge(name, v, (a, b) -> a.getCreatedAt().isAfter(b.getCreatedAt()) ? a : b);
            } else if (knownEpisodeValidatorNames.contains(name) && v.getTvEpisode() != null) {
                Long episodeId = v.getTvEpisode().getId();
                latestEpisodeValidations
                        .computeIfAbsent(episodeId, k -> new HashMap<>())
                        .merge(name, v, (a, b) -> a.getCreatedAt().isAfter(b.getCreatedAt()) ? a : b);
            }
        }
        subValidations.clear();
        tvHierarchyService
                .loadAllHierarchies()
                .forEach((tvRequestId, children) -> subValidations.put(
                        tvRequestId,
                        TvHierarchyTreeGrid.allChildrenValidation(
                                children, episodeValidators, latestEpisodeValidations)));

        tvRequestsWithNotes.clear();
        noteRepository
                .findAll()
                .forEach(n -> tvRequestsWithNotes.add(n.getRequest().getId()));
        List<TvRequest> all = tvRequestRepository.findAll();
        long validCount = all.stream()
                .filter(mr -> mr.isValid(knownValidatorNames, latestValidations.getOrDefault(mr.getId(), Map.of())))
                .count();
        long staleCount =
                all.stream().filter(mr -> Boolean.TRUE.equals(mr.getStale())).count();
        long withNotesCount = all.stream()
                .filter(mr -> tvRequestsWithNotes.contains(mr.getId()))
                .count();
        showValidLabel.setText("Show valid rows (" + validCount + ")");
        showStaleLabel.setText("Show stale rows (" + staleCount + ")");
        showWithNotesLabel.setText("Show rows with notes (" + withNotesCount + ")");
        totalLabel.setText("Total TV shows: " + all.size());
        SonarrQueue sonarrQueue = tvController.getSonarrQueue();
        updateSonarrQueueCard(sonarrQueue);
        updateQueueMaps(sonarrQueue);
        updateSonarrHealthCard(tvController.getSonarrHealth());

        allRequests = all;
        applyFilters();
        triggerDownloadLoad();
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        triggerDownloadLoad();
    }

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
            tvController.markStale(mr.getId(), reason.getValue());
            dialog.close();
            refreshGrid();
        });
        Button cancel = new Button("Cancel", e -> dialog.close());

        dialog.add(cannedReasons, reason);
        dialog.getFooter().add(cancel, submit);
        dialog.open();
    }

    private void openAddNoteDialog(TvRequest mr) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Add note to \"" + mr.getTitle() + "\"");

        TextArea note = new TextArea("Note");
        note.setWidthFull();
        note.setMinHeight("8em");

        Button submit = new Button("Submit", e -> {
            if (note.getValue() == null || note.getValue().isBlank()) {
                return;
            }
            tvController.addNote(mr.getId(), note.getValue());
            dialog.close();
            refreshGrid();
        });
        Button cancel = new Button("Cancel", e -> dialog.close());

        dialog.add(note);
        dialog.getFooter().add(cancel, submit);
        dialog.open();
    }

    private void openViewNotesDialog(TvRequest mr) {
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

    private void openPlexQueryUrlDialog(TvRequest mr) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Plex query URL for \"" + mr.getTitle() + "\"");
        dialog.setWidth("600px");

        String url = plexClient.showQueryUrl(mr.getTitle());
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

    private static final String IMPORT_BLOCKED_COLOR = "#f44336";
    private static final String IMPORT_PENDING_COLOR = "#ffeb3b";
    private static final String HEALTH_WARNING_COLOR = "#ffeb3b";

    /**
     * Updates the queue card's number, a per-state breakdown tooltip, and a severity background: red when any item is
     * importBlocked (highest priority), yellow when any is importPending.
     */
    private void updateSonarrQueueCard(SonarrQueue queue) {
        if (queue == null) {
            sonarrQueueValue.setText("—");
            sonarrQueueTooltip.setText("Sonarr queue unavailable");
            sonarrQueueCard.getStyle().remove("background-color");
            return;
        }

        Integer total = queue.getTotalRecords();
        sonarrQueueValue.setText(total == null ? "—" : String.valueOf(total));

        List<SonarrQueueRecord> records = queue.getRecords() == null ? List.of() : queue.getRecords();
        Map<String, Long> byState = records.stream()
                .map(SonarrQueueRecord::getTrackedDownloadState)
                .filter(state -> state != null)
                .collect(Collectors.groupingBy(state -> state, Collectors.counting()));

        String breakdown = byState.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue()
                        .reversed()
                        .thenComparing(Map.Entry.<String, Long>comparingByKey()))
                .map(e -> e.getKey() + ": " + e.getValue())
                .collect(Collectors.joining(", "));
        sonarrQueueTooltip.setText(breakdown.isEmpty() ? "No active downloads" : breakdown);

        if (byState.containsKey("importBlocked")) {
            sonarrQueueCard.getStyle().set("background-color", IMPORT_BLOCKED_COLOR);
        } else if (byState.containsKey("importPending")) {
            sonarrQueueCard.getStyle().set("background-color", IMPORT_PENDING_COLOR);
        } else {
            sonarrQueueCard.getStyle().remove("background-color");
        }
    }

    /** Sets the health count and a tooltip listing each reported issue. */
    private void updateSonarrHealthCard(List<SonarrHealthItem> health) {
        if (health == null) {
            sonarrHealthValue.setText("—");
            sonarrHealthTooltip.setText("Sonarr health unavailable");
            sonarrHealthCard.getStyle().remove("background-color");
            return;
        }
        sonarrHealthValue.setText(String.valueOf(health.size()));
        if (health.isEmpty()) {
            sonarrHealthTooltip.setText("No health issues");
            sonarrHealthCard.getStyle().remove("background-color");
            return;
        }
        sonarrHealthTooltip.setText(health.stream()
                .map(h -> RequestViewSupport.healthIssueLine(h.getType(), h.getMessage()))
                .collect(Collectors.joining("\n")));
        if (health.stream().anyMatch(h -> "warning".equalsIgnoreCase(h.getType()))) {
            sonarrHealthCard.getStyle().set("background-color", HEALTH_WARNING_COLOR);
        } else {
            sonarrHealthCard.getStyle().remove("background-color");
        }
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
                        children, episodeValidators, latestEpisodeValidations, downloads, tvController);

        VerticalLayout layout = new VerticalLayout(fields, hierarchy);
        layout.setWidthFull();
        layout.setPadding(false);
        return layout;
    }

    private static FormLayout buildFieldDump(TvRequest mr) {
        return RequestViewSupport.fieldDump(TvRequest.class, mr, DETAIL_PRIORITY_FIELDS);
    }
}
