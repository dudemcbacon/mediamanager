package report.butt.mediamanager.controller;

import java.time.Instant;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import org.jobrunr.scheduling.JobRequestScheduler;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import report.butt.mediamanager.client.OmbiClient;
import report.butt.mediamanager.client.SonarrClient;
import report.butt.mediamanager.exceptions.RequestNotFoundException;
import report.butt.mediamanager.job.FfprobeScanJobRequest;
import report.butt.mediamanager.model.FfprobeScan;
import report.butt.mediamanager.model.Note;
import report.butt.mediamanager.model.TvChildRequest;
import report.butt.mediamanager.model.TvEpisodeRequest;
import report.butt.mediamanager.model.TvRequest;
import report.butt.mediamanager.model.TvSeasonRequest;
import report.butt.mediamanager.model.ombi.OmbiReprocessResponse;
import report.butt.mediamanager.model.plex.EpisodeKey;
import report.butt.mediamanager.model.sonarr.Episode;
import report.butt.mediamanager.model.sonarr.SonarrCommand;
import report.butt.mediamanager.model.sonarr.SonarrHealthItem;
import report.butt.mediamanager.model.sonarr.SonarrQueue;
import report.butt.mediamanager.model.sonarr.SonarrQueueRecord;
import report.butt.mediamanager.repository.TvChildRequestRepository;
import report.butt.mediamanager.repository.TvEpisodeRequestRepository;
import report.butt.mediamanager.repository.TvRequestRepository;
import report.butt.mediamanager.repository.TvSeasonRequestRepository;
import report.butt.mediamanager.service.FfprobeScanService;
import report.butt.mediamanager.service.RequestAdminService;
import report.butt.mediamanager.service.TvRefreshService;
import report.butt.mediamanager.service.ValidatorService;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

@Controller
@NullMarked
public class TvController {

    private static final Logger log = LoggerFactory.getLogger(TvController.class);
    private static final String ANY_QUALITY_PROFILE = "Any";

    private final TvRequestRepository tvRequestRepository;
    private final TvChildRequestRepository tvChildRequestRepository;
    private final TvSeasonRequestRepository tvSeasonRequestRepository;
    private final TvEpisodeRequestRepository tvEpisodeRequestRepository;
    private final OmbiClient ombiClient;
    private final SonarrClient sonarrClient;
    private final ObjectMapper objectMapper;
    private final TvRefreshService tvRefreshService;
    private final ValidatorService validatorService;
    private final RequestAdminService requestAdminService;
    private final FfprobeScanService ffprobeScanService;
    private final JobRequestScheduler jobRequestScheduler;

    // Spring constructor injection; the parameter count reflects injected collaborators, not a design smell.
    @SuppressWarnings("TooManyParameters")
    @Autowired
    public TvController(
            TvRequestRepository tvRequestRepository,
            TvChildRequestRepository tvChildRequestRepository,
            TvSeasonRequestRepository tvSeasonRequestRepository,
            TvEpisodeRequestRepository tvEpisodeRequestRepository,
            OmbiClient ombiClient,
            SonarrClient sonarrClient,
            ObjectMapper objectMapper,
            TvRefreshService tvRefreshService,
            ValidatorService validatorService,
            RequestAdminService requestAdminService,
            FfprobeScanService ffprobeScanService,
            JobRequestScheduler jobRequestScheduler) {
        this.tvRequestRepository = tvRequestRepository;
        this.tvChildRequestRepository = tvChildRequestRepository;
        this.tvSeasonRequestRepository = tvSeasonRequestRepository;
        this.tvEpisodeRequestRepository = tvEpisodeRequestRepository;
        this.ombiClient = ombiClient;
        this.sonarrClient = sonarrClient;
        this.objectMapper = objectMapper;
        this.tvRefreshService = tvRefreshService;
        this.validatorService = validatorService;
        this.requestAdminService = requestAdminService;
        this.ffprobeScanService = ffprobeScanService;
        this.jobRequestScheduler = jobRequestScheduler;
    }

    @PostMapping("/tv/refresh-all")
    public String refreshAll() {
        log.info("Refresh-all request");
        tvRefreshService.refreshAll();
        return "redirect:/tv";
    }

    @PostMapping("/tv/search-missing")
    public String searchMissing() {
        List<TvRequest> tvRequests = tvRequestRepository.findAll().stream()
                .filter(tvRequest -> Objects.equals(tvRequest.getOmbiRequestStatus(), "Common.ProcessingRequest")
                        && tvRequest.getSonarrEpisodeFileCount() != null
                        && tvRequest.getSonarrTotalEpisodeCount() != null
                        && tvRequest.getSonarrTotalEpisodeCount() > 0
                        && tvRequest.getSonarrEpisodeFileCount() < tvRequest.getSonarrTotalEpisodeCount()
                        && tvRequest.getSonarrSeriesId() != null)
                .toList();

        List<Integer> seriesIds =
                tvRequests.stream().map(TvRequest::getSonarrSeriesId).toList();

        log.info("Triggering Sonarr SeriesSearch for {} series: {}", seriesIds.size(), seriesIds);
        if (!seriesIds.isEmpty()) {
            SonarrCommand command = sonarrClient.searchSeries(seriesIds);
            log.info(
                    "Sonarr command {} ({}) status={} result={}",
                    command.getId(),
                    command.getCommandName(),
                    command.getStatus(),
                    command.getResult());

            Instant now = Instant.now();
            tvRequests.forEach(tvRequest -> tvRequest.setSonarrLastSearched(now));
            tvRequestRepository.saveAll(tvRequests);
        }

        return "redirect:/tv";
    }

    @GetMapping("/tv")
    public String tv(Model model) {
        List<TvRequest> tvRequests = tvRequestRepository.findAll().stream()
                .sorted(Comparator.comparing(
                        TvRequest::getSonarrSeriesId, Comparator.nullsFirst(Comparator.naturalOrder())))
                .toList();

        model.addAttribute("tvRequests", tvRequests);
        return "tv";
    }

    @PostMapping("/tv/{seriesId}/search")
    public String search(@PathVariable Integer seriesId) {
        log.info("Triggering Sonarr SeriesSearch for series {}", seriesId);
        SonarrCommand command = sonarrClient.searchSeries(List.of(seriesId));
        log.info(
                "Sonarr command {} ({}) status={} result={}",
                command.getId(),
                command.getCommandName(),
                command.getStatus(),
                command.getResult());

        Instant now = Instant.now();
        tvRequestRepository.findAll().stream()
                .filter(tvRequest -> seriesId.equals(tvRequest.getSonarrSeriesId()))
                .forEach(tvRequest -> {
                    tvRequest.setSonarrLastSearched(now);
                    tvRequestRepository.save(tvRequest);
                });

        return "redirect:/tv";
    }

    @PostMapping("/tv/{id}/refresh")
    public String refresh(@PathVariable Long id) {
        log.info("Refresh request for tv request {}", id);
        tvRefreshService.refreshOne(id);
        return "redirect:/tv";
    }

    @PostMapping("/tv/{id}/quality-profile-any")
    @PreAuthorize("hasRole('ADMIN')")
    public String setQualityProfileToAny(@PathVariable Long id) {
        log.info("Set quality profile to '{}' request for tv request {}", ANY_QUALITY_PROFILE, id);
        TvRequest tvRequest = tvRequestRepository.findById(id).orElseThrow(() -> new RequestNotFoundException(id));

        Integer sonarrSeriesId = tvRequest.getSonarrSeriesId();
        if (sonarrSeriesId == null) {
            log.warn(
                    "TvRequest {} ({}) has no sonarrSeriesId; cannot change quality profile", id, tvRequest.getTitle());
            return "redirect:/tv";
        }

        @Nullable Integer profileId = sonarrClient.getQualityProfileIdByName(ANY_QUALITY_PROFILE);
        if (profileId == null) {
            log.warn("No Sonarr quality profile named '{}'; cannot change quality profile", ANY_QUALITY_PROFILE);
            return "redirect:/tv";
        }

        sonarrClient.updateSeriesQualityProfile(sonarrSeriesId, profileId);
        tvRefreshService.refreshOne(id);
        return "redirect:/tv";
    }

    @PostMapping("/tv/{id}/validate")
    public String validate(@PathVariable Long id) {
        log.info("Validate request for tv request {}", id);
        TvRequest tvRequest = tvRequestRepository.findById(id).orElseThrow(() -> new RequestNotFoundException(id));
        validatorService.validateWithEpisodes(tvRequest);
        return "redirect:/tv";
    }

    @PostMapping("/tv/validate-all")
    public String validateAll() {
        log.info("Validate-all request");
        validatorService.validateAllTv();
        return "redirect:/tv";
    }

    @PostMapping("/tv/{id}/search-one")
    public String searchOne(@PathVariable Long id) {
        log.info("Search request for tv request {}", id);
        TvRequest tvRequest = tvRequestRepository.findById(id).orElseThrow(() -> new RequestNotFoundException(id));

        Integer sonarrSeriesId = tvRequest.getSonarrSeriesId();
        if (sonarrSeriesId == null) {
            log.warn("TvRequest {} ({}) has no sonarrSeriesId; skipping search", id, tvRequest.getTitle());
            return "redirect:/tv";
        }

        SonarrCommand command = sonarrClient.searchSeries(List.of(sonarrSeriesId));
        log.info(
                "Sonarr command {} ({}) status={} result={}",
                command.getId(),
                command.getCommandName(),
                command.getStatus(),
                command.getResult());

        tvRequest.setSonarrLastSearched(Instant.now());
        tvRequestRepository.save(tvRequest);
        return "redirect:/tv";
    }

    @PostMapping("/tv/search-all-series")
    public String searchAllSeries() {
        log.info("Search-all-series request (not available)");
        List<TvRequest> tvRequests = tvRequestRepository.findAll().stream()
                .filter(tr -> tr.getSonarrSeriesId() != null)
                .filter(tr -> !tr.isAvailable())
                .toList();

        if (tvRequests.isEmpty()) {
            log.info("No unavailable tv requests with a sonarrSeriesId; nothing to search");
            return "redirect:/tv";
        }

        List<Integer> seriesIds =
                tvRequests.stream().map(TvRequest::getSonarrSeriesId).toList();
        log.info("Triggering Sonarr SeriesSearch for {} series: {}", seriesIds.size(), seriesIds);
        SonarrCommand command = sonarrClient.searchSeries(seriesIds);
        log.info(
                "Sonarr command {} ({}) status={} result={}",
                command.getId(),
                command.getCommandName(),
                command.getStatus(),
                command.getResult());

        Instant now = Instant.now();
        tvRequests.forEach(tr -> tr.setSonarrLastSearched(now));
        tvRequestRepository.saveAll(tvRequests);
        return "redirect:/tv";
    }

    /** Triggers a Sonarr SeasonSearch for every season not marked available, across all shows. */
    @Transactional
    public void searchAllSeasons() {
        log.info("Search-all-seasons request (not available)");
        for (TvRequest tvRequest : tvRequestRepository.findAll()) {
            if (tvRequest.getSonarrSeriesId() == null) {
                continue;
            }
            List<TvSeasonRequest> seasons = seasonsOf(tvRequest).stream()
                    .filter(season -> !Objects.equals(season.getOmbiSeasonAvailable(), true))
                    .toList();
            if (!seasons.isEmpty()) {
                searchSeasons(tvRequest.getSonarrSeriesId(), "tv request " + tvRequest.getId(), seasons);
            }
        }
    }

    /** Triggers a Sonarr EpisodeSearch for every episode not marked available, across all shows. */
    @Transactional
    public void searchAllEpisodes() {
        log.info("Search-all-episodes request (not available)");
        for (TvRequest tvRequest : tvRequestRepository.findAll()) {
            if (tvRequest.getSonarrSeriesId() == null) {
                continue;
            }
            Set<EpisodeKey> keys = unavailableEpisodeKeys(seasonsOf(tvRequest));
            if (!keys.isEmpty()) {
                searchEpisodes(tvRequest.getSonarrSeriesId(), "tv request " + tvRequest.getId(), keys);
            }
        }
    }

    @Transactional
    public void searchAllSeasonsForRequest(Long tvRequestId) {
        TvRequest tvRequest =
                tvRequestRepository.findById(tvRequestId).orElseThrow(() -> new RequestNotFoundException(tvRequestId));
        searchSeasons(tvRequest.getSonarrSeriesId(), "tv request " + tvRequestId, seasonsOf(tvRequest));
    }

    @Transactional
    public void searchAllEpisodesForRequest(Long tvRequestId) {
        TvRequest tvRequest =
                tvRequestRepository.findById(tvRequestId).orElseThrow(() -> new RequestNotFoundException(tvRequestId));
        searchEpisodes(
                tvRequest.getSonarrSeriesId(), "tv request " + tvRequestId, episodeKeysOfSeasons(seasonsOf(tvRequest)));
    }

    @Transactional
    public void searchAllSeasonsForChild(Long childId) {
        TvChildRequest child =
                tvChildRequestRepository.findById(childId).orElseThrow(() -> new RequestNotFoundException(childId));
        searchSeasons(seriesId(child), "tv child request " + childId, child.getSeasonRequests());
    }

    @Transactional
    public void searchAllEpisodesForChild(Long childId) {
        TvChildRequest child =
                tvChildRequestRepository.findById(childId).orElseThrow(() -> new RequestNotFoundException(childId));
        searchEpisodes(seriesId(child), "tv child request " + childId, episodeKeysOfSeasons(child.getSeasonRequests()));
    }

    @Transactional
    public void searchSeason(Long seasonId) {
        TvSeasonRequest season =
                tvSeasonRequestRepository.findById(seasonId).orElseThrow(() -> new RequestNotFoundException(seasonId));
        searchSeasons(seriesId(season), "tv season request " + seasonId, List.of(season));
    }

    @Transactional
    public void searchAllEpisodesForSeason(Long seasonId) {
        TvSeasonRequest season =
                tvSeasonRequestRepository.findById(seasonId).orElseThrow(() -> new RequestNotFoundException(seasonId));
        searchEpisodes(
                seriesId(season), "tv season request " + seasonId, episodeKeysOfEpisodes(season.getEpisodeRequests()));
    }

    @Transactional
    public void searchEpisode(Long episodeId) {
        TvEpisodeRequest episode = tvEpisodeRequestRepository
                .findById(episodeId)
                .orElseThrow(() -> new RequestNotFoundException(episodeId));
        searchEpisodes(seriesId(episode), "tv episode request " + episodeId, episodeKeysOfEpisodes(List.of(episode)));
    }

    /**
     * Triggers a Sonarr series search for the given Sonarr series ids and stamps their requests' last-search time. Used
     * by the admin page's "Search all" action over the not-searched-recently shows.
     */
    public void searchSeries(Collection<Integer> sonarrSeriesIds) {
        List<Integer> ids =
                sonarrSeriesIds.stream().filter(id -> id != null).distinct().toList();
        if (ids.isEmpty()) {
            log.info("No series ids to search");
            return;
        }
        log.info("Triggering Sonarr SeriesSearch for {} series: {}", ids.size(), ids);
        SonarrCommand command = sonarrClient.searchSeries(ids);
        log.info(
                "Sonarr command {} ({}) status={} result={}",
                command.getId(),
                command.getCommandName(),
                command.getStatus(),
                command.getResult());
        Instant now = Instant.now();
        List<TvRequest> matching = tvRequestRepository.findAll().stream()
                .filter(tr -> tr.getSonarrSeriesId() != null && ids.contains(tr.getSonarrSeriesId()))
                .toList();
        matching.forEach(tr -> tr.setSonarrLastSearched(now));
        tvRequestRepository.saveAll(matching);
    }

    /**
     * Deletes any Sonarr downloads for this episode (removing them from the download client and blocklisting the
     * releases) and then triggers a fresh Sonarr episode search. Used by the "Delete Download" context-menu action.
     */
    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public void deleteEpisodeDownloadAndSearch(Long episodeId) {
        TvEpisodeRequest episode = tvEpisodeRequestRepository
                .findById(episodeId)
                .orElseThrow(() -> new RequestNotFoundException(episodeId));
        Integer seriesId = seriesId(episode);
        Integer seasonNumber = episode.getTvSeasonRequest() == null
                ? null
                : episode.getTvSeasonRequest().getOmbiSeasonNumber();
        Integer episodeNumber = episode.getOmbiEpisodeNumber();
        if (seriesId != null && seasonNumber != null && episodeNumber != null) {
            deleteSonarrQueueItems(seriesId, seasonNumber, episodeNumber, "tv episode request " + episodeId);
        } else {
            log.warn("tv episode request {} missing series/season/episode info; skipping download deletion", episodeId);
        }
        searchEpisode(episodeId);
    }

    /**
     * Queues a JobRunr job that ffprobe-scans the episode's local file (run asynchronously on a background worker,
     * concurrency capped by {@code jobrunr.background-job-server.worker-count}) and stores the format + stream data.
     * Used by the "Scan with FFprobe" context-menu action. ADMIN-only because it ultimately executes an ffprobe
     * subprocess on the server.
     */
    @PreAuthorize("hasRole('ADMIN')")
    public void scanWithFfprobe(Long episodeId) {
        log.info("Queuing FFprobe scan for tv episode request {}", episodeId);
        jobRequestScheduler.enqueue(new FfprobeScanJobRequest(FfprobeScanJobRequest.MediaType.EPISODE, episodeId));
    }

    /**
     * Queues an ffprobe scan (one JobRunr job per episode that has a local file) for every episode in the series and
     * returns how many were queued. ADMIN-only because each job ultimately runs an ffprobe subprocess on the server.
     */
    @PreAuthorize("hasRole('ADMIN')")
    public int scanSeriesWithFfprobe(Long tvRequestId) {
        var episodeIds = tvEpisodeRequestRepository.findScannableEpisodeIdsByTvRequestId(tvRequestId);
        log.info("Queuing FFprobe scans for {} episode(s) of tv request {}", episodeIds.size(), tvRequestId);
        for (Long episodeId : episodeIds) {
            jobRequestScheduler.enqueue(new FfprobeScanJobRequest(FfprobeScanJobRequest.MediaType.EPISODE, episodeId));
        }
        return episodeIds.size();
    }

    /** The most recent stored ffprobe scan for a TV episode (read-only), used by "View FFprobe Results". */
    public Optional<FfprobeScan> getLatestFfprobeScan(Long episodeId) {
        return ffprobeScanService.getLatestEpisodeScan(episodeId);
    }

    /** Deletes (from the download client, with blocklist) every Sonarr queue item for the given episode. */
    private void deleteSonarrQueueItems(Integer seriesId, Integer seasonNumber, Integer episodeNumber, String label) {
        SonarrQueue queue;
        try {
            queue = sonarrClient.getQueue();
        } catch (RuntimeException e) {
            log.warn("Failed to fetch Sonarr queue; cannot delete downloads for {}", label, e);
            return;
        }
        if (queue == null || queue.getRecords() == null) {
            return;
        }
        for (SonarrQueueRecord record : queue.getRecords()) {
            Episode recordEpisode = record.getEpisode();
            if (seriesId.equals(record.getSeriesId())
                    && recordEpisode != null
                    && seasonNumber.equals(recordEpisode.getSeasonNumber())
                    && episodeNumber.equals(recordEpisode.getEpisodeNumber())
                    && record.getId() != null) {
                log.info("Deleting Sonarr queue item {} ({}) for {}", record.getId(), record.getDownloadId(), label);
                sonarrClient.deleteQueueItem(record.getId());
            }
        }
    }

    /** Triggers one Sonarr SeasonSearch per (distinct) season number under the given node. */
    private void searchSeasons(@Nullable Integer sonarrSeriesId, String label, List<TvSeasonRequest> seasons) {
        if (sonarrSeriesId == null) {
            log.warn("{} has no sonarrSeriesId; skipping season search", label);
            return;
        }
        List<Integer> seasonNumbers = seasons.stream()
                .map(TvSeasonRequest::getOmbiSeasonNumber)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (seasonNumbers.isEmpty()) {
            log.info("{} has no seasons; nothing to search", label);
            return;
        }
        log.info("Triggering Sonarr SeasonSearch for {} seasons {} of series {}", label, seasonNumbers, sonarrSeriesId);
        for (Integer seasonNumber : seasonNumbers) {
            SonarrCommand command = sonarrClient.searchSeason(sonarrSeriesId, seasonNumber);
            log.info(
                    "Sonarr command {} ({}) status={} result={}",
                    command.getId(),
                    command.getCommandName(),
                    command.getStatus(),
                    command.getResult());
        }
    }

    /**
     * Resolves the requested episodes to Sonarr episode ids by matching season/episode number, then triggers a single
     * Sonarr EpisodeSearch. Sonarr's EpisodeSearch keys off its own episode ids, which we don't persist, so we look
     * them up from Sonarr at search time.
     */
    private void searchEpisodes(@Nullable Integer sonarrSeriesId, String label, Set<EpisodeKey> keys) {
        if (sonarrSeriesId == null) {
            log.warn("{} has no sonarrSeriesId; skipping episode search", label);
            return;
        }
        if (keys.isEmpty()) {
            log.info("{} has no episodes; nothing to search", label);
            return;
        }
        List<Integer> episodeIds = sonarrClient.getEpisodes(sonarrSeriesId).stream()
                .filter(e -> e.getId() != null && e.getSeasonNumber() != null && e.getEpisodeNumber() != null)
                .filter(e -> keys.contains(new EpisodeKey(e.getSeasonNumber(), e.getEpisodeNumber())))
                .map(Episode::getId)
                .toList();
        if (episodeIds.isEmpty()) {
            log.warn("{} matched no Sonarr episodes for series {}; nothing to search", label, sonarrSeriesId);
            return;
        }
        log.info(
                "Triggering Sonarr EpisodeSearch for {} ({} episodes of series {})",
                label,
                episodeIds.size(),
                sonarrSeriesId);
        SonarrCommand command = sonarrClient.searchEpisodes(episodeIds);
        log.info(
                "Sonarr command {} ({}) status={} result={}",
                command.getId(),
                command.getCommandName(),
                command.getStatus(),
                command.getResult());
    }

    private List<TvSeasonRequest> seasonsOf(TvRequest tvRequest) {
        return tvChildRequestRepository.findByParentOrderByIdAsc(tvRequest).stream()
                .flatMap(child -> child.getSeasonRequests().stream())
                .toList();
    }

    private static Set<EpisodeKey> episodeKeysOfSeasons(List<TvSeasonRequest> seasons) {
        Set<EpisodeKey> keys = new HashSet<>();
        for (TvSeasonRequest season : seasons) {
            keys.addAll(episodeKeysOfEpisodes(season.getEpisodeRequests()));
        }
        return keys;
    }

    private static Set<EpisodeKey> unavailableEpisodeKeys(List<TvSeasonRequest> seasons) {
        Set<EpisodeKey> keys = new HashSet<>();
        for (TvSeasonRequest season : seasons) {
            if (season.getOmbiSeasonNumber() == null) {
                continue;
            }
            for (TvEpisodeRequest episode : season.getEpisodeRequests()) {
                if (Objects.equals(episode.getOmbiAvailable(), true) || episode.getOmbiEpisodeNumber() == null) {
                    continue;
                }
                keys.add(new EpisodeKey(season.getOmbiSeasonNumber(), episode.getOmbiEpisodeNumber()));
            }
        }
        return keys;
    }

    private static Set<EpisodeKey> episodeKeysOfEpisodes(List<TvEpisodeRequest> episodes) {
        Set<EpisodeKey> keys = new HashSet<>();
        for (TvEpisodeRequest episode : episodes) {
            Integer seasonNumber = episode.getTvSeasonRequest() == null
                    ? null
                    : episode.getTvSeasonRequest().getOmbiSeasonNumber();
            if (seasonNumber == null || episode.getOmbiEpisodeNumber() == null) {
                continue;
            }
            keys.add(new EpisodeKey(seasonNumber, episode.getOmbiEpisodeNumber()));
        }
        return keys;
    }

    private static @Nullable Integer seriesId(TvChildRequest child) {
        return child.getParent() == null ? null : child.getParent().getSonarrSeriesId();
    }

    private static @Nullable Integer seriesId(TvSeasonRequest season) {
        return season.getTvChildRequest() == null ? null : seriesId(season.getTvChildRequest());
    }

    private static @Nullable Integer seriesId(TvEpisodeRequest episode) {
        return episode.getTvSeasonRequest() == null ? null : seriesId(episode.getTvSeasonRequest());
    }

    /** Sonarr's current download queue, or null if Sonarr can't be reached. */
    public @Nullable SonarrQueue getSonarrQueue() {
        try {
            return sonarrClient.getQueue();
        } catch (RuntimeException e) {
            log.warn("Failed to fetch Sonarr queue", e);
            return null;
        }
    }

    /** Active Sonarr health issues, or null if Sonarr can't be reached. */
    public @Nullable List<SonarrHealthItem> getSonarrHealth() {
        try {
            return sonarrClient.getHealth();
        } catch (RuntimeException e) {
            log.warn("Failed to fetch Sonarr health", e);
            return null;
        }
    }

    @PostMapping("/tv/{id}/mark-available")
    @PreAuthorize("hasRole('ADMIN')")
    public String markAvailable(@PathVariable Long id) {
        log.info("Mark available request for tv request {}", id);
        TvRequest tvRequest = tvRequestRepository.findById(id).orElseThrow(() -> new RequestNotFoundException(id));

        log.info("Found TvRequest for {}", tvRequest.getTitle());

        // Ombi's tv/available endpoint operates on child (per-user) request ids, not the parent
        // request id, so mark every child of this show available.
        List<TvChildRequest> children = tvChildRequestRepository.findByParentOrderByIdAsc(tvRequest);
        if (children.isEmpty()) {
            log.warn("No child requests for tv request {} ({}); nothing to mark available", id, tvRequest.getTitle());
            return "redirect:/tv";
        }

        for (TvChildRequest child : children) {
            Integer childOmbiRequestId = child.getOmbiRequestId();
            if (childOmbiRequestId == null) {
                log.warn("Skipping child {} of tv request {} with null ombiRequestId", child.getId(), id);
                continue;
            }
            OmbiReprocessResponse response = ombiClient.markTvAvailable(childOmbiRequestId);
            try {
                log.info(
                        "Ombi mark-available response for tv child request {} (parent {} - {}): {}",
                        childOmbiRequestId,
                        id,
                        tvRequest.getTitle(),
                        objectMapper.writeValueAsString(response));
            } catch (JacksonException e) {
                log.warn(
                        "Failed to serialize Ombi mark-available response for tv child request {} (parent {} - {})",
                        childOmbiRequestId,
                        id,
                        tvRequest.getTitle(),
                        e);
            }
        }

        return "redirect:/tv";
    }

    @PostMapping("/tv/{id}/delete")
    @PreAuthorize("hasRole('ADMIN')")
    public String delete(@PathVariable Long id) {
        log.info("Delete request for tv request {}", id);
        requestAdminService.delete(tvRequestRepository, id);
        return "redirect:/tv";
    }

    @PostMapping("/tv/{id}/notes")
    public Note addNote(@PathVariable Long id, @RequestParam("notes") String notes) {
        log.info("Add note request for tv request {}", id);
        return requestAdminService.addNote(tvRequestRepository, id, notes);
    }

    @PostMapping("/tv/{id}/mark-stale")
    public String markStale(@PathVariable Long id, @RequestParam("reason") String reason) {
        log.info("Mark stale request for tv request {} with reason: {}", id, reason);
        requestAdminService.markStale(tvRequestRepository, id, reason);
        return "redirect:/tv";
    }
}
