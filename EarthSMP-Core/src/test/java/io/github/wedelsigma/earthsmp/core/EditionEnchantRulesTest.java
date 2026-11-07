package io.github.wedelsigma.earthsmp.core;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class EditionEnchantRulesTest {
    @Test void fortuneTimeline() {
        assertFalse(EditionEnchantRules.permitted("fortune", 1, 6));
        assertTrue(EditionEnchantRules.permitted("fortune", 1, 7));
        assertFalse(EditionEnchantRules.permitted("fortune", 2, 16));
        assertTrue(EditionEnchantRules.permitted("fortune", 3, 16));
        assertEquals(1, EditionEnchantRules.normalizedLevel("fortune", 2, 20));
        assertEquals(0, EditionEnchantRules.normalizedLevel("fortune", 3, 0));
    }
    @Test void crossbowQuickChargeTimeline() {
        assertEquals(2, EditionEnchantRules.maximum("quick_charge", 0));
        assertEquals(2, EditionEnchantRules.maximum("quick_charge", 9));
        assertEquals(3, EditionEnchantRules.maximum("quick_charge", 10));
        assertTrue(EditionEnchantRules.permitted("quick_charge", 2, 0));
        assertTrue(EditionEnchantRules.permitted("quick_charge", 2, 9));
        assertFalse(EditionEnchantRules.permitted("quick_charge", 3, 9));
        assertTrue(EditionEnchantRules.permitted("quick_charge", 3, 10));
        assertEquals(2, EditionEnchantRules.normalizedLevel("quick_charge", 3, 9));
        assertEquals(3, EditionEnchantRules.normalizedLevel("quick_charge", 3, 10));
    }
    @Test void multishotBannedAtEveryDayAndRemovedFromItem() {
        for (int day : new int[]{0, 1, 6, 9, 10, 16, 999}) {
            assertEquals(0, EditionEnchantRules.maximum("multishot", day));
            assertFalse(EditionEnchantRules.permitted("multishot", 1, day));
            assertTrue(EditionEnchantRules.needsChange("multishot", 1, day));
            assertEquals(0, EditionEnchantRules.normalizedLevel("multishot", 1, day));
        }
    }
    @Test void crossbowPiercingAndUniversalUnbreakingStayUnchanged() {
        assertTrue(EditionEnchantRules.permitted("piercing", 1, 9));
        assertFalse(EditionEnchantRules.permitted("piercing", 2, 9));
        assertTrue(EditionEnchantRules.permitted("piercing", 2, 10));
        assertFalse(EditionEnchantRules.permitted("piercing", 3, 100));
        for (int day : new int[]{0, 1, 9, 10, 999}) {
            assertTrue(EditionEnchantRules.permitted("unbreaking", 3, day));
            assertEquals(3, EditionEnchantRules.maximum("unbreaking", day));
        }
    }
    @Test void combatAndQol() {
        assertTrue(EditionEnchantRules.permitted("sharpness", 1, 9));
        assertFalse(EditionEnchantRules.permitted("sharpness", 2, 9));
        assertTrue(EditionEnchantRules.permitted("sharpness", 2, 10));
        assertFalse(EditionEnchantRules.permitted("mending", 1, 999));
        assertTrue(EditionEnchantRules.permitted("efficiency", 5, 1));
        assertTrue(EditionEnchantRules.permitted("unbreaking", 3, 1));
        assertTrue(EditionEnchantRules.permitted("respiration", 3, 1));
        assertFalse(EditionEnchantRules.permitted("depth_strider", 3, 20));
    }
}
