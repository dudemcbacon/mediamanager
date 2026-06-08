package report.butt.mediamanager.route;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import report.butt.mediamanager.model.MovieRequest;
import report.butt.mediamanager.route.AdminView.RequesterCount;

class AdminViewLeaderboardTest {

    private static MovieRequest requestedBy(String username) {
        MovieRequest movie = new MovieRequest("Movie", 1, false, 1, "Common.ProcessingRequest");
        movie.setOmbiUserName(username);
        return movie;
    }

    private static MovieRequest availableRequestedBy(String username) {
        // isAvailable() requires radarrHasFile==true AND ombiRequestStatus==Common.Available.
        MovieRequest movie = new MovieRequest("Movie", 1, true, 1, "Common.Available");
        movie.setRadarrHasFile(true);
        movie.setOmbiUserName(username);
        return movie;
    }

    @Test
    void countsPerUserSortedDescending() {
        List<RequesterCount> board = AdminView.leaderboard(
                List.of(requestedBy("alice"), requestedBy("alice"), requestedBy("alice"), requestedBy("bob")));

        assertEquals(2, board.size());
        assertEquals("alice", board.get(0).username());
        assertEquals(3, board.get(0).count());
        assertEquals("bob", board.get(1).username());
        assertEquals(1, board.get(1).count());
    }

    @Test
    void blankOrNullUsernamesBucketIntoUnknown() {
        List<RequesterCount> board =
                AdminView.leaderboard(List.of(requestedBy(null), requestedBy("  "), requestedBy("carol")));

        assertEquals(2, board.size());
        assertEquals("unknown", board.get(0).username()); // 2 anonymous requests → top
        assertEquals(2, board.get(0).count());
        assertTrue(board.stream().anyMatch(r -> "carol".equals(r.username()) && r.count() == 1));
    }

    @Test
    void emptyInputYieldsEmptyLeaderboard() {
        assertTrue(AdminView.leaderboard(List.of()).isEmpty());
    }

    @Test
    void computesAvailablePercentage() {
        // alice: 4 requests, 1 available → 25%.
        List<RequesterCount> board = AdminView.leaderboard(List.of(
                availableRequestedBy("alice"), requestedBy("alice"), requestedBy("alice"), requestedBy("alice")));

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
        assertEquals(10, AdminView.leaderboard(many).size());
    }
}
