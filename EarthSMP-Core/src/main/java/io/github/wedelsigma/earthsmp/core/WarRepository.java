package io.github.wedelsigma.earthsmp.core;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Persistent diplomacy, sharing NationDatabase's synchronous SQLite connection. */
public final class WarRepository {
    public static final long ALLY_WINDOW_SECONDS = 4L * 60 * 60;
    private final NationDatabase nations;
    private final Connection db;

    public enum Status {
        SUCCESS, PEACE_PROPOSED, PEACE_CANCELLED, PEACE_ENDED,
        NOT_AUTHORIZED, NO_NATION, NATION_NOT_FOUND, SAME_NATION,
        WAR_ALREADY_EXISTS, WAR_NOT_FOUND, NOT_ACTIVE,
        NOT_PARTICIPANT, ALREADY_PARTICIPANT, ALREADY_INVITED,
        INVITE_NOT_FOUND, JOIN_WINDOW_CLOSED, PRIMARY_CANNOT_LEAVE,
        ONLY_PRIMARIES_CAN_PEACE, PEACE_ALREADY_OFFERED
    }
    public enum Side { ATTACKER, DEFENDER }
    public record Outcome(Status status, long warId) {}
    public record NationOption(String id, String name) {}
    public record War(long id, String attackerId, String attackerName, String defenderId,
                      String defenderName, String declaration, long declaredAt,
                      String peaceOfferedBy, Long endedAt) {
        public boolean active() { return endedAt == null; }
        public boolean joinWindowOpen(long now) { return active() && now < declaredAt + ALLY_WINDOW_SECONDS; }
    }
    public record Participant(String nationId, String nationName, Side side, boolean primaryParty) {}
    public record Invitation(long warId, String nationId, String nationName, Side side,
                             String attackerName, String defenderName, long declaredAt) {}

    public WarRepository(NationDatabase nations) {
        this.nations = nations;
        this.db = nations.diplomacyConnection();
    }
    private long now() { return Instant.now().getEpochSecond(); }

    private static void bind(PreparedStatement q, Object... items) throws SQLException {
        for (int i=0; i<items.length; i++) {
            Object x = items[i];
            if (x instanceof Integer n) q.setInt(i+1,n);
            else if (x instanceof Long n) q.setLong(i+1,n);
            else q.setString(i+1,x == null ? null : x.toString());
        }
    }
    private boolean exists(String query,Object... args) throws SQLException {
        try (PreparedStatement q = db.prepareStatement(query)) {
            bind(q,args);
            try(ResultSet r=q.executeQuery()) { return r.next(); }
        }
    }
    private NationDatabase.Membership manager(UUID player) throws SQLException {
        var member = nations.findMembership(player);
        if(member.isEmpty()) return null;
        return switch(member.get().role()) {
            case "LEADER", "DEPUTY" -> member.get();
            default -> null;
        };
    }
    private Outcome refuse(Status s,long id) throws SQLException { db.rollback(); return new Outcome(s,id); }
    private static void rollback(Connection c, Throwable t) {
        try {c.rollback();} catch(SQLException ex) {t.addSuppressed(ex);}
    }
    public List<NationOption> listNations() throws SQLException {
        List<NationOption> result = new ArrayList<>();
        try(PreparedStatement q=db.prepareStatement("SELECT id, name FROM nations ORDER BY name COLLATE NOCASE");
            ResultSet r=q.executeQuery()) {
            while(r.next()) result.add(new NationOption(r.getString(1),r.getString(2)));
        }
        return result;
    }
    public Optional<War> find(long id) throws SQLException {
        try(PreparedStatement q=db.prepareStatement("""
                SELECT w.id,w.attacker_id,a.name,w.defender_id,d.name,w.declaration,
                       w.declared_at,w.peace_offered_by,w.ended_at
                FROM wars w JOIN nations a ON a.id=w.attacker_id
                JOIN nations d ON d.id=w.defender_id WHERE w.id=?
                """)) {
            q.setLong(1,id);
            try(ResultSet r=q.executeQuery()) {
                if(!r.next()) return Optional.empty();
                long ended = r.getLong(9);
                // JDBC wasNull() applies to the immediately preceding column read.
                Long endedAt = r.wasNull() ? null : ended;
                return Optional.of(new War(r.getLong(1),r.getString(2),r.getString(3),
                    r.getString(4),r.getString(5),r.getString(6),r.getLong(7),
                    r.getString(8),endedAt));
            }
        }
    }
    public List<War> activeWars() throws SQLException {
        List<War> result=new ArrayList<>();
        try (PreparedStatement q=db.prepareStatement("SELECT id FROM wars WHERE ended_at IS NULL ORDER BY id DESC");
             ResultSet r=q.executeQuery()) {
            while(r.next()) find(r.getLong(1)).ifPresent(result::add);
        }
        return result;
    }
    public List<Participant> participants(long warId) throws SQLException {
        List<Participant> result=new ArrayList<>();
        try(PreparedStatement q=db.prepareStatement("""
                SELECT p.nation_id,n.name,p.side,p.primary_party
                FROM war_participants p JOIN nations n ON n.id=p.nation_id
                WHERE p.war_id=? ORDER BY p.primary_party DESC,n.name COLLATE NOCASE
                """)) {
            q.setLong(1,warId);
            try(ResultSet r=q.executeQuery()) {
                while(r.next()) result.add(new Participant(r.getString(1),r.getString(2),
                    Side.valueOf(r.getString(3)),r.getInt(4)==1));
            }
        }
        return result;
    }
    public List<Invitation> invitationsFor(UUID player) throws SQLException {
        var member=nations.findMembership(player);
        if(member.isEmpty()) return List.of();
        List<Invitation> result=new ArrayList<>();
        try(PreparedStatement q=db.prepareStatement("""
                SELECT i.war_id,i.nation_id,n.name,i.side,a.name,d.name,w.declared_at
                FROM war_invitations i JOIN wars w ON w.id=i.war_id
                JOIN nations n ON n.id=i.nation_id JOIN nations a ON a.id=w.attacker_id
                JOIN nations d ON d.id=w.defender_id
                WHERE i.nation_id=? AND w.ended_at IS NULL AND w.declared_at + ? > ?
                ORDER BY i.invited_at DESC
                """)) {
            bind(q,member.get().nationId(),ALLY_WINDOW_SECONDS,now());
            try(ResultSet r=q.executeQuery()) {
                while(r.next()) result.add(new Invitation(r.getLong(1),r.getString(2),r.getString(3),
                    Side.valueOf(r.getString(4)),r.getString(5),r.getString(6),r.getLong(7)));
            }
        }
        return result;
    }
    public Optional<Participant> participant(long warId, String nationId) throws SQLException {
        return participants(warId).stream().filter(p -> p.nationId().equals(nationId)).findFirst();
    }
    public boolean nationAtWar(String id) throws SQLException { return nations.nationAtWar(id); }
    public boolean opposed(String nationA,String nationB) throws SQLException {
        return exists("""
            SELECT 1 FROM war_participants a JOIN war_participants b ON a.war_id=b.war_id
            JOIN wars w ON w.id=a.war_id WHERE w.ended_at IS NULL AND
            a.nation_id=? AND b.nation_id=? AND a.side<>b.side LIMIT 1
            """,nationA,nationB);
    }
    public Outcome declare(UUID actor,String defenderNationId,String reason) throws SQLException {
        String note=reason==null?"":reason.strip();
        if(note.length()>200) note=note.substring(0,200);
        db.setAutoCommit(false);
        try {
            var member=manager(actor);
            if(member==null) return refuse(Status.NOT_AUTHORIZED,0);
            String attacker=member.nationId();
            if(attacker.equals(defenderNationId)) return refuse(Status.SAME_NATION,0);
            if(!exists("SELECT 1 FROM nations WHERE id=?",defenderNationId)) return refuse(Status.NATION_NOT_FOUND,0);
            if(exists("""
                SELECT 1 FROM wars WHERE ended_at IS NULL AND
                ((attacker_id=? AND defender_id=?) OR (attacker_id=? AND defender_id=?))
                """,attacker,defenderNationId,defenderNationId,attacker))
                return refuse(Status.WAR_ALREADY_EXISTS,0);
            long at=now();
            long id;
            try(PreparedStatement q=db.prepareStatement("""
                INSERT INTO wars(attacker_id,defender_id,declared_by,declaration,declared_at)
                VALUES(?,?,?,?,?)
                """,Statement.RETURN_GENERATED_KEYS)) {
                bind(q,attacker,defenderNationId,actor.toString(),note,at);
                q.executeUpdate();
                try(ResultSet r=q.getGeneratedKeys()) {
                    if(!r.next()) throw new SQLException("War INSERT did not return ID");
                    id=r.getLong(1);
                }
            }
            try(PreparedStatement q=db.prepareStatement("""
                INSERT INTO war_participants(war_id,nation_id,side,primary_party,joined_at)
                VALUES(?,?,?,1,?)
                """)) {
                bind(q,id,attacker,Side.ATTACKER.name(),at); q.executeUpdate();
                bind(q,id,defenderNationId,Side.DEFENDER.name(),at); q.executeUpdate();
            }
            db.commit();
            return new Outcome(Status.SUCCESS,id);
        } catch(SQLException|RuntimeException e){rollback(db,e);throw e;}
        finally {db.setAutoCommit(true);}
    }
    public Outcome inviteAlly(UUID actor,long warId,String targetNationId) throws SQLException {
        db.setAutoCommit(false);
        try {
            var member=manager(actor);
            if(member==null) return refuse(Status.NOT_AUTHORIZED,warId);
            var w=find(warId);
            if(w.isEmpty()) return refuse(Status.WAR_NOT_FOUND,warId);
            if(!w.get().active()) return refuse(Status.NOT_ACTIVE,warId);
            if(!w.get().joinWindowOpen(now())) return refuse(Status.JOIN_WINDOW_CLOSED,warId);
            var side=participant(warId,member.nationId());
            if(side.isEmpty()) return refuse(Status.NOT_PARTICIPANT,warId);
            if(!exists("SELECT 1 FROM nations WHERE id=?",targetNationId))
                return refuse(Status.NATION_NOT_FOUND,warId);
            if(participant(warId,targetNationId).isPresent())
                return refuse(Status.ALREADY_PARTICIPANT,warId);
            if(exists("SELECT 1 FROM war_invitations WHERE war_id=? AND nation_id=?",warId,targetNationId))
                return refuse(Status.ALREADY_INVITED,warId);
            try(PreparedStatement q=db.prepareStatement("""
                INSERT INTO war_invitations(war_id,nation_id,side,invited_by,invited_at)
                VALUES(?,?,?,?,?)
                """)) {
                bind(q,warId,targetNationId,side.get().side().name(),actor.toString(),now());q.executeUpdate();
            }
            db.commit();return new Outcome(Status.SUCCESS,warId);
        } catch(SQLException|RuntimeException e){rollback(db,e);throw e;}
        finally {db.setAutoCommit(true);}
    }
    public Outcome answerInvite(UUID actor,long warId,boolean accept) throws SQLException {
        db.setAutoCommit(false);
        try {
            var member=manager(actor);
            if(member==null) return refuse(Status.NOT_AUTHORIZED,warId);
            var war=find(warId);
            if(war.isEmpty()) return refuse(Status.WAR_NOT_FOUND,warId);
            if(!war.get().active()) return refuse(Status.NOT_ACTIVE,warId);
            if(!war.get().joinWindowOpen(now())) return refuse(Status.JOIN_WINDOW_CLOSED,warId);
            String side;
            try(PreparedStatement q=db.prepareStatement("""
                SELECT side FROM war_invitations WHERE war_id=? AND nation_id=?
                """)) {
                bind(q,warId,member.nationId());
                try(ResultSet r=q.executeQuery()) {
                    if(!r.next()) return refuse(Status.INVITE_NOT_FOUND,warId);
                    side=r.getString(1);
                }
            }
            if(participant(warId,member.nationId()).isPresent())
                return refuse(Status.ALREADY_PARTICIPANT,warId);
            if(accept) {
                try(PreparedStatement q=db.prepareStatement("""
                    INSERT INTO war_participants(war_id,nation_id,side,primary_party,joined_at)
                    VALUES(?,?,?,0,?)
                    """)) {
                    bind(q,warId,member.nationId(),side,now());q.executeUpdate();
                }
            }
            try(PreparedStatement q=db.prepareStatement("DELETE FROM war_invitations WHERE war_id=? AND nation_id=?")) {
                bind(q,warId,member.nationId());q.executeUpdate();
            }
            db.commit();return new Outcome(Status.SUCCESS,warId);
        } catch(SQLException|RuntimeException e){rollback(db,e);throw e;}
        finally{db.setAutoCommit(true);}
    }
    public Outcome leaveWar(UUID actor,long warId) throws SQLException {
        db.setAutoCommit(false);
        try {
            var member=manager(actor);
            if(member==null) return refuse(Status.NOT_AUTHORIZED,warId);
            var w=find(warId);
            if(w.isEmpty()) return refuse(Status.WAR_NOT_FOUND,warId);
            if(!w.get().active()) return refuse(Status.NOT_ACTIVE,warId);
            var p=participant(warId,member.nationId());
            if(p.isEmpty()) return refuse(Status.NOT_PARTICIPANT,warId);
            if(p.get().primaryParty()) return refuse(Status.PRIMARY_CANNOT_LEAVE,warId);
            try(PreparedStatement q=db.prepareStatement("DELETE FROM war_participants WHERE war_id=? AND nation_id=?")) {
                bind(q,warId,member.nationId());q.executeUpdate();
            }
            db.commit();return new Outcome(Status.SUCCESS,warId);
        } catch(SQLException|RuntimeException e){rollback(db,e);throw e;}
        finally{db.setAutoCommit(true);}
    }
    public Outcome peace(UUID actor,long warId) throws SQLException {
        db.setAutoCommit(false);
        try {
            var member=manager(actor);
            if(member==null) return refuse(Status.NOT_AUTHORIZED,warId);
            var w=find(warId);
            if(w.isEmpty()) return refuse(Status.WAR_NOT_FOUND,warId);
            if(!w.get().active()) return refuse(Status.NOT_ACTIVE,warId);
            String nation=member.nationId();
            if(!nation.equals(w.get().attackerId()) && !nation.equals(w.get().defenderId()))
                return refuse(Status.ONLY_PRIMARIES_CAN_PEACE,warId);
            if(nation.equals(w.get().peaceOfferedBy())) return refuse(Status.PEACE_ALREADY_OFFERED,warId);
            if(w.get().peaceOfferedBy()==null) {
                try(PreparedStatement q=db.prepareStatement("UPDATE wars SET peace_offered_by=? WHERE id=? AND ended_at IS NULL")) {
                    bind(q,nation,warId);q.executeUpdate();
                }
                db.commit();return new Outcome(Status.PEACE_PROPOSED,warId);
            }
            try(PreparedStatement q=db.prepareStatement("""
                UPDATE wars SET ended_at=?,ended_by=?,peace_offered_by=NULL
                WHERE id=? AND ended_at IS NULL
                """)) {
                bind(q,now(),actor.toString(),warId);q.executeUpdate();
            }
            try(PreparedStatement q=db.prepareStatement("DELETE FROM war_invitations WHERE war_id=?")) {
                q.setLong(1,warId);q.executeUpdate();
            }
            db.commit();return new Outcome(Status.PEACE_ENDED,warId);
        } catch(SQLException|RuntimeException e){rollback(db,e);throw e;}
        finally{db.setAutoCommit(true);}
    }
    public Outcome cancelPeace(UUID actor,long warId) throws SQLException {
        db.setAutoCommit(false);
        try {
            var member=manager(actor);
            if(member==null) return refuse(Status.NOT_AUTHORIZED,warId);
            var w=find(warId);
            if(w.isEmpty()) return refuse(Status.WAR_NOT_FOUND,warId);
            if(!w.get().active()) return refuse(Status.NOT_ACTIVE,warId);
            if(!member.nationId().equals(w.get().peaceOfferedBy()))
                return refuse(Status.ONLY_PRIMARIES_CAN_PEACE,warId);
            try(PreparedStatement q=db.prepareStatement("UPDATE wars SET peace_offered_by=NULL WHERE id=?")) {
                q.setLong(1,warId);q.executeUpdate();
            }
            db.commit();return new Outcome(Status.PEACE_CANCELLED,warId);
        } catch(SQLException|RuntimeException e){rollback(db,e);throw e;}
        finally{db.setAutoCommit(true);}
    }
    public Outcome adminEnd(long warId,UUID admin) throws SQLException {
        db.setAutoCommit(false);
        try {
            var w=find(warId);
            if(w.isEmpty()) return refuse(Status.WAR_NOT_FOUND,warId);
            if(!w.get().active()) return refuse(Status.NOT_ACTIVE,warId);
            try(PreparedStatement q=db.prepareStatement("UPDATE wars SET ended_at=?,ended_by=?,peace_offered_by=NULL WHERE id=?")) {
                bind(q,now(),"ADMIN:"+admin,warId);q.executeUpdate();
            }
            try(PreparedStatement q=db.prepareStatement("DELETE FROM war_invitations WHERE war_id=?")) {
                q.setLong(1,warId);q.executeUpdate();
            }
            db.commit();return new Outcome(Status.SUCCESS,warId);
        } catch(SQLException|RuntimeException e){rollback(db,e);throw e;}
        finally{db.setAutoCommit(true);}
    }
}
