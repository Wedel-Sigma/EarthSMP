package io.github.wedelsigma.earthsmp.core;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class HomeWarmupRulesTest {
    @Test void countdownUsesRealElapsedTimeAndCeiling() {
        long startedAt = 1_000_000_000L;
        long deadline = startedAt + HomeWarmupRules.WARMUP_NANOS;
        assertEquals(30, HomeWarmupRules.remainingSeconds(deadline, startedAt));
        assertEquals(30, HomeWarmupRules.remainingSeconds(deadline, startedAt + 1));
        assertEquals(29, HomeWarmupRules.remainingSeconds(deadline, startedAt + 1_000_000_000L));
        assertEquals(1, HomeWarmupRules.remainingSeconds(deadline, deadline - 1));
        assertEquals(0, HomeWarmupRules.remainingSeconds(deadline, deadline));
        assertEquals(0, HomeWarmupRules.remainingSeconds(deadline, deadline + 1000));
    }

    @Test void positionChangeCancelsButRotationDoesNotMatter() {
        assertFalse(HomeWarmupRules.moved(5, 70, -3, 5, 70, -3));
        assertTrue(HomeWarmupRules.moved(5, 70, -3, 5.00001, 70, -3));
        assertTrue(HomeWarmupRules.moved(5, 70, -3, 5, 69.9, -3));
        assertTrue(HomeWarmupRules.moved(5, 70, -3, 5, 70, -3.01));
    }
}
