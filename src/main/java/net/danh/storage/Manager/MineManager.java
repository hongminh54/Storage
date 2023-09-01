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
        return playerdata.get(p.getName() + "_" + material);
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

    public static void addPluginBlocks(String material) {
        blocksdata.put(material, material);

    }

    public static @NotNull PlayerData getPlayerDatabase(@NotNull Player player) {

        PlayerData playerStats = Storage.db.getData(player.getName());

        if (playerStats == null) {
            playerStats = new PlayerData(player.getName(), createNewData(), File.getConfig().getInt("settings.default_max_storage"));
            Storage.db.createTable(playerStats);
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
        if (amount > 0) {
            if (blocksdata.containsKey(material) && hasPlayerBlock(p, material)) {
                int old_data = getPlayerBlock(p, material);
                int new_data = old_data + amount;
                int max_storage = getMaxBlock(p);
                if (old_data >= max_storage) return false;
                playerdata.replace(p.getName() + "_" + material, Math.min(new_data, max_storage));
                return true;
            } else if (blocksdata.containsKey(material) && !hasPlayerBlock(p, material)) {
                setBlock(p, material, amount);
                return true;
            }
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
    }

    public static void savePlayerData(@NotNull Player p) {
        PlayerData playerData = new PlayerData(p.getName(), convertOfflineData(p), getMaxBlock(p));
        Storage.db.updateTable(playerData);
    }

    public static String getDrop(@NotNull Block block) {
        NMSAssistant nms = new NMSAssistant();
        ItemStack itemStack = XMaterial.matchXMaterial(block.getType()).parseItem();
        if (nms.isVersionLessThanOrEqualTo(12) && itemStack != null) {
            return blocksdrop.get(itemStack.getType() + ";" + XMaterial.matchXMaterial(block.getType()).parseItem().getDurability());
        }
        return blocksdrop.get(block.getType().name() + ";0");
    }


    public static void loadBlocks() {
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
        ItemStack itemStack = XMaterial.matchXMaterial(block.getType()).parseItem();
        if (itemStack != null) {
            if (File.getConfig().contains("blocks." + itemStack.getType().name() + ";" + itemStack.getDurability() + ".drop")) {
                return File.getConfig().getString("blocks." + itemStack.getType().name() + ";" + itemStack.getDurability() + ".drop") != null;
            } else if (File.getConfig().contains("blocks." + itemStack.getType().name() + ".drop")) {
                return File.getConfig().getString("blocks." + itemStack.getType().name() + ".drop") != null;
            }
            return false;
        }
        return false;
    }

    public static boolean checkBreak(@NotNull String block) {
        Optional<XMaterial> material = XMaterial.matchXMaterial(block.replace(";", ":"));
        if (material.isPresent()) {
            ItemStack itemStack = material.get().parseItem();
            if (itemStack != null) {
                if (File.getConfig().contains("blocks." + itemStack.getType().name() + ";" + itemStack.getDurability() + ".drop")) {
                    return File.getConfig().getString("blocks." + itemStack.getType().name() + ";" + itemStack.getDurability() + ".drop") != null;
                } else if (File.getConfig().contains("blocks." + itemStack.getType().name() + ".drop")) {
                    return File.getConfig().getString("blocks." + itemStack.getType().name() + ".drop") != null;
                }
                return false;
            }
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

}
