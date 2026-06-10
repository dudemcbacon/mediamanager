package report.butt.mediamanager.route;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.grid.contextmenu.GridContextMenu;
import com.vaadin.flow.component.grid.contextmenu.GridMenuItem;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.treegrid.TreeGrid;
import com.vaadin.flow.data.provider.hierarchy.TreeData;
import com.vaadin.flow.data.provider.hierarchy.TreeDataProvider;
import com.vaadin.flow.data.renderer.LitRenderer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import report.butt.mediamanager.controller.TvController;
import report.butt.mediamanager.model.TvChildRequest;
import report.butt.mediamanager.model.TvEpisodeRequest;
import report.butt.mediamanager.model.TvSeasonRequest;
import report.butt.mediamanager.model.Validation;
import report.butt.mediamanager.model.deluge.DelugeTorrent;
import report.butt.mediamanager.model.plex.EpisodeKey;
import report.butt.mediamanager.model.sabnzbd.SabnzbdSlot;
import report.butt.mediamanager.route.TvHierarchyRow.ChildRow;
import report.butt.mediamanager.route.TvHierarchyRow.EpisodeRow;
import report.butt.mediamanager.route.TvHierarchyRow.SeasonRow;
import report.butt.mediamanager.security.SecurityUtils;
import report.butt.mediamanager.validation.EpisodeValidator;

class TvHierarchyTreeGrid extends TreeGrid<TvHierarchyRow> {

    private static final Logger log = LoggerFactory.getLogger(TvHierarchyTreeGrid.class);

    /** A queued episode's Sonarr protocol joined to its Deluge torrent or SABnzbd slot (either may be null). */
    record EpisodeDownload(String protocol, DelugeTorrent torrent, SabnzbdSlot slot) {}

    // Resolved once at construction (using each episode's known season number) so the Type/Progress/Peers
    // renderers never touch the lazy TvSeasonRequest association off the UI/transaction.
    private final Map<Long, EpisodeDownload> downloadByEpisodeId = new HashMap<>();
    // Type badges roll up: a season carries the protocols of its downloading episodes, a child the
    // protocols of its downloading seasons. Progress/Peers stay episode-only.
    private final Map<Long, Set<String>> protocolsBySeasonId = new HashMap<>();
    private final Map<Long, Set<String>> protocolsByChildId = new HashMap<>();
    private final Executor uiTaskExecutor;

    TvHierarchyTreeGrid(
            List<TvChildRequest> children,
            List<EpisodeValidator> episodeValidators,
            Map<Long, Map<String, Validation>> latestEpisodeValidations,
            Map<EpisodeKey, EpisodeDownload> episodeDownloads,
            TvController tvController,
            Executor uiTaskExecutor) {
        this.uiTaskExecutor = uiTaskExecutor;
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
                    indexDownload(child, season, episode, episodeDownloads);
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

        addComponentColumn(this::typeComponent).setHeader("Type").setAutoWidth(true);
        addColumn(progressRenderer()).setHeader("Progress").setAutoWidth(true);
        addColumn(this::peersText).setHeader("Peers").setAutoWidth(true);

        for (EpisodeValidator validator : episodeValidators) {
            String name = validator.getClass().getSimpleName();
            addComponentColumn(row -> episodeValidationComponent(row, name, latestEpisodeValidations))
                    .setHeader(RequestViewSupport.headerWithTooltip(
                            validator.shortName(), validator.title(), validator.description()))
                    .setAutoWidth(true);
        }

        addSearchContextMenu(tvController);
    }

    /**
     * Per-row search actions. Items are added once and shown/hidden by row type via the dynamic content handler, since
     * {@link GridContextMenu} reuses one menu across all rows.
     */
    private void addSearchContextMenu(TvController tvController) {
        GridContextMenu<TvHierarchyRow> contextMenu = addContextMenu();
        GridMenuItem<TvHierarchyRow> searchAllSeasons =
                contextMenu.addItem("Search All Seasons", e -> e.getItem().ifPresent(row -> {
                    if (row instanceof ChildRow(TvChildRequest child)) {
                        runAsync("Searching all seasons…", () -> tvController.searchAllSeasonsForChild(child.getId()));
                    }
                }));
        GridMenuItem<TvHierarchyRow> searchAllEpisodes =
                contextMenu.addItem("Search All Episodes", e -> e.getItem().ifPresent(row -> {
                    switch (row) {
                        case ChildRow(TvChildRequest child) ->
                            runAsync(
                                    "Searching all episodes…",
                                    () -> tvController.searchAllEpisodesForChild(child.getId()));
                        case SeasonRow(TvSeasonRequest season) ->
                            runAsync(
                                    "Searching all episodes…",
                                    () -> tvController.searchAllEpisodesForSeason(season.getId()));
                        case EpisodeRow ignored -> {}
                    }
                }));
        GridMenuItem<TvHierarchyRow> searchSeason =
                contextMenu.addItem("Search Season", e -> e.getItem().ifPresent(row -> {
                    if (row instanceof SeasonRow(TvSeasonRequest season)) {
                        runAsync("Searching season…", () -> tvController.searchSeason(season.getId()));
                    }
                }));
        GridMenuItem<TvHierarchyRow> searchEpisode =
                contextMenu.addItem("Search Episode", e -> e.getItem().ifPresent(row -> {
                    if (row instanceof EpisodeRow(TvEpisodeRequest episode)) {
                        runAsync("Searching episode…", () -> tvController.searchEpisode(episode.getId()));
                    }
                }));
        GridMenuItem<TvHierarchyRow> deleteDownload =
                contextMenu.addItem("Delete Download", e -> e.getItem().ifPresent(row -> {
                    if (row instanceof EpisodeRow(TvEpisodeRequest episode)) {
                        runAsync(
                                "Deleting download…",
                                () -> tvController.deleteEpisodeDownloadAndSearch(episode.getId()));
                    }
                }));

        contextMenu.setDynamicContentHandler(row -> {
            if (row == null) {
                return false;
            }
            searchAllSeasons.setVisible(row instanceof ChildRow);
            searchAllEpisodes.setVisible(row instanceof ChildRow || row instanceof SeasonRow);
            searchSeason.setVisible(row instanceof SeasonRow);
            searchEpisode.setVisible(row instanceof EpisodeRow);
            // Deleting a download mutates Sonarr/the download client, so it's ADMIN-only (matches MovieRequestView
            // and the server-side @PreAuthorize on TvController#deleteEpisodeDownloadAndSearch).
            deleteDownload.setVisible(row instanceof EpisodeRow && SecurityUtils.isAdmin());
            return true;
        });
    }

    /** Runs a blocking Sonarr search/delete off the UI thread so the detail grid doesn't freeze. */
    private void runAsync(String workingMessage, Runnable action) {
        getUI().ifPresent(ui -> RequestViewSupport.runAsync(ui, log, workingMessage, action, null, uiTaskExecutor));
    }

    /**
     * Records an episode's queued-download info (keyed by its persistent id) and rolls its protocol up to the owning
     * season and child, so the Type badge can be shown at every level. Skipped unless the episode is in the Sonarr
     * queue and both numbers are known.
     */
    private void indexDownload(
            TvChildRequest child,
            TvSeasonRequest season,
            TvEpisodeRequest episode,
            Map<EpisodeKey, EpisodeDownload> episodeDownloads) {
        if (episode.getId() == null || season.getOmbiSeasonNumber() == null || episode.getOmbiEpisodeNumber() == null) {
            return;
        }
        EpisodeDownload download =
                episodeDownloads.get(new EpisodeKey(season.getOmbiSeasonNumber(), episode.getOmbiEpisodeNumber()));
        if (download == null) {
            return;
        }
        downloadByEpisodeId.put(episode.getId(), download);
        if (download.protocol() == null) {
            return;
        }
        if (season.getId() != null) {
            protocolsBySeasonId
                    .computeIfAbsent(season.getId(), k -> new HashSet<>())
                    .add(download.protocol());
        }
        if (child.getId() != null) {
            protocolsByChildId
                    .computeIfAbsent(child.getId(), k -> new HashSet<>())
                    .add(download.protocol());
        }
    }

    /**
     * Type cell: a {@link Badge} per Sonarr protocol — solid blue (default) for torrent, solid green for usenet — or a
     * dash when nothing under the row is in the Sonarr queue. Episodes carry their own protocol; seasons and children
     * carry the rolled-up protocols of their downloading descendants.
     */
    private Component typeComponent(TvHierarchyRow row) {
        Set<String> protocols =
                switch (row) {
                    case EpisodeRow(TvEpisodeRequest episode) -> {
                        EpisodeDownload download =
                                episode.getId() == null ? null : downloadByEpisodeId.get(episode.getId());
                        yield download == null || download.protocol() == null ? Set.of() : Set.of(download.protocol());
                    }
                    case SeasonRow(TvSeasonRequest season) ->
                        season.getId() == null ? Set.of() : protocolsBySeasonId.getOrDefault(season.getId(), Set.of());
                    case ChildRow(TvChildRequest child) ->
                        child.getId() == null ? Set.of() : protocolsByChildId.getOrDefault(child.getId(), Set.of());
                };
        return RequestViewSupport.protocolBadges(protocols);
    }

    /** Progress cell: a thin determinate bar with the percentage for a queued episode, else a dash. */
    private LitRenderer<TvHierarchyRow> progressRenderer() {
        return LitRenderer.<TvHierarchyRow>of(RequestViewSupport.progressCellTemplate(false))
                .withProperty("pct", this::progressPercent)
                .withProperty("label", this::progressText);
    }

    /** Numeric download percentage (0–100) for the Progress bar, or -1 when there's nothing to show. */
    private double progressPercent(TvHierarchyRow row) {
        EpisodeDownload download = downloadFor(row);
        return download == null
                ? -1
                : RequestViewSupport.progressPercentOf(download.protocol(), download.torrent(), download.slot());
    }

    /** Download progress for a queued episode: torrent progress or SABnzbd percentage, else a dash. */
    private String progressText(TvHierarchyRow row) {
        EpisodeDownload download = downloadFor(row);
        if (download == null || download.protocol() == null) {
            return "—";
        }
        if (RequestViewSupport.isTorrent(download.protocol())) {
            return download.torrent() == null
                    ? "—"
                    : RequestViewSupport.formatProgress(download.torrent().getProgress());
        }
        return download.slot() == null
                ? "—"
                : RequestViewSupport.formatPercentage(download.slot().getPercentage());
    }

    /** Peer/seed counts for a queued torrent episode, or a dash for usenet or episodes not in the queue. */
    private String peersText(TvHierarchyRow row) {
        EpisodeDownload download = downloadFor(row);
        if (download == null || !RequestViewSupport.isTorrent(download.protocol())) {
            return "—";
        }
        return download.torrent() == null ? "—" : RequestViewSupport.formatPeers(download.torrent());
    }

    private EpisodeDownload downloadFor(TvHierarchyRow row) {
        return row instanceof EpisodeRow(TvEpisodeRequest episode) && episode.getId() != null
                ? downloadByEpisodeId.get(episode.getId())
                : null;
    }

    private static Component episodeValidationComponent(
            TvHierarchyRow row, String validationName, Map<Long, Map<String, Validation>> latestEpisodeValidations) {
        Boolean result =
                switch (row) {
                    case EpisodeRow(TvEpisodeRequest episode) ->
                        episodeResult(episode, validationName, latestEpisodeValidations);
                    // A season rolls up its episodes; a child rolls up its seasons. Each is valid only
                    // when everything beneath it is validated and valid.
                    case SeasonRow(TvSeasonRequest season) ->
                        seasonResult(season, validationName, latestEpisodeValidations);
                    case ChildRow(TvChildRequest child) -> childResult(child, validationName, latestEpisodeValidations);
                };
        // null = nothing validated yet (unknown), not a failure.
        return result == null ? new Span("—") : resultIcon(result);
    }

    /**
     * Row-level roll-up for the parent grid's Sub-Validations column: TRUE only when every child (and thus every season
     * and episode beneath it) is validated and valid across all episode validators, null when nothing is validated yet,
     * FALSE otherwise. Uses the same per-child {@link #childResult} the tree displays, so the column and the expanded
     * rows agree.
     */
    static Boolean allChildrenValidation(
            List<TvChildRequest> children,
            List<EpisodeValidator> validators,
            Map<Long, Map<String, Validation>> latestEpisodeValidations) {
        List<Boolean> childResults = new ArrayList<>();
        for (TvChildRequest child : children) {
            for (EpisodeValidator validator : validators) {
                childResults.add(childResult(child, validator.getClass().getSimpleName(), latestEpisodeValidations));
            }
        }
        return rollUp(childResults);
    }

    private static Boolean episodeResult(
            TvEpisodeRequest episode,
            String validationName,
            Map<Long, Map<String, Validation>> latestEpisodeValidations) {
        Map<String, Validation> byName = latestEpisodeValidations.get(episode.getId());
        Validation v = byName == null ? null : byName.get(validationName);
        return v == null ? null : Boolean.TRUE.equals(v.getResult());
    }

    private static Boolean seasonResult(
            TvSeasonRequest season,
            String validationName,
            Map<Long, Map<String, Validation>> latestEpisodeValidations) {
        return rollUp(season.getEpisodeRequests().stream()
                .map(episode -> episodeResult(episode, validationName, latestEpisodeValidations))
                .toList());
    }

    private static Boolean childResult(
            TvChildRequest child, String validationName, Map<Long, Map<String, Validation>> latestEpisodeValidations) {
        return rollUp(child.getSeasonRequests().stream()
                .map(season -> seasonResult(season, validationName, latestEpisodeValidations))
                .toList());
    }

    /**
     * Aggregates tri-state results: null if none are known (unknown), TRUE only if every entry is known and valid,
     * FALSE otherwise (any failing or not-yet-validated entry).
     */
    private static Boolean rollUp(List<Boolean> results) {
        boolean anyKnown = false;
        boolean allValid = true;
        for (Boolean result : results) {
            if (result != null) {
                anyKnown = true;
            }
            if (!Boolean.TRUE.equals(result)) {
                allValid = false;
            }
        }
        return anyKnown ? allValid : null;
    }

    private static String idText(TvHierarchyRow row) {
        Long id =
                switch (row) {
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
            icon.getStyle().set("color", "var(--aura-green, green)");
            return icon;
        }
        Icon icon = VaadinIcon.CLOSE.create();
        icon.getStyle().set("color", "var(--aura-red, red)");
        return icon;
    }

    static Component placeholderWhenEmpty() {
        return new Span("No child requests yet — try Refresh");
    }
}
