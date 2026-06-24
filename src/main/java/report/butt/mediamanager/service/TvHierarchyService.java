package report.butt.mediamanager.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.jspecify.annotations.NullMarked;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import report.butt.mediamanager.model.TvChildRequest;
import report.butt.mediamanager.model.TvEpisodeRequest;
import report.butt.mediamanager.model.TvRequest;
import report.butt.mediamanager.model.TvSeasonRequest;
import report.butt.mediamanager.repository.TvChildRequestRepository;
import report.butt.mediamanager.repository.TvEpisodeRequestRepository;
import report.butt.mediamanager.repository.TvSeasonRequestRepository;

@Service
@NullMarked
public class TvHierarchyService {

    private static final Comparator<TvSeasonRequest> SEASON_ORDER =
            Comparator.comparing(TvSeasonRequest::getOmbiSeasonNumber, Comparator.nullsLast(Integer::compare));

    private static final Comparator<TvEpisodeRequest> EPISODE_ORDER =
            Comparator.comparing(TvEpisodeRequest::getOmbiEpisodeNumber, Comparator.nullsLast(Integer::compare));

    private final TvChildRequestRepository tvChildRequestRepository;
    private final TvSeasonRequestRepository tvSeasonRequestRepository;
    private final TvEpisodeRequestRepository tvEpisodeRequestRepository;

    public TvHierarchyService(
            TvChildRequestRepository tvChildRequestRepository,
            TvSeasonRequestRepository tvSeasonRequestRepository,
            TvEpisodeRequestRepository tvEpisodeRequestRepository) {
        this.tvChildRequestRepository = tvChildRequestRepository;
        this.tvSeasonRequestRepository = tvSeasonRequestRepository;
        this.tvEpisodeRequestRepository = tvEpisodeRequestRepository;
    }

    @Transactional(readOnly = true)
    public List<TvChildRequest> loadHierarchy(TvRequest parent) {
        List<TvChildRequest> children = tvChildRequestRepository.findByParentOrderByIdAsc(parent);
        for (TvChildRequest child : children) {
            List<TvSeasonRequest> seasons = child.getSeasonRequests();
            seasons.sort(SEASON_ORDER);
            for (TvSeasonRequest season : seasons) {
                season.getEpisodeRequests().sort(EPISODE_ORDER);
            }
        }
        return children;
    }

    /**
     * Loads every show's child hierarchy in three bulk queries (one per level) instead of one lazy load per show,
     * wiring each child's seasons and each season's episodes so callers can roll up over the object graph without N+1.
     * Read-only, so the collections we attach are never flushed back. Keyed by parent {@link TvRequest} id; shows with
     * no children are absent.
     */
    @Transactional(readOnly = true)
    public Map<Long, List<TvChildRequest>> loadAllHierarchies() {
        List<TvChildRequest> children = tvChildRequestRepository.findAll();
        if (children.isEmpty()) {
            return Map.of();
        }

        List<Long> childIds = children.stream().map(TvChildRequest::getId).toList();
        Map<Long, List<TvSeasonRequest>> seasonsByChild =
                tvSeasonRequestRepository.findByTvChildRequestIdIn(childIds).stream()
                        .collect(Collectors.groupingBy(s ->
                                Objects.requireNonNull(s.getTvChildRequest()).getId()));

        List<Long> seasonIds = seasonsByChild.values().stream()
                .flatMap(List::stream)
                .map(TvSeasonRequest::getId)
                .toList();
        Map<Long, List<TvEpisodeRequest>> episodesBySeason = seasonIds.isEmpty()
                ? Map.of()
                : tvEpisodeRequestRepository.findByTvSeasonRequestIdIn(seasonIds).stream()
                        .collect(Collectors.groupingBy(e ->
                                Objects.requireNonNull(e.getTvSeasonRequest()).getId()));

        for (TvChildRequest child : children) {
            List<TvSeasonRequest> seasons = seasonsByChild.getOrDefault(child.getId(), List.of());
            child.setSeasonRequests(seasons);
            for (TvSeasonRequest season : seasons) {
                season.setEpisodeRequests(episodesBySeason.getOrDefault(season.getId(), List.of()));
            }
        }

        return children.stream().collect(Collectors.groupingBy(c -> Objects.requireNonNull(c.getParent())
                .getId()));
    }

    /**
     * Loads every episode in the library grouped by its owning show's {@link TvRequest} id, using three bulk queries
     * (one per level) instead of one lazy load per show. Unlike {@link #loadAllHierarchies()} this never mutates the
     * entities' managed collections, so it is safe to call inside a read-write transaction (e.g. bulk validation)
     * without risking an orphan-removal flush. Shows with no episodes are absent from the map.
     */
    @Transactional(readOnly = true)
    public Map<Long, List<TvEpisodeRequest>> loadEpisodesByRequestId() {
        List<TvChildRequest> children = tvChildRequestRepository.findAll();
        if (children.isEmpty()) {
            return Map.of();
        }

        Map<Long, Long> parentByChildId = children.stream()
                .collect(Collectors.toMap(TvChildRequest::getId, c -> Objects.requireNonNull(c.getParent())
                        .getId()));

        List<Long> childIds = children.stream().map(TvChildRequest::getId).toList();
        List<TvSeasonRequest> seasons = tvSeasonRequestRepository.findByTvChildRequestIdIn(childIds);
        Map<Long, Long> childBySeasonId = seasons.stream()
                .collect(Collectors.toMap(TvSeasonRequest::getId, s -> Objects.requireNonNull(s.getTvChildRequest())
                        .getId()));

        List<Long> seasonIds = seasons.stream().map(TvSeasonRequest::getId).toList();
        List<TvEpisodeRequest> episodes =
                seasonIds.isEmpty() ? List.of() : tvEpisodeRequestRepository.findByTvSeasonRequestIdIn(seasonIds);

        Map<Long, List<TvEpisodeRequest>> episodesByRequestId = new HashMap<>();
        for (TvEpisodeRequest episode : episodes) {
            Long childId = childBySeasonId.get(
                    Objects.requireNonNull(episode.getTvSeasonRequest()).getId());
            Long parentId = childId == null ? null : parentByChildId.get(childId);
            if (parentId == null) {
                continue;
            }
            episodesByRequestId
                    .computeIfAbsent(parentId, k -> new ArrayList<>())
                    .add(episode);
        }
        return episodesByRequestId;
    }

    /** Flattens a show's hierarchy to its episodes, fully initialized within the transaction. */
    @Transactional(readOnly = true)
    public List<TvEpisodeRequest> loadEpisodes(TvRequest parent) {
        return loadHierarchy(parent).stream()
                .flatMap(child -> child.getSeasonRequests().stream())
                .flatMap(season -> season.getEpisodeRequests().stream())
                .toList();
    }
}
