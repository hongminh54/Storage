package net.danh.storage.Utils;

import com.tchristofferson.configupdater.ConfigUpdater;
import net.danh.storage.Manager.MineManager;
import net.danh.storage.Storage;
import net.xconfig.bukkit.model.SimpleConfigurationManager;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;
import java.util.logging.Level;

public class File {

    public static SimpleConfigurationManager getFileSetting() {
        return SimpleConfigurationManager.get();
    }

    public static FileConfiguration getConfig() {
        return getFileSetting().get("config.yml");
    }

    public static FileConfiguration getMessage() {
        return getFileSetting().get("message.yml");
    }

    public static FileConfiguration getGUIStorage() {
        return getFileSetting().get("GUI/storage.yml");
    }

    public static FileConfiguration getItemStorage() {
        return getFileSetting().get("GUI/items.yml");
    }

    public static void loadFiles() {
        getFileSetting().build("", false, "config.yml", "message.yml");
    }

    public static void reloadFiles() {
        getFileSetting().reload("config.yml", "message.yml", "GUI/storage.yml", "GUI/items.yml", "GUI/transfer.yml", "GUI/transfer-multi.yml");
        for (Player p : Bukkit.getOnlinePlayers()) {
            MineManager.savePlayerData(p);
            MineManager.loadPlayerData(p);
        }
    }

    public static void loadGUI() {
        getFileSetting().build("", false, "GUI/storage.yml", "GUI/items.yml", "GUI/transfer.yml", "GUI/transfer-multi.yml");
    }

    public static void saveFiles() {
        getFileSetting().save("config.yml", "message.yml", "GUI/storage.yml", "GUI/items.yml", "GUI/transfer.yml", "GUI/transfer-multi.yml");
    }

    public static void updateConfig() {
        getFileSetting().save("config.yml");
        java.io.File configFile = new java.io.File(Storage.getStorage().getDataFolder(), "config.yml");
        FileConfiguration defaultConfig = YamlConfiguration.loadConfiguration(new InputStreamReader(Objects.requireNonNull(Storage.getStorage().getResource("config.yml")), StandardCharsets.UTF_8));
        FileConfiguration currentConfig = YamlConfiguration.loadConfiguration(configFile);
        int default_configVersion = defaultConfig.getInt("config_version");
        int current_configVersion = defaultConfig.contains("config_version") ? defaultConfig.getInt("config_version") : 0;
        if (default_configVersion > current_configVersion || default_configVersion < current_configVersion) {
            List<String> default_whitelist_fortune = defaultConfig.getStringList("whitelist_fortune");
            List<String> current_whitelist_fortune = currentConfig.getStringList("whitelist_fortune");
            List<String> default_blacklist_world = defaultConfig.getStringList("blacklist_world");
            List<String> current_blacklist_world = currentConfig.getStringList("blacklist_world");
            Storage.getStorage().getLogger().log(Level.WARNING, "Your config is updating...");
            if (current_whitelist_fortune.isEmpty()) {
                getConfig().set("whitelist_fortune", default_whitelist_fortune);
                getFileSetting().save("config.yml");
            }
            if (current_blacklist_world.isEmpty()) {
                getConfig().set("blacklist_world", default_blacklist_world);
                getFileSetting().save("config.yml");
            }
            try {
                ConfigUpdater.update(Storage.getStorage(), "config.yml", configFile, "items", "blocks", "worth");
                Storage.getStorage().getLogger().log(Level.WARNING, "Your config have been updated successful");
            } catch (IOException e) {
                Storage.getStorage().getLogger().log(Level.WARNING, "Can not update config by it self, please backup and rename your config then restart to get newest config!!");
                e.printStackTrace();
            }
            getFileSetting().reload("config.yml");
        }
    }

    public static void updateMessage() {
        getFileSetting().save("message.yml");
        java.io.File configFile = new java.io.File(Storage.getStorage().getDataFolder(), "message.yml");
        FileConfiguration defaultConfig = YamlConfiguration.loadConfiguration(new InputStreamReader(Objects.requireNonNull(Storage.getStorage().getResource("message.yml")), StandardCharsets.UTF_8));
        FileConfiguration currentConfig = YamlConfiguration.loadConfiguration(configFile);
        int default_configVersion = defaultConfig.getInt("message_version");
        int current_configVersion = defaultConfig.contains("message_version") ? defaultConfig.getInt("message_version") : 0;
        if (default_configVersion > current_configVersion || default_configVersion < current_configVersion) {
            Storage.getStorage().getLogger().log(Level.WARNING, "Your message is updating...");
            List<String> default_admin_help = defaultConfig.getStringList("admin.help");
            List<String> default_user_help = defaultConfig.getStringList("user.help");
            List<String> current_admin_help = currentConfig.getStringList("admin.help");
            List<String> current_user_help = currentConfig.getStringList("user.help");
            if (default_admin_help.size() != current_admin_help.size()) {
                getConfig().set("admin.help", default_admin_help);
                getFileSetting().save("message.yml");
            }
            if (default_user_help.size() != current_user_help.size()) {
                getConfig().set("user.help", default_user_help);
                getFileSetting().save("message.yml");
            }
            try {
                ConfigUpdater.update(Storage.getStorage(), "message.yml", configFile);
                Storage.getStorage().getLogger().log(Level.WARNING, "Your message have been updated successful");
            } catch (IOException e) {
                Storage.getStorage().getLogger().log(Level.WARNING, "Can not update message by it self, please backup and rename your message then restart to get newest message!!");
                e.printStackTrace();
            }
            getFileSetting().reload("message.yml");
        }
    }
}
