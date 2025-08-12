package net.danh.storage.Manager;

import com.cryptomorin.xseries.XMaterial;
import net.danh.storage.Database.PlayerData;
import net.danh.storage.NMS.NMSAssistant;
import net.danh.storage.Storage;
import net.danh.storage.Utils.File;
import net.danh.storage.Utils.Number;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class MineManager {

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
        if (amount > 0 && blocksdata.containsKey(material)) {
            int currentAmount = getPlayerBlock(p, material);
            int maxStorage = getMaxBlock(p);

            if (currentAmount >= maxStorage) return false;

            int availableSpace = maxStorage - currentAmount;
            int amountToAdd = Math.min(amount, availableSpace);
            int newAmount = currentAmount + amountToAdd;

            playerdata.put(p.getName() + "_" + material, newAmount);
            return amountToAdd > 0;
        }
        return false;
    }

    public static boolean removeBlockAmount(Player p, String material, int amount) {
        if (amount > 0) {
            int old_data = getPlayerBlock(p, material);
            int new_data = old_data - amount;
            if (old_data <= 0) return false;
            playerdata.replace(p.getName() + "_" + material, Math.max(new_data, 0));
            return true;
        }
        return false;
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
        return blocksdrop.get(block.getType() + ";" + (new NMSAssistant().isVersionLessThanOrEqualTo(12) ? block.getData() : "0"));
    }

    public static void loadBlocks() {
        if (!blocksdrop.isEmpty()) {
            blocksdrop.clear();
        }
        if (!blocksdata.isEmpty()) {
            blocksdata.clear();
        }
        for (String block_break : Objects.requireNonNull(File.getConfig().getConfigurationSection("blocks")).getKeys(false)) {
            String item_drop = File.getConfig().getString("blocks." + block_break + ".drop");
            NMSAssistant nms = new NMSAssistant();
            if (item_drop != null) {
                if (!item_drop.contains(";")) {
                    addPluginBlocks(item_drop + ";0");
                    blocksdrop.put(block_break, item_drop + ";0");
                } else {
                    if (nms.isVersionLessThanOrEqualTo(12)) {
                        String[] item_data = item_drop.split(";");
                        String item_material = item_data[0] + ";" + item_data[1];
                        addPluginBlocks(item_material);
                        blocksdrop.put(block_break, item_material);
                    } else {
                        String[] item_data = item_drop.split(";");
                        addPluginBlocks(item_data[0] + ";0");
                        blocksdrop.put(block_break, item_data[0] + ";0");
                    }
                }
            }
        }
    }

    public static boolean checkBreak(@NotNull Block block) {
        if (File.getConfig().contains("blocks." + block.getType().name() + ";" + (new NMSAssistant().isVersionLessThanOrEqualTo(12) ? block.getData() : "0") + ".drop")) {
            return File.getConfig().getString("blocks." + block.getType().name() + ";" + (new NMSAssistant().isVersionLessThanOrEqualTo(12) ? block.getData() : "0") + ".drop") != null;
        } else if (File.getConfig().contains("blocks." + block.getType().name() + ".drop")) {
            return File.getConfig().getString("blocks." + block.getType().name() + ".drop") != null;
        }
        return false;
    }

    public static String getMaterial(String material) {
        String material_data = material.replace(":", ";");
        NMSAssistant nms = new NMSAssistant();
        if (nms.isVersionGreaterThanOrEqualTo(13)) {
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
        for (String drops : getPluginBlocks()) {
            if (drops != null) {
                NMSAssistant nms = new NMSAssistant();
                if (nms.isVersionLessThanOrEqualTo(12)) {
                    Optional<XMaterial> xMaterial = XMaterial.matchXMaterial(drops);
                    if (xMaterial.isPresent()) {
                        ItemStack itemStack = xMaterial.get().parseItem();
                        if (itemStack != null && item.getType().equals(itemStack.getType())) {
                            return drops;
                        }
                    }
                }
                if (drops.contains(";")) {
                    Optional<XMaterial> xMaterial = XMaterial.matchXMaterial(drops.split(";")[0]);
                    if (xMaterial.isPresent()) {
                        ItemStack itemStack = xMaterial.get().parseItem();
                        if (itemStack != null && item.getType().equals(itemStack.getType())) {
                            return drops;
                        }
                    }
                } else {
                    Optional<XMaterial> xMaterial = XMaterial.matchXMaterial(drops);
                    if (xMaterial.isPresent()) {
                        ItemStack itemStack = xMaterial.get().parseItem();
                        if (itemStack != null && item.getType().equals(itemStack.getType())) {
                            return drops;
                        }
                    }
                }
            }
        }
        return null;
    }

}
