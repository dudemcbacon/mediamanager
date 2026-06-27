package report.butt.mediamanager.service;

import java.time.Duration;
import org.jspecify.annotations.NullMarked;

/**
 * Computes a torrent's "stuckness": a 0..1 score of how unlikely a download is to ever finish, used to surface and
 * prioritise removal of dead downloads. It blends four signals:
 *
 * <ul>
 *   <li><b>seeds</b> — fewer seeds means less able to recover; decays exponentially (0 seeds → 1).
 *   <li><b>age in queue</b> — longer waits raise the pressure, saturating at {@link #TIME_MAX}.
 *   <li><b>progress</b> — dampens the score as the download nears completion, so a near-done torrent reads as
 *       <i>less</i> stuck.
 *   <li><b>days without seeds</b> — a long seedless drought means "dead, never coming back". It ramps up a power curve
 *       (low through the first two months, ~1 by {@link #DROUGHT_DAYS}) and overrides the progress dampener, so a
 *       torrent stalled at 95% for months is still caught.
 * </ul>
 *
 * <p>Usenet (SABnzbd) downloads expose neither a seed count nor a queue timestamp, so this is torrent-only.
 */
@NullMarked
public final class Stuckness {

    /** Seed-count decay scale: 3 seeds → ~0.37, 10 seeds → ~0.03. */
    private static final double SEED_SCALE = 3.0;

    /** Age at which the time pressure saturates. */
    private static final Duration TIME_MAX = Duration.ofDays(7);

    /** Progress dampening exponent: higher sharpens the drop-off near completion. */
    private static final double PROGRESS_GAMMA = 2.0;

    /** Days seedless at which the drought score reaches 1 (and clamps there). */
    private static final double DROUGHT_DAYS = 90.0;

    /** Drought ramp exponent: keeps days 30/60 low, then slams to 1 by day 90. */
    private static final double DROUGHT_POWER = 4.0;

    /** Relative weights of the age and seed signals in the base score (sum to 1). */
    private static final double WEIGHT_TIME = 0.5;

    private static final double WEIGHT_SEED = 0.5;

    private Stuckness() {}

    /**
     * Computes the stuckness score for one torrent.
     *
     * @param seeds total seeds in the swarm (0 if unknown)
     * @param age how long the torrent has been queued
     * @param progressPercent download progress, 0..100
     * @param daysWithoutSeeds days the torrent has been continuously seedless (0 if it currently has seeds)
     * @return stuckness in [0, 1]
     */
    public static double score(int seeds, Duration age, double progressPercent, double daysWithoutSeeds) {
        double timeScore = clamp01((double) age.toSeconds() / TIME_MAX.toSeconds());
        double seedScore = Math.exp(-Math.max(0, seeds) / SEED_SCALE);
        double progressDamp = Math.pow(1.0 - clamp01(progressPercent / 100.0), PROGRESS_GAMMA);
        double base = progressDamp * (WEIGHT_TIME * timeScore + WEIGHT_SEED * seedScore);

        // A long seedless drought floors the score regardless of progress — the deliberate exception to the
        // "near-complete reads as less stuck" rule, so dead-but-nearly-finished torrents are still caught.
        double droughtScore = Math.min(1.0, Math.pow(Math.max(0.0, daysWithoutSeeds) / DROUGHT_DAYS, DROUGHT_POWER));

        return Math.max(base, droughtScore);
    }

    private static double clamp01(double value) {
        return Math.min(1.0, Math.max(0.0, value));
    }
}
