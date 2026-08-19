package report.butt.mediamanager.route;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;
import report.butt.mediamanager.route.StatsView.CodecCount;

/** Covers the codec aggregation shared by the Movie codecs and TV episode codecs stats tables. */
@NullMarked
class StatsViewCodecCountsTest {

    @Test
    void countsPerCodecSortedByCountDescending() {
        List<CodecCount> counts =
                StatsView.codecCounts(List.of(1L, 2L, 3L), Map.of(1L, "hevc", 2L, "hevc", 3L, "h264"));

        assertEquals(2, counts.size());
        assertEquals("hevc", counts.get(0).codec());
        assertEquals(2, counts.get(0).count());
        assertEquals("h264", counts.get(1).codec());
        assertEquals(1, counts.get(1).count());
    }

    @Test
    void requestsWithoutACodecBucketIntoNoCodec() {
        List<CodecCount> counts = StatsView.codecCounts(List.of(1L, 2L), Map.of(1L, "av1"));

        assertEquals(
                Map.of("av1", 1L, StatsView.NO_CODEC, 1L),
                counts.stream().collect(Collectors.toMap(CodecCount::codec, CodecCount::count)));
    }

    @Test
    void blankCodecIsTreatedAsMissing() {
        List<CodecCount> counts = StatsView.codecCounts(List.of(1L), Map.of(1L, "   "));

        assertEquals(1, counts.size());
        assertEquals(StatsView.NO_CODEC, counts.get(0).codec());
    }

    @Test
    void codecCasingIsNormalizedIntoOneRow() {
        List<CodecCount> counts = StatsView.codecCounts(List.of(1L, 2L), Map.of(1L, "HEVC", 2L, "hevc"));

        assertEquals(1, counts.size());
        assertEquals("hevc", counts.get(0).codec());
        assertEquals(2, counts.get(0).count());
    }

    /** Scans hold only a soft reference to their request, so the map can outlive a deleted movie or episode. */
    @Test
    void codecsForRequestsThatNoLongerExistAreIgnored() {
        List<CodecCount> counts = StatsView.codecCounts(List.of(1L), Map.of(1L, "hevc", 99L, "av1"));

        assertEquals(1, counts.size());
        assertEquals("hevc", counts.get(0).codec());
        assertEquals(1, counts.get(0).count());
    }

    @Test
    void sharesAreRelativeToTheWholeLibraryAndSumToOneHundred() {
        List<CodecCount> counts =
                StatsView.codecCounts(List.of(1L, 2L, 3L, 4L), Map.of(1L, "hevc", 2L, "hevc", 3L, "hevc"));

        assertEquals(75.0, counts.get(0).percentOfLibrary(), 0.001);
        assertEquals(25.0, counts.get(1).percentOfLibrary(), 0.001);
        assertEquals(
                100.0, counts.stream().mapToDouble(CodecCount::percentOfLibrary).sum(), 0.001);
    }

    @Test
    void countsSumToTheLibrarySize() {
        List<CodecCount> counts = StatsView.codecCounts(List.of(1L, 2L, 3L), Map.of(1L, "av1"));

        assertEquals(3, counts.stream().mapToLong(CodecCount::count).sum());
    }

    @Test
    void emptyLibraryYieldsNoRows() {
        assertTrue(StatsView.codecCounts(List.of(), Map.of()).isEmpty());
    }
}
