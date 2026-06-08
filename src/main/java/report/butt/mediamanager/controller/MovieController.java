package report.butt.mediamanager.controller;

import java.time.Instant;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import report.butt.mediamanager.client.OmbiClient;
import report.butt.mediamanager.client.RadarrClient;
import report.butt.mediamanager.exceptions.RequestNotFoundException;
import report.butt.mediamanager.model.MovieRequest;
import report.butt.mediamanager.model.Note;
import report.butt.mediamanager.model.ombi.OmbiReprocessResponse;
import report.butt.mediamanager.model.radarr.RadarrCommand;
import report.butt.mediamanager.model.radarr.RadarrHealthItem;
import report.butt.mediamanager.model.radarr.RadarrQueue;
import report.butt.mediamanager.model.radarr.RadarrQueueRecord;
import report.butt.mediamanager.repository.MovieRequestRepository;
import report.butt.mediamanager.service.MovieRefreshService;
import report.butt.mediamanager.service.RequestAdminService;
import report.butt.mediamanager.service.ValidatorService;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

@Controller
public class MovieController {

    private static final Logger log = LoggerFactory.getLogger(MovieController.class);
    private static final String ANY_QUALITY_PROFILE = "Any";

    private final MovieRequestRepository movieRequestRepository;
    private final OmbiClient ombiClient;
    private final RadarrClient radarrClient;
    private final ObjectMapper objectMapper;
    private final MovieRefreshService movieRefreshService;
    private final ValidatorService validatorService;
    private final RequestAdminService requestAdminService;

    @Autowired
    public MovieController(
            MovieRequestRepository movieRequestRepository,
            OmbiClient ombiClient,
            RadarrClient radarrClient,
            ObjectMapper objectMapper,
            MovieRefreshService movieRefreshService,
            ValidatorService validatorService,
            RequestAdminService requestAdminService) {
        this.movieRequestRepository = movieRequestRepository;
        this.ombiClient = ombiClient;
        this.radarrClient = radarrClient;
        this.objectMapper = objectMapper;
        this.movieRefreshService = movieRefreshService;
        this.validatorService = validatorService;
        this.requestAdminService = requestAdminService;
    }

    /** Radarr's current download queue, or null if Radarr can't be reached. */
    public RadarrQueue getRadarrQueue() {
        try {
            return radarrClient.getQueue();
        } catch (Exception e) {
            log.warn("Failed to fetch Radarr queue", e);
            return null;
        }
    }

    /** Active Radarr health issues, or null if Radarr can't be reached. */
    public List<RadarrHealthItem> getRadarrHealth() {
        try {
            return radarrClient.getHealth();
        } catch (Exception e) {
            log.warn("Failed to fetch Radarr health", e);
            return null;
        }
    }

    @PostMapping("/movies/refresh-all")
    public String refreshAll() {
        log.info("Refresh-all request");
        movieRefreshService.refreshAll();
        return "redirect:/movies";
    }

    @PostMapping("/movies/search-missing")
    public String searchMissing() {
        List<MovieRequest> movieRequests = movieRequestRepository.findAll().stream()
                .filter(movieRequest -> "Common.ProcessingRequest".equals(movieRequest.getOmbiRequestStatus())
                        && Boolean.FALSE.equals(movieRequest.getRadarrHasFile())
                        && movieRequest.getRadarrRequestId() != null)
                .toList();

        List<Integer> movieIds =
                movieRequests.stream().map(MovieRequest::getRadarrRequestId).toList();

        log.info("Triggering Radarr MoviesSearch for {} movies: {}", movieIds.size(), movieIds);
        if (!movieIds.isEmpty()) {
            RadarrCommand command = radarrClient.searchMovies(movieIds);
            log.info(
                    "Radarr command {} ({}) status={} result={}",
                    command.getId(),
                    command.getCommandName(),
                    command.getStatus(),
                    command.getResult());

            Instant now = Instant.now();
            movieRequests.forEach(movieRequest -> movieRequest.setRadarrLastSearchTime(now));
            movieRequestRepository.saveAll(movieRequests);
        }

        return "redirect:/movies";
    }

    @GetMapping("/movies")
    public String movies(Model model) {
        List<MovieRequest> movieRequests = movieRequestRepository.findAll().stream()
                .sorted(Comparator.comparing(
                        MovieRequest::getRadarrRequestId, Comparator.nullsFirst(Comparator.naturalOrder())))
                .toList();

        model.addAttribute("movies", movieRequests);
        return "movies";
    }

    @PostMapping("/movies/{movieId}/search")
    public String search(@PathVariable Integer movieId) {
        log.info("Triggering Radarr MoviesSearch for movie {}", movieId);
        RadarrCommand command = radarrClient.searchMovies(List.of(movieId));
        log.info(
                "Radarr command {} ({}) status={} result={}",
                command.getId(),
                command.getCommandName(),
                command.getStatus(),
                command.getResult());

        Instant now = Instant.now();
        movieRequestRepository.findAll().stream()
                .filter(movieRequest -> movieId.equals(movieRequest.getRadarrRequestId()))
                .forEach(movieRequest -> {
                    movieRequest.setRadarrLastSearchTime(now);
                    movieRequestRepository.save(movieRequest);
                });

        return "redirect:/movies";
    }

    @PostMapping("/movies/{id}/refresh")
    public String refresh(@PathVariable Long id) {
        log.info("Refresh request for movie request {}", id);
        movieRefreshService.refreshOne(id);
        return "redirect:/movies";
    }

    @PostMapping("/movies/{id}/validate")
    public String validate(@PathVariable Long id) {
        log.info("Validate request for movie request {}", id);
        MovieRequest movieRequest =
                movieRequestRepository.findById(id).orElseThrow(() -> new RequestNotFoundException(id));
        validatorService.validate(movieRequest);
        return "redirect:/movies";
    }

    @PostMapping("/movies/validate-all")
    public String validateAll() {
        log.info("Validate-all request");
        validatorService.validateAllMovies();
        return "redirect:/movies";
    }

    @PostMapping("/movies/{id}/search-one")
    public String searchOne(@PathVariable Long id) {
        log.info("Search request for movie request {}", id);
        MovieRequest movieRequest =
                movieRequestRepository.findById(id).orElseThrow(() -> new RequestNotFoundException(id));

        Integer radarrRequestId = movieRequest.getRadarrRequestId();
        if (radarrRequestId == null) {
            log.warn("MovieRequest {} ({}) has no radarrRequestId; skipping search", id, movieRequest.getTitle());
            return "redirect:/movies";
        }

        RadarrCommand command = radarrClient.searchMovies(List.of(radarrRequestId));
        log.info(
                "Radarr command {} ({}) status={} result={}",
                command.getId(),
                command.getCommandName(),
                command.getStatus(),
                command.getResult());

        movieRequest.setRadarrLastSearchTime(Instant.now());
        movieRequestRepository.save(movieRequest);
        return "redirect:/movies";
    }

    @PostMapping("/movies/search-all")
    public String searchAll() {
        log.info("Search-all request");
        List<MovieRequest> movieRequests = movieRequestRepository.findAll().stream()
                .filter(mr -> mr.getRadarrRequestId() != null)
                .toList();

        if (movieRequests.isEmpty()) {
            log.info("No movie requests with a radarrRequestId; nothing to search");
            return "redirect:/movies";
        }

        List<Integer> movieIds =
                movieRequests.stream().map(MovieRequest::getRadarrRequestId).toList();
        log.info("Triggering Radarr MoviesSearch for {} movies: {}", movieIds.size(), movieIds);
        RadarrCommand command = radarrClient.searchMovies(movieIds);
        log.info(
                "Radarr command {} ({}) status={} result={}",
                command.getId(),
                command.getCommandName(),
                command.getStatus(),
                command.getResult());

        Instant now = Instant.now();
        movieRequests.forEach(mr -> mr.setRadarrLastSearchTime(now));
        movieRequestRepository.saveAll(movieRequests);
        return "redirect:/movies";
    }

    /**
     * Triggers a Radarr search for the given Radarr movie ids and stamps their request's last-search time. Used by the
     * admin page's "Search all" action over the not-searched-recently movies.
     */
    public void searchMovies(Collection<Integer> radarrRequestIds) {
        List<Integer> ids =
                radarrRequestIds.stream().filter(id -> id != null).distinct().toList();
        if (ids.isEmpty()) {
            log.info("No movie ids to search");
            return;
        }
        log.info("Triggering Radarr MoviesSearch for {} movies: {}", ids.size(), ids);
        RadarrCommand command = radarrClient.searchMovies(ids);
        log.info(
                "Radarr command {} ({}) status={} result={}",
                command.getId(),
                command.getCommandName(),
                command.getStatus(),
                command.getResult());
        Instant now = Instant.now();
        List<MovieRequest> matching = movieRequestRepository.findAll().stream()
                .filter(mr -> mr.getRadarrRequestId() != null && ids.contains(mr.getRadarrRequestId()))
                .toList();
        matching.forEach(mr -> mr.setRadarrLastSearchTime(now));
        movieRequestRepository.saveAll(matching);
    }

    /**
     * Deletes any Radarr downloads for this movie (removing them from the download client and blocklisting the
     * releases) and then triggers a fresh Radarr search. Used by the "Delete Download" context-menu action.
     */
    public void deleteDownloadAndSearch(Long id) {
        MovieRequest movieRequest =
                movieRequestRepository.findById(id).orElseThrow(() -> new RequestNotFoundException(id));

        Integer radarrRequestId = movieRequest.getRadarrRequestId();
        if (radarrRequestId == null) {
            log.warn(
                    "MovieRequest {} ({}) has no radarrRequestId; skipping delete-download and search",
                    id,
                    movieRequest.getTitle());
            return;
        }

        deleteRadarrQueueItems(radarrRequestId, id, movieRequest.getTitle());

        RadarrCommand command = radarrClient.searchMovies(List.of(radarrRequestId));
        log.info(
                "Radarr command {} ({}) status={} result={}",
                command.getId(),
                command.getCommandName(),
                command.getStatus(),
                command.getResult());
        movieRequest.setRadarrLastSearchTime(Instant.now());
        movieRequestRepository.save(movieRequest);
    }

    /** Deletes (from the download client, with blocklist) every Radarr queue item for the given movie. */
    private void deleteRadarrQueueItems(Integer radarrMovieId, Long requestId, String title) {
        RadarrQueue queue;
        try {
            queue = radarrClient.getQueue();
        } catch (Exception e) {
            log.warn(
                    "Failed to fetch Radarr queue; cannot delete downloads for movie request {} ({})",
                    requestId,
                    title,
                    e);
            return;
        }
        if (queue == null || queue.getRecords() == null) {
            return;
        }
        for (RadarrQueueRecord record : queue.getRecords()) {
            if (radarrMovieId.equals(record.getMovieId()) && record.getId() != null) {
                log.info(
                        "Deleting Radarr queue item {} ({}) for movie request {} ({})",
                        record.getId(),
                        record.getDownloadId(),
                        requestId,
                        title);
                radarrClient.deleteQueueItem(record.getId());
            }
        }
    }

    @PostMapping("/movies/{id}/quality-profile-any")
    public String setQualityProfileToAny(@PathVariable Long id) {
        log.info("Set quality profile to '{}' request for movie request {}", ANY_QUALITY_PROFILE, id);
        MovieRequest movieRequest =
                movieRequestRepository.findById(id).orElseThrow(() -> new RequestNotFoundException(id));

        Integer radarrRequestId = movieRequest.getRadarrRequestId();
        if (radarrRequestId == null) {
            log.warn(
                    "MovieRequest {} ({}) has no radarrRequestId; cannot change quality profile",
                    id,
                    movieRequest.getTitle());
            return "redirect:/movies";
        }

        Integer profileId = radarrClient.getQualityProfileIdByName(ANY_QUALITY_PROFILE);
        if (profileId == null) {
            log.warn("No Radarr quality profile named '{}'; cannot change quality profile", ANY_QUALITY_PROFILE);
            return "redirect:/movies";
        }

        radarrClient.updateMovieQualityProfile(radarrRequestId, profileId);
        movieRefreshService.refreshOne(id);
        return "redirect:/movies";
    }

    @PostMapping("/movies/{id}/mark-available")
    public String markAvailable(@PathVariable Long id) {
        log.info("Mark available request for movie request {}", id);
        MovieRequest movieRequest =
                movieRequestRepository.findById(id).orElseThrow(() -> new RequestNotFoundException(id));

        log.info("Found MovieRequest for {}", movieRequest.getTitle());

        OmbiReprocessResponse response = ombiClient.markMovieAvailable(movieRequest.getOmbiRequestId());
        try {
            log.info(
                    "Ombi mark-available response for movie request {} ({}): {}",
                    id,
                    movieRequest.getTitle(),
                    objectMapper.writeValueAsString(response));
        } catch (JacksonException e) {
            log.warn(
                    "Failed to serialize Ombi mark-available response for movie request {} ({})",
                    id,
                    movieRequest.getTitle(),
                    e);
        }

        return "redirect:/movies";
    }

    @PostMapping("/movies/{id}/delete")
    public String delete(@PathVariable Long id) {
        log.info("Delete request for movie request {}", id);
        requestAdminService.delete(movieRequestRepository, id);
        return "redirect:/movies";
    }

    @PostMapping("/movies/{id}/notes")
    public Note addNote(@PathVariable Long id, @RequestParam("notes") String notes) {
        log.info("Add note request for movie request {}", id);
        return requestAdminService.addNote(movieRequestRepository, id, notes);
    }

    @PostMapping("/movies/{id}/mark-stale")
    public String markStale(@PathVariable Long id, @RequestParam("reason") String reason) {
        log.info("Mark stale request for movie request {} with reason: {}", id, reason);
        requestAdminService.markStale(movieRequestRepository, id, reason);
        return "redirect:/movies";
    }

    @PostMapping("/movies/{id}/reprocess")
    public String reprocess(@PathVariable Long id) {
        log.info("Reprocess request for movie request {}", id);
        MovieRequest movieRequest =
                movieRequestRepository.findById(id).orElseThrow(() -> new RequestNotFoundException(id));

        log.info("Found MovieRequest for {}", movieRequest.getTitle());

        OmbiReprocessResponse response = ombiClient.reprocessMovieRequest(movieRequest.getOmbiRequestId());
        try {
            log.info(
                    "Ombi reprocess response for movie request {} ({}): {}",
                    id,
                    movieRequest.getTitle(),
                    objectMapper.writeValueAsString(response));
        } catch (JacksonException e) {
            log.warn(
                    "Failed to serialize Ombi reprocess response for movie request {} ({})",
                    id,
                    movieRequest.getTitle(),
                    e);
        }

        movieRefreshService.refreshOne(id);

        log.info("MovieRequest successfully re-processed.");
        return "redirect:/movies";
    }
}
