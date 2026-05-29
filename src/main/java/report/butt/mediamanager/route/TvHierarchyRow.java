package report.butt.mediamanager.route;

import org.jspecify.annotations.NullMarked;
import report.butt.mediamanager.model.TvChildRequest;
import report.butt.mediamanager.model.TvEpisodeRequest;
import report.butt.mediamanager.model.TvSeasonRequest;

@NullMarked
public sealed interface TvHierarchyRow
        permits TvHierarchyRow.ChildRow, TvHierarchyRow.SeasonRow, TvHierarchyRow.EpisodeRow {

    record ChildRow(TvChildRequest child) implements TvHierarchyRow {}

    record SeasonRow(TvSeasonRequest season) implements TvHierarchyRow {}

    record EpisodeRow(TvEpisodeRequest episode) implements TvHierarchyRow {}
}
