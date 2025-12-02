package net.danh.storage.API;

import net.danh.storage.API.exceptions.StorageException;
import net.danh.storage.Manager.MineManager;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * API for batch storage operations
 * Provides efficient methods for bulk operations on storage
 *
 * @author hongminh54
 * @version 2.3.3
 */
public class StorageBatchAPI {

    /**
     * Add multiple items to player's storage
     *
     * @param player The player
     * @param items  Map of material -> amount
     * @return Map of material -> success status
     */
    @NotNull
    public static Map<String, Boolean> addItems(@NotNull Player player, @NotNull Map<String, Integer> items) {
        Map<String, Boolean> results = new HashMap<>();

        for (Map.Entry<String, Integer> entry : items.entrySet()) {
            String material = entry.getKey();
            Integer amount = entry.getValue();

            if (amount == null || amount <= 0) {
                results.put(material, false);
                continue;
            }

            try {
                boolean success = StorageAPI.addItem(player, material, amount);
                results.put(material, success);
            } catch (StorageException e) {
                results.put(material, false);
            }
        }

        return results;
    }

    /**
     * Remove multiple items from player's storage
     *
     * @param player The player
     * @param items  Map of material -> amount
     * @return Map of material -> success status
     */
    @NotNull
    public static Map<String, Boolean> removeItems(@NotNull Player player, @NotNull Map<String, Integer> items) {
        Map<String, Boolean> results = new HashMap<>();

        for (Map.Entry<String, Integer> entry : items.entrySet()) {
            String material = entry.getKey();
            Integer amount = entry.getValue();

            if (amount == null || amount <= 0) {
                results.put(material, false);
                continue;
            }

            try {
                boolean success = StorageAPI.removeItem(player, material, amount);
                results.put(material, success);
            } catch (StorageException e) {
                results.put(material, false);
            }
        }

        return results;
    }

    /**
     * Set multiple item amounts in player's storage
     *
     * @param player The player
     * @param items  Map of material -> amount
     */
    public static void setItems(@NotNull Player player, @NotNull Map<String, Integer> items) {
        for (Map.Entry<String, Integer> entry : items.entrySet()) {
            String material = entry.getKey();
            Integer amount = entry.getValue();

            if (amount != null) {
                MineManager.setBlock(player, material, Math.max(0, amount));
            }
        }
    }

    /**
     * Get amounts of multiple materials from player's storage
     *
     * @param player    The player
     * @param materials List of materials to query
     * @return Map of material -> amount
     */
    @NotNull
    public static Map<String, Integer> getMultipleItemAmounts(@NotNull Player player, @NotNull List<String> materials) {
        Map<String, Integer> results = new HashMap<>();

        for (String material : materials) {
            int amount = StorageAPI.getItemAmount(player, material);
            results.put(material, amount);
        }

        return results;
    }

    /**
     * Get storage data for multiple players
     *
     * @param players List of players
     * @return Map of Player -> Map of material -> amount
     */
    @NotNull
    public static Map<Player, Map<String, Integer>> getMultiplePlayersStorage(@NotNull List<Player> players) {
        Map<Player, Map<String, Integer>> results = new HashMap<>();

        for (Player player : players) {
            StoragePlayer storagePlayer = StorageAPI.getStoragePlayer(player);
            Map<String, Integer> playerData = storagePlayer.getStoredMaterialsWithAmounts();
            results.put(player, playerData);
        }

        return results;
    }

    /**
     * Check if player has all specified items in required amounts
     *
     * @param player The player
     * @param items  Map of material -> required amount
     * @return true if player has all items in sufficient amounts
     */
    public static boolean hasAllItems(@NotNull Player player, @NotNull Map<String, Integer> items) {
        for (Map.Entry<String, Integer> entry : items.entrySet()) {
            String material = entry.getKey();
            Integer requiredAmount = entry.getValue();

            if (requiredAmount == null || requiredAmount <= 0) {
                continue;
            }

            int currentAmount = StorageAPI.getItemAmount(player, material);
            if (currentAmount < requiredAmount) {
                return false;
            }
        }

        return true;
    }

    /**
     * Clear multiple materials from player's storage
     *
     * @param player    The player
     * @param materials List of materials to clear
     */
    public static void clearMaterials(@NotNull Player player, @NotNull List<String> materials) {
        for (String material : materials) {
            MineManager.setBlock(player, material, 0);
        }
    }

    /**
     * Transfer multiple items between players
     *
     * @param sender   Sender player
     * @param receiver Receiver player
     * @param items    Map of material -> amount
     * @return Map of material -> success status
     */
    @NotNull
    public static Map<String, Boolean> transferMultipleItems(@NotNull Player sender, @NotNull Player receiver,
                                                             @NotNull Map<String, Integer> items) {
        Map<String, Boolean> results = new HashMap<>();

        for (Map.Entry<String, Integer> entry : items.entrySet()) {
            String material = entry.getKey();
            Integer amount = entry.getValue();

            if (amount == null || amount <= 0) {
                results.put(material, false);
                continue;
            }

            try {
                boolean success = StorageAPI.transferItem(sender, receiver, material, amount);
                results.put(material, success);
            } catch (StorageException e) {
                results.put(material, false);
            }
        }

        return results;
    }

    /**
     * Async add multiple items to player's storage
     * Note: The actual storage operations run on main thread to properly fire events
     *
     * @param player The player
     * @param items  Map of material -> amount
     * @return CompletableFuture with results
     */
    @NotNull
    public static CompletableFuture<Map<String, Boolean>> addItemsAsync(@NotNull Player player,
                                                                        @NotNull Map<String, Integer> items) {
        CompletableFuture<Map<String, Boolean>> future = new CompletableFuture<>();
        org.bukkit.Bukkit.getScheduler().runTask(StorageAPI.getPlugin(), () -> {
            Map<String, Boolean> result = addItems(player, items);
            future.complete(result);
        });
        return future;
    }

    /**
     * Async remove multiple items from player's storage
     * Note: The actual storage operations run on main thread to properly fire events
     *
     * @param player The player
     * @param items  Map of material -> amount
     * @return CompletableFuture with results
     */
    @NotNull
    public static CompletableFuture<Map<String, Boolean>> removeItemsAsync(@NotNull Player player,
                                                                           @NotNull Map<String, Integer> items) {
        CompletableFuture<Map<String, Boolean>> future = new CompletableFuture<>();
        org.bukkit.Bukkit.getScheduler().runTask(StorageAPI.getPlugin(), () -> {
            Map<String, Boolean> result = removeItems(player, items);
            future.complete(result);
        });
        return future;
    }

    /**
     * Async get storage data for multiple players
     * Note: This is safe to run async as it only reads data
     *
     * @param players List of players
     * @return CompletableFuture with results
     */
    @NotNull
    public static CompletableFuture<Map<Player, Map<String, Integer>>> getMultiplePlayersStorageAsync(
            @NotNull List<Player> players) {
        return CompletableFuture.supplyAsync(() -> getMultiplePlayersStorage(players));
    }
}
