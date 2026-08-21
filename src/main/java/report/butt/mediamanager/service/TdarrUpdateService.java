package report.butt.mediamanager.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.jspecify.annotations.NullMarked;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import report.butt.mediamanager.model.MovieRequest;
import report.butt.mediamanager.model.TvEpisodeRequest;
import report.butt.mediamanager.model.tdarr.TdarrTranscodeUpdate;
import report.butt.mediamanager.repository.MovieRequestRepository;
import report.butt.mediamanager.repository.TvEpisodeRequestRepository;

/**
 * Applies a Tdarr transcode-complete payload to the library by resolving its filename against the Plex paths already
 * stored on movies ({@code plexMediaFilename}) and TV episodes ({@code plexPath}) — the same fields a refresh uses to
 * look a file up in Tdarr.
 *
 * <p>Resolution depends on what the payload carries:
 *
 * <ul>
 *   <li>a value containing {@code /} is treated as a full path and matched exactly, so a precise path can never spill
 *       onto a same-named file in another folder;
 *   <li>a bare filename matches every movie and episode whose path ends with it. That is deliberately plural: the same
 *       episode filename can legitimately exist under two shows, and all matches are updated.
 * </ul>
 */
@Service
@NullMarked
public class TdarrUpdateService {

    private static final Logger log = LoggerFactory.getLogger(TdarrUpdateService.class);

    /** LIKE escape character, chosen so neither Java nor JPQL string escaping is involved. */
    private static final char LIKE_ESCAPE = '!';

    /** What the webhook changed: one descriptor per updated row, plus per-type counts for the response. */
    // `updated` is always built with List.copyOf; there is no immutable-typed collection library on the classpath.
    @SuppressWarnings("ImmutableMemberCollection")
    public record Result(String filename, int movies, int episodes, List<String> updated) {
        public int matched() {
            return movies + episodes;
        }
    }

    private final MovieRequestRepository movieRequestRepository;
    private final TvEpisodeRequestRepository tvEpisodeRequestRepository;

    public TdarrUpdateService(
            MovieRequestRepository movieRequestRepository, TvEpisodeRequestRepository tvEpisodeRequestRepository) {
        this.movieRequestRepository = movieRequestRepository;
        this.tvEpisodeRequestRepository = tvEpisodeRequestRepository;
    }

    /**
     * Resolves {@code update.filename} and writes its Tdarr values onto every match. Null values in the payload are
     * left alone rather than clearing what is stored, so a partial payload can't wipe data.
     *
     * @return what was updated; {@link Result#matched()} is zero when the filename resolves to nothing
     * @throws IllegalArgumentException if the payload carries no filename
     */
    @Transactional
    public Result apply(TdarrTranscodeUpdate update) {
        String filename = update.getFilename();
        if (filename == null || filename.isBlank()) {
            throw new IllegalArgumentException("filename is required");
        }
        String trimmed = filename.trim();
        String pattern = likePattern(trimmed);

        // One timestamp for the whole call, so every row this payload touches shares the same stamp.
        Instant now = Instant.now();
        List<String> updated = new ArrayList<>();
        List<MovieRequest> movies = movieRequestRepository.findByPlexMediaFilenameLike(pattern);
        for (MovieRequest movie : movies) {
            applyTo(movie, update, now);
            updated.add("MOVIE " + movie.getId() + " " + movie.getPlexMediaFilename());
        }
        List<TvEpisodeRequest> episodes = tvEpisodeRequestRepository.findByPlexPathLike(pattern);
        for (TvEpisodeRequest episode : episodes) {
            applyTo(episode, update, now);
            updated.add("EPISODE " + episode.getId() + " " + episode.getPlexPath());
        }

        // Saved explicitly rather than relying on dirty checking, so the write is obvious and the method reads the same
        // whether or not the entities happen to be managed.
        movieRequestRepository.saveAll(movies);
        tvEpisodeRequestRepository.saveAll(episodes);

        var result = new Result(trimmed, movies.size(), episodes.size(), List.copyOf(updated));
        if (result.matched() == 0) {
            log.warn("Tdarr webhook: no movie or episode matches {}", trimmed);
        } else {
            log.info(
                    "Tdarr webhook: updated {} movie(s) and {} episode(s) for {} -> {}",
                    result.movies(),
                    result.episodes(),
                    trimmed,
                    update.getTranscodeDecisionMaker());
        }
        return result;
    }

    /** {@code now} is stamped unconditionally: a matched row was successfully updated, whatever the payload carried. */
    private static void applyTo(MovieRequest movie, TdarrTranscodeUpdate update, Instant now) {
        if (update.getHealthCheck() != null) {
            movie.setTdarrHealthCheck(update.getHealthCheck());
        }
        if (update.getTranscodeDecisionMaker() != null) {
            movie.setTdarrTranscodeDecisionMaker(update.getTranscodeDecisionMaker());
        }
        if (update.getOldSize() != null) {
            movie.setTdarrOldSizeGb(update.getOldSize());
        }
        if (update.getNewSize() != null) {
            movie.setTdarrNewSizeGb(update.getNewSize());
        }
        movie.setTdarrLastUpdated(now);
    }

    private static void applyTo(TvEpisodeRequest episode, TdarrTranscodeUpdate update, Instant now) {
        if (update.getHealthCheck() != null) {
            episode.setTdarrHealthCheck(update.getHealthCheck());
        }
        if (update.getTranscodeDecisionMaker() != null) {
            episode.setTdarrTranscodeDecisionMaker(update.getTranscodeDecisionMaker());
        }
        if (update.getOldSize() != null) {
            episode.setTdarrOldSizeGb(update.getOldSize());
        }
        if (update.getNewSize() != null) {
            episode.setTdarrNewSizeGb(update.getNewSize());
        }
        episode.setTdarrLastUpdated(now);
    }

    /**
     * The LIKE pattern for a payload filename: the escaped value itself when it looks like a path (matching exactly),
     * or a {@code %/name} suffix pattern for a bare filename.
     */
    static String likePattern(String filename) {
        String escaped = escapeLike(filename);
        return filename.indexOf('/') >= 0 ? escaped : "%/" + escaped;
    }

    /**
     * Neutralises LIKE metacharacters so a filename is matched literally. {@code _} matters in practice — plenty of
     * release names contain underscores, and unescaped it would match any single character.
     */
    private static String escapeLike(String value) {
        var escaped = new StringBuilder(value.length());
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c == LIKE_ESCAPE || c == '%' || c == '_') {
                escaped.append(LIKE_ESCAPE);
            }
            escaped.append(c);
        }
        return escaped.toString();
    }
}
