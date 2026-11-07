package io.github.wedelsigma.earthsmp.core;

/** Time calculations for the global PvP combat tag; testable without Paper. */
public final class CombatTagRules {
    public static final long DURATION_MILLIS = 60_000L;
    private CombatTagRules() { }

    public static long expiresAt(long hitAt) {
        return Math.addExact(hitAt, DURATION_MILLIS);
    }

    public static boolean isTagged(long now, long until) {
        return now < until;
    }

    public static int remainingSeconds(long now, long until) {
        if (now >= until) return 0;
        return (int) Math.min(Integer.MAX_VALUE, (until - now + 999L) / 1000L);
    }
}
