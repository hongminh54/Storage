package net.danh.storage.Database;

import net.danh.storage.Storage;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.util.logging.Level;

public class YMLDatabase extends Database {

    private final File dataFolder;
    private final File playersFile;
    private FileConfiguration playersConfig;

    public YMLDatabase(Storage instance) {
        super(instance);
        this.dataFolder = new File(instance.getDataFolder(), "playerdata");
        this.playersFile = new File(dataFolder, "players.yml");
        initializeFiles();
    }

    private void initializeFiles() {
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }

        if (!playersFile.exists()) {
            try {
                playersFile.createNewFile();
            } catch (IOException e) {
                Storage.getStorage().getLogger().log(Level.SEVERE, "Could not create players.yml file", e);
            }
        }

        playersConfig = YamlConfiguration.loadConfiguration(playersFile);
    }

    @Override
    public Connection getSQLConnection() {
        // YML database doesn't use SQL connections
        return null;
    }

    @Override
    public void load() {
        initializeFiles();
        Storage.getStorage().getLogger().info("Loaded YML Data");
    }

    @Override
    public PlayerData getData(String player) {
        if (!playersConfig.contains(player)) {
            return null;
        }

        String data = playersConfig.getString(player + ".data", "{}");
        int max = playersConfig.getInt(player + ".max", 100000);
        boolean autoPickup = playersConfig.getBoolean(player + ".autopickup", true);

        return new PlayerData(player, data, max, autoPickup);
    }

    @Override
    public void createTable(@NotNull PlayerData playerData) {
        playersConfig.set(playerData.getPlayer() + ".data", playerData.getData());
        playersConfig.set(playerData.getPlayer() + ".max", playerData.getMax());
        playersConfig.set(playerData.getPlayer() + ".autopickup", playerData.isAutoPickup());
        saveConfig();
    }

    @Override
    public void updateTable(@NotNull PlayerData playerData) {
        playersConfig.set(playerData.getPlayer() + ".data", playerData.getData());
        playersConfig.set(playerData.getPlayer() + ".max", playerData.getMax());
        playersConfig.set(playerData.getPlayer() + ".autopickup", playerData.isAutoPickup());
        saveConfig();
    }

    @Override
    public void deleteData(String player) {
        playersConfig.set(player, null);
        saveConfig();
    }

    @Override
    public void updateAutoPickup(String player, boolean autoPickup) {
        if (playersConfig.contains(player)) {
            playersConfig.set(player + ".autopickup", autoPickup);
            saveConfig();
        }
    }

    private void saveConfig() {
        try {
            playersConfig.save(playersFile);
        } catch (IOException e) {
            Storage.getStorage().getLogger().log(Level.SEVERE, "Could not save players.yml file", e);
        }
    }

    // Reload config from file (useful for external modifications)
    public void reloadConfig() {
        playersConfig = YamlConfiguration.loadConfiguration(playersFile);
    }
}