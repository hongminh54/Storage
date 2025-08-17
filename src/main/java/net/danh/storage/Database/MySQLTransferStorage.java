package net.danh.storage.Database;

import net.danh.storage.Data.TransferData;
import net.danh.storage.Storage;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

public class MySQLTransferStorage {

    private final String transferTable = "TransferLogs";
    private final Storage plugin;
    private final MySQL mysql;

    public MySQLTransferStorage(Storage plugin) {
        this.plugin = plugin;
        this.mysql = new MySQL(plugin);
        createTransferTable();
    }

    public void createTransferTable() {
        Connection conn = null;
        PreparedStatement ps = null;
        try {
            conn = mysql.getSQLConnection();
            ps = conn.prepareStatement("CREATE TABLE IF NOT EXISTS " + transferTable + " (" +
                    "id INTEGER PRIMARY KEY AUTO_INCREMENT," +
                    "sender VARCHAR(36) NOT NULL," +
                    "receiver VARCHAR(36) NOT NULL," +
                    "material VARCHAR(50) NOT NULL," +
                    "amount INTEGER NOT NULL," +
                    "timestamp BIGINT NOT NULL," +
                    "status VARCHAR(20) NOT NULL," +
                    "INDEX idx_sender (sender)," +
                    "INDEX idx_receiver (receiver)," +
                    "INDEX idx_timestamp (timestamp)" +
                    ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;");
            ps.executeUpdate();
        } catch (SQLException ex) {
            plugin.getLogger().log(Level.SEVERE, "Unable to create MySQL transfer table", ex);
        } finally {
            try {
                if (ps != null) ps.close();
                if (conn != null) conn.close();
            } catch (SQLException ex) {
                plugin.getLogger().log(Level.SEVERE, "Unable to close MySQL connection", ex);
            }
        }
    }

    public void insertTransfer(TransferData transferData) {
        Connection conn = null;
        PreparedStatement ps = null;
        try {
            conn = mysql.getSQLConnection();
            ps = conn.prepareStatement("INSERT INTO " + transferTable +
                    " (sender, receiver, material, amount, timestamp, status) VALUES (?, ?, ?, ?, ?, ?)");
            ps.setString(1, transferData.getSender());
            ps.setString(2, transferData.getReceiver());
            ps.setString(3, transferData.getMaterial());
            ps.setInt(4, transferData.getAmount());
            ps.setLong(5, transferData.getTimestamp());
            ps.setString(6, transferData.getStatus());
            ps.executeUpdate();
        } catch (SQLException ex) {
            plugin.getLogger().log(Level.SEVERE, "Unable to insert MySQL transfer data", ex);
        } finally {
            try {
                if (ps != null) ps.close();
                if (conn != null) conn.close();
            } catch (SQLException ex) {
                plugin.getLogger().log(Level.SEVERE, "Unable to close MySQL connection", ex);
            }
        }
    }

    public List<TransferData> getTransferHistory(String playerName, int limit, int offset) {
        List<TransferData> transfers = new ArrayList<>();
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = mysql.getSQLConnection();
            ps = conn.prepareStatement("SELECT * FROM " + transferTable +
                    " WHERE sender = ? OR receiver = ? ORDER BY timestamp DESC LIMIT ? OFFSET ?");
            ps.setString(1, playerName);
            ps.setString(2, playerName);
            ps.setInt(3, limit);
            ps.setInt(4, offset);
            rs = ps.executeQuery();

            while (rs.next()) {
                TransferData transfer = new TransferData(
                        rs.getInt("id"),
                        rs.getString("sender"),
                        rs.getString("receiver"),
                        rs.getString("material"),
                        rs.getInt("amount"),
                        rs.getLong("timestamp"),
                        rs.getString("status")
                );
                transfers.add(transfer);
            }
        } catch (SQLException ex) {
            plugin.getLogger().log(Level.SEVERE, "Unable to get MySQL transfer history", ex);
        } finally {
            try {
                if (rs != null) rs.close();
                if (ps != null) ps.close();
                if (conn != null) conn.close();
            } catch (SQLException ex) {
                plugin.getLogger().log(Level.SEVERE, "Unable to close MySQL connection", ex);
            }
        }
        return transfers;
    }

    public int getTotalTransferCount(String playerName) {
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = mysql.getSQLConnection();
            ps = conn.prepareStatement("SELECT COUNT(*) FROM " + transferTable +
                    " WHERE sender = ? OR receiver = ?");
            ps.setString(1, playerName);
            ps.setString(2, playerName);
            rs = ps.executeQuery();

            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException ex) {
            plugin.getLogger().log(Level.SEVERE, "Unable to get MySQL transfer count", ex);
        } finally {
            try {
                if (rs != null) rs.close();
                if (ps != null) ps.close();
                if (conn != null) conn.close();
            } catch (SQLException ex) {
                plugin.getLogger().log(Level.SEVERE, "Unable to close MySQL connection", ex);
            }
        }
        return 0;
    }

    public List<TransferData> getAllTransferHistory(int limit) {
        List<TransferData> transfers = new ArrayList<>();
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = mysql.getSQLConnection();
            ps = conn.prepareStatement("SELECT * FROM " + transferTable +
                    " ORDER BY timestamp DESC LIMIT ?");
            ps.setInt(1, limit);
            rs = ps.executeQuery();

            while (rs.next()) {
                TransferData transfer = new TransferData(
                        rs.getInt("id"),
                        rs.getString("sender"),
                        rs.getString("receiver"),
                        rs.getString("material"),
                        rs.getInt("amount"),
                        rs.getLong("timestamp"),
                        rs.getString("status")
                );
                transfers.add(transfer);
            }
        } catch (SQLException ex) {
            plugin.getLogger().log(Level.SEVERE, "Unable to get all MySQL transfer history", ex);
        } finally {
            try {
                if (rs != null) rs.close();
                if (ps != null) ps.close();
                if (conn != null) conn.close();
            } catch (SQLException ex) {
                plugin.getLogger().log(Level.SEVERE, "Unable to close MySQL connection", ex);
            }
        }
        return transfers;
    }
}