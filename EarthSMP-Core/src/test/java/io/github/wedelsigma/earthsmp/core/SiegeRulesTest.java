package io.github.wedelsigma.earthsmp.core;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SiegeRulesTest {
    private static Instant polish(int year,int month,int day,int hour,int minute) {
        return ZonedDateTime.of(LocalDate.of(year,month,day),LocalTime.of(hour,minute),SiegeRules.WARSAW).toInstant();
    }
    @Test void timeWindowIsWarsawBasedAndEndsAtMidnight() {
        assertFalse(SiegeRules.openAt(polish(2026,11,5,9,59)));
        assertTrue(SiegeRules.openAt(polish(2026,11,5,10,0)));
        assertTrue(SiegeRules.openAt(polish(2026,11,5,23,59)));
        assertFalse(SiegeRules.openAt(polish(2026,11,6,0,0)));
        assertTrue(SiegeRules.openAt(polish(2026,7,5,10,0))); // summer UTC+2
        assertFalse(SiegeRules.openAt(polish(2026,7,5,9,59)));
    }
    @Test void rosterCountsOfflineAndUsesCeilTwentyPercent() {
        assertEquals(1,SiegeRules.requiredDefenders(0));
        assertEquals(1,SiegeRules.requiredDefenders(1));
        assertEquals(1,SiegeRules.requiredDefenders(5));
        assertEquals(2,SiegeRules.requiredDefenders(6));
        assertEquals(2,SiegeRules.requiredDefenders(10));
        assertEquals(3,SiegeRules.requiredDefenders(11));
        assertEquals(6,SiegeRules.requiredDefenders(30));
        assertFalse(SiegeRules.mayBegin(polish(2026,11,5,11,0),11,2));
        assertTrue(SiegeRules.mayBegin(polish(2026,11,5,11,0),11,3));
        assertFalse(SiegeRules.mayBegin(polish(2026,11,5,9,59),11,3));
    }
    @Test void graceIsExactlyThirtyMinutesAndResettable() {
        Instant down=polish(2026,11,5,15,0);
        assertFalse(SiegeRules.graceExpired(down,down.plusSeconds(1799)));
        assertTrue(SiegeRules.graceExpired(down,down.plusSeconds(1800)));
        assertFalse(SiegeRules.graceExpired(null,down.plusSeconds(10000)));
    }
}
