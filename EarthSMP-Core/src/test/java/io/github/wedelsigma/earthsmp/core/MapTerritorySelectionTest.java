package io.github.wedelsigma.earthsmp.core;

import java.nio.file.Path;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Connection;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class MapTerritorySelectionTest {
    @TempDir Path temp;

    private static void setup(Connection c, UUID a, UUID b) throws Exception {
        try (var st=c.createStatement()) { st.execute("INSERT INTO nations(id,name) VALUES('a','Alfa'),('b','Bravo')"); }
        try (var q=c.prepareStatement("INSERT INTO nation_members(player_uuid,nation_id,role) VALUES(?,?, 'LEADER')")) {
            q.setString(1,a.toString());q.setString(2,"a");q.executeUpdate();
            q.setString(1,b.toString());q.setString(2,"b");q.executeUpdate();
        }
    }
    private static NationDatabase.TerritoryChunk ch(int x, int z) { return new NationDatabase.TerritoryChunk(x,z); }

    @Test void sparseAddOnlyAddsSelectedChunksNotBoundingRectangle() throws Exception {
        try (NationDatabase db=new NationDatabase(temp.resolve("sparse.db"))) {
            UUID a=UUID.randomUUID(),b=UUID.randomUUID();setup(db.diplomacyConnection(),a,b);
            var request=db.submitSelectedTerritoryRequest(a,"Earth",true,List.of(ch(-2,-2),ch(7,8),ch(2,-5))).orElseThrow();
            var record=db.findTerritoryRequest(request).orElseThrow();
            assertEquals(3, record.size()); assertTrue(record.sparse());
            assertEquals(3, db.territoryRequestChunks(request).size());
            assertEquals(NationDatabase.TerritoryDecision.APPROVED,db.reviewTerritory(request,"ADMIN",true));
            assertEquals("a",db.findTerritory("Earth",-2,-2).orElseThrow().nationId());
            assertEquals("a",db.findTerritory("Earth",7,8).orElseThrow().nationId());
            assertTrue(db.findTerritory("Earth",0,0).isEmpty());
            assertEquals(3, db.countClaimedChunks("a"));
        }
    }
    @Test void approvalDetectsForeignClaimsAndDoesNotPartiallyApply() throws Exception {
        try (NationDatabase db=new NationDatabase(temp.resolve("foreign.db"))) {
            UUID a=UUID.randomUUID(),b=UUID.randomUUID();setup(db.diplomacyConnection(),a,b);
            long id=db.submitSelectedTerritoryRequest(a,"Earth",true,List.of(ch(1,1),ch(2,2))).orElseThrow();
            try (var q=db.diplomacyConnection().prepareStatement(
                    "INSERT INTO territory_chunks(world_name,chunk_x,chunk_z,nation_id) VALUES ('Earth',2,2,'b')")) {
                q.executeUpdate();
            }
            assertEquals(NationDatabase.TerritoryDecision.CONFLICT,db.reviewTerritory(id,"ADMIN",true));
            assertTrue(db.findTerritory("Earth",1,1).isEmpty());
            assertEquals("b",db.findTerritory("Earth",2,2).orElseThrow().nationId());
            assertEquals("PENDING",db.findTerritoryRequest(id).orElseThrow().status());
            assertEquals(NationDatabase.TerritoryDecision.REJECTED,db.reviewTerritory(id,"ADMIN",false));
        }
    }
    @Test void sparseRemoveDeletesOnlySelectedOwnChunk() throws Exception {
        try (NationDatabase db=new NationDatabase(temp.resolve("remove.db"))) {
            UUID a=UUID.randomUUID(),b=UUID.randomUUID();setup(db.diplomacyConnection(),a,b);
            try(var q=db.diplomacyConnection().prepareStatement("""
                    INSERT INTO territory_chunks(world_name,chunk_x,chunk_z,nation_id)
                    VALUES('Earth',0,0,'a'),('Earth',1,0,'a'),('Earth',0,1,'a')
                    """)){q.executeUpdate();}
            long id=db.submitSelectedTerritoryRequest(a,"Earth",false,List.of(ch(1,0))).orElseThrow();
            assertEquals(NationDatabase.TerritoryDecision.APPROVED,db.reviewTerritory(id,"ADMIN",true));
            assertEquals(2,db.countClaimedChunks("a"));
            assertTrue(db.findTerritory("Earth",1,0).isEmpty());
            assertTrue(db.findTerritory("Earth",0,0).isPresent());
        }
    }
    @Test void persistsSparseRequestAndPreservesLegacyRectanglesAcrossRestart() throws Exception {
        Path file=temp.resolve("restart.db");UUID a=UUID.randomUUID(),b=UUID.randomUUID();
        long exact,legacy;
        try(NationDatabase db=new NationDatabase(file)) {
            setup(db.diplomacyConnection(),a,b);
            exact=db.submitSelectedTerritoryRequest(a,"Earth",true,List.of(ch(-6,3),ch(8,-2))).orElseThrow();
            legacy=db.submitTerritoryRequest(a,"Earth",true,1,1,2,2).orElseThrow();
        }
        try(NationDatabase db=new NationDatabase(file)) {
            assertEquals(2,db.findTerritoryRequest(exact).orElseThrow().size());
            assertEquals(2,db.territoryRequestChunks(exact).size());
            assertEquals(4,db.territoryRequestChunks(legacy).size());
            assertEquals(NationDatabase.TerritoryDecision.APPROVED,db.reviewTerritory(exact,"ADMIN",true));
            assertEquals(NationDatabase.TerritoryDecision.APPROVED,db.reviewTerritory(legacy,"ADMIN",true));
            assertEquals(6,db.countClaimedChunks("a"));
        }
    }
}
