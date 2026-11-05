package io.github.wedelsigma.earthsmp.core;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.UUID;

/** Persistent, main-thread-only storage shared with NationDatabase and its backups. */
public final class SiegeStorage {
    private final Connection connection;

    public SiegeStorage(NationDatabase nations) {
        this.connection = nations.diplomacyConnection();
    }

    public void markPlaced(String world, int x, int y, int z, String material) throws SQLException {
        try (PreparedStatement stmt = connection.prepareStatement("""
                INSERT INTO player_placed_blocks(world_name,block_x,block_y,block_z,block_type)
                VALUES (?,?,?,?,?)
                ON CONFLICT(world_name,block_x,block_y,block_z)
                DO UPDATE SET block_type=excluded.block_type
                """)) {
            stmt.setString(1,world);stmt.setInt(2,x);stmt.setInt(3,y);stmt.setInt(4,z);
            stmt.setString(5,material);stmt.executeUpdate();
        }
    }

    /** A stored type mismatch is treated as natural/unknown terrain, never as player-built. */
    public boolean isPlayerPlaced(String world, int x, int y, int z, String material) throws SQLException {
        try (PreparedStatement stmt = connection.prepareStatement("""
                SELECT block_type FROM player_placed_blocks
                WHERE world_name=? AND block_x=? AND block_y=? AND block_z=?
                """)) {
            stmt.setString(1,world);stmt.setInt(2,x);stmt.setInt(3,y);stmt.setInt(4,z);
            try(ResultSet rows=stmt.executeQuery()) {
                return rows.next() && material.equals(rows.getString(1));
            }
        }
    }

    public void forget(String world, int x, int y, int z) throws SQLException {
        try (PreparedStatement stmt=connection.prepareStatement("""
                DELETE FROM player_placed_blocks
                WHERE world_name=? AND block_x=? AND block_y=? AND block_z=?
                """)) {
            stmt.setString(1,world);stmt.setInt(2,x);stmt.setInt(3,y);stmt.setInt(4,z);
            stmt.executeUpdate();
        }
    }

    /** The immutable ID makes copies of a pickaxe share the same 15-use allowance. */
    public String issuePickaxe() throws SQLException {
        String id=UUID.randomUUID().toString();
        try(PreparedStatement stmt=connection.prepareStatement("""
                INSERT INTO siege_pickaxes(id,remaining,issued_at) VALUES (?,15,?)
                """)) {
            stmt.setString(1,id);stmt.setLong(2,Instant.now().getEpochSecond());stmt.executeUpdate();
        }
        return id;
    }

    public void revokePickaxe(String id) throws SQLException {
        try(PreparedStatement stmt=connection.prepareStatement("DELETE FROM siege_pickaxes WHERE id=? AND remaining=15")) {
            stmt.setString(1,id);stmt.executeUpdate();
        }
    }

    public int usesRemaining(String id) throws SQLException {
        try(PreparedStatement stmt=connection.prepareStatement("SELECT remaining FROM siege_pickaxes WHERE id=?")) {
            stmt.setString(1,id);
            try(ResultSet rows=stmt.executeQuery()) { return rows.next()?rows.getInt(1):0; }
        }
    }

    /** Returns the new count, or -1 if invalid/fully spent. */
    public int usePickaxe(String id) throws SQLException {
        try(PreparedStatement stmt=connection.prepareStatement("""
                UPDATE siege_pickaxes SET remaining=remaining-1 WHERE id=? AND remaining>0
                """)) {
            stmt.setString(1,id);
            if(stmt.executeUpdate()!=1) return -1;
        }
        return usesRemaining(id);
    }
}
