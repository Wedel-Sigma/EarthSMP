package io.github.wedelsigma.earthsmp.core;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class EditionItemRulesTest {
    @Test void limitsAndUnrestricted() {
        assertEquals(8, EditionItemRules.cap("GOLDEN_APPLE"));
        assertEquals(3, EditionItemRules.cap("ENDER_PEARL"));
        assertEquals(16, EditionItemRules.cap("COBWEB"));
        assertEquals(3, EditionItemRules.cap("TOTEM_OF_UNDYING"));
        assertEquals(-1, EditionItemRules.cap("DIAMOND"));
        assertEquals(0, EditionItemRules.cap("RESPAWN_ANCHOR"));
        assertEquals(0, EditionItemRules.cap("END_CRYSTAL"));
        assertEquals(0, EditionItemRules.cap("ENCHANTED_GOLDEN_APPLE"));
    }
    @Test void partialPickupAndNoDeleting() {
        assertEquals(2, EditionItemRules.transferable("GOLDEN_APPLE", 6, 32));
        assertEquals(0, EditionItemRules.transferable("GOLDEN_APPLE", 8, 20));
        assertEquals(2, EditionItemRules.transferable("ENDER_PEARL", 1, 16));
        assertEquals(0, EditionItemRules.transferable("END_CRYSTAL", 0, 64));
        assertEquals(64, EditionItemRules.transferable("DIAMOND", 999, 64));
    }
}
