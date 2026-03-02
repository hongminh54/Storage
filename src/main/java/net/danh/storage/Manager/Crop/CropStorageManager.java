package net.danh.storage.Manager.Crop;

import net.danh.storage.API.events.*;
import net.danh.storage.Database.PlayerData;
import net.danh.storage.Storage;
import net.danh.storage.Utils.File;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.permissions.PermissionAttachmentInfo;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class CropStorageManager {

    private static final String STORAGE_LIMIT_PERMISSION_PREFIX = "storage.cropstorage.storage.";
    private static final String STORAGE_LIMIT_PERMISSION_MAX_PREFIX = "storage.cropstorage.storage.max.";
    private static final HashMap<String, Set<String>> disabledAutoPickupItems = new HashMap<>();
    private static final HashMap<UUID, Boolean> groundStoreToggle = new HashMap<>();
    private static final String GROUND_STORE_DATA_PREFIX = "cropgroundstore:";
    private static final String MAX_OVERRIDE_DATA_PREFIX = "cropmaxoverride:";
    private static final String AUTO_SELL_DATA_PREFIX = "cropautosell:";
    private static final HashMap<UUID, Integer> maxOverrideData = new HashMap<>();
    private static final HashMap<String, Set<String>> autoSellItems = new HashMap<>();
    private static final Set<String> pendingAutoSell = new HashSet<>();
    private static final Map<String, Long> lastAutoSellAt = new HashMap<>();
    public static HashMap<String, Integer> playerdata = new HashMap<>();
    public static HashMap<UUID, Boolean> toggle = new HashMap<>();
    public static HashMap<UUID, Integer> playermaxdata = new HashMap<>();
    private static List<String> configuredDrops = new ArrayList<>();
    private static List<String> invalidItems = new ArrayList<>();
    private static boolean systemEnabled = false;

    @NotNull
    public static String getSellableSymbol(@NotNull String itemName) {
        String normalized = itemName.replace(":", ";").toUpperCase();

        ConfigurationSection worthSection = File.getCropStorageConfig().getConfigurationSection("worth");
        if (worthSection == null) {
            return File.getMessage().getString("cropstorage.sellable.no", "&c✘");
        }

        String worthKey = resolveWorthKey(worthSection, normalized);
        if (worthKey == null) {
            return File.getMessage().getString("cropstorage.sellable.no", "&c✘");
        }

        boolean sellable = parseWorthValue(worthSection, worthKey) > 0;
        return File.getMessage().getString(
                sellable ? "cropstorage.sellable.yes" : "cropstorage.sellable.no",
                sellable ? "&a✔" : "&c✘"
        ).replace("#item#", itemName);
    }

    private static double parseWorthValue(ConfigurationSection section, String worthKey) {
        if (section == null || worthKey == null) {
            return 0D;
        }

        Object raw = section.get(worthKey);
        if (raw instanceof java.lang.Number) {
            return ((java.lang.Number) raw).doubleValue();
        }

        if (raw instanceof String) {
            String value = ((String) raw).trim();
            if (value.isEmpty()) {
                return 0D;
            }
            String[] parts = value.split(";", -1);
            try {
                return Double.parseDouble(parts[0].trim());
            } catch (NumberFormatException ignored) {
                return 0D;
            }
        }

        return section.getDouble(worthKey);
    }

    private static String resolveWorthKey(@NotNull ConfigurationSection section, @NotNull String itemKey) {
        if (section.contains(itemKey)) {
            return itemKey;
        }

        if (itemKey.endsWith(";0")) {
            String noData = itemKey.substring(0, itemKey.length() - 2);
            if (section.contains(noData)) {
                return noData;
            }
        }

        String withZero = itemKey + ";0";
        if (section.contains(withZero)) {
            return withZero;
        }

        return null;
    }

    public static void initialize() {
        Storage.getStorage().getLogger().info("[CropStorage] Initializing CropStorage feature...");

        systemEnabled = File.getCropStorageConfig().getBoolean("settings.enabled", true);
        if (!systemEnabled) {
            Storage.getStorage().getLogger().info("[CropStorage] Feature is disabled in config");
            return;
        }

        loadConfiguredDrops();

        Storage.getStorage().getLogger().info("[CropStorage] ========== INITIALIZATION SUMMARY ==========");
        Storage.getStorage().getLogger().info("[CropStorage] Valid items loaded: " + configuredDrops.size());

        if (!invalidItems.isEmpty()) {
            Storage.getStorage().getLogger().warning("[CropStorage] Invalid items found: " + invalidItems.size());
            Storage.getStorage().getLogger()
                    .warning("[CropStorage] Items with errors: " + String.join(", ", invalidItems));
            Storage.getStorage().getLogger().warning("[CropStorage] These items will NOT appear in the GUI!");
        } else {
            Storage.getStorage().getLogger()
                    .info("[CropStorage] No invalid items found - all items loaded successfully!");
        }

        Storage.getStorage().getLogger().info("[CropStorage] Initialization completed!");
        Storage.getStorage().getLogger().info("[CropStorage] ===================================");
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
        return systemEnabled;
    }

    public static boolean isGroundStoreSystemEnabled() {
        return File.getCropStorageConfig().getBoolean(
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
            status = File.getCropStorageConfig().getBoolean(
                    "ground_store.default_enabled",
                    false);
        }
        groundStoreToggle.put(playerId, status);
        return status;
    }

    public static boolean toggleGroundStore(@NotNull Player player) {
        return toggleGroundStore(player, false);
    }

    public static boolean toggleGroundStore(@NotNull Player player, boolean fireEvent) {
        boolean current = isGroundStoreEnabled(player);
        boolean next = !current;

        if (fireEvent) {
            CropStorageGroundStoreToggleEvent event = new CropStorageGroundStoreToggleEvent(player, next);
            Bukkit.getPluginManager().callEvent(event);
            if (event.isCancelled()) {
                return current;
            }
            next = event.getNewState();
        }

        setGroundStoreEnabled(player, next);
        return next;
    }

    public static void setGroundStoreEnabled(@NotNull Player player, boolean enabled) {
        groundStoreToggle.put(player.getUniqueId(), enabled);
        savePlayerData(player);
    }

    public static boolean isGroundStoreItemAllowed(@NotNull String itemName) {
        if (!isConfiguredDrop(itemName)) {
            return false;
        }

        List<String> allowed = File.getCropStorageConfig().getStringList(
                "ground_store.allowed_items");
        if (allowed == null || allowed.isEmpty()) {
            return false;
        }
        return allowed.contains(itemName);
    }

    private static void loadConfiguredDrops() {
        List<String> items = File.getCropStorageConfig().getStringList("items_drop");
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
                Storage.getStorage().getLogger().severe("[CropStorage] INVALID ITEM CONFIGURATION");
                Storage.getStorage().getLogger().severe("Item: <empty or null>");
                Storage.getStorage().getLogger().severe("Error: Item name is empty or null");
                Storage.getStorage().getLogger()
                        .severe("Fix: Remove empty lines from items_drop list in cropstorage.yml");
                Storage.getStorage().getLogger().severe("========================================");
                continue;
            }

            // Validate against Material enum
            try {
                Material mat = Material.valueOf(itemName.toUpperCase());
                if (mat == null) {
                    throw new IllegalArgumentException("Material not found");
                }
            } catch (IllegalArgumentException e) {
                invalidItems.add(itemName);
                Storage.getStorage().getLogger().severe("========================================");
                Storage.getStorage().getLogger().severe("[CropStorage] INVALID ITEM CONFIGURATION");
                Storage.getStorage().getLogger().severe("Item: " + itemName);
                Storage.getStorage().getLogger().severe("Error: Material does not exist in Minecraft");
                Storage.getStorage().getLogger().severe("Fix: Use a valid Material name (e.g., WHEAT, CARROT, POTATO)");
                Storage.getStorage().getLogger().severe("========================================");
                continue;
            }
            configuredDrops.add(itemName.toUpperCase());
        }
    }

    public static void reloadConfiguredDrops() {
        if (!isSystemEnabled())
            return;
        loadConfiguredDrops();

        Storage.getStorage().getLogger().info("[CropStorage] ========== RELOAD SUMMARY ==========");
        Storage.getStorage().getLogger().info("[CropStorage] Valid items loaded: " + configuredDrops.size());

        if (!invalidItems.isEmpty()) {
            Storage.getStorage().getLogger().warning("[CropStorage] Invalid items found: " + invalidItems.size());
            Storage.getStorage().getLogger()
                    .warning("[CropStorage] Items with errors: " + String.join(", ", invalidItems));
        } else {
            Storage.getStorage().getLogger()
                    .info("[CropStorage] No invalid items found - all items loaded successfully!");
        }

        Storage.getStorage().getLogger().info("[CropStorage] ===================================");
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
        return configuredDrops.contains(itemName.toUpperCase());
    }

    @NotNull
    public static String getItemDisplayName(@NotNull String itemName) {
        // Try custom display name from config
        String displayName = File.getCropStorageConfig().getString(
                "crop_display_names." + itemName.toUpperCase());
        if (displayName != null && !displayName.trim().isEmpty()) {
            return displayName;
        }

        // Format Material name: WHEAT -> Wheat, NETHER_WART -> Nether Wart
        String[] parts = itemName.toLowerCase().split("_");
        StringBuilder formatted = new StringBuilder();
        for (String part : parts) {
            if (formatted.length() > 0) {
                formatted.append(" ");
            }
            formatted.append(part.substring(0, 1).toUpperCase()).append(part.substring(1));
        }
        return formatted.toString();
    }

    /**
     * Get the Material for a crop item.
     */
    @NotNull
    public static Material getItemMaterial(@NotNull String itemName) {
        try {
            return Material.valueOf(itemName.toUpperCase());
        } catch (IllegalArgumentException e) {
            return Material.BARRIER;
        }
    }

    public static int getPlayerItem(@NotNull Player player, @NotNull String itemName) {
        return playerdata.getOrDefault(player.getName() + "_crop_" + itemName.toUpperCase(), 0);
    }

    public static boolean hasPlayerItem(@NotNull Player player, @NotNull String itemName) {
        return playerdata.containsKey(player.getName() + "_crop_" + itemName.toUpperCase());
    }

    public static int getMaxStorage(@NotNull Player player) {
        return playermaxdata.getOrDefault(player.getUniqueId(),
                File.getCropStorageConfig().getInt(
                        "settings.default_max_storage",
                        100000));
    }

    public static Integer getMaxStorageOverride(@NotNull Player player) {
        return maxOverrideData.get(player.getUniqueId());
    }

    public static void setMaxStorageOverride(@NotNull Player player,
                                             int maxStorage) {
        maxOverrideData.put(player.getUniqueId(), Math.max(0, maxStorage));
    }

    public static void clearMaxStorageOverride(@NotNull Player player) {
        maxOverrideData.remove(player.getUniqueId());
    }

    public static int getPermissionMaxStorage(@NotNull Player player) {
        int defaultMax = File.getCropStorageConfig().getInt(
                "settings.default_max_storage",
                100000);

        if (player.isOp() || player.hasPermission("storage.admin")) {
            return defaultMax;
        }

        int bestLimit = -1;
        int bestPriority = Integer.MIN_VALUE;

        // Numeric permission: storage.cropstorage.storage.max.<n>
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

        ConfigurationSection section = File.getCropStorageConfig().getConfigurationSection(
                "crop_storage_permissions");
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

    public static void refreshPermissionMaxStorage(@NotNull Player player) {
        int computed = getPermissionMaxStorage(player);
        playermaxdata.put(player.getUniqueId(), Math.max(0, computed));
    }

    public static boolean getToggleStatus(@NotNull Player player) {
        UUID playerId = player.getUniqueId();
        Boolean status = toggle.get(playerId);
        if (status == null) {
            status = File.getCropStorageConfig().getBoolean("settings.default_auto_pickup", false);
            toggle.put(playerId, status);
        }
        return status;
    }

    public static void setToggleStatus(@NotNull Player player, boolean status) {
        toggle.put(player.getUniqueId(), status);
    }

    public static boolean setToggleStatus(@NotNull Player player, boolean status, boolean fireEvent) {
        boolean current = getToggleStatus(player);
        boolean next = status;

        if (fireEvent) {
            CropStorageToggleEvent event = new CropStorageToggleEvent(player, next);
            Bukkit.getPluginManager().callEvent(event);
            if (event.isCancelled()) {
                return current;
            }
            next = event.getNewState();
        }

        toggle.put(player.getUniqueId(), next);
        savePlayerData(player);
        return next;
    }

    public static boolean setItemAutoSell(@NotNull Player player,
                                          @NotNull String itemName,
                                          boolean enabled) {
        String upper = itemName.toUpperCase();
        if (!isConfiguredDrop(upper)) {
            return isAutoSellEnabledForItem(player, upper);
        }

        String playerName = player.getName();
        Set<String> set = autoSellItems.get(playerName);
        if (set == null) {
            set = new HashSet<>();
            autoSellItems.put(playerName, set);
        }

        boolean changed;
        if (enabled) {
            changed = set.add(upper);
        } else {
            changed = set.remove(upper);
        }

        if (changed) {
            savePlayerData(player);
        }

        if (isAutoSellTriggerOnToggle()) {
            if (enabled && getPlayerItem(player, upper) > 0) {
                scheduleAutoSellIfNeeded(player, upper);
            }
        }
        return isAutoSellEnabledForItem(player, upper);
    }

    public static @NotNull Set<String> getEnabledAutoSellItems(@NotNull Player player) {
        Set<String> set = autoSellItems.get(player.getName());
        if (set == null || set.isEmpty()) {
            return new HashSet<>();
        }
        return new HashSet<>(set);
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
        return !disabledItems.contains(itemName.toUpperCase());
    }

    public static boolean isItemAutoPickupDisabled(@NotNull Player player,
                                                   @NotNull String itemName) {
        Set<String> disabledItems = disabledAutoPickupItems.get(player.getName());
        if (disabledItems == null || disabledItems.isEmpty()) {
            return false;
        }
        return disabledItems.contains(itemName.toUpperCase());
    }

    public static boolean toggleItemAutoPickup(@NotNull Player player,
                                               @NotNull String itemName) {
        return toggleItemAutoPickup(player, itemName, false);
    }

    public static boolean toggleItemAutoPickup(@NotNull Player player,
                                               @NotNull String itemName,
                                               boolean fireEvent) {
        String playerName = player.getName();
        String upperItem = itemName.toUpperCase();

        if (!isConfiguredDrop(upperItem)) {
            return isAutoPickupEnabledForItem(player, upperItem);
        }

        boolean currentEnabled = isAutoPickupEnabledForItem(player, upperItem);
        boolean nextEnabled = !currentEnabled;

        if (fireEvent) {
            CropStorageItemToggleEvent event = new CropStorageItemToggleEvent(player, upperItem, nextEnabled);
            Bukkit.getPluginManager().callEvent(event);
            if (event.isCancelled()) {
                return currentEnabled;
            }
            nextEnabled = event.getNewState();
        }

        Set<String> disabledItems = disabledAutoPickupItems.get(playerName);
        if (disabledItems == null) {
            disabledItems = new HashSet<>();
            disabledAutoPickupItems.put(playerName, disabledItems);
        }

        if (nextEnabled) {
            disabledItems.remove(upperItem);
        } else {
            disabledItems.add(upperItem);
        }
        savePlayerData(player);
        return nextEnabled;
    }

    public static boolean addItemAmount(@NotNull Player player, @NotNull String itemName, int amount) {
        return addItemAmount(player, itemName, amount, false);
    }

    public static boolean addItemAmount(@NotNull Player player, @NotNull String itemName, int amount,
                                        boolean fireEvent) {
        if (!isSystemEnabled() || !isConfiguredDrop(itemName) || amount <= 0)
            return false;

        int requestedAmount = amount;
        if (fireEvent) {
            CropStorageDepositEvent event = new CropStorageDepositEvent(player, itemName.toUpperCase(), amount);
            Bukkit.getPluginManager().callEvent(event);
            if (event.isCancelled()) {
                return false;
            }
            requestedAmount = event.getAmount();
            if (requestedAmount <= 0) {
                return false;
            }
        }

        String key = player.getName() + "_crop_" + itemName.toUpperCase();
        int current = playerdata.getOrDefault(key, 0);
        int max = getMaxStorage(player);
        if (current >= max)
            return false;

        int amountToAdd = Math.min(requestedAmount, max - current);
        if (amountToAdd <= 0)
            return false;

        playerdata.put(key, current + amountToAdd);

        if (isAutoSellTriggerOnDeposit()) {
            scheduleAutoSellIfNeeded(player, itemName);
        }
        return true;
    }

    private static boolean isAutoSellTriggerOnDeposit() {
        String mode = File.getCropStorageConfig().getString("auto_sell.trigger", "");
        if (mode != null && !mode.trim().isEmpty()) {
            switch (mode.trim().toUpperCase()) {
                case "BOTH":
                case "DEPOSIT_ONLY":
                    return true;
                case "TOGGLE_ONLY":
                case "DISABLED":
                    return false;
                default:
                    break;
            }
        }
        return File.getCropStorageConfig().getBoolean("auto_sell.trigger_on_deposit", true);
    }

    private static boolean isAutoSellTriggerOnToggle() {
        String mode = File.getCropStorageConfig().getString("auto_sell.trigger", "");
        if (mode != null && !mode.trim().isEmpty()) {
            switch (mode.trim().toUpperCase()) {
                case "BOTH":
                case "TOGGLE_ONLY":
                    return true;
                case "DEPOSIT_ONLY":
                case "DISABLED":
                    return false;
                default:
                    break;
            }
        }
        return File.getCropStorageConfig().getBoolean("auto_sell.trigger_on_toggle", true);
    }

    public static void scheduleAutoSellOnJoin(@NotNull Player player) {
        if (!isSystemEnabled()) {
            return;
        }
        if (!isAutoSellSystemEnabled()) {
            return;
        }
        if (!isAutoSellTriggerOnToggle()) {
            return;
        }

        Set<String> enabled = autoSellItems.get(player.getName());
        if (enabled == null || enabled.isEmpty()) {
            return;
        }

        for (String itemName : new HashSet<>(enabled)) {
            if (itemName == null || itemName.trim().isEmpty()) {
                continue;
            }
            if (getPlayerItem(player, itemName) <= 0) {
                continue;
            }
            scheduleAutoSellIfNeeded(player, itemName);
        }
    }

    public static boolean isAutoSellSystemEnabled() {
        return File.getCropStorageConfig().getBoolean("auto_sell.enabled", true);
    }

    public static boolean isAutoSellEnabledForItem(@NotNull Player player, @NotNull String itemName) {
        if (!isAutoSellSystemEnabled()) {
            return false;
        }
        Set<String> enabled = autoSellItems.get(player.getName());
        if (enabled == null || enabled.isEmpty()) {
            return false;
        }
        return enabled.contains(itemName.toUpperCase());
    }

    public static boolean toggleItemAutoSell(@NotNull Player player, @NotNull String itemName) {
        String upper = itemName.toUpperCase();
        if (!isConfiguredDrop(upper)) {
            return isAutoSellEnabledForItem(player, upper);
        }

        String playerName = player.getName();
        Set<String> enabled = autoSellItems.get(playerName);
        if (enabled == null) {
            enabled = new HashSet<>();
            autoSellItems.put(playerName, enabled);
        }

        boolean next;
        if (enabled.contains(upper)) {
            enabled.remove(upper);
            next = false;
        } else {
            enabled.add(upper);
            next = true;
        }

        savePlayerData(player);
        return next;
    }

    private static void scheduleAutoSellIfNeeded(@NotNull Player player, @NotNull String itemName) {
        String upper = itemName.toUpperCase();
        if (!isAutoSellEnabledForItem(player, upper)) {
            return;
        }

        String key = player.getUniqueId() + "_crop_" + upper;
        synchronized (pendingAutoSell) {
            if (!pendingAutoSell.add(key)) {
                return;
            }
        }

        long delayTicks = resolveAutoSellDelayTicks(player);
        long delayMillis = Math.max(50L, delayTicks * 50L);
        long now = System.currentTimeMillis();
        long earliest;
        synchronized (lastAutoSellAt) {
            long last = lastAutoSellAt.getOrDefault(key, 0L);
            earliest = last <= 0L ? now : Math.max(now, last + delayMillis);
        }
        long scheduleDelayMillis = Math.max(0L, earliest - now);
        long scheduleDelayTicks = Math.max(1L, (long) Math.ceil(scheduleDelayMillis / 50D));

        net.danh.storage.Utils.SchedulerUtil.runTaskLater(Storage.getStorage(), () -> {
            synchronized (pendingAutoSell) {
                pendingAutoSell.remove(key);
            }

            if (!player.isOnline()) {
                return;
            }
            if (!isAutoSellEnabledForItem(player, upper)) {
                return;
            }
            if (getPlayerItem(player, upper) <= 0) {
                return;
            }

            new net.danh.storage.Action.CropSell(player, upper, -1).doAction();

            synchronized (lastAutoSellAt) {
                lastAutoSellAt.put(key, System.currentTimeMillis());
            }
        }, scheduleDelayTicks);
    }

    public static int clearAutoSell(@NotNull Player player) {
        String playerName = player.getName();
        Set<String> current = autoSellItems.remove(playerName);
        int cleared = current == null ? 0 : current.size();
        if (cleared > 0) {
            savePlayerData(player);
        }

        String prefix = player.getUniqueId() + "_crop_";
        synchronized (pendingAutoSell) {
            pendingAutoSell.removeIf(key -> key != null && key.startsWith(prefix));
        }
        synchronized (lastAutoSellAt) {
            lastAutoSellAt.keySet().removeIf(key -> key != null && key.startsWith(prefix));
        }

        return cleared;
    }

    private static long resolveAutoSellDelayTicks(@NotNull Player player) {
        double delaySeconds = File.getCropStorageConfig().getDouble("auto_sell.default_delay", 5D);
        delaySeconds = applyAutoSellDelayPermission(player, delaySeconds);
        if (delaySeconds < 0D) {
            delaySeconds = 0D;
        }
        long ticks = (long) Math.ceil(delaySeconds * 20D);
        return Math.max(1L, ticks);
    }

    private static double applyAutoSellDelayPermission(@NotNull Player player, double currentSeconds) {
        double best = currentSeconds;
        for (PermissionAttachmentInfo pai : player.getEffectivePermissions()) {
            if (pai == null || !pai.getValue()) {
                continue;
            }
            String perm = pai.getPermission();
            if (perm == null) {
                continue;
            }
            if (!perm.startsWith("storage.cropstorage.autosell.delay.")) {
                continue;
            }
            String raw = perm.substring("storage.cropstorage.autosell.delay.".length()).trim();
            if (raw.isEmpty()) {
                continue;
            }
            double value;
            try {
                value = Double.parseDouble(raw);
            } catch (NumberFormatException ignored) {
                continue;
            }
            if (value < 0D) {
                continue;
            }
            best = Math.min(best, value);
        }
        return best;
    }

    public static boolean removeItemAmount(@NotNull Player player, @NotNull String itemName, int amount) {
        return removeItemAmount(player, itemName, amount, false);
    }

    public static boolean removeItemAmount(@NotNull Player player, @NotNull String itemName, int amount,
                                           boolean fireEvent) {
        if (!isSystemEnabled() || amount <= 0)
            return false;

        int requestedAmount = amount;
        if (fireEvent) {
            CropStorageWithdrawEvent event = new CropStorageWithdrawEvent(player, itemName.toUpperCase(), amount);
            Bukkit.getPluginManager().callEvent(event);
            if (event.isCancelled()) {
                return false;
            }
            requestedAmount = event.getAmount();
            if (requestedAmount <= 0) {
                return false;
            }
        }

        String key = player.getName() + "_crop_" + itemName.toUpperCase();
        int current = playerdata.getOrDefault(key, 0);
        if (current < requestedAmount)
            return false;

        int amountToRemove = requestedAmount;
        int newValue = current - amountToRemove;
        if (newValue <= 0) {
            playerdata.remove(key);
        } else {
            playerdata.put(key, newValue);
        }
        return true;
    }

    public static void setItemAmount(@NotNull Player player, @NotNull String itemName, int amount) {
        String key = player.getName() + "_crop_" + itemName.toUpperCase();
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
        autoSellItems.remove(player.getName());

        UUID playerId = player.getUniqueId();

        PlayerData data = Storage.dataStorage.getData(player.getName());
        if (data == null) {
            int permissionMax = Math.max(0, getPermissionMaxStorage(player));
            playermaxdata.put(playerId, permissionMax);
            toggle.put(playerId, File.getCropStorageConfig().getBoolean(
                    "settings.default_auto_pickup",
                    false));
            return;
        }

        String dataString = data.getData();
        if (dataString == null || dataString.isEmpty()) {
            int permissionMax = Math.max(0, getPermissionMaxStorage(player));
            playermaxdata.put(playerId, permissionMax);
            toggle.put(playerId, File.getCropStorageConfig().getBoolean(
                    "settings.default_auto_pickup",
                    false));
            return;
        }

        // Parse data format: "crop:WHEAT:100,CARROT:50;croptoggle:true"
        String[] dataParts = dataString.split(";");
        for (String part : dataParts) {
            if (part == null || part.isEmpty())
                continue;

            if (part.startsWith("crop:")) {
                String cropData = part.substring(5);
                if (cropData.isEmpty())
                    continue;

                String[] items = cropData.split(",");
                for (String item : items) {
                    if (item == null || item.isEmpty())
                        continue;
                    String[] itemParts = item.split(":");
                    if (itemParts.length == 2) {
                        String key = player.getName() + "_crop_" + itemParts[0].toUpperCase();
                        try {
                            int value = Integer.parseInt(itemParts[1]);
                            if (value > 0) {
                                playerdata.put(key, value);
                            }
                        } catch (NumberFormatException ignored) {
                        }
                    }
                }
            } else if (part.startsWith("croptoggle:")) {
                String raw = part.substring("croptoggle:".length()).trim();
                toggle.put(playerId, "true".equalsIgnoreCase(raw));
            } else if (part.startsWith(MAX_OVERRIDE_DATA_PREFIX)) {
                Integer parsed = parseMaxOverride(part);
                if (parsed != null) {
                    setMaxStorageOverride(player, Math.max(0, parsed));
                } else {
                    clearMaxStorageOverride(player);
                }
            } else if (part.startsWith("cropautopickupoff:")) {
                String disabledData = part.substring("cropautopickupoff:"
                        .length());
                if (!disabledData.isEmpty()) {
                    Set<String> disabledItems = new HashSet<>();
                    for (String raw : disabledData.split(",")) {
                        if (raw == null)
                            continue;
                        String cropItem = raw.trim().toUpperCase();
                        if (cropItem.isEmpty())
                            continue;
                        if (isConfiguredDrop(cropItem)) {
                            disabledItems.add(cropItem);
                        }
                    }
                    if (!disabledItems.isEmpty()) {
                        disabledAutoPickupItems.put(player.getName(),
                                disabledItems);
                    }
                }
            } else if (part.startsWith(AUTO_SELL_DATA_PREFIX)) {
                String enabledData = part.substring(AUTO_SELL_DATA_PREFIX.length()).trim();
                if (!enabledData.isEmpty()) {
                    Set<String> enabledItems = new HashSet<>();
                    for (String raw : enabledData.split(",")) {
                        if (raw == null) {
                            continue;
                        }
                        String cropItem = raw.trim().toUpperCase();
                        if (cropItem.isEmpty()) {
                            continue;
                        }
                        if (isConfiguredDrop(cropItem)) {
                            enabledItems.add(cropItem);
                        }
                    }
                    if (!enabledItems.isEmpty()) {
                        autoSellItems.put(player.getName(), enabledItems);
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
                    File.getCropStorageConfig(),
                    "settings.max_storage_mode",
                    databaseMax,
                    permissionMax);
        }
        playermaxdata.put(playerId, Math.max(0, resolvedMax));

        if (!groundStoreToggle.containsKey(playerId)) {
            groundStoreToggle.put(playerId, File.getCropStorageConfig()
                    .getBoolean("ground_store.default_enabled", false));
        }
    }

    public static void savePlayerData(@NotNull Player player) {
        if (!isSystemEnabled())
            return;

        StringBuilder cropData = new StringBuilder();
        String playerName = player.getName();
        UUID playerId = player.getUniqueId();

        for (String drop : configuredDrops) {
            String key = playerName + "_crop_" + drop;
            if (playerdata.containsKey(key)) {
                int amount = playerdata.get(key);
                if (amount > 0) {
                    if (cropData.length() > 0) {
                        cropData.append(",");
                    }
                    cropData.append(drop).append(":").append(amount);
                }
            }
        }

        PlayerData existingData = Storage.dataStorage.getData(playerName);
        String existingDataString = existingData != null ? existingData.getData() : "";

        StringBuilder finalData = new StringBuilder();
        if (existingDataString != null && !existingDataString.isEmpty()) {
            String[] existingParts = existingDataString.split(";");

            for (String part : existingParts) {
                if (!part.startsWith("crop:")
                        && !part.startsWith("croptoggle:")
                        && !part.startsWith("cropautopickupoff:")
                        && !part.startsWith(AUTO_SELL_DATA_PREFIX)
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

        if (cropData.length() > 0) {
            if (finalData.length() > 0) {
                finalData.append(";");
            }
            finalData.append("crop:").append(cropData);
        }

        if (toggle.containsKey(playerId)) {
            if (finalData.length() > 0) {
                finalData.append(";");
            }
            finalData.append("croptoggle:").append(toggle.get(playerId));
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
                finalData.append("cropautopickupoff:")
                        .append(disabledData);
            }
        }

        Set<String> enabledAutoSell = autoSellItems.get(playerName);
        if (enabledAutoSell != null && !enabledAutoSell.isEmpty()) {
            StringBuilder enabledData = new StringBuilder();
            for (String item : configuredDrops) {
                if (!enabledAutoSell.contains(item)) {
                    continue;
                }
                if (enabledData.length() > 0) {
                    enabledData.append(",");
                }
                enabledData.append(item);
            }

            if (enabledData.length() > 0) {
                if (finalData.length() > 0) {
                    finalData.append(";");
                }
                finalData.append(AUTO_SELL_DATA_PREFIX)
                        .append(enabledData);
            }
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

        int maxStorage;
        if (existingData != null) {
            maxStorage = existingData.getMax();
        } else {
            maxStorage = File.getCropStorageConfig().getInt(
                    "settings.default_max_storage",
                    100000);
        }

        boolean autoPickup = toggle.getOrDefault(playerId,
                File.getCropStorageConfig().getBoolean(
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
        playerdata.entrySet().removeIf(entry -> entry.getKey().startsWith(playerName + "_crop_"));
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
            String key = playerName + "_crop_" + drop;
            if (playerdata.containsKey(key)) {
                items.put(drop, playerdata.get(key));
            }
        }

        return items;
    }

    public static int getPlayerItem(@NotNull String playerName, @NotNull String itemName) {
        return playerdata.getOrDefault(playerName + "_crop_" + itemName.toUpperCase(), 0);
    }

    public static int getMaxStorage(@NotNull String playerName) {
        int defaultMax = File.getCropStorageConfig().getInt("settings.default_max_storage", 100000);

        PlayerData data = Storage.dataStorage.getData(playerName);
        if (data == null) {
            return defaultMax;
        }

        Integer overrideMax = null;
        String rawData = data.getData();
        if (rawData != null && !rawData.isEmpty()) {
            int idx = rawData.indexOf(MAX_OVERRIDE_DATA_PREFIX);
            if (idx >= 0) {
                int start = idx + MAX_OVERRIDE_DATA_PREFIX.length();
                int end = rawData.indexOf(';', start);
                String raw = end >= 0
                        ? rawData.substring(start, end)
                        : rawData.substring(start);
                raw = raw.trim();
                try {
                    overrideMax = Integer.parseInt(raw);
                } catch (NumberFormatException ignored) {
                }
            }
        }

        if (overrideMax != null) {
            return Math.max(0, overrideMax);
        }

        int databaseMax = Math.max(0, data.getMax());
        int permissionMaxFallback = Math.max(0, defaultMax);
        int resolved = File.resolveMaxStorage(
                File.getCropStorageConfig(),
                "settings.max_storage_mode",
                databaseMax,
                permissionMaxFallback);
        return Math.max(0, resolved);
    }

    public static boolean loadOfflinePlayerData(@NotNull String playerName) {
        if (!isSystemEnabled())
            return false;

        Player onlinePlayer = Bukkit.getPlayer(playerName);
        if (onlinePlayer != null) {
            return true;
        }

        for (String key : playerdata.keySet()) {
            if (key.startsWith(playerName + "_crop_")) {
                return true;
            }
        }

        PlayerData data = Storage.dataStorage.getData(playerName);
        if (data == null) {
            return false;
        }

        String dataString = data.getData();
        if (dataString == null || dataString.isEmpty()) {
            return false;
        }

        Set<String> disabledItems = null;

        String[] dataParts = dataString.split(";");
        for (String part : dataParts) {
            if (part == null || part.isEmpty())
                continue;

            if (part.startsWith("crop:")) {
                String cropData = part.substring(5);
                if (cropData.isEmpty())
                    continue;

                String[] items = cropData.split(",");
                for (String item : items) {
                    if (item == null || item.isEmpty())
                        continue;
                    String[] itemParts = item.split(":");
                    if (itemParts.length == 2) {
                        String key = playerName + "_crop_" + itemParts[0].toUpperCase();
                        try {
                            int value = Integer.parseInt(itemParts[1]);
                            if (value > 0) {
                                playerdata.put(key, value);
                            }
                        } catch (NumberFormatException ignored) {
                        }
                    }
                }
            } else if (part.startsWith("cropautopickupoff:")) {
                String disabledData = part.substring("cropautopickupoff:"
                        .length());
                if (!disabledData.isEmpty()) {
                    disabledItems = new HashSet<>();
                    for (String raw : disabledData.split(",")) {
                        if (raw == null)
                            continue;
                        String cropItem = raw.trim().toUpperCase();
                        if (cropItem.isEmpty())
                            continue;
                        if (isConfiguredDrop(cropItem)) {
                            disabledItems.add(cropItem);
                        }
                    }
                }
            }
        }

        if (disabledItems != null && !disabledItems.isEmpty()) {
            disabledAutoPickupItems.put(playerName, disabledItems);
        }

        return true;
    }

    public static boolean hasOfflinePlayerData(@NotNull String playerName) {
        if (!isSystemEnabled())
            return false;
        return Storage.dataStorage.getData(playerName) != null;
    }

    public static void cleanupOfflinePlayerData(@NotNull String playerName) {
        Player onlinePlayer = Bukkit.getPlayer(playerName);
        if (onlinePlayer != null) {
            return;
        }

        playerdata.entrySet().removeIf(entry -> entry.getKey().startsWith(playerName + "_crop_"));

        disabledAutoPickupItems.remove(playerName);
    }

    /**
     * Get the block-to-drop mapping for crop harvesting.
     * Maps block Material name -> drop Material name.
     */
    public static Map<String, String> getCropBlockMapping() {
        Map<String, String> mapping = new HashMap<>();
        ConfigurationSection section = File.getCropStorageConfig().getConfigurationSection("crop_block_mapping");
        if (section != null) {
            for (String blockKey : section.getKeys(false)) {
                String dropItem = section.getString(blockKey);
                if (dropItem != null && !dropItem.isEmpty()) {
                    mapping.put(blockKey.toUpperCase(), dropItem.toUpperCase());
                }
            }
        }
        return mapping;
    }
}
