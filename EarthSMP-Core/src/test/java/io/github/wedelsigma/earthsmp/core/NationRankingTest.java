package io.github.wedelsigma.earthsmp.core;

import java.nio.file.Path;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class NationRankingTest {
    @TempDir Path temp;

    @Test void countsOnlyPlacedCoresIncludingConqueredAndSupportsZeroCoreNations() throws Exception {
        try (NationDatabase db = new NationDatabase(temp.resolve("ranking.db"))) {
            var c = db.diplomacyConnection();
            try (var statement = c.createStatement()) {
                statement.executeUpdate("INSERT INTO nations(id,name) VALUES('a','Alfa'),('b','Beta'),('c','Cel')");
                statement.executeUpdate("""
                        INSERT INTO protection_cores(id,nation_id,world_name,block_x,block_y,block_z,origin,is_capital,placed_by)
                        VALUES ('coreA','a','Earth',0,70,0,'FREE',1,'test'),
                               ('coreB','a','Earth',100,70,0,'CRAFTED',0,'test'),
                               ('coreC','b','Earth',200,70,0,'FREE',1,'test')
                        """);
                statement.executeUpdate("""
                        INSERT INTO core_tokens(id,nation_id,origin,state)
                        VALUES ('token-unused','c','CRAFTED','ITEM')
                        """);
            }
            try (var stmt = c.prepareStatement("INSERT INTO nation_members(player_uuid,nation_id,role) VALUES(?,'a','LEADER')")) {
                stmt.setString(1, UUID.randomUUID().toString());
                stmt.executeUpdate();
            }
            var before = db.nationRanking();
            assertEquals(3, before.size());
            assertEquals("Alfa", before.get(0).name());
            assertEquals(2, before.get(0).placedCores());
            assertEquals(1, before.get(0).members());
            assertEquals(0, before.get(2).placedCores(), "core tokens in ITEM state do not count");

            try (var statement = c.createStatement()) {
                statement.executeUpdate("UPDATE protection_cores SET nation_id='b' WHERE id='coreB'");
            }
            var after = db.nationRanking();
            assertEquals("Beta", after.get(0).name());
            assertEquals(2, after.get(0).placedCores(), "captured core counts for new owner");
            assertEquals(1, after.get(1).placedCores());
        }
    }

    @Test void tieBreaksAlphabetically() throws Exception {
        try (NationDatabase db = new NationDatabase(temp.resolve("tie.db"))) {
            try (var statement = db.diplomacyConnection().createStatement()) {
                statement.executeUpdate("INSERT INTO nations(id,name) VALUES('z','Zeta'),('a','Alfa')");
            }
            assertEquals("Alfa", db.nationRanking().get(0).name());
        }
    }
}
