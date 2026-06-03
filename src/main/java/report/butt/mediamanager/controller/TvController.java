package report.butt.mediamanager.controller;

import java.time.Instant;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
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
import report.butt.mediamanager.repository.NoteRepository;
import report.butt.mediamanager.repository.TvChildRequestRepository;
import report.butt.mediamanager.repository.TvEpisodeRequestRepository;
import report.butt.mediamanager.repository.TvRequestRepository;
import report.butt.mediamanager.repository.TvSeasonRequestRepository;
import report.butt.mediamanager.repository.ValidationRepository;
import report.butt.mediamanager.service.TvRefreshService;
import report.butt.mediamanager.service.ValidatorService;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

@Controller
public class TvController {

    private static final Logger log = LoggerFactory.getLogger(TvController.class);

    private final TvRequestRepository tvRequestRepository;
    private final TvChildRequestRepository tvChildRequestRepository;
    private final TvSeasonRequestRepository tvSeasonRequestRepository;
    private final TvEpisodeRequestRepository tvEpisodeRequestRepository;
    private final ValidationRepository validationRepository;
    private final NoteRepository noteRepository;
    private final OmbiClient ombiClient;
    private final SonarrClient sonarrClient;
    private final ObjectMapper objectMapper;
    private final TvRefreshService tvRefreshService;
    private final ValidatorService validatorService;

    @Autowired
    public TvController(
            TvRequestRepository tvRequestRepository,
            TvChildRequestRepository tvChildRequestRepository,
            TvSeasonRequestRepository tvSeasonRequestRepository,
            TvEpisodeRequestRepository tvEpisodeRequestRepository,
            ValidationRepository validationRepository,
            NoteRepository noteRepository,
            OmbiClient ombiClient,
            SonarrClient sonarrClient,
            ObjectMapper objectMapper,
            TvRefreshService tvRefreshService,
            ValidatorService validatorService) {
        this.tvRequestRepository = tvRequestRepository;
        this.tvChildRequestRepository = tvChildRequestRepository;
        this.tvSeasonRequestRepository = tvSeasonRequestRepository;
        this.tvEpisodeRequestRepository = tvEpisodeRequestRepository;
        this.validationRepository = validationRepository;
        this.noteRepository = noteRepository;
        this.ombiClient = ombiClient;
        this.sonarrClient = sonarrClient;
        this.objectMapper = objectMapper;
        this.tvRefreshService = tvRefreshService;
        this.validatorService = validatorService;
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
                .filter(tvRequest -> "Common.ProcessingRequest".equals(tvRequest.getOmbiRequestStatus())
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
        tvRequestRepository.findAll().forEach(validatorService::validateWithEpisodes);
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
                    .filter(season -> !Boolean.TRUE.equals(season.getOmbiSeasonAvailable()))
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
        searchEpisodes(tvRequest.getSonarrSeriesId(), "tv request " + tvRequestId, episodeKeysOfSeasons(seasonsOf(tvRequest)));
    }

    @Transactional
    public void searchAllSeasonsForChild(Long childId) {
        TvChildRequest child = tvChildRequestRepository
                .findById(childId)
                .orElseThrow(() -> new RequestNotFoundException(childId));
        searchSeasons(seriesId(child), "tv child request " + childId, child.getSeasonRequests());
    }

    @Transactional
    public void searchAllEpisodesForChild(Long childId) {
        TvChildRequest child = tvChildRequestRepository
                .findById(childId)
                .orElseThrow(() -> new RequestNotFoundException(childId));
        searchEpisodes(seriesId(child), "tv child request " + childId, episodeKeysOfSeasons(child.getSeasonRequests()));
    }

    @Transactional
    public void searchSeason(Long seasonId) {
        TvSeasonRequest season = tvSeasonRequestRepository
                .findById(seasonId)
                .orElseThrow(() -> new RequestNotFoundException(seasonId));
        searchSeasons(seriesId(season), "tv season request " + seasonId, List.of(season));
    }

    @Transactional
    public void searchAllEpisodesForSeason(Long seasonId) {
        TvSeasonRequest season = tvSeasonRequestRepository
                .findById(seasonId)
                .orElseThrow(() -> new RequestNotFoundException(seasonId));
        searchEpisodes(seriesId(season), "tv season request " + seasonId, episodeKeysOfEpisodes(season.getEpisodeRequests()));
    }

    @Transactional
    public void searchEpisode(Long episodeId) {
        TvEpisodeRequest episode = tvEpisodeRequestRepository
                .findById(episodeId)
                .orElseThrow(() -> new RequestNotFoundException(episodeId));
        searchEpisodes(seriesId(episode), "tv episode request " + episodeId, episodeKeysOfEpisodes(List.of(episode)));
    }

    /** Triggers one Sonarr SeasonSearch per (distinct) season number under the given node. */
    private void searchSeasons(Integer sonarrSeriesId, String label, List<TvSeasonRequest> seasons) {
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
     * Resolves the requested episodes to Sonarr episode ids by matching season/episode number, then
     * triggers a single Sonarr EpisodeSearch. Sonarr's EpisodeSearch keys off its own episode ids,
     * which we don't persist, so we look them up from Sonarr at search time.
     */
    private void searchEpisodes(Integer sonarrSeriesId, String label, Set<EpisodeKey> keys) {
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
        log.info("Triggering Sonarr EpisodeSearch for {} ({} episodes of series {})", label, episodeIds.size(), sonarrSeriesId);
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
                if (Boolean.TRUE.equals(episode.getOmbiAvailable()) || episode.getOmbiEpisodeNumber() == null) {
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

    private static Integer seriesId(TvChildRequest child) {
        return child.getParent() == null ? null : child.getParent().getSonarrSeriesId();
    }

    private static Integer seriesId(TvSeasonRequest season) {
        return season.getTvChildRequest() == null ? null : seriesId(season.getTvChildRequest());
    }

    private static Integer seriesId(TvEpisodeRequest episode) {
        return episode.getTvSeasonRequest() == null ? null : seriesId(episode.getTvSeasonRequest());
    }

    /** Sonarr's current download queue, or null if Sonarr can't be reached. */
    public SonarrQueue getSonarrQueue() {
        try {
            return sonarrClient.getQueue();
        } catch (Exception e) {
            log.warn("Failed to fetch Sonarr queue", e);
            return null;
        }
    }

    /** Active Sonarr health issues, or null if Sonarr can't be reached. */
    public List<SonarrHealthItem> getSonarrHealth() {
        try {
            return sonarrClient.getHealth();
        } catch (Exception e) {
            log.warn("Failed to fetch Sonarr health", e);
            return null;
        }
    }

    @PostMapping("/tv/{id}/mark-available")
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
    @Transactional
    public String delete(@PathVariable Long id) {
        log.info("Delete request for tv request {}", id);
        TvRequest tvRequest = tvRequestRepository.findById(id).orElseThrow(() -> new RequestNotFoundException(id));
        validationRepository.deleteByRequest(tvRequest);
        noteRepository.deleteByRequest(tvRequest);
        tvRequestRepository.delete(tvRequest);
        return "redirect:/tv";
    }

    @PostMapping("/tv/{id}/notes")
    public Note addNote(@PathVariable Long id, @RequestParam("notes") String notes) {
        log.info("Add note request for tv request {}", id);
        TvRequest tvRequest = tvRequestRepository.findById(id).orElseThrow(() -> new RequestNotFoundException(id));
        return noteRepository.save(new Note(notes, tvRequest));
    }

    @PostMapping("/tv/{id}/mark-stale")
    public String markStale(@PathVariable Long id, @RequestParam("reason") String reason) {
        log.info("Mark stale request for tv request {} with reason: {}", id, reason);
        TvRequest tvRequest = tvRequestRepository.findById(id).orElseThrow(() -> new RequestNotFoundException(id));

        tvRequest.setStale(true);
        tvRequest.setStaleReason(reason);
        tvRequest.setMarkedStaleAt(Instant.now());
        tvRequestRepository.save(tvRequest);

        return "redirect:/tv";
    }
}
