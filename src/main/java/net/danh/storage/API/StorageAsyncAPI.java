package net.danh.storage.API;

import net.danh.storage.API.exceptions.StorageException;
import net.danh.storage.Manager.MineManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

/**
 * API for asynchronous storage operations
 * Provides non-blocking methods to prevent server lag
 *
 * @author hongminh54
 * @version 2.3.3
 */
public class StorageAsyncAPI {

    /**
     * Asynchronously add item to player's storage
     *
     * @param player   The player
     * @param material Material name
     * @param amount   Amount to add
     * @return CompletableFuture with success status
     */
    @NotNull
    public static CompletableFuture<Boolean> addItemAsync(@NotNull Player player, @NotNull String material, int amount) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return StorageAPI.addItem(player, material, amount);
            } catch (StorageException e) {
                throw new CompletionException(e);
            }
        });
    }

    /**
     * Asynchronously remove item from player's storage
     *
     * @param player   The player
     * @param material Material name
     * @param amount   Amount to remove
     * @return CompletableFuture with success status
     */
    @NotNull
    public static CompletableFuture<Boolean> removeItemAsync(@NotNull Player player, @NotNull String material, int amount) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return StorageAPI.removeItem(player, material, amount);
            } catch (StorageException e) {
                throw new CompletionException(e);
            }
        });
    }

    /**
     * Asynchronously get item amount from player's storage
     *
     * @param player   The player
     * @param material Material name
     * @return CompletableFuture with item amount
     */
    @NotNull
    public static CompletableFuture<Integer> getItemAmountAsync(@NotNull Player player, @NotNull String material) {
        return CompletableFuture.supplyAsync(() -> StorageAPI.getItemAmount(player, material));
    }

    /**
     * Asynchronously get all stored materials for player
     *
     * @param player The player
     * @return CompletableFuture with map of material -> amount
     */
    @NotNull
    public static CompletableFuture<Map<String, Integer>> getStoredMaterialsAsync(@NotNull Player player) {
        return CompletableFuture.supplyAsync(() -> {
            StoragePlayer storagePlayer = StorageAPI.getStoragePlayer(player);
            return storagePlayer.getStoredMaterialsWithAmounts();
        });
    }

    /**
     * Asynchronously load player data from database
     *
     * @param player The player
     * @return CompletableFuture that completes when data is loaded
     */
    @NotNull
    public static CompletableFuture<Void> loadPlayerDataAsync(@NotNull Player player) {
        return CompletableFuture.runAsync(() -> MineManager.loadPlayerData(player));
    }

    /**
     * Asynchronously save player data to database
     *
     * @param player The player
     * @return CompletableFuture that completes when data is saved
     */
    @NotNull
    public static CompletableFuture<Void> savePlayerDataAsync(@NotNull Player player) {
        return CompletableFuture.runAsync(() -> MineManager.savePlayerData(player));
    }

    /**
     * Asynchronously transfer item between players
     *
     * @param sender   Sender player
     * @param receiver Receiver player
     * @param material Material to transfer
     * @param amount   Amount to transfer
     * @return CompletableFuture with success status
     */
    @NotNull
    public static CompletableFuture<Boolean> transferItemAsync(@NotNull Player sender, @NotNull Player receiver,
                                                               @NotNull String material, int amount) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return StorageAPI.transferItem(sender, receiver, material, amount);
            } catch (StorageException e) {
                throw new CompletionException(e);
            }
        });
    }

    /**
     * Asynchronously get top players by specific material
     *
     * @param material Material name
     * @param limit    Maximum number of players to return
     * @return CompletableFuture with map of player name -> amount
     */
    @NotNull
    public static CompletableFuture<Map<String, Integer>> getTopPlayersByMaterialAsync(@NotNull String material, int limit) {
        return CompletableFuture.supplyAsync(() -> {
            Map<String, Integer> topPlayers = new HashMap<>();

            for (Player player : Bukkit.getOnlinePlayers()) {
                int amount = StorageAPI.getItemAmount(player, material);
                if (amount > 0) {
                    topPlayers.put(player.getName(), amount);
                }
            }

            return topPlayers.entrySet().stream()
                    .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                    .limit(limit)
                    .collect(HashMap::new, (m, e) -> m.put(e.getKey(), e.getValue()), HashMap::putAll);
        });
    }

    /**
     * Asynchronously get server total for specific material
     *
     * @param material Material name
     * @return CompletableFuture with total amount across all online players
     */
    @NotNull
    public static CompletableFuture<Long> getServerTotalAsync(@NotNull String material) {
        return CompletableFuture.supplyAsync(() -> {
            long total = 0;
            for (Player player : Bukkit.getOnlinePlayers()) {
                total += StorageAPI.getItemAmount(player, material);
            }
            return total;
        });
    }

    /**
     * Asynchronously get server totals for all materials
     *
     * @return CompletableFuture with map of material -> total amount
     */
    @NotNull
    public static CompletableFuture<Map<String, Long>> getServerTotalsAsync() {
        return CompletableFuture.supplyAsync(() -> {
            Map<String, Long> totals = new HashMap<>();
            List<String> materials = StorageAPI.getStorableMaterials();

            for (String material : materials) {
                long total = 0;
                for (Player player : Bukkit.getOnlinePlayers()) {
                    total += StorageAPI.getItemAmount(player, material);
                }
                if (total > 0) {
                    totals.put(material, total);
                }
            }

            return totals;
        });
    }

    /**
     * Asynchronously batch save all online players
     *
     * @return CompletableFuture with number of players saved
     */
    @NotNull
    public static CompletableFuture<Integer> saveAllPlayersAsync() {
        return CompletableFuture.supplyAsync(() -> {
            int count = 0;
            for (Player player : Bukkit.getOnlinePlayers()) {
                try {
                    MineManager.savePlayerData(player);
                    count++;
                } catch (Exception e) {
                    // Log error but continue
                }
            }
            return count;
        });
    }

    /**
     * Asynchronously check if player has enough materials
     *
     * @param player    The player
     * @param materials Map of material -> required amount
     * @return CompletableFuture with true if player has all materials
     */
    @NotNull
    public static CompletableFuture<Boolean> hasAllMaterialsAsync(@NotNull Player player,
                                                                  @NotNull Map<String, Integer> materials) {
        return CompletableFuture.supplyAsync(() -> {
            for (Map.Entry<String, Integer> entry : materials.entrySet()) {
                int playerAmount = StorageAPI.getItemAmount(player, entry.getKey());
                if (playerAmount < entry.getValue()) {
                    return false;
                }
            }
            return true;
        });
    }

    /**
     * Execute async operation with callback on main thread
     *
     * @param operation Async operation to execute
     * @param callback  Callback to run on main thread with result
     * @param <T>       Result type
     */
    public static <T> void executeWithCallback(@NotNull CompletableFuture<T> operation,
                                               @NotNull AsyncCallback<T> callback) {
        operation.whenComplete((result, error) -> {
            Bukkit.getScheduler().runTask(StorageAPI.getPlugin(), () -> {
                if (error != null) {
                    callback.onError(error);
                } else {
                    callback.onSuccess(result);
                }
            });
        });
    }

    /**
     * Callback interface for async operations
     *
     * @param <T> Result type
     */
    public interface AsyncCallback<T> {
        /**
         * Called on main thread when operation succeeds
         *
         * @param result Operation result
         */
        void onSuccess(T result);

        /**
         * Called on main thread when operation fails
         *
         * @param error Error that occurred
         */
        void onError(Throwable error);
    }
}
