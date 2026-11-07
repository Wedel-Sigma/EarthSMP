package io.github.wedelsigma.earthsmp.core;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CombatTagRulesTest {
    @Test void lastsExactlyOneMinute() {
        long until = CombatTagRules.expiresAt(1_000L);
        assertEquals(61_000L, until);
        assertTrue(CombatTagRules.isTagged(60_999L, until));
        assertFalse(CombatTagRules.isTagged(61_000L, until));
    }
    @Test void subsequentHitRenewsFullMinute() {
        long first = CombatTagRules.expiresAt(1_000L);
        long later = CombatTagRules.expiresAt(55_000L);
        assertEquals(61_000L, first);
        assertEquals(115_000L, later);
        assertEquals(60, CombatTagRules.remainingSeconds(55_000L, later));
    }
    @Test void countdownRoundsUpAndStopsAtZero() {
        assertEquals(60, CombatTagRules.remainingSeconds(1000, 61000));
        assertEquals(1, CombatTagRules.remainingSeconds(60999, 61000));
        assertEquals(0, CombatTagRules.remainingSeconds(61000, 61000));
        assertEquals(0, CombatTagRules.remainingSeconds(70000, 61000));
    }
}
