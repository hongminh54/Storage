package net.danh.storage.Database;

import net.danh.storage.Data.TransferData;
import net.danh.storage.Storage;
import net.danh.storage.Utils.File;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

public class TransferDatabase {
    private final String transferTable = "TransferLogs";
    private final Storage plugin;
    private final java.io.File transferFile;
    private FileConfiguration transferConfig;

    public TransferDatabase(Storage plugin) {
        this.plugin = plugin;
        this.transferFile = new java.io.File(plugin.getDataFolder(), "playerdata/transfers.yml");
        initializeYmlFile();
    }

    private void initializeYmlFile() {
        if (!transferFile.getParentFile().exists()) {
            transferFile.getParentFile().mkdirs();
        }
        if (!transferFile.exists()) {
            try {
                transferFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().log(Level.SEVERE, "Could not create transfers.yml file", e);
            }
        }
        transferConfig = YamlConfiguration.loadConfiguration(transferFile);
    }

    private boolean isYmlDatabase() {
        String databaseType = File.getConfig().getString("database.type", "sqlite").toLowerCase();
        return "yml".equals(databaseType) || "yaml".equals(databaseType);
    }

    public void createTransferTable() {
        if (isYmlDatabase()) {
            // YML database doesn't need table creation
            return;
        }
        
        Connection conn = null;
        PreparedStatement ps = null;
        try {
            conn = Storage.db.getSQLConnection();
            if (conn == null) return;
            ps = conn.prepareStatement("CREATE TABLE IF NOT EXISTS " + transferTable + " (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "sender TEXT NOT NULL," +
                    "receiver TEXT NOT NULL," +
                    "material TEXT NOT NULL," +
                    "amount INTEGER NOT NULL," +
                    "timestamp LONG NOT NULL," +
                    "status TEXT NOT NULL" +
                    ");");
            ps.executeUpdate();
        } catch (SQLException ex) {
            plugin.getLogger().log(Level.SEVERE, "Unable to create transfer table", ex);
        } finally {
            try {
                if (ps != null) ps.close();
                if (conn != null) conn.close();
            } catch (SQLException ex) {
                plugin.getLogger().log(Level.SEVERE, "Unable to close connection", ex);
            }
        }
    }

    public void insertTransfer(TransferData transferData) {
        if (isYmlDatabase()) {
            insertTransferYml(transferData);
        } else {
            insertTransferSql(transferData);
        }
    }

    private void insertTransferYml(TransferData transferData) {
        try {
            int nextId = transferConfig.getInt("next_id", 1);
            String path = "transfers." + nextId;
            
            transferConfig.set(path + ".sender", transferData.getSender());
            transferConfig.set(path + ".receiver", transferData.getReceiver());
            transferConfig.set(path + ".material", transferData.getMaterial());
            transferConfig.set(path + ".amount", transferData.getAmount());
            transferConfig.set(path + ".timestamp", transferData.getTimestamp());
            transferConfig.set(path + ".status", transferData.getStatus());
            transferConfig.set("next_id", nextId + 1);
            
            transferConfig.save(transferFile);
        } catch (IOException ex) {
            plugin.getLogger().log(Level.SEVERE, "Unable to insert transfer data to YML", ex);
        }
    }

    private void insertTransferSql(TransferData transferData) {
        Connection conn = null;
        PreparedStatement ps = null;
        try {
            conn = Storage.db.getSQLConnection();
            if (conn == null) return;
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
            plugin.getLogger().log(Level.SEVERE, "Unable to insert transfer data", ex);
        } finally {
            try {
                if (ps != null) ps.close();
                if (conn != null) conn.close();
            } catch (SQLException ex) {
                plugin.getLogger().log(Level.SEVERE, "Unable to close connection", ex);
            }
        }
    }

    public List<TransferData> getTransferHistory(String playerName, int limit) {
        return getTransferHistory(playerName, limit, 0);
    }

    public List<TransferData> getTransferHistory(String playerName, int limit, int offset) {
        if (isYmlDatabase()) {
            return getTransferHistoryYml(playerName, limit, offset);
        } else {
            return getTransferHistorySql(playerName, limit, offset);
        }
    }

    private List<TransferData> getTransferHistoryYml(String playerName, int limit, int offset) {
        List<TransferData> transfers = new ArrayList<>();
        List<TransferData> allTransfers = new ArrayList<>();
        
        if (transferConfig.getConfigurationSection("transfers") != null) {
            for (String key : transferConfig.getConfigurationSection("transfers").getKeys(false)) {
                String path = "transfers." + key;
                String sender = transferConfig.getString(path + ".sender");
                String receiver = transferConfig.getString(path + ".receiver");
                
                if (playerName.equals(sender) || playerName.equals(receiver)) {
                    TransferData transfer = new TransferData(
                            Integer.parseInt(key),
                            sender,
                            receiver,
                            transferConfig.getString(path + ".material"),
                            transferConfig.getInt(path + ".amount"),
                            transferConfig.getLong(path + ".timestamp"),
                            transferConfig.getString(path + ".status")
                    );
                    allTransfers.add(transfer);
                }
            }
        }
        
        // Sort by timestamp descending
        allTransfers.sort((a, b) -> Long.compare(b.getTimestamp(), a.getTimestamp()));
        
        // Apply offset and limit
        int start = Math.max(0, offset);
        int end = Math.min(allTransfers.size(), start + limit);
        
        if (start < allTransfers.size()) {
            transfers = allTransfers.subList(start, end);
        }
        
        return transfers;
    }

    private List<TransferData> getTransferHistorySql(String playerName, int limit, int offset) {
        List<TransferData> transfers = new ArrayList<>();
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = Storage.db.getSQLConnection();
            if (conn == null) return transfers;
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
            plugin.getLogger().log(Level.SEVERE, "Unable to get transfer history", ex);
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
        if (isYmlDatabase()) {
            return getTotalTransferCountYml(playerName);
        } else {
            return getTotalTransferCountSql(playerName);
        }
    }

    private int getTotalTransferCountYml(String playerName) {
        int count = 0;
        if (transferConfig.getConfigurationSection("transfers") != null) {
            for (String key : transferConfig.getConfigurationSection("transfers").getKeys(false)) {
                String path = "transfers." + key;
                String sender = transferConfig.getString(path + ".sender");
                String receiver = transferConfig.getString(path + ".receiver");
                
                if (playerName.equals(sender) || playerName.equals(receiver)) {
                    count++;
                }
            }
        }
        return count;
    }

    private int getTotalTransferCountSql(String playerName) {
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = Storage.db.getSQLConnection();
            if (conn == null) return 0;
            ps = conn.prepareStatement("SELECT COUNT(*) FROM " + transferTable +
                    " WHERE sender = ? OR receiver = ?");
            ps.setString(1, playerName);
            ps.setString(2, playerName);
            rs = ps.executeQuery();

            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException ex) {
            plugin.getLogger().log(Level.SEVERE, "Unable to get transfer count", ex);
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

    public List<TransferData> getAllTransferHistory(int limit) {
        if (isYmlDatabase()) {
            return getAllTransferHistoryYml(limit);
        } else {
            return getAllTransferHistorySql(limit);
        }
    }

    private List<TransferData> getAllTransferHistoryYml(int limit) {
        List<TransferData> transfers = new ArrayList<>();
        
        if (transferConfig.getConfigurationSection("transfers") != null) {
            for (String key : transferConfig.getConfigurationSection("transfers").getKeys(false)) {
                String path = "transfers." + key;
                TransferData transfer = new TransferData(
                        Integer.parseInt(key),
                        transferConfig.getString(path + ".sender"),
                        transferConfig.getString(path + ".receiver"),
                        transferConfig.getString(path + ".material"),
                        transferConfig.getInt(path + ".amount"),
                        transferConfig.getLong(path + ".timestamp"),
                        transferConfig.getString(path + ".status")
                );
                transfers.add(transfer);
            }
        }
        
        // Sort by timestamp descending
        transfers.sort((a, b) -> Long.compare(b.getTimestamp(), a.getTimestamp()));
        
        // Apply limit
        if (transfers.size() > limit) {
            transfers = transfers.subList(0, limit);
        }
        
        return transfers;
    }

    private List<TransferData> getAllTransferHistorySql(int limit) {
        List<TransferData> transfers = new ArrayList<>();
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = Storage.db.getSQLConnection();
            if (conn == null) return transfers;
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
            plugin.getLogger().log(Level.SEVERE, "Unable to get all transfer history", ex);
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
}
