package io.github.wedelsigma.earthsmp.core;

/** Pure capture counter math. One step is measured in real seconds, never game ticks. */
public final class CaptureRules {
    public static final int REQUIRED_SECONDS = 480;
    private CaptureRules() {}

    public static int advance(int previous, long elapsedSeconds, boolean attackerInside, boolean defenderInside) {
        if (previous < 0 || previous > REQUIRED_SECONDS || elapsedSeconds < 0)
            throw new IllegalArgumentException("Invalid capture time");
        long delta = attackerInside && !defenderInside ? elapsedSeconds
                : defenderInside && !attackerInside ? -2L * elapsedSeconds : 0;
        return (int) Math.max(0L, Math.min(REQUIRED_SECONDS, previous + delta));
    }
}
