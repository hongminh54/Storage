package net.danh.storage;

import net.danh.storage.API.StorageAPI;
import net.danh.storage.CMD.CropStorageCMD;
import net.danh.storage.CMD.MythicStorageCMD;
import net.danh.storage.CMD.StorageCMD;
import net.danh.storage.Database.*;
import net.danh.storage.GUI.GUI;
import net.danh.storage.Listeners.*;
import net.danh.storage.Listeners.Crop.CropBreak;
import net.danh.storage.Listeners.LuckPerms.LuckPermsListener;
import net.danh.storage.Listeners.Mythic.MythicMobDeath;
import net.danh.storage.Listeners.Mythic.MythicMobsLoadListener;
import net.danh.storage.Manager.*;
import net.danh.storage.Manager.Crafting.CraftingManager;
import net.danh.storage.Manager.Crafting.RecipeEditManager;
import net.danh.storage.Manager.Crop.CropStorageManager;
import net.danh.storage.Manager.Event.EventManager;
import net.danh.storage.Manager.Mythic.MythicStorageManager;
import net.danh.storage.Manager.Mythic.MythicTransferManager;
import net.danh.storage.Manager.SpecialMaterial.SpecialMaterialManager;
import net.danh.storage.NMS.NMSAssistant;
import net.danh.storage.Placeholder.CraftingPlaceholder;
import net.danh.storage.Placeholder.PAPI;
import net.danh.storage.Utils.File;
import net.danh.storage.Utils.SchedulerUtil;
import net.danh.storage.Utils.UpdateChecker;
import net.xconfig.bukkit.model.SimpleConfigurationManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;
import java.util.logging.Level;

public final class Storage extends JavaPlugin {

    // Debug Storage
    private static final boolean DEBUG_STORAGE_AUTO_ADD_ALL_VANILLA = true;

    public static IDataStorage dataStorage;
    public static Database db;
    private static Storage storage;

    private static boolean WorldGuard;
    private static boolean MMOItems;
    private static boolean MythicLib;

    private static boolean debugVanillaConfigApplied;

    public static Storage getStorage() {
        return storage;
    }

    public static boolean isWorldGuardInstalled() {
        return WorldGuard;
    }

    public static boolean isMMOItemsInstalled() {
        return MMOItems;
    }

    public static boolean isMythicLibInstalled() {
        return MythicLib;
    }

    public static void updateMmoitemsHookState(boolean enabled) {
        MMOItems = enabled;
        Storage plugin = getStorage();
        if (plugin != null) {
            plugin.getLogger().log(
                    Level.INFO,
                    enabled ? "Hook with MMOItems" : "Unhook MMOItems");
        }
    }

    public static void updateMythicLibHookState(boolean enabled) {
        MythicLib = enabled;

        Storage plugin = getStorage();
        if (plugin != null) {
            plugin.getLogger().log(
                    Level.INFO,
                    enabled ? "Hook with MythicLib" : "Unhook MythicLib");
        }

        if (enabled) {
            if (plugin == null) {
                return;
            }
            if (!DEBUG_STORAGE_AUTO_ADD_ALL_VANILLA) {
                return;
            }
            if (debugVanillaConfigApplied) {
                return;
            }
            SchedulerUtil.runTask(plugin, () -> {
                try {
                    plugin.applyDebugVanillaStorageConfigIfEnabled();
                } finally {
                    debugVanillaConfigApplied = true;
                }
            });
        }
    }

    private static String toTitleCase(String raw) {
        if (raw == null) {
            return "";
        }
        String cleaned = raw.trim();
        if (cleaned.isEmpty()) {
            return "";
        }

        String[] parts = cleaned.toLowerCase(Locale.ENGLISH).split("_");
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            if (part.isEmpty()) {
                continue;
            }
            if (sb.length() > 0) {
                sb.append(' ');
            }
            sb.append(Character.toUpperCase(part.charAt(0)));
            if (part.length() > 1) {
                sb.append(part.substring(1));
            }
        }
        return sb.toString();
    }

    @Override
    public void onLoad() {
        storage = this;

        StorageAPI.initialize(this);
        getLogger().log(Level.INFO, "Storage API initialized");

        if (getServer().getPluginManager().getPlugin("WorldGuard") != null) {
            WorldGuard = true;
            net.danh.storage.WorldGuard.WorldGuard.register(storage);
            getLogger().log(Level.INFO, "Hook with WorldGuard");
        }
    }

    public void applyDebugVanillaStorageConfigIfEnabled() {
        if (!DEBUG_STORAGE_AUTO_ADD_ALL_VANILLA) {
            return;
        }
        applyDebugVanillaStorageConfig();
    }

    @Override
    public void onEnable() {
        getLogger().log(Level.INFO, "Loading...");

        if (SchedulerUtil.isFolia()) {
            getLogger().log(Level.INFO, "Detected Folia server - Using regionized scheduler");
        } else {
            getLogger().log(Level.INFO, "Detected Bukkit/Spigot/Paper server - Using standard scheduler");
        }

        MMOItems = Bukkit.getPluginManager().getPlugin("MMOItems") != null
                && Bukkit.getPluginManager().isPluginEnabled("MMOItems");
        if (MMOItems) {
            getLogger().log(Level.INFO, "Hook with MMOItems");
        }
        MythicLib = Bukkit.getPluginManager().getPlugin("MythicLib") != null
                && Bukkit.getPluginManager().isPluginEnabled("MythicLib");
        if (MythicLib) {
            getLogger().log(Level.INFO, "Hook with MythicLib");
        }

        GUI.register(storage);
        SimpleConfigurationManager.register(storage);
        File.loadFiles();
        File.loadGUI();
        File.updateConfig();
        File.updateMessage();
        File.updateEventConfig();
        File.updateEnchantConfig();
        File.updateSpecialMaterialConfig();
        File.updateMythicStorageConfig();
        File.updateCraftingConfig();
        File.updateCropStorageConfig();

        applyDebugVanillaStorageConfigIfEnabled();
        debugVanillaConfigApplied = DEBUG_STORAGE_AUTO_ADD_ALL_VANILLA
                && Bukkit.getPluginManager().isPluginEnabled("MythicLib");
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new PAPI().register();
            new CraftingPlaceholder(this).register();
        }
        UpdateChecker updateChecker = new UpdateChecker(storage);
        registerEvents(updateChecker, new JoinQuit(), new BlockBreak(), new ChatListener(), new BlockPlace(),
                new GroundStoreListener(), new PluginLoadListener());
        updateChecker.fetch();
        new StorageCMD("storage");
        new MythicStorageCMD("mythicstorage");
        new CropStorageCMD("cropstorage");

        dataStorage = DatabaseFactory.createDatabase(this);
        dataStorage.load();
        getLogger().info("Using " + dataStorage.getType() + " database for data storage");

        if (dataStorage instanceof SQLiteAdapter) {
            db = ((SQLiteAdapter) dataStorage).getSQLiteDatabase();
        } else if (dataStorage instanceof MySQLAdapter) {
            db = ((MySQLAdapter) dataStorage).getMySQLDatabase();
        } else {
            db = new DatabaseCompatibilityWrapper(dataStorage);
        }
        TransferManager.initialize();
        MineManager.loadBlocks();
        ConvertOreManager.loadConvertOptions();
        AutoSaveManager.initialize();
        EventManager.initialize();
        EnchantManager.loadEnchants();
        SpecialMaterialManager.loadSpecialMaterials();
        CraftingManager.loadRecipes();

        // Initialize MythicStorage if MythicMobs is available
        initializeMythicStorage();

        // Initialize CropStorage
        initializeCropStorage();

        if (Bukkit.getPluginManager().getPlugin("LuckPerms") != null) {
            LuckPermsListener.register(this);
        }

        getLogger().log(Level.INFO, "Loading completed. Have fun!");
        if (new NMSAssistant().isVersionLessThanOrEqualTo(12)) {
            getLogger().log(Level.WARNING, "Some material can working incorrect way with your version server ("
                    + new NMSAssistant().getNMSVersion() + ")");
            getLogger().log(Level.WARNING,
                    "If the material doesn't work, you should go to the GitHub Issues section and report it to the author!");
        }
    }

    private void applyDebugVanillaStorageConfig() {
        long startNs = System.nanoTime();
        try {
            boolean legacyItemData = new NMSAssistant().isVersionLessThanOrEqualTo(12);
            ConfigurationSection blocksSection = File.getConfig()
                    .getConfigurationSection("blocks");
            if (blocksSection == null) {
                blocksSection = File.getConfig().createSection("blocks");
            }

            ConfigurationSection itemsSection = File.getConfig()
                    .getConfigurationSection("items");
            if (itemsSection == null) {
                itemsSection = File.getConfig().createSection("items");
            }

            Set<String> blockKeys = new LinkedHashSet<>();
            Set<String> autoSmeltDropKeys = new LinkedHashSet<>();
            int addedBlocks = 0;
            int addedAutoSmeltBlocks = 0;
            int addedItems = 0;
            int addedAutoSmeltItems = 0;

            for (Material material : Material.values()) {
                if (material == null || !material.isBlock()) {
                    continue;
                }
                if (material == Material.AIR
                        || material.name().endsWith("_AIR")) {
                    continue;
                }

                String key = material.name() + ";0";
                blockKeys.add(key);

                if (!blocksSection.contains(key + ".drop")) {
                    blocksSection.set(key + ".drop", key);
                    addedBlocks++;
                }

                if (MythicLib && !blocksSection.contains(key + ".drop_autosmelt")) {
                    String autoSmeltDropKey = null;
                    try {
                        io.lumine.mythic.lib.version.OreDrops drops = io.lumine.mythic.lib.MythicLib.plugin
                                .getVersion()
                                .getWrapper()
                                .getOreDrops(material);
                        if (drops != null) {
                            ItemStack generated = drops.generate(0);
                            autoSmeltDropKey = getDebugDropKey(
                                    generated,
                                    legacyItemData);
                        }
                    } catch (Throwable ignored) {
                        autoSmeltDropKey = null;
                    }

                    if (autoSmeltDropKey != null
                            && !autoSmeltDropKey.equalsIgnoreCase(key)) {
                        blocksSection.set(
                                key + ".drop_autosmelt",
                                autoSmeltDropKey);
                        addedAutoSmeltBlocks++;
                        autoSmeltDropKeys.add(autoSmeltDropKey);

                        if (!itemsSection.contains(autoSmeltDropKey)) {
                            String[] parts = autoSmeltDropKey.split(";");
                            String display = toTitleCase(parts[0]);
                            itemsSection.set(
                                    autoSmeltDropKey,
                                    "&7" + display);
                            addedAutoSmeltItems++;
                        }
                    }
                }

                if (!itemsSection.contains(key)) {
                    String display = toTitleCase(material.name());
                    itemsSection.set(key, "&7" + display);
                    addedItems++;
                }
            }

            List<String> allowed = File.getConfig().getStringList(
                    "ground_store.allowed_items");
            Set<String> mergedAllowed = new LinkedHashSet<>();
            if (allowed != null) {
                mergedAllowed.addAll(allowed);
            }
            int beforeAllowed = mergedAllowed.size();
            mergedAllowed.addAll(blockKeys);
            mergedAllowed.addAll(autoSmeltDropKeys);
            int addedAllowed = mergedAllowed.size() - beforeAllowed;
            File.getConfig().set(
                    "ground_store.allowed_items",
                    new ArrayList<>(mergedAllowed));

            long elapsedMs = (System.nanoTime() - startNs) / 1_000_000L;
            getLogger().log(
                    Level.WARNING,
                    "[DEBUG] Storage debug mode applied: blocks="
                            + blockKeys.size() + ", added_blocks="
                            + addedBlocks + ", added_items="
                            + addedItems + ", added_allowed_items="
                            + addedAllowed + ", added_autosmelt_blocks="
                            + addedAutoSmeltBlocks
                            + ", added_autosmelt_items="
                            + addedAutoSmeltItems
                            + " (" + elapsedMs + "ms)");
        } catch (Exception ex) {
            getLogger().log(
                    Level.SEVERE,
                    "[DEBUG] Failed to apply vanilla debug storage config",
                    ex);
        }
    }

    private String getDebugDropKey(ItemStack itemStack, boolean legacyItemData) {
        if (itemStack == null || itemStack.getType() == null) {
            return null;
        }
        Material type = itemStack.getType();
        if (type == Material.AIR || type.name().endsWith("_AIR")) {
            return null;
        }
        if (legacyItemData) {
            short durability = itemStack.getDurability();
            return type.name() + ";" + durability;
        }
        return type.name() + ";0";
    }

    @Override
    public void onDisable() {
        getLogger().log(Level.INFO, "Shutting down...");
        EventManager.shutdown();
        AutoSaveManager.stopAutoSave();
        for (Player p : Bukkit.getOnlinePlayers()) {
            MineManager.savePlayerData(p);
            MythicStorageManager.savePlayerData(p);
            CropStorageManager.savePlayerData(p);
            // Cleanup player-specific data from managers
            MineManager.cleanupPlayerData(p);
            MythicStorageManager.cleanupPlayerData(p);
            CropStorageManager.cleanupPlayerData(p);
            SoundManager.cleanupPlayer(p);
        }
        TransferManager.cancelAllTransfers();
        MythicTransferManager.cancelAllTransfers();
        CraftingManager.cancelAllCrafting();
        ParticleManager.stopAllAnimations();
        RecipeEditManager.clearFlagCache();

        StorageAPI.shutdown();
        getLogger().log(Level.INFO, "Storage API shutdown");

        getLogger().log(Level.INFO, "Shutting down completed. See you again!");
    }

    public void registerEvents(Listener... listeners) {
        Arrays.asList(listeners).forEach(listener -> getServer().getPluginManager().registerEvents(listener, storage));
    }

    private void initializeMythicStorage() {
        if (Bukkit.getPluginManager().getPlugin("MythicMobs") != null &&
                Bukkit.getPluginManager().isPluginEnabled("MythicMobs")) {

            getLogger().info("[MythicStorage] MythicMobs already loaded, initializing immediately...");
            MythicStorageManager.initialize();
            MythicTransferManager.initialize();

            if (MythicStorageManager.isSystemEnabled()) {
                MythicMobDeath.registerListener(this);
            }
        } else {
            getLogger().info("[MythicStorage] MythicMobs not loaded yet, waiting for plugin enable...");
            registerEvents(new MythicMobsLoadListener());
        }
    }

    private void initializeCropStorage() {
        CropStorageManager.initialize();
        if (CropStorageManager.isSystemEnabled()) {
            getLogger().info("[CropStorage] System enabled, registering crop break listener...");
            registerEvents(new CropBreak());

            // Load data for online players (in case of reload)
            for (Player p : Bukkit.getOnlinePlayers()) {
                CropStorageManager.loadPlayerData(p);
            }
        } else {
            getLogger().info("[CropStorage] System disabled in config.");
        }
    }
}
