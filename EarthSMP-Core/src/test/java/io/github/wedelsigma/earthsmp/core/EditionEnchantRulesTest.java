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
