package report.butt.mediamanager;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import report.butt.mediamanager.model.MovieRequest;
import report.butt.mediamanager.model.OmbiMovieRequest;
import report.butt.mediamanager.repository.MovieRequestRepository;
import report.butt.mediamanager.service.OmbiService;

@Configuration
class LoadDatabase {

  private static final Logger log = LoggerFactory.getLogger(LoadDatabase.class);

  @Bean
  CommandLineRunner initDatabase(MovieRequestRepository repository, OmbiService ombiService) {

    return args -> {
      List<OmbiMovieRequest> movies = ombiService.getMovies();

      movies.stream()
          .map(ombiMovie -> new MovieRequest(ombiMovie.getTitle(), // Assuming getTitle() exists on OmbiMovieRequest
              ombiMovie.getTheMovieDbId(),
              ombiMovie.getAvailable(),
              ombiMovie.getId(),
              ombiMovie.getRequestStatus()))
          .map(repository::save)
          .forEach(savedMovie -> log.info("Preloading " + savedMovie));
    };
  }
}
