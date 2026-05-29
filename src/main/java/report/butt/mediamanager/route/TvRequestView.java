package report.butt.mediamanager.route;

import com.vaadin.flow.component.button.Button;
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
import com.vaadin.flow.data.provider.SortDirection;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.spring.annotation.UIScope;
import com.vaadin.flow.theme.aura.Aura;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
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
import report.butt.mediamanager.controller.TvController;
import report.butt.mediamanager.model.Note;
import report.butt.mediamanager.model.TvChildRequest;
import report.butt.mediamanager.model.TvRequest;
import report.butt.mediamanager.model.Validation;
import report.butt.mediamanager.repository.NoteRepository;
import report.butt.mediamanager.repository.TvRequestRepository;
import report.butt.mediamanager.repository.ValidationRepository;
import report.butt.mediamanager.service.TvHierarchyService;
import report.butt.mediamanager.validation.Validator;

@Component
@UIScope
@StyleSheet(Aura.STYLESHEET)
@StyleSheet("grid-available.css")
public class TvRequestView extends VerticalLayout {

    private final Grid<TvRequest> grid = new Grid<>(TvRequest.class, false);
    private final TvRequestRepository tvRequestRepository;
    private final TvController tvController;
    private final ValidationRepository validationRepository;
    private final NoteRepository noteRepository;
    private final TvHierarchyService tvHierarchyService;
    private final Set<String> knownValidatorNames;
    private final Map<Long, Map<String, Validation>> latestValidations = new HashMap<>();
    private final Set<Long> tvRequestsWithNotes = new HashSet<>();
    private final Checkbox showValidCheckbox = new Checkbox(true);
    private final Checkbox showStaleCheckbox = new Checkbox(false);
    private final Checkbox showWithNotesCheckbox = new Checkbox(true);
    private final Span showValidLabel = coloredLabel("Show valid rows", "#2e8b57");
    private final Span showStaleLabel = coloredLabel("Show stale rows", "#b8860b");
    private final Span showWithNotesLabel = coloredLabel("Show rows with notes", "#1e6fce");
    private final String ombiUrl;
    private final String sonarrUrl;
    private final String plexUrl;
    private final String plexMachineIdentifier;
    private final String plexTvSectionId;
    private final String plexToken;

    public TvRequestView(
            TvRequestRepository tvRequestRepository,
            TvController tvController,
            ValidationRepository validationRepository,
            NoteRepository noteRepository,
            List<Validator<TvRequest>> validators,
            PlexClient plexClient,
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
        this.plexTvSectionId = plexClient.getTvSectionId();
        this.plexToken = plexClient.getPlexToken();
        this.knownValidatorNames =
                validators.stream().map(v -> v.getClass().getSimpleName()).collect(Collectors.toUnmodifiableSet());
        setSizeFull();

        Grid.Column<TvRequest> titleColumn = grid.addColumn(TvRequest::getTitle)
                .setHeader("Title")
                .setAutoWidth(true)
                .setSortable(true)
                .setComparator(
                        Comparator.comparing(TvRequest::getTitle, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)));

        grid.addComponentColumn(this::ombiLink).setHeader("Ombi").setAutoWidth(true);
        grid.addComponentColumn(this::sonarrLink).setHeader("Sonarr").setAutoWidth(true);
        grid.addComponentColumn(this::plexLink).setHeader("Plex").setAutoWidth(true);
        grid.addComponentColumn(this::plexAppLink).setHeader("Plex App").setAutoWidth(true);
        grid.addComponentColumn(TvRequestView::tvdbLink).setHeader("TVDB").setAutoWidth(true);

        validators.stream()
                .sorted(Comparator.comparingInt(Validator<TvRequest>::sortOrder))
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

        grid.setItemDetailsRenderer(new ComponentRenderer<>(this::createDetails));
        grid.setDetailsVisibleOnClick(true);

        GridContextMenu<TvRequest> contextMenu = grid.addContextMenu();
        contextMenu.addItem("Refresh", e -> e.getItem().ifPresent(mr -> {
            tvController.refresh(mr.getId());
            tvController.validate(mr.getId());
            refreshGrid();
        }));
        contextMenu.addItem("Search", e -> e.getItem().ifPresent(mr -> {
            tvController.searchOne(mr.getId());
            refreshGrid();
        }));
        GridMenuItem<TvRequest> markAvailableItem =
                contextMenu.addItem("Mark Available", e -> e.getItem().ifPresent(mr -> {
                    tvController.markAvailable(mr.getId());
                    tvController.refresh(mr.getId());
                    tvController.validate(mr.getId());
                    refreshGrid();
                }));
        contextMenu.addItem("Mark as Stale", e -> e.getItem().ifPresent(this::openMarkStaleDialog));
        contextMenu.addItem("Add Note", e -> e.getItem().ifPresent(this::openAddNoteDialog));
        contextMenu.addItem("View Notes", e -> e.getItem().ifPresent(this::openViewNotesDialog));
        contextMenu.addItem("Delete TV Request", e -> e.getItem().ifPresent(mr -> {
            tvController.delete(mr.getId());
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
            if (tvRequestsWithNotes.contains(mr.getId()) && !mr.isAvailable()) {
                return "has_notes";
            }
            Map<String, Validation> latestForRow = latestValidations.getOrDefault(mr.getId(), Map.of());
            return mr.isValid(knownValidatorNames, latestForRow) ? "available" : "not_available";
        });

        refreshGrid();
        grid.sort(List.of(new GridSortOrder<>(titleColumn, SortDirection.ASCENDING)));
        grid.setSizeFull();

        Button refreshAll = new Button("Refresh All", e -> {
            tvController.refreshAll();
            tvController.validateAll();
            refreshGrid();
        });
        Button validateAll = new Button("Validate All", e -> {
            tvController.validateAll();
            refreshGrid();
        });
        Button searchAll = new Button("Search All", e -> {
            tvController.searchAll();
            refreshGrid();
        });
        showValidCheckbox.addValueChangeListener(e -> refreshGrid());
        showStaleCheckbox.addValueChangeListener(e -> refreshGrid());
        showWithNotesCheckbox.addValueChangeListener(e -> refreshGrid());
        showValidCheckbox.setLabelComponent(showValidLabel);
        showStaleCheckbox.setLabelComponent(showStaleLabel);
        showWithNotesCheckbox.setLabelComponent(showWithNotesLabel);
        HorizontalLayout toolbar = new HorizontalLayout(
                refreshAll, validateAll, searchAll, showValidCheckbox, showStaleCheckbox, showWithNotesCheckbox);
        toolbar.setAlignItems(FlexComponent.Alignment.CENTER);

        add(toolbar, grid);
    }

    private void refreshGrid() {
        latestValidations.clear();
        for (Validation v : validationRepository.findAll()) {
            if (!knownValidatorNames.contains(v.getValidationName())) {
                continue;
            }
            Long tvRequestId = v.getRequest().getId();
            latestValidations
                    .computeIfAbsent(tvRequestId, k -> new HashMap<>())
                    .merge(v.getValidationName(), v, (a, b) -> a.getCreatedAt().isAfter(b.getCreatedAt()) ? a : b);
        }
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

        boolean showValid = Boolean.TRUE.equals(showValidCheckbox.getValue());
        boolean showStale = Boolean.TRUE.equals(showStaleCheckbox.getValue());
        boolean showWithNotes = Boolean.TRUE.equals(showWithNotesCheckbox.getValue());
        List<TvRequest> rows = all.stream()
                .filter(mr -> showStale || !Boolean.TRUE.equals(mr.getStale()))
                .filter(mr -> showWithNotes || !tvRequestsWithNotes.contains(mr.getId()))
                .filter(mr -> showValid
                        || !mr.isValid(knownValidatorNames, latestValidations.getOrDefault(mr.getId(), Map.of())))
                .toList();
        grid.setItems(rows);
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

    private com.vaadin.flow.component.Component ombiLink(TvRequest mr) {
        Integer externalProviderId = mr.getOmbiExternalProviderId();
        if (externalProviderId == null) {
            return new Span("—");
        }
        return externalLink(ombiUrl + "/details/tv/" + externalProviderId);
    }

    private com.vaadin.flow.component.Component sonarrLink(TvRequest mr) {
        Integer tvdbId = mr.getTvdbId();
        if (tvdbId == null) {
            return new Span("—");
        }
        return externalLink(sonarrUrl + "/series/" + tvdbId);
    }

    private com.vaadin.flow.component.Component plexLink(TvRequest mr) {
        String title = mr.getTitle();
        if (title == null || title.isBlank() || plexTvSectionId == null) {
            return new Span("—");
        }
        String url = plexUrl + "/library/sections/" + plexTvSectionId + "/all?includeGuids=1&title="
                + URLEncoder.encode(title, StandardCharsets.UTF_8)
                + "&X-Plex-Token=" + URLEncoder.encode(plexToken, StandardCharsets.UTF_8);
        return externalLink(url);
    }

    private com.vaadin.flow.component.Component plexAppLink(TvRequest mr) {
        String ratingKey = mr.getPlexMetadataId();
        if (ratingKey == null || ratingKey.isBlank() || plexMachineIdentifier == null) {
            return new Span("—");
        }
        String url = plexUrl + "/web/index.html#!/server/" + plexMachineIdentifier + "/details?key=/library/metadata/"
                + ratingKey;
        return externalLink(url);
    }

    private static com.vaadin.flow.component.Component tvdbLink(TvRequest mr) {
        Integer tvdbId = mr.getTvdbId();
        if (tvdbId == null) {
            return new Span("—");
        }
        return externalLink("https://www.thetvdb.com/?id=" + tvdbId + "&tab=series");
    }

    private static com.vaadin.flow.component.Component headerWithTooltip(String shortName, String description) {
        Span label = new Span(shortName);
        Tooltip.forComponent(label).setText(description);
        return label;
    }

    private static Span coloredLabel(String text, String color) {
        Span span = new Span(text);
        span.getStyle().set("color", color);
        return span;
    }

    private static Anchor externalLink(String href) {
        Anchor link = new Anchor(href, new Icon(VaadinIcon.EXTERNAL_LINK));
        link.setTarget(AnchorTarget.BLANK);
        return link;
    }

    private com.vaadin.flow.component.Component latestResult(TvRequest mr, String validationName) {
        Map<String, Validation> byName = latestValidations.get(mr.getId());
        Validation v = byName == null ? null : byName.get(validationName);
        if (v == null) {
            return new Span("—");
        }
        return resultIcon(v.getResult());
    }

    private Boolean latestResultValue(TvRequest mr, String validationName) {
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

    private static final List<String> DETAIL_PRIORITY_FIELDS = List.of("id", "title", "tvdbId");

    private com.vaadin.flow.component.Component createDetails(TvRequest mr) {
        FormLayout fields = buildFieldDump(mr);

        List<TvChildRequest> children = tvHierarchyService.loadHierarchy(mr);
        com.vaadin.flow.component.Component hierarchy =
                children.isEmpty() ? TvHierarchyTreeGrid.placeholderWhenEmpty() : new TvHierarchyTreeGrid(children);

        VerticalLayout layout = new VerticalLayout(fields, hierarchy);
        layout.setWidthFull();
        layout.setPadding(false);
        return layout;
    }

    private static FormLayout buildFieldDump(TvRequest mr) {
        FormLayout layout = new FormLayout();
        layout.setResponsiveSteps(
                new FormLayout.ResponsiveStep("0", 1),
                new FormLayout.ResponsiveStep("600px", 2),
                new FormLayout.ResponsiveStep("900px", 3));

        List<Field> fields = collectFields(TvRequest.class).stream()
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
