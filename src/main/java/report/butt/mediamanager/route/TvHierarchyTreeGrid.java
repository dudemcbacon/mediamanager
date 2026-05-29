package report.butt.mediamanager.route;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.treegrid.TreeGrid;
import com.vaadin.flow.data.provider.hierarchy.TreeData;
import com.vaadin.flow.data.provider.hierarchy.TreeDataProvider;
import java.util.List;
import report.butt.mediamanager.model.TvChildRequest;
import report.butt.mediamanager.model.TvEpisodeRequest;
import report.butt.mediamanager.model.TvSeasonRequest;
import report.butt.mediamanager.route.TvHierarchyRow.ChildRow;
import report.butt.mediamanager.route.TvHierarchyRow.EpisodeRow;
import report.butt.mediamanager.route.TvHierarchyRow.SeasonRow;

class TvHierarchyTreeGrid extends TreeGrid<TvHierarchyRow> {

    TvHierarchyTreeGrid(List<TvChildRequest> children) {
        setSizeFull();

        TreeData<TvHierarchyRow> treeData = new TreeData<>();
        for (TvChildRequest child : children) {
            ChildRow childRow = new ChildRow(child);
            treeData.addItem(null, childRow);
            for (TvSeasonRequest season : child.getSeasonRequests()) {
                SeasonRow seasonRow = new SeasonRow(season);
                treeData.addItem(childRow, seasonRow);
                for (TvEpisodeRequest episode : season.getEpisodeRequests()) {
                    treeData.addItem(seasonRow, new EpisodeRow(episode));
                }
            }
        }
        setDataProvider(new TreeDataProvider<>(treeData));

        addHierarchyColumn(TvHierarchyTreeGrid::titleText).setHeader("Title").setAutoWidth(true);
        addColumn(TvHierarchyTreeGrid::idText).setHeader("Id").setAutoWidth(true);
        addComponentColumn(TvHierarchyTreeGrid::availableComponent)
                .setHeader("Available")
                .setAutoWidth(true);
        addColumn(TvHierarchyTreeGrid::countsText).setHeader("Counts").setAutoWidth(true);
        addColumn(TvHierarchyTreeGrid::statusText).setHeader("Status").setAutoWidth(true);
    }

    private static String idText(TvHierarchyRow row) {
        Long id = switch (row) {
            case ChildRow(TvChildRequest c) -> c.getId();
            case SeasonRow(TvSeasonRequest s) -> s.getId();
            case EpisodeRow(TvEpisodeRequest e) -> e.getId();
        };
        return id == null ? "—" : id.toString();
    }

    private static String titleText(TvHierarchyRow row) {
        return switch (row) {
            case ChildRow(TvChildRequest c) -> c.getTitle() == null ? "—" : c.getTitle();
            case SeasonRow(TvSeasonRequest s) ->
                "Season " + (s.getOmbiSeasonNumber() == null ? "?" : s.getOmbiSeasonNumber());
            case EpisodeRow(TvEpisodeRequest e) -> {
                String prefix = "E" + (e.getOmbiEpisodeNumber() == null ? "?" : e.getOmbiEpisodeNumber());
                String title = e.getOmbiTitle();
                yield (title == null || title.isBlank()) ? prefix : prefix + " — " + title;
            }
        };
    }

    private static Component availableComponent(TvHierarchyRow row) {
        return switch (row) {
            case ChildRow(TvChildRequest c) -> resultIcon(c.isAvailable());
            case SeasonRow(TvSeasonRequest s) -> resultIcon(Boolean.TRUE.equals(s.getOmbiSeasonAvailable()));
            case EpisodeRow(TvEpisodeRequest e) -> resultIcon(Boolean.TRUE.equals(e.getOmbiAvailable()));
        };
    }

    private static String countsText(TvHierarchyRow row) {
        return switch (row) {
            case ChildRow(TvChildRequest c) -> {
                List<TvSeasonRequest> seasons = c.getSeasonRequests();
                long available = seasons.stream()
                        .filter(s -> Boolean.TRUE.equals(s.getOmbiSeasonAvailable()))
                        .count();
                yield available + "/" + seasons.size();
            }
            case SeasonRow(TvSeasonRequest s) -> {
                List<TvEpisodeRequest> episodes = s.getEpisodeRequests();
                long available = episodes.stream()
                        .filter(e -> Boolean.TRUE.equals(e.getOmbiAvailable()))
                        .count();
                yield available + "/" + episodes.size();
            }
            case EpisodeRow ignored -> "—";
        };
    }

    private static String statusText(TvHierarchyRow row) {
        return switch (row) {
            case ChildRow(TvChildRequest c) -> c.getOmbiRequestStatus() == null ? "—" : c.getOmbiRequestStatus();
            case SeasonRow ignored -> "—";
            case EpisodeRow(TvEpisodeRequest e) -> e.getOmbiRequestStatus() == null ? "—" : e.getOmbiRequestStatus();
        };
    }

    private static Component resultIcon(boolean result) {
        if (result) {
            Icon icon = VaadinIcon.CHECK.create();
            icon.getStyle().set("color", "var(--lumo-success-color, green)");
            return icon;
        }
        Icon icon = VaadinIcon.CLOSE.create();
        icon.getStyle().set("color", "var(--lumo-error-color, red)");
        return icon;
    }

    static Component placeholderWhenEmpty() {
        return new Span("No child requests yet — try Refresh");
    }
}
