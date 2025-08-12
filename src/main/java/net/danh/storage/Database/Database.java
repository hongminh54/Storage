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
            PreparedStatement ps = connection.prepareStatement("SELECT * FROM " + table + " WHERE player = ?");
            ResultSet rs = ps.executeQuery();
            close(ps, rs);

        } catch (SQLException ex) {
            Storage.getStorage().getLogger().log(Level.SEVERE, "Unable to retrieve connection", ex);
        }
    }

    // These are the methods you can use to get things out of your database. You of course can make new ones to return different things in the database.
    // This returns the number of people the player killed.
    public PlayerData getData(String player) {
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs;
        try {
            conn = getSQLConnection();
            ps = conn.prepareStatement("SELECT * FROM " + table + " WHERE player = '" + player + "';");
            rs = ps.executeQuery();
            if (rs.next()) {
                boolean autoPickup = true;
                try {
                    autoPickup = rs.getBoolean("autopickup");
                } catch (SQLException e) {
                    autoPickup = true;
                }
                return new PlayerData(rs.getString("player"), rs.getString("data"), rs.getInt("max"), autoPickup);
            }
        } catch (SQLException ex) {
            Storage.getStorage().getLogger().log(Level.SEVERE, Errors.sqlConnectionExecute(), ex);
        } finally {
            try {
                if (ps != null) ps.close();
                if (conn != null) conn.close();
            } catch (SQLException ex) {
                Storage.getStorage().getLogger().log(Level.SEVERE, Errors.sqlConnectionClose(), ex);
            }
        }
        return null;
    }

    public void createTable(@NotNull PlayerData playerData) {
        Connection conn = null;
        PreparedStatement ps = null;
        try {
            conn = getSQLConnection();
            ps = conn.prepareStatement("INSERT INTO " + table + " (player,data,max,autopickup) VALUES(?,?,?,?)");
            ps.setString(1, playerData.getPlayer());
            ps.setString(2, playerData.getData());
            ps.setInt(3, playerData.getMax());
            ps.setBoolean(4, playerData.isAutoPickup());
            ps.executeUpdate();
        } catch (SQLException ex) {
            Storage.getStorage().getLogger().log(Level.SEVERE, Errors.sqlConnectionExecute(), ex);
        } finally {
            try {
                if (ps != null)
                    ps.close();
                if (conn != null)
                    conn.close();
            } catch (SQLException ex) {
                Storage.getStorage().getLogger().log(Level.SEVERE, Errors.sqlConnectionClose(), ex);
            }
        }
    }

    public void updateTable(@NotNull PlayerData playerData) {
        Connection conn = null;
        PreparedStatement ps = null;
        try {
            conn = getSQLConnection();
            ps = conn.prepareStatement("UPDATE " + table + " SET data = ?, max = ?, autopickup = ? " +
                    "WHERE player = ?");
            conn.setAutoCommit(false);
            ps.setString(1, playerData.getData());
            ps.setInt(2, playerData.getMax());
            ps.setBoolean(3, playerData.isAutoPickup());
            ps.setString(4, playerData.getPlayer());
            ps.addBatch();
            ps.executeBatch();
            conn.commit();
        } catch (SQLException ex) {
            Storage.getStorage().getLogger().log(Level.SEVERE, Errors.sqlConnectionExecute(), ex);
        } finally {
            try {
                if (ps != null)
                    ps.close();
                if (conn != null)
                    conn.close();
            } catch (SQLException ex) {
                Storage.getStorage().getLogger().log(Level.SEVERE, Errors.sqlConnectionClose(), ex);
            }
        }
    }

    public void deleteData(String player) {
        Connection conn = null;
        PreparedStatement ps = null;
        try {
            conn = getSQLConnection();
            ps = conn.prepareStatement("DELETE FROM " + table + " WHERE player = ?");
            ps.setString(1, player);
            ps.executeUpdate();
        } catch (SQLException ex) {
            Storage.getStorage().getLogger().log(Level.SEVERE, Errors.sqlConnectionExecute(), ex);
        } finally {
            try {
                if (ps != null)
                    ps.close();
                if (conn != null)
                    conn.close();
            } catch (SQLException ex) {
                Storage.getStorage().getLogger().log(Level.SEVERE, Errors.sqlConnectionClose(), ex);
            }
        }
    }

    public void updateAutoPickup(String player, boolean autoPickup) {
        Connection conn = null;
        PreparedStatement ps = null;
        try {
            conn = getSQLConnection();
            ps = conn.prepareStatement("UPDATE " + table + " SET autopickup = ? WHERE player = ?");
            ps.setBoolean(1, autoPickup);
            ps.setString(2, player);
            ps.executeUpdate();
        } catch (SQLException ex) {
            Storage.getStorage().getLogger().log(Level.SEVERE, Errors.sqlConnectionExecute(), ex);
        } finally {
            try {
                if (ps != null) ps.close();
                if (conn != null) conn.close();
            } catch (SQLException ex) {
                Storage.getStorage().getLogger().log(Level.SEVERE, Errors.sqlConnectionClose(), ex);
            }
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
