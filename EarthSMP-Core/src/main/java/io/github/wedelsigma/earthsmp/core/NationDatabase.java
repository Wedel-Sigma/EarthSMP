package io.github.wedelsigma.earthsmp.core;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

/**
 * SQLite-backed nation repository. Use it only from Paper's main server thread.
 * All writes affecting multiple records are transactional.
 */
public final class NationDatabase implements AutoCloseable {

    private static final int SCHEMA_VERSION = 7;
    private static final long INVITE_LIFETIME_SECONDS = 24 * 60 * 60;
    private final Connection connection;

    public record Membership(String nationId, String nationName, String role) {}
    public record NationMember(UUID playerUuid, String role) {}
    public record NationInvite(String nationId, String nationName, long expiresAt) {}

    public enum InviteResult { SENT, NOT_AUTHORIZED, TARGET_ALREADY_MEMBER, SELF_INVITE }
    public enum AcceptResult { JOINED, ALREADY_MEMBER, NO_ACTIVE_INVITE }
    public enum ManageResult {
        SUCCESS, NOT_AUTHORIZED, NOT_IN_SAME_NATION, CANNOT_MODIFY_LEADER,
        INVALID_ROLE, DEPUTY_LIMIT, CANNOT_MODIFY_SELF
    }
    public enum MemberAction { PROMOTE, DEMOTE, KICK }
    public enum LeaveResult { LEFT, NOT_MEMBER, LEADER_MUST_TRANSFER }
    public enum TransferResult { TRANSFERRED, NOT_LEADER, INVALID_TARGET }
    public enum WithdrawResult { REMOVED, NOT_AUTHORIZED, NOT_FOUND }
    public record OutgoingInvite(UUID targetUuid, long expiresAt) {}

    public NationDatabase(Path file) throws IOException, SQLException {
        Path absoluteFile = file.toAbsolutePath();
        Files.createDirectories(absoluteFile.getParent());
        Connection opened = DriverManager.getConnection("jdbc:sqlite:" + absoluteFile);
        try {
            initializeSchema(opened);
        } catch (SQLException | RuntimeException error) {
            try {
                opened.close();
            } catch (SQLException closeError) {
                error.addSuppressed(closeError);
            }
            throw error;
        }
        this.connection = opened;
    }

    private static void initializeSchema(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute("PRAGMA foreign_keys = ON");
            statement.execute("PRAGMA busy_timeout = 5000");
            int version;
            try (ResultSet result = statement.executeQuery("PRAGMA user_version")) {
                version = result.next() ? result.getInt(1) : 0;
            }
            if (version > SCHEMA_VERSION) {
                throw new SQLException("Database schema is newer than this plugin (v" + version + ")");
            }
            if (version == 0) {
                connection.setAutoCommit(false);
                try {
                    statement.executeUpdate("""
                            CREATE TABLE IF NOT EXISTS nations (
                                id TEXT PRIMARY KEY,
                                name TEXT NOT NULL UNIQUE COLLATE NOCASE,
                                created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP
                            )
                            """);
                    statement.executeUpdate("""
                            CREATE TABLE IF NOT EXISTS nation_members (
                                player_uuid TEXT PRIMARY KEY,
                                nation_id TEXT NOT NULL,
                                role TEXT NOT NULL CHECK (role IN ('LEADER', 'DEPUTY', 'MEMBER')),
                                joined_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                FOREIGN KEY (nation_id) REFERENCES nations(id) ON DELETE CASCADE
                            )
                            """);
                    statement.executeUpdate("""
                            CREATE INDEX IF NOT EXISTS idx_nation_members_nation_id
                            ON nation_members(nation_id)
                            """);
                    statement.executeUpdate("""
                            CREATE UNIQUE INDEX IF NOT EXISTS idx_nation_one_leader
                            ON nation_members(nation_id) WHERE role = 'LEADER'
                            """);
                    statement.executeUpdate("""
                            CREATE TRIGGER IF NOT EXISTS trg_nation_deputies_insert
                            BEFORE INSERT ON nation_members
                            WHEN NEW.role = 'DEPUTY'
                              AND (SELECT COUNT(*) FROM nation_members
                                   WHERE nation_id = NEW.nation_id AND role = 'DEPUTY') >= 2
                            BEGIN
                                SELECT RAISE(ABORT, 'A nation can have at most two deputies');
                            END
                            """);
                    statement.executeUpdate("""
                            CREATE TRIGGER IF NOT EXISTS trg_nation_deputies_update
                            BEFORE UPDATE OF nation_id, role ON nation_members
                            WHEN NEW.role = 'DEPUTY'
                              AND (SELECT COUNT(*) FROM nation_members
                                   WHERE nation_id = NEW.nation_id AND role = 'DEPUTY'
                                     AND player_uuid <> OLD.player_uuid) >= 2
                            BEGIN
                                SELECT RAISE(ABORT, 'A nation can have at most two deputies');
                            END
                            """);
                    statement.execute("PRAGMA user_version = 1");
                    connection.commit();
                    version = 1;
                } catch (SQLException | RuntimeException error) {
                    rollbackAfter(connection, error);
                    throw error;
                } finally {
                    connection.setAutoCommit(true);
                }
            }
            if (version == 1) {
                connection.setAutoCommit(false);
                try {
                    statement.executeUpdate("""
                            CREATE TABLE IF NOT EXISTS nation_invites (
                                nation_id TEXT NOT NULL,
                                player_uuid TEXT NOT NULL,
                                expires_at INTEGER NOT NULL,
                                created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                PRIMARY KEY (nation_id, player_uuid),
                                FOREIGN KEY (nation_id) REFERENCES nations(id) ON DELETE CASCADE
                            )
                            """);
                    statement.executeUpdate("""
                            CREATE INDEX IF NOT EXISTS idx_nation_invites_target
                            ON nation_invites(player_uuid, expires_at)
                            """);
                    statement.execute("PRAGMA user_version = 2");
                    connection.commit();
                    version = 2;
                } catch (SQLException | RuntimeException error) {
                    try {
                        connection.rollback();
                    } catch (SQLException rollbackError) {
                        error.addSuppressed(rollbackError);
                    }
                    throw error;
                } finally {
                    connection.setAutoCommit(true);
                }
            }
            if (version == 2) {
                connection.setAutoCommit(false);
                try {
                    statement.executeUpdate("""
                            CREATE TABLE IF NOT EXISTS territory_chunks (
                                world_name TEXT NOT NULL,
                                chunk_x INTEGER NOT NULL,
                                chunk_z INTEGER NOT NULL,
                                nation_id TEXT NOT NULL,
                                claimed_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                PRIMARY KEY (world_name, chunk_x, chunk_z),
                                FOREIGN KEY (nation_id) REFERENCES nations(id) ON DELETE CASCADE
                            )
                            """);
                    statement.executeUpdate("""
                            CREATE INDEX IF NOT EXISTS idx_territory_nation
                            ON territory_chunks(nation_id)
                            """);
                    statement.executeUpdate("""
                            CREATE TABLE IF NOT EXISTS territory_requests (
                                id INTEGER PRIMARY KEY AUTOINCREMENT,
                                nation_id TEXT NOT NULL,
                                player_uuid TEXT NOT NULL,
                                world_name TEXT NOT NULL,
                                action TEXT NOT NULL CHECK (action IN ('ADD', 'REMOVE')),
                                min_x INTEGER NOT NULL,
                                min_z INTEGER NOT NULL,
                                max_x INTEGER NOT NULL,
                                max_z INTEGER NOT NULL,
                                status TEXT NOT NULL DEFAULT 'PENDING'
                                    CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED', 'CANCELLED')),
                                reviewed_by TEXT,
                                created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                reviewed_at TEXT,
                                FOREIGN KEY (nation_id) REFERENCES nations(id) ON DELETE CASCADE
                            )
                            """);
                    statement.executeUpdate("""
                            CREATE INDEX IF NOT EXISTS idx_territory_requests_pending
                            ON territory_requests(status, id)
                            """);
                    statement.executeUpdate("""
                            CREATE TABLE IF NOT EXISTS territory_audit (
                                id INTEGER PRIMARY KEY AUTOINCREMENT,
                                actor TEXT NOT NULL,
                                action TEXT NOT NULL,
                                nation_id TEXT,
                                world_name TEXT NOT NULL,
                                min_x INTEGER NOT NULL,
                                min_z INTEGER NOT NULL,
                                max_x INTEGER NOT NULL,
                                max_z INTEGER NOT NULL,
                                created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP
                            )
                            """);
                    statement.execute("PRAGMA user_version = 3");
                    connection.commit();
                    version = 3;
                } catch (SQLException | RuntimeException error) {
                    rollbackAfter(connection, error);
                    throw error;
                } finally {
                    connection.setAutoCommit(true);
                }
            }
            if (version == 3) {
                connection.setAutoCommit(false);
                try {
                    statement.executeUpdate("""
                            CREATE TABLE IF NOT EXISTS protection_cores (
                                id TEXT PRIMARY KEY,
                                nation_id TEXT NOT NULL,
                                world_name TEXT NOT NULL,
                                block_x INTEGER NOT NULL,
                                block_y INTEGER NOT NULL,
                                block_z INTEGER NOT NULL,
                                origin TEXT NOT NULL CHECK (origin IN ('FREE', 'CRAFTED', 'CONQUERED')),
                                is_capital INTEGER NOT NULL DEFAULT 0 CHECK (is_capital IN (0, 1)),
                                placed_by TEXT NOT NULL,
                                placed_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                UNIQUE(world_name, block_x, block_y, block_z),
                                FOREIGN KEY (nation_id) REFERENCES nations(id) ON DELETE CASCADE
                            )
                            """);
                    statement.executeUpdate("""
                            CREATE UNIQUE INDEX IF NOT EXISTS idx_one_free_core_per_nation
                            ON protection_cores(nation_id) WHERE origin = 'FREE'
                            """);
                    statement.executeUpdate("""
                            CREATE UNIQUE INDEX IF NOT EXISTS idx_one_capital_per_nation
                            ON protection_cores(nation_id) WHERE is_capital = 1
                            """);
                    statement.executeUpdate("""
                            CREATE INDEX IF NOT EXISTS idx_core_coordinates
                            ON protection_cores(world_name, block_x, block_z)
                            """);
                    statement.execute("PRAGMA user_version = 4");
                    connection.commit();
                    version = 4;
                } catch (SQLException | RuntimeException error) {
                    rollbackAfter(connection, error);
                    throw error;
                } finally {
                    connection.setAutoCommit(true);
                }
            }
            if (version == 4) {
                connection.setAutoCommit(false);
                try {
                    statement.executeUpdate("""
                            CREATE TABLE IF NOT EXISTS core_tokens (
                                id TEXT PRIMARY KEY,
                                nation_id TEXT NOT NULL,
                                origin TEXT NOT NULL CHECK (origin IN ('FREE', 'CRAFTED')),
                                state TEXT NOT NULL CHECK (state IN ('ITEM', 'PLACED')),
                                issued_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                FOREIGN KEY (nation_id) REFERENCES nations(id) ON DELETE CASCADE
                            )
                            """);
                    // Adopt legacy v1.0 protection cores, retaining their IDs and nation ownership.
                    statement.executeUpdate("""
                            INSERT OR IGNORE INTO core_tokens (id, nation_id, origin, state)
                            SELECT id, nation_id, origin, 'PLACED' FROM protection_cores
                            WHERE origin IN ('FREE', 'CRAFTED')
                            """);
                    statement.executeUpdate("""
                            CREATE UNIQUE INDEX IF NOT EXISTS idx_one_free_token_per_nation
                            ON core_tokens(nation_id) WHERE origin = 'FREE'
                            """);
                    statement.executeUpdate("""
                            CREATE INDEX IF NOT EXISTS idx_core_tokens_nation_origin
                            ON core_tokens(nation_id, origin, state)
                            """);
                    statement.execute("PRAGMA user_version = 5");
                    connection.commit();
                    version = 5;
                } catch (SQLException | RuntimeException error) {
                    rollbackAfter(connection, error);
                    throw error;
                } finally {
                    connection.setAutoCommit(true);
                }
            }
            if (version == 5) {
                connection.setAutoCommit(false);
                try {
                    statement.executeUpdate("""
                            CREATE TABLE IF NOT EXISTS wars (
                                id INTEGER PRIMARY KEY AUTOINCREMENT,
                                attacker_id TEXT NOT NULL REFERENCES nations(id),
                                defender_id TEXT NOT NULL REFERENCES nations(id),
                                declared_by TEXT NOT NULL,
                                declaration TEXT NOT NULL DEFAULT '',
                                declared_at INTEGER NOT NULL,
                                peace_offered_by TEXT REFERENCES nations(id),
                                ended_at INTEGER,
                                ended_by TEXT,
                                CHECK (attacker_id <> defender_id)
                            )
                            """);
                    statement.executeUpdate("""
                            CREATE UNIQUE INDEX IF NOT EXISTS idx_one_active_main_war
                            ON wars(min(attacker_id, defender_id), max(attacker_id, defender_id))
                            WHERE ended_at IS NULL
                            """);
                    statement.executeUpdate("""
                            CREATE TABLE IF NOT EXISTS war_participants (
                                war_id INTEGER NOT NULL REFERENCES wars(id) ON DELETE CASCADE,
                                nation_id TEXT NOT NULL REFERENCES nations(id),
                                side TEXT NOT NULL CHECK (side IN ('ATTACKER', 'DEFENDER')),
                                primary_party INTEGER NOT NULL DEFAULT 0 CHECK (primary_party IN (0,1)),
                                joined_at INTEGER NOT NULL,
                                PRIMARY KEY (war_id, nation_id)
                            )
                            """);
                    statement.executeUpdate("""
                            CREATE INDEX IF NOT EXISTS idx_war_participants_nation
                            ON war_participants(nation_id, war_id)
                            """);
                    statement.executeUpdate("""
                            CREATE TABLE IF NOT EXISTS war_invitations (
                                war_id INTEGER NOT NULL REFERENCES wars(id) ON DELETE CASCADE,
                                nation_id TEXT NOT NULL REFERENCES nations(id),
                                side TEXT NOT NULL CHECK (side IN ('ATTACKER', 'DEFENDER')),
                                invited_by TEXT NOT NULL,
                                invited_at INTEGER NOT NULL,
                                PRIMARY KEY (war_id, nation_id)
                            )
                            """);
                    statement.execute("PRAGMA user_version = 6");
                    connection.commit();
                    version = 6;
                } catch (SQLException | RuntimeException error) {
                    rollbackAfter(connection, error);
                    throw error;
                } finally {
                    connection.setAutoCommit(true);
                }
            }
            if (version == 6) {
                connection.setAutoCommit(false);
                try {
                    statement.executeUpdate("""
                            CREATE TABLE IF NOT EXISTS player_placed_blocks (
                                world_name TEXT NOT NULL,
                                block_x INTEGER NOT NULL,
                                block_y INTEGER NOT NULL,
                                block_z INTEGER NOT NULL,
                                block_type TEXT NOT NULL,
                                PRIMARY KEY (world_name, block_x, block_y, block_z)
                            )
                            """);
                    statement.executeUpdate("""
                            CREATE TABLE IF NOT EXISTS siege_pickaxes (
                                id TEXT PRIMARY KEY,
                                remaining INTEGER NOT NULL CHECK (remaining BETWEEN 0 AND 15),
                                issued_at INTEGER NOT NULL
                            )
                            """);
                    statement.execute("PRAGMA user_version = 7");
                    connection.commit();
                } catch (SQLException | RuntimeException error) {
                    rollbackAfter(connection, error);
                    throw error;
                } finally {
                    connection.setAutoCommit(true);
                }
            }
        }
    }

    // Package-private: WarRepository shares the existing connection/transaction boundary.
    Connection diplomacyConnection() { return connection; }

    public boolean nationAtWar(String nationId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT 1 FROM war_participants p JOIN wars w ON w.id=p.war_id
                WHERE p.nation_id=? AND w.ended_at IS NULL LIMIT 1
                """)) {
            statement.setString(1, nationId);
            try (ResultSet rows = statement.executeQuery()) { return rows.next(); }
        }
    }

    public int countNations() throws SQLException {
        try (Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery("SELECT COUNT(*) FROM nations")) {
            result.next();
            return result.getInt(1);
        }
    }

    /** Exactly zero or one nation per player, enforced by nation_members.player_uuid. */
    public Optional<Membership> findMembership(UUID playerUuid) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT n.id, n.name, m.role
                FROM nation_members AS m
                JOIN nations AS n ON n.id = m.nation_id
                WHERE m.player_uuid = ?
                """)) {
            statement.setString(1, playerUuid.toString());
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) return Optional.empty();
                return Optional.of(new Membership(result.getString(1), result.getString(2), result.getString(3)));
            }
        }
    }

    public boolean createNation(String name, UUID founderUuid) throws SQLException {
        connection.setAutoCommit(false);
        try {
            if (findMembership(founderUuid).isPresent() || isNameTaken(name)) {
                connection.rollback();
                return false;
            }
            String nationId = UUID.randomUUID().toString();
            try (PreparedStatement nation = connection.prepareStatement(
                    "INSERT INTO nations (id, name) VALUES (?, ?)")) {
                nation.setString(1, nationId);
                nation.setString(2, name);
                nation.executeUpdate();
            }
            try (PreparedStatement member = connection.prepareStatement(
                    "INSERT INTO nation_members (player_uuid, nation_id, role) VALUES (?, ?, 'LEADER')")) {
                member.setString(1, founderUuid.toString());
                member.setString(2, nationId);
                member.executeUpdate();
            }
            // A founder cannot keep stale invitations from other countries.
            try (PreparedStatement clear = connection.prepareStatement(
                    "DELETE FROM nation_invites WHERE player_uuid = ?")) {
                clear.setString(1, founderUuid.toString());
                clear.executeUpdate();
            }
            connection.commit();
            return true;
        } catch (SQLException | RuntimeException error) {
            rollbackAfter(connection, error);
            throw error;
        } finally {
            connection.setAutoCommit(true);
        }
    }

    private boolean isNameTaken(String proposed) throws SQLException {
        String proposedKey = proposed.toLowerCase(Locale.ROOT);
        try (Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery("SELECT name FROM nations")) {
            while (result.next()) {
                if (result.getString(1).toLowerCase(Locale.ROOT).equals(proposedKey)) return true;
            }
        }
        return false;
    }

    public List<NationMember> findMembers(String nationId) throws SQLException {
        List<NationMember> resultMembers = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT player_uuid, role FROM nation_members WHERE nation_id = ?
                ORDER BY CASE role WHEN 'LEADER' THEN 0 WHEN 'DEPUTY' THEN 1 ELSE 2 END, joined_at
                """)) {
            statement.setString(1, nationId);
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    resultMembers.add(new NationMember(UUID.fromString(result.getString(1)), result.getString(2)));
                }
            }
        }
        return resultMembers;
    }

    public InviteResult invite(UUID actorUuid, UUID targetUuid) throws SQLException {
        if (actorUuid.equals(targetUuid)) return InviteResult.SELF_INVITE;
        Optional<Membership> actor = findMembership(actorUuid);
        if (actor.isEmpty() || !isManager(actor.get().role())) return InviteResult.NOT_AUTHORIZED;
        if (findMembership(targetUuid).isPresent()) return InviteResult.TARGET_ALREADY_MEMBER;
        long expires = Instant.now().getEpochSecond() + INVITE_LIFETIME_SECONDS;
        try (PreparedStatement statement = connection.prepareStatement("""
                INSERT INTO nation_invites(nation_id, player_uuid, expires_at)
                VALUES (?, ?, ?)
                ON CONFLICT(nation_id, player_uuid) DO UPDATE SET
                    expires_at = excluded.expires_at,
                    created_at = CURRENT_TIMESTAMP
                """)) {
            statement.setString(1, actor.get().nationId());
            statement.setString(2, targetUuid.toString());
            statement.setLong(3, expires);
            statement.executeUpdate();
        }
        return InviteResult.SENT;
    }

    public List<NationInvite> findInvites(UUID playerUuid) throws SQLException {
        List<NationInvite> invites = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT n.id, n.name, i.expires_at
                FROM nation_invites AS i
                JOIN nations AS n ON n.id = i.nation_id
                WHERE i.player_uuid = ? AND i.expires_at > ?
                ORDER BY n.name
                """)) {
            statement.setString(1, playerUuid.toString());
            statement.setLong(2, Instant.now().getEpochSecond());
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) invites.add(new NationInvite(result.getString(1), result.getString(2), result.getLong(3)));
            }
        }
        return invites;
    }

    private Optional<NationInvite> findInviteByName(UUID playerUuid, String name) throws SQLException {
        return findInvites(playerUuid).stream()
                .filter(invite -> invite.nationName().equalsIgnoreCase(name))
                .findFirst();
    }

    public AcceptResult acceptInvite(UUID playerUuid, String nationName) throws SQLException {
        connection.setAutoCommit(false);
        try {
            if (findMembership(playerUuid).isPresent()) {
                connection.rollback();
                return AcceptResult.ALREADY_MEMBER;
            }
            Optional<NationInvite> invitation = findInviteByName(playerUuid, nationName);
            if (invitation.isEmpty()) {
                connection.rollback();
                return AcceptResult.NO_ACTIVE_INVITE;
            }
            try (PreparedStatement statement = connection.prepareStatement(
                    "INSERT INTO nation_members(player_uuid, nation_id, role) VALUES (?, ?, 'MEMBER')")) {
                statement.setString(1, playerUuid.toString());
                statement.setString(2, invitation.get().nationId());
                statement.executeUpdate();
            }
            // Joining one nation clears all other outstanding invitations.
            try (PreparedStatement statement = connection.prepareStatement(
                    "DELETE FROM nation_invites WHERE player_uuid = ?")) {
                statement.setString(1, playerUuid.toString());
                statement.executeUpdate();
            }
            connection.commit();
            return AcceptResult.JOINED;
        } catch (SQLException | RuntimeException error) {
            rollbackAfter(connection, error);
            throw error;
        } finally {
            connection.setAutoCommit(true);
        }
    }

    public boolean declineInvite(UUID playerUuid, String nationName) throws SQLException {
        Optional<NationInvite> invitation = findInviteByName(playerUuid, nationName);
        if (invitation.isEmpty()) return false;
        try (PreparedStatement statement = connection.prepareStatement(
                "DELETE FROM nation_invites WHERE nation_id = ? AND player_uuid = ?")) {
            statement.setString(1, invitation.get().nationId());
            statement.setString(2, playerUuid.toString());
            return statement.executeUpdate() == 1;
        }
    }

    public ManageResult manageMember(UUID actorUuid, UUID targetUuid, MemberAction action) throws SQLException {
        if (actorUuid.equals(targetUuid)) return ManageResult.CANNOT_MODIFY_SELF;
        Optional<Membership> actor = findMembership(actorUuid);
        if (actor.isEmpty() || !isManager(actor.get().role())) return ManageResult.NOT_AUTHORIZED;
        Optional<Membership> target = findMembership(targetUuid);
        if (target.isEmpty() || !actor.get().nationId().equals(target.get().nationId())) {
            return ManageResult.NOT_IN_SAME_NATION;
        }
        if (target.get().role().equals("LEADER")) return ManageResult.CANNOT_MODIFY_LEADER;
        if (action == MemberAction.PROMOTE) {
            if (!target.get().role().equals("MEMBER")) return ManageResult.INVALID_ROLE;
            if (countDeputies(actor.get().nationId()) >= 2) return ManageResult.DEPUTY_LIMIT;
        } else if (action == MemberAction.DEMOTE && !target.get().role().equals("DEPUTY")) {
            return ManageResult.INVALID_ROLE;
        }

        if (action == MemberAction.KICK) {
            try (PreparedStatement statement = connection.prepareStatement(
                    "DELETE FROM nation_members WHERE player_uuid = ? AND nation_id = ?")) {
                statement.setString(1, targetUuid.toString());
                statement.setString(2, actor.get().nationId());
                statement.executeUpdate();
            }
        } else {
            String newRole = action == MemberAction.PROMOTE ? "DEPUTY" : "MEMBER";
            try (PreparedStatement statement = connection.prepareStatement(
                    "UPDATE nation_members SET role = ? WHERE player_uuid = ? AND nation_id = ?")) {
                statement.setString(1, newRole);
                statement.setString(2, targetUuid.toString());
                statement.setString(3, actor.get().nationId());
                statement.executeUpdate();
            }
        }
        return ManageResult.SUCCESS;
    }

    /** Non-leaders may leave voluntarily. Leaders must transfer ownership first. */
    public LeaveResult leaveNation(UUID actorUuid) throws SQLException {
        Optional<Membership> actor = findMembership(actorUuid);
        if (actor.isEmpty()) return LeaveResult.NOT_MEMBER;
        if ("LEADER".equals(actor.get().role())) return LeaveResult.LEADER_MUST_TRANSFER;
        try (PreparedStatement statement = connection.prepareStatement(
                "DELETE FROM nation_members WHERE player_uuid = ? AND role <> 'LEADER'")) {
            statement.setString(1, actorUuid.toString());
            return statement.executeUpdate() == 1 ? LeaveResult.LEFT : LeaveResult.LEADER_MUST_TRANSFER;
        }
    }

    /** Transfer in a transaction; unique index still guarantees exactly one leader. */
    public TransferResult transferLeadership(UUID actorUuid, UUID targetUuid) throws SQLException {
        if (actorUuid.equals(targetUuid)) return TransferResult.INVALID_TARGET;
        connection.setAutoCommit(false);
        try {
            Optional<Membership> actor = findMembership(actorUuid);
            if (actor.isEmpty() || !"LEADER".equals(actor.get().role())) {
                connection.rollback();
                return TransferResult.NOT_LEADER;
            }
            Optional<Membership> target = findMembership(targetUuid);
            if (target.isEmpty() || !actor.get().nationId().equals(target.get().nationId())) {
                connection.rollback();
                return TransferResult.INVALID_TARGET;
            }
            try (PreparedStatement demote = connection.prepareStatement(
                    "UPDATE nation_members SET role = 'MEMBER' WHERE player_uuid = ? AND role = 'LEADER'")) {
                demote.setString(1, actorUuid.toString());
                if (demote.executeUpdate() != 1) throw new SQLException("Leader changed during transfer");
            }
            try (PreparedStatement promote = connection.prepareStatement(
                    "UPDATE nation_members SET role = 'LEADER' WHERE player_uuid = ? AND nation_id = ?")) {
                promote.setString(1, targetUuid.toString());
                promote.setString(2, actor.get().nationId());
                if (promote.executeUpdate() != 1) throw new SQLException("Target changed during transfer");
            }
            connection.commit();
            return TransferResult.TRANSFERRED;
        } catch (SQLException | RuntimeException error) {
            rollbackAfter(connection, error);
            throw error;
        } finally {
            connection.setAutoCommit(true);
        }
    }

    public List<OutgoingInvite> findOutgoingInvites(UUID actorUuid) throws SQLException {
        Optional<Membership> actor = findMembership(actorUuid);
        if (actor.isEmpty() || !isManager(actor.get().role())) return List.of();
        List<OutgoingInvite> result = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT player_uuid, expires_at FROM nation_invites
                WHERE nation_id = ? AND expires_at > ? ORDER BY created_at DESC
                """)) {
            statement.setString(1, actor.get().nationId());
            statement.setLong(2, Instant.now().getEpochSecond());
            try (ResultSet rows = statement.executeQuery()) {
                while (rows.next()) result.add(new OutgoingInvite(UUID.fromString(rows.getString(1)), rows.getLong(2)));
            }
        }
        return result;
    }

    public WithdrawResult withdrawInvite(UUID actorUuid, UUID targetUuid) throws SQLException {
        Optional<Membership> actor = findMembership(actorUuid);
        if (actor.isEmpty() || !isManager(actor.get().role())) return WithdrawResult.NOT_AUTHORIZED;
        try (PreparedStatement statement = connection.prepareStatement(
                "DELETE FROM nation_invites WHERE nation_id = ? AND player_uuid = ? AND expires_at > ?")) {
            statement.setString(1, actor.get().nationId());
            statement.setString(2, targetUuid.toString());
            statement.setLong(3, Instant.now().getEpochSecond());
            return statement.executeUpdate() == 1 ? WithdrawResult.REMOVED : WithdrawResult.NOT_FOUND;
        }
    }

    private int countDeputies(String nationId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT COUNT(*) FROM nation_members WHERE nation_id = ? AND role = 'DEPUTY'")) {
            statement.setString(1, nationId);
            try (ResultSet result = statement.executeQuery()) {
                result.next();
                return result.getInt(1);
            }
        }
    }

    private static boolean isManager(String role) {
        return "LEADER".equals(role) || "DEPUTY".equals(role);
    }

    private static void rollbackAfter(Connection connection, Exception error) {
        try {
            connection.rollback();
        } catch (SQLException rollbackError) {
            error.addSuppressed(rollbackError);
        }
    }

    // Territory is strictly political. This layer NEVER modifies blocks or claims WorldGuard regions.
    public record TerritoryOwner(String nationId, String nationName) {}
    public record TerritoryRequest(long id, String nationId, String nationName, String action,
                                   String world, int minX, int minZ, int maxX, int maxZ, String status) {
        public long size() { return (long) (maxX - minX + 1) * (maxZ - minZ + 1); }
    }
    public enum TerritoryDecision { APPROVED, REJECTED, CANCELLED, NOT_FOUND, CONFLICT, NOT_AUTHORIZED }

    public Optional<TerritoryOwner> findTerritory(String world, int chunkX, int chunkZ) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT n.id, n.name FROM territory_chunks AS c JOIN nations AS n ON n.id = c.nation_id
                WHERE c.world_name = ? AND c.chunk_x = ? AND c.chunk_z = ?
                """)) {
            statement.setString(1, world);
            statement.setInt(2, chunkX);
            statement.setInt(3, chunkZ);
            try (ResultSet rows = statement.executeQuery()) {
                return rows.next() ? Optional.of(new TerritoryOwner(rows.getString(1), rows.getString(2)))
                                   : Optional.empty();
            }
        }
    }

    public Optional<String> findNationIdByName(String name) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT id FROM nations WHERE name = ? COLLATE NOCASE")) {
            statement.setString(1, name);
            try (ResultSet rows = statement.executeQuery()) {
                return rows.next() ? Optional.of(rows.getString(1)) : Optional.empty();
            }
        }
    }

    /** Only LEADER / DEPUTY can submit political border proposals. */
    public Optional<Long> submitTerritoryRequest(UUID actor, String world, boolean add,
                                                  int minX, int minZ, int maxX, int maxZ) throws SQLException {
        Optional<Membership> membership = findMembership(actor);
        if (membership.isEmpty() || !isManager(membership.get().role())) return Optional.empty();
        try (PreparedStatement statement = connection.prepareStatement("""
                INSERT INTO territory_requests
                  (nation_id, player_uuid, world_name, action, min_x, min_z, max_x, max_z)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, membership.get().nationId());
            statement.setString(2, actor.toString());
            statement.setString(3, world);
            statement.setString(4, add ? "ADD" : "REMOVE");
            statement.setInt(5, minX);
            statement.setInt(6, minZ);
            statement.setInt(7, maxX);
            statement.setInt(8, maxZ);
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) return Optional.of(keys.getLong(1));
            }
        }
        throw new SQLException("SQLite did not return territory request ID");
    }

    public List<TerritoryRequest> findPendingTerritoryRequests() throws SQLException {
        List<TerritoryRequest> result = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT r.id, r.nation_id, n.name, r.action, r.world_name, r.min_x, r.min_z,
                       r.max_x, r.max_z, r.status
                FROM territory_requests AS r JOIN nations AS n ON n.id = r.nation_id
                WHERE r.status = 'PENDING' ORDER BY r.id LIMIT 50
                """)) {
            try (ResultSet rows = statement.executeQuery()) {
                while (rows.next()) result.add(readTerritoryRequest(rows));
            }
        }
        return result;
    }

    public List<TerritoryRequest> findOwnPendingTerritoryRequests(UUID actor) throws SQLException {
        Optional<Membership> membership = findMembership(actor);
        if (membership.isEmpty()) return List.of();
        List<TerritoryRequest> result = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT r.id, r.nation_id, n.name, r.action, r.world_name, r.min_x, r.min_z,
                       r.max_x, r.max_z, r.status
                FROM territory_requests AS r JOIN nations AS n ON n.id = r.nation_id
                WHERE r.status = 'PENDING' AND r.nation_id = ? ORDER BY r.id LIMIT 50
                """)) {
            statement.setString(1, membership.get().nationId());
            try (ResultSet rows = statement.executeQuery()) {
                while (rows.next()) result.add(readTerritoryRequest(rows));
            }
        }
        return result;
    }

    private static TerritoryRequest readTerritoryRequest(ResultSet rows) throws SQLException {
        return new TerritoryRequest(rows.getLong(1), rows.getString(2), rows.getString(3), rows.getString(4),
                rows.getString(5), rows.getInt(6), rows.getInt(7), rows.getInt(8), rows.getInt(9), rows.getString(10));
    }

    /** Decision and claim changes are one atomic SQLite transaction. */
    public TerritoryDecision reviewTerritory(long requestId, String reviewer, boolean approve) throws SQLException {
        connection.setAutoCommit(false);
        try {
            TerritoryRequest request;
            try (PreparedStatement statement = connection.prepareStatement("""
                    SELECT r.id, r.nation_id, n.name, r.action, r.world_name, r.min_x, r.min_z,
                           r.max_x, r.max_z, r.status
                    FROM territory_requests AS r JOIN nations AS n ON n.id = r.nation_id
                    WHERE r.id = ? AND r.status = 'PENDING'
                    """)) {
                statement.setLong(1, requestId);
                try (ResultSet rows = statement.executeQuery()) {
                    if (!rows.next()) { connection.rollback(); return TerritoryDecision.NOT_FOUND; }
                    request = readTerritoryRequest(rows);
                }
            }
            if (approve) {
                if ("ADD".equals(request.action())) {
                    if (hasForeignClaims(request.world(), request.minX(), request.minZ(), request.maxX(), request.maxZ(), request.nationId())) {
                        connection.rollback();
                        return TerritoryDecision.CONFLICT;
                    }
                    insertTerritoryRectangle(request.world(), request.minX(), request.minZ(),
                            request.maxX(), request.maxZ(), request.nationId(), false);
                } else {
                    deleteTerritoryRectangle(request.world(), request.minX(), request.minZ(),
                            request.maxX(), request.maxZ(), request.nationId());
                }
            }
            try (PreparedStatement decision = connection.prepareStatement("""
                    UPDATE territory_requests SET status = ?, reviewed_by = ?, reviewed_at = CURRENT_TIMESTAMP
                    WHERE id = ? AND status = 'PENDING'
                    """)) {
                decision.setString(1, approve ? "APPROVED" : "REJECTED");
                decision.setString(2, reviewer);
                decision.setLong(3, requestId);
                if (decision.executeUpdate() != 1) throw new SQLException("Territory request concurrently changed");
            }
            addTerritoryAudit(reviewer, approve ? "APPROVE_" + request.action() : "REJECT_" + request.action(),
                    request.nationId(), request.world(), request.minX(), request.minZ(), request.maxX(), request.maxZ());
            connection.commit();
            return approve ? TerritoryDecision.APPROVED : TerritoryDecision.REJECTED;
        } catch (SQLException | RuntimeException error) {
            rollbackAfter(connection, error);
            throw error;
        } finally {
            connection.setAutoCommit(true);
        }
    }

    public TerritoryDecision cancelTerritoryRequest(long id, UUID actor) throws SQLException {
        Optional<Membership> membership = findMembership(actor);
        if (membership.isEmpty() || !isManager(membership.get().role())) return TerritoryDecision.NOT_AUTHORIZED;
        try (PreparedStatement statement = connection.prepareStatement("""
                UPDATE territory_requests
                SET status = 'CANCELLED', reviewed_by = ?, reviewed_at = CURRENT_TIMESTAMP
                WHERE id = ? AND status = 'PENDING' AND nation_id = ?
                """)) {
            statement.setString(1, actor.toString());
            statement.setLong(2, id);
            statement.setString(3, membership.get().nationId());
            return statement.executeUpdate() == 1 ? TerritoryDecision.CANCELLED : TerritoryDecision.NOT_FOUND;
        }
    }

    private boolean hasForeignClaims(String world, int minX, int minZ, int maxX, int maxZ,
                                     String nationId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT 1 FROM territory_chunks
                WHERE world_name = ? AND chunk_x BETWEEN ? AND ? AND chunk_z BETWEEN ? AND ?
                  AND nation_id <> ? LIMIT 1
                """)) {
            statement.setString(1, world);
            statement.setInt(2, minX);
            statement.setInt(3, maxX);
            statement.setInt(4, minZ);
            statement.setInt(5, maxZ);
            statement.setString(6, nationId);
            try (ResultSet rows = statement.executeQuery()) {
                return rows.next();
            }
        }
    }

    private void insertTerritoryRectangle(String world, int minX, int minZ, int maxX, int maxZ,
                                          String nationId, boolean override) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(override ? """
                INSERT INTO territory_chunks(world_name, chunk_x, chunk_z, nation_id)
                VALUES (?, ?, ?, ?)
                ON CONFLICT(world_name, chunk_x, chunk_z) DO UPDATE SET nation_id = excluded.nation_id,
                    claimed_at = CURRENT_TIMESTAMP
                """ : """
                INSERT OR IGNORE INTO territory_chunks(world_name, chunk_x, chunk_z, nation_id)
                VALUES (?, ?, ?, ?)
                """)) {
            for (int x = minX; x <= maxX; x++) {
                for (int z = minZ; z <= maxZ; z++) {
                    statement.setString(1, world);
                    statement.setInt(2, x);
                    statement.setInt(3, z);
                    statement.setString(4, nationId);
                    statement.addBatch();
                }
            }
            statement.executeBatch();
        }
    }

    private void deleteTerritoryRectangle(String world, int minX, int minZ, int maxX, int maxZ,
                                          String nationIdOrNull) throws SQLException {
        String sql = "DELETE FROM territory_chunks WHERE world_name = ? AND chunk_x BETWEEN ? AND ? "
                + "AND chunk_z BETWEEN ? AND ?"
                + (nationIdOrNull == null ? "" : " AND nation_id = ?");
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, world);
            statement.setInt(2, minX);
            statement.setInt(3, maxX);
            statement.setInt(4, minZ);
            statement.setInt(5, maxZ);
            if (nationIdOrNull != null) statement.setString(6, nationIdOrNull);
            statement.executeUpdate();
        }
    }

    /** Full admin override, including reassignment of already-claimed chunks. */
    public boolean adminAssignTerritory(String actor, String nationName, String world,
                                        int minX, int minZ, int maxX, int maxZ) throws SQLException {
        Optional<String> nationId = findNationIdByName(nationName);
        if (nationId.isEmpty()) return false;
        connection.setAutoCommit(false);
        try {
            insertTerritoryRectangle(world, minX, minZ, maxX, maxZ, nationId.get(), true);
            addTerritoryAudit(actor, "ADMIN_ASSIGN", nationId.get(), world, minX, minZ, maxX, maxZ);
            connection.commit();
            return true;
        } catch (SQLException | RuntimeException error) {
            rollbackAfter(connection, error);
            throw error;
        } finally {
            connection.setAutoCommit(true);
        }
    }

    public void adminClearTerritory(String actor, String world, int minX, int minZ, int maxX, int maxZ)
            throws SQLException {
        connection.setAutoCommit(false);
        try {
            deleteTerritoryRectangle(world, minX, minZ, maxX, maxZ, null);
            addTerritoryAudit(actor, "ADMIN_CLEAR", null, world, minX, minZ, maxX, maxZ);
            connection.commit();
        } catch (SQLException | RuntimeException error) {
            rollbackAfter(connection, error);
            throw error;
        } finally {
            connection.setAutoCommit(true);
        }
    }

    private void addTerritoryAudit(String actor, String action, String nationId, String world,
                                   int minX, int minZ, int maxX, int maxZ) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                INSERT INTO territory_audit(actor, action, nation_id, world_name, min_x, min_z, max_x, max_z)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """)) {
            statement.setString(1, actor);
            statement.setString(2, action);
            statement.setString(3, nationId);
            statement.setString(4, world);
            statement.setInt(5, minX);
            statement.setInt(6, minZ);
            statement.setInt(7, maxX);
            statement.setInt(8, maxZ);
            statement.executeUpdate();
        }
    }

    public long countClaimedChunks(String nationId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT COUNT(*) FROM territory_chunks WHERE nation_id = ?")) {
            statement.setString(1, nationId);
            try (ResultSet rows = statement.executeQuery()) {
                rows.next(); return rows.getLong(1);
            }
        }
    }


    /** Map export: public political claims only. No player coordinates, cores or protected-base locations. */
    public record TerritoryMapChunk(String nationId, String nationName, int chunkX, int chunkZ) {}

    public List<TerritoryMapChunk> territoryMapChunks(String worldName) throws SQLException {
        List<TerritoryMapChunk> chunks = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT n.id, n.name, c.chunk_x, c.chunk_z
                FROM territory_chunks c JOIN nations n ON n.id = c.nation_id
                WHERE c.world_name = ? ORDER BY n.id, c.chunk_z, c.chunk_x
                """)) {
            statement.setString(1, worldName);
            try (ResultSet rows = statement.executeQuery()) {
                while (rows.next()) {
                    chunks.add(new TerritoryMapChunk(rows.getString(1), rows.getString(2),
                            rows.getInt(3), rows.getInt(4)));
                }
            }
        }
        return chunks;
    }


    // Protection cores are deliberately stored separately from political territory.
    // Public squaremap exports MUST NEVER query or expose these coordinates.
    public record ProtectionCore(String id, String nationId, String nationName,
                                 String world, int x, int y, int z, boolean capital) {
        public boolean contains(int blockX, int blockZ) {
            return Math.abs((long) blockX - x) <= 32 && Math.abs((long) blockZ - z) <= 32;
        }
        public boolean isAnchor(int blockX, int blockY, int blockZ) {
            return x == blockX && y == blockY && z == blockZ;
        }
    }

    public enum CorePlacement { PLACED, NO_NATION, NO_CLAIM, ALREADY_HAS_FREE_CORE, OVERLAP, AT_WAR }

    public List<ProtectionCore> allProtectionCores() throws SQLException {
        List<ProtectionCore> result = new ArrayList<>();
        try (PreparedStatement query = connection.prepareStatement("""
                SELECT c.id, c.nation_id, n.name, c.world_name,
                       c.block_x, c.block_y, c.block_z, c.is_capital
                FROM protection_cores c JOIN nations n ON n.id = c.nation_id
                ORDER BY c.placed_at, c.id
                """); ResultSet rows = query.executeQuery()) {
            while (rows.next()) {
                result.add(new ProtectionCore(rows.getString(1), rows.getString(2), rows.getString(3),
                        rows.getString(4), rows.getInt(5), rows.getInt(6), rows.getInt(7), rows.getInt(8) == 1));
            }
        }
        return result;
    }

    /** Admin-only testing: one initial FREE core per nation, no recipe and no conquest yet. */
    public CorePlacement placeInitialCore(UUID actor, String world, int x, int y, int z) throws SQLException {
        connection.setAutoCommit(false);
        try {
            Optional<Membership> owner = findMembership(actor);
            if (owner.isEmpty()) { connection.rollback(); return CorePlacement.NO_NATION; }
            String nationId = owner.get().nationId();
            if (nationAtWar(nationId)) { connection.rollback(); return CorePlacement.AT_WAR; }
            Optional<TerritoryOwner> territory = findTerritory(world, Math.floorDiv(x, 16), Math.floorDiv(z, 16));
            if (territory.isEmpty() || !nationId.equals(territory.get().nationId())) {
                connection.rollback(); return CorePlacement.NO_CLAIM;
            }
            try (PreparedStatement query = connection.prepareStatement("""
                    SELECT 1 FROM core_tokens WHERE nation_id = ? AND origin = 'FREE' LIMIT 1
                    """)) {
                query.setString(1, nationId);
                try (ResultSet rows = query.executeQuery()) {
                    if (rows.next()) { connection.rollback(); return CorePlacement.ALREADY_HAS_FREE_CORE; }
                }
            }
            try (PreparedStatement query = connection.prepareStatement("""
                    SELECT 1 FROM protection_cores WHERE world_name = ?
                      AND block_x BETWEEN ? AND ? AND block_z BETWEEN ? AND ? LIMIT 1
                    """)) {
                query.setString(1, world);
                query.setLong(2, (long)x - 64L);
                query.setLong(3, (long)x + 64L);
                query.setLong(4, (long)z - 64L);
                query.setLong(5, (long)z + 64L);
                try (ResultSet rows = query.executeQuery()) {
                    if (rows.next()) { connection.rollback(); return CorePlacement.OVERLAP; }
                }
            }
            boolean capital;
            try (PreparedStatement query = connection.prepareStatement("""
                    SELECT 1 FROM protection_cores WHERE nation_id = ? AND is_capital = 1 LIMIT 1
                    """)) {
                query.setString(1, nationId);
                try (ResultSet rows = query.executeQuery()) { capital = !rows.next(); }
            }
            String coreId = UUID.randomUUID().toString();
            try (PreparedStatement insert = connection.prepareStatement("""
                    INSERT INTO protection_cores (id, nation_id, world_name, block_x, block_y, block_z,
                                                  origin, is_capital, placed_by)
                    VALUES (?, ?, ?, ?, ?, ?, 'FREE', ?, ?)
                    """)) {
                insert.setString(1, coreId);
                insert.setString(2, nationId);
                insert.setString(3, world);
                insert.setInt(4, x); insert.setInt(5, y); insert.setInt(6, z);
                insert.setInt(7, capital ? 1 : 0);
                insert.setString(8, actor.toString());
                insert.executeUpdate();
            }
            try (PreparedStatement token = connection.prepareStatement("""
                    INSERT INTO core_tokens (id, nation_id, origin, state)
                    VALUES (?, ?, 'FREE', 'PLACED')
                    """)) {
                token.setString(1, coreId);
                token.setString(2, nationId);
                token.executeUpdate();
            }
            connection.commit();
            return CorePlacement.PLACED;
        } catch (SQLException | RuntimeException error) {
            rollbackAfter(connection, error);
            throw error;
        } finally { connection.setAutoCommit(true); }
    }

    public boolean adminRemoveCore(String coreId) throws SQLException {
        connection.setAutoCommit(false);
        try {
            int removed;
            try (PreparedStatement statement = connection.prepareStatement("DELETE FROM protection_cores WHERE id = ?")) {
                statement.setString(1, coreId);
                removed = statement.executeUpdate();
            }
            if (removed == 1) {
                try (PreparedStatement statement = connection.prepareStatement("DELETE FROM core_tokens WHERE id = ?")) {
                    statement.setString(1, coreId);
                    statement.executeUpdate();
                }
            }
            connection.commit();
            return removed == 1;
        } catch (SQLException | RuntimeException error) {
            rollbackAfter(connection, error);
            throw error;
        } finally { connection.setAutoCommit(true); }
    }

    public enum CoreIssueStatus { ISSUED, NOT_AUTHORIZED, ALREADY_FREE, CRAFT_LIMIT }
    public record CoreIssue(CoreIssueStatus status, String tokenId, String nationId, String origin) {}
    public record CoreStock(int free, int crafted, int placed) {}
    public enum TokenPlaceResult { PLACED, INVALID_TOKEN, ALREADY_PLACED, NOT_AUTHORIZED, NO_CLAIM, OVERLAP, AT_WAR }

    public CoreStock coreStock(UUID player) throws SQLException {
        Optional<Membership> member = findMembership(player);
        if (member.isEmpty()) return new CoreStock(0, 0, 0);
        try (PreparedStatement query = connection.prepareStatement("""
                SELECT COALESCE(SUM(CASE WHEN origin='FREE' THEN 1 ELSE 0 END),0),
                       COALESCE(SUM(CASE WHEN origin='CRAFTED' THEN 1 ELSE 0 END),0),
                       COALESCE(SUM(CASE WHEN state='PLACED' THEN 1 ELSE 0 END),0)
                FROM core_tokens WHERE nation_id=?
                """)) {
            query.setString(1, member.get().nationId());
            try (ResultSet rows = query.executeQuery()) {
                rows.next();
                return new CoreStock(rows.getInt(1), rows.getInt(2), rows.getInt(3));
            }
        }
    }

    public boolean canCraftCore(UUID actor) throws SQLException {
        Optional<Membership> member = findMembership(actor);
        if (member.isEmpty() || !isManager(member.get().role())) return false;
        try (PreparedStatement query = connection.prepareStatement(
                "SELECT COUNT(*) FROM core_tokens WHERE nation_id=? AND origin='CRAFTED'")) {
            query.setString(1, member.get().nationId());
            try (ResultSet rows = query.executeQuery()) {
                rows.next();
                return rows.getInt(1) < 2;
            }
        }
    }

    /** Tokens are nation-bound and registered before giving players an item. */
    public CoreIssue issueCoreToken(UUID actor, boolean free) throws SQLException {
        connection.setAutoCommit(false);
        try {
            Optional<Membership> member = findMembership(actor);
            if (member.isEmpty() || !isManager(member.get().role())) {
                connection.rollback();
                return new CoreIssue(CoreIssueStatus.NOT_AUTHORIZED, null, null, null);
            }
            String nationId = member.get().nationId();
            String origin = free ? "FREE" : "CRAFTED";
            try (PreparedStatement query = connection.prepareStatement(
                    "SELECT COUNT(*) FROM core_tokens WHERE nation_id=? AND origin=?")) {
                query.setString(1, nationId);
                query.setString(2, origin);
                try (ResultSet rows = query.executeQuery()) {
                    rows.next();
                    int count = rows.getInt(1);
                    if ((free && count >= 1) || (!free && count >= 2)) {
                        connection.rollback();
                        return new CoreIssue(free ? CoreIssueStatus.ALREADY_FREE : CoreIssueStatus.CRAFT_LIMIT,
                                null, null, null);
                    }
                }
            }
            String tokenId = UUID.randomUUID().toString();
            try (PreparedStatement insert = connection.prepareStatement("""
                    INSERT INTO core_tokens (id, nation_id, origin, state)
                    VALUES (?, ?, ?, 'ITEM')
                    """)) {
                insert.setString(1, tokenId);
                insert.setString(2, nationId);
                insert.setString(3, origin);
                insert.executeUpdate();
            }
            connection.commit();
            return new CoreIssue(CoreIssueStatus.ISSUED, tokenId, nationId, origin);
        } catch (SQLException | RuntimeException error) {
            rollbackAfter(connection, error);
            throw error;
        } finally { connection.setAutoCommit(true); }
    }

    /** For exceptional failure to deliver an item to its recipient. */
    public void revokeUndeliveredCore(String tokenId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "DELETE FROM core_tokens WHERE id=? AND state='ITEM'")) {
            statement.setString(1, tokenId);
            statement.executeUpdate();
        }
    }

    /** Each token can be placed at most once, even if an item stack was duplicated. */
    public TokenPlaceResult placeCoreToken(UUID actor, String tokenId, String itemNation, String world,
                                           int x, int y, int z) throws SQLException {
        connection.setAutoCommit(false);
        try {
            Optional<Membership> actorMember = findMembership(actor);
            if (actorMember.isEmpty() || !isManager(actorMember.get().role()) ||
                    !actorMember.get().nationId().equals(itemNation)) {
                connection.rollback(); return TokenPlaceResult.NOT_AUTHORIZED;
            }
            if (nationAtWar(itemNation)) { connection.rollback(); return TokenPlaceResult.AT_WAR; }
            String origin;
            String state;
            try (PreparedStatement query = connection.prepareStatement(
                    "SELECT origin, state FROM core_tokens WHERE id=? AND nation_id=?")) {
                query.setString(1, tokenId);
                query.setString(2, itemNation);
                try (ResultSet rows = query.executeQuery()) {
                    if (!rows.next()) { connection.rollback(); return TokenPlaceResult.INVALID_TOKEN; }
                    origin = rows.getString(1); state = rows.getString(2);
                }
            }
            if (!state.equals("ITEM")) { connection.rollback(); return TokenPlaceResult.ALREADY_PLACED; }
            Optional<TerritoryOwner> territory = findTerritory(world, Math.floorDiv(x,16), Math.floorDiv(z,16));
            if (territory.isEmpty() || !territory.get().nationId().equals(itemNation)) {
                connection.rollback(); return TokenPlaceResult.NO_CLAIM;
            }
            try (PreparedStatement query = connection.prepareStatement("""
                    SELECT 1 FROM protection_cores WHERE world_name=?
                      AND block_x BETWEEN ? AND ? AND block_z BETWEEN ? AND ? LIMIT 1
                    """)) {
                query.setString(1, world);
                query.setLong(2, (long)x-64); query.setLong(3, (long)x+64);
                query.setLong(4, (long)z-64); query.setLong(5, (long)z+64);
                try (ResultSet rows = query.executeQuery()) {
                    if (rows.next()) { connection.rollback(); return TokenPlaceResult.OVERLAP; }
                }
            }
            boolean capital;
            try (PreparedStatement query = connection.prepareStatement(
                    "SELECT 1 FROM protection_cores WHERE nation_id=? AND is_capital=1 LIMIT 1")) {
                query.setString(1, itemNation);
                try (ResultSet rows = query.executeQuery()) { capital = !rows.next(); }
            }
            try (PreparedStatement insert = connection.prepareStatement("""
                    INSERT INTO protection_cores
                      (id,nation_id,world_name,block_x,block_y,block_z,origin,is_capital,placed_by)
                    VALUES (?,?,?,?,?,?,?,?,?)
                    """)) {
                insert.setString(1,tokenId); insert.setString(2,itemNation); insert.setString(3,world);
                insert.setInt(4,x); insert.setInt(5,y); insert.setInt(6,z);
                insert.setString(7,origin); insert.setInt(8,capital ? 1 : 0);
                insert.setString(9,actor.toString()); insert.executeUpdate();
            }
            try (PreparedStatement statement = connection.prepareStatement(
                    "UPDATE core_tokens SET state='PLACED' WHERE id=? AND state='ITEM'")) {
                statement.setString(1, tokenId);
                if (statement.executeUpdate() != 1) throw new SQLException("Token state changed while placing");
            }
            connection.commit();
            return TokenPlaceResult.PLACED;
        } catch (SQLException | RuntimeException error) {
            rollbackAfter(connection, error);
            throw error;
        } finally { connection.setAutoCommit(true); }
    }

    // v1.2: relocating an own core reuses its existing token ID and does not consume another slot.
    // All operations run on Paper's primary thread; SQLite state transitions are transactional.
    public enum CorePickupStatus {
        PICKED_UP, NOT_FOUND, NOT_AUTHORIZED, CAPITAL, CONQUERED, TOKEN_MISMATCH, AT_WAR
    }
    public record CorePickup(CorePickupStatus status, String tokenId, String nationId, String origin) {}

    public CorePickup pickupOwnCore(UUID actor, String coreId) throws SQLException {
        connection.setAutoCommit(false);
        try {
            Optional<Membership> member = findMembership(actor);
            if (member.isEmpty() || !isManager(member.get().role())) {
                connection.rollback();
                return new CorePickup(CorePickupStatus.NOT_AUTHORIZED, null, null, null);
            }
            String nationId, origin;
            boolean capital;
            try (PreparedStatement query = connection.prepareStatement("""
                    SELECT nation_id, origin, is_capital FROM protection_cores WHERE id=?
                    """)) {
                query.setString(1, coreId);
                try (ResultSet rows = query.executeQuery()) {
                    if (!rows.next()) {
                        connection.rollback();
                        return new CorePickup(CorePickupStatus.NOT_FOUND, null, null, null);
                    }
                    nationId = rows.getString(1);
                    origin = rows.getString(2);
                    capital = rows.getInt(3) == 1;
                }
            }
            if (!nationId.equals(member.get().nationId())) {
                connection.rollback();
                return new CorePickup(CorePickupStatus.NOT_AUTHORIZED, null, null, null);
            }
            if (nationAtWar(nationId)) {
                connection.rollback();
                return new CorePickup(CorePickupStatus.AT_WAR, null, null, null);
            }
            if (origin.equals("CONQUERED")) {
                connection.rollback();
                return new CorePickup(CorePickupStatus.CONQUERED, null, null, null);
            }
            if (capital) {
                connection.rollback();
                return new CorePickup(CorePickupStatus.CAPITAL, null, null, null);
            }
            try (PreparedStatement query = connection.prepareStatement("""
                    SELECT 1 FROM core_tokens
                    WHERE id=? AND nation_id=? AND origin=? AND state='PLACED'
                    """)) {
                query.setString(1, coreId);
                query.setString(2, nationId);
                query.setString(3, origin);
                try (ResultSet rows = query.executeQuery()) {
                    if (!rows.next()) {
                        connection.rollback();
                        return new CorePickup(CorePickupStatus.TOKEN_MISMATCH, null, null, null);
                    }
                }
            }
            try (PreparedStatement delete = connection.prepareStatement("""
                    DELETE FROM protection_cores WHERE id=? AND nation_id=? AND is_capital=0
                    """)) {
                delete.setString(1, coreId);
                delete.setString(2, nationId);
                if (delete.executeUpdate() != 1) throw new SQLException("Core vanished during pickup");
            }
            try (PreparedStatement update = connection.prepareStatement("""
                    UPDATE core_tokens SET state='ITEM'
                    WHERE id=? AND nation_id=? AND state='PLACED'
                    """)) {
                update.setString(1, coreId);
                update.setString(2, nationId);
                if (update.executeUpdate() != 1) throw new SQLException("Could not return core token to ITEM");
            }
            connection.commit();
            return new CorePickup(CorePickupStatus.PICKED_UP, coreId, nationId, origin);
        } catch (SQLException | RuntimeException error) {
            rollbackAfter(connection, error);
            throw error;
        } finally { connection.setAutoCommit(true); }
    }

    /** Compensation if Bukkit fails to deliver the item or remove the physical lodestone. */
    public void restorePickedUpCore(ProtectionCore previous, UUID actor) throws SQLException {
        connection.setAutoCommit(false);
        try {
            String origin;
            try (PreparedStatement query = connection.prepareStatement("""
                    SELECT origin FROM core_tokens WHERE id=? AND nation_id=? AND state='ITEM'
                    """)) {
                query.setString(1, previous.id());
                query.setString(2, previous.nationId());
                try (ResultSet rows = query.executeQuery()) {
                    if (!rows.next()) throw new SQLException("Cannot restore pickup: token not ITEM");
                    origin = rows.getString(1);
                }
            }
            try (PreparedStatement insert = connection.prepareStatement("""
                    INSERT INTO protection_cores
                    (id, nation_id, world_name, block_x, block_y, block_z, origin, is_capital, placed_by)
                    VALUES (?, ?, ?, ?, ?, ?, ?, 0, ?)
                    """)) {
                insert.setString(1, previous.id());
                insert.setString(2, previous.nationId());
                insert.setString(3, previous.world());
                insert.setInt(4, previous.x());
                insert.setInt(5, previous.y());
                insert.setInt(6, previous.z());
                insert.setString(7, origin);
                insert.setString(8, actor.toString());
                insert.executeUpdate();
            }
            try (PreparedStatement update = connection.prepareStatement("""
                    UPDATE core_tokens SET state='PLACED' WHERE id=? AND state='ITEM'
                    """)) {
                update.setString(1, previous.id());
                if (update.executeUpdate() != 1) throw new SQLException("Could not restore token PLACED state");
            }
            connection.commit();
        } catch (SQLException | RuntimeException error) {
            rollbackAfter(connection, error);
            throw error;
        } finally { connection.setAutoCommit(true); }
    }

    public enum SetCapitalResult { UPDATED, ALREADY_CAPITAL, NOT_FOUND }

    /** Admin-only: transfers capital status to a different PLACED core of the same nation. */
    public SetCapitalResult adminSetCapital(String coreId) throws SQLException {
        connection.setAutoCommit(false);
        try {
            String nationId;
            boolean already;
            try (PreparedStatement query = connection.prepareStatement("""
                    SELECT nation_id, is_capital FROM protection_cores WHERE id=?
                    """)) {
                query.setString(1, coreId);
                try (ResultSet rows = query.executeQuery()) {
                    if (!rows.next()) { connection.rollback(); return SetCapitalResult.NOT_FOUND; }
                    nationId = rows.getString(1);
                    already = rows.getInt(2) == 1;
                }
            }
            if (already) { connection.rollback(); return SetCapitalResult.ALREADY_CAPITAL; }
            try (PreparedStatement clear = connection.prepareStatement("""
                    UPDATE protection_cores SET is_capital=0 WHERE nation_id=? AND is_capital=1
                    """)) {
                clear.setString(1, nationId);
                clear.executeUpdate();
            }
            try (PreparedStatement set = connection.prepareStatement("""
                    UPDATE protection_cores SET is_capital=1 WHERE id=? AND nation_id=?
                    """)) {
                set.setString(1, coreId);
                set.setString(2, nationId);
                if (set.executeUpdate() != 1) throw new SQLException("Target capital disappeared");
            }
            connection.commit();
            return SetCapitalResult.UPDATED;
        } catch (SQLException | RuntimeException error) {
            rollbackAfter(connection, error);
            throw error;
        } finally { connection.setAutoCommit(true); }
    }

    /**
     * Atomically changes ownership after an eight-minute capture. The final
     * defender core is destroyed and the nation is dissolved instead.
     * Never call this merely because a war was declared: SiegeManager has to
     * validate the legal siege, war side, and the full capture counter first.
     */
    public enum CaptureOutcome { CONQUERED, LAST_CORE_DESTROYED, NOT_FOUND, INVALID_VICTOR }

    public CaptureOutcome captureCore(String coreId, String expectedDefender, String victorId)
            throws SQLException {
        connection.setAutoCommit(false);
        try {
            if (expectedDefender.equals(victorId)) {
                connection.rollback(); return CaptureOutcome.INVALID_VICTOR;
            }
            try (PreparedStatement q = connection.prepareStatement("SELECT 1 FROM nations WHERE id=?")) {
                q.setString(1, victorId);
                try (ResultSet r = q.executeQuery()) {
                    if (!r.next()) { connection.rollback(); return CaptureOutcome.INVALID_VICTOR; }
                }
            }
            try (PreparedStatement q = connection.prepareStatement(
                    "SELECT 1 FROM protection_cores WHERE id=? AND nation_id=?")) {
                q.setString(1, coreId); q.setString(2, expectedDefender);
                try (ResultSet r = q.executeQuery()) {
                    if (!r.next()) { connection.rollback(); return CaptureOutcome.NOT_FOUND; }
                }
            }
            int remaining;
            try (PreparedStatement q = connection.prepareStatement(
                    "SELECT COUNT(*) FROM protection_cores WHERE nation_id=?")) {
                q.setString(1, expectedDefender);
                try (ResultSet r = q.executeQuery()) { r.next(); remaining = r.getInt(1); }
            }
            if (remaining > 1) {
                // A captured core does not count against either nation's own crafting limit.
                // Its old FREE/CRAFTED token cannot stay usable by the former owner.
                try (PreparedStatement q = connection.prepareStatement("DELETE FROM core_tokens WHERE id=?")) {
                    q.setString(1, coreId); q.executeUpdate();
                }
                try (PreparedStatement q = connection.prepareStatement("""
                        UPDATE protection_cores SET nation_id=?,origin='CONQUERED',is_capital=0
                        WHERE id=? AND nation_id=?
                        """)) {
                    q.setString(1, victorId); q.setString(2, coreId);
                    q.setString(3, expectedDefender);
                    if (q.executeUpdate() != 1) throw new SQLException("Core changed during conquest");
                }
                // The defender must retain a capital if it still owns another core.
                // Prefer a non-conquered core; use the oldest placed core as a stable fallback.
                // This also repairs a missing capital left by an older conquest.
                try (PreparedStatement q = connection.prepareStatement("""
                        UPDATE protection_cores SET is_capital=1
                        WHERE id=(
                            SELECT id FROM protection_cores
                            WHERE nation_id=?
                            ORDER BY CASE WHEN origin='CONQUERED' THEN 1 ELSE 0 END, rowid ASC
                            LIMIT 1
                        )
                        AND NOT EXISTS (
                            SELECT 1 FROM protection_cores WHERE nation_id=? AND is_capital=1
                        )
                        """)) {
                    q.setString(1, expectedDefender);
                    q.setString(2, expectedDefender);
                    q.executeUpdate();
                }
                connection.commit();
                return CaptureOutcome.CONQUERED;
            }

            // Nation deletion must first clear references from the diplomacy schema.
            // The active principal wars cease; allied wars continue without this nation.
            try (PreparedStatement q = connection.prepareStatement(
                    "DELETE FROM wars WHERE attacker_id=? OR defender_id=?")) {
                q.setString(1, expectedDefender); q.setString(2, expectedDefender); q.executeUpdate();
            }
            try (PreparedStatement q = connection.prepareStatement(
                    "UPDATE wars SET peace_offered_by=NULL WHERE peace_offered_by=?")) {
                q.setString(1, expectedDefender); q.executeUpdate();
            }
            try (PreparedStatement q = connection.prepareStatement(
                    "DELETE FROM war_invitations WHERE nation_id=?")) {
                q.setString(1, expectedDefender); q.executeUpdate();
            }
            try (PreparedStatement q = connection.prepareStatement(
                    "DELETE FROM war_participants WHERE nation_id=?")) {
                q.setString(1, expectedDefender); q.executeUpdate();
            }
            try (PreparedStatement q = connection.prepareStatement(
                    "DELETE FROM nations WHERE id=?")) {
                q.setString(1, expectedDefender);
                if (q.executeUpdate() != 1) throw new SQLException("Defending nation vanished");
            }
            // CASCADE invalidates stored core tokens and makes all political chunks neutral.
            connection.commit();
            return CaptureOutcome.LAST_CORE_DESTROYED;
        } catch (SQLException | RuntimeException error) {
            rollbackAfter(connection, error);
            throw error;
        } finally {
            connection.setAutoCommit(true);
        }
    }

    @Override
    public void close() throws SQLException {
        connection.close();
    }
}
