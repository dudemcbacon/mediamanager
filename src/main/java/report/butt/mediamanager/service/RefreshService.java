package report.butt.mediamanager.service;

import java.util.List;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import report.butt.mediamanager.client.MetadataResult;
import report.butt.mediamanager.client.OmbiClient;
import report.butt.mediamanager.client.PlexClient;
import report.butt.mediamanager.client.RadarrClient;
import report.butt.mediamanager.exceptions.MovieRequestNotFoundException;
import report.butt.mediamanager.model.MovieRequest;
import report.butt.mediamanager.model.ombi.OmbiMovieRequest;
import report.butt.mediamanager.model.plex.PlexMedia;
import report.butt.mediamanager.model.plex.PlexMetadata;
import report.butt.mediamanager.model.plex.PlexPart;
import report.butt.mediamanager.model.radarr.Movie;
import report.butt.mediamanager.repository.MovieRequestRepository;

@Service
public class RefreshService {

  private static final Logger log = LoggerFactory.getLogger(RefreshService.class);

  private final MovieRequestRepository repository;
  private final OmbiClient ombiClient;
  private final RadarrClient radarrClient;
  private final PlexClient plexClient;

  public RefreshService(MovieRequestRepository repository, OmbiClient ombiClient, RadarrClient radarrClient,
      PlexClient plexClient) {
    this.repository = repository;
    this.ombiClient = ombiClient;
    this.radarrClient = radarrClient;
    this.plexClient = plexClient;
  }

  public void refreshAll() {
    List<OmbiMovieRequest> ombiMovies = ombiClient.getMovies();
    List<Movie> radarrMovies = radarrClient.getMovies();

    ombiMovies.forEach(ombiMovie -> {
      MovieRequest movieRequest = repository.findByOmbiRequestId(ombiMovie.getId())
          .orElseGet(() -> new MovieRequest(ombiMovie.getTitle(),
              ombiMovie.getTheMovieDbId(),
              ombiMovie.getAvailable(),
              ombiMovie.getId(),
              ombiMovie.getRequestStatus()));

      Movie radarrMovie = radarrMovies.stream()
          .filter(rm -> Objects.equals(rm.getTmdbId(), ombiMovie.getTheMovieDbId()))
          .findFirst()
          .orElse(null);

      applyUpdates(movieRequest, ombiMovie, radarrMovie);
      repository.save(movieRequest);
      log.info("Refreshed {}", movieRequest);
    });
  }

  public void refreshOne(Long id) {
    MovieRequest movieRequest = repository.findById(id)
        .orElseThrow(() -> new MovieRequestNotFoundException(id));

    Integer ombiRequestId = movieRequest.getOmbiRequestId();
    OmbiMovieRequest ombiMovie = ombiRequestId == null ? null
        : ombiClient.getMovies().stream()
            .filter(m -> ombiRequestId.equals(m.getId()))
            .findFirst()
            .orElse(null);

    Integer tmdbid = ombiMovie != null ? ombiMovie.getTheMovieDbId() : movieRequest.getTmdbid();
    Movie radarrMovie = null;
    if (tmdbid != null) {
      List<Movie> radarrMovies = radarrClient.getMoviesByTmdbId(tmdbid);
      if (!radarrMovies.isEmpty()) {
        radarrMovie = radarrMovies.get(0);
      }
    }

    applyUpdates(movieRequest, ombiMovie, radarrMovie);
    repository.save(movieRequest);
    log.info("Refreshed {} ({})", id, movieRequest.getTitle());
  }

  private void applyUpdates(MovieRequest movieRequest, OmbiMovieRequest ombiMovie, Movie radarrMovie) {
    if (ombiMovie != null) {
      String ombiUserName = ombiMovie.getRequestedUser() == null
          ? null
          : ombiMovie.getRequestedUser().getUserName();
      movieRequest.setTitle(ombiMovie.getTitle());
      movieRequest.setTmdbid(ombiMovie.getTheMovieDbId());
      movieRequest.setOmbiAvailable(ombiMovie.getAvailable());
      movieRequest.setOmbiRequestStatus(ombiMovie.getRequestStatus());
      movieRequest.setOmbiUserName(ombiUserName);
    }

    if (radarrMovie != null) {
      movieRequest.setRadarrRequestId(radarrMovie.getId());
      movieRequest.setRadarrHasFile(radarrMovie.getHasFile());
      movieRequest.setRadarrMonitored(radarrMovie.getMonitored());
      movieRequest.setRadarrIsAvailable(radarrMovie.getIsAvailable());
      movieRequest.setRadarrPath(radarrMovie.getPath());
      movieRequest.setRadarrRootFolderPath(radarrMovie.getRootFolderPath());
      movieRequest.setRadarrOriginalLanguage(
          radarrMovie.getOriginalLanguage() == null ? null : radarrMovie.getOriginalLanguage().getName());
      movieRequest.setRadarrHistoryCount(radarrClient.getMovieHistory(radarrMovie.getId()).size());
      applyPlexUpdates(movieRequest, radarrMovie);
    }
  }

  private void applyPlexUpdates(MovieRequest movieRequest, Movie radarrMovie) {
    try {
      MetadataResult plexResult = plexClient.getMovieByTmdbId(radarrMovie.getTmdbId(),
          radarrMovie.getTitle(), radarrMovie.getYear());
      movieRequest.setPlexMetadataUrl(plexResult.url());
      PlexMetadata plexMetadata = plexResult.metadata();
      if (plexMetadata != null) {
        log.info("Plex match found for tmdbId {} (url={}): {}", radarrMovie.getTmdbId(),
            plexResult.url(), plexMetadata.getTitle());
        movieRequest.setPlexMetadataId(plexMetadata.getRatingKey());
        movieRequest.setPlexAddedAt(plexMetadata.getAddedAt());
        movieRequest.setPlexUpdatedAt(plexMetadata.getUpdatedAt());
        if (plexMetadata.getGuids() != null) {
          plexMetadata.getGuids().stream()
              .map(g -> g.getId())
              .filter(id -> id != null && id.startsWith("tmdb://"))
              .map(id -> id.substring("tmdb://".length()))
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
      } else {
        log.info("No Plex match found for tmdbId {}", radarrMovie.getTmdbId());
      }
    } catch (Exception e) {
      log.warn("Plex lookup failed for tmdbId {}", radarrMovie.getTmdbId(), e);
    }
  }
}
