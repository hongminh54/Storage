package net.danh.storage.Manager;

import net.danh.storage.Database.PlayerData;
import net.danh.storage.MythicMobs.MythicMobsHelper;
import net.danh.storage.Storage;
import net.danh.storage.Utils.File;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class MythicStorageManager {

    public static HashMap<String, Integer> playerdata = new HashMap<>();
    public static HashMap<Player, Boolean> toggle = new HashMap<>();
    public static HashMap<Player, Integer> playermaxdata = new HashMap<>();
    private static List<String> configuredDrops = new ArrayList<>();
    private static MythicMobsHelper mythicMobsHelper;
    private static boolean systemEnabled = false;

    public static void initialize() {
        Storage.getStorage().getLogger().info("[MythicStorage] Initializing MythicStorage feature...");
        mythicMobsHelper = new MythicMobsHelper();
        if (!mythicMobsHelper.isInitialized()) {
            Storage.getStorage().getLogger().warning("[MythicStorage] MythicMobs not found! MythicStorage feature will be disabled.");
            systemEnabled = false;
            return;
        }

        systemEnabled = File.getMythicStorageConfig().getBoolean("settings.enabled", true);
        if (!systemEnabled) {
            Storage.getStorage().getLogger().info("[MythicStorage] Feature is disabled in config");
            return;
        }

        loadConfiguredDrops();
        Storage.getStorage().getLogger().info("[MythicStorage] Initialized successfully with " + configuredDrops.size() + " configured drops");
    }

    public static boolean isSystemEnabled() {
        return systemEnabled && mythicMobsHelper != null && mythicMobsHelper.isInitialized();
    }

    public static MythicMobsHelper getMythicMobsHelper() {
        return mythicMobsHelper;
    }

    private static void loadConfiguredDrops() {
        List<String> items = File.getMythicStorageConfig().getStringList("items_drop");
        if (items == null) {
            configuredDrops = new ArrayList<>();
            return;
        }

        configuredDrops = new ArrayList<>();
        for (String itemName : items) {
            if (itemName == null || itemName.trim().isEmpty()) continue;

            if (!mythicMobsHelper.isValidMythicItem(itemName)) {
                Storage.getStorage().getLogger().warning("[MythicStorage] Invalid MythicMobs item in config: " + itemName);
                continue;
            }
            configuredDrops.add(itemName);
        }
    }

    public static void reloadConfiguredDrops() {
        if (!isSystemEnabled()) return;
        loadConfiguredDrops();
        Storage.getStorage().getLogger().info("[MythicStorage] Reloaded " + configuredDrops.size() + " configured drops");
    }

    public static List<String> getConfiguredDrops() {
        return new ArrayList<>(configuredDrops);
    }

    public static boolean isConfiguredDrop(@NotNull String itemName) {
        return configuredDrops.contains(itemName);
    }

    public static int getPlayerItem(@NotNull Player player, @NotNull String itemName) {
        return playerdata.getOrDefault(player.getName() + "_" + itemName, 0);
    }

    public static boolean hasPlayerItem(@NotNull Player player, @NotNull String itemName) {
        return playerdata.containsKey(player.getName() + "_" + itemName);
    }

    public static int getMaxStorage(@NotNull Player player) {
        return playermaxdata.getOrDefault(player, File.getMythicStorageConfig().getInt("settings.default_max_storage", 100000));
    }

    public static boolean getToggleStatus(@NotNull Player player) {
        Boolean status = toggle.get(player);
        if (status == null) {
            status = File.getMythicStorageConfig().getBoolean("settings.default_auto_pickup", false);
            toggle.put(player, status);
        }
        return status;
    }

    public static void setToggleStatus(@NotNull Player player, boolean status) {
        toggle.put(player, status);
    }

    public static boolean addItemAmount(@NotNull Player player, @NotNull String itemName, int amount) {
        if (!isSystemEnabled()) return false;
        if (!isConfiguredDrop(itemName)) return false;
        if (amount <= 0) return false;

        String key = player.getName() + "_" + itemName;
        int current = playerdata.getOrDefault(key, 0);
        int max = getMaxStorage(player);

        if (current >= max) {
            return false;
        }

        int newAmount = Math.min(current + amount, max);
        playerdata.put(key, newAmount);
        return true;
    }

    public static boolean removeItemAmount(@NotNull Player player, @NotNull String itemName, int amount) {
        if (!isSystemEnabled()) return false;
        if (amount <= 0) return false;

        String key = player.getName() + "_" + itemName;
        int current = playerdata.getOrDefault(key, 0);

        if (current < amount) {
            return false;
        }

        int newValue = current - amount;
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
        if (!isSystemEnabled()) return;

        PlayerData data = Storage.dataStorage.getData(player.getName());
        if (data == null) {
            playermaxdata.put(player, File.getMythicStorageConfig().getInt("settings.default_max_storage", 100000));
            toggle.put(player, File.getMythicStorageConfig().getBoolean("settings.default_auto_pickup", false));
            return;
        }

        String dataString = data.getData();
        if (dataString == null || dataString.isEmpty()) {
            playermaxdata.put(player, data.getMax());
            toggle.put(player, File.getMythicStorageConfig().getBoolean("settings.default_auto_pickup", false));
            return;
        }

        // Parse data format: "mythic:item1:amount1,item2:amount2;mythictoggle:true"
        String[] dataParts = dataString.split(";");
        for (String part : dataParts) {
            if (part == null || part.isEmpty()) continue;

            if (part.startsWith("mythic:")) {
                String mythicData = part.substring(7);
                if (mythicData.isEmpty()) continue;

                String[] items = mythicData.split(",");
                for (String item : items) {
                    if (item == null || item.isEmpty()) continue;
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
                try {
                    toggle.put(player, Boolean.parseBoolean(part.substring(13)));
                } catch (Exception ignored) {
                }
            }
        }

        playermaxdata.put(player, data.getMax());
    }

    public static void savePlayerData(@NotNull Player player) {
        if (!isSystemEnabled()) return;

        StringBuilder mythicData = new StringBuilder();
        String playerName = player.getName();

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
                if (!part.startsWith("mythic:") && !part.startsWith("mythictoggle:") && !part.isEmpty()) {
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

        if (toggle.containsKey(player)) {
            if (finalData.length() > 0) {
                finalData.append(";");
            }
            finalData.append("mythictoggle:").append(toggle.get(player));
        }

        int maxStorage = playermaxdata.getOrDefault(player,
                File.getMythicStorageConfig().getInt("settings.default_max_storage", 100000));

        boolean autoPickup = MineManager.getToggleStatus(player);

        PlayerData newData = new PlayerData(playerName, finalData.toString(), maxStorage, autoPickup);

        if (existingData == null) {
            Storage.dataStorage.createTable(newData);
        } else {
            Storage.dataStorage.updateTable(newData);
        }
    }

    public static void cleanupPlayerData(@NotNull Player player) {
        if (player == null) return;
        toggle.remove(player);
        playermaxdata.remove(player);

        String playerName = player.getName();
        playerdata.entrySet().removeIf(entry -> entry.getKey().startsWith(playerName + "_"));
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
}
