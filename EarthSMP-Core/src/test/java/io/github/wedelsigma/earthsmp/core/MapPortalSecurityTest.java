package io.github.wedelsigma.earthsmp.core;

import static org.junit.jupiter.api.Assertions.*;
import java.net.URI;
import org.junit.jupiter.api.Test;

final class MapPortalSecurityTest {
    @Test void httpsOriginCanUseDefaultPortWithoutExplicit443() {
        URI origin = MapPortalSecurity.validateOrigin("https://map.example.org");
        assertEquals("https://map.example.org",origin.toString());
        assertTrue(MapPortalSecurity.hostAllowed(origin,"map.example.org"));
        assertTrue(MapPortalSecurity.hostAllowed(origin,"map.example.org:443"));
        assertTrue(MapPortalSecurity.originAllowed(origin,"https://map.example.org"));
        assertTrue(MapPortalSecurity.originAllowed(origin,"https://map.example.org:443"));
        assertFalse(MapPortalSecurity.hostAllowed(origin,"map.example.org:8766"));
        assertFalse(MapPortalSecurity.originAllowed(origin,"http://map.example.org"));
        assertFalse(MapPortalSecurity.originAllowed(origin,"https://evil.example.org"));
        assertFalse(MapPortalSecurity.originAllowed(origin,"https://map.example.org.evil.test"));
        assertFalse(MapPortalSecurity.hostAllowed(origin,"map.example.org@evil.test"));
        assertThrows(IllegalArgumentException.class,()->MapPortalSecurity.validateOrigin("https://map.example.org/secret"));
    }

    @Test void localOriginRemainsStrict() {
        URI origin = MapPortalSecurity.validateOrigin("http://127.0.0.1:8766");
        assertTrue(MapPortalSecurity.hostAllowed(origin,"127.0.0.1:8766"));
        assertFalse(MapPortalSecurity.hostAllowed(origin,"127.0.0.1"));
        assertTrue(MapPortalSecurity.originAllowed(origin,"http://127.0.0.1:8766"));
        assertFalse(MapPortalSecurity.originAllowed(origin,"http://127.0.0.1:8080"));
    }

    @Test void proxyIpHeadersAreNotTrustedWithoutAnExplicitPeerMatch() {
        assertEquals("127.0.0.1",MapPortalSecurity.clientAddress("127.0.0.1","","1.2.3.4"));
        assertEquals("192.0.2.5",MapPortalSecurity.clientAddress("192.0.2.5","127.0.0.1","1.2.3.4"));
        assertEquals("203.0.113.8",MapPortalSecurity.clientAddress("127.0.0.1","127.0.0.1","203.0.113.8"));
        assertEquals("127.0.0.1",MapPortalSecurity.clientAddress("127.0.0.1","127.0.0.1","9.9.9.9, 1.2.3.4"));
        assertEquals("127.0.0.1",MapPortalSecurity.clientAddress("127.0.0.1","127.0.0.1","999.2.3.4"));
        assertEquals("2001:db8::1",MapPortalSecurity.clientAddress("127.0.0.1","127.0.0.1","2001:db8::1"));
        assertEquals("127.0.0.1",MapPortalSecurity.clientAddress("127.0.0.1","127.0.0.1","client.example.org"));
    }

    @Test void borderJsonIsPublicButContainsNoPlayerDetails() {
        var border = new MapWorldBorder(7680,-256,5120);
        assertEquals("{\"centerX\":7680.0,\"centerZ\":-256.0,\"size\":5120.0}",border.json());
        assertThrows(IllegalArgumentException.class,()->new MapWorldBorder(0,0,Double.NaN));
    }
}
