package report.butt.mediamanager.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.jspecify.annotations.NullMarked;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;
import report.butt.mediamanager.model.Request;
import report.butt.mediamanager.model.TvEpisodeRequest;
import report.butt.mediamanager.model.Validation;

@NullMarked
public interface ValidationRepository extends JpaRepository<Validation, Long> {
    Optional<Validation> findByRequestAndValidationName(Request request, String validationName);

    Optional<Validation> findByTvEpisodeAndValidationName(TvEpisodeRequest tvEpisode, String validationName);

    List<Validation> findByRequest(Request request);

    List<Validation> findByTvEpisode(TvEpisodeRequest tvEpisode);

    /** All validations for the given episodes, fetched in a single {@code IN} query (used for one show's refresh). */
    List<Validation> findByTvEpisodeIn(Collection<TvEpisodeRequest> tvEpisodes);

    @Transactional
    void deleteByRequest(Request request);

    @Transactional
    void deleteByTvEpisode(TvEpisodeRequest tvEpisode);
}
