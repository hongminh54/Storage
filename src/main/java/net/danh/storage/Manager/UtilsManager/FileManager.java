package net.danh.storage.Manager.UtilsManager;

import net.xconfig.bukkit.model.SimpleConfigurationManager;
import org.bukkit.configuration.file.FileConfiguration;

public class FileManager {

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

    public static void loadGUI() {
        getFileSetting().build("", false, "GUI/storage.yml", "GUI/items.yml");
    }

    public static void saveFiles() {
        getFileSetting().save("config.yml", "message.yml", "GUI/storage.yml", "GUI/items.yml");
    }
}
