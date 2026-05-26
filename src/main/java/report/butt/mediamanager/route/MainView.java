package report.butt.mediamanager.route;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridSortOrder;
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
import com.vaadin.flow.router.Route;
import com.vaadin.flow.theme.aura.Aura;

import org.springframework.beans.factory.annotation.Value;

import report.butt.mediamanager.controller.MovieController;
import report.butt.mediamanager.model.MovieRequest;
import report.butt.mediamanager.model.Validation;
import report.butt.mediamanager.repository.MovieRequestRepository;
import report.butt.mediamanager.repository.ValidationRepository;
import report.butt.mediamanager.validation.MovieValidator;

@Route
@StyleSheet(Aura.STYLESHEET)
@StyleSheet("grid-available.css")
public class MainView extends VerticalLayout {

  private final Grid<MovieRequest> grid = new Grid<>(MovieRequest.class, false);
  private final MovieRequestRepository movieRequestRepository;
  private final MovieController movieController;
  private final ValidationRepository validationRepository;
  private final Set<String> knownValidatorNames;
  private final Map<Long, Map<String, Validation>> latestValidations = new HashMap<>();
  private final Checkbox showValidCheckbox = new Checkbox("Show valid rows", true);
  private final String ombiUrl;
  private final String radarrUrl;

  public MainView(MovieRequestRepository movieRequestRepository, MovieController movieController,
      ValidationRepository validationRepository, List<MovieValidator> validators,
      @Value("${ombi.url}") String ombiUrl,
      @Value("${radarr.url}") String radarrUrl) {
    this.movieRequestRepository = movieRequestRepository;
    this.movieController = movieController;
    this.validationRepository = validationRepository;
    this.ombiUrl = ombiUrl;
    this.radarrUrl = radarrUrl;
    this.knownValidatorNames = validators.stream()
        .map(v -> v.getClass().getSimpleName())
        .collect(Collectors.toUnmodifiableSet());
    setSizeFull();

    Grid.Column<MovieRequest> titleColumn = grid.addColumn(MovieRequest::getTitle).setHeader("Title")
        .setAutoWidth(true).setSortable(true).setComparator(Comparator.comparing(MovieRequest::getTitle,
            Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)));

    grid.addComponentColumn(this::ombiLink).setHeader("Ombi").setAutoWidth(true);
    grid.addComponentColumn(this::radarrLink).setHeader("Radarr").setAutoWidth(true);
    grid.addComponentColumn(MainView::plexLink).setHeader("Plex").setAutoWidth(true);
    grid.addComponentColumn(MainView::tmdbLink).setHeader("TMDB").setAutoWidth(true);

    validators.stream()
        .sorted(Comparator.comparingInt(MovieValidator::sortOrder))
        .forEach(validator -> {
          String name = validator.getClass().getSimpleName();
          grid.addComponentColumn(mr -> latestResult(mr, name))
              .setHeader(headerWithTooltip(validator.shortName(), validator.description()))
              .setAutoWidth(true)
              .setSortable(true).setComparator(Comparator.comparing(
                  mr -> latestResultValue(mr, name),
                  Comparator.nullsLast(Comparator.naturalOrder())));
        });

    grid.addComponentColumn(this::createActionButtons).setHeader("Actions").setAutoWidth(true);

    grid.setItemDetailsRenderer(new ComponentRenderer<>(MainView::createDetails));
    grid.setDetailsVisibleOnClick(true);

    grid.setPartNameGenerator(mr -> {
      Map<String, Validation> latestForRow = latestValidations.getOrDefault(mr.getId(), Map.of());
      return mr.isValid(knownValidatorNames, latestForRow) ? "available" : "not_available";
    });

    refreshGrid();
    grid.sort(List.of(new GridSortOrder<>(titleColumn, SortDirection.ASCENDING)));
    grid.setSizeFull();

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
    HorizontalLayout toolbar = new HorizontalLayout(refreshAll, validateAll, searchAll, showValidCheckbox);
    toolbar.setAlignItems(FlexComponent.Alignment.CENTER);

    add(toolbar, grid);
  }

  private void refreshGrid() {
    latestValidations.clear();
    for (Validation v : validationRepository.findAll()) {
      if (!knownValidatorNames.contains(v.getValidationName())) {
        continue;
      }
      Long movieRequestId = v.getMovieRequest().getId();
      latestValidations
          .computeIfAbsent(movieRequestId, k -> new HashMap<>())
          .merge(v.getValidationName(), v,
              (a, b) -> a.getCreatedAt().isAfter(b.getCreatedAt()) ? a : b);
    }
    boolean showValid = Boolean.TRUE.equals(showValidCheckbox.getValue());
    List<MovieRequest> rows = movieRequestRepository.findAll().stream()
        .filter(mr -> showValid || !mr.isValid(knownValidatorNames,
            latestValidations.getOrDefault(mr.getId(), Map.of())))
        .toList();
    grid.setItems(rows);
  }

  private HorizontalLayout createActionButtons(MovieRequest mr) {
    Button refresh = new Button("Refresh", e -> {
      movieController.refresh(mr.getId());
      movieController.validate(mr.getId());
      refreshGrid();
    });
    Button search = new Button("Search", e -> {
      movieController.searchOne(mr.getId());
      refreshGrid();
    });
    Button markAvailable = new Button("Mark Available", e -> {
      movieController.markAvailable(mr.getId());
      movieController.refresh(mr.getId());
      movieController.validate(mr.getId());
      refreshGrid();
    });
    markAvailable.setEnabled(mr.getOmbiRequestId() != null);
    Button markStale = new Button("Mark as Stale", e -> openMarkStaleDialog(mr));
    return new HorizontalLayout(refresh, search, markAvailable, markStale);
  }

  private void openMarkStaleDialog(MovieRequest mr) {
    Dialog dialog = new Dialog();
    dialog.setHeaderTitle("Mark \"" + mr.getTitle() + "\" as stale");

    TextArea reason = new TextArea("Reason");
    reason.setWidthFull();
    reason.setMinHeight("8em");
    if (mr.getStaleReason() != null) {
      reason.setValue(mr.getStaleReason());
    }

    Button submit = new Button("Submit", e -> {
      movieController.markStale(mr.getId(), reason.getValue());
      dialog.close();
      refreshGrid();
    });
    Button cancel = new Button("Cancel", e -> dialog.close());

    dialog.add(reason);
    dialog.getFooter().add(cancel, submit);
    dialog.open();
  }

  private Component ombiLink(MovieRequest mr) {
    Integer tmdbid = mr.getTmdbid();
    if (tmdbid == null) {
      return new Span("—");
    }
    return externalLink(ombiUrl + "/details/movie/" + tmdbid);
  }

  private Component radarrLink(MovieRequest mr) {
    Integer tmdbid = mr.getTmdbid();
    if (tmdbid == null) {
      return new Span("—");
    }
    return externalLink(radarrUrl + "/movie/" + tmdbid);
  }

  private static Component plexLink(MovieRequest mr) {
    String url = mr.getPlexMetadataUrl();
    if (url == null || url.isBlank()) {
      return new Span("—");
    }
    return externalLink(url);
  }

  private static Component tmdbLink(MovieRequest mr) {
    Integer tmdbid = mr.getTmdbid();
    if (tmdbid == null) {
      return new Span("—");
    }
    return externalLink("https://www.themoviedb.org/movie/" + tmdbid);
  }

  private static Component headerWithTooltip(String shortName, String description) {
    Span label = new Span(shortName);
    Tooltip.forComponent(label).setText(description);
    return label;
  }

  private static Anchor externalLink(String href) {
    Anchor link = new Anchor(href, new Icon(VaadinIcon.EXTERNAL_LINK));
    link.setTarget(AnchorTarget.BLANK);
    return link;
  }

  private Component latestResult(MovieRequest mr, String validationName) {
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

  private static Component resultIcon(Boolean result) {
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

    List<Field> fields = Arrays.stream(MovieRequest.class.getDeclaredFields())
        .filter(f -> !Modifier.isStatic(f.getModifiers()) && !f.isSynthetic())
        .sorted(Comparator
            .<Field>comparingInt(f -> {
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
