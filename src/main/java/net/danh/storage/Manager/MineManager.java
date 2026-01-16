package net.danh.storage.Manager;

import com.cryptomorin.xseries.XMaterial;
import net.danh.storage.API.StorageHookAPI;
import net.danh.storage.API.events.StorageDepositEvent;
import net.danh.storage.API.events.StorageToggleEvent;
import net.danh.storage.API.events.StorageWithdrawEvent;
import net.danh.storage.Database.PlayerData;
import net.danh.storage.NMS.NMSAssistant;
import net.danh.storage.Storage;
import net.danh.storage.Utils.File;
import net.danh.storage.Utils.Number;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.permissions.PermissionAttachmentInfo;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class MineManager {

    private static final NMSAssistant NMS = new NMSAssistant();
    private static final boolean IS_LEGACY = NMS.isVersionLessThanOrEqualTo(12);
    private static final boolean IS_BEFORE_9 = NMS.isVersionLessThan(9);
    private static final Map<InvLookupKey, String> inventoryDropLookup =
            new HashMap<>();
    private static final String STORAGE_LIMIT_PERMISSION_PREFIX =
            "storage.storage.";
    private static final String STORAGE_LIMIT_PERMISSION_MAX_PREFIX =
            "storage.storage.max.";
    private static final HashMap<String, Set<String>> disabledAutoPickupItems =
            new HashMap<>();
    private static final HashMap<UUID, Boolean> groundStoreToggle =
            new HashMap<>();
    private static final String GROUND_STORE_DATA_PREFIX = ";groundstore:";
    private static final String TOGGLE_DATA_PREFIX = ";toggle:";
    private static final String MAX_OVERRIDE_DATA_PREFIX = ";maxoverride:";
    private static final HashMap<UUID, Integer> maxOverrideData =
            new HashMap<>();
    public static HashMap<String, Integer> playerdata = new HashMap<>();
    public static HashMap<UUID, Integer> playermaxdata = new HashMap<>();
    public static HashMap<String, String> blocksdata = new HashMap<>();
    public static HashMap<String, String> blocksdrop = new HashMap<>();
    public static HashMap<String, String> blocksdropAutoSmelt = new HashMap<>();
    public static HashMap<UUID, Boolean> toggle = new HashMap<>();

    public static int getPlayerBlock(@NotNull Player p, String material) {
        return playerdata.getOrDefault(p.getName() + "_" + material, 0);
    }

    public static boolean hasPlayerBlock(@NotNull Player p, String material) {
        return playerdata.containsKey(p.getName() + "_" + material);
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

    public static int getMaxBlock(Player p) {
        if (p == null) {
            return File.getConfig().getInt("settings.default_max_storage",
                    100000);
        }
        return playermaxdata.getOrDefault(p.getUniqueId(), File.getConfig().getInt(
                "settings.default_max_storage",
                100000
        ));
    }

    @Contract(" -> new")
    public static @NotNull List<String> getPluginBlocks() {
        return new ArrayList<>(blocksdata.values());
    }

    @Contract(" -> new")
    public static @NotNull List<String> getOrderedPluginBlocks() {
        List<String> orderedBlocks = new ArrayList<>();
        ConfigurationSection section = File.getConfig().getConfigurationSection(
                "blocks"
        );
        if (section == null) {
            return orderedBlocks;
        }
        for (String block_break : section.getKeys(false)) {
            String item_drop = File.getConfig().getString("blocks." + block_break + ".drop");
            String item_drop_autosmelt = File.getConfig().getString(
                    "blocks." + block_break + ".drop_autosmelt"
            );

            addOrderedDrop(orderedBlocks, item_drop);
            addOrderedDrop(orderedBlocks, item_drop_autosmelt);
        }
        return orderedBlocks;
    }

    private static void addOrderedDrop(@NotNull List<String> orderedBlocks,
                                       String configuredDrop) {
        if (configuredDrop == null) {
            return;
        }
        if (!configuredDrop.contains(";")) {
            String material = configuredDrop + ";0";
            if (!orderedBlocks.contains(material)) {
                orderedBlocks.add(material);
            }
            return;
        }

        String[] item_data = configuredDrop.split(";");
        if (IS_LEGACY) {
            String item_material = item_data[0] + ";" + item_data[1];
            if (!orderedBlocks.contains(item_material)) {
                orderedBlocks.add(item_material);
            }
        } else {
            String material = item_data[0] + ";0";
            if (!orderedBlocks.contains(material)) {
                orderedBlocks.add(material);
            }
        }
    }

    public static void addPluginBlocks(String material) {
        blocksdata.put(material, material);

    }

    public static @NotNull PlayerData getPlayerDatabase(@NotNull Player player) {

        PlayerData playerStats = Storage.db.getData(player.getName());

        if (playerStats == null) {
            boolean defaultAutoPickup = File.getConfig().getBoolean("settings.default_auto_pickup");
            playerStats = new PlayerData(player.getName(), createNewData(), File.getConfig().getInt("settings.default_max_storage"), defaultAutoPickup);
            Storage.db.createTable(playerStats);
            toggle.put(player.getUniqueId(), defaultAutoPickup);
        } else {
            toggle.put(player.getUniqueId(), playerStats.isAutoPickup());
        }

        return playerStats;
    }

    public static boolean isGroundStoreSystemEnabled() {
        return File.getConfig().getBoolean("ground_store.enabled", true);
    }

    public static boolean isGroundStoreEnabled(@NotNull Player player) {
        if (!isGroundStoreSystemEnabled()) {
            return false;
        }
        UUID playerId = player.getUniqueId();
        Boolean status = groundStoreToggle.get(playerId);
        if (status == null) {
            PlayerData playerData = getPlayerDatabase(player);
            status = parseGroundStoreStatus(playerData.getData());
            if (status == null) {
                status = File.getConfig().getBoolean(
                        "ground_store.default_enabled",
                        false
                );
            }
            groundStoreToggle.put(playerId, status);
        }
        return status;
    }

    public static boolean toggleGroundStore(@NotNull Player player) {
        boolean current = isGroundStoreEnabled(player);
        boolean next = !current;
        groundStoreToggle.put(player.getUniqueId(), next);
        savePlayerData(player);
        return next;
    }

    public static boolean isGroundStoreItemAllowed(@NotNull String dropKey) {
        if (!getPluginBlocks().contains(dropKey)) {
            return false;
        }

        List<String> allowed = File.getConfig().getStringList(
                "ground_store.allowed_items"
        );
        if (allowed == null || allowed.isEmpty()) {
            return false;
        }
        return allowed.contains(dropKey);
    }

    private static @NotNull String createNewData() {
        StringBuilder mapAsString = new StringBuilder("{");
        for (String block : getPluginBlocks()) {
            mapAsString.append(block).append("=").append(0).append(", ");
        }
        mapAsString.delete(mapAsString.length() - 2, mapAsString.length()).append("}");
        return mapAsString.toString();
    }

    public static @NotNull String convertOfflineData(Player p) {
        StringBuilder mapAsString = new StringBuilder("{");
        for (String block : getPluginBlocks()) {
            if (playerdata.containsKey(p.getName() + "_" + block)) {
                mapAsString.append(block).append("=").append(getPlayerBlock(p, block)).append(", ");
            } else {
                mapAsString.append(block).append("=").append(0).append(", ");
            }
        }
        mapAsString.delete(mapAsString.length() - 2, mapAsString.length()).append("}");

        Set<String> disabledItems = disabledAutoPickupItems.get(p.getName());
        if (disabledItems != null && !disabledItems.isEmpty()) {
            StringBuilder disabledData = new StringBuilder();
            for (String key : getPluginBlocks()) {
                if (!disabledItems.contains(key)) {
                    continue;
                }
                if (disabledData.length() > 0) {
                    disabledData.append(",");
                }
                disabledData.append(key);
            }
            if (disabledData.length() > 0) {
                mapAsString.append(";autopickupoff:")
                        .append(disabledData);
            }
        }

        UUID playerId = p.getUniqueId();
        if (groundStoreToggle.containsKey(playerId)) {
            mapAsString.append(GROUND_STORE_DATA_PREFIX)
                    .append(groundStoreToggle.get(playerId));
        }

        mapAsString.append(TOGGLE_DATA_PREFIX)
                .append(getToggleStatus(p));

        Integer maxOverride = getMaxStorageOverride(p);
        if (maxOverride != null) {
            mapAsString.append(MAX_OVERRIDE_DATA_PREFIX)
                    .append(Math.max(0, maxOverride));
        }

        return mapAsString.toString();
    }

    public static @NotNull List<String> convertOnlineData(@NotNull String data) {
        String data_1 = data;
        int start = data.indexOf('{');
        int end = data.indexOf('}');
        if (start >= 0 && end > start) {
            data_1 = data.substring(start + 1, end);
        }
        data_1 = data_1.replace(" ", "");
        List<String> list = new ArrayList<>();
        List<String> testlist = new ArrayList<>();
        for (String blocklist : data_1.split(",")) {
            String[] block = blocklist.split("=");
            if (getPluginBlocks().contains(block[0])) {
                list.add(block[0] + ";" + block[1]);
                testlist.add(block[0]);
            }
        }
        for (String blocklist : getPluginBlocks()) {
            if (!testlist.contains(blocklist)) {
                list.add(blocklist + ";" + 0);
                testlist.add(blocklist);
            }
        }
        return list;
    }

    public static boolean isAutoPickupEnabledForItem(@NotNull Player player,
                                                     @NotNull String material) {
        if (!getToggleStatus(player)) {
            return false;
        }
        Set<String> disabledItems = disabledAutoPickupItems.get(player.getName());
        if (disabledItems == null || disabledItems.isEmpty()) {
            return true;
        }
        return !disabledItems.contains(material);
    }

    public static boolean toggleItemAutoPickup(@NotNull Player player,
                                               @NotNull String material) {
        String playerName = player.getName();
        Set<String> disabledItems = disabledAutoPickupItems.get(playerName);
        if (disabledItems == null) {
            disabledItems = new HashSet<>();
            disabledAutoPickupItems.put(playerName, disabledItems);
        }

        if (disabledItems.contains(material)) {
            disabledItems.remove(material);
            savePlayerData(player);
            return true;
        }

        disabledItems.add(material);
        savePlayerData(player);
        return false;
    }

    private static void loadDisabledAutoPickupItems(@NotNull String playerName,
                                                    @NotNull String data) {
        disabledAutoPickupItems.remove(playerName);
        int idx = data.indexOf(";autopickupoff:");
        if (idx < 0) {
            return;
        }

        String disabledData = data.substring(idx + ";autopickupoff:"
                .length());
        if (disabledData.isEmpty()) {
            return;
        }

        Set<String> disabledItems = new HashSet<>();
        for (String raw : disabledData.split(",")) {
            if (raw == null) {
                continue;
            }
            String item = raw.trim();
            if (item.isEmpty()) {
                continue;
            }
            if (getPluginBlocks().contains(item)) {
                disabledItems.add(item);
            }
        }

        disabledAutoPickupItems.put(playerName, disabledItems);
    }

    public static void setBlock(@NotNull Player p, String material, int amount) {
        playerdata.put(p.getName() + "_" + material, amount);
    }

    public static void setBlock(Player p, @NotNull List<String> list) {
        list.forEach(block -> {
            String[] block_data = block.split(";");
            String material = block_data[0] + ";" + block_data[1];
            int amount = Number.getInteger(block_data[2]);
            setBlock(p, material, amount);
        });
    }

    public static boolean addBlockAmount(Player p, String material, int amount) {
        return addBlockAmount(p, material, amount, true);
    }

    public static boolean addBlockAmount(Player p, String material, int amount, boolean fireEvent) {
        if (amount <= 0 || !blocksdata.containsKey(material)) return false;

        int currentAmount = getPlayerBlock(p, material);
        int maxStorage = getMaxBlock(p);
        if (currentAmount >= maxStorage) return false;

        int availableSpace = maxStorage - currentAmount;
        int amountToAdd = Math.min(amount, availableSpace);
        if (amountToAdd <= 0) return false;

        if (fireEvent) {
            if (!StorageHookAPI.callBeforeDeposit(p, material, amountToAdd)) return false;

            StorageDepositEvent event = new StorageDepositEvent(p, material, amountToAdd);
            Bukkit.getPluginManager().callEvent(event);
            if (event.isCancelled()) return false;
            amountToAdd = Math.min(event.getAmount(), availableSpace);
        }

        playerdata.put(p.getName() + "_" + material, currentAmount + amountToAdd);

        if (fireEvent) {
            StorageHookAPI.callAfterDeposit(p, material, amountToAdd);
        }
        return true;
    }

    public static int getPermissionMaxStorage(@NotNull Player player) {
        int defaultMax = File.getConfig().getInt(
                "settings.default_max_storage",
                100000
        );

        if (player.isOp() || player.hasPermission("storage.admin")) {
            return defaultMax;
        }

        int bestLimit = -1;
        int bestPriority = Integer.MIN_VALUE;

        // Numeric permission: storage.storage.max.<n>
        for (PermissionAttachmentInfo pai :
                player.getEffectivePermissions()) {
            String perm = pai.getPermission();
            if (perm == null || !pai.getValue()) {
                continue;
            }
            if (!perm.startsWith(STORAGE_LIMIT_PERMISSION_MAX_PREFIX)) {
                continue;
            }

            String numberPart = perm.substring(
                    STORAGE_LIMIT_PERMISSION_MAX_PREFIX.length()
            );
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

        // Rank permission: storage.storage.<rank>
        ConfigurationSection section =
                File.getConfig().getConfigurationSection(
                        "storage_permissions"
                );
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

    public static boolean removeBlockAmount(Player p, String material,
                                            int amount) {
        return removeBlockAmount(p, material, amount, true);
    }

    public static boolean removeBlockAmount(Player p, String material,
                                            int amount, boolean fireEvent) {
        if (amount <= 0) return false;

        int oldData = getPlayerBlock(p, material);
        if (oldData <= 0) return false;

        int amountToRemove = amount;

        if (fireEvent) {
            if (!StorageHookAPI.callBeforeWithdraw(p, material,
                    amountToRemove)) return false;

            StorageWithdrawEvent event = new StorageWithdrawEvent(p, material,
                    amountToRemove);
            Bukkit.getPluginManager().callEvent(event);
            if (event.isCancelled()) return false;
            amountToRemove = event.getAmount();
        }

        playerdata.replace(p.getName() + "_" + material,
                Math.max(oldData - amountToRemove, 0));

        if (fireEvent) {
            StorageHookAPI.callAfterWithdraw(p, material, amountToRemove);
        }
        return true;
    }

    public static void loadPlayerData(Player p) {
        PlayerData playerData = getPlayerDatabase(p);
        List<String> list = convertOnlineData(playerData.getData());

        int databaseMax = Math.max(0, playerData.getMax());
        int permissionMax = Math.max(0, getPermissionMaxStorage(p));
        Integer overrideMax = null;

        setBlock(p, list);

        String rawData = playerData.getData();
        if (rawData != null && !rawData.isEmpty()) {
            loadDisabledAutoPickupItems(p.getName(), rawData);
            Integer parsedMaxOverride = parseMaxOverride(rawData);
            if (parsedMaxOverride != null) {
                overrideMax = Math.max(0, parsedMaxOverride);
                setMaxStorageOverride(p, overrideMax);
            } else {
                clearMaxStorageOverride(p);
            }
            Boolean groundStatus = parseGroundStoreStatus(rawData);
            if (groundStatus != null) {
                groundStoreToggle.put(p.getUniqueId(), groundStatus);
            }

            Boolean toggleStatus = parseToggleStatus(rawData);
            if (toggleStatus != null) {
                toggle.put(p.getUniqueId(), toggleStatus);
            }
        }

        if (!toggle.containsKey(p.getUniqueId())) {
            toggle.put(p.getUniqueId(), playerData.isAutoPickup());
        }

        if (!groundStoreToggle.containsKey(p.getUniqueId())) {
            groundStoreToggle.put(p.getUniqueId(), File.getConfig().getBoolean(
                    "ground_store.default_enabled",
                    false
            ));
        }

        int resolvedMax;
        if (overrideMax != null) {
            resolvedMax = overrideMax;
        } else {
            resolvedMax = File.resolveMaxStorage(
                    File.getConfig(),
                    "settings.max_storage_mode",
                    databaseMax,
                    permissionMax
            );
        }
        playermaxdata.put(p.getUniqueId(), Math.max(0, resolvedMax));
    }

    public static void savePlayerData(@NotNull Player p) {
        boolean autoPickup = getToggleStatus(p);
        PlayerData existing = Storage.db.getData(p.getName());
        int maxToSave;
        if (existing != null) {
            maxToSave = existing.getMax();
        } else {
            maxToSave = File.getConfig().getInt(
                    "settings.default_max_storage",
                    100000
            );
        }
        PlayerData playerData = new PlayerData(
                p.getName(),
                convertOfflineData(p),
                Math.max(0, maxToSave),
                autoPickup
        );
        if (existing == null) {
            Storage.db.createTable(playerData);
        } else {
            Storage.db.updateTable(playerData);
        }
    }

    public static void cleanupPlayerData(@NotNull Player p) {
        UUID playerId = p.getUniqueId();
        toggle.remove(playerId);
        playermaxdata.remove(playerId);
        clearMaxStorageOverride(p);
        groundStoreToggle.remove(playerId);

        String playerName = p.getName();
        playerdata.entrySet().removeIf(entry -> entry.getKey().startsWith(playerName + "_"));
        disabledAutoPickupItems.remove(playerName);
    }

    private static Boolean parseGroundStoreStatus(String data) {
        if (data == null || data.isEmpty()) {
            return null;
        }
        int idx = data.indexOf(GROUND_STORE_DATA_PREFIX);
        if (idx < 0) {
            return null;
        }
        int start = idx + GROUND_STORE_DATA_PREFIX.length();
        int end = data.indexOf(';', start);
        String raw = end >= 0 ? data.substring(start, end) : data.substring(start);
        raw = raw.trim().toLowerCase(Locale.ROOT);
        if (raw.isEmpty()) {
            return null;
        }
        return "true".equals(raw);
    }

    private static Integer parseMaxOverride(String data) {
        if (data == null || data.isEmpty()) {
            return null;
        }
        int idx = data.indexOf(MAX_OVERRIDE_DATA_PREFIX);
        if (idx < 0) {
            return null;
        }
        int start = idx + MAX_OVERRIDE_DATA_PREFIX.length();
        int end = data.indexOf(';', start);
        String raw = end >= 0 ? data.substring(start, end) : data.substring(start);
        if (raw == null) {
            return null;
        }
        raw = raw.trim();
        if (raw.isEmpty()) {
            return null;
        }
        try {
            return Integer.parseInt(raw);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private static Boolean parseToggleStatus(String data) {
        if (data == null || data.isEmpty()) {
            return null;
        }
        int idx = data.indexOf(TOGGLE_DATA_PREFIX);
        if (idx < 0) {
            return null;
        }
        int start = idx + TOGGLE_DATA_PREFIX.length();
        int end = data.indexOf(';', start);
        String raw = end >= 0 ? data.substring(start, end) : data.substring(start);
        raw = raw.trim().toLowerCase(Locale.ROOT);
        if (raw.isEmpty()) {
            return null;
        }
        return "true".equals(raw);
    }

    public static boolean getToggleStatus(@NotNull Player p) {
        UUID playerId = p.getUniqueId();
        Boolean status = toggle.get(playerId);
        if (status == null) {
            PlayerData playerData = getPlayerDatabase(p);
            Boolean parsed = parseToggleStatus(playerData.getData());
            status = parsed != null ? parsed : playerData.isAutoPickup();
            toggle.put(playerId, status);
        }
        return status;
    }

    public static void setToggleStatus(@NotNull Player p, boolean enabled) {
        setToggleStatus(p, enabled, true);
    }

    public static void setToggleStatus(@NotNull Player p, boolean enabled, boolean fireEvent) {
        if (fireEvent) {
            if (!StorageHookAPI.callBeforeToggle(p, enabled)) return;

            StorageToggleEvent event = new StorageToggleEvent(p, enabled);
            Bukkit.getPluginManager().callEvent(event);
            if (event.isCancelled()) return;
            enabled = event.getNewState();
        }

        toggle.put(p.getUniqueId(), enabled);

        if (fireEvent) {
            StorageHookAPI.callAfterToggle(p, enabled);
        }
    }

    public static int getMaxTransferableAmount(@NotNull Player sender, @NotNull Player receiver, @NotNull String material) {
        int senderAmount = getPlayerBlock(sender, material);
        if (senderAmount <= 0) {
            return 0;
        }

        int receiverMaxStorage = getMaxBlock(receiver);
        int receiverCurrentAmount = getPlayerBlock(receiver, material);
        int availableSpace = receiverMaxStorage - receiverCurrentAmount;

        return Math.min(senderAmount, Math.max(0, availableSpace));
    }

    public static @NotNull Map<String, Integer> calculateOptimalMultiTransfer(@NotNull Player sender, @NotNull Player receiver, @NotNull Map<String, Integer> requestedAmounts) {
        Map<String, Integer> optimizedAmounts = new HashMap<>();

        for (Map.Entry<String, Integer> entry : requestedAmounts.entrySet()) {
            String material = entry.getKey();
            int requestedAmount = entry.getValue();

            int maxTransferable = getMaxTransferableAmount(sender, receiver, material);

            if (maxTransferable > 0) {
                int optimalAmount = Math.min(requestedAmount, maxTransferable);
                optimizedAmounts.put(material, optimalAmount);
            }
        }

        return optimizedAmounts;
    }

    public static String getDrop(@NotNull Block block) {
        return getDrop(block, false);
    }

    public static String getDrop(@NotNull Block block, boolean preferAutoSmelt) {
        String blockType = block.getType().name();
        if (blockType.equals("GLOWING_REDSTONE_ORE")) {
            blockType = "REDSTONE_ORE";
        }
        String key = blockType + ";" + (IS_LEGACY ? block.getData() : "0");

        if (preferAutoSmelt) {
            String autoDrop = blocksdropAutoSmelt.get(key);
            if (autoDrop != null) {
                return autoDrop;
            }
            autoDrop = blocksdropAutoSmelt.get(blockType);
            if (autoDrop != null) {
                return autoDrop;
            }
            if (!IS_LEGACY) {
                String altKey = blockType + ";0";
                autoDrop = blocksdropAutoSmelt.get(altKey);
                if (autoDrop != null) {
                    return autoDrop;
                }
            }
        }

        String drop = blocksdrop.get(key);
        if (drop != null) {
            return drop;
        }
        drop = blocksdrop.get(blockType);
        if (drop != null) {
            return drop;
        }
        if (!IS_LEGACY) {
            String altKey = blockType + ";0";
            drop = blocksdrop.get(altKey);
            return drop;
        }
        return null;
    }

    public static void loadBlocks() {
        if (!blocksdrop.isEmpty()) {
            blocksdrop.clear();
        }
        if (!blocksdropAutoSmelt.isEmpty()) {
            blocksdropAutoSmelt.clear();
        }
        if (!blocksdata.isEmpty()) {
            blocksdata.clear();
        }
        if (!inventoryDropLookup.isEmpty()) {
            inventoryDropLookup.clear();
        }
        ConfigurationSection section = File.getConfig().getConfigurationSection(
                "blocks"
        );
        if (section == null) {
            return;
        }
        for (String block_break : section.getKeys(false)) {
            String item_drop = File.getConfig().getString("blocks." + block_break + ".drop");
            String item_drop_autosmelt = File.getConfig().getString(
                    "blocks." + block_break + ".drop_autosmelt"
            );
            if (item_drop != null) {
                if (!item_drop.contains(";")) {
                    String normalizedDrop = item_drop + ";0";
                    addPluginBlocks(normalizedDrop);
                    blocksdrop.put(block_break, normalizedDrop);
                    addInventoryLookupEntry(normalizedDrop);
                } else {
                    if (IS_LEGACY) {
                        String[] item_data = item_drop.split(";");
                        String item_material = item_data[0] + ";" + item_data[1];
                        addPluginBlocks(item_material);
                        blocksdrop.put(block_break, item_material);
                        addInventoryLookupEntry(item_material);
                    } else {
                        String[] item_data = item_drop.split(";");
                        String normalizedDrop = item_data[0] + ";0";
                        addPluginBlocks(normalizedDrop);
                        blocksdrop.put(block_break, normalizedDrop);
                        addInventoryLookupEntry(normalizedDrop);
                    }
                }
            }

            if (item_drop_autosmelt != null) {
                if (!item_drop_autosmelt.contains(";")) {
                    String normalizedDrop = item_drop_autosmelt + ";0";
                    addPluginBlocks(normalizedDrop);
                    blocksdropAutoSmelt.put(block_break, normalizedDrop);
                    addInventoryLookupEntry(normalizedDrop);
                } else {
                    if (IS_LEGACY) {
                        String[] item_data = item_drop_autosmelt.split(";");
                        String item_material = item_data[0] + ";" + item_data[1];
                        addPluginBlocks(item_material);
                        blocksdropAutoSmelt.put(block_break, item_material);
                        addInventoryLookupEntry(item_material);
                    } else {
                        String[] item_data = item_drop_autosmelt.split(";");
                        String normalizedDrop = item_data[0] + ";0";
                        addPluginBlocks(normalizedDrop);
                        blocksdropAutoSmelt.put(block_break, normalizedDrop);
                        addInventoryLookupEntry(normalizedDrop);
                    }
                }
            }
        }
    }

    private static void addInventoryLookupEntry(String dropKey) {
        if (dropKey == null || dropKey.isEmpty()) {
            return;
        }

        String materialName = dropKey;
        short dataValue = 0;
        if (dropKey.contains(";")) {
            String[] parts = dropKey.split(";", 2);
            materialName = parts[0];
            if (IS_LEGACY && parts.length > 1) {
                dataValue = (short) Number.getInteger(parts[1]);
            }
        }

        Optional<XMaterial> xMaterial = XMaterial.matchXMaterial(materialName);
        if (!xMaterial.isPresent()) {
            return;
        }

        Material material = xMaterial.get().parseMaterial();
        if (material == null) {
            return;
        }

        short durability = IS_LEGACY ? dataValue : 0;
        inventoryDropLookup.put(new InvLookupKey(material, durability), dropKey);
    }

    public static boolean checkBreak(@NotNull Block block) {
        String blockType = block.getType().name();
        if (blockType.equals("GLOWING_REDSTONE_ORE")) {
            blockType = "REDSTONE_ORE";
        }
        String dataKey = blockType + ";" + (IS_LEGACY ? block.getData() : "0");
        if (blocksdrop.containsKey(dataKey)) {
            return true;
        }
        if (blocksdropAutoSmelt.containsKey(dataKey)) {
            return true;
        }
        if (blocksdrop.containsKey(blockType)) {
            return true;
        }
        if (blocksdropAutoSmelt.containsKey(blockType)) {
            return true;
        }
        if (!IS_LEGACY && blocksdrop.containsKey(blockType + ";0")) {
            return true;
        }
        return !IS_LEGACY && blocksdropAutoSmelt.containsKey(blockType + ";0");
    }

    public static String normalizeMaterial(String material) {
        if (material == null || material.isEmpty()) {
            return material;
        }

        if (material.contains(";")) {
            return material;
        }

        if (material.contains(":")) {
            return material.replace(":", ";");
        }

        return material + ";0";
    }

    public static String getMaterial(String material) {
        String material_data = material.replace(":", ";");
        if (NMS.isVersionGreaterThanOrEqualTo(13)) {
            return material_data.split(";")[0] + ";0";
        } else {
            if (Number.getInteger(material_data.split(";")[1]) > 0) {
                return material;
            } else {
                return material_data.split(";")[0] + ";0";
            }
        }
    }

    public static String getItemStackDrop(ItemStack item) {
        if (item == null) {
            return null;
        }
        short durability = 0;
        if (IS_LEGACY) {
            durability = item.getDurability();
        }
        return inventoryDropLookup.get(new InvLookupKey(item.getType(), durability));
    }

    public static boolean isBefore9() {
        return IS_BEFORE_9;
    }

    public static int getPlayerBlock(@NotNull String playerName, @NotNull String material) {
        return playerdata.getOrDefault(playerName + "_" + material, 0);
    }

    public static int getMaxStorage(@NotNull String playerName) {
        return File.getConfig().getInt("settings.default_max_storage", 100000);
    }

    public static boolean loadOfflinePlayerData(@NotNull String playerName) {
        Player onlinePlayer = Bukkit.getPlayer(playerName);
        if (onlinePlayer != null) {
            return true;
        }

        for (String key : playerdata.keySet()) {
            if (key.startsWith(playerName + "_")) {
                return true;
            }
        }

        PlayerData data = Storage.db.getData(playerName);
        if (data == null) {
            return false;
        }

        String rawData = data.getData();
        if (rawData != null && !rawData.isEmpty()) {
            loadDisabledAutoPickupItems(playerName, rawData);
        }

        List<String> list = convertOnlineData(data.getData());
        for (String block : list) {
            String[] block_data = block.split(";");
            if (block_data.length >= 3) {
                String material = block_data[0] + ";" + block_data[1];
                int amount = Number.getInteger(block_data[2]);
                playerdata.put(playerName + "_" + material, amount);
            }
        }

        return true;
    }

    public static boolean hasOfflinePlayerData(@NotNull String playerName) {
        return Storage.db.getData(playerName) != null;
    }

    public static void cleanupOfflinePlayerData(@NotNull String playerName) {
        // Don't cleanup if player is online
        Player onlinePlayer = Bukkit.getPlayer(playerName);
        if (onlinePlayer != null) {
            return;
        }

        playerdata.entrySet().removeIf(entry -> entry.getKey().startsWith(playerName + "_"));
        disabledAutoPickupItems.remove(playerName);
    }

    private static final class InvLookupKey {

        private final Material material;
        private final short durability;

        private InvLookupKey(@NotNull Material material, short durability) {
            this.material = material;
            this.durability = durability;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof InvLookupKey)) {
                return false;
            }
            InvLookupKey that = (InvLookupKey) o;
            return durability == that.durability && material == that.material;
        }

        @Override
        public int hashCode() {
            int result = material.hashCode();
            result = 31 * result + durability;
            return result;
        }
    }

}
