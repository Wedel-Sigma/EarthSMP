package io.github.wedelsigma.earthsmp.core;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class EditionPersistenceTest {
    @TempDir Path temp;

    @Test void launchAndHomeQuotaPersistAcrossRestarts() throws Exception {
        Path file = temp.resolve("edition.db");
        UUID player = UUID.randomUUID();
        Instant launch = Instant.parse("2026-11-06T17:00:00Z");
        LocalDate day3 = LocalDate.of(2026, 11, 8);
        try (NationDatabase db = new NationDatabase(file)) {
            assertTrue(db.editionStart().isEmpty());
            assertTrue(db.startEdition(launch));
            assertFalse(db.startEdition(launch.plusSeconds(2000)));
            assertEquals(launch, db.editionStart().orElseThrow());
            for (int use = 1; use <= 4; use++) assertEquals(use, db.recordHomeUse(player, day3, 4));
            assertThrows(java.sql.SQLException.class, () -> db.recordHomeUse(player, day3, 4));
            assertEquals(4, db.homeUses(player, day3));
            assertEquals(0, db.homeUses(player, day3.plusDays(1)));
        }
        try (NationDatabase db = new NationDatabase(file)) {
            assertEquals(launch, db.editionStart().orElseThrow());
            assertEquals(4, db.homeUses(player, day3));
            assertEquals(1, db.recordHomeUse(player, day3.plusDays(1), 4));
            assertEquals(1, db.homeUses(player, day3.plusDays(1)));
        }
    }

    @Test void schemaEightUpgradesToNineWithoutClearingEditionData() throws Exception {
        Path file = temp.resolve("upgrade.db");
        Instant launch = Instant.parse("2026-11-06T17:00:00Z");
        try (NationDatabase db = new NationDatabase(file)) {
            assertTrue(db.startEdition(launch));
            try (var st = db.diplomacyConnection().createStatement()) {
                st.execute("DROP TABLE map_sessions");
                st.execute("DROP TABLE map_accounts");
                st.execute("PRAGMA user_version = 8");
            }
        }
        try (NationDatabase db = new NationDatabase(file)) {
            assertEquals(launch, db.editionStart().orElseThrow());
            try (var st = db.diplomacyConnection().createStatement();
                 var v = st.executeQuery("PRAGMA user_version")) {
                assertTrue(v.next());
                assertEquals(9, v.getInt(1));
            }
            try (MapPortalAuth auth = new MapPortalAuth(file)) {
                assertTrue(auth.linkedAccounts().isEmpty());
            }
        }
    }

    @Test void capitalCooldownIsPerNationAndAutomaticallyExpires() throws Exception {
        try (NationDatabase db = new NationDatabase(temp.resolve("capital.db"))) {
            UUID leader = UUID.randomUUID();
            Connection c = db.diplomacyConnection();
            try (PreparedStatement p = c.prepareStatement("INSERT INTO nations(id,name) VALUES('a','Alfa')")) {
                p.executeUpdate();
            }
            try (PreparedStatement p = c.prepareStatement(
                    "INSERT INTO nation_members(player_uuid,nation_id,role) VALUES(?,'a','LEADER')")) {
                p.setString(1, leader.toString()); p.executeUpdate();
            }
            try (PreparedStatement p = c.prepareStatement("""
                    INSERT INTO protection_cores(id,nation_id,world_name,block_x,block_y,block_z,origin,is_capital,placed_by)
                    VALUES('a1','a','Earth',100,70,100,'FREE',1,'test'),
                          ('a2','a','Earth',300,70,100,'CRAFTED',0,'test')
                    """)) { p.executeUpdate(); }
            assertEquals(0L, db.capitalCooldownRemaining("a", Instant.now()),
                    "Creating a nation must not start a six-hour cooldown");
            assertEquals(NationDatabase.OwnCapitalResult.UPDATED, db.setOwnCapital(leader, "a2"));
            assertEquals(NationDatabase.OwnCapitalResult.COOLDOWN, db.setOwnCapital(leader, "a1"));
            assertTrue(db.capitalCooldownRemaining("a", Instant.now()) > 0);
            long lastChange;
            try (PreparedStatement p = c.prepareStatement(
                    "SELECT changed_at FROM nation_capital_cooldowns WHERE nation_id='a'");
                 ResultSet r = p.executeQuery()) {
                assertTrue(r.next());
                lastChange = r.getLong(1);
            }
            assertEquals(21600L, db.capitalCooldownRemaining("a", Instant.ofEpochSecond(lastChange)));
            assertEquals(14400L, db.capitalCooldownRemaining("a", Instant.ofEpochSecond(lastChange + 7200L)));
            assertEquals(0L, db.capitalCooldownRemaining("a", Instant.ofEpochSecond(lastChange + 21600L)));
            try (PreparedStatement p = c.prepareStatement(
                    "UPDATE nation_capital_cooldowns SET changed_at=? WHERE nation_id='a'")) {
                p.setLong(1, Instant.now().minusSeconds(7 * 3600).getEpochSecond()); p.executeUpdate();
            }
            assertEquals(NationDatabase.OwnCapitalResult.UPDATED, db.setOwnCapital(leader, "a1"));
            assertEquals(1, db.capitalOf("a").orElseThrow().capital() ? 1 : 0);
        }
    }
}
