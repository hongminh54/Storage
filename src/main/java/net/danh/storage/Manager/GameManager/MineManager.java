package net.danh.storage.Manager.GameManager;

import net.danh.storage.Manager.UtilsManager.FileManager;
import net.danh.storage.NMS.NMSAssistant;
import net.danh.storage.Storage;
import net.danh.storage.Utils.Number;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.logging.Level;

public class MineManager {

    public static HashMap<String, Integer> playerdata = new HashMap<>();
    public static HashMap<Player, Integer> playermaxdata = new HashMap<>();
    public static HashMap<String, String> blocksdata = new HashMap<>();
    public static HashMap<String, String> blocksdrop = new HashMap<>();

    public static int getPlayerBlock(Player p, String material) {
        return playerdata.get(p.getName() + "_" + material);
    }

    public static boolean hasPlayerBlock(Player p, String material) {
        return playerdata.containsKey(p.getName() + "_" + material);
    }

    public static int getMaxBlock(Player p) {
        return playermaxdata.get(p);
    }

    public static List<String> getPluginBlocks() {
        return new ArrayList<>(blocksdata.values());
    }

    public static void addPluginBlocks(String material) {
        blocksdata.put(material, material);
    }

    public static PlayerData getPlayerDatabase(Player player) throws SQLException {

        PlayerData playerStats = Storage.db.getData(player.getName());

        if (playerStats == null) {
            playerStats = new PlayerData(player.getName(), createNewData(), FileManager.getConfig().getInt("settings.default_max_storage"));
            Storage.db.createTable(playerStats);
        }

        return playerStats;
    }

    private static String createNewData() {
        StringBuilder mapAsString = new StringBuilder("{");
        for (String block : getPluginBlocks()) {
            mapAsString.append(block).append("=").append(0).append(", ");
        }
        mapAsString.delete(mapAsString.length() - 2, mapAsString.length()).append("}");
        return mapAsString.toString();
    }

    public static String convertOfflineData(Player p) {
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

    public static List<String> convertOnlineData(String data) {
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

    public static void setBlock(Player p, String material, int amount) {
        playerdata.put(p.getName() + "_" + material, amount);
    }

    public static void setBlock(Player p, List<String> list) {
        list.forEach(block -> {
            String[] block_data = block.split(";");
            String material = block_data[0];
            int amount = Number.getInteger(block_data[1]);
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
            return false;
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
        try {
            PlayerData playerData = getPlayerDatabase(p);
            List<String> list = convertOnlineData(playerData.getData());
            playermaxdata.put(p, playerData.getMax());
            setBlock(p, list);
        } catch (SQLException e) {
            Storage.getStorage().getLogger().log(Level.WARNING, "Can't loadPlayerData() ! Report to author!");
            throw new RuntimeException(e);
        }
    }

    public static void savePlayerData(Player p) {
        PlayerData playerData = new PlayerData(p.getName(), convertOfflineData(p), getMaxBlock(p));
        Storage.db.updateTable(playerData);
    }

    public static boolean checkBreak(Block block) {
        return FileManager.getConfig().getString("blocks." + block.getType().name() + ".drop") != null;
    }

    public static String getDrop(Block block) {
        return blocksdrop.get(block.getType().name());
    }


    public static void loadBlocks() {
        for (String block_break : Objects.requireNonNull(FileManager.getConfig().getConfigurationSection("blocks")).getKeys(false)) {
            NMSAssistant nms = new NMSAssistant();
            if (nms.isVersionGreaterThan(12)) {
                String item_drop = FileManager.getConfig().getString("blocks." + block_break + ".drop");
                if (item_drop != null) {
                    addPluginBlocks(item_drop);
                    blocksdrop.put(block_break, item_drop);
                }
            }
            if (nms.isVersionGreaterThan(12)) {
                String item_drop = FileManager.getConfig().getString("blocks." + block_break + ".drop");
                if (item_drop != null) {
                    String[] item_data = item_drop.split(";");
                    if (item_data.length == 1) {
                        String item_material = item_data[0];
                        addPluginBlocks(item_material);
                        blocksdrop.put(block_break, item_material);
                    } else if (item_data.length == 2) {
                        String item_material = item_data[0] + ";" + item_data[1];
                        addPluginBlocks(item_material);
                        blocksdrop.put(block_break, item_material);
                    }
                }
            }
        }
    }
}
