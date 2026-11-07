package io.github.wedelsigma.earthsmp.core;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class SecurityAlertPolicyTest {
    @Test void repeatedEventsRequireThresholdAndRespectCooldown() {
        var policy = new SecurityAlertPolicy();
        assertFalse(policy.observe("p1", "ITEM_LIMIT", 1_000L, 4, 60_000L, 60_000L, false).notifyAdmins());
        assertFalse(policy.observe("p1", "ITEM_LIMIT", 2_000L, 4, 60_000L, 60_000L, false).notifyAdmins());
        assertFalse(policy.observe("p1", "ITEM_LIMIT", 3_000L, 4, 60_000L, 60_000L, false).notifyAdmins());
        assertTrue(policy.observe("p1", "ITEM_LIMIT", 4_000L, 4, 60_000L, 60_000L, false).notifyAdmins());
        assertFalse(policy.observe("p1", "ITEM_LIMIT", 5_000L, 4, 60_000L, 60_000L, false).notifyAdmins());
        assertTrue(policy.observe("p1", "ITEM_LIMIT", 64_000L, 4, 60_000L, 60_000L, true).notifyAdmins());
    }
    @Test void differentCategoriesAndPlayersAreIndependent() {
        var policy = new SecurityAlertPolicy();
        assertTrue(policy.observe("p1", "CORE_ANCHOR", 1000L, 4, 60000L, 60000L, true).notifyAdmins());
        assertTrue(policy.observe("p2", "CORE_ANCHOR", 1000L, 4, 60000L, 60000L, true).notifyAdmins());
        assertTrue(policy.observe("p1", "INVENTORY_OVERFLOW", 1000L, 4, 60000L, 60000L, true).notifyAdmins());
        assertFalse(policy.observe("p1", "CORE_ANCHOR", 1001L, 4, 60000L, 60000L, true).notifyAdmins());
    }
    @Test void countResetsAfterWindow() {
        var policy = new SecurityAlertPolicy();
        assertEquals(1, policy.observe("p", "EVENT", 0L, 4, 60000L, 60000L, false).count());
        assertEquals(1, policy.observe("p", "EVENT", 60000L, 4, 60000L, 60000L, false).count());
    }
}
