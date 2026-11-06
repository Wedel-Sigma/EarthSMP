package io.github.wedelsigma.earthsmp.core;

import java.awt.image.BufferedImage;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CoreMarkerImagesTest {
    @Test void coordinatesAreAlwaysEmbeddedInAVisibleImage() {
        assertEquals("X: -120  Y: 64  Z: 312", CoreMarkerImages.label(-120, 64, 312));
        BufferedImage img = CoreMarkerImages.create(-120, 64, 312);
        assertTrue(img.getWidth() >= 60);
        assertTrue(img.getHeight() >= 50);
        assertNotEquals(0, img.getRGB(img.getWidth() / 2, 12) >>> 24,
                "The coordinate label background must be visible");
        assertNotEquals(0, img.getRGB(img.getWidth() / 2, 39) >>> 24,
                "The lodestone-style icon must be visible");
    }

    @Test void negativeAndLongCoordinatesCannotOverflowImage() {
        BufferedImage a = CoreMarkerImages.create(-30_000_000, -64, 30_000_000);
        BufferedImage b = CoreMarkerImages.create(1, 70, 2);
        assertTrue(a.getWidth() > b.getWidth());
        assertEquals(a.getHeight(), b.getHeight());
    }
}
