package io.github.wedelsigma.earthsmp.core;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Objects;
import java.util.Optional;

/**
 * One shared calendar for the entire EarthSMP edition.
 * A server day begins at 06:00 Europe/Warsaw, including DST transitions.
 * This class never increments counters on a timer: elapsed dates determine the day.
 */
public final class ServerClock {
    public static final ZoneId ZONE = ZoneId.of("Europe/Warsaw");
    public static final LocalTime RESET_TIME = LocalTime.of(6, 0);
    private final Clock clock;

    public ServerClock() { this(Clock.systemUTC()); }

    ServerClock(Clock clock) { this.clock = Objects.requireNonNull(clock); }

    public Instant now() { return clock.instant(); }

    /** Same date key from 06:00 through 05:59:59 the following calendar morning. */
    public LocalDate serverDate(Instant when) {
        ZonedDateTime local = when.atZone(ZONE);
        LocalDate date = local.toLocalDate();
        return local.toLocalTime().isBefore(RESET_TIME) ? date.minusDays(1) : date;
    }

    /** Day 0 denotes an edition that has not started yet (or a future start). */
    public int day(Optional<Instant> editionStart) {
        return day(editionStart, now());
    }

    public int day(Optional<Instant> editionStart, Instant when) {
        if (editionStart.isEmpty() || when.isBefore(editionStart.get())) return 0;
        return Math.toIntExact(ChronoUnit.DAYS.between(serverDate(editionStart.get()), serverDate(when)) + 1);
    }

    public ZonedDateTime nextReset(Instant when) {
        return serverDate(when).plusDays(1).atTime(RESET_TIME).atZone(ZONE);
    }

    public Duration untilReset(Instant when) {
        return Duration.between(when, nextReset(when).toInstant());
    }
}
