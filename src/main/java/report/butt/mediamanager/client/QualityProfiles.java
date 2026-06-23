package report.butt.mediamanager.client;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Shared helpers for the Radarr/Sonarr quality-profile caches. Radarr and Sonarr expose distinct (generated)
 * {@code QualityProfile} types, so the id/name extractors are passed in rather than relying on a common interface.
 */
@NullMarked
final class QualityProfiles {

    private static final Logger log = LoggerFactory.getLogger(QualityProfiles.class);

    private QualityProfiles() {}

    /**
     * Builds an id&rarr;name map from fetched profiles (skipping null ids), logging the count under {@code source}.
     * Returns an empty map when {@code profiles} is null.
     */
    static <T> Map<Integer, String> index(
            @Nullable List<T> profiles, Function<T, Integer> idFn, Function<T, String> nameFn, String source) {
        Map<Integer, String> byId = profiles == null
                ? Map.of()
                : profiles.stream()
                        .filter(p -> idFn.apply(p) != null)
                        .collect(Collectors.toMap(idFn, nameFn, (a, b) -> a));
        log.info("Cached {} {} quality profiles", byId.size(), source);
        return byId;
    }

    /** Resolves a cached quality profile id by its (exact) name, or null if none matches. */
    static @Nullable Integer idByName(Map<Integer, String> profilesById, String name) {
        return profilesById.entrySet().stream()
                .filter(e -> e.getValue() != null && e.getValue().equals(name))
                .map(Map.Entry::getKey)
                .findFirst()
                .orElse(null);
    }
}
