package io.github.wedelsigma.earthsmp.core;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ServerClockTest {
    private static Instant at(String iso) { return Instant.parse(iso); }

    @Test void dayChangesAtSixWarsawTime() {
        var clock = new ServerClock();
        Instant start = at("2026-11-06T17:00:00Z"); // Fri 18:00 Warsaw
        assertEquals(1, clock.day(Optional.of(start), start));
        assertEquals(1, clock.day(Optional.of(start), at("2026-11-07T04:59:59Z")));
        assertEquals(2, clock.day(Optional.of(start), at("2026-11-07T05:00:00Z")));
        assertEquals(3, clock.day(Optional.of(start), at("2026-11-08T05:00:00Z")));
        assertEquals(0, clock.day(Optional.empty(), start));
        assertEquals(0, clock.day(Optional.of(start), start.minusSeconds(1)));
    }

    @Test void daylightSavingSpringAndAutumnKeepSixLocal() {
        var clock = new ServerClock();
        // Spring forward in Poland 29 March 2026: 06:00 CEST = 04:00 UTC.
        assertEquals(LocalDate.of(2026, 3, 28), clock.serverDate(at("2026-03-29T03:59:59Z")));
        assertEquals(LocalDate.of(2026, 3, 29), clock.serverDate(at("2026-03-29T04:00:00Z")));
        assertEquals(at("2026-03-29T04:00:00Z"), clock.nextReset(at("2026-03-29T03:59:59Z")).toInstant());
        // Autumn rollback 25 October 2026: 06:00 CET = 05:00 UTC.
        assertEquals(LocalDate.of(2026, 10, 24), clock.serverDate(at("2026-10-25T04:59:59Z")));
        assertEquals(LocalDate.of(2026, 10, 25), clock.serverDate(at("2026-10-25T05:00:00Z")));
        assertEquals(at("2026-10-25T05:00:00Z"), clock.nextReset(at("2026-10-25T04:59:59Z")).toInstant());
    }

    @Test void worksAfterServerWasOfflineForDays() {
        var clock = new ServerClock(Clock.fixed(at("2026-11-10T08:00:00Z"), ZoneOffset.UTC));
        assertEquals(5, clock.day(Optional.of(at("2026-11-06T17:00:00Z"))));
    }
}
