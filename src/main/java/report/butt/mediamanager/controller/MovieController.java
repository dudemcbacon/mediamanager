package report.butt.mediamanager.controller;

import java.time.Instant;
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
import report.butt.mediamanager.exceptions.MovieRequestNotFoundException;
import report.butt.mediamanager.model.MovieRequest;
import report.butt.mediamanager.model.ombi.OmbiReprocessResponse;
import report.butt.mediamanager.model.radarr.RadarrCommand;
import report.butt.mediamanager.repository.MovieRequestRepository;
import report.butt.mediamanager.service.MovieValidatorService;
import report.butt.mediamanager.service.RefreshService;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

@Controller
public class MovieController {

  private static final Logger log = LoggerFactory.getLogger(MovieController.class);

  private final MovieRequestRepository movieRequestRepository;
  private final OmbiClient ombiClient;
  private final RadarrClient radarrClient;
  private final ObjectMapper objectMapper;
  private final RefreshService refreshService;
  private final MovieValidatorService movieValidatorService;

  @Autowired
  public MovieController(MovieRequestRepository movieRequestRepository, OmbiClient ombiClient,
      RadarrClient radarrClient, ObjectMapper objectMapper, RefreshService refreshService,
      MovieValidatorService movieValidatorService) {
    this.movieRequestRepository = movieRequestRepository;
    this.ombiClient = ombiClient;
    this.radarrClient = radarrClient;
    this.objectMapper = objectMapper;
    this.refreshService = refreshService;
    this.movieValidatorService = movieValidatorService;
  }

  @PostMapping("/movies/refresh-all")
  public String refreshAll() {
    log.info("Refresh-all request");
    refreshService.refreshAll();
    return "redirect:/movies";
  }

  @PostMapping("/movies/search-missing")
  public String searchMissing() {
    List<MovieRequest> movieRequests = movieRequestRepository.findAll().stream()
        .filter(movieRequest -> "Common.ProcessingRequest".equals(movieRequest.getOmbiRequestStatus())
            && Boolean.FALSE.equals(movieRequest.getRadarrHasFile())
            && movieRequest.getRadarrHistoryCount() != null
            && movieRequest.getRadarrHistoryCount() == 0
            && movieRequest.getRadarrRequestId() != null)
        .toList();

    List<Integer> movieIds = movieRequests.stream().map(MovieRequest::getRadarrRequestId).toList();

    log.info("Triggering Radarr MoviesSearch for {} movies: {}", movieIds.size(), movieIds);
    if (!movieIds.isEmpty()) {
      RadarrCommand command = radarrClient.searchMovies(movieIds);
      log.info("Radarr command {} ({}) status={} result={}", command.getId(), command.getCommandName(),
          command.getStatus(), command.getResult());

      Instant now = Instant.now();
      movieRequests.forEach(movieRequest -> movieRequest.setRadarrLastSearched(now));
      movieRequestRepository.saveAll(movieRequests);
    }

    return "redirect:/movies";
  }

  @GetMapping("/movies")
  public String movies(Model model) {
    List<MovieRequest> movieRequests = movieRequestRepository.findAll().stream()
        .sorted(Comparator.comparing(MovieRequest::getRadarrRequestId,
            Comparator.nullsFirst(Comparator.naturalOrder())))
        .toList();

    model.addAttribute("movies", movieRequests);
    return "movies";
  }

  @PostMapping("/movies/{movieId}/search")
  public String search(@PathVariable Integer movieId) {
    log.info("Triggering Radarr MoviesSearch for movie {}", movieId);
    RadarrCommand command = radarrClient.searchMovies(List.of(movieId));
    log.info("Radarr command {} ({}) status={} result={}", command.getId(), command.getCommandName(),
        command.getStatus(), command.getResult());

    Instant now = Instant.now();
    movieRequestRepository.findAll().stream()
        .filter(movieRequest -> movieId.equals(movieRequest.getRadarrRequestId()))
        .forEach(movieRequest -> {
          movieRequest.setRadarrLastSearched(now);
          movieRequestRepository.save(movieRequest);
        });

    return "redirect:/movies";
  }

  @PostMapping("/movies/{id}/refresh")
  public String refresh(@PathVariable Long id) {
    log.info("Refresh request for movie request {}", id);
    refreshService.refreshOne(id);
    return "redirect:/movies";
  }

  @PostMapping("/movies/{id}/validate")
  public String validate(@PathVariable Long id) {
    log.info("Validate request for movie request {}", id);
    MovieRequest movieRequest = movieRequestRepository.findById(id)
        .orElseThrow(() -> new MovieRequestNotFoundException(id));
    movieValidatorService.validate(movieRequest);
    return "redirect:/movies";
  }

  @PostMapping("/movies/validate-all")
  public String validateAll() {
    log.info("Validate-all request");
    movieRequestRepository.findAll().forEach(movieValidatorService::validate);
    return "redirect:/movies";
  }

  @PostMapping("/movies/{id}/search-one")
  public String searchOne(@PathVariable Long id) {
    log.info("Search request for movie request {}", id);
    MovieRequest movieRequest = movieRequestRepository.findById(id)
        .orElseThrow(() -> new MovieRequestNotFoundException(id));

    Integer radarrRequestId = movieRequest.getRadarrRequestId();
    if (radarrRequestId == null) {
      log.warn("MovieRequest {} ({}) has no radarrRequestId; skipping search", id, movieRequest.getTitle());
      return "redirect:/movies";
    }

    RadarrCommand command = radarrClient.searchMovies(List.of(radarrRequestId));
    log.info("Radarr command {} ({}) status={} result={}", command.getId(), command.getCommandName(),
        command.getStatus(), command.getResult());

    movieRequest.setRadarrLastSearched(Instant.now());
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

    List<Integer> movieIds = movieRequests.stream().map(MovieRequest::getRadarrRequestId).toList();
    log.info("Triggering Radarr MoviesSearch for {} movies: {}", movieIds.size(), movieIds);
    RadarrCommand command = radarrClient.searchMovies(movieIds);
    log.info("Radarr command {} ({}) status={} result={}", command.getId(), command.getCommandName(),
        command.getStatus(), command.getResult());

    Instant now = Instant.now();
    movieRequests.forEach(mr -> mr.setRadarrLastSearched(now));
    movieRequestRepository.saveAll(movieRequests);
    return "redirect:/movies";
  }

  @PostMapping("/movies/{id}/mark-available")
  public String markAvailable(@PathVariable Long id) {
    log.info("Mark available request for movie request {}", id);
    MovieRequest movieRequest = movieRequestRepository.findById(id)
        .orElseThrow(() -> new MovieRequestNotFoundException(id));

    log.info("Found MovieRequest for {}", movieRequest.getTitle());

    OmbiReprocessResponse response = ombiClient.markMovieAvailable(movieRequest.getOmbiRequestId());
    try {
      log.info("Ombi mark-available response for movie request {} ({}): {}", id, movieRequest.getTitle(),
          objectMapper.writeValueAsString(response));
    } catch (JacksonException e) {
      log.warn("Failed to serialize Ombi mark-available response for movie request {} ({})", id, movieRequest.getTitle(), e);
    }

    return "redirect:/movies";
  }

  @PostMapping("/movies/{id}/mark-stale")
  public String markStale(@PathVariable Long id, @RequestParam("reason") String reason) {
    log.info("Mark stale request for movie request {} with reason: {}", id, reason);
    MovieRequest movieRequest = movieRequestRepository.findById(id)
        .orElseThrow(() -> new MovieRequestNotFoundException(id));

    movieRequest.setStale(true);
    movieRequest.setStaleReason(reason);
    movieRequest.setMarkedStaleAt(Instant.now());
    movieRequestRepository.save(movieRequest);

    return "redirect:/movies";
  }

  @PostMapping("/movies/{id}/reprocess")
  public String reprocess(@PathVariable Long id) {
    log.info("Reprocess request for movie request {}", id);
    MovieRequest movieRequest = movieRequestRepository.findById(id)
        .orElseThrow(() -> new MovieRequestNotFoundException(id));

    log.info("Found MovieRequest for {}", movieRequest.getTitle());

    OmbiReprocessResponse response = ombiClient.reprocessMovieRequest(movieRequest.getOmbiRequestId());
    try {
      log.info("Ombi reprocess response for movie request {} ({}): {}", id, movieRequest.getTitle(),
          objectMapper.writeValueAsString(response));
    } catch (JacksonException e) {
      log.warn("Failed to serialize Ombi reprocess response for movie request {} ({})", id, movieRequest.getTitle(), e);
    }

    refreshService.refreshOne(id);

    log.info("MovieRequest successfully re-processed.");
    return "redirect:/movies";
  }
}
