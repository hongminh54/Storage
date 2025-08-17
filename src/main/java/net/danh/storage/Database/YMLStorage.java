package net.danh.storage.Database;

import net.danh.storage.Storage;
import net.danh.storage.Utils.File;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.logging.Level;

public class YMLStorage implements IDataStorage {

    private final Storage plugin;
    private final java.io.File dataFolder;
    private final java.io.File playersFolder;

    public YMLStorage(Storage plugin) {
        this.plugin = plugin;
        this.dataFolder = new java.io.File(plugin.getDataFolder(), "data");
        this.playersFolder = new java.io.File(dataFolder, "players");
    }

    @Override
    public void load() {
        // Create data directory structure
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }
        if (!playersFolder.exists()) {
            playersFolder.mkdirs();
        }

        plugin.getLogger().info("Loaded YML Data Storage (Per-player files)");
    }

    @Override
    @Nullable
    public PlayerData getData(@NotNull String player) {
        java.io.File playerFile = getPlayerFile(player);
        if (!playerFile.exists()) {
            return null;
        }

        FileConfiguration playerConfig = YamlConfiguration.loadConfiguration(playerFile);

        String data = playerConfig.getString("data", "{}");
        int max = playerConfig.getInt("max", File.getConfig().getInt("settings.default_max_storage", 100000));
        boolean autoPickup = playerConfig.getBoolean("autopickup", File.getConfig().getBoolean("settings.default_auto_pickup", false));

        return new PlayerData(player, data, max, autoPickup);
    }

    @Override
    public void createTable(@NotNull PlayerData playerData) {
        savePlayerData(playerData);
    }

    @Override
    public void updateTable(@NotNull PlayerData playerData) {
        savePlayerData(playerData);
    }

    @Override
    public void deleteData(@NotNull String player) {
        java.io.File playerFile = getPlayerFile(player);
        if (playerFile.exists()) {
            if (!playerFile.delete()) {
                plugin.getLogger().log(Level.WARNING, "Could not delete player file for: " + player);
            }
        }
    }

    @Override
    public String getType() {
        return "YML";
    }

    /**
     * Get the file for a specific player
     *
     * @param player Player name
     * @return Player's data file
     */
    private java.io.File getPlayerFile(@NotNull String player) {
        return new java.io.File(playersFolder, player + ".yml");
    }

    /**
     * Save player data to individual file
     *
     * @param playerData Player data to save
     */
    private void savePlayerData(@NotNull PlayerData playerData) {
        java.io.File playerFile = getPlayerFile(playerData.getPlayer());

        FileConfiguration playerConfig = new YamlConfiguration();
        playerConfig.set("data", playerData.getData());
        playerConfig.set("max", playerData.getMax());
        playerConfig.set("autopickup", playerData.isAutoPickup());

        // Last updated timestamp
        playerConfig.set("last_updated", formatTimestamp(System.currentTimeMillis()));

        try {
            playerConfig.save(playerFile);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not save player data for: " + playerData.getPlayer(), e);
        }
    }

    /**
     * Format timestamp to readable string
     *
     * @param timestamp Timestamp to format
     * @return Formatted date string
     */
    private String formatTimestamp(long timestamp) {
        java.text.SimpleDateFormat formatter = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return formatter.format(new java.util.Date(timestamp));
    }
}