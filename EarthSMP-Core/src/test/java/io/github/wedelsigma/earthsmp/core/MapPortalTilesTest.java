package io.github.wedelsigma.earthsmp.core;

import static org.junit.jupiter.api.Assertions.*;
import java.io.ByteArrayInputStream;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;

final class MapPortalTilesTest {
    @Test void emptyMapTilesBecomeValidTransparentPng() throws Exception {
        for (int status : new int[]{200, 404}) {
            byte[] png = MapPortalServer.pngOrTransparent(status, new byte[0]);
            assertNotNull(png);
            var image = ImageIO.read(new ByteArrayInputStream(png));
            assertNotNull(image);
            assertEquals(1, image.getWidth());
            assertEquals(1, image.getHeight());
            assertEquals(0, image.getRGB(0, 0) >>> 24);
        }
    }

    @Test void realTilesArePreservedAndNetworkErrorsAreNotFalselyHidden() {
        byte[] bytes = new byte[]{(byte)137, 80, 78, 71};
        assertArrayEquals(bytes, MapPortalServer.pngOrTransparent(200, bytes));
        assertNull(MapPortalServer.pngOrTransparent(502, new byte[0]));
    }
}
