package net.danh.storage.Database;

import net.danh.storage.Data.MobTransferData;
import net.danh.storage.Storage;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

public class MobTransferDatabase {
    private final String transferTable = "MobTransferLogs";
    private final Storage plugin;
    private YMLMobTransferStorage ymlStorage;
    private MySQLMobTransferStorage mysqlStorage;

    public MobTransferDatabase(Storage plugin) {
        this.plugin = plugin;

        if (Storage.dataStorage instanceof MySQLAdapter) {
            mysqlStorage = new MySQLMobTransferStorage(plugin);
        } else if (!(Storage.dataStorage instanceof SQLiteAdapter)) {
            ymlStorage = new YMLMobTransferStorage(plugin);
        }
    }

    public void createTransferTable() {
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
                plugin.getLogger().log(Level.SEVERE, "Unable to create mob transfer table", ex);
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

    public void insertTransfer(MobTransferData transferData) {
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
            plugin.getLogger().log(Level.SEVERE, "Unable to insert mob transfer data", ex);
        } finally {
            try {
                if (ps != null) ps.close();
                if (conn != null) conn.close();
            } catch (SQLException ex) {
                plugin.getLogger().log(Level.SEVERE, "Unable to close connection", ex);
            }
        }
    }

    public List<MobTransferData> getTransferHistory(String playerName, int limit, int offset) {
        if (mysqlStorage != null) {
            return mysqlStorage.getTransferHistory(playerName, limit, offset);
        } else if (ymlStorage != null) {
            return ymlStorage.getTransferHistory(playerName, limit, offset);
        }

        List<MobTransferData> transfers = new ArrayList<>();
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
                transfers.add(new MobTransferData(
                        rs.getInt("id"),
                        rs.getString("sender"),
                        rs.getString("receiver"),
                        rs.getString("item_name"),
                        rs.getInt("amount"),
                        rs.getLong("timestamp"),
                        rs.getString("status")
                ));
            }
        } catch (SQLException ex) {
            plugin.getLogger().log(Level.SEVERE, "Unable to get mob transfer history", ex);
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
            plugin.getLogger().log(Level.SEVERE, "Unable to get mob transfer count", ex);
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

    public long getTotalSentAmount(String playerName) {
        if (mysqlStorage != null) {
            return mysqlStorage.getTotalSentAmount(playerName);
        } else if (ymlStorage != null) {
            return ymlStorage.getTotalSentAmount(playerName);
        }
        return getTotalAmount("sender", playerName);
    }

    public long getTotalReceivedAmount(String playerName) {
        if (mysqlStorage != null) {
            return mysqlStorage.getTotalReceivedAmount(playerName);
        } else if (ymlStorage != null) {
            return ymlStorage.getTotalReceivedAmount(playerName);
        }
        return getTotalAmount("receiver", playerName);
    }

    private long getTotalAmount(String column, String playerName) {
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = Storage.db.getSQLConnection();
            ps = conn.prepareStatement("SELECT COALESCE(SUM(amount), 0) FROM " + transferTable +
                    " WHERE " + column + " = ? AND status LIKE 'SUCCESS%'");
            ps.setString(1, playerName);
            rs = ps.executeQuery();

            if (rs.next()) {
                return rs.getLong(1);
            }
        } catch (SQLException ex) {
            plugin.getLogger().log(Level.SEVERE, "Unable to get mob transfer amount for " + column, ex);
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
