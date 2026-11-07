package io.github.wedelsigma.earthsmp.core;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class EditionDimensionRulesTest {
    @Test void lockedBeforeDayAndAfter() {
        assertTrue(EditionDimensionRules.allowed("NORMAL", 0, 10));
        assertFalse(EditionDimensionRules.allowed("NETHER", 0, 10));
        assertFalse(EditionDimensionRules.allowed("NETHER", 9, 10));
        assertTrue(EditionDimensionRules.allowed("NETHER", 10, 10));
        assertFalse(EditionDimensionRules.allowed("NETHER", 10, 11));
        assertTrue(EditionDimensionRules.allowed("NETHER", 11, 11));
        assertFalse(EditionDimensionRules.allowed("THE_END", 999, 10));
    }
}
