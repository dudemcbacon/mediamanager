package report.butt.mediamanager.route;

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
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.AnchorTarget;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.shared.Tooltip;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.provider.SortDirection;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.spring.annotation.UIScope;
import com.vaadin.flow.theme.aura.Aura;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import report.butt.mediamanager.client.PlexClient;
import report.butt.mediamanager.controller.MovieController;
import report.butt.mediamanager.model.MovieRequest;
import report.butt.mediamanager.model.Note;
import report.butt.mediamanager.model.Validation;
import report.butt.mediamanager.model.radarr.RadarrHealthItem;
import report.butt.mediamanager.model.radarr.RadarrQueue;
import report.butt.mediamanager.model.radarr.RadarrQueueRecord;
import report.butt.mediamanager.repository.MovieRequestRepository;
import report.butt.mediamanager.repository.NoteRepository;
import report.butt.mediamanager.repository.ValidationRepository;
import report.butt.mediamanager.validation.Validator;

@Component
@UIScope
@StyleSheet(Aura.STYLESHEET)
@StyleSheet("grid-available.css")
public class MovieRequestView extends VerticalLayout {

    private final Grid<MovieRequest> grid = new Grid<>(MovieRequest.class, false);
    private final MovieRequestRepository movieRequestRepository;
    private final MovieController movieController;
    private final ValidationRepository validationRepository;
    private final NoteRepository noteRepository;
    private final Set<String> knownValidatorNames;
    private final Map<Long, Map<String, Validation>> latestValidations = new HashMap<>();
    private final Set<Long> movieRequestsWithNotes = new HashSet<>();
    private final Checkbox showValidCheckbox = new Checkbox(true);
    private final Checkbox showStaleCheckbox = new Checkbox(false);
    private final Checkbox showWithNotesCheckbox = new Checkbox(true);
    private final Span showValidLabel = coloredLabel("Show valid rows", "#2e8b57");
    private final Span showStaleLabel = coloredLabel("Show stale rows", "#b8860b");
    private final Span showWithNotesLabel = coloredLabel("Show rows with notes", "#1e6fce");
    private final Span totalLabel = coloredLabel("Total movies", "#333");
    private final Span radarrQueueValue = new Span("—");
    private final Card radarrQueueCard = statCard("Radarr Queue", radarrQueueValue);
    private final Tooltip radarrQueueTooltip = Tooltip.forComponent(radarrQueueCard);
    private final Span radarrHealthValue = new Span("—");
    private final Card radarrHealthCard = statCard("Health Issues", radarrHealthValue);
    private final Tooltip radarrHealthTooltip = Tooltip.forComponent(radarrHealthCard);
    private final TextField searchField = new TextField();
    private List<MovieRequest> allRequests = List.of();
    private final String ombiUrl;
    private final String radarrUrl;
    private final String plexUrl;
    private final String plexMachineIdentifier;

    public MovieRequestView(
            MovieRequestRepository movieRequestRepository,
            MovieController movieController,
            ValidationRepository validationRepository,
            NoteRepository noteRepository,
            List<Validator<MovieRequest>> validators,
            PlexClient plexClient,
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

        grid.addComponentColumn(this::ombiLink).setHeader("Ombi").setAutoWidth(true);
        grid.addComponentColumn(this::radarrLink).setHeader("Radarr").setAutoWidth(true);
        grid.addComponentColumn(MovieRequestView::plexLink).setHeader("Plex").setAutoWidth(true);
        grid.addComponentColumn(this::plexAppLink).setHeader("Plex App").setAutoWidth(true);
        grid.addComponentColumn(MovieRequestView::tmdbLink).setHeader("TMDB").setAutoWidth(true);

        validators.stream()
                .sorted(Comparator.comparingInt(Validator<MovieRequest>::sortOrder))
                .forEach(validator -> {
                    String name = validator.getClass().getSimpleName();
                    grid.addComponentColumn(mr -> latestResult(mr, name))
                            .setHeader(headerWithTooltip(validator.shortName(), validator.description()))
                            .setAutoWidth(true)
                            .setSortable(true)
                            .setComparator(Comparator.comparing(
                                    mr -> latestResultValue(mr, name),
                                    Comparator.nullsLast(Comparator.naturalOrder())));
                });

        grid.setItemDetailsRenderer(new ComponentRenderer<>(MovieRequestView::createDetails));
        grid.setDetailsVisibleOnClick(true);

        GridContextMenu<MovieRequest> contextMenu = grid.addContextMenu();
        contextMenu.addItem("Refresh", e -> e.getItem().ifPresent(mr -> {
            movieController.refresh(mr.getId());
            movieController.validate(mr.getId());
            refreshGrid();
        }));
        contextMenu.addItem("Search", e -> e.getItem().ifPresent(mr -> {
            movieController.searchOne(mr.getId());
            refreshGrid();
        }));
        GridMenuItem<MovieRequest> markAvailableItem =
                contextMenu.addItem("Mark Available", e -> e.getItem().ifPresent(mr -> {
                    movieController.markAvailable(mr.getId());
                    movieController.refresh(mr.getId());
                    movieController.validate(mr.getId());
                    refreshGrid();
                }));
        contextMenu.addItem("Mark as Stale", e -> e.getItem().ifPresent(this::openMarkStaleDialog));
        contextMenu.addItem("Add Note", e -> e.getItem().ifPresent(this::openAddNoteDialog));
        contextMenu.addItem("View Notes", e -> e.getItem().ifPresent(this::openViewNotesDialog));
        contextMenu.addItem("Delete Movie Request", e -> e.getItem().ifPresent(mr -> {
            movieController.delete(mr.getId());
            refreshGrid();
        }));
        contextMenu.setDynamicContentHandler(mr -> {
            if (mr == null) {
                return false;
            }
            markAvailableItem.setEnabled(mr.getOmbiRequestId() != null);
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
        HorizontalLayout toolbar = new HorizontalLayout(
                searchField,
                refreshAll,
                validateAll,
                searchAll,
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
        updateRadarrQueueCard(movieController.getRadarrQueue());
        updateRadarrHealthCard(movieController.getRadarrHealth());

        allRequests = all;
        applyFilters();
    }

    /**
     * Applies the toolbar filters to the already-loaded {@link #allRequests} without re-querying.
     * A non-blank search matches rows by title and ignores the show/stale/notes toggles, so
     * matching rows surface even when those filters would otherwise hide them.
     */
    private void applyFilters() {
        String term = searchField.getValue() == null ? "" : searchField.getValue().trim();
        if (!term.isBlank()) {
            String lower = term.toLowerCase();
            grid.setItems(allRequests.stream()
                    .filter(mr -> mr.getTitle() != null
                            && mr.getTitle().toLowerCase().contains(lower))
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

    private com.vaadin.flow.component.Component ombiLink(MovieRequest mr) {
        Integer tmdbid = mr.getTmdbid();
        if (tmdbid == null) {
            return new Span("—");
        }
        return externalLink(ombiUrl + "/details/movie/" + tmdbid);
    }

    private com.vaadin.flow.component.Component radarrLink(MovieRequest mr) {
        Integer tmdbid = mr.getTmdbid();
        if (tmdbid == null) {
            return new Span("—");
        }
        return externalLink(radarrUrl + "/movie/" + tmdbid);
    }

    private static com.vaadin.flow.component.Component plexLink(MovieRequest mr) {
        String url = mr.getPlexMetadataUrl();
        if (url == null || url.isBlank()) {
            return new Span("—");
        }
        return externalLink(url);
    }

    private com.vaadin.flow.component.Component plexAppLink(MovieRequest mr) {
        String ratingKey = mr.getPlexMetadataId();
        if (ratingKey == null || ratingKey.isBlank() || plexMachineIdentifier == null) {
            return new Span("—");
        }
        String url = plexUrl + "/web/index.html#!/server/" + plexMachineIdentifier + "/details?key=/library/metadata/"
                + ratingKey;
        return externalLink(url);
    }

    private static com.vaadin.flow.component.Component tmdbLink(MovieRequest mr) {
        Integer tmdbid = mr.getTmdbid();
        if (tmdbid == null) {
            return new Span("—");
        }
        return externalLink("https://www.themoviedb.org/movie/" + tmdbid);
    }

    private static com.vaadin.flow.component.Component headerWithTooltip(String shortName, String description) {
        Span label = new Span(shortName);
        Tooltip.forComponent(label).setText(description);
        return label;
    }

    private static final String IMPORT_BLOCKED_COLOR = "#f44336";
    private static final String IMPORT_PENDING_COLOR = "#ffeb3b";
    private static final String HEALTH_WARNING_COLOR = "#ffeb3b";

    /**
     * Updates the queue card's number, a per-state breakdown tooltip, and a severity background:
     * red when any item is importBlocked (highest priority), yellow when any is importPending.
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
        radarrHealthTooltip.setText(
                health.stream().map(MovieRequestView::healthIssueLine).collect(Collectors.joining("\n")));
        if (health.stream().anyMatch(h -> "warning".equalsIgnoreCase(h.getType()))) {
            radarrHealthCard.getStyle().set("background-color", HEALTH_WARNING_COLOR);
        } else {
            radarrHealthCard.getStyle().remove("background-color");
        }
    }

    private static String healthIssueLine(RadarrHealthItem item) {
        String type = item.getType() == null ? "" : "[" + item.getType() + "] ";
        String message = item.getMessage() == null ? "" : item.getMessage();
        return "• " + type + message;
    }

    private static Card statCard(String title, Span value) {
        value.getStyle().set("font-size", "var(--lumo-font-size-xxl)").set("font-weight", "bold");
        Card card = new Card();
        card.setTitle(title);
        card.add(value);
        return card;
    }

    private static Span coloredLabel(String text, String color) {
        Span span = new Span(text);
        span.getStyle().set("color", color);
        return span;
    }

    private static Anchor externalLink(String href) {
        Anchor link = new Anchor(href, new Icon(VaadinIcon.EXTERNAL_LINK));
        link.setTarget(AnchorTarget.BLANK);
        // Keep right-clicks on the link from bubbling to the grid's context menu, so the browser's
        // own link menu (open in new tab, copy link) handles them instead.
        link.getElement().executeJs("this.addEventListener('contextmenu', e => e.stopPropagation())");
        return link;
    }

    private com.vaadin.flow.component.Component latestResult(MovieRequest mr, String validationName) {
        Map<String, Validation> byName = latestValidations.get(mr.getId());
        Validation v = byName == null ? null : byName.get(validationName);
        if (v == null) {
            return new Span("—");
        }
        return resultIcon(v.getResult());
    }

    private Boolean latestResultValue(MovieRequest mr, String validationName) {
        Map<String, Validation> byName = latestValidations.get(mr.getId());
        Validation v = byName == null ? null : byName.get(validationName);
        return v == null ? null : v.getResult();
    }

    private static com.vaadin.flow.component.Component resultIcon(Boolean result) {
        if (Boolean.TRUE.equals(result)) {
            Icon icon = VaadinIcon.CHECK.create();
            icon.getStyle().set("color", "var(--lumo-success-color, green)");
            return icon;
        }
        Icon icon = VaadinIcon.CLOSE.create();
        icon.getStyle().set("color", "var(--lumo-error-color, red)");
        return icon;
    }

    private static final List<String> DETAIL_PRIORITY_FIELDS = List.of("id", "title", "tmdbid");

    private static FormLayout createDetails(MovieRequest mr) {
        FormLayout layout = new FormLayout();
        layout.setResponsiveSteps(
                new FormLayout.ResponsiveStep("0", 1),
                new FormLayout.ResponsiveStep("600px", 2),
                new FormLayout.ResponsiveStep("900px", 3));

        List<Field> fields = collectFields(MovieRequest.class).stream()
                .filter(f -> !Modifier.isStatic(f.getModifiers()) && !f.isSynthetic())
                .sorted(Comparator.<Field>comparingInt(f -> {
                            int idx = DETAIL_PRIORITY_FIELDS.indexOf(f.getName());
                            return idx == -1 ? Integer.MAX_VALUE : idx;
                        })
                        .thenComparing(Field::getName, String.CASE_INSENSITIVE_ORDER))
                .toList();

        for (Field field : fields) {
            field.setAccessible(true);
            Object value;
            try {
                value = field.get(mr);
            } catch (IllegalAccessException e) {
                value = "<inaccessible>";
            }
            addField(layout, humanizeFieldName(field.getName()), value);
        }

        return layout;
    }

    private static List<Field> collectFields(Class<?> type) {
        List<Field> fields = new java.util.ArrayList<>();
        for (Class<?> c = type; c != null && c != Object.class; c = c.getSuperclass()) {
            fields.addAll(Arrays.asList(c.getDeclaredFields()));
        }
        return fields;
    }

    private static String humanizeFieldName(String name) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            if (i > 0 && Character.isUpperCase(c) && !Character.isUpperCase(name.charAt(i - 1))) {
                sb.append(' ');
            }
            sb.append(i == 0 ? Character.toUpperCase(c) : c);
        }
        return sb.toString();
    }

    private static void addField(FormLayout layout, String label, Object value) {
        Span valueSpan = new Span(value == null ? "—" : String.valueOf(value));
        layout.addFormItem(valueSpan, label);
    }
}
