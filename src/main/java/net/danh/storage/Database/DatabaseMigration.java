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
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

public class DatabaseMigration {
    
    private final Storage plugin;
    private static String lastDatabaseType = null;
    
    public DatabaseMigration(Storage plugin) {
        this.plugin = plugin;
    }
    
    /**
     * Check if database type has changed and perform migration if needed
     */
    public static void checkAndMigrate() {
        String currentType = File.getConfig().getString("database.type", "sqlite").toLowerCase();
        
        if (lastDatabaseType != null && !lastDatabaseType.equals(currentType)) {
            Storage.getStorage().getLogger().info("Database type changed from " + lastDatabaseType + " to " + currentType);
            
            // Perform migration asynchronously
            CompletableFuture.runAsync(() -> {
                try {
                    DatabaseMigration migration = new DatabaseMigration(Storage.getStorage());
                    migration.migrateData(lastDatabaseType, currentType);
                } catch (Exception e) {
                    Storage.getStorage().getLogger().log(Level.SEVERE, "Failed to migrate database", e);
                }
            });
        }
        
        lastDatabaseType = currentType;
    }
    
    /**
     * Migrate all data from source database type to target database type
     */
    private void migrateData(String fromType, String toType) throws Exception {
        plugin.getLogger().info("Starting database migration from " + fromType + " to " + toType);
        
        // Create backup before migration
        createBackup(fromType);
        
        // Migrate PlayerData
        int playerDataCount = migratePlayerData(fromType, toType);
        
        // Migrate TransferData
        int transferDataCount = migrateTransferData(fromType, toType);
        
        plugin.getLogger().info("Migration completed: " + playerDataCount + " player records, " + 
                               transferDataCount + " transfer records");
    }
    
    /**
     * Migrate player data between database types
     */
    private int migratePlayerData(String fromType, String toType) throws Exception {
        List<PlayerData> playerDataList = readAllPlayerData(fromType);
        
        if (playerDataList.isEmpty()) {
            plugin.getLogger().info("No player data to migrate");
            return 0;
        }
        
        // Write to new database type
        writeAllPlayerData(toType, playerDataList);
        
        plugin.getLogger().info("Migrated " + playerDataList.size() + " player data records");
        return playerDataList.size();
    }
    
    /**
     * Migrate transfer data between database types
     */
    private int migrateTransferData(String fromType, String toType) throws Exception {
        List<TransferData> transferDataList = readAllTransferData(fromType);
        
        if (transferDataList.isEmpty()) {
            plugin.getLogger().info("No transfer data to migrate");
            return 0;
        }
        
        // Write to new database type
        writeAllTransferData(toType, transferDataList);
        
        plugin.getLogger().info("Migrated " + transferDataList.size() + " transfer data records");
        return transferDataList.size();
    }
    
    /**
     * Read all player data from specified database type
     */
    private List<PlayerData> readAllPlayerData(String databaseType) throws Exception {
        List<PlayerData> playerDataList = new ArrayList<>();
        
        if ("sqlite".equals(databaseType)) {
            // Read from SQLite
            Connection conn = null;
            PreparedStatement ps = null;
            ResultSet rs = null;
            
            try {
                SQLite sqliteDb = new SQLite(plugin);
                conn = sqliteDb.getSQLConnection();
                if (conn == null) return playerDataList;
                
                ps = conn.prepareStatement("SELECT player, data, max, autopickup FROM PlayerData");
                rs = ps.executeQuery();
                
                while (rs.next()) {
                    String player = rs.getString("player");
                    String data = rs.getString("data");
                    int max = rs.getInt("max");
                    boolean autoPickup = true;
                    try {
                        autoPickup = rs.getBoolean("autopickup");
                    } catch (SQLException e) {
                        autoPickup = true;
                    }
                    
                    playerDataList.add(new PlayerData(player, data, max, autoPickup));
                }
            } finally {
                if (rs != null) rs.close();
                if (ps != null) ps.close();
                if (conn != null) conn.close();
            }
        } else {
            // Read from YML
            java.io.File playersFile = new java.io.File(plugin.getDataFolder(), "playerdata/players.yml");
            if (playersFile.exists()) {
                FileConfiguration config = YamlConfiguration.loadConfiguration(playersFile);
                
                for (String player : config.getKeys(false)) {
                    String data = config.getString(player + ".data", "{}");
                    int max = config.getInt(player + ".max", 100000);
                    boolean autoPickup = config.getBoolean(player + ".autopickup", true);
                    
                    playerDataList.add(new PlayerData(player, data, max, autoPickup));
                }
            }
        }
        
        return playerDataList;
    }
    
    /**
     * Write all player data to specified database type
     */
    private void writeAllPlayerData(String databaseType, List<PlayerData> playerDataList) throws Exception {
        if ("sqlite".equals(databaseType)) {
            // Write to SQLite
            SQLite sqliteDb = new SQLite(plugin);
            sqliteDb.load(); // Initialize tables
            
            for (PlayerData playerData : playerDataList) {
                PlayerData existing = sqliteDb.getData(playerData.getPlayer());
                if (existing == null) {
                    sqliteDb.createTable(playerData);
                } else {
                    sqliteDb.updateTable(playerData);
                }
            }
        } else {
            // Write to YML
            java.io.File playerDataFolder = new java.io.File(plugin.getDataFolder(), "playerdata");
            if (!playerDataFolder.exists()) {
                playerDataFolder.mkdirs();
            }
            
            java.io.File playersFile = new java.io.File(playerDataFolder, "players.yml");
            FileConfiguration config = new YamlConfiguration();
            
            for (PlayerData playerData : playerDataList) {
                config.set(playerData.getPlayer() + ".data", playerData.getData());
                config.set(playerData.getPlayer() + ".max", playerData.getMax());
                config.set(playerData.getPlayer() + ".autopickup", playerData.isAutoPickup());
            }
            
            config.save(playersFile);
        }
    }
    
    /**
     * Read all transfer data from specified database type
     */
    private List<TransferData> readAllTransferData(String databaseType) throws Exception {
        List<TransferData> transferDataList = new ArrayList<>();
        
        if ("sqlite".equals(databaseType)) {
            // Read from SQLite
            Connection conn = null;
            PreparedStatement ps = null;
            ResultSet rs = null;
            
            try {
                SQLite sqliteDb = new SQLite(plugin);
                conn = sqliteDb.getSQLConnection();
                if (conn == null) return transferDataList;
                
                // Check if transfer table exists
                if (!tableExists(conn, "TransferLogs")) {
                    return transferDataList;
                }
                
                ps = conn.prepareStatement("SELECT id, sender, receiver, material, amount, timestamp, status FROM TransferLogs");
                rs = ps.executeQuery();
                
                while (rs.next()) {
                    TransferData transferData = new TransferData(
                            rs.getInt("id"),
                            rs.getString("sender"),
                            rs.getString("receiver"),
                            rs.getString("material"),
                            rs.getInt("amount"),
                            rs.getLong("timestamp"),
                            rs.getString("status")
                    );
                    transferDataList.add(transferData);
                }
            } finally {
                if (rs != null) rs.close();
                if (ps != null) ps.close();
                if (conn != null) conn.close();
            }
        } else {
            // Read from YML
            java.io.File transferFile = new java.io.File(plugin.getDataFolder(), "playerdata/transfers.yml");
            if (transferFile.exists()) {
                FileConfiguration config = YamlConfiguration.loadConfiguration(transferFile);
                
                if (config.getConfigurationSection("transfers") != null) {
                    for (String key : config.getConfigurationSection("transfers").getKeys(false)) {
                        String path = "transfers." + key;
                        TransferData transferData = new TransferData(
                                Integer.parseInt(key),
                                config.getString(path + ".sender"),
                                config.getString(path + ".receiver"),
                                config.getString(path + ".material"),
                                config.getInt(path + ".amount"),
                                config.getLong(path + ".timestamp"),
                                config.getString(path + ".status")
                        );
                        transferDataList.add(transferData);
                    }
                }
            }
        }
        
        return transferDataList;
    }
    
    /**
     * Write all transfer data to specified database type
     */
    private void writeAllTransferData(String databaseType, List<TransferData> transferDataList) throws Exception {
        if ("sqlite".equals(databaseType)) {
            // Write to SQLite - use existing TransferDatabase logic
            TransferDatabase transferDb = new TransferDatabase(plugin);
            transferDb.createTransferTable();
            
            for (TransferData transferData : transferDataList) {
                transferDb.insertTransfer(transferData);
            }
        } else {
            // Write to YML
            java.io.File playerDataFolder = new java.io.File(plugin.getDataFolder(), "playerdata");
            if (!playerDataFolder.exists()) {
                playerDataFolder.mkdirs();
            }
            
            java.io.File transferFile = new java.io.File(playerDataFolder, "transfers.yml");
            FileConfiguration config = new YamlConfiguration();
            
            int nextId = 1;
            for (TransferData transferData : transferDataList) {
                String path = "transfers." + transferData.getId();
                config.set(path + ".sender", transferData.getSender());
                config.set(path + ".receiver", transferData.getReceiver());
                config.set(path + ".material", transferData.getMaterial());
                config.set(path + ".amount", transferData.getAmount());
                config.set(path + ".timestamp", transferData.getTimestamp());
                config.set(path + ".status", transferData.getStatus());
                
                if (transferData.getId() >= nextId) {
                    nextId = transferData.getId() + 1;
                }
            }
            
            config.set("next_id", nextId);
            config.save(transferFile);
        }
    }
    
    /**
     * Create backup of current database
     */
    private void createBackup(String databaseType) {
        try {
            String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
            
            if ("sqlite".equals(databaseType)) {
                java.io.File currentDb = new java.io.File(plugin.getDataFolder(), "PlayerData.db");
                if (currentDb.exists()) {
                    java.io.File backupDb = new java.io.File(plugin.getDataFolder(), 
                            "PlayerData_migration_backup_" + timestamp + ".db");
                    java.nio.file.Files.copy(currentDb.toPath(), backupDb.toPath());
                    plugin.getLogger().info("Created SQLite backup: " + backupDb.getName());
                }
            } else {
                java.io.File playerDataFolder = new java.io.File(plugin.getDataFolder(), "playerdata");
                if (playerDataFolder.exists()) {
                    java.io.File backupFolder = new java.io.File(plugin.getDataFolder(), 
                            "playerdata_migration_backup_" + timestamp);
                    copyFolder(playerDataFolder, backupFolder);
                    plugin.getLogger().info("Created YML backup: " + backupFolder.getName());
                }
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Failed to create backup", e);
        }
    }
    
    private void copyFolder(java.io.File source, java.io.File destination) throws IOException {
        if (!destination.exists()) {
            destination.mkdirs();
        }
        
        for (java.io.File file : source.listFiles()) {
            if (file.isDirectory()) {
                copyFolder(file, new java.io.File(destination, file.getName()));
            } else {
                java.nio.file.Files.copy(file.toPath(), 
                        new java.io.File(destination, file.getName()).toPath());
            }
        }
    }
    
    private boolean tableExists(Connection conn, String tableName) {
        try {
            PreparedStatement ps = conn.prepareStatement("SELECT name FROM sqlite_master WHERE type='table' AND name=?");
            ps.setString(1, tableName);
            ResultSet rs = ps.executeQuery();
            boolean exists = rs.next();
            rs.close();
            ps.close();
            return exists;
        } catch (SQLException e) {
            return false;
        }
    }
}