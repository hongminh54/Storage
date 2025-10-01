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

    private static final HashMap<String, Integer> playerData = new HashMap<>();
    private static final HashMap<Player, Boolean> toggle = new HashMap<>();
    private static final HashMap<Player, Integer> playerMaxData = new HashMap<>();
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
        Storage.getStorage().getLogger().info("[MythicStorage] Configured items: " + String.join(", ", configuredDrops));
    }

    public static boolean isSystemEnabled() {
        return systemEnabled && mythicMobsHelper != null && mythicMobsHelper.isInitialized();
    }

    public static MythicMobsHelper getMythicMobsHelper() {
        return mythicMobsHelper;
    }

    private static void loadConfiguredDrops() {
        configuredDrops = File.getMythicStorageConfig().getStringList("items_drop");
        if (configuredDrops == null) {
            configuredDrops = new ArrayList<>();
        }

        Storage.getStorage().getLogger().info("[MythicStorage] Loading " + configuredDrops.size() + " items from config...");

        configuredDrops.removeIf(itemName -> {
            if (!mythicMobsHelper.isValidMythicItem(itemName)) {
                Storage.getStorage().getLogger().warning("[MythicStorage] Invalid MythicMobs item in config: " + itemName);
                return true;
            }
            Storage.getStorage().getLogger().fine("[MythicStorage] Validated: " + itemName);
            return false;
        });
    }

    public static void reloadConfiguredDrops() {
        loadConfiguredDrops();
    }

    public static List<String> getConfiguredDrops() {
        return new ArrayList<>(configuredDrops);
    }

    public static boolean isConfiguredDrop(@NotNull String itemName) {
        return configuredDrops.contains(itemName);
    }

    public static int getPlayerItem(@NotNull Player player, @NotNull String itemName) {
        return playerData.getOrDefault(player.getName() + "_" + itemName, 0);
    }

    public static boolean hasPlayerItem(@NotNull Player player, @NotNull String itemName) {
        return playerData.containsKey(player.getName() + "_" + itemName);
    }

    public static int getMaxStorage(@NotNull Player player) {
        return playerMaxData.getOrDefault(player, File.getMythicStorageConfig().getInt("settings.default_max_storage", 100000));
    }

    public static boolean getToggleStatus(@NotNull Player player) {
        return toggle.getOrDefault(player, File.getMythicStorageConfig().getBoolean("settings.default_auto_pickup", false));
    }

    public static void setToggleStatus(@NotNull Player player, boolean status) {
        toggle.put(player, status);
    }

    public static boolean addItemAmount(@NotNull Player player, @NotNull String itemName, int amount) {
        if (!isSystemEnabled()) return false;
        if (!isConfiguredDrop(itemName)) return false;

        String key = player.getName() + "_" + itemName;
        int current = playerData.getOrDefault(key, 0);
        int max = getMaxStorage(player);

        if (current >= max) {
            return false;
        }

        int newAmount = Math.min(current + amount, max);
        playerData.put(key, newAmount);
        return true;
    }

    public static boolean removeItemAmount(@NotNull Player player, @NotNull String itemName, int amount) {
        if (!isSystemEnabled()) return false;

        String key = player.getName() + "_" + itemName;
        int current = playerData.getOrDefault(key, 0);

        if (current < amount) {
            return false;
        }

        playerData.put(key, current - amount);
        return true;
    }

    public static void setItemAmount(@NotNull Player player, @NotNull String itemName, int amount) {
        String key = player.getName() + "_" + itemName;
        if (amount <= 0) {
            playerData.remove(key);
        } else {
            playerData.put(key, amount);
        }
    }

    public static void loadPlayerData(@NotNull Player player) {
        if (!isSystemEnabled()) return;

        PlayerData data = Storage.dataStorage.getData(player.getName());
        if (data == null) {
            playerMaxData.put(player, File.getMythicStorageConfig().getInt("settings.default_max_storage", 100000));
            toggle.put(player, File.getMythicStorageConfig().getBoolean("settings.default_auto_pickup", false));
            return;
        }

        String dataString = data.getData();
        if (dataString == null || dataString.isEmpty()) {
            playerMaxData.put(player, data.getMax());
            toggle.put(player, File.getMythicStorageConfig().getBoolean("settings.default_auto_pickup", false));
            return;
        }

        String[] dataParts = dataString.split(";");
        for (String part : dataParts) {
            if (part.startsWith("mythic:")) {
                String mythicData = part.substring(7);
                if (mythicData.isEmpty()) continue;

                String[] items = mythicData.split(",");
                for (String item : items) {
                    if (item.isEmpty()) continue;
                    String[] itemParts = item.split(":");
                    if (itemParts.length == 2) {
                        String key = player.getName() + "_" + itemParts[0];
                        try {
                            playerData.put(key, Integer.parseInt(itemParts[1]));
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

        playerMaxData.put(player, data.getMax());
    }

    public static void savePlayerData(@NotNull Player player) {
        if (!isSystemEnabled()) return;

        StringBuilder mythicData = new StringBuilder();
        String playerName = player.getName();

        for (String drop : configuredDrops) {
            String key = playerName + "_" + drop;
            if (playerData.containsKey(key)) {
                int amount = playerData.get(key);
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

        int maxStorage = playerMaxData.getOrDefault(player,
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
        String playerName = player.getName();
        playerData.entrySet().removeIf(entry -> entry.getKey().startsWith(playerName + "_"));
        toggle.remove(player);
        playerMaxData.remove(player);
    }

    public static HashMap<String, Integer> getPlayerAllItems(@NotNull Player player) {
        HashMap<String, Integer> items = new HashMap<>();
        String playerName = player.getName();

        for (String drop : configuredDrops) {
            String key = playerName + "_" + drop;
            if (playerData.containsKey(key)) {
                items.put(drop, playerData.get(key));
            }
        }

        return items;
    }
}
