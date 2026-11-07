package io.github.wedelsigma.earthsmp.core;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class RespawnBedRulesTest {
    @Test void bothBedHalvesAndBothSupportsAreProtected() {
        // Head x=10, foot x=9, both y=65
        for (int x : new int[] {9, 10}) {
            assertTrue(RespawnBedRules.protects(x,65,10, 10,65,10, 9,65,10));
            assertTrue(RespawnBedRules.protects(x,64,10, 10,65,10, 9,65,10));
        }
        assertFalse(RespawnBedRules.protects(8,65,10, 10,65,10, 9,65,10));
        assertFalse(RespawnBedRules.protects(10,63,10, 10,65,10, 9,65,10));
        assertFalse(RespawnBedRules.protects(10,64,11, 10,65,10, 9,65,10));
    }
}
