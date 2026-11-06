package io.github.wedelsigma.earthsmp.core;

import java.nio.file.Path;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class SiegeStorageTest {
    @TempDir Path temp;

    @Test void provenancePersistsAndUnknownTerrainCannotBeMarkedByGuessingType() throws Exception {
        Path path=temp.resolve("provenance.sqlite");
        try(NationDatabase database=new NationDatabase(path)) {
            try(PreparedStatement stmt=database.diplomacyConnection().prepareStatement("PRAGMA user_version");
                ResultSet rows=stmt.executeQuery()) {
                assertTrue(rows.next());assertEquals(10,rows.getInt(1));
            }
            var stored=new SiegeStorage(database);
            assertFalse(stored.isPlayerPlaced("Earth",1,65,2,"STONE"));
            stored.markPlaced("Earth",1,65,2,"STONE");
            assertTrue(stored.isPlayerPlaced("Earth",1,65,2,"STONE"));
            assertFalse(stored.isPlayerPlaced("Earth",1,65,2,"DEEPSLATE"));
            assertFalse(stored.isPlayerPlaced("Other",1,65,2,"STONE"));
        }
        try(NationDatabase reopened=new NationDatabase(path)) {
            var stored=new SiegeStorage(reopened);
            assertTrue(stored.isPlayerPlaced("Earth",1,65,2,"STONE"));
            stored.forget("Earth",1,65,2);
            assertFalse(stored.isPlayerPlaced("Earth",1,65,2,"STONE"));
        }
    }

    @Test void copiedPickaxeIdHasOneSharedFifteenBlockAllowance() throws Exception {
        Path path=temp.resolve("pickaxe.sqlite");
        String id;
        try(NationDatabase database=new NationDatabase(path)) {
            var stored=new SiegeStorage(database);
            id=stored.issuePickaxe();
            assertEquals(15,stored.usesRemaining(id));
            for(int i=14;i>=0;i--) assertEquals(i,stored.usePickaxe(id));
            assertEquals(-1,stored.usePickaxe(id));
            assertEquals(0,stored.usesRemaining(id));
            assertEquals(0,stored.usesRemaining("not-a-real-id"));
        }
        try(NationDatabase reopened=new NationDatabase(path)) {
            var stored=new SiegeStorage(reopened);
            assertEquals(0,stored.usesRemaining(id));
            assertEquals(-1,stored.usePickaxe(id));
        }
    }
}
