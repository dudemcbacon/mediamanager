package report.butt.mediamanager.controller;

import java.util.List;
import org.jspecify.annotations.NullMarked;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import report.butt.mediamanager.exceptions.RequestNotFoundException;
import report.butt.mediamanager.model.MovieRequest;
import report.butt.mediamanager.repository.MovieRequestRepository;

@RestController
@NullMarked
// This raw CRUD REST API has no callers in the app (the UI uses the Vaadin views + MovieController); it appears to be
// leftover scaffolding and is a candidate for removal. Until then, lock the whole thing down to ADMIN.
@PreAuthorize("hasRole('ADMIN')")
public class MovieRequestController {

    private final MovieRequestRepository repository;

    public MovieRequestController(MovieRequestRepository repository) {
        this.repository = repository;
    }

    // Aggregate root
    // tag::get-aggregate-root[]
    @GetMapping("/movie_requests")
    List<MovieRequest> all() {
        return repository.findAll();
    }
    // end::get-aggregate-root[]

    @PostMapping("/movie_requests")
    MovieRequest newMovieRequest(@RequestBody MovieRequest newMovieRequest) {
        return repository.save(newMovieRequest);
    }

    // Single item

    @GetMapping("/movie_requests/{id}")
    MovieRequest one(@PathVariable Long id) {

        return repository.findById(id).orElseThrow(() -> new RequestNotFoundException(id));
    }

    @PutMapping("/movie_requests/{id}")
    MovieRequest replaceMovieRequest(@RequestBody MovieRequest newMovieRequest, @PathVariable Long id) {

        return repository
                .findById(id)
                .map(movieRequest -> {
                    movieRequest.setTitle(newMovieRequest.getTitle());
                    movieRequest.setTmdbid(newMovieRequest.getTmdbid());
                    movieRequest.setOmbiAvailable(newMovieRequest.getOmbiAvailable());
                    movieRequest.setOmbiRequestId(newMovieRequest.getOmbiRequestId());
                    movieRequest.setOmbiRequestStatus(newMovieRequest.getOmbiRequestStatus());
                    return repository.save(movieRequest);
                })
                .orElseGet(() -> {
                    return repository.save(newMovieRequest);
                });
    }

    @DeleteMapping("/movie_requests/{id}")
    void deleteMovieRequest(@PathVariable Long id) {
        repository.deleteById(id);
    }
}
