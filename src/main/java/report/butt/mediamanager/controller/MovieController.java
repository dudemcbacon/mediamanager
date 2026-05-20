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

import report.butt.mediamanager.client.OmbiClient;
import report.butt.mediamanager.client.PlexClient;
import report.butt.mediamanager.client.RadarrClient;
import report.butt.mediamanager.exceptions.MovieRequestNotFoundException;
import report.butt.mediamanager.model.MovieRequest;
import report.butt.mediamanager.model.ombi.OmbiReprocessResponse;
import report.butt.mediamanager.model.plex.PlexMedia;
import report.butt.mediamanager.model.plex.PlexMetadata;
import report.butt.mediamanager.model.plex.PlexPart;
import report.butt.mediamanager.model.radarr.Movie;
import report.butt.mediamanager.model.radarr.RadarrCommand;
import report.butt.mediamanager.repository.MovieRequestRepository;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

@Controller
public class MovieController {

  private static final Logger log = LoggerFactory.getLogger(MovieController.class);

  private final MovieRequestRepository movieRequestRepository;
  private final OmbiClient ombiClient;
  private final RadarrClient radarrClient;
  private final PlexClient plexClient;
  private final ObjectMapper objectMapper;

  @Autowired
  public MovieController(MovieRequestRepository movieRequestRepository, OmbiClient ombiClient,
      RadarrClient radarrClient, PlexClient plexClient, ObjectMapper objectMapper) {
    this.movieRequestRepository = movieRequestRepository;
    this.ombiClient = ombiClient;
    this.radarrClient = radarrClient;
    this.plexClient = plexClient;
    this.objectMapper = objectMapper;
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
    MovieRequest movieRequest = movieRequestRepository.findById(id)
        .orElseThrow(() -> new MovieRequestNotFoundException(id));

    Integer ombiRequestId = movieRequest.getOmbiRequestId();
    ombiClient.getMovies().stream()
        .filter(m -> ombiRequestId.equals(m.getId()))
        .findFirst()
        .ifPresent(ombiMovie -> {
          movieRequest.setTitle(ombiMovie.getTitle());
          movieRequest.setTmdbid(ombiMovie.getTheMovieDbId());
          movieRequest.setOmbiAvailable(ombiMovie.getAvailable());
          movieRequest.setOmbiRequestStatus(ombiMovie.getRequestStatus());
          movieRequest.setOmbiUserName(ombiMovie.getRequestedUser() == null ? null : ombiMovie.getRequestedUser().getUserName());
        });

    List<Movie> radarrMovies = radarrClient.getMoviesByTmdbId(movieRequest.getTmdbid());
    if (!radarrMovies.isEmpty()) {
      Movie radarrMovie = radarrMovies.get(0);
      movieRequest.setRadarrRequestId(radarrMovie.getId());
      movieRequest.setRadarrHasFile(radarrMovie.getHasFile());
      movieRequest.setRadarrMonitored(radarrMovie.getMonitored());
      movieRequest.setRadarrIsAvailable(radarrMovie.getIsAvailable());
      movieRequest.setRadarrHistoryCount(radarrClient.getMovieHistory(radarrMovie.getId()).size());

      try {
        var plexResult = plexClient.getMovieByTmdbId(radarrMovie.getTmdbId(), radarrMovie.getTitle(), radarrMovie.getYear());
        movieRequest.setPlexMetadataUrl(plexResult.url());
        PlexMetadata plexMetadata = plexResult.metadata();
        if (plexMetadata != null) {
          movieRequest.setPlexMetadataId(plexMetadata.getRatingKey());
          movieRequest.setPlexAddedAt(plexMetadata.getAddedAt());
          movieRequest.setPlexUpdatedAt(plexMetadata.getUpdatedAt());
          if (plexMetadata.getGuids() != null) {
            plexMetadata.getGuids().stream()
                .map(g -> g.getId())
                .filter(gid -> gid != null && gid.startsWith("tmdb://"))
                .map(gid -> gid.substring("tmdb://".length()))
                .mapToInt(Integer::parseInt)
                .findFirst()
                .ifPresent(movieRequest::setPlexTmdbid);
          }
          if (plexMetadata.getMedia() != null && !plexMetadata.getMedia().isEmpty()) {
            PlexMedia media = plexMetadata.getMedia().get(0);
            movieRequest.setPlexMediaId(media.getId());
            movieRequest.setPlexMediaDuration(media.getDuration());
            if (media.getPart() != null && !media.getPart().isEmpty()) {
              PlexPart part = media.getPart().get(0);
              movieRequest.setPlexMediaFilename(part.getFile());
              movieRequest.setPlexMediaSize(part.getSize());
            }
          }
        }
      } catch (Exception e) {
        log.warn("Plex lookup failed during refresh for movie request {} ({})", id, movieRequest.getTitle(), e);
      }
    }

    movieRequestRepository.save(movieRequest);
    log.info("MovieRequest {} ({}) refreshed.", id, movieRequest.getTitle());
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

    log.info("Searching Radarr for Movie with tmdbId {}", movieRequest.getTmdbid());
    List<Movie> radarrMovies = radarrClient.getMoviesByTmdbId(movieRequest.getTmdbid());
    if (radarrMovies.isEmpty()) {
      log.warn("Movie was not returned by Radarr. Did the Ombi request not make it?");
    } else {
      Movie radarrMovie = radarrMovies.get(0);
      log.info("Found Movie {} ({}). Updating MovieRequest with Radarr information...", radarrMovie.getTitle(),
          radarrMovie.getId());
      movieRequest.setRadarrRequestId(radarrMovie.getId());
      movieRequest.setRadarrHasFile(radarrMovie.getHasFile());
      movieRequest.setRadarrMonitored(radarrMovie.getMonitored());
      movieRequest.setRadarrIsAvailable(radarrMovie.getIsAvailable());
      movieRequestRepository.save(movieRequest);
    }

    log.info("MovieRequest successfully re-processed.");
    return "redirect:/movies";
  }
}
