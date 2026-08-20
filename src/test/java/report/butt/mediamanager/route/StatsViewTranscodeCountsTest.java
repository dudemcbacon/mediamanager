package report.butt.mediamanager.route;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import report.butt.mediamanager.model.MovieRequest;
import report.butt.mediamanager.route.StatsView.TranscodeCount;

/** Covers the Tdarr transcode-verdict aggregation behind the Movie transcode status stats table. */
@NullMarked
class StatsViewTranscodeCountsTest {

    private static List<MovieRequest> moviesWithVerdicts(@Nullable String... verdicts) {
        return Arrays.stream(verdicts)
                .map(verdict -> {
                    var movie = new MovieRequest("Title", 100, true, 1, "Common.Available");
                    movie.setTdarrTranscodeDecisionMaker(verdict);
                    return movie;
                })
                .toList();
    }

    @Test
    void countsPerVerdictSortedByCountDescending() {
        List<TranscodeCount> counts =
                StatsView.transcodeCounts(moviesWithVerdicts("Transcode success", "Transcode success", "Queued"));

        assertEquals(2, counts.size());
        assertEquals("Transcode success", counts.get(0).status());
        assertEquals(2, counts.get(0).count());
        assertEquals("Queued", counts.get(1).status());
        assertEquals(1, counts.get(1).count());
    }

    @Test
    void moviesWithoutAVerdictBucketIntoNoData() {
        List<TranscodeCount> counts = StatsView.transcodeCounts(moviesWithVerdicts("Not required", null));

        assertEquals(
                Map.of("Not required", 1L, StatsView.NO_TRANSCODE_DATA, 1L),
                counts.stream().collect(Collectors.toMap(TranscodeCount::status, TranscodeCount::count)));
    }

    @Test
    void blankVerdictIsTreatedAsMissing() {
        List<TranscodeCount> counts = StatsView.transcodeCounts(moviesWithVerdicts("   "));

        assertEquals(1, counts.size());
        assertEquals(StatsView.NO_TRANSCODE_DATA, counts.get(0).status());
    }

    /** Unlike codecs, verdicts keep Tdarr's casing — they are display phrases, so they must not be lowercased. */
    @Test
    void verdictCasingIsPreserved() {
        List<TranscodeCount> counts = StatsView.transcodeCounts(moviesWithVerdicts("Transcode error"));

        assertEquals("Transcode error", counts.get(0).status());
    }

    @Test
    void sharesAreRelativeToTheWholeLibraryAndSumToOneHundred() {
        List<TranscodeCount> counts = StatsView.transcodeCounts(
                moviesWithVerdicts("Transcode success", "Transcode success", "Transcode success", "Queued"));

        assertEquals(75.0, counts.get(0).percentOfLibrary(), 0.001);
        assertEquals(25.0, counts.get(1).percentOfLibrary(), 0.001);
        assertEquals(
                100.0,
                counts.stream().mapToDouble(TranscodeCount::percentOfLibrary).sum(),
                0.001);
    }

    @Test
    void countsSumToTheLibrarySize() {
        List<TranscodeCount> counts = StatsView.transcodeCounts(moviesWithVerdicts("Queued", null, null));

        assertEquals(3, counts.stream().mapToLong(TranscodeCount::count).sum());
    }

    @Test
    void emptyLibraryYieldsNoRows() {
        assertTrue(StatsView.transcodeCounts(List.of()).isEmpty());
    }

    // --- episodes: the same aggregation, fed by the database's GROUP BY rows instead of entities ---

    private static List<Object[]> groupedRows(Object... verdictThenCount) {
        List<Object[]> rows = new ArrayList<>();
        for (int i = 0; i < verdictThenCount.length; i += 2) {
            rows.add(new Object[] {verdictThenCount[i], verdictThenCount[i + 1]});
        }
        return rows;
    }

    @Test
    void episodeCountsPerVerdictSortedByCountDescending() {
        List<TranscodeCount> counts =
                StatsView.episodeTranscodeCounts(groupedRows("Queued", 3L, "Transcode success", 7L));

        assertEquals(2, counts.size());
        assertEquals("Transcode success", counts.get(0).status());
        assertEquals(7, counts.get(0).count());
        assertEquals("Queued", counts.get(1).status());
        assertEquals(3, counts.get(1).count());
    }

    /** The GROUP BY returns a null verdict for episodes Tdarr hasn't reported on. */
    @Test
    void episodesWithANullVerdictBucketIntoNoData() {
        List<TranscodeCount> counts = StatsView.episodeTranscodeCounts(groupedRows("Not required", 2L, null, 5L));

        assertEquals(
                Map.of("Not required", 2L, StatsView.NO_TRANSCODE_DATA, 5L),
                counts.stream().collect(Collectors.toMap(TranscodeCount::status, TranscodeCount::count)));
    }

    /** A null and a blank verdict are the same bucket, so their counts must be summed rather than clobbering. */
    @Test
    void nullAndBlankVerdictsMergeIntoOneRow() {
        List<TranscodeCount> counts = StatsView.episodeTranscodeCounts(groupedRows(null, 4L, "   ", 6L));

        assertEquals(1, counts.size());
        assertEquals(StatsView.NO_TRANSCODE_DATA, counts.get(0).status());
        assertEquals(10, counts.get(0).count());
        assertEquals(100.0, counts.get(0).percentOfLibrary(), 0.001);
    }

    @Test
    void episodeSharesAreRelativeToTheWholeLibrary() {
        List<TranscodeCount> counts =
                StatsView.episodeTranscodeCounts(groupedRows("Transcode success", 30L, "Queued", 10L));

        assertEquals(75.0, counts.get(0).percentOfLibrary(), 0.001);
        assertEquals(25.0, counts.get(1).percentOfLibrary(), 0.001);
    }

    /** JPA's COUNT can come back as any Number subtype depending on the dialect. */
    @Test
    void episodeCountsAcceptAnyNumberType() {
        List<TranscodeCount> counts = StatsView.episodeTranscodeCounts(groupedRows("Queued", Integer.valueOf(2)));

        assertEquals(2, counts.get(0).count());
    }

    @Test
    void emptyEpisodeLibraryYieldsNoRows() {
        assertTrue(StatsView.episodeTranscodeCounts(List.of()).isEmpty());
    }
}
