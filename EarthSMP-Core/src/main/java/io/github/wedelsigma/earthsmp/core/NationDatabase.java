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
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

/**
 * Small SQLite-backed repository for nations and memberships.
 * Called synchronously from the server thread in this development milestone.
 */
public final class NationDatabase implements AutoCloseable {

    // No schema changes since 0.2.0: existing databases are compatible.
    private static final int SCHEMA_VERSION = 1;
    private final Connection connection;

    public record Membership(String nationId, String nationName, String role) {}

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
                    statement.execute("PRAGMA user_version = " + SCHEMA_VERSION);
                    connection.commit();
                } catch (SQLException | RuntimeException error) {
                    connection.rollback();
                    throw error;
                } finally {
                    connection.setAutoCommit(true);
                }
            }
        }
    }

    public int countNations() throws SQLException {
        try (Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery("SELECT COUNT(*) FROM nations")) {
            result.next();
            return result.getInt(1);
        }
    }

    /** At most one membership row per player, guaranteed by the primary key. */
    public Optional<Membership> findMembership(UUID playerUuid) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT n.id, n.name, m.role
                FROM nation_members AS m
                JOIN nations AS n ON n.id = m.nation_id
                WHERE m.player_uuid = ?
                """)) {
            statement.setString(1, playerUuid.toString());
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) {
                    return Optional.empty();
                }
                return Optional.of(new Membership(result.getString(1), result.getString(2), result.getString(3)));
            }
        }
    }

    /**
     * Creates a nation and assigns its founder the LEADER role atomically.
     * Returns false if the player is already in a nation or the name is taken.
     */
    public boolean createNation(String name, UUID founderUuid) throws SQLException {
        connection.setAutoCommit(false);
        try {
            // Human-friendly duplicate check. SQLite NOCASE itself covers ASCII names.
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
            connection.commit();
            return true;
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

    private boolean isNameTaken(String proposed) throws SQLException {
        // Compare with Java case folding as SQLite NOCASE covers ASCII only.
        String proposedKey = proposed.toLowerCase(Locale.ROOT);
        try (Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery("SELECT name FROM nations")) {
            while (result.next()) {
                if (result.getString(1).toLowerCase(Locale.ROOT).equals(proposedKey)) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public void close() throws SQLException {
        connection.close();
    }
}
