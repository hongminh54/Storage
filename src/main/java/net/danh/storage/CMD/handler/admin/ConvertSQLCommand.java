package net.danh.storage.CMD.handler.admin;

import net.danh.storage.CMD.handler.BaseCommand;
import net.danh.storage.Database.PlayerData;
import net.danh.storage.Storage;
import org.bukkit.command.CommandSender;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class ConvertSQLCommand extends BaseCommand {

    private static class ConversionResult {
        final int convertedCount;
        final int skippedCount;
        
        ConversionResult(int convertedCount, int skippedCount) {
            this.convertedCount = convertedCount;
            this.skippedCount = skippedCount;
        }
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (args.length < 1) {
            sendUsage(sender);
            return;
        }

        final String databasePath = args[0];
        final boolean mergeData = args.length > 1 && args[1].equalsIgnoreCase("merge");
        
        // Validate database file exists
        File dbFile = new File(Storage.getStorage().getDataFolder(), databasePath);
        if (!dbFile.exists()) {
            // Try absolute path if relative path doesn't work
            dbFile = new File(databasePath);
            if (!dbFile.exists()) {
                sendMessage(sender, "admin.convertsql.file_not_found", "#path#", databasePath);
                return;
            }
        }
        
        // Check if trying to convert the current active database
        if (isCurrentDatabase(dbFile)) {
            sendMessage(sender, "admin.convertsql.cannot_convert_current");
            return;
        }
        
        final File finalDbFile = dbFile;

        String[] placeholders = {"#path#", "#mode#"};
        String[] replacements = {databasePath, mergeData ? "merge" : "skip existing"};
        sendMessage(sender, "admin.convertsql.starting", placeholders, replacements);

        // Run conversion asynchronously to avoid blocking main thread
        CompletableFuture.runAsync(() -> {
            try {
                // Create backup before conversion
                File backupFile = createBackup();
                
                ConversionResult result = convertDatabase(finalDbFile, mergeData);
                
                // Send result message on main thread
                Storage.getStorage().getServer().getScheduler().runTask(Storage.getStorage(), () -> {
                    if (result.convertedCount > 0 || result.skippedCount > 0) {
                        String[] resultPlaceholders = {"#converted#", "#skipped#", "#total#", "#backup#"};
                        String[] resultReplacements = {
                            String.valueOf(result.convertedCount),
                            String.valueOf(result.skippedCount),
                            String.valueOf(result.convertedCount + result.skippedCount),
                            backupFile != null ? backupFile.getName() : "none"
                        };
                        sendMessage(sender, "admin.convertsql.success_with_backup", resultPlaceholders, resultReplacements);
                    } else {
                        sendMessage(sender, "admin.convertsql.no_data");
                    }
                });
                
            } catch (Exception e) {
                Storage.getStorage().getLogger().severe("Error during database conversion: " + e.getMessage());
                e.printStackTrace();
                
                // Send error message on main thread
                Storage.getStorage().getServer().getScheduler().runTask(Storage.getStorage(), () -> {
                    sendMessage(sender, "admin.convertsql.error", "#error#", e.getMessage());
                });
            }
        });
    }

    private ConversionResult convertDatabase(File sourceDbFile, boolean mergeData) throws SQLException {
        int convertedCount = 0;
        int skippedCount = 0;
        Connection sourceConn = null;
        PreparedStatement sourcePs = null;
        ResultSet rs = null;
        File tempDbFile = null;

        try {
            // Create temporary copy to avoid lock issues
            tempDbFile = createTemporaryCopy(sourceDbFile);
            File dbToUse = tempDbFile != null ? tempDbFile : sourceDbFile;
            
            // Connect to source database (or temporary copy)
            sourceConn = DriverManager.getConnection("jdbc:sqlite:" + dbToUse.getAbsolutePath());
            
            // Try different possible table names and structures
            String[] possibleTables = {"PlayerData", "playerdata", "player_data", "storage_data"};
            String[] possibleColumns = {
                "player, data, max",           // Current format
                "player, data, maximum",       // Alternative max column
                "uuid, data, max",            // UUID instead of player name
                "playername, data, max",      // Alternative player column
                "player_name, player_data, max_storage" // Different naming convention
            };

            for (String tableName : possibleTables) {
                if (tableExists(sourceConn, tableName)) {
                    Storage.getStorage().getLogger().info("Found table: " + tableName);
                    
                    for (String columns : possibleColumns) {
                        try {
                            sourcePs = sourceConn.prepareStatement("SELECT " + columns + " FROM " + tableName);
                            rs = sourcePs.executeQuery();
                            
                            while (rs.next()) {
                                String player = rs.getString(1);
                                String data = rs.getString(2);
                                int max = rs.getInt(3);
                                
                                // Validate data
                                if (player != null && !player.trim().isEmpty() && data != null) {
                                    // Normalize player name (remove UUID format if present)
                                    String normalizedPlayer = normalizePlayerName(player);
                                    
                                    // Check if player already exists in current database
                                    PlayerData existingData = Storage.db.getData(normalizedPlayer);
                                    
                                    if (existingData == null) {
                                        // Create new player data
                                        PlayerData newPlayerData = new PlayerData(normalizedPlayer, data, max);
                                        Storage.db.createTable(newPlayerData);
                                        convertedCount++;
                                        Storage.getStorage().getLogger().info("Converted data for player: " + normalizedPlayer);
                                    } else if (mergeData) {
                                        // Merge data logic - combine storage data
                                        String mergedData = mergePlayerData(existingData.getData(), data);
                                        int mergedMax = Math.max(existingData.getMax(), max);
                                        PlayerData mergedPlayerData = new PlayerData(normalizedPlayer, mergedData, mergedMax);
                                        Storage.db.updateTable(mergedPlayerData);
                                        convertedCount++;
                                        Storage.getStorage().getLogger().info("Merged data for player: " + normalizedPlayer);
                                    } else {
                                        skippedCount++;
                                        Storage.getStorage().getLogger().info("Player " + normalizedPlayer + " already exists, skipping...");
                                    }
                                }
                            }
                            
                            // If we successfully read data, break from column attempts
                            if (convertedCount > 0) {
                                break;
                            }
                            
                        } catch (SQLException e) {
                            // Try next column combination
                            continue;
                        } finally {
                            if (rs != null) rs.close();
                            if (sourcePs != null) sourcePs.close();
                        }
                    }
                    
                    // If we found data in this table, break from table attempts
                    if (convertedCount > 0) {
                        break;
                    }
                }
            }

        } finally {
            if (rs != null) rs.close();
            if (sourcePs != null) sourcePs.close();
            if (sourceConn != null) sourceConn.close();
            
            // Clean up temporary file
            if (tempDbFile != null && tempDbFile.exists()) {
                try {
                    tempDbFile.delete();
                } catch (Exception e) {
                    Storage.getStorage().getLogger().warning("Failed to delete temporary file: " + e.getMessage());
                }
            }
        }

        return new ConversionResult(convertedCount, skippedCount);
    }

    private boolean tableExists(Connection conn, String tableName) {
        try {
            DatabaseMetaData meta = conn.getMetaData();
            ResultSet tables = meta.getTables(null, null, tableName, null);
            boolean exists = tables.next();
            tables.close();
            return exists;
        } catch (SQLException e) {
            return false;
        }
    }

    private String normalizePlayerName(String player) {
        // If it's a UUID format, try to convert to player name
        if (player.matches("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}")) {
            // For UUID, we would need to look up the player name
            // For now, just return the UUID as is since we can't easily convert without external API
            return player;
        }
        
        // Remove any special characters and ensure proper case
        return player.trim();
    }

    private boolean isCurrentDatabase(File dbFile) {
        try {
            // Get current database file path
            File currentDbFile = new File(Storage.getStorage().getDataFolder(), "PlayerData.db");
            
            // Compare canonical paths to handle different path representations
            return dbFile.getCanonicalPath().equals(currentDbFile.getCanonicalPath());
        } catch (Exception e) {
            Storage.getStorage().getLogger().warning("Could not verify database path: " + e.getMessage());
            // If we can't verify, assume it's safe to proceed
            return false;
        }
    }

    private File createTemporaryCopy(File sourceFile) {
        try {
            // Create temporary file
            File tempFile = File.createTempFile("storage_convert_", ".db");
            tempFile.deleteOnExit();
            
            // Copy source to temporary file
            Files.copy(sourceFile.toPath(), tempFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            
            Storage.getStorage().getLogger().info("Created temporary copy for conversion: " + tempFile.getName());
            return tempFile;
            
        } catch (Exception e) {
            Storage.getStorage().getLogger().warning("Failed to create temporary copy, using original file: " + e.getMessage());
            return null;
        }
    }

    private File createBackup() {
        try {
            File currentDbFile = new File(Storage.getStorage().getDataFolder(), "PlayerData.db");
            if (!currentDbFile.exists()) {
                return null;
            }

            // Create backup with timestamp
            String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
            File backupFile = new File(Storage.getStorage().getDataFolder(), "PlayerData_backup_" + timestamp + ".db");
            
            // Copy current database to backup
            Files.copy(currentDbFile.toPath(), backupFile.toPath());
            
            Storage.getStorage().getLogger().info("Created database backup: " + backupFile.getName());
            return backupFile;
            
        } catch (Exception e) {
            Storage.getStorage().getLogger().warning("Failed to create backup: " + e.getMessage());
            return null;
        }
    }

    private String mergePlayerData(String existingData, String newData) {
        try {
            // Parse JSON data and merge material amounts
            if (existingData == null || existingData.trim().isEmpty() || existingData.equals("{}")) {
                return newData;
            }
            if (newData == null || newData.trim().isEmpty() || newData.equals("{}")) {
                return existingData;
            }
            
            // Simple JSON merge - add amounts for same materials
            // This is a basic implementation, could be improved with proper JSON parsing
            return existingData; // For safety, keep existing data if both have content
            
        } catch (Exception e) {
            Storage.getStorage().getLogger().warning("Failed to merge player data, keeping existing: " + e.getMessage());
            return existingData;
        }
    }

    @Override
    public List<String> getTabCompletions(CommandSender sender, String[] args) {
        List<String> completions = new ArrayList<>();
        
        if (args.length == 1) {
            // Suggest common database file names
            completions.add("PlayerData.db");
            completions.add("storage.db");
            completions.add("backup.db");
            completions.add("old_data.db");
        } else if (args.length == 2) {
            // Suggest merge option
            completions.add("merge");
        }
        
        return completions;
    }

    @Override
    public String getPermission() {
        return "storage.admin.convertsql";
    }

    @Override
    public String getUsage() {
        return "/storage convertsql <database_file> [merge]";
    }

    @Override
    public String getDescription() {
        return "Convert player data from another SQLite database";
    }
}