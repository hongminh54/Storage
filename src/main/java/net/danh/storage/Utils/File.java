package net.danh.storage.Utils;

import com.tchristofferson.configupdater.ConfigUpdater;
import net.danh.storage.Manager.ConvertOreManager;
import net.danh.storage.Manager.MineManager;
import net.danh.storage.Manager.SpecialMaterialManager;
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

    public static FileConfiguration getConvertOreConfig() {
        return getFileSetting().get("GUI/convert-ore.yml");
    }

    public static FileConfiguration getEventConfig() {
        return getFileSetting().get("events.yml");
    }

    public static FileConfiguration getEnchantsConfig() {
        return getFileSetting().get("enchants.yml");
    }

    public static FileConfiguration getSpecialMaterialConfig() {
        return getFileSetting().get("special_material.yml");
    }

    public static void loadFiles() {
        getFileSetting().build("", false, "config.yml", "message.yml", "events.yml", "enchants.yml", "special_material.yml");
        copyExampleFiles();
    }

    public static void reloadFiles() {
        getFileSetting().reload("config.yml", "message.yml", "events.yml", "enchants.yml", "special_material.yml", "GUI/storage.yml", "GUI/items.yml", "GUI/transfer.yml", "GUI/transfer-multi.yml", "GUI/convert-ore.yml");
        for (Player p : Bukkit.getOnlinePlayers()) {
            MineManager.savePlayerData(p);
            MineManager.loadPlayerData(p);
        }
        ConvertOreManager.loadConvertOptions();
        SpecialMaterialManager.loadSpecialMaterials();
    }

    public static void loadGUI() {
        getFileSetting().build("", false, "GUI/storage.yml", "GUI/items.yml", "GUI/transfer.yml", "GUI/transfer-multi.yml", "GUI/convert-ore.yml");
    }

    public static void updateConfig() {
        java.io.File configFile = new java.io.File(Storage.getStorage().getDataFolder(), "config.yml");
        FileConfiguration defaultConfig = YamlConfiguration.loadConfiguration(new InputStreamReader(Objects.requireNonNull(Storage.getStorage().getResource("config.yml")), StandardCharsets.UTF_8));
        FileConfiguration currentConfig = YamlConfiguration.loadConfiguration(configFile);
        int default_configVersion = defaultConfig.getInt("config_version");
        int current_configVersion = currentConfig.contains("config_version") ? currentConfig.getInt("config_version") : 0;
        if (default_configVersion > current_configVersion || default_configVersion < current_configVersion) {
            List<String> default_whitelist_fortune = defaultConfig.getStringList("whitelist_fortune");
            List<String> current_whitelist_fortune = currentConfig.getStringList("whitelist_fortune");
            List<String> default_blacklist_world = defaultConfig.getStringList("blacklist_world");
            List<String> current_blacklist_world = currentConfig.getStringList("blacklist_world");
            Storage.getStorage().getLogger().log(Level.WARNING, "Your config is updating...");
            if (current_whitelist_fortune.isEmpty()) {
                getConfig().set("whitelist_fortune", default_whitelist_fortune);
            }
            if (current_blacklist_world.isEmpty()) {
                getConfig().set("blacklist_world", default_blacklist_world);
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
        java.io.File configFile = new java.io.File(Storage.getStorage().getDataFolder(), "message.yml");
        FileConfiguration defaultConfig = YamlConfiguration.loadConfiguration(new InputStreamReader(Objects.requireNonNull(Storage.getStorage().getResource("message.yml")), StandardCharsets.UTF_8));
        FileConfiguration currentConfig = YamlConfiguration.loadConfiguration(configFile);
        int default_configVersion = defaultConfig.getInt("message_version");
        int current_configVersion = currentConfig.contains("message_version") ? currentConfig.getInt("message_version") : 0;
        if (default_configVersion > current_configVersion || default_configVersion < current_configVersion) {
            Storage.getStorage().getLogger().log(Level.WARNING, "Your message is updating...");
            List<String> default_admin_help = defaultConfig.getStringList("admin.help");
            List<String> default_user_help = defaultConfig.getStringList("user.help");
            List<String> current_admin_help = currentConfig.getStringList("admin.help");
            List<String> current_user_help = currentConfig.getStringList("user.help");
            if (default_admin_help.size() != current_admin_help.size()) {
                getMessage().set("admin.help", default_admin_help);
            }
            if (default_user_help.size() != current_user_help.size()) {
                getMessage().set("user.help", default_user_help);
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

    public static void updateEventConfig() {
        java.io.File configFile = new java.io.File(Storage.getStorage().getDataFolder(), "events.yml");
        FileConfiguration defaultConfig = YamlConfiguration.loadConfiguration(new InputStreamReader(Objects.requireNonNull(Storage.getStorage().getResource("events.yml")), StandardCharsets.UTF_8));
        FileConfiguration currentConfig = YamlConfiguration.loadConfiguration(configFile);
        int default_configVersion = defaultConfig.getInt("config_version");
        int current_configVersion = currentConfig.contains("config_version") ? currentConfig.getInt("config_version") : 0;
        if (default_configVersion > current_configVersion || default_configVersion < current_configVersion) {
            Storage.getStorage().getLogger().log(Level.WARNING, "Your events config is updating...");
            try {
                ConfigUpdater.update(Storage.getStorage(), "events.yml", configFile);
                Storage.getStorage().getLogger().log(Level.WARNING, "Your events config have been updated successful");
            } catch (IOException e) {
                Storage.getStorage().getLogger().log(Level.WARNING, "Can not update events config by it self, please backup and rename your events config then restart to get newest config!!");
                e.printStackTrace();
            }
            getFileSetting().reload("events.yml");
        }
    }

    public static void updateEnchantConfig() {
        java.io.File configFile = new java.io.File(Storage.getStorage().getDataFolder(), "enchants.yml");
        FileConfiguration defaultConfig = YamlConfiguration.loadConfiguration(new InputStreamReader(Objects.requireNonNull(Storage.getStorage().getResource("enchants.yml")), StandardCharsets.UTF_8));
        FileConfiguration currentConfig = YamlConfiguration.loadConfiguration(configFile);
        int default_enchantVersion = defaultConfig.getInt("enchant_version");
        int current_enchantVersion = currentConfig.contains("enchant_version") ? currentConfig.getInt("enchant_version") : 0;
        if (default_enchantVersion > current_enchantVersion || default_enchantVersion < current_enchantVersion) {
            Storage.getStorage().getLogger().log(Level.WARNING, "Your enchants config is updating...");
            try {
                ConfigUpdater.update(Storage.getStorage(), "enchants.yml", configFile);
                Storage.getStorage().getLogger().log(Level.WARNING, "Your enchants config have been updated successful");
            } catch (IOException e) {
                Storage.getStorage().getLogger().log(Level.WARNING, "Can not update enchants config by it self, please backup and rename your enchants config then restart to get newest config!!");
                e.printStackTrace();
            }
            getFileSetting().reload("enchants.yml");
        }
    }

    public static void updateSpecialMaterialConfig() {
        java.io.File configFile = new java.io.File(Storage.getStorage().getDataFolder(), "special_material.yml");
        FileConfiguration defaultConfig = YamlConfiguration.loadConfiguration(new InputStreamReader(Objects.requireNonNull(Storage.getStorage().getResource("special_material.yml")), StandardCharsets.UTF_8));
        FileConfiguration currentConfig = YamlConfiguration.loadConfiguration(configFile);
        int default_specialMaterialVersion = defaultConfig.getInt("special_materials_version");
        int current_specialMaterialVersion = currentConfig.contains("special_materials_version") ? currentConfig.getInt("special_materials_version") : 0;
        if (default_specialMaterialVersion > current_specialMaterialVersion || default_specialMaterialVersion < current_specialMaterialVersion) {
            Storage.getStorage().getLogger().log(Level.WARNING, "Your special materials config is updating...");
            try {
                ConfigUpdater.update(Storage.getStorage(), "special_material.yml", configFile);
                Storage.getStorage().getLogger().log(Level.WARNING, "Your special materials config have been updated successful");
            } catch (IOException e) {
                Storage.getStorage().getLogger().log(Level.WARNING, "Can not update special materials config by it self, please backup and rename your special materials config then restart to get newest config!!");
                e.printStackTrace();
            }
            getFileSetting().reload("special_material.yml");
            SpecialMaterialManager.loadSpecialMaterials();
        }
    }

    public static void saveEnchantConfig() {
        try {
            java.io.File configFile = new java.io.File(Storage.getStorage().getDataFolder(), "enchants.yml");
            getEnchantsConfig().save(configFile);
        } catch (IOException e) {
            Storage.getStorage().getLogger().log(Level.SEVERE, "Could not save enchants.yml config: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void copyExampleFiles() {
        java.io.File exampleFile = new java.io.File(Storage.getStorage().getDataFolder(), "particles-examples.yml");

        // Only copy if file doesn't exist to avoid overwriting user modifications
        if (!exampleFile.exists()) {
            try {
                Storage.getStorage().saveResource("particles-examples.yml", false);
                Storage.getStorage().getLogger().log(Level.INFO, "Created particles-examples.yml - Check this file for Advanced Geometric Patterns examples!");
            } catch (Exception e) {
                Storage.getStorage().getLogger().log(Level.WARNING, "Could not create particles-examples.yml: " + e.getMessage());
            }
        }
    }
}
