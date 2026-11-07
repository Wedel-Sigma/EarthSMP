package io.github.wedelsigma.earthsmp.core;

/** Pure timing rules, independent of Paper/SQLite, so edge cases can be unit-tested. */
public final class PvpRules {
    public static final long SHIELD_MILLIS = 8_000L;
    public static final long COOLDOWN_MILLIS = 300_000L;

    private PvpRules() { }

    public static boolean shieldActive(long now, long activeUntil) {
        return now < activeUntil;
    }

    public static boolean mayActivate(long now, long lastActivation) {
        return lastActivation == Long.MIN_VALUE || now - lastActivation >= COOLDOWN_MILLIS;
    }

    public static long activeUntil(long activatedAt) {
        return activatedAt + SHIELD_MILLIS;
    }
}
