package io.github.wedelsigma.earthsmp.core;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class CoreConquestTest {
    @TempDir Path temp;

    private static void statement(Connection c, String sql, Object... args) throws Exception {
        try (PreparedStatement q = c.prepareStatement(sql)) {
            for (int i = 0; i < args.length; i++) q.setObject(i+1, args[i] instanceof UUID id ? id.toString() : args[i]);
            q.executeUpdate();
        }
    }

    private static int count(Connection c, String sql) throws Exception {
        try (PreparedStatement q = c.prepareStatement(sql); ResultSet r = q.executeQuery()) {
            r.next(); return r.getInt(1);
        }
    }

    @Test void conquerOneThenDestroyLastWithoutDamagingVictorsNation() throws Exception {
        Path file = temp.resolve("conquest.db");
        try (NationDatabase db = new NationDatabase(file)) {
            Connection c = db.diplomacyConnection();
            UUID one = UUID.randomUUID(), two = UUID.randomUUID();
            statement(c, "INSERT INTO nations(id,name) VALUES ('a','Alfa'),('b','Bravo')");
            statement(c, "INSERT INTO nation_members(player_uuid,nation_id,role) VALUES (?, 'a','LEADER')", one);
            statement(c, "INSERT INTO nation_members(player_uuid,nation_id,role) VALUES (?, 'b','LEADER')", two);
            statement(c, "INSERT INTO territory_chunks(world_name,chunk_x,chunk_z,nation_id) VALUES ('Earth',4,4,'b')");
            statement(c, "INSERT INTO protection_cores(id,nation_id,world_name,block_x,block_y,block_z,origin,is_capital,placed_by) VALUES ('core1','b','Earth',100,70,100,'FREE',1,'test'),('core2','b','Earth',300,70,100,'CRAFTED',0,'test')");
            statement(c, "INSERT INTO core_tokens(id,nation_id,origin,state) VALUES ('core1','b','FREE','PLACED'),('core2','b','CRAFTED','PLACED')");
            var wars = new WarRepository(db);
            assertEquals(WarRepository.Status.SUCCESS,wars.declare(one,"b","").status());
            assertEquals(NationDatabase.CaptureOutcome.CONQUERED, db.captureCore("core1","b","a"));
            assertEquals(2, db.countNations());
            assertEquals(0, count(c,"SELECT COUNT(*) FROM core_tokens WHERE id='core1'"));
            assertEquals(1, count(c,"SELECT COUNT(*) FROM protection_cores WHERE id='core1' AND nation_id='a' AND origin='CONQUERED'"));
            assertEquals(1, count(c,"SELECT COUNT(*) FROM protection_cores WHERE nation_id='b'"));
            assertEquals(1, count(c,"SELECT COUNT(*) FROM protection_cores WHERE id='core2' AND nation_id='b' AND is_capital=1"),
                    "The defender's remaining core must automatically become the new capital");
            assertEquals(NationDatabase.CaptureOutcome.LAST_CORE_DESTROYED,db.captureCore("core2","b","a"));
            assertEquals(1,db.countNations());
            assertEquals(1, count(c,"SELECT COUNT(*) FROM protection_cores WHERE nation_id='a' AND id='core1'"));
            assertEquals(0, count(c,"SELECT COUNT(*) FROM territory_chunks WHERE nation_id='b'"));
            assertEquals(0, count(c,"SELECT COUNT(*) FROM wars WHERE ended_at IS NULL"));
            assertEquals(0, count(c,"SELECT COUNT(*) FROM core_tokens WHERE nation_id='b'"));
        }
    }

    @Test void cannotTakeYourOwnCoreOrForgedCoreId() throws Exception {
        try (NationDatabase db = new NationDatabase(temp.resolve("rejected.db"))) {
            var c=db.diplomacyConnection();
            statement(c,"INSERT INTO nations(id,name) VALUES ('a','Alfa'),('b','Bravo')");
            assertEquals(NationDatabase.CaptureOutcome.INVALID_VICTOR, db.captureCore("random","a","a"));
            assertEquals(NationDatabase.CaptureOutcome.NOT_FOUND, db.captureCore("random","a","b"));
        }
    }
}
