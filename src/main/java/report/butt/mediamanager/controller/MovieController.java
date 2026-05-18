package report.butt.mediamanager.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import report.butt.mediamanager.model.MovieRequest;
import report.butt.mediamanager.repository.MovieRequestRepository;

@Controller
public class MovieController {

  private final MovieRequestRepository movieRequestRepository;

  @Autowired
  public MovieController(MovieRequestRepository movieRequestRepository) {
    this.movieRequestRepository = movieRequestRepository;
  }

  @GetMapping("/movies")
  public String movies(Model model) {
    List<MovieRequest> movieRequests = movieRequestRepository.findAll();

    model.addAttribute("movies", movieRequests);
    return "movies";
  }
}
