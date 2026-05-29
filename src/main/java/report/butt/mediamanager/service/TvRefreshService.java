package report.butt.mediamanager.service;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
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
        List<OmbiTvRequest> ombiTvRequests = ombiClient.getTvRequests();
        List<Series> sonarrSeries = sonarrClient.getAllSeries();
        Set<String> validCacheKeys = new HashSet<>();

        ombiTvRequests.forEach(ombiTv -> {
            OmbiTvChildRequest firstChild = firstChild(ombiTv);
            TvRequest tvRequest = repository
                    .findByOmbiRequestId(ombiTv.getId())
                    .orElseGet(() -> new TvRequest(
                            ombiTv.getTitle(),
                            ombiTv.getTvDbId(),
                            firstChild == null ? null : firstChild.getAvailable(),
                            ombiTv.getId(),
                            firstChild == null ? null : firstChild.getRequestStatus()));

            Series series = sonarrSeries.stream()
                    .filter(s -> Objects.equals(s.getTvdbId(), ombiTv.getTvDbId()))
                    .findFirst()
                    .orElse(null);

            applyUpdates(tvRequest, ombiTv, series);
            if (series != null && series.getTvdbId() != null) {
                validCacheKeys.add(PlexClient.tvCacheKey(series.getTvdbId()));
            }
            tvRequest = repository.save(tvRequest);
            refreshChildren(tvRequest, ombiTv);
            log.info("Refreshed {}", tvRequest);
        });

        plexCacheService.cleanExcept("tv-", validCacheKeys);
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

        applyUpdates(tvRequest, ombiTv, series);
        tvRequest = repository.save(tvRequest);
        if (ombiTv != null) {
            refreshChildren(tvRequest, ombiTv);
        }
        log.info("Refreshed {} ({})", id, tvRequest.getTitle());
    }

    private void applyUpdates(TvRequest tvRequest, OmbiTvRequest ombiTv, Series series) {
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
            applyPlexUpdates(tvRequest, series);
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

    record EpisodeKey(int seasonNumber, int episodeNumber) {}

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
            log.warn("Ombi TV search lookup failed for externalProviderId {}", ombiTv.getExternalProviderId(), e);
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

    private void applyPlexUpdates(TvRequest tvRequest, Series series) {
        try {
            MetadataResult plexResult =
                    plexClient.getShowByTvdbId(series.getTvdbId(), series.getTitle(), series.getYear());
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
