package net.danh.storage.Database;

import net.danh.storage.Data.MythicTransferData;
import net.danh.storage.Storage;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

public class MythicTransferDatabase {
    private final String transferTable = "MythicTransferLogs";
    private final Storage plugin;
    private YMLMythicTransferStorage ymlStorage;
    private MySQLMythicTransferStorage mysqlStorage;

    public MythicTransferDatabase(Storage plugin) {
        this.plugin = plugin;

        // Initialize appropriate storage based on database type
        if (Storage.dataStorage instanceof MySQLAdapter) {
            mysqlStorage = new MySQLMythicTransferStorage(plugin);
        } else if (!(Storage.dataStorage instanceof SQLiteAdapter)) {
            ymlStorage = new YMLMythicTransferStorage(plugin);
        }
    }

    public void createTransferTable() {
        // Only create SQL table if using SQLite database
        if (mysqlStorage != null) {
            mysqlStorage.createTransferTable();
        } else if (Storage.dataStorage instanceof SQLiteAdapter) {
            Connection conn = null;
            PreparedStatement ps = null;
            try {
                conn = Storage.db.getSQLConnection();
                ps = conn.prepareStatement("CREATE TABLE IF NOT EXISTS " + transferTable + " (" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                        "sender TEXT NOT NULL," +
                        "receiver TEXT NOT NULL," +
                        "item_name TEXT NOT NULL," +
                        "amount INTEGER NOT NULL," +
                        "timestamp LONG NOT NULL," +
                        "status TEXT NOT NULL" +
                        ");");
                ps.executeUpdate();
            } catch (SQLException ex) {
                plugin.getLogger().log(Level.SEVERE, "Unable to create mythic transfer table", ex);
            } finally {
                try {
                    if (ps != null) ps.close();
                    if (conn != null) conn.close();
                } catch (SQLException ex) {
                    plugin.getLogger().log(Level.SEVERE, "Unable to close connection", ex);
                }
            }
        }
    }

    public void insertTransfer(MythicTransferData transferData) {
        if (mysqlStorage != null) {
            mysqlStorage.insertTransfer(transferData);
            return;
        } else if (ymlStorage != null) {
            ymlStorage.insertTransfer(transferData);
            return;
        }

        Connection conn = null;
        PreparedStatement ps = null;
        try {
            conn = Storage.db.getSQLConnection();
            ps = conn.prepareStatement("INSERT INTO " + transferTable +
                    " (sender, receiver, item_name, amount, timestamp, status) VALUES (?, ?, ?, ?, ?, ?)");
            ps.setString(1, transferData.getSender());
            ps.setString(2, transferData.getReceiver());
            ps.setString(3, transferData.getItemName());
            ps.setInt(4, transferData.getAmount());
            ps.setLong(5, transferData.getTimestamp());
            ps.setString(6, transferData.getStatus());
            ps.executeUpdate();
        } catch (SQLException ex) {
            plugin.getLogger().log(Level.SEVERE, "Unable to insert mythic transfer data", ex);
        } finally {
            try {
                if (ps != null) ps.close();
                if (conn != null) conn.close();
            } catch (SQLException ex) {
                plugin.getLogger().log(Level.SEVERE, "Unable to close connection", ex);
            }
        }
    }

    public List<MythicTransferData> getTransferHistory(String playerName, int limit) {
        return getTransferHistory(playerName, limit, 0);
    }

    public List<MythicTransferData> getTransferHistory(String playerName, int limit, int offset) {
        if (mysqlStorage != null) {
            return mysqlStorage.getTransferHistory(playerName, limit, offset);
        } else if (ymlStorage != null) {
            return ymlStorage.getTransferHistory(playerName, limit, offset);
        }

        List<MythicTransferData> transfers = new ArrayList<>();
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = Storage.db.getSQLConnection();
            ps = conn.prepareStatement("SELECT * FROM " + transferTable +
                    " WHERE sender = ? OR receiver = ? ORDER BY timestamp DESC LIMIT ? OFFSET ?");
            ps.setString(1, playerName);
            ps.setString(2, playerName);
            ps.setInt(3, limit);
            ps.setInt(4, offset);
            rs = ps.executeQuery();

            while (rs.next()) {
                MythicTransferData transfer = new MythicTransferData(
                        rs.getInt("id"),
                        rs.getString("sender"),
                        rs.getString("receiver"),
                        rs.getString("item_name"),
                        rs.getInt("amount"),
                        rs.getLong("timestamp"),
                        rs.getString("status")
                );
                transfers.add(transfer);
            }
        } catch (SQLException ex) {
            plugin.getLogger().log(Level.SEVERE, "Unable to get mythic transfer history", ex);
        } finally {
            try {
                if (rs != null) rs.close();
                if (ps != null) ps.close();
                if (conn != null) conn.close();
            } catch (SQLException ex) {
                plugin.getLogger().log(Level.SEVERE, "Unable to close connection", ex);
            }
        }
        return transfers;
    }

    public int getTotalTransferCount(String playerName) {
        if (mysqlStorage != null) {
            return mysqlStorage.getTotalTransferCount(playerName);
        } else if (ymlStorage != null) {
            return ymlStorage.getTotalTransferCount(playerName);
        }

        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = Storage.db.getSQLConnection();
            ps = conn.prepareStatement("SELECT COUNT(*) FROM " + transferTable +
                    " WHERE sender = ? OR receiver = ?");
            ps.setString(1, playerName);
            ps.setString(2, playerName);
            rs = ps.executeQuery();

            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException ex) {
            plugin.getLogger().log(Level.SEVERE, "Unable to get mythic transfer count", ex);
        } finally {
            try {
                if (rs != null) rs.close();
                if (ps != null) ps.close();
                if (conn != null) conn.close();
            } catch (SQLException ex) {
                plugin.getLogger().log(Level.SEVERE, "Unable to close connection", ex);
            }
        }
        return 0;
    }
}
