package io.github.wedelsigma.earthsmp.core;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PvpRulesTest {
    @Test void firstHitBlocksExactlyEightSeconds() {
        long now = 1_000_000;
        assertTrue(PvpRules.shieldActive(now + 7999, PvpRules.activeUntil(now)));
        assertFalse(PvpRules.shieldActive(now + 8000, PvpRules.activeUntil(now)));
    }

    @Test void cooldownStartsWithActivationNotShieldEnd() {
        long now = 1_000_000;
        assertTrue(PvpRules.mayActivate(now, Long.MIN_VALUE));
        assertFalse(PvpRules.mayActivate(now + 8_000, now));
        assertFalse(PvpRules.mayActivate(now + 299_999, now));
        assertTrue(PvpRules.mayActivate(now + 300_000, now));
    }
}
