package io.github.wedelsigma.earthsmp.core;

/** Pure, testable rules for the 30-second /home preparation. */
final class HomeWarmupRules {
    static final int WARMUP_SECONDS = 30;
    static final long WARMUP_NANOS = WARMUP_SECONDS * 1_000_000_000L;

    private HomeWarmupRules() { }

    static int remainingSeconds(long deadlineNanos, long nowNanos) {
        long left = deadlineNanos - nowNanos;
        if (left <= 0L) return 0;
        return (int) Math.min(WARMUP_SECONDS, (left + 999_999_999L) / 1_000_000_000L);
    }

    /** Looking around (yaw/pitch) does not count as movement; any XYZ change does. */
    static boolean moved(double startX, double startY, double startZ,
                         double x, double y, double z) {
        return Double.compare(startX, x) != 0
                || Double.compare(startY, y) != 0
                || Double.compare(startZ, z) != 0;
    }
}
