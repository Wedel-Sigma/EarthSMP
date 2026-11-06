package io.github.wedelsigma.earthsmp.core;

/** Formats a remaining cooldown without rounding a partial minute up to a full hour. */
public final class CapitalCooldownText {
    private CapitalCooldownText() {}

    public static String remaining(long seconds) {
        long remaining = Math.max(0L, seconds);
        long hours = remaining / 3600L;
        long minutes = (remaining % 3600L) / 60L;
        long secs = remaining % 60L;
        if (hours > 0) return hours + " godz. " + minutes + " min " + secs + " s";
        if (minutes > 0) return minutes + " min " + secs + " s";
        return secs + " s";
    }
}
