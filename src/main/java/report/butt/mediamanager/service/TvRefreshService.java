package report.butt.mediamanager.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import report.butt.mediamanager.client.MetadataResult;
import report.butt.mediamanager.client.OmbiClient;
import report.butt.mediamanager.client.PlexClient;
import report.butt.mediamanager.client.SonarrClient;
import report.butt.mediamanager.exceptions.RequestNotFoundException;
import report.butt.mediamanager.model.TvChildRequest;
import report.butt.mediamanager.model.TvEpisodeRequest;
import report.butt.mediamanager.model.TvRequest;
import report.butt.mediamanager.model.TvSeasonRequest;
import report.butt.mediamanager.model.ombi.OmbiTvChildRequest;
import report.butt.mediamanager.model.ombi.OmbiTvEpisode;
import report.butt.mediamanager.model.ombi.OmbiTvRequest;
import report.butt.mediamanager.model.ombi.OmbiTvSearchResult;
import report.butt.mediamanager.model.ombi.OmbiTvSeasonRequest;
import report.butt.mediamanager.model.plex.EpisodeKey;
import report.butt.mediamanager.model.plex.PlexMedia;
import report.butt.mediamanager.model.plex.PlexMetadata;
import report.butt.mediamanager.model.plex.PlexPart;
import report.butt.mediamanager.model.sonarr.Series;
import report.butt.mediamanager.repository.TvChildRequestRepository;
import report.butt.mediamanager.repository.TvEpisodeRequestRepository;
import report.butt.mediamanager.repository.TvRequestRepository;
import report.butt.mediamanager.repository.TvSeasonRequestRepository;

@Service
public class TvRefreshService {

    private static final Logger log = LoggerFactory.getLogger(TvRefreshService.class);

    private final TvRequestRepository repository;
    private final TvChildRequestRepository childRepository;
    private final TvSeasonRequestRepository seasonRepository;
    private final TvEpisodeRequestRepository episodeRepository;
    private final OmbiClient ombiClient;
    private final SonarrClient sonarrClient;
    private final PlexClient plexClient;
    private final PlexCacheService plexCacheService;

    public TvRefreshService(
            TvRequestRepository repository,
            TvChildRequestRepository childRepository,
            TvSeasonRequestRepository seasonRepository,
            TvEpisodeRequestRepository episodeRepository,
            OmbiClient ombiClient,
            SonarrClient sonarrClient,
            PlexClient plexClient,
            PlexCacheService plexCacheService) {
        this.repository = repository;
        this.childRepository = childRepository;
        this.seasonRepository = seasonRepository;
        this.episodeRepository = episodeRepository;
        this.ombiClient = ombiClient;
        this.sonarrClient = sonarrClient;
        this.plexClient = plexClient;
        this.plexCacheService = plexCacheService;
    }

    @Transactional
    public void refreshAll() {
        // Phase 1 — Fetch: four external calls plus four flat preload queries, all independent of
        // the show count.
        List<OmbiTvRequest> ombiTvRequests = ombiClient.getTvRequests();
        List<Series> sonarrSeries = sonarrClient.getAllSeries();
        Map<Integer, PlexMetadata> showsByTvdb = plexClient.getAllShowsIndexedByTvdb();
        Map<String, Map<EpisodeKey, String>> episodesByShow = plexClient.getAllEpisodesIndexedByShow();

        Map<Integer, Series> sonarrByTvdb = sonarrSeries.stream()
                .filter(s -> s.getTvdbId() != null)
                .collect(Collectors.toMap(Series::getTvdbId, Function.identity(), (a, b) -> a));

        List<Integer> parentOmbiIds = ombiTvRequests.stream()
                .map(OmbiTvRequest::getId)
                .filter(Objects::nonNull)
                .toList();
        Map<Integer, TvRequest> existingByOmbiId = parentOmbiIds.isEmpty()
                ? Map.of()
                : repository.findByOmbiRequestIdIn(parentOmbiIds).stream()
                        .collect(Collectors.toMap(TvRequest::getOmbiRequestId, Function.identity(), (a, b) -> a));

        List<Integer> childOmbiIds = ombiTvRequests.stream()
                .filter(t -> t.getChildRequests() != null)
                .flatMap(t -> t.getChildRequests().stream())
                .map(OmbiTvChildRequest::getId)
                .filter(Objects::nonNull)
                .toList();
        Map<Integer, TvChildRequest> existingChildrenByOmbiId = childOmbiIds.isEmpty()
                ? Map.of()
                : childRepository.findByOmbiRequestIdIn(childOmbiIds).stream()
                        .collect(Collectors.toMap(TvChildRequest::getOmbiRequestId, Function.identity(), (a, b) -> a));

        List<Long> childIds = existingChildrenByOmbiId.values().stream()
                .map(TvChildRequest::getId)
                .filter(Objects::nonNull)
                .toList();
        Map<Long, Map<Integer, TvSeasonRequest>> seasonsByChild = new HashMap<>();
        List<TvSeasonRequest> preloadedSeasons =
                childIds.isEmpty() ? List.of() : seasonRepository.findByTvChildRequestIdIn(childIds);
        for (TvSeasonRequest season : preloadedSeasons) {
            Long childId = season.getTvChildRequest() == null ? null : season.getTvChildRequest().getId();
            if (childId == null || season.getOmbiSeasonNumber() == null) {
                continue;
            }
            seasonsByChild.computeIfAbsent(childId, k -> new HashMap<>()).put(season.getOmbiSeasonNumber(), season);
        }

        List<Long> seasonIds = preloadedSeasons.stream()
                .map(TvSeasonRequest::getId)
                .filter(Objects::nonNull)
                .toList();
        Map<Long, Map<Integer, TvEpisodeRequest>> episodesBySeason = new HashMap<>();
        List<TvEpisodeRequest> preloadedEpisodes =
                seasonIds.isEmpty() ? List.of() : episodeRepository.findByTvSeasonRequestIdIn(seasonIds);
        for (TvEpisodeRequest episode : preloadedEpisodes) {
            Long seasonId = episode.getTvSeasonRequest() == null ? null : episode.getTvSeasonRequest().getId();
            if (seasonId == null || episode.getOmbiEpisodeNumber() == null) {
                continue;
            }
            episodesBySeason
                    .computeIfAbsent(seasonId, k -> new HashMap<>())
                    .put(episode.getOmbiEpisodeNumber(), episode);
        }

        // Phases 2 & 3 — Match (in-memory join) and Apply + change-detect.
        List<TvRequest> toSaveRequests = new ArrayList<>();
        List<TvChildRequest> toSaveChildren = new ArrayList<>();
        List<TvSeasonRequest> toSaveSeasons = new ArrayList<>();
        List<TvEpisodeRequest> toSaveEpisodes = new ArrayList<>();
        Set<String> validCacheKeys = new HashSet<>();
        int unchanged = 0;

        for (OmbiTvRequest ombiTv : ombiTvRequests) {
            OmbiTvChildRequest firstChild = firstChild(ombiTv);
            TvRequest existing = existingByOmbiId.get(ombiTv.getId());
            TvRequest tvRequest = existing != null
                    ? existing
                    : new TvRequest(
                            ombiTv.getTitle(),
                            ombiTv.getTvDbId(),
                            firstChild == null ? null : firstChild.getAvailable(),
                            ombiTv.getId(),
                            firstChild == null ? null : firstChild.getRequestStatus());

            Series series = ombiTv.getTvDbId() == null ? null : sonarrByTvdb.get(ombiTv.getTvDbId());

            Integer beforeHash = existing == null ? null : tvRequest.hashCode();
            applyUpdates(tvRequest, ombiTv, series, showsByTvdb);
            if (series != null && series.getTvdbId() != null) {
                validCacheKeys.add(PlexClient.tvCacheKey(series.getTvdbId()));
            }
            if (beforeHash == null || beforeHash != tvRequest.hashCode()) {
                toSaveRequests.add(tvRequest);
                log.info("Refreshed {}", tvRequest);
            } else {
                unchanged++;
            }

            unchanged += applyChildren(
                    tvRequest,
                    ombiTv,
                    existingChildrenByOmbiId,
                    seasonsByChild,
                    episodesBySeason,
                    resolveEpisodePaths(tvRequest, episodesByShow),
                    toSaveChildren,
                    toSaveSeasons,
                    toSaveEpisodes);
        }

        // Phase 4 — Persist changed rows in FK dependency order, then prune stale cache files.
        if (!toSaveRequests.isEmpty()) {
            repository.saveAll(toSaveRequests);
        }
        if (!toSaveChildren.isEmpty()) {
            childRepository.saveAll(toSaveChildren);
        }
        if (!toSaveSeasons.isEmpty()) {
            seasonRepository.saveAll(toSaveSeasons);
        }
        if (!toSaveEpisodes.isEmpty()) {
            episodeRepository.saveAll(toSaveEpisodes);
        }

        plexCacheService.cleanExcept("tv-", validCacheKeys);
        log.info(
                "TV refresh complete: {}/{}/{}/{} saved, {} unchanged",
                toSaveRequests.size(),
                toSaveChildren.size(),
                toSaveSeasons.size(),
                toSaveEpisodes.size(),
                unchanged);
    }

    private Map<EpisodeKey, String> resolveEpisodePaths(
            TvRequest tvRequest, Map<String, Map<EpisodeKey, String>> episodesByShow) {
        String ratingKey = tvRequest.getPlexMetadataId();
        if (ratingKey == null) {
            return Map.of();
        }
        return episodesByShow.getOrDefault(ratingKey, Map.of());
    }

    /**
     * Applies and change-detects the child/season/episode hierarchy for one show using preloaded
     * rows, appending changed entities to the supplied save lists. Returns the number of unchanged
     * entities skipped.
     */
    private int applyChildren(
            TvRequest tvRequest,
            OmbiTvRequest ombiTv,
            Map<Integer, TvChildRequest> existingChildrenByOmbiId,
            Map<Long, Map<Integer, TvSeasonRequest>> seasonsByChild,
            Map<Long, Map<Integer, TvEpisodeRequest>> episodesBySeason,
            Map<EpisodeKey, String> episodePaths,
            List<TvChildRequest> toSaveChildren,
            List<TvSeasonRequest> toSaveSeasons,
            List<TvEpisodeRequest> toSaveEpisodes) {
        if (ombiTv.getChildRequests() == null) {
            return 0;
        }
        int unchanged = 0;
        for (OmbiTvChildRequest ombiChild : ombiTv.getChildRequests()) {
            TvChildRequest existingChild = existingChildrenByOmbiId.get(ombiChild.getId());
            TvChildRequest child = existingChild != null
                    ? existingChild
                    : new TvChildRequest(
                            tvRequest,
                            ombiChild.getTitle(),
                            ombiTv.getTvDbId(),
                            ombiChild.getAvailable(),
                            ombiChild.getId(),
                            ombiChild.getRequestStatus());

            Integer beforeHash = existingChild == null ? null : child.hashCode();
            applyChildUpdates(child, tvRequest, ombiTv, ombiChild);
            if (beforeHash == null || beforeHash != child.hashCode()) {
                toSaveChildren.add(child);
            } else {
                unchanged++;
            }

            Map<Integer, TvSeasonRequest> existingSeasons = existingChild == null || existingChild.getId() == null
                    ? Map.of()
                    : seasonsByChild.getOrDefault(existingChild.getId(), Map.of());
            unchanged += applySeasons(
                    child, ombiChild, existingSeasons, episodesBySeason, episodePaths, toSaveSeasons, toSaveEpisodes);
        }
        return unchanged;
    }

    private int applySeasons(
            TvChildRequest child,
            OmbiTvChildRequest ombiChild,
            Map<Integer, TvSeasonRequest> existingSeasons,
            Map<Long, Map<Integer, TvEpisodeRequest>> episodesBySeason,
            Map<EpisodeKey, String> episodePaths,
            List<TvSeasonRequest> toSaveSeasons,
            List<TvEpisodeRequest> toSaveEpisodes) {
        if (ombiChild.getSeasonRequests() == null) {
            return 0;
        }
        int unchanged = 0;
        for (OmbiTvSeasonRequest ombiSeason : ombiChild.getSeasonRequests()) {
            boolean allEpisodesAvailable = allEpisodesAvailable(ombiSeason);
            TvSeasonRequest existingSeason =
                    ombiSeason.getSeasonNumber() == null ? null : existingSeasons.get(ombiSeason.getSeasonNumber());
            TvSeasonRequest season = existingSeason != null
                    ? existingSeason
                    : new TvSeasonRequest(
                            child, ombiSeason.getId(), ombiSeason.getSeasonNumber(), allEpisodesAvailable);

            Integer beforeHash = existingSeason == null ? null : season.hashCode();
            season.setTvChildRequest(child);
            season.setOmbiSeasonRequestId(ombiSeason.getId());
            season.setOmbiSeasonNumber(ombiSeason.getSeasonNumber());
            season.setOmbiSeasonAvailable(allEpisodesAvailable);
            if (beforeHash == null || beforeHash != season.hashCode()) {
                toSaveSeasons.add(season);
            } else {
                unchanged++;
            }

            Map<Integer, TvEpisodeRequest> existingEpisodes = existingSeason == null || existingSeason.getId() == null
                    ? Map.of()
                    : episodesBySeason.getOrDefault(existingSeason.getId(), Map.of());
            unchanged += refreshEpisodes(season, ombiSeason, existingEpisodes, episodePaths, toSaveEpisodes);
        }
        return unchanged;
    }

    private int refreshEpisodes(
            TvSeasonRequest season,
            OmbiTvSeasonRequest ombiSeason,
            Map<Integer, TvEpisodeRequest> existingEpisodes,
            Map<EpisodeKey, String> episodePaths,
            List<TvEpisodeRequest> toSaveEpisodes) {
        if (ombiSeason.getEpisodes() == null) {
            return 0;
        }
        int unchanged = 0;
        for (OmbiTvEpisode ombiEpisode : ombiSeason.getEpisodes()) {
            TvEpisodeRequest existingEpisode = ombiEpisode.getEpisodeNumber() == null
                    ? null
                    : existingEpisodes.get(ombiEpisode.getEpisodeNumber());
            TvEpisodeRequest episode = existingEpisode != null
                    ? existingEpisode
                    : new TvEpisodeRequest(season, ombiEpisode.getId(), ombiEpisode.getEpisodeNumber());

            Integer beforeHash = existingEpisode == null ? null : episode.hashCode();
            applyEpisodeUpdates(episode, season, ombiEpisode, episodePaths);
            if (beforeHash == null || beforeHash != episode.hashCode()) {
                toSaveEpisodes.add(episode);
            } else {
                unchanged++;
            }
        }
        return unchanged;
    }

    @Transactional
    public void refreshOne(Long id) {
        TvRequest tvRequest = repository.findById(id).orElseThrow(() -> new RequestNotFoundException(id));

        Integer ombiRequestId = tvRequest.getOmbiRequestId();
        OmbiTvRequest ombiTv = ombiRequestId == null
                ? null
                : ombiClient.getTvRequests().stream()
                        .filter(t -> ombiRequestId.equals(t.getId()))
                        .findFirst()
                        .orElse(null);

        Integer tvdbId = ombiTv != null ? ombiTv.getTvDbId() : null;
        Series series = null;
        if (tvdbId != null) {
            List<Series> sonarrSeries = sonarrClient.getSeriesByTvdbId(tvdbId);
            if (!sonarrSeries.isEmpty()) {
                series = sonarrSeries.get(0);
            }
        }

        applyUpdates(tvRequest, ombiTv, series, null);
        tvRequest = repository.save(tvRequest);
        if (ombiTv != null) {
            refreshChildren(tvRequest, ombiTv);
        }
        log.info("Refreshed {} ({})", id, tvRequest.getTitle());
    }

    private void applyUpdates(
            TvRequest tvRequest, OmbiTvRequest ombiTv, Series series, Map<Integer, PlexMetadata> showsByTvdb) {
        if (ombiTv != null) {
            backfillTotalSeasons(ombiTv);
            OmbiTvChildRequest firstChild = firstChild(ombiTv);
            String ombiUserName = firstChild == null || firstChild.getRequestedUser() == null
                    ? null
                    : firstChild.getRequestedUser().getUserName();
            tvRequest.setTitle(ombiTv.getTitle());
            tvRequest.setTvdbId(ombiTv.getTvDbId());
            tvRequest.setOmbiAvailable(firstChild == null ? null : firstChild.getAvailable());
            tvRequest.setOmbiRequestStatus(firstChild == null ? null : firstChild.getRequestStatus());
            tvRequest.setOmbiUserName(ombiUserName);
            tvRequest.setOmbiExternalProviderId(ombiTv.getExternalProviderId());
            tvRequest.setOmbiTotalSeasons(ombiTv.getTotalSeasons());
        }

        if (series != null) {
            tvRequest.setSonarrSeriesId(series.getId());
            tvRequest.setSonarrMonitored(series.getMonitored());
            tvRequest.setSonarrPath(series.getPath());
            tvRequest.setSonarrRootFolderPath(series.getRootFolderPath());
            tvRequest.setSonarrOriginalLanguage(
                    series.getOriginalLanguage() == null
                            ? null
                            : series.getOriginalLanguage().getName());
            if (series.getStatistics() != null) {
                tvRequest.setSonarrEpisodeFileCount(series.getStatistics().getEpisodeFileCount());
                tvRequest.setSonarrEpisodeCount(series.getStatistics().getEpisodeCount());
                tvRequest.setSonarrTotalEpisodeCount(series.getStatistics().getTotalEpisodeCount());
            }
            applyPlexUpdates(tvRequest, series, showsByTvdb);
        }
    }

    private void refreshChildren(TvRequest tvRequest, OmbiTvRequest ombiTv) {
        if (ombiTv.getChildRequests() == null) {
            return;
        }
        Map<EpisodeKey, String> episodePaths = loadEpisodePaths(tvRequest);
        ombiTv.getChildRequests().forEach(ombiChild -> {
            TvChildRequest child = childRepository
                    .findByOmbiRequestId(ombiChild.getId())
                    .orElseGet(() -> new TvChildRequest(
                            tvRequest,
                            ombiChild.getTitle(),
                            ombiTv.getTvDbId(),
                            ombiChild.getAvailable(),
                            ombiChild.getId(),
                            ombiChild.getRequestStatus()));
            applyChildUpdates(child, tvRequest, ombiTv, ombiChild);
            TvChildRequest savedChild = childRepository.save(child);
            refreshSeasons(savedChild, ombiChild, episodePaths);
        });
    }

    private Map<EpisodeKey, String> loadEpisodePaths(TvRequest tvRequest) {
        String plexMetadataId = tvRequest.getPlexMetadataId();
        if (plexMetadataId == null) {
            return Map.of();
        }
        try {
            List<PlexMetadata> grandchildren = plexClient.getShowGrandchildren(plexMetadataId);
            Map<EpisodeKey, String> result = new HashMap<>();
            for (PlexMetadata episode : grandchildren) {
                Integer seasonNumber = episode.getParentIndex();
                Integer episodeNumber = episode.getIndex();
                if (seasonNumber == null || episodeNumber == null) {
                    continue;
                }
                String file = firstFile(episode);
                if (file != null) {
                    result.put(new EpisodeKey(seasonNumber, episodeNumber), file);
                }
            }
            return result;
        } catch (Exception e) {
            log.warn("Plex grandchildren lookup failed for plexMetadataId {}", plexMetadataId, e);
            return Map.of();
        }
    }

    private static String firstFile(PlexMetadata episode) {
        if (episode.getMedia() == null || episode.getMedia().isEmpty()) {
            return null;
        }
        PlexMedia media = episode.getMedia().get(0);
        if (media.getPart() == null || media.getPart().isEmpty()) {
            return null;
        }
        return media.getPart().get(0).getFile();
    }

    private void applyChildUpdates(
            TvChildRequest child, TvRequest tvRequest, OmbiTvRequest ombiTv, OmbiTvChildRequest ombiChild) {
        String ombiUserName = ombiChild.getRequestedUser() == null
                ? null
                : ombiChild.getRequestedUser().getUserName();
        child.setParent(tvRequest);
        child.setOmbiParentRequestId(ombiChild.getParentRequestId());
        child.setTitle(ombiChild.getTitle());
        child.setTvdbId(ombiTv.getTvDbId());
        child.setOmbiRequestId(ombiChild.getId());
        child.setOmbiAvailable(ombiChild.getAvailable());
        child.setOmbiRequestStatus(ombiChild.getRequestStatus());
        child.setOmbiUserName(ombiUserName);
        child.setOmbiTotalSeasons(ombiTv.getTotalSeasons());
    }

    private void refreshSeasons(
            TvChildRequest child, OmbiTvChildRequest ombiChild, Map<EpisodeKey, String> episodePaths) {
        if (ombiChild.getSeasonRequests() == null) {
            return;
        }
        ombiChild.getSeasonRequests().forEach(ombiSeason -> {
            boolean allEpisodesAvailable = allEpisodesAvailable(ombiSeason);
            TvSeasonRequest season = seasonRepository
                    .findByTvChildRequestIdAndOmbiSeasonNumber(child.getId(), ombiSeason.getSeasonNumber())
                    .orElseGet(() -> new TvSeasonRequest(
                            child, ombiSeason.getId(), ombiSeason.getSeasonNumber(), allEpisodesAvailable));
            season.setTvChildRequest(child);
            season.setOmbiSeasonRequestId(ombiSeason.getId());
            season.setOmbiSeasonNumber(ombiSeason.getSeasonNumber());
            season.setOmbiSeasonAvailable(allEpisodesAvailable);
            TvSeasonRequest savedSeason = seasonRepository.save(season);
            refreshEpisodes(savedSeason, ombiSeason, episodePaths);
        });
    }

    private static boolean allEpisodesAvailable(OmbiTvSeasonRequest ombiSeason) {
        List<OmbiTvEpisode> episodes = ombiSeason.getEpisodes();
        if (episodes == null || episodes.isEmpty()) {
            return false;
        }
        return episodes.stream().allMatch(episode -> Boolean.TRUE.equals(episode.getAvailable()));
    }

    private void refreshEpisodes(
            TvSeasonRequest season, OmbiTvSeasonRequest ombiSeason, Map<EpisodeKey, String> episodePaths) {
        if (ombiSeason.getEpisodes() == null) {
            return;
        }
        ombiSeason.getEpisodes().forEach(ombiEpisode -> {
            TvEpisodeRequest episode = episodeRepository
                    .findByTvSeasonRequestIdAndOmbiEpisodeNumber(season.getId(), ombiEpisode.getEpisodeNumber())
                    .orElseGet(() -> new TvEpisodeRequest(season, ombiEpisode.getId(), ombiEpisode.getEpisodeNumber()));
            applyEpisodeUpdates(episode, season, ombiEpisode, episodePaths);
            episodeRepository.save(episode);
        });
    }

    private void applyEpisodeUpdates(
            TvEpisodeRequest episode,
            TvSeasonRequest season,
            OmbiTvEpisode ombiEpisode,
            Map<EpisodeKey, String> episodePaths) {
        episode.setTvSeasonRequest(season);
        episode.setOmbiEpisodeId(ombiEpisode.getId());
        episode.setOmbiEpisodeNumber(ombiEpisode.getEpisodeNumber());
        episode.setOmbiTitle(ombiEpisode.getTitle());
        episode.setOmbiAvailable(ombiEpisode.getAvailable());
        episode.setOmbiApproved(ombiEpisode.getApproved());
        episode.setOmbiRequested(ombiEpisode.getRequested());
        episode.setOmbiRequestStatus(ombiEpisode.getRequestStatus());
        if (season.getOmbiSeasonNumber() != null && ombiEpisode.getEpisodeNumber() != null) {
            String path = episodePaths.get(
                    new EpisodeKey(season.getOmbiSeasonNumber(), ombiEpisode.getEpisodeNumber()));
            if (path != null) {
                episode.setSonarrPath(path);
            }
        }
    }

    private void backfillTotalSeasons(OmbiTvRequest ombiTv) {
        if (!Integer.valueOf(0).equals(ombiTv.getTotalSeasons()) || ombiTv.getExternalProviderId() == null) {
            return;
        }
        try {
            OmbiTvSearchResult search = ombiClient.searchTv(ombiTv.getExternalProviderId());
            if (search != null && search.getSeasonRequests() != null) {
                ombiTv.setTotalSeasons(search.getSeasonRequests().size());
            }
        } catch (Exception e) {
            // The Ombi search endpoint can return a 500 (e.g. when its upstream metadata provider
            // returns a gzip body Ombi fails to parse). This is external and unfixable here, and
            // totalSeasons is best-effort enrichment, so log concisely and continue rather than
            // dumping a stack trace for an expected, handled condition.
            log.warn(
                    "Ombi TV search lookup failed for externalProviderId {}: {}",
                    ombiTv.getExternalProviderId(),
                    e.getMessage());
        }
    }

    private OmbiTvChildRequest firstChild(OmbiTvRequest ombiTv) {
        if (ombiTv == null
                || ombiTv.getChildRequests() == null
                || ombiTv.getChildRequests().isEmpty()) {
            return null;
        }
        return ombiTv.getChildRequests().get(0);
    }

    private void applyPlexUpdates(TvRequest tvRequest, Series series, Map<Integer, PlexMetadata> showsByTvdb) {
        try {
            MetadataResult plexResult;
            if (showsByTvdb != null) {
                PlexMetadata prefetched = series.getTvdbId() == null ? null : showsByTvdb.get(series.getTvdbId());
                String cacheUrl = plexClient.cacheTvMetadata(series.getTvdbId(), prefetched);
                plexResult = new MetadataResult(cacheUrl, prefetched);
            } else {
                plexResult = plexClient.getShowByTvdbId(series.getTvdbId(), series.getTitle(), series.getYear());
            }
            tvRequest.setPlexMetadataUrl(plexResult.url());
            PlexMetadata plexMetadata = plexResult.metadata();
            if (plexMetadata != null) {
                log.info(
                        "Plex match found for tvdbId {}: {}",
                        series.getTvdbId(),
                        plexMetadata.getTitle());
                tvRequest.setPlexMetadataId(plexMetadata.getRatingKey());
                tvRequest.setPlexAddedAt(plexMetadata.getAddedAt());
                tvRequest.setPlexUpdatedAt(plexMetadata.getUpdatedAt());
                if (plexMetadata.getGuids() != null) {
                    plexMetadata.getGuids().stream()
                            .map(g -> g.getId())
                            .filter(id -> id != null && id.startsWith("tvdb://"))
                            .map(id -> id.substring("tvdb://".length()))
                            .mapToInt(Integer::parseInt)
                            .findFirst()
                            .ifPresent(tvRequest::setPlexTvdbId);
                }
                if (plexMetadata.getMedia() != null && !plexMetadata.getMedia().isEmpty()) {
                    PlexMedia media = plexMetadata.getMedia().get(0);
                    tvRequest.setPlexMediaId(media.getId());
                    tvRequest.setPlexMediaDuration(media.getDuration());
                    if (media.getPart() != null && !media.getPart().isEmpty()) {
                        PlexPart part = media.getPart().get(0);
                        tvRequest.setPlexMediaFilename(part.getFile());
                        tvRequest.setPlexMediaSize(part.getSize());
                    }
                }
            } else {
                log.info("No Plex match found for tvdbId {}", series.getTvdbId());
            }
        } catch (Exception e) {
            log.warn("Plex lookup failed for tvdbId {}", series.getTvdbId(), e);
        }
    }
}
