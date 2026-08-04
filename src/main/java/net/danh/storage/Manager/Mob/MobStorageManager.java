package net.danh.storage.Manager.Mob;

import net.danh.storage.API.events.*;
import net.danh.storage.Action.MobSell;
import net.danh.storage.Database.PlayerData;
import net.danh.storage.Storage;
import net.danh.storage.Utils.File;
import net.danh.storage.Utils.MaterialUtils;
import net.danh.storage.Utils.SchedulerUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.permissions.PermissionAttachmentInfo;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class MobStorageManager {

    private static final String STORAGE_LIMIT_PERMISSION_PREFIX = "storage.mobstorage.storage.";
    private static final String STORAGE_LIMIT_PERMISSION_MAX_PREFIX = "storage.mobstorage.storage.max.";
    private static final String GROUND_STORE_DATA_PREFIX = "mobgroundstore:";
    private static final String MAX_OVERRIDE_DATA_PREFIX = "mobmaxoverride:";
    private static final String DISABLED_ITEM_DATA_PREFIX = "mobdisabled:";
    private static final String AUTO_SELL_DATA_PREFIX = "mobautosell:";
    private static final HashMap<String, Set<String>> disabledAutoPickupItems = new HashMap<>();
    private static final HashMap<String, Set<String>> autoSellItems = new HashMap<>();
    private static final Set<String> pendingAutoSell = new HashSet<>();
    private static final Map<String, Long> lastAutoSellAt = new HashMap<>();
    private static final HashMap<UUID, Boolean> groundStoreToggle = new HashMap<>();
    private static final HashMap<UUID, Integer> maxOverrideData = new HashMap<>();
    private static final Map<String, Set<String>> MOB_NAME_ALIASES = createMobNameAliases();
    public static HashMap<String, Integer> playerdata = new HashMap<>();
    public static HashMap<UUID, Boolean> toggle = new HashMap<>();
    public static HashMap<UUID, Integer> playermaxdata = new HashMap<>();
    private static List<String> configuredDrops = new ArrayList<>();
    private static List<String> invalidItems = new ArrayList<>();
    private static Map<String, Set<String>> configuredMobDrops = new HashMap<>();
    private static boolean systemEnabled = false;

    @NotNull
    public static String getSellableSymbol(@NotNull String itemName) {
        String normalized = itemName.replace(":", ";").toUpperCase(Locale.ENGLISH);
        ConfigurationSection worthSection = File.getMobStorageConfig().getConfigurationSection("worth");
        if (worthSection == null) {
            return File.getMessage().getString("mobstorage.sellable.no", "&c✘");
        }

        String worthKey = resolveWorthKey(worthSection, normalized);
        if (worthKey == null) {
            return File.getMessage().getString("mobstorage.sellable.no", "&c✘");
        }

        boolean sellable = parseWorthValue(worthSection, worthKey) > 0;
        return File.getMessage().getString(
                sellable ? "mobstorage.sellable.yes" : "mobstorage.sellable.no",
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
        Storage.getStorage().getLogger().info("[MobStorage] Initializing MobStorage feature...");

        systemEnabled = File.getMobStorageConfig().getBoolean("settings.enabled", true);
        if (!systemEnabled) {
            Storage.getStorage().getLogger().info("[MobStorage] Feature is disabled in config");
            return;
        }

        loadConfiguredMobDrops();

        Storage.getStorage().getLogger().info("[MobStorage] ========== INITIALIZATION SUMMARY ==========");
        Storage.getStorage().getLogger().info("[MobStorage] Valid items loaded: " + configuredDrops.size());
        Storage.getStorage().getLogger().info("[MobStorage] Valid mob drop mappings loaded: " + configuredMobDrops.size());

        if (!invalidItems.isEmpty()) {
            Storage.getStorage().getLogger().warning("[MobStorage] Invalid items found: " + invalidItems.size());
            Storage.getStorage().getLogger().warning("[MobStorage] Items with errors: " + String.join(", ", invalidItems));
        } else {
            Storage.getStorage().getLogger().info("[MobStorage] No invalid items found - all items loaded successfully!");
        }

        Storage.getStorage().getLogger().info("[MobStorage] Initialization completed!");
        Storage.getStorage().getLogger().info("[MobStorage] ===================================");
    }

    public static boolean isSystemEnabled() {
        return systemEnabled;
    }

    public static boolean isGroundStoreSystemEnabled() {
        return File.getMobStorageConfig().getBoolean("ground_store.enabled", true);
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
        status = data != null ? parseGroundStoreStatus(data.data()) : null;
        if (status == null) {
            status = File.getMobStorageConfig().getBoolean("ground_store.default_enabled", false);
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
            MobStorageGroundStoreToggleEvent event = new MobStorageGroundStoreToggleEvent(player, next);
            Bukkit.getPluginManager().callEvent(event);
            if (event.isCancelled()) {
                return current;
            }
            next = event.getNewState();
        }

        groundStoreToggle.put(player.getUniqueId(), next);
        savePlayerData(player);
        return next;
    }

    public static boolean isGroundStoreItemAllowed(@NotNull String itemName) {
        if (!isConfiguredDrop(itemName)) {
            return false;
        }

        List<String> allowed = File.getMobStorageConfig().getStringList("ground_store.allowed_items");
        if (allowed == null || allowed.isEmpty()) {
            return false;
        }
        return allowed.contains(itemName.toUpperCase(Locale.ENGLISH));
    }

    private static void loadConfiguredMobDrops() {
        List<String> entries = File.getMobStorageConfig().getStringList("mob_drops");
        configuredDrops = new ArrayList<>();
        invalidItems = new ArrayList<>();
        configuredMobDrops = new HashMap<>();
        Set<String> uniqueDrops = new LinkedHashSet<>();
        if (entries == null) {
            return;
        }

        for (String entry : entries) {
            parseMobDropEntry(entry, uniqueDrops);
        }
        configuredDrops = new ArrayList<>(uniqueDrops);
    }

    private static void parseMobDropEntry(String entry, Set<String> uniqueDrops) {
        if (entry == null || entry.trim().isEmpty()) {
            invalidItems.add("<empty>");
            return;
        }

        String[] parts = entry.split(";");
        if (parts.length < 2) {
            invalidItems.add(entry);
            return;
        }

        String mobName = parts[0].trim().toUpperCase(Locale.ENGLISH);
        if (mobName.isEmpty()) {
            invalidItems.add(entry);
            return;
        }

        List<String> parsedDrops = new ArrayList<>();

        for (int i = 1; i < parts.length; i++) {
            String itemName = parts[i] == null ? "" : parts[i].trim().toUpperCase(Locale.ENGLISH);
            if (itemName.isEmpty()) {
                continue;
            }
            parsedDrops.add(itemName);
            uniqueDrops.add(itemName);
        }

        if (parsedDrops.isEmpty()) {
            invalidItems.add(entry);
            return;
        }

        for (String key : getMobLookupKeys(mobName)) {
            Set<String> drops = configuredMobDrops.get(key);
            if (drops == null) {
                drops = new LinkedHashSet<>();
                configuredMobDrops.put(key, drops);
            }
            drops.addAll(parsedDrops);
        }
    }

    public static void reloadConfiguredDrops() {
        systemEnabled = File.getMobStorageConfig().getBoolean("settings.enabled", true);
        if (!isSystemEnabled()) {
            configuredDrops = new ArrayList<>();
            invalidItems = new ArrayList<>();
            configuredMobDrops = new HashMap<>();
            return;
        }
        loadConfiguredMobDrops();
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
        return configuredDrops.contains(itemName.toUpperCase(Locale.ENGLISH));
    }

    public static boolean isEntityTypeAllowed(@NotNull EntityType entityType) {
        for (String key : getMobLookupKeys(entityType.name())) {
            if (configuredMobDrops.containsKey(key)) {
                return true;
            }
        }
        return false;
    }

    public static boolean isConfiguredDropForMob(@NotNull EntityType entityType, @NotNull String itemName) {
        String item = itemName.toUpperCase(Locale.ENGLISH);
        for (String key : getMobLookupKeys(entityType.name())) {
            Set<String> drops = configuredMobDrops.get(key);
            if (drops != null && drops.contains(item)) {
                return true;
            }
        }
        return false;
    }

    public static Material resolveMaterial(@NotNull String itemName) {
        return MaterialUtils.matchMaterial(itemName.toUpperCase(Locale.ENGLISH));
    }

    private static Set<String> getMobLookupKeys(@NotNull String mobName) {
        String upper = mobName.toUpperCase(Locale.ENGLISH);
        Set<String> keys = new LinkedHashSet<>();
        keys.add(upper);
        Set<String> aliases = MOB_NAME_ALIASES.get(upper);
        if (aliases != null) {
            keys.addAll(aliases);
        }
        return keys;
    }

    private static Map<String, Set<String>> createMobNameAliases() {
        Map<String, Set<String>> aliases = new HashMap<>();
        addMobAliasGroup(aliases, "SNOWMAN", "SNOW_GOLEM");
        addMobAliasGroup(aliases, "MUSHROOM_COW", "MOOSHROOM");
        addMobAliasGroup(aliases, "PIG_ZOMBIE", "ZOMBIFIED_PIGLIN");
        return aliases;
    }

    private static void addMobAliasGroup(Map<String, Set<String>> aliases, String first, String second) {
        addMobAlias(aliases, first, second);
        addMobAlias(aliases, second, first);
    }

    private static void addMobAlias(Map<String, Set<String>> aliases, String key, String alias) {
        Set<String> values = aliases.get(key);
        if (values == null) {
            values = new LinkedHashSet<>();
            aliases.put(key, values);
        }
        values.add(alias);
    }

    @NotNull
    public static String getItemDisplayName(@NotNull String itemName) {
        String upper = itemName.toUpperCase(Locale.ENGLISH);
        String displayName = File.getMobStorageConfig().getString("mob_display_names." + upper);
        if (displayName != null && !displayName.trim().isEmpty()) {
            return displayName;
        }

        String[] parts = upper.toLowerCase(Locale.ENGLISH).split("_");
        StringBuilder formatted = new StringBuilder();
        for (String part : parts) {
            if (part.isEmpty()) {
                continue;
            }
            if (formatted.length() > 0) {
                formatted.append(" ");
            }
            formatted.append(part.substring(0, 1).toUpperCase(Locale.ENGLISH)).append(part.substring(1));
        }
        return formatted.length() > 0 ? formatted.toString() : upper;
    }

    public static int getPlayerItem(@NotNull Player player, @NotNull String itemName) {
        return getPlayerItem(player.getName(), itemName);
    }

    public static int getPlayerItem(@NotNull String playerName, @NotNull String itemName) {
        return playerdata.getOrDefault(playerName + "_mob_" + itemName.toUpperCase(Locale.ENGLISH), 0);
    }

    public static HashMap<String, Integer> getPlayerAllItems(@NotNull Player player) {
        HashMap<String, Integer> items = new HashMap<>();
        String playerName = player.getName();
        for (String drop : configuredDrops) {
            String key = playerName + "_mob_" + drop;
            if (playerdata.containsKey(key)) {
                items.put(drop, playerdata.get(key));
            }
        }
        return items;
    }

    public static int getMaxStorage(@NotNull Player player) {
        return playermaxdata.getOrDefault(player.getUniqueId(), File.getMobStorageConfig().getInt("settings.default_max_storage", 5000));
    }

    public static Integer getMaxStorageOverride(@NotNull Player player) {
        return maxOverrideData.get(player.getUniqueId());
    }

    public static void setMaxStorageOverride(@NotNull Player player, int maxStorage) {
        maxOverrideData.put(player.getUniqueId(), Math.max(0, maxStorage));
    }

    public static void clearMaxStorageOverride(@NotNull Player player) {
        maxOverrideData.remove(player.getUniqueId());
    }

    public static int getPermissionMaxStorage(@NotNull Player player) {
        int defaultMax = File.getMobStorageConfig().getInt("settings.default_max_storage", 5000);

        if (player.isOp() || player.hasPermission("storage.admin")) {
            return defaultMax;
        }

        int bestLimit = -1;
        int bestPriority = Integer.MIN_VALUE;

        for (PermissionAttachmentInfo pai : player.getEffectivePermissions()) {
            String perm = pai.getPermission();
            if (perm == null || !pai.getValue()) {
                continue;
            }
            if (!perm.startsWith(STORAGE_LIMIT_PERMISSION_MAX_PREFIX)) {
                continue;
            }

            try {
                int value = Integer.parseInt(perm.substring(STORAGE_LIMIT_PERMISSION_MAX_PREFIX.length()));
                if (value > bestLimit) {
                    bestLimit = value;
                }
            } catch (NumberFormatException ignored) {
            }
        }

        ConfigurationSection section = File.getMobStorageConfig().getConfigurationSection("mob_storage_permissions");
        if (section != null) {
            for (String key : section.getKeys(false)) {
                if (key == null || key.trim().isEmpty()) {
                    continue;
                }
                if (!player.hasPermission(STORAGE_LIMIT_PERMISSION_PREFIX + key)) {
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
            status = File.getMobStorageConfig().getBoolean("settings.default_auto_pickup", false);
            toggle.put(playerId, status);
        }
        return status;
    }

    public static boolean setToggleStatus(@NotNull Player player, boolean status, boolean fireEvent) {
        boolean current = getToggleStatus(player);
        boolean next = status;

        if (fireEvent) {
            MobStorageToggleEvent event = new MobStorageToggleEvent(player, next);
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

    public static boolean isAutoPickupEnabledForItem(@NotNull Player player, @NotNull String itemName) {
        if (!getToggleStatus(player)) {
            return false;
        }
        return !isItemAutoPickupDisabled(player, itemName);
    }

    public static boolean isItemAutoPickupDisabled(@NotNull Player player, @NotNull String itemName) {
        Set<String> disabledItems = disabledAutoPickupItems.get(player.getName());
        if (disabledItems == null || disabledItems.isEmpty()) {
            return false;
        }
        return disabledItems.contains(itemName.toUpperCase(Locale.ENGLISH));
    }

    public static boolean toggleItemAutoPickup(@NotNull Player player, @NotNull String itemName, boolean fireEvent) {
        String upperItem = itemName.toUpperCase(Locale.ENGLISH);
        if (!isConfiguredDrop(upperItem)) {
            return isAutoPickupEnabledForItem(player, upperItem);
        }

        boolean currentEnabled = isAutoPickupEnabledForItem(player, upperItem);
        boolean nextEnabled = !currentEnabled;

        if (fireEvent) {
            MobStorageItemToggleEvent event = new MobStorageItemToggleEvent(player, upperItem, nextEnabled);
            Bukkit.getPluginManager().callEvent(event);
            if (event.isCancelled()) {
                return currentEnabled;
            }
            nextEnabled = event.getNewState();
        }

        Set<String> disabledItems = disabledAutoPickupItems.get(player.getName());
        if (disabledItems == null) {
            disabledItems = new HashSet<>();
            disabledAutoPickupItems.put(player.getName(), disabledItems);
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
        return addItemAmount(player, itemName, amount, true);
    }

    public static boolean addItemAmount(@NotNull Player player, @NotNull String itemName, int amount, boolean fireEvent) {
        String upper = itemName.toUpperCase(Locale.ENGLISH);
        if (!isSystemEnabled() || !isConfiguredDrop(upper) || amount <= 0) {
            return false;
        }

        int requestedAmount = amount;
        if (fireEvent) {
            MobStorageDepositEvent event = new MobStorageDepositEvent(player, upper, amount);
            Bukkit.getPluginManager().callEvent(event);
            if (event.isCancelled()) {
                return false;
            }
            requestedAmount = event.getAmount();
            if (requestedAmount <= 0) {
                return false;
            }
        }

        String key = player.getName() + "_mob_" + upper;
        int current = playerdata.getOrDefault(key, 0);
        int max = getMaxStorage(player);
        if (current >= max || current + requestedAmount > max) {
            return false;
        }

        playerdata.put(key, current + requestedAmount);
        if (isAutoSellTriggerOnDeposit()) {
            scheduleAutoSellIfNeeded(player, upper);
        }
        return true;
    }

    public static boolean setItemAutoSell(@NotNull Player player, @NotNull String itemName, boolean enabled) {
        String upper = itemName.toUpperCase(Locale.ENGLISH);
        if (!isConfiguredDrop(upper)) {
            return isAutoSellEnabledForItem(player, upper);
        }

        String playerName = player.getName();
        Set<String> set = autoSellItems.get(playerName);
        if (set == null) {
            set = new HashSet<>();
            autoSellItems.put(playerName, set);
        }

        boolean changed = enabled ? set.add(upper) : set.remove(upper);
        if (changed) {
            savePlayerData(player);
        }

        if (enabled && isAutoSellTriggerOnToggle() && getPlayerItem(player, upper) > 0) {
            scheduleAutoSellIfNeeded(player, upper);
        }
        return isAutoSellEnabledForItem(player, upper);
    }

    @NotNull
    public static Set<String> getEnabledAutoSellItems(@NotNull Player player) {
        Set<String> enabled = autoSellItems.get(player.getName());
        return enabled == null || enabled.isEmpty() ? new HashSet<>() : new HashSet<>(enabled);
    }

    public static boolean isAutoSellSystemEnabled() {
        return File.getMobStorageConfig().getBoolean("auto_sell.enabled", true);
    }

    public static boolean isAutoSellEnabledForItem(@NotNull Player player, @NotNull String itemName) {
        if (!isAutoSellSystemEnabled()) {
            return false;
        }
        Set<String> enabled = autoSellItems.get(player.getName());
        return enabled != null && enabled.contains(itemName.toUpperCase(Locale.ENGLISH));
    }

    public static boolean toggleItemAutoSell(@NotNull Player player, @NotNull String itemName) {
        String upper = itemName.toUpperCase(Locale.ENGLISH);
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
        if (next && isAutoSellTriggerOnToggle() && getPlayerItem(player, upper) > 0) {
            scheduleAutoSellIfNeeded(player, upper);
        }
        return next;
    }

    public static int clearAutoSell(@NotNull Player player) {
        String playerName = player.getName();
        Set<String> current = autoSellItems.remove(playerName);
        int cleared = current == null ? 0 : current.size();
        if (cleared > 0) {
            savePlayerData(player);
        }

        String prefix = player.getUniqueId() + "_mob_";
        synchronized (pendingAutoSell) {
            pendingAutoSell.removeIf(key -> key != null && key.startsWith(prefix));
        }
        synchronized (lastAutoSellAt) {
            lastAutoSellAt.keySet().removeIf(key -> key != null && key.startsWith(prefix));
        }
        return cleared;
    }

    public static void scheduleAutoSellOnJoin(@NotNull Player player) {
        if (!isSystemEnabled() || !isAutoSellSystemEnabled() || !isAutoSellTriggerOnToggle()) {
            return;
        }
        Set<String> enabled = autoSellItems.get(player.getName());
        if (enabled == null || enabled.isEmpty()) {
            return;
        }
        for (String itemName : new HashSet<>(enabled)) {
            if (itemName != null && getPlayerItem(player, itemName) > 0) {
                scheduleAutoSellIfNeeded(player, itemName);
            }
        }
    }

    private static boolean isAutoSellTriggerOnDeposit() {
        String mode = File.getMobStorageConfig().getString("auto_sell.trigger", "");
        if (mode != null && !mode.trim().isEmpty()) {
            switch (mode.trim().toUpperCase(Locale.ENGLISH)) {
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
        return File.getMobStorageConfig().getBoolean("auto_sell.trigger_on_deposit", true);
    }

    private static boolean isAutoSellTriggerOnToggle() {
        String mode = File.getMobStorageConfig().getString("auto_sell.trigger", "");
        if (mode != null && !mode.trim().isEmpty()) {
            switch (mode.trim().toUpperCase(Locale.ENGLISH)) {
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
        return File.getMobStorageConfig().getBoolean("auto_sell.trigger_on_toggle", true);
    }

    private static void scheduleAutoSellIfNeeded(@NotNull Player player, @NotNull String itemName) {
        String upper = itemName.toUpperCase(Locale.ENGLISH);
        if (!isAutoSellEnabledForItem(player, upper)) {
            return;
        }

        String key = player.getUniqueId() + "_mob_" + upper;
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

        SchedulerUtil.runTaskLater(Storage.getStorage(), player, () -> {
            synchronized (pendingAutoSell) {
                pendingAutoSell.remove(key);
            }
            if (!player.isOnline() || !isAutoSellEnabledForItem(player, upper) || getPlayerItem(player, upper) <= 0) {
                return;
            }
            new MobSell(player, upper, -1).doAction();
            synchronized (lastAutoSellAt) {
                lastAutoSellAt.put(key, System.currentTimeMillis());
            }
        }, scheduleDelayTicks);
    }

    private static long resolveAutoSellDelayTicks(@NotNull Player player) {
        double delaySeconds = File.getMobStorageConfig().getDouble("auto_sell.default_delay", 5D);
        delaySeconds = applyAutoSellDelayPermission(player, delaySeconds);
        if (delaySeconds < 0D) {
            delaySeconds = 0D;
        }
        return Math.max(1L, (long) Math.ceil(delaySeconds * 20D));
    }

    private static double applyAutoSellDelayPermission(@NotNull Player player, double currentSeconds) {
        double best = currentSeconds;
        for (PermissionAttachmentInfo pai : player.getEffectivePermissions()) {
            if (pai == null || !pai.getValue()) {
                continue;
            }
            String perm = pai.getPermission();
            if (perm == null || !perm.startsWith("storage.mobstorage.autosell.delay.")) {
                continue;
            }
            String raw = perm.substring("storage.mobstorage.autosell.delay.".length()).trim();
            if (raw.isEmpty()) {
                continue;
            }
            try {
                double value = Double.parseDouble(raw);
                if (value >= 0D && value < best) {
                    best = value;
                }
            } catch (NumberFormatException ignored) {
            }
        }
        return best;
    }

    public static boolean removeItemAmount(@NotNull Player player, @NotNull String itemName, int amount) {
        return removeItemAmount(player, itemName, amount, true);
    }

    public static boolean removeItemAmount(@NotNull Player player, @NotNull String itemName, int amount, boolean fireEvent) {
        if (!isSystemEnabled() || amount <= 0) {
            return false;
        }

        String upper = itemName.toUpperCase(Locale.ENGLISH);
        int requestedAmount = amount;
        if (fireEvent) {
            MobStorageWithdrawEvent event = new MobStorageWithdrawEvent(player, upper, amount);
            Bukkit.getPluginManager().callEvent(event);
            if (event.isCancelled()) {
                return false;
            }
            requestedAmount = event.getAmount();
            if (requestedAmount <= 0) {
                return false;
            }
        }

        String key = player.getName() + "_mob_" + upper;
        int current = playerdata.getOrDefault(key, 0);
        if (current < requestedAmount) {
            return false;
        }

        int next = current - requestedAmount;
        if (next <= 0) {
            playerdata.remove(key);
        } else {
            playerdata.put(key, next);
        }
        return true;
    }

    public static void setItemAmount(@NotNull Player player, @NotNull String itemName, int amount) {
        String key = player.getName() + "_mob_" + itemName.toUpperCase(Locale.ENGLISH);
        if (amount <= 0) {
            playerdata.remove(key);
        } else {
            playerdata.put(key, amount);
        }
    }

    public static void loadPlayerData(@NotNull Player player) {
        if (!isSystemEnabled()) {
            return;
        }

        disabledAutoPickupItems.remove(player.getName());
        autoSellItems.remove(player.getName());
        UUID playerId = player.getUniqueId();

        PlayerData data = Storage.dataStorage.getData(player.getName());
        if (data == null) {
            playermaxdata.put(playerId, Math.max(0, getPermissionMaxStorage(player)));
            toggle.put(playerId, File.getMobStorageConfig().getBoolean("settings.default_auto_pickup", false));
            groundStoreToggle.put(playerId, File.getMobStorageConfig().getBoolean("ground_store.default_enabled", false));
            return;
        }

        String dataString = data.data();
        if (dataString != null && !dataString.isEmpty()) {
            for (String part : dataString.split(";")) {
                if (part == null || part.isEmpty()) {
                    continue;
                }

                if (part.startsWith("mob:")) {
                    loadStoredItems(player.getName(), part.substring(4));
                } else if (part.startsWith("mobtoggle:")) {
                    toggle.put(playerId, "true".equalsIgnoreCase(part.substring("mobtoggle:".length()).trim()));
                } else if (part.startsWith(MAX_OVERRIDE_DATA_PREFIX)) {
                    Integer parsed = parseMaxOverride(part);
                    if (parsed != null) {
                        setMaxStorageOverride(player, parsed);
                    } else {
                        clearMaxStorageOverride(player);
                    }
                } else if (part.startsWith(DISABLED_ITEM_DATA_PREFIX)) {
                    loadDisabledItems(player.getName(), part.substring(DISABLED_ITEM_DATA_PREFIX.length()));
                } else if (part.startsWith(AUTO_SELL_DATA_PREFIX)) {
                    loadAutoSellItems(player.getName(), part.substring(AUTO_SELL_DATA_PREFIX.length()));
                } else if (part.startsWith(GROUND_STORE_DATA_PREFIX)) {
                    Boolean status = parseGroundStoreStatus(part);
                    if (status != null) {
                        groundStoreToggle.put(playerId, status);
                    }
                }
            }
        }

        int databaseMax = Math.max(0, data.max());
        int permissionMax = Math.max(0, getPermissionMaxStorage(player));
        Integer overrideMax = maxOverrideData.get(playerId);
        int resolvedMax = overrideMax != null ? overrideMax : File.resolveMaxStorage(File.getMobStorageConfig(), "settings.max_storage_mode", databaseMax, permissionMax);
        playermaxdata.put(playerId, Math.max(0, resolvedMax));

        if (!toggle.containsKey(playerId)) {
            toggle.put(playerId, File.getMobStorageConfig().getBoolean("settings.default_auto_pickup", false));
        }
        if (!groundStoreToggle.containsKey(playerId)) {
            groundStoreToggle.put(playerId, File.getMobStorageConfig().getBoolean("ground_store.default_enabled", false));
        }
    }

    private static void loadStoredItems(@NotNull String playerName, @NotNull String rawData) {
        if (rawData.isEmpty()) {
            return;
        }
        for (String item : rawData.split(",")) {
            if (item == null || item.isEmpty()) {
                continue;
            }
            String[] itemParts = item.split(":");
            if (itemParts.length != 2) {
                continue;
            }
            String itemName = itemParts[0].toUpperCase(Locale.ENGLISH);
            if (!isConfiguredDrop(itemName)) {
                continue;
            }
            try {
                int value = Integer.parseInt(itemParts[1]);
                if (value > 0) {
                    playerdata.put(playerName + "_mob_" + itemName, value);
                }
            } catch (NumberFormatException ignored) {
            }
        }
    }

    private static void loadDisabledItems(@NotNull String playerName, @NotNull String rawData) {
        if (rawData.isEmpty()) {
            return;
        }
        Set<String> disabledItems = new HashSet<>();
        for (String raw : rawData.split(",")) {
            if (raw == null) {
                continue;
            }
            String item = raw.trim().toUpperCase(Locale.ENGLISH);
            if (!item.isEmpty() && isConfiguredDrop(item)) {
                disabledItems.add(item);
            }
        }
        if (!disabledItems.isEmpty()) {
            disabledAutoPickupItems.put(playerName, disabledItems);
        }
    }

    private static void loadAutoSellItems(@NotNull String playerName, @NotNull String rawData) {
        if (rawData.isEmpty()) {
            return;
        }
        Set<String> enabledItems = new HashSet<>();
        for (String raw : rawData.split(",")) {
            if (raw == null) {
                continue;
            }
            String item = raw.trim().toUpperCase(Locale.ENGLISH);
            if (!item.isEmpty() && isConfiguredDrop(item)) {
                enabledItems.add(item);
            }
        }
        if (!enabledItems.isEmpty()) {
            autoSellItems.put(playerName, enabledItems);
        }
    }

    public static void savePlayerData(@NotNull Player player) {
        if (!isSystemEnabled()) {
            return;
        }

        StringBuilder mobData = new StringBuilder();
        String playerName = player.getName();
        UUID playerId = player.getUniqueId();

        for (String drop : configuredDrops) {
            String key = playerName + "_mob_" + drop;
            int amount = playerdata.getOrDefault(key, 0);
            if (amount <= 0) {
                continue;
            }
            if (mobData.length() > 0) {
                mobData.append(",");
            }
            mobData.append(drop).append(":").append(amount);
        }

        PlayerData existingData = Storage.dataStorage.getData(playerName);
        String existingDataString = existingData != null ? existingData.data() : "";
        StringBuilder finalData = new StringBuilder();
        if (existingDataString != null && !existingDataString.isEmpty()) {
            for (String part : existingDataString.split(";")) {
                if (part.startsWith("mob:")
                        || part.startsWith("mobtoggle:")
                        || part.startsWith(MAX_OVERRIDE_DATA_PREFIX)
                        || part.startsWith(DISABLED_ITEM_DATA_PREFIX)
                        || part.startsWith(AUTO_SELL_DATA_PREFIX)
                        || part.startsWith(GROUND_STORE_DATA_PREFIX)
                        || part.isEmpty()) {
                    continue;
                }
                if (finalData.length() > 0) {
                    finalData.append(";");
                }
                finalData.append(part);
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
            appendData(finalData, AUTO_SELL_DATA_PREFIX, enabledData.toString());
        }

        appendData(finalData, "mob:", mobData.toString());

        if (toggle.containsKey(playerId)) {
            appendData(finalData, "mobtoggle:", String.valueOf(toggle.get(playerId)));
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
            appendData(finalData, DISABLED_ITEM_DATA_PREFIX, disabledData.toString());
        }

        Integer maxOverride = maxOverrideData.get(playerId);
        if (maxOverride != null) {
            appendData(finalData, MAX_OVERRIDE_DATA_PREFIX, String.valueOf(Math.max(0, maxOverride)));
        }

        if (groundStoreToggle.containsKey(playerId)) {
            appendData(finalData, GROUND_STORE_DATA_PREFIX, String.valueOf(groundStoreToggle.get(playerId)));
        }

        int maxStorage = existingData != null ? existingData.max() : File.getMobStorageConfig().getInt("settings.default_max_storage", 5000);
        boolean autoPickup = toggle.getOrDefault(playerId, File.getMobStorageConfig().getBoolean("settings.default_auto_pickup", false));

        PlayerData newData = new PlayerData(playerName, finalData.toString(), Math.max(0, maxStorage), autoPickup);
        if (existingData == null) {
            Storage.dataStorage.createTable(newData);
        } else {
            Storage.dataStorage.updateTable(newData);
        }
    }

    private static void appendData(@NotNull StringBuilder builder, @NotNull String prefix, @NotNull String value) {
        if (value.isEmpty()) {
            return;
        }
        if (builder.length() > 0) {
            builder.append(";");
        }
        builder.append(prefix).append(value);
    }

    public static void cleanupPlayerData(@NotNull Player player) {
        UUID playerId = player.getUniqueId();
        toggle.remove(playerId);
        groundStoreToggle.remove(playerId);
        playermaxdata.remove(playerId);
        clearMaxStorageOverride(player);
        disabledAutoPickupItems.remove(player.getName());
        autoSellItems.remove(player.getName());
        playerdata.entrySet().removeIf(entry -> entry.getKey().startsWith(player.getName() + "_mob_"));
        String prefix = player.getUniqueId() + "_mob_";
        synchronized (pendingAutoSell) {
            pendingAutoSell.removeIf(key -> key != null && key.startsWith(prefix));
        }
        synchronized (lastAutoSellAt) {
            lastAutoSellAt.keySet().removeIf(key -> key != null && key.startsWith(prefix));
        }
    }

    private static Integer parseMaxOverride(@NotNull String part) {
        if (!part.startsWith(MAX_OVERRIDE_DATA_PREFIX)) {
            return null;
        }
        try {
            return Math.max(0, Integer.parseInt(part.substring(MAX_OVERRIDE_DATA_PREFIX.length()).trim()));
        } catch (NumberFormatException ignored) {
            return null;
        }
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
        raw = raw.trim().toLowerCase(Locale.ENGLISH);
        if (raw.isEmpty()) {
            return null;
        }
        return "true".equals(raw);
    }
}
