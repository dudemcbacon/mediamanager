package report.butt.mediamanager.route;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import report.butt.mediamanager.model.MovieRequest;
import report.butt.mediamanager.route.StatsView.RequesterCount;

@NullMarked
class StatsViewLeaderboardTest {

    private static MovieRequest requestedBy(@Nullable String username) {
        var movie = new MovieRequest("Movie", 1, false, 1, "Common.ProcessingRequest");
        movie.setOmbiUserName(username);
        return movie;
    }

    private static MovieRequest availableRequestedBy(String username) {
        // isAvailable() requires radarrHasFile==true AND ombiRequestStatus==Common.Available.
        var movie = new MovieRequest("Movie", 1, true, 1, "Common.Available");
        movie.setRadarrHasFile(true);
        movie.setOmbiUserName(username);
        return movie;
    }

    @Test
    void countsPerUserSortedDescending() {
        List<RequesterCount> board = StatsView.leaderboard(
                List.of(requestedBy("alice"), requestedBy("alice"), requestedBy("alice"), requestedBy("bob")),
                Map.of());

        assertEquals(2, board.size());
        assertEquals("alice", board.get(0).username());
        assertEquals(3, board.get(0).count());
        assertEquals("bob", board.get(1).username());
        assertEquals(1, board.get(1).count());
    }

    @Test
    void blankOrNullUsernamesBucketIntoUnknown() {
        List<RequesterCount> board =
                StatsView.leaderboard(List.of(requestedBy(null), requestedBy("  "), requestedBy("carol")), Map.of());

        assertEquals(2, board.size());
        assertEquals("unknown", board.get(0).username()); // 2 anonymous requests → top
        assertEquals(2, board.get(0).count());
        assertTrue(board.stream().anyMatch(r -> Objects.equals(r.username(), "carol") && r.count() == 1));
    }

    @Test
    void emptyInputYieldsEmptyLeaderboard() {
        assertTrue(StatsView.leaderboard(List.of(), Map.of()).isEmpty());
    }

    @Test
    void computesAvailablePercentage() {
        // alice: 4 requests, 1 available → 25%.
        List<RequesterCount> board = StatsView.leaderboard(
                List.of(
                        availableRequestedBy("alice"),
                        requestedBy("alice"),
                        requestedBy("alice"),
                        requestedBy("alice")),
                Map.of());

        assertEquals(1, board.size());
        assertEquals(4, board.get(0).count());
        assertEquals(1, board.get(0).available());
        assertEquals(25.0, board.get(0).percentComplete(), 0.001);
    }

    @Test
    void limitsToTopTen() {
        List<MovieRequest> many = new ArrayList<>();
        for (int i = 0; i < 12; i++) {
            many.add(requestedBy("user-" + i));
        }
        assertEquals(10, StatsView.leaderboard(many, Map.of()).size());
    }

    @Test
    void bytesLookedUpByNormalizedUserKey() {
        // bytesByUser keys are pre-normalized: blank/null users roll up to "unknown".
        var bytes = Map.of("alice", 5_000L, "unknown", 2_000L);
        List<RequesterCount> board = StatsView.leaderboard(
                List.of(requestedBy("alice"), requestedBy("alice"), requestedBy(null), requestedBy("bob")), bytes);

        // alice: 5000, unknown: 2000, bob: 0 (no entry).
        assertEquals(
                5_000L,
                board.stream()
                        .filter(r -> Objects.equals(r.username(), "alice"))
                        .findFirst()
                        .orElseThrow()
                        .bytes());
        assertEquals(
                2_000L,
                board.stream()
                        .filter(r -> Objects.equals(r.username(), "unknown"))
                        .findFirst()
                        .orElseThrow()
                        .bytes());
        assertEquals(
                0L,
                board.stream()
                        .filter(r -> Objects.equals(r.username(), "bob"))
                        .findFirst()
                        .orElseThrow()
                        .bytes());
    }
}
