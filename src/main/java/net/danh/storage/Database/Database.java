package net.danh.storage.Database;

import net.danh.storage.Storage;
import org.jetbrains.annotations.NotNull;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Level;


public abstract class Database {
    public String table = "PlayerData";
    Storage main;
    Connection connection;

    public Database(Storage instance) {
        main = instance;
    }

    public abstract Connection getSQLConnection();

    public abstract void load();

    public void initialize() {
        connection = getSQLConnection();
        try {
            PreparedStatement ps = connection.prepareStatement("SELECT * FROM " + table + " LIMIT 1");
            ResultSet rs = ps.executeQuery();
            close(ps, rs);

        } catch (SQLException ex) {
            Storage.getStorage().getLogger().log(Level.SEVERE, "Unable to retrieve connection", ex);
        }
    }

    // These are the methods you can use to get things out of your database. You of course can make new ones to return different things in the database.
    // This returns the number of people the player killed.
    public PlayerData getData(String player) {
        try (Connection conn = getSQLConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT * FROM " + table + " WHERE player = ?")) {
            ps.setString(1, player);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                boolean autoPickup = false;
                try {
                    autoPickup = rs.getBoolean("autopickup");
                } catch (SQLException ignored) {
                }
                return new PlayerData(rs.getString("player"), rs.getString("data"), rs.getInt("max"), autoPickup);
            }
        } catch (SQLException ex) {
            Storage.getStorage().getLogger().log(Level.SEVERE, Errors.sqlConnectionExecute(), ex);
            return null;
        }
    }

    public void createTable(@NotNull PlayerData playerData) {
        try (Connection conn = getSQLConnection();
             PreparedStatement ps = conn.prepareStatement("INSERT INTO " + table + " (player,data,max,autopickup) VALUES(?,?,?,?)")) {
            ps.setString(1, playerData.player());
            ps.setString(2, playerData.data());
            ps.setInt(3, playerData.max());
            ps.setBoolean(4, playerData.autoPickup());
            ps.executeUpdate();
        } catch (SQLException ex) {
            Storage.getStorage().getLogger().log(Level.SEVERE, Errors.sqlConnectionExecute(), ex);
        }
    }

    public void updateTable(@NotNull PlayerData playerData) {
        Connection conn = null;
        try {
            conn = getSQLConnection();
            conn.setAutoCommit(false);
            try (PreparedStatement ps = conn.prepareStatement(
                    "UPDATE " + table + " SET data = ?, max = ?, autopickup = ? WHERE player = ?")) {
                ps.setString(1, playerData.data());
                ps.setInt(2, playerData.max());
                ps.setBoolean(3, playerData.autoPickup());
                ps.setString(4, playerData.player());
                ps.executeUpdate();
            }
            conn.commit();
        } catch (SQLException ex) {
            Storage.getStorage().getLogger().log(Level.SEVERE, Errors.sqlConnectionExecute(), ex);
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException ignored) {
                }
            }
        } finally {
            if (conn != null) {
                try {
                    conn.close();
                } catch (SQLException ex) {
                    Storage.getStorage().getLogger().log(Level.SEVERE, Errors.sqlConnectionClose(), ex);
                }
            }
        }
    }

    public void deleteData(String player) {
        try (Connection conn = getSQLConnection();
             PreparedStatement ps = conn.prepareStatement("DELETE FROM " + table + " WHERE player = ?")) {
            ps.setString(1, player);
            ps.executeUpdate();
        } catch (SQLException ex) {
            Storage.getStorage().getLogger().log(Level.SEVERE, Errors.sqlConnectionExecute(), ex);
        }
    }

    public void close(PreparedStatement ps, ResultSet rs) {
        try {
            if (ps != null) ps.close();
            if (rs != null) rs.close();
        } catch (SQLException ex) {
            Error.close(Storage.getStorage(), ex);
        }
    }
}
