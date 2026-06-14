package net.danh.storage.Utils;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static net.danh.storage.Storage.getStorage;

public final class AutoPickupCache {

    // Storage (BlockBreak)
    private static volatile boolean storageActionBarEnabled = false;
    private static volatile boolean storageTitleEnabled = false;
    private static volatile boolean preventRebreak = false;
    private static volatile Set<String> storageBlacklist = Collections.emptySet();
    private static volatile Set<String> storageFortuneWhitelist = Collections.emptySet();

    // Ground store – Storage
    private static volatile boolean storageGroundStoreActionBar = false;
    private static volatile boolean storageGroundStoreTitleEnabled = false;


    // MythicStorage (GroundStoreListener)
    private static volatile boolean mythicActionBarEnabled = false;
    private static volatile boolean mythicTitleEnabled = false;
    private static volatile Set<String> mythicBlacklist = Collections.emptySet();


    // CropStorage (CropBreak + GroundStoreListener)
    private static volatile boolean cropActionBarEnabled = false;
    private static volatile boolean cropTitleEnabled = false;
    private static volatile Set<String> cropBlacklist = Collections.emptySet();

    // MobStorage (MobDeath + GroundStoreListener)
    private static volatile boolean mobActionBarEnabled = false;
    private static volatile boolean mobTitleEnabled = false;
    private static volatile Set<String> mobBlacklist = Collections.emptySet();


    // MMOItems autosmelt hook
    private static volatile boolean mmoitemsAutoSmeltEnabled = true;

    // Keep internal; all construction prevented
    private AutoPickupCache() {
    }

    public static void reload() {
        try {
            org.bukkit.configuration.file.FileConfiguration cfg = File.getConfig();
            org.bukkit.configuration.file.FileConfiguration mythic = File.getMythicStorageConfig();
            org.bukkit.configuration.file.FileConfiguration crop = File.getCropStorageConfig();
            org.bukkit.configuration.file.FileConfiguration mob = File.getMobStorageConfig();

            //Storage
            preventRebreak = cfg.getBoolean("prevent_rebreak", false);
            storageActionBarEnabled = cfg.getBoolean("mine.actionbar.enable", false);
            storageTitleEnabled = cfg.getBoolean("mine.title.enable", false);
            storageBlacklist = immutableSet(cfg.getStringList("blacklist_world"));
            storageFortuneWhitelist = immutableSet(cfg.getStringList("whitelist_fortune"));
            mmoitemsAutoSmeltEnabled = cfg.getBoolean("hooks.mmoitems_autosmelt.enabled", true);

            storageGroundStoreActionBar = cfg.getBoolean("mine.actionbar.enable", false)
                    && cfg.getBoolean("ground_store.notification.actionbar.enable", true);
            storageGroundStoreTitleEnabled = cfg.getBoolean("mine.title.enable", false)
                    && cfg.getBoolean("ground_store.notification.title.enable", true);

            //MythicStorage
            mythicActionBarEnabled = mythic.getBoolean("notification.actionbar.enable", true)
                    && mythic.getBoolean("ground_store.notification.actionbar.enable", true);
            mythicTitleEnabled = mythic.getBoolean("notification.title.enable", false)
                    && mythic.getBoolean("ground_store.notification.title.enable", true);
            mythicBlacklist = immutableSet(mythic.getStringList("blacklist_world"));

            //CropStorage
            cropActionBarEnabled = crop.getBoolean("notification.actionbar.enable", false)
                    && crop.getBoolean("ground_store.notification.actionbar.enable", true);
            cropTitleEnabled = crop.getBoolean("notification.title.enable", false)
                    && crop.getBoolean("ground_store.notification.title.enable", true);
            cropBlacklist = immutableSet(crop.getStringList("blacklist_world"));

            //MobStorage
            mobActionBarEnabled = mob.getBoolean("notification.actionbar.enable", false)
                    && mob.getBoolean("ground_store.notification.actionbar.enable", true);
            mobTitleEnabled = mob.getBoolean("notification.title.enable", false)
                    && mob.getBoolean("ground_store.notification.title.enable", true);
            mobBlacklist = immutableSet(mob.getStringList("blacklist_world"));

        } catch (Exception ex) {
            getStorage().getLogger()
                    .warning("[AutoPickupCache] Failed to reload cache: " + ex.getMessage());
        }
    }

    @NotNull
    private static Set<String> immutableSet(@Nullable List<String> list) {
        if (list == null || list.isEmpty()) {
            return Collections.emptySet();
        }
        return Collections.unmodifiableSet(new HashSet<>(list));
    }


    // Public getters


    public static boolean isPreventRebreak() {
        return preventRebreak;
    }

    public static boolean isStorageActionBarEnabled() {
        return storageActionBarEnabled;
    }

    public static boolean isStorageTitleEnabled() {
        return storageTitleEnabled;
    }

    public static boolean isMmoitemsAutoSmeltEnabled() {
        return mmoitemsAutoSmeltEnabled;
    }

    public static boolean isStorageGroundStoreActionBarEnabled() {
        return storageGroundStoreActionBar;
    }

    public static boolean isStorageGroundStoreTitleEnabled() {
        return storageGroundStoreTitleEnabled;
    }

    public static boolean isMythicActionBarEnabled() {
        return mythicActionBarEnabled;
    }

    public static boolean isMythicTitleEnabled() {
        return mythicTitleEnabled;
    }

    public static boolean isCropActionBarEnabled() {
        return cropActionBarEnabled;
    }

    public static boolean isCropTitleEnabled() {
        return cropTitleEnabled;
    }

    public static boolean isMobActionBarEnabled() {
        return mobActionBarEnabled;
    }

    public static boolean isMobTitleEnabled() {
        return mobTitleEnabled;
    }

    public static boolean isStorageWorldBlacklisted(@NotNull String worldName) {
        return !storageBlacklist.isEmpty() && storageBlacklist.contains(worldName);
    }

    public static boolean isMythicWorldBlacklisted(@NotNull String worldName) {
        return !mythicBlacklist.isEmpty() && mythicBlacklist.contains(worldName);
    }

    public static boolean isCropWorldBlacklisted(@NotNull String worldName) {
        return !cropBlacklist.isEmpty() && cropBlacklist.contains(worldName);
    }

    public static boolean isMobWorldBlacklisted(@NotNull String worldName) {
        return !mobBlacklist.isEmpty() && mobBlacklist.contains(worldName);
    }

    public static boolean isFortuneWhitelisted(@NotNull String materialName) {
        return storageFortuneWhitelist.contains(materialName);
    }
}
