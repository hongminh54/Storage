package net.danh.storage.Manager.Mythic;

import net.danh.storage.API.events.MythicStorageDepositEvent;
import net.danh.storage.API.events.MythicStorageWithdrawEvent;
import net.danh.storage.Database.PlayerData;
import net.danh.storage.MythicMobs.MythicMobsHelper;
import net.danh.storage.Storage;
import net.danh.storage.Utils.File;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.permissions.PermissionAttachmentInfo;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class MythicStorageManager {

    private static final String STORAGE_LIMIT_PERMISSION_PREFIX = "storage.mythicstorage.storage.";
    private static final String STORAGE_LIMIT_PERMISSION_MAX_PREFIX = "storage.mythicstorage.storage.max.";
    private static final HashMap<String, Set<String>> disabledAutoPickupItems = new HashMap<>();
    private static final HashMap<UUID, Boolean> groundStoreToggle = new HashMap<>();
    private static final String GROUND_STORE_DATA_PREFIX = "mythicgroundstore:";
    private static final String MAX_OVERRIDE_DATA_PREFIX = "mythicmaxoverride:";
    private static final HashMap<UUID, Integer> maxOverrideData = new HashMap<>();
    public static HashMap<String, Integer> playerdata = new HashMap<>();
    public static HashMap<UUID, Boolean> toggle = new HashMap<>();
    public static HashMap<UUID, Integer> playermaxdata = new HashMap<>();
    private static List<String> configuredDrops = new ArrayList<>();
    private static List<String> invalidItems = new ArrayList<>();
    private static MythicMobsHelper mythicMobsHelper;
    private static boolean systemEnabled = false;

    public static void initialize() {
        Storage.getStorage().getLogger().info("[MythicStorage] Initializing MythicStorage feature...");
        mythicMobsHelper = new MythicMobsHelper();
        if (!mythicMobsHelper.isInitialized()) {
            Storage.getStorage().getLogger()
                    .warning("[MythicStorage] MythicMobs not found! MythicStorage feature will be disabled.");
            systemEnabled = false;
            return;
        }

        systemEnabled = File.getMythicStorageConfig().getBoolean("settings.enabled", true);
        if (!systemEnabled) {
            Storage.getStorage().getLogger().info("[MythicStorage] Feature is disabled in config");
            return;
        }

        loadConfiguredDrops();

        Storage.getStorage().getLogger().info("[MythicStorage] ========== INITIALIZATION SUMMARY ==========");
        Storage.getStorage().getLogger().info("[MythicStorage] Valid items loaded: " + configuredDrops.size());

        if (!invalidItems.isEmpty()) {
            Storage.getStorage().getLogger().warning("[MythicStorage] Invalid items found: " + invalidItems.size());
            Storage.getStorage().getLogger()
                    .warning("[MythicStorage] Items with errors: " + String.join(", ", invalidItems));
            Storage.getStorage().getLogger().warning("[MythicStorage] These items will NOT appear in the GUI!");
            Storage.getStorage().getLogger()
                    .warning("[MythicStorage] Check the detailed error messages above for fixes");
        } else {
            Storage.getStorage().getLogger()
                    .info("[MythicStorage] No invalid items found - all items loaded successfully!");
        }

        Storage.getStorage().getLogger().info("[MythicStorage] Initialization completed!");
        Storage.getStorage().getLogger().info("[MythicStorage] ===================================");
    }

    private static Integer parseMaxOverride(@NotNull String part) {
        if (part == null || part.isEmpty()) {
            return null;
        }
        if (!part.startsWith(MAX_OVERRIDE_DATA_PREFIX)) {
            return null;
        }
        String raw = part.substring(MAX_OVERRIDE_DATA_PREFIX.length()).trim();
        if (raw.isEmpty()) {
            return null;
        }
        try {
            return Integer.parseInt(raw);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    public static boolean isSystemEnabled() {
        return systemEnabled && mythicMobsHelper != null && mythicMobsHelper.isInitialized();
    }

    public static boolean isGroundStoreSystemEnabled() {
        return File.getMythicStorageConfig().getBoolean(
                "ground_store.enabled",
                true);
    }

    public static boolean isGroundStoreEnabled(@NotNull Player player) {
        if (!isSystemEnabled() || !isGroundStoreSystemEnabled()) {
            return false;
        }

        UUID playerId = player.getUniqueId();
        Boolean status = groundStoreToggle.get(playerId);
        if (status != null) {
            return status;
        }

        PlayerData data = Storage.dataStorage.getData(player.getName());
        status = data != null ? parseGroundStoreStatus(data.getData()) : null;
        if (status == null) {
            status = File.getMythicStorageConfig().getBoolean(
                    "ground_store.default_enabled",
                    false);
        }
        groundStoreToggle.put(playerId, status);
        return status;
    }

    public static boolean toggleGroundStore(@NotNull Player player) {
        boolean current = isGroundStoreEnabled(player);
        boolean next = !current;
        groundStoreToggle.put(player.getUniqueId(), next);
        savePlayerData(player);
        return next;
    }

    public static boolean isGroundStoreItemAllowed(@NotNull String itemName) {
        if (!isConfiguredDrop(itemName)) {
            return false;
        }

        List<String> allowed = File.getMythicStorageConfig().getStringList(
                "ground_store.allowed_items");
        if (allowed == null || allowed.isEmpty()) {
            return false;
        }
        return allowed.contains(itemName);
    }

    public static MythicMobsHelper getMythicMobsHelper() {
        return mythicMobsHelper;
    }

    private static void loadConfiguredDrops() {
        List<String> items = File.getMythicStorageConfig().getStringList("items_drop");
        if (items == null) {
            configuredDrops = new ArrayList<>();
            invalidItems = new ArrayList<>();
            return;
        }

        configuredDrops = new ArrayList<>();
        invalidItems = new ArrayList<>();

        for (String itemName : items) {
            if (itemName == null || itemName.trim().isEmpty()) {
                invalidItems.add(itemName);
                Storage.getStorage().getLogger().severe("========================================");
                Storage.getStorage().getLogger().severe("[MythicStorage] INVALID ITEM CONFIGURATION");
                Storage.getStorage().getLogger().severe("Item: <empty or null>");
                Storage.getStorage().getLogger().severe("Error: Item name is empty or null");
                Storage.getStorage().getLogger()
                        .severe("Fix: Remove empty lines from items_drop list in mythicstorage.yml");
                Storage.getStorage().getLogger().severe("========================================");
                continue;
            }

            if (!mythicMobsHelper.isValidMythicItem(itemName)) {
                invalidItems.add(itemName);
                Storage.getStorage().getLogger().severe("========================================");
                Storage.getStorage().getLogger().severe("[MythicStorage] INVALID ITEM CONFIGURATION");
                Storage.getStorage().getLogger().severe("Item: " + itemName);
                Storage.getStorage().getLogger().severe("Error: Item does not exist in MythicMobs configuration");
                Storage.getStorage().getLogger().severe("Possible causes:");
                Storage.getStorage().getLogger().severe("  1. Item ID is misspelled or does not exist");
                Storage.getStorage().getLogger().severe("  2. MythicMobs has not loaded this item yet");
                Storage.getStorage().getLogger().severe("  3. Item configuration file is missing or invalid");
                Storage.getStorage().getLogger()
                        .severe("Fix: Use the item ID (not filename) from your MythicMobs configuration");
                Storage.getStorage().getLogger().severe(
                        "Example: In 'MythicMobs/Items/weapons.yml' with item 'crown:', use 'crown' in items_drop");
                Storage.getStorage().getLogger()
                        .severe("Note: The item ID is the key name inside the yml file, NOT the filename");
                Storage.getStorage().getLogger().severe("========================================");
                continue;
            }
            configuredDrops.add(itemName);
        }
    }

    public static void reloadConfiguredDrops() {
        if (!isSystemEnabled())
            return;
        loadConfiguredDrops();

        Storage.getStorage().getLogger().info("[MythicStorage] ========== RELOAD SUMMARY ==========");
        Storage.getStorage().getLogger().info("[MythicStorage] Valid items loaded: " + configuredDrops.size());

        if (!invalidItems.isEmpty()) {
            Storage.getStorage().getLogger().warning("[MythicStorage] Invalid items found: " + invalidItems.size());
            Storage.getStorage().getLogger()
                    .warning("[MythicStorage] Items with errors: " + String.join(", ", invalidItems));
            Storage.getStorage().getLogger().warning("[MythicStorage] These items will NOT appear in the GUI!");
            Storage.getStorage().getLogger()
                    .warning("[MythicStorage] Check the detailed error messages above for fixes");
        } else {
            Storage.getStorage().getLogger()
                    .info("[MythicStorage] No invalid items found - all items loaded successfully!");
        }

        Storage.getStorage().getLogger().info("[MythicStorage] ===================================");
    }

    public static List<String> getConfiguredDrops() {
        return new ArrayList<>(configuredDrops);
    }

    public static List<String> getInvalidItems() {
        return new ArrayList<>(invalidItems);
    }

    public static boolean hasInvalidItems() {
        return !invalidItems.isEmpty();
    }

    public static boolean isConfiguredDrop(@NotNull String itemName) {
        return configuredDrops.contains(itemName);
    }

    @NotNull
    public static String getItemDisplayNameOrId(@NotNull String itemName, @NotNull Player player) {
        if (!isSystemEnabled() || mythicMobsHelper == null) {
            return itemName;
        }

        String displayName = mythicMobsHelper.getItemDisplayName(itemName);

        if (displayName == null || displayName.trim().isEmpty()) {
            return itemName;
        }

        return displayName;
    }

    public static int getPlayerItem(@NotNull Player player, @NotNull String itemName) {
        return playerdata.getOrDefault(player.getName() + "_" + itemName, 0);
    }

    public static int getMaxStorage(@NotNull Player player) {
        return playermaxdata.getOrDefault(player.getUniqueId(),
                File.getMythicStorageConfig().getInt(
                        "settings.default_max_storage",
                        100000));
    }

    public static void setMaxStorageOverride(@NotNull Player player,
                                             int maxStorage) {
        maxOverrideData.put(player.getUniqueId(), Math.max(0, maxStorage));
    }

    public static void clearMaxStorageOverride(@NotNull Player player) {
        maxOverrideData.remove(player.getUniqueId());
    }

    public static int getPermissionMaxStorage(@NotNull Player player) {
        int defaultMax = File.getMythicStorageConfig().getInt(
                "settings.default_max_storage",
                100000);

        if (player.isOp() || player.hasPermission("storage.admin")) {
            return defaultMax;
        }

        int bestLimit = -1;
        int bestPriority = Integer.MIN_VALUE;

        // Numeric permission: storage.storage.max.<n>
        for (PermissionAttachmentInfo pai : player.getEffectivePermissions()) {
            String perm = pai.getPermission();
            if (perm == null || !pai.getValue()) {
                continue;
            }
            if (!perm.startsWith(STORAGE_LIMIT_PERMISSION_MAX_PREFIX)) {
                continue;
            }

            String numberPart = perm.substring(
                    STORAGE_LIMIT_PERMISSION_MAX_PREFIX.length());
            int value;
            try {
                value = Integer.parseInt(numberPart);
            } catch (NumberFormatException ignored) {
                continue;
            }
            if (value > bestLimit) {
                bestLimit = value;
            }
        }

        ConfigurationSection section = File.getMythicStorageConfig().getConfigurationSection(
                "mythic_storage_permissions");
        if (section != null) {
            for (String key : section.getKeys(false)) {
                if (key == null || key.trim().isEmpty()) {
                    continue;
                }
                String permission = STORAGE_LIMIT_PERMISSION_PREFIX + key;
                if (!player.hasPermission(permission)) {
                    continue;
                }

                int limit = section.getInt(key + ".max_storage", -1);
                int priority = section.getInt(key + ".priority", 0);
                if (limit < 0) {
                    continue;
                }
                if (priority > bestPriority) {
                    bestPriority = priority;
                    bestLimit = limit;
                }
            }
        }

        return bestLimit >= 0 ? bestLimit : defaultMax;
    }

    public static boolean getToggleStatus(@NotNull Player player) {
        UUID playerId = player.getUniqueId();
        Boolean status = toggle.get(playerId);
        if (status == null) {
            status = File.getMythicStorageConfig().getBoolean("settings.default_auto_pickup", false);
            toggle.put(playerId, status);
        }
        return status;
    }

    public static void setToggleStatus(@NotNull Player player, boolean status) {
        toggle.put(player.getUniqueId(), status);
    }

    public static boolean isAutoPickupEnabledForItem(@NotNull Player player,
                                                     @NotNull String itemName) {
        if (!getToggleStatus(player)) {
            return false;
        }
        Set<String> disabledItems = disabledAutoPickupItems.get(player.getName());
        if (disabledItems == null || disabledItems.isEmpty()) {
            return true;
        }
        return !disabledItems.contains(itemName);
    }

    public static boolean toggleItemAutoPickup(@NotNull Player player,
                                               @NotNull String itemName) {
        String playerName = player.getName();
        Set<String> disabledItems = disabledAutoPickupItems.computeIfAbsent(playerName, k -> new HashSet<>());

        if (disabledItems.contains(itemName)) {
            disabledItems.remove(itemName);
            savePlayerData(player);
            return true;
        }

        disabledItems.add(itemName);
        savePlayerData(player);
        return false;
    }

    public static boolean addItemAmount(@NotNull Player player, @NotNull String itemName, int amount) {
        return addItemAmount(player, itemName, amount, true);
    }

    public static boolean addItemAmount(@NotNull Player player, @NotNull String itemName, int amount,
                                        boolean fireEvent) {
        if (!isSystemEnabled() || !isConfiguredDrop(itemName) || amount <= 0)
            return false;

        String key = player.getName() + "_" + itemName;
        int current = playerdata.getOrDefault(key, 0);
        int max = getMaxStorage(player);
        if (current >= max)
            return false;

        int amountToAdd = Math.min(amount, max - current);
        if (amountToAdd <= 0)
            return false;

        if (fireEvent) {
            MythicStorageDepositEvent event = new MythicStorageDepositEvent(player, itemName, amountToAdd);
            Bukkit.getPluginManager().callEvent(event);
            if (event.isCancelled())
                return false;
            amountToAdd = Math.min(event.getAmount(), max - current);
        }

        playerdata.put(key, current + amountToAdd);
        return true;
    }

    public static boolean removeItemAmount(@NotNull Player player, @NotNull String itemName, int amount) {
        return removeItemAmount(player, itemName, amount, true);
    }

    public static boolean removeItemAmount(@NotNull Player player, @NotNull String itemName, int amount,
                                           boolean fireEvent) {
        if (!isSystemEnabled() || amount <= 0)
            return false;

        String key = player.getName() + "_" + itemName;
        int current = playerdata.getOrDefault(key, 0);
        if (current < amount)
            return false;

        int amountToRemove = amount;

        if (fireEvent) {
            MythicStorageWithdrawEvent event = new MythicStorageWithdrawEvent(player, itemName, amountToRemove);
            Bukkit.getPluginManager().callEvent(event);
            if (event.isCancelled())
                return false;
            amountToRemove = event.getAmount();
        }

        int newValue = current - amountToRemove;
        if (newValue <= 0) {
            playerdata.remove(key);
        } else {
            playerdata.put(key, newValue);
        }
        return true;
    }

    public static void setItemAmount(@NotNull Player player, @NotNull String itemName, int amount) {
        String key = player.getName() + "_" + itemName;
        if (amount <= 0) {
            playerdata.remove(key);
        } else {
            playerdata.put(key, amount);
        }
    }

    public static void loadPlayerData(@NotNull Player player) {
        if (!isSystemEnabled())
            return;

        disabledAutoPickupItems.remove(player.getName());

        UUID playerId = player.getUniqueId();

        PlayerData data = Storage.dataStorage.getData(player.getName());
        if (data == null) {
            int permissionMax = Math.max(0, getPermissionMaxStorage(player));
            playermaxdata.put(playerId, permissionMax);
            toggle.put(playerId, File.getMythicStorageConfig().getBoolean(
                    "settings.default_auto_pickup",
                    false));
            return;
        }

        String dataString = data.getData();
        if (dataString == null || dataString.isEmpty()) {
            int permissionMax = Math.max(0, getPermissionMaxStorage(player));
            playermaxdata.put(playerId, permissionMax);
            toggle.put(playerId, File.getMythicStorageConfig().getBoolean(
                    "settings.default_auto_pickup",
                    false));
            return;
        }

        // Parse data format: "mythic:item1:amount1,item2:amount2;mythictoggle:true"
        String[] dataParts = dataString.split(";");
        for (String part : dataParts) {
            if (part == null || part.isEmpty())
                continue;

            if (part.startsWith("mythic:")) {
                String mythicData = part.substring(7);
                if (mythicData.isEmpty())
                    continue;

                String[] items = mythicData.split(",");
                for (String item : items) {
                    if (item == null || item.isEmpty())
                        continue;
                    String[] itemParts = item.split(":");
                    if (itemParts.length == 2) {
                        String key = player.getName() + "_" + itemParts[0];
                        try {
                            int value = Integer.parseInt(itemParts[1]);
                            if (value > 0) {
                                playerdata.put(key, value);
                            }
                        } catch (NumberFormatException ignored) {
                        }
                    }
                }
            } else if (part.startsWith("mythictoggle:")) {
                String raw = part.substring("mythictoggle:".length()).trim();
                toggle.put(playerId, "true".equalsIgnoreCase(raw));
            } else if (part.startsWith(MAX_OVERRIDE_DATA_PREFIX)) {
                Integer parsed = parseMaxOverride(part);
                if (parsed != null) {
                    setMaxStorageOverride(player, Math.max(0, parsed));
                } else {
                    clearMaxStorageOverride(player);
                }
            } else if (part.startsWith("mythicautopickupoff:")) {
                String disabledData = part.substring("mythicautopickupoff:"
                        .length());
                if (!disabledData.isEmpty()) {
                    Set<String> disabledItems = new HashSet<>();
                    for (String raw : disabledData.split(",")) {
                        if (raw == null)
                            continue;
                        String item = raw.trim();
                        if (item.isEmpty())
                            continue;
                        if (isConfiguredDrop(item)) {
                            disabledItems.add(item);
                        }
                    }
                    if (!disabledItems.isEmpty()) {
                        disabledAutoPickupItems.put(player.getName(),
                                disabledItems);
                    }
                }
            } else if (part.startsWith(GROUND_STORE_DATA_PREFIX)) {
                Boolean status = parseGroundStoreStatus(part);
                if (status != null) {
                    groundStoreToggle.put(playerId, status);
                }
            }
        }

        int databaseMax = Math.max(0, data.getMax());
        int permissionMax = Math.max(0, getPermissionMaxStorage(player));
        Integer overrideMax = maxOverrideData.get(playerId);
        int resolvedMax;
        if (overrideMax != null) {
            resolvedMax = overrideMax;
        } else {
            resolvedMax = File.resolveMaxStorage(
                    File.getMythicStorageConfig(),
                    "settings.max_storage_mode",
                    databaseMax,
                    permissionMax);
        }
        playermaxdata.put(playerId, Math.max(0, resolvedMax));

        if (!groundStoreToggle.containsKey(playerId)) {
            groundStoreToggle.put(playerId, File.getMythicStorageConfig()
                    .getBoolean("ground_store.default_enabled", false));
        }
    }

    public static void savePlayerData(@NotNull Player player) {
        if (!isSystemEnabled())
            return;

        StringBuilder mythicData = new StringBuilder();
        String playerName = player.getName();
        UUID playerId = player.getUniqueId();

        for (String drop : configuredDrops) {
            String key = playerName + "_" + drop;
            if (playerdata.containsKey(key)) {
                int amount = playerdata.get(key);
                if (amount > 0) {
                    if (mythicData.length() > 0) {
                        mythicData.append(",");
                    }
                    mythicData.append(drop).append(":").append(amount);
                }
            }
        }

        PlayerData existingData = Storage.dataStorage.getData(playerName);
        String existingDataString = existingData != null ? existingData.getData() : "";

        StringBuilder finalData = new StringBuilder();
        if (existingDataString != null && !existingDataString.isEmpty()) {
            String[] existingParts = existingDataString.split(";");

            for (String part : existingParts) {
                if (!part.startsWith("mythic:")
                        && !part.startsWith("mythictoggle:")
                        && !part.startsWith("mythicautopickupoff:")
                        && !part.startsWith(GROUND_STORE_DATA_PREFIX)
                        && !part.startsWith(MAX_OVERRIDE_DATA_PREFIX)
                        && !part.isEmpty()) {
                    if (finalData.length() > 0) {
                        finalData.append(";");
                    }
                    finalData.append(part);
                }
            }
        }

        if (mythicData.length() > 0) {
            if (finalData.length() > 0) {
                finalData.append(";");
            }
            finalData.append("mythic:").append(mythicData);
        }

        if (toggle.containsKey(playerId)) {
            if (finalData.length() > 0) {
                finalData.append(";");
            }
            finalData.append("mythictoggle:").append(toggle.get(playerId));
        }

        Integer maxOverride = maxOverrideData.get(playerId);
        if (maxOverride != null) {
            if (finalData.length() > 0) {
                finalData.append(";");
            }
            finalData.append(MAX_OVERRIDE_DATA_PREFIX)
                    .append(Math.max(0, maxOverride));
        }

        if (groundStoreToggle.containsKey(playerId)) {
            if (finalData.length() > 0) {
                finalData.append(";");
            }
            finalData.append(GROUND_STORE_DATA_PREFIX)
                    .append(groundStoreToggle.get(playerId));
        }

        Set<String> disabledItems = disabledAutoPickupItems.get(playerName);
        if (disabledItems != null && !disabledItems.isEmpty()) {
            StringBuilder disabledData = new StringBuilder();
            for (String item : configuredDrops) {
                if (!disabledItems.contains(item)) {
                    continue;
                }
                if (disabledData.length() > 0) {
                    disabledData.append(",");
                }
                disabledData.append(item);
            }

            if (disabledData.length() > 0) {
                if (finalData.length() > 0) {
                    finalData.append(";");
                }
                finalData.append("mythicautopickupoff:")
                        .append(disabledData);
            }
        }

        int maxStorage;
        if (existingData != null) {
            maxStorage = existingData.getMax();
        } else {
            maxStorage = File.getMythicStorageConfig().getInt(
                    "settings.default_max_storage",
                    100000);
        }

        boolean autoPickup = toggle.getOrDefault(playerId,
                File.getMythicStorageConfig().getBoolean(
                        "settings.default_auto_pickup",
                        false));

        PlayerData newData = new PlayerData(
                playerName,
                finalData.toString(),
                Math.max(0, maxStorage),
                autoPickup);

        if (existingData == null) {
            Storage.dataStorage.createTable(newData);
        } else {
            Storage.dataStorage.updateTable(newData);
        }
    }

    public static void cleanupPlayerData(@NotNull Player player) {
        if (player == null)
            return;
        UUID playerId = player.getUniqueId();
        toggle.remove(playerId);
        groundStoreToggle.remove(playerId);
        playermaxdata.remove(playerId);
        clearMaxStorageOverride(player);
        disabledAutoPickupItems.remove(player.getName());

        String playerName = player.getName();
        playerdata.entrySet().removeIf(entry -> entry.getKey().startsWith(playerName + "_"));
    }

    private static Boolean parseGroundStoreStatus(String data) {
        if (data == null || data.isEmpty()) {
            return null;
        }

        String raw = data;
        if (!raw.startsWith(GROUND_STORE_DATA_PREFIX)) {
            int idx = raw.indexOf(GROUND_STORE_DATA_PREFIX);
            if (idx < 0) {
                return null;
            }
            raw = raw.substring(idx);
        }

        raw = raw.substring(GROUND_STORE_DATA_PREFIX.length());
        int end = raw.indexOf(';');
        if (end >= 0) {
            raw = raw.substring(0, end);
        }
        raw = raw.trim().toLowerCase(Locale.ROOT);
        if (raw.isEmpty()) {
            return null;
        }
        return "true".equals(raw);
    }

    public static HashMap<String, Integer> getPlayerAllItems(@NotNull Player player) {
        HashMap<String, Integer> items = new HashMap<>();
        String playerName = player.getName();

        for (String drop : configuredDrops) {
            String key = playerName + "_" + drop;
            if (playerdata.containsKey(key)) {
                items.put(drop, playerdata.get(key));
            }
        }

        return items;
    }

    public static int getPlayerItem(@NotNull String playerName, @NotNull String itemName) {
        return playerdata.getOrDefault(playerName + "_" + itemName, 0);
    }

    public static int getMaxStorage() {
        return File.getMythicStorageConfig().getInt("settings.default_max_storage", 100000);
    }

    public static void loadOfflinePlayerData(@NotNull String playerName) {
        if (!isSystemEnabled())
            return;

        Player onlinePlayer = Bukkit.getPlayer(playerName);
        if (onlinePlayer != null) {
            return; // Player is online, data already loaded
        }

        for (String key : playerdata.keySet()) {
            if (key.startsWith(playerName + "_")) {
                return; // Data already loaded
            }
        }

        PlayerData data = Storage.dataStorage.getData(playerName);
        if (data == null) {
            return; // Player has no data
        }

        String dataString = data.getData();
        if (dataString == null || dataString.isEmpty()) {
            return;
        }

        Set<String> disabledItems = null;

        String[] dataParts = dataString.split(";");
        for (String part : dataParts) {
            if (part == null || part.isEmpty())
                continue;

            if (part.startsWith("mythic:")) {
                String mythicData = part.substring(7);
                if (mythicData.isEmpty())
                    continue;

                String[] items = mythicData.split(",");
                for (String item : items) {
                    if (item == null || item.isEmpty())
                        continue;
                    String[] itemParts = item.split(":");
                    if (itemParts.length == 2) {
                        String key = playerName + "_" + itemParts[0];
                        try {
                            int value = Integer.parseInt(itemParts[1]);
                            if (value > 0) {
                                playerdata.put(key, value);
                            }
                        } catch (NumberFormatException ignored) {
                        }
                    }
                }
            } else if (part.startsWith("mythicautopickupoff:")) {
                String disabledData = part.substring("mythicautopickupoff:"
                        .length());
                if (!disabledData.isEmpty()) {
                    disabledItems = new HashSet<>();
                    for (String raw : disabledData.split(",")) {
                        if (raw == null)
                            continue;
                        String item = raw.trim();
                        if (item.isEmpty())
                            continue;
                        if (isConfiguredDrop(item)) {
                            disabledItems.add(item);
                        }
                    }
                }
            }
        }

        if (disabledItems != null && !disabledItems.isEmpty()) {
            disabledAutoPickupItems.put(playerName, disabledItems);
        }

    }

    public static void cleanupOfflinePlayerData(@NotNull String playerName) {
        Player onlinePlayer = Bukkit.getPlayer(playerName);
        if (onlinePlayer != null) {
            return;
        }

        playerdata.entrySet().removeIf(entry -> entry.getKey().startsWith(playerName + "_"));

        disabledAutoPickupItems.remove(playerName);
    }

    public static String getDisplayName(String internalName) {
        if (internalName == null) return "Unknown";
        return File.getMythicStorageConfig().getString("items." + internalName, internalName);
    }

    public static void addItemAmount(@NotNull String playerName, @NotNull String internalName, int amount) {
        if (!isSystemEnabled() || amount <= 0) return;
        int current = getPlayerItem(playerName, internalName);
        playerdata.put(playerName + "_" + internalName, current + amount);
    }

    public static void removeItemAmount(@NotNull String playerName, @NotNull String internalName, int amount) {
        if (!isSystemEnabled() || amount <= 0) return;
        int current = getPlayerItem(playerName, internalName);
        playerdata.put(playerName + "_" + internalName, Math.max(0, current - amount));
    }
}
