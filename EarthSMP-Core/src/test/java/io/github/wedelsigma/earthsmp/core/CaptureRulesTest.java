package io.github.wedelsigma.earthsmp.core;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CaptureRulesTest {
    @Test void eightMinutesToCapture() {
        int progress = 0;
        for (int i = 0; i < 479; i++) progress = CaptureRules.advance(progress, 1, true, false);
        assertEquals(479, progress);
        assertEquals(480, CaptureRules.advance(progress, 1, true, false));
    }
    @Test void contestPausesAndDefendersRecoverTwiceAsFast() {
        assertEquals(240, CaptureRules.advance(240, 33, true, true));
        assertEquals(240, CaptureRules.advance(240, 33, false, false));
        assertEquals(174, CaptureRules.advance(240, 33, false, true));
        assertEquals(0, CaptureRules.advance(10, 6, false, true));
    }
    @Test void clampsAndRejectsBadState() {
        assertEquals(480, CaptureRules.advance(479, 9999, true, false));
        assertThrows(IllegalArgumentException.class, () -> CaptureRules.advance(-1, 1, true, false));
        assertThrows(IllegalArgumentException.class, () -> CaptureRules.advance(0, -1, true, false));
    }
    @Test void exact3dZoneBoundaries() {
        assertTrue(CoreCaptureZone.inside(4, 110, -4, 0, 100, 0));
        assertTrue(CoreCaptureZone.inside(-4, 95, 4, 0, 100, 0));
        assertFalse(CoreCaptureZone.inside(5, 100, 0, 0, 100, 0));
        assertFalse(CoreCaptureZone.inside(0, 111, 0, 0, 100, 0));
    }
}
