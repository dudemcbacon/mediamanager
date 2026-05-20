package report.butt.mediamanager;

import java.util.List;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import report.butt.mediamanager.client.MetadataResult;
import report.butt.mediamanager.client.OmbiClient;
import report.butt.mediamanager.model.plex.PlexMedia;
import report.butt.mediamanager.model.plex.PlexMetadata;
import report.butt.mediamanager.model.plex.PlexPart;
import report.butt.mediamanager.client.PlexClient;
import report.butt.mediamanager.client.RadarrClient;
import report.butt.mediamanager.model.MovieRequest;
import report.butt.mediamanager.model.ombi.OmbiMovieRequest;
import report.butt.mediamanager.model.radarr.Movie;
import report.butt.mediamanager.repository.MovieRequestRepository;

@Configuration
class LoadDatabase {

  private static final Logger log = LoggerFactory.getLogger(LoadDatabase.class);

  @Bean
  CommandLineRunner initDatabase(MovieRequestRepository repository, OmbiClient ombiClient, RadarrClient radarrClient,
      PlexClient plexClient) {

    return args -> {
      List<OmbiMovieRequest> ombiMovies = ombiClient.getMovies();
      List<Movie> radarMovies = radarrClient.getMovies();

      ombiMovies.stream()
          .map(ombiMovie -> {
            String ombiUserName = ombiMovie.getRequestedUser() == null
                ? null
                : ombiMovie.getRequestedUser().getUserName();
            MovieRequest movieRequest = repository.findByOmbiRequestId(ombiMovie.getId())
                .map(existing -> {
                  existing.setTitle(ombiMovie.getTitle());
                  existing.setTmdbid(ombiMovie.getTheMovieDbId());
                  existing.setOmbiAvailable(ombiMovie.getAvailable());
                  existing.setOmbiRequestStatus(ombiMovie.getRequestStatus());
                  existing.setOmbiUserName(ombiUserName);
                  return existing;
                })
                .orElseGet(() -> {
                  MovieRequest created = new MovieRequest(ombiMovie.getTitle(),
                      ombiMovie.getTheMovieDbId(),
                      ombiMovie.getAvailable(),
                      ombiMovie.getId(),
                      ombiMovie.getRequestStatus());
                  created.setOmbiUserName(ombiUserName);
                  return created;
                });
            radarMovies.stream()
                .filter(radarMovie -> Objects.equals(radarMovie.getTmdbId(), ombiMovie.getTheMovieDbId()))
                .findFirst()
                .ifPresent(radarMovie -> {
                  movieRequest.setRadarrRequestId(radarMovie.getId());
                  movieRequest.setRadarrHasFile(radarMovie.getHasFile());
                  movieRequest.setRadarrMonitored(radarMovie.getMonitored());
                  movieRequest.setRadarrIsAvailable(radarMovie.getIsAvailable());
                  movieRequest.setRadarrHistoryCount(radarrClient.getMovieHistory(radarMovie.getId()).size());
                  try {
                    MetadataResult plexResult = plexClient.getMovieByTmdbId(radarMovie.getTmdbId(),
                        radarMovie.getTitle(), radarMovie.getYear());
                    movieRequest.setPlexMetadataUrl(plexResult.url());
                    PlexMetadata plexMetadata = plexResult.metadata();
                    if (plexMetadata != null) {
                      log.info("Plex match found for tmdbId {} (url={}): {}", radarMovie.getTmdbId(),
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
                      log.info("No Plex match found for tmdbId {}", radarMovie.getTmdbId());
                    }
                  } catch (Exception e) {
                    log.warn("Plex lookup failed for tmdbId {}", radarMovie.getTmdbId(), e);
                  }
                });
            return movieRequest;
          })
          .map(repository::save)
          .forEach(savedMovie -> log.info("Preloading " + savedMovie));
    };
  }
}
