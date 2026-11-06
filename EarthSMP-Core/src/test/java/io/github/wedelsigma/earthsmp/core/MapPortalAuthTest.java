package io.github.wedelsigma.earthsmp.core;

import java.nio.file.Path;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class MapPortalAuthTest {
    @TempDir Path temp;

    @Test void oneTimePairingAndPasswordSignInSurviveServerRestart() throws Exception {
        Path file = temp.resolve("persist.db");
        UUID player = UUID.randomUUID();
        var clock = new AdjustableClock(Instant.parse("2026-11-06T16:00:00Z"));
        try (NationDatabase db = new NationDatabase(file)) {
            assertTrue(db.editionStart().isEmpty());
        }
        String password = "Safe_Example_123456";
        String persistentSession;
        try (MapPortalAuth auth = new MapPortalAuth(file, clock)) {
            String code = auth.issue(player, "Tester").orElseThrow();
            assertEquals(10, code.length());
            assertTrue(auth.authenticate("bogus").isEmpty());
            assertTrue(auth.redeem(code, "short", "client-A").isEmpty());
            var linked = auth.redeem(code, password, "client-B").orElseThrow();
            assertEquals(player, auth.authenticate(linked.token()).orElseThrow());
            assertTrue(auth.redeem(code, password, "client-C").isEmpty(), "pairing code cannot be reused");
            assertTrue(auth.issue(player, "Tester").isEmpty(), "re-linking must be unnecessary");
            assertTrue(auth.login("Tester", "wrong-password", "client-D").isEmpty());
            var secondDevice = auth.login("tester", password, "client-D").orElseThrow();
            persistentSession = secondDevice.token();
            assertEquals(player, auth.authenticate(secondDevice.token()).orElseThrow());
        }
        try (MapPortalAuth afterRestart = new MapPortalAuth(file, clock)) {
            assertEquals(player, afterRestart.authenticate(persistentSession).orElseThrow(), "sessions survive Paper restart");
            assertEquals(player, afterRestart.login("Tester", password, "client-F").orElseThrow().player());
            clock.advance(Duration.ofDays(30));
            assertTrue(afterRestart.authenticate(persistentSession).isEmpty(), "browser session expires in 30 days");
            assertTrue(afterRestart.hasAccount(player), "account does not expire");
            assertEquals(player, afterRestart.login("Tester", password, "client-G").orElseThrow().player(),
                    "after session expiry user signs in by password, without Minecraft");
            afterRestart.revoke(player);
            assertTrue(afterRestart.authenticate(persistentSession).isEmpty());
            afterRestart.reset(player);
            assertFalse(afterRestart.hasAccount(player));
            assertTrue(afterRestart.issue(player, "Tester").isPresent(), "recovery requires a new MC pairing");
        }
    }

    @Test void codesExpireAndPasswordsAreRateLimited() throws Exception {
        Path file = temp.resolve("rate.db");
        try (NationDatabase db = new NationDatabase(file)) { }
        var clock = new AdjustableClock(Instant.parse("2026-11-06T16:00:00Z"));
        try (MapPortalAuth auth = new MapPortalAuth(file, clock)) {
            UUID player = UUID.randomUUID();
            String code = auth.issue(player, "TestUser").orElseThrow();
            for (int i = 0; i < 6; i++) assertTrue(auth.redeem("INVALID", "Secret_Password_123", "address-A").isEmpty());
            assertTrue(auth.redeem(code, "Secret_Password_123", "address-A").isEmpty(), "address is blocked");
            assertTrue(auth.redeem(code, "Secret_Password_123", "address-B").isPresent());
            String expired = auth.issue(UUID.randomUUID(), "AnotherUser").orElseThrow();
            clock.advance(Duration.ofMinutes(5));
            assertTrue(auth.redeem(expired, "Secret_Password_123", "address-C").isEmpty());
        }
    }

    @Test void nationFilteringWorksWhenViewerIsOfflineAndNetherRemainsHidden() {
        UUID viewer = UUID.randomUUID(), ally = UUID.randomUUID(), enemy = UUID.randomUUID(), neutral = UUID.randomUUID();
        var snap = new MapPortalSnapshot(Map.of(
            viewer, new MapPortalSnapshot.Viewer("Viewer", "alfa", "Alfa"), // offline but linked
            ally, new MapPortalSnapshot.Viewer("Ally", "alfa", "Alfa"),
            enemy, new MapPortalSnapshot.Viewer("Enemy", "beta", "Beta")
        ), List.of(
            new MapPortalSnapshot.Position(ally, "Ally", "alfa", 33, 70, 44),
            new MapPortalSnapshot.Position(enemy, "Enemy", "beta", 55, 70, 66),
            new MapPortalSnapshot.Position(neutral, "Neutral", null, 77, 70, 88)
        ));
        var visible = snap.visibleTo(viewer).orElseThrow();
        assertEquals(1, visible.size());
        String body = MapPortalJson.positions(viewer, snap, visible);
        assertTrue(body.contains("\"name\":\"Ally\""));
        assertFalse(body.contains("Enemy"));
        assertFalse(body.contains("Neutral"));
        assertTrue(snap.visibleTo(neutral).isEmpty(), "unlinked account cannot query positions");
        var movedNation = new MapPortalSnapshot(Map.of(viewer, new MapPortalSnapshot.Viewer("Viewer", "beta", "Beta")), snap.overworld());
        assertEquals("Enemy", movedNation.visibleTo(viewer).orElseThrow().get(0).name(),
                "current nation membership controls access, not original pairing nation");
        var nether = new MapPortalSnapshot(Map.of(viewer, new MapPortalSnapshot.Viewer("Viewer", "alfa", "Alfa")), List.of());
        assertTrue(nether.visibleTo(viewer).orElseThrow().isEmpty());
    }

    @Test void jsonNamesAreProperlyEscaped() {
        assertEquals("\"\\\"\\\\\\n\"", MapPortalJson.quote("\"\\\n"));
        assertEquals("\"<\"", MapPortalJson.quote("<"));
    }

    private static final class AdjustableClock extends Clock {
        private Instant instant;
        AdjustableClock(Instant instant) { this.instant = instant; }
        void advance(Duration amount) { instant = instant.plus(amount); }
        @Override public ZoneOffset getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(java.time.ZoneId zone) { return this; }
        @Override public Instant instant() { return instant; }
    }
}
