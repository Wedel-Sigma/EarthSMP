package io.github.wedelsigma.earthsmp.core;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class WarRepositoryTest {
    @TempDir Path temp;
    private final UUID leaderA=UUID.randomUUID();
    private final UUID leaderB=UUID.randomUUID();
    private final UUID leaderC=UUID.randomUUID();
    private static void register(Connection c,String id,String name,UUID leader) throws SQLException {
        try(PreparedStatement q=c.prepareStatement("INSERT INTO nations(id,name) VALUES (?,?)")) {
            q.setString(1,id);q.setString(2,name);q.executeUpdate();
        }
        try(PreparedStatement q=c.prepareStatement("INSERT INTO nation_members(player_uuid,nation_id,role) VALUES (?,?,'LEADER')")) {
            q.setString(1,leader.toString());q.setString(2,id);q.executeUpdate();
        }
    }
    @Test void migrationAndWarLifecycleSurviveReopen() throws Exception {
        Path file=temp.resolve("test.sqlite");
        long warId;
        try(NationDatabase db=new NationDatabase(file)) {
            Connection sql=db.diplomacyConnection();
            try(PreparedStatement q=sql.prepareStatement("PRAGMA user_version");ResultSet r=q.executeQuery()) {
                assertTrue(r.next());assertEquals(8,r.getInt(1));
            }
            register(sql,"a","Alfa",leaderA);
            register(sql,"b","Bravo",leaderB);
            register(sql,"c","Charlie",leaderC);
            var wars=new WarRepository(db);
            var declared=wars.declare(leaderA,"b","Wojna testowa");
            assertEquals(WarRepository.Status.SUCCESS,declared.status());
            warId=declared.warId();
            assertEquals(WarRepository.Status.WAR_ALREADY_EXISTS,wars.declare(leaderB,"a","").status());
            assertTrue(wars.opposed("a","b"));
            assertTrue(db.nationAtWar("a"));
            assertEquals(WarRepository.Status.SUCCESS,wars.inviteAlly(leaderA,warId,"c").status());
            assertEquals(1,wars.invitationsFor(leaderC).size());
            assertEquals(WarRepository.Status.SUCCESS,wars.answerInvite(leaderC,warId,true).status());
            assertTrue(wars.opposed("c","b"));
            assertEquals(WarRepository.Status.PRIMARY_CANNOT_LEAVE,wars.leaveWar(leaderA,warId).status());
            assertEquals(WarRepository.Status.SUCCESS,wars.leaveWar(leaderC,warId).status());
            assertFalse(db.nationAtWar("c"));
            assertEquals(WarRepository.Status.PEACE_PROPOSED,wars.peace(leaderA,warId).status());
            // Offering peace does not end the war until the other primary party agrees.
            assertTrue(wars.find(warId).orElseThrow().active());
            assertEquals("a",wars.find(warId).orElseThrow().peaceOfferedBy());
            assertEquals(WarRepository.Status.PEACE_ALREADY_OFFERED,wars.peace(leaderA,warId).status());
            assertEquals(WarRepository.Status.PEACE_ENDED,wars.peace(leaderB,warId).status());
            assertFalse(wars.find(warId).orElseThrow().active());
            assertFalse(wars.nationAtWar("a"));
            assertFalse(wars.opposed("a","b"));
        }
        try(NationDatabase restored=new NationDatabase(file)) {
            var wars=new WarRepository(restored);
            assertTrue(wars.find(warId).isPresent());
            assertFalse(wars.find(warId).orElseThrow().active());
            assertEquals(0,wars.activeWars().size());
            assertEquals(3,restored.countNations());
        }
    }
    @Test void cannotForgeInvitationsOrPeaceWithoutAuthority() throws Exception {
        try(NationDatabase db=new NationDatabase(temp.resolve("other.sqlite"))) {
            Connection sql=db.diplomacyConnection();
            register(sql,"a","A",leaderA);register(sql,"b","B",leaderB);
            register(sql,"c","C",leaderC);
            var wars=new WarRepository(db);
            assertEquals(WarRepository.Status.NOT_AUTHORIZED,wars.declare(UUID.randomUUID(),"b","").status());
            long id=wars.declare(leaderA,"b","").warId();
            assertEquals(WarRepository.Status.INVITE_NOT_FOUND,wars.answerInvite(leaderC,id,true).status());
            assertEquals(WarRepository.Status.ONLY_PRIMARIES_CAN_PEACE,wars.peace(leaderC,id).status());
        }
    }
}
