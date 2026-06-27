package report.butt.mediamanager.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;

@NullMarked
class StucknessTest {

    private static final double DELTA = 0.02;

    @Test
    void newHealthyDownloadIsNotStuck() {
        // Plenty of seeds, just queued, no progress yet → the time gate keeps it near zero.
        double score = Stuckness.score(20, Duration.ofMinutes(10), 0.0, 0.0);
        assertTrue(score < 0.05, "expected ~0 but was " + score);
    }

    @Test
    void seedlessStalledDownloadIsMaximallyStuck() {
        // No seeds, waited well past the time cap, no progress → everything points to stuck.
        assertEquals(1.0, Stuckness.score(0, Duration.ofDays(10), 0.0, 2.0), DELTA);
    }

    @Test
    void nearlyCompleteRecentDownloadReadsAsLowStuck() {
        // 95% done with only a short drought → progress dampens it down, even with no seeds.
        double score = Stuckness.score(0, Duration.ofDays(10), 95.0, 2.0);
        assertTrue(score < 0.05, "expected ~0 but was " + score);
    }

    @Test
    void longDroughtOverridesHighProgress() {
        // The dead-but-nearly-complete case: 95% done but seedless for 90 days → floored at fully stuck.
        assertEquals(1.0, Stuckness.score(0, Duration.ofDays(10), 95.0, 90.0), DELTA);
    }

    @Test
    void droughtCurveIsLowThroughTwoMonthsThenSaturatesAtNinety() {
        // progress=100 zeroes the base, isolating the drought term so the curve shape can be asserted directly.
        assertEquals(0.012, Stuckness.score(0, Duration.ZERO, 100.0, 30.0), DELTA);
        assertEquals(0.198, Stuckness.score(0, Duration.ZERO, 100.0, 60.0), DELTA);
        assertEquals(1.0, Stuckness.score(0, Duration.ZERO, 100.0, 90.0), DELTA);
    }

    @Test
    void droughtClampsToOnePastNinetyDays() {
        assertEquals(1.0, Stuckness.score(0, Duration.ZERO, 100.0, 180.0), DELTA);
    }

    @Test
    void scoreStaysWithinUnitInterval() {
        double score = Stuckness.score(0, Duration.ofDays(365), 0.0, 365.0);
        assertTrue(score >= 0.0 && score <= 1.0, "out of range: " + score);
    }
}
