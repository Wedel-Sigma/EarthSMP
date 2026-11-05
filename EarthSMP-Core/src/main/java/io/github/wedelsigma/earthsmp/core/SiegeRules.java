package io.github.wedelsigma.earthsmp.core;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;

/** Pure rules for siege scheduling; independent of server ticks and the host timezone. */
public final class SiegeRules {
    public static final ZoneId WARSAW = ZoneId.of("Europe/Warsaw");
    public static final Duration GRACE_PERIOD = Duration.ofMinutes(30);
    private static final LocalTime OPENS = LocalTime.of(10, 0);
    private SiegeRules() {}

    /** The legal window is [10:00, 24:00) in Poland, including DST transitions. */
    public static boolean openAt(Instant instant) {
    if (Boolean.getBoolean("earthsmp.testing.ignoreSiegeHours")) {
        return true;
    }

    return !instant.atZone(WARSAW).toLocalTime().isBefore(OPENS);
}

    /** Offline and inactive players are counted in the roster denominator. */
    public static int requiredDefenders(int rosterSize) {
        if (rosterSize < 0) throw new IllegalArgumentException("rosterSize must be >= 0");
        return Math.max(1, (rosterSize + 4) / 5);
    }

    public static boolean mayBegin(Instant time, int rosterSize, int onlineDefenders) {
        return openAt(time) && onlineDefenders >= requiredDefenders(rosterSize);
    }

    /** Once missing defenders are detected, the deadline is absolute wall-clock time. */
    public static boolean graceExpired(Instant missingSince, Instant now) {
        return missingSince != null && !now.isBefore(missingSince.plus(GRACE_PERIOD));
    }
}
