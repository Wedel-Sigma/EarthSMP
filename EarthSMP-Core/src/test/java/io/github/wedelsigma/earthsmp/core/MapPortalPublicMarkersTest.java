package io.github.wedelsigma.earthsmp.core;

import static org.junit.jupiter.api.Assertions.*;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

final class MapPortalPublicMarkersTest {
    @Test void removesThirdPartyLayersAndTheirPrivateMarkers() {
        String input = "[{\"id\":\"earthsmp-political-territories\",\"markers\":{}},"
                + "{\"id\":\"third-party-players\",\"markers\":{\"player\":{\"x\":123}}},"
                + "{\"id\":\"earthsmp-protection-cores\",\"markers\":{\"core\":{\"x\":50}}}]";
        String filtered = new String(MapPortalPublicMarkers.onlyFirstPartyPublicLayers(input.getBytes(StandardCharsets.UTF_8)),
                StandardCharsets.UTF_8);
        assertTrue(filtered.contains("earthsmp-political-territories"));
        assertTrue(filtered.contains("earthsmp-protection-cores"));
        assertFalse(filtered.contains("third-party-players"));
        assertFalse(filtered.contains("123"));
        assertTrue(filtered.contains("50"));
    }

    @Test void invalidMarkerFormatFailsClosed() {
        assertThrows(IllegalArgumentException.class,
                () -> MapPortalPublicMarkers.onlyFirstPartyPublicLayers("{}".getBytes(StandardCharsets.UTF_8)));
    }
}
