package net.danh.storage.Utils;

import com.tchristofferson.configupdater.ConfigUpdater;
import net.danh.storage.Manager.ConvertOreManager;
import net.danh.storage.Manager.Crop.CropStorageManager;
import net.danh.storage.Manager.MineManager;
import net.danh.storage.Manager.Mob.MobStorageManager;
import net.danh.storage.Manager.Mythic.MythicStorageManager;
import net.danh.storage.Manager.SpecialMaterial.SpecialMaterialManager;
import net.danh.storage.Storage;
import net.xconfig.bukkit.model.SimpleConfigurationManager;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.logging.Level;

public class File {

    private static void mergeMissingKeys(
            FileConfiguration currentConfig,
            FileConfiguration defaultConfig) {
        if (currentConfig == null || defaultConfig == null) {
            return;
        }

        Set<String> keys = defaultConfig.getKeys(true);
        for (String key : keys) {
            if (currentConfig.contains(key)) {
                continue;
            }

            Object value = defaultConfig.get(key);
            if (value != null) {
                currentConfig.set(key, value);
            }
        }
    }

    private static void updateVersionedConfig(
            String fileName,
            String versionKey,
            String logName,
            String... ignoredSections) {
        java.io.File configFile = new java.io.File(
                Storage.getStorage().getDataFolder(),
                fileName);
        FileConfiguration defaultConfig = YamlConfiguration.loadConfiguration(
                new InputStreamReader(
                        Objects.requireNonNull(
                                Storage.getStorage().getResource(fileName)),
                        StandardCharsets.UTF_8));
        FileConfiguration currentConfig = YamlConfiguration.loadConfiguration(
                configFile);

        int defaultVersion = defaultConfig.getInt(versionKey);
        int currentVersion = currentConfig.contains(versionKey)
                ? currentConfig.getInt(versionKey)
                : 0;

        if (defaultVersion <= currentVersion) {
            return;
        }

        Storage.getStorage().getLogger().log(
                Level.WARNING,
                "Your " + logName + " is updating from v" + currentVersion
                        + " to v" + defaultVersion + "...");

        mergeMissingKeys(currentConfig, defaultConfig);

        try {
            currentConfig.set(versionKey, defaultVersion);
            currentConfig.save(configFile);

            if (ignoredSections != null && ignoredSections.length > 0) {
                ConfigUpdater.update(
                        Storage.getStorage(),
                        fileName,
                        configFile,
                        ignoredSections);
            } else {
                ConfigUpdater.update(Storage.getStorage(), fileName, configFile);
            }

            FileConfiguration updatedConfig = YamlConfiguration.loadConfiguration(
                    configFile);
            if (updatedConfig.getInt(versionKey) != defaultVersion) {
                updatedConfig.set(versionKey, defaultVersion);
                updatedConfig.save(configFile);
            }

            Storage.getStorage().getLogger().log(
                    Level.WARNING,
                    "Your " + logName
                            + " have been updated successful to v"
                            + defaultVersion);
        } catch (IOException e) {
            Storage.getStorage().getLogger().log(
                    Level.SEVERE,
                    "Failed to update " + logName + " file",
                    e);
        }

        getFileSetting().reload(fileName);
    }

    public static int resolveMaxStorage(
            FileConfiguration config,
            String modePath,
            int databaseValue,
            int permissionValue) {
        if (config == null) {
            return permissionValue;
        }

        String mode = config.getString(modePath, "PERMISSION");
        if (mode == null) {
            mode = "PERMISSION";
        }
        mode = mode.trim().toUpperCase(Locale.ROOT);

        if ("DATABASE".equals(mode)) {
            return databaseValue;
        }
        if ("MAX".equals(mode)) {
            return Math.max(databaseValue, permissionValue);
        }
        if ("MIN".equals(mode)) {
            return Math.min(databaseValue, permissionValue);
        }

        return permissionValue;
    }

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

    public static FileConfiguration getViewStorageConfig() {
        return getFileSetting().get("GUI/view-storage.yml");
    }

    public static FileConfiguration getMythicStorageGUIConfig() {
        return getFileSetting().get("GUI/mythicstorage.yml");
    }

    public static FileConfiguration getViewMythicStorageGUIConfig() {
        return getFileSetting().get("GUI/view-mythicstorage.yml");
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

    public static FileConfiguration getMythicStorageConfig() {
        return getFileSetting().get("mythicstorage.yml");
    }

    public static FileConfiguration getCropStorageConfig() {
        return getFileSetting().get("cropstorage.yml");
    }

    public static FileConfiguration getMobStorageConfig() {
        return getFileSetting().get("mobstorage.yml");
    }

    public static FileConfiguration getMobStorageGUIConfig() {
        return getFileSetting().get("GUI/mobstorage.yml");
    }

    public static FileConfiguration getMobItemStorageGUIConfig() {
        return getFileSetting().get("GUI/mob-items.yml");
    }

    public static FileConfiguration getViewMobStorageGUIConfig() {
        return getFileSetting().get("GUI/view-mobstorage.yml");
    }

    public static FileConfiguration getMobTransferGUIConfig() {
        return getFileSetting().get("GUI/mob-transfer.yml");
    }

    public static FileConfiguration getMobTransferMultiGUIConfig() {
        return getFileSetting().get("GUI/mob-transfer-multi.yml");
    }

    public static FileConfiguration getCropStorageGUIConfig() {
        return getFileSetting().get("GUI/cropstorage.yml");
    }

    public static FileConfiguration getCropItemStorageGUIConfig() {
        return getFileSetting().get("GUI/crop-items.yml");
    }

    public static FileConfiguration getViewCropStorageGUIConfig() {
        return getFileSetting().get("GUI/view-cropstorage.yml");
    }

    public static FileConfiguration getCropTransferGUIConfig() {
        return getFileSetting().get("GUI/crop-transfer.yml");
    }

    public static FileConfiguration getCropTransferMultiGUIConfig() {
        return getFileSetting().get("GUI/crop-transfer-multi.yml");
    }

    public static FileConfiguration getFriendStorageConfig() {
        return getFileSetting().get("friendstorage.yml");
    }


    public static FileConfiguration getCraftingConfig() {
        return getFileSetting().get("crafting.yml");
    }

    public static void saveCraftingConfig() {
        getFileSetting().save("crafting.yml");
    }

    public static FileConfiguration getRecipeListGUIConfig() {
        return getFileSetting().get("GUI/recipe-list.yml");
    }

    public static FileConfiguration getRecipeEditorGUIConfig() {
        return getFileSetting().get("GUI/recipe-editor.yml");
    }

    public static FileConfiguration getRecipeEditorListGUIConfig() {
        return getFileSetting().get("GUI/recipe-editor-list.yml");
    }

    public static FileConfiguration getMaterialSelectionGUIConfig() {
        return getFileSetting().get("GUI/material-selection.yml");
    }

    public static FileConfiguration getMaterialEditorGUIConfig() {
        return getFileSetting().get("GUI/material-editor.yml");
    }

    public static FileConfiguration getMythicMaterialSelectionGUIConfig() {
        return getFileSetting().get("GUI/mythic-material-selection.yml");
    }

    public static FileConfiguration getConfirmationGUIConfig() {
        return getFileSetting().get("GUI/confirmation.yml");
    }

    public static void loadFiles() {
        getFileSetting().build("", false, "config.yml", "message.yml", "events.yml", "enchants.yml",
                "special_material.yml", "mythicstorage.yml", "cropstorage.yml", "crafting.yml",
                "friendstorage.yml", "mobstorage.yml");
        copyExampleFiles();
        AutoPickupCache.reload();
    }

    public static void reloadFiles() {
        getFileSetting().reload("config.yml", "message.yml", "events.yml", "enchants.yml", "special_material.yml",
                "mythicstorage.yml", "cropstorage.yml", "crafting.yml", "friendstorage.yml", "mobstorage.yml",
                "GUI/storage.yml", "GUI/items.yml",
                "GUI/transfer.yml", "GUI/transfer-multi.yml", "GUI/convert-ore.yml", "GUI/view-storage.yml",
                "GUI/mythicstorage.yml", "GUI/view-mythicstorage.yml", "GUI/cropstorage.yml",
                "GUI/view-cropstorage.yml", "GUI/crop-items.yml", "GUI/crop-transfer.yml",
                "GUI/crop-transfer-multi.yml",
                "GUI/mobstorage.yml", "GUI/mob-items.yml", "GUI/view-mobstorage.yml", "GUI/mob-transfer.yml",
                "GUI/mob-transfer-multi.yml",
                "GUI/mythictransfer.yml", "GUI/mythictransfer-multi.yml",
                "GUI/recipe-list.yml", "GUI/recipe-editor.yml", "GUI/recipe-editor-list.yml",
                "GUI/material-selection.yml", "GUI/material-editor.yml", "GUI/mythic-material-selection.yml",
                "GUI/confirmation.yml");
        for (Player p : Bukkit.getOnlinePlayers()) {
            MineManager.savePlayerData(p);
            MineManager.loadPlayerData(p);

            if (MythicStorageManager.isSystemEnabled()) {
                MythicStorageManager.savePlayerData(p);
                MythicStorageManager.loadPlayerData(p);
            }

            if (CropStorageManager.isSystemEnabled()) {
                CropStorageManager.savePlayerData(p);
                CropStorageManager.loadPlayerData(p);
            }

            if (MobStorageManager.isSystemEnabled()) {
                MobStorageManager.savePlayerData(p);
                MobStorageManager.loadPlayerData(p);
            }
        }
        ConvertOreManager.loadConvertOptions();
        SpecialMaterialManager.loadSpecialMaterials();
        AutoPickupCache.reload();
    }

    public static void loadGUI() {
        getFileSetting().build("", false, "GUI/storage.yml", "GUI/items.yml", "GUI/transfer.yml",
                "GUI/transfer-multi.yml", "GUI/convert-ore.yml", "GUI/view-storage.yml", "GUI/mythicstorage.yml",
                "GUI/view-mythicstorage.yml", "GUI/cropstorage.yml", "GUI/view-cropstorage.yml",
                "GUI/crop-items.yml", "GUI/crop-transfer.yml", "GUI/crop-transfer-multi.yml",
                "GUI/mobstorage.yml", "GUI/mob-items.yml", "GUI/view-mobstorage.yml", "GUI/mob-transfer.yml",
                "GUI/mob-transfer-multi.yml",
                "GUI/mythictransfer.yml", "GUI/mythictransfer-multi.yml", "GUI/recipe-list.yml",
                "GUI/recipe-editor.yml", "GUI/recipe-editor-list.yml", "GUI/material-selection.yml",
                "GUI/material-editor.yml", "GUI/mythic-material-selection.yml", "GUI/confirmation.yml");
    }

    public static void updateConfig() {
        updateVersionedConfig(
                "config.yml",
                "config_version",
                "config",
                "items",
                "blocks",
                "worth");
    }

    public static void updateMessage() {
        updateVersionedConfig("message.yml", "message_version", "message");
    }

    public static void updateEventConfig() {
        updateVersionedConfig("events.yml", "config_version", "events config");
    }

    public static void updateEnchantConfig() {
        updateVersionedConfig("enchants.yml", "enchant_version", "enchants config");
    }

    public static void updateSpecialMaterialConfig() {
        updateVersionedConfig(
                "special_material.yml",
                "special_materials_version",
                "special materials config");
        SpecialMaterialManager.loadSpecialMaterials();
    }

    public static void updateMythicStorageConfig() {
        updateVersionedConfig(
                "mythicstorage.yml",
                "mythicstorage_version",
                "mythicstorage config");
    }

    public static void updateCropStorageConfig() {
        updateVersionedConfig(
                "cropstorage.yml",
                "cropstorage_version",
                "cropstorage config");
    }

    public static void updateMobStorageConfig() {
        updateVersionedConfig(
                "mobstorage.yml",
                "mobstorage_version",
                "mobstorage config");
    }

    public static void updateFriendStorageConfig() {
        updateVersionedConfig(
                "friendstorage.yml",
                "friendstorage_version",
                "friendstorage config");
    }

    public static void updateCraftingConfig() {
        updateVersionedConfig(
                "crafting.yml",
                "crafting_version",
                "crafting config");
    }

    public static void saveEnchantConfig() {
        try {
            java.io.File configFile = new java.io.File(Storage.getStorage().getDataFolder(), "enchants.yml");
            getEnchantsConfig().save(configFile);
        } catch (IOException e) {
            Storage.getStorage().getLogger().log(Level.SEVERE, "Failed to save enchants.yml config", e);
        }
    }

    private static void copyExampleFiles() {
        java.io.File exampleFile = new java.io.File(Storage.getStorage().getDataFolder(), "particles-examples.yml");

        // Only copy if file doesn't exist to avoid overwriting user modifications
        if (!exampleFile.exists()) {
            try {
                Storage.getStorage().saveResource("particles-examples.yml", false);
                Storage.getStorage().getLogger().log(Level.INFO,
                        "Created particles-examples.yml - Check this file for Advanced Geometric Patterns examples!");
            } catch (Exception e) {
                Storage.getStorage().getLogger().log(Level.WARNING,
                        "Could not create particles-examples.yml: " + e.getMessage());
            }
        }
    }
}
