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
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class MineManager {

    private static final NMSAssistant NMS = new NMSAssistant();
    private static final boolean IS_LEGACY = NMS.isVersionLessThanOrEqualTo(12);
    private static final boolean IS_BEFORE_9 = NMS.isVersionLessThan(9);
    private static final Map<Material, String> inventoryDropLookup =
            new EnumMap<>(Material.class);
    public static HashMap<String, Integer> playerdata = new HashMap<>();
    public static HashMap<Player, Integer> playermaxdata = new HashMap<>();
    public static HashMap<String, String> blocksdata = new HashMap<>();
    public static HashMap<String, String> blocksdrop = new HashMap<>();
    public static HashMap<Player, Boolean> toggle = new HashMap<>();

    public static int getPlayerBlock(@NotNull Player p, String material) {
        return playerdata.getOrDefault(p.getName() + "_" + material, 0);
    }

    public static boolean hasPlayerBlock(@NotNull Player p, String material) {
        return playerdata.containsKey(p.getName() + "_" + material);
    }

    public static int getMaxBlock(Player p) {
        return playermaxdata.get(p);
    }

    @Contract(" -> new")
    public static @NotNull List<String> getPluginBlocks() {
        return new ArrayList<>(blocksdata.values());
    }

    @Contract(" -> new")
    public static @NotNull List<String> getOrderedPluginBlocks() {
        List<String> orderedBlocks = new ArrayList<>();
        for (String block_break : Objects.requireNonNull(File.getConfig().getConfigurationSection("blocks")).getKeys(false)) {
            String item_drop = File.getConfig().getString("blocks." + block_break + ".drop");
            NMSAssistant nms = new NMSAssistant();
            if (item_drop != null) {
                if (!item_drop.contains(";")) {
                    String material = item_drop + ";0";
                    if (!orderedBlocks.contains(material)) {
                        orderedBlocks.add(material);
                    }
                } else {
                    if (nms.isVersionLessThanOrEqualTo(12)) {
                        String[] item_data = item_drop.split(";");
                        String item_material = item_data[0] + ";" + item_data[1];
                        if (!orderedBlocks.contains(item_material)) {
                            orderedBlocks.add(item_material);
                        }
                    } else {
                        String[] item_data = item_drop.split(";");
                        String material = item_data[0] + ";0";
                        if (!orderedBlocks.contains(material)) {
                            orderedBlocks.add(material);
                        }
                    }
                }
            }
        }
        return orderedBlocks;
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
            toggle.put(player, defaultAutoPickup);
        } else {
            toggle.put(player, playerStats.isAutoPickup());
        }

        return playerStats;
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
        return mapAsString.toString();
    }

    public static @NotNull List<String> convertOnlineData(@NotNull String data) {
        String data_1 = data.replace("{", "").replace("}", "").replace(" ", "");
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

    public static boolean removeBlockAmount(Player p, String material, int amount) {
        return removeBlockAmount(p, material, amount, true);
    }

    public static boolean removeBlockAmount(Player p, String material, int amount, boolean fireEvent) {
        if (amount <= 0) return false;

        int oldData = getPlayerBlock(p, material);
        if (oldData <= 0) return false;

        int amountToRemove = amount;

        if (fireEvent) {
            if (!StorageHookAPI.callBeforeWithdraw(p, material, amountToRemove)) return false;

            StorageWithdrawEvent event = new StorageWithdrawEvent(p, material, amountToRemove);
            Bukkit.getPluginManager().callEvent(event);
            if (event.isCancelled()) return false;
            amountToRemove = event.getAmount();
        }

        playerdata.replace(p.getName() + "_" + material, Math.max(oldData - amountToRemove, 0));

        if (fireEvent) {
            StorageHookAPI.callAfterWithdraw(p, material, amountToRemove);
        }
        return true;
    }

    public static void loadPlayerData(Player p) {
        PlayerData playerData = getPlayerDatabase(p);
        List<String> list = convertOnlineData(playerData.getData());
        playermaxdata.put(p, playerData.getMax());
        setBlock(p, list);

        if (!toggle.containsKey(p)) {
            toggle.put(p, playerData.isAutoPickup());
        }
    }

    public static void savePlayerData(@NotNull Player p) {
        boolean autoPickup = toggle.getOrDefault(p, false);
        PlayerData playerData = new PlayerData(p.getName(), convertOfflineData(p), getMaxBlock(p), autoPickup);
        Storage.db.updateTable(playerData);
    }

    public static void cleanupPlayerData(@NotNull Player p) {
        toggle.remove(p);
        playermaxdata.remove(p);

        String playerName = p.getName();
        playerdata.entrySet().removeIf(entry -> entry.getKey().startsWith(playerName + "_"));
    }

    public static boolean getToggleStatus(@NotNull Player p) {
        Boolean status = toggle.get(p);
        if (status == null) {
            PlayerData playerData = getPlayerDatabase(p);
            status = playerData.isAutoPickup();
            toggle.put(p, status);
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

        toggle.put(p, enabled);

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
        String blockType = block.getType().name();
        if (blockType.equals("GLOWING_REDSTONE_ORE")) {
            blockType = "REDSTONE_ORE";
        }
        return blocksdrop.get(blockType + ";" + (IS_LEGACY ? block.getData() : "0"));
    }

    public static void loadBlocks() {
        if (!blocksdrop.isEmpty()) {
            blocksdrop.clear();
        }
        if (!blocksdata.isEmpty()) {
            blocksdata.clear();
        }
        if (!inventoryDropLookup.isEmpty()) {
            inventoryDropLookup.clear();
        }
        for (String block_break : Objects.requireNonNull(File.getConfig().getConfigurationSection("blocks")).getKeys(false)) {
            String item_drop = File.getConfig().getString("blocks." + block_break + ".drop");
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
        }
    }

    private static void addInventoryLookupEntry(String dropKey) {
        if (dropKey == null || dropKey.isEmpty()) {
            return;
        }

        String materialName = dropKey;
        if (dropKey.contains(";")) {
            materialName = dropKey.split(";", 2)[0];
        }

        Optional<XMaterial> xMaterial = XMaterial.matchXMaterial(materialName);
        if (!xMaterial.isPresent()) {
            return;
        }

        Material material = xMaterial.get().parseMaterial();
        if (material == null) {
            return;
        }

        inventoryDropLookup.put(material, dropKey);
    }

    public static boolean checkBreak(@NotNull Block block) {
        String blockType = block.getType().name();
        if (blockType.equals("GLOWING_REDSTONE_ORE")) {
            blockType = "REDSTONE_ORE";
        }
        String dataKey = blockType + ";" + (IS_LEGACY ? block.getData() : "0");
        return blocksdrop.containsKey(dataKey) || blocksdrop.containsKey(blockType);
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
        return inventoryDropLookup.get(item.getType());
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
    }

}
