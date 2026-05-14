package net.danh.storage.API;

import net.danh.storage.API.exceptions.StorageException;
import net.danh.storage.Manager.MineManager;
import net.danh.storage.Utils.SchedulerUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

/**
 * API for asynchronous storage operations
 * Provides non-blocking methods to prevent server lag
 *
 * @author hongminh54
 * @version 2.3.3
 */
public class StorageAsyncAPI {

    @NotNull
    private static <T> CompletableFuture<T> supplyOnMainThread(
            @NotNull Supplier<T> supplier
    ) {
        CompletableFuture<T> future = new CompletableFuture<>();
        SchedulerUtil.runTask(StorageAPI.getPlugin(), () -> {
            try {
                future.complete(supplier.get());
            } catch (Throwable t) {
                future.completeExceptionally(t);
            }
        });
        return future;
    }

    /**
     * Asynchronously add item to player's storage
     * Note: The actual storage operation runs on main thread to properly fire events
     *
     * @param player   The player
     * @param material Material name
     * @param amount   Amount to add
     * @return CompletableFuture with success status
     */
    @NotNull
    public static CompletableFuture<Boolean> addItemAsync(@NotNull Player player, @NotNull String material, int amount) {
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        SchedulerUtil.runTask(StorageAPI.getPlugin(), player, () -> {
            try {
                boolean result = StorageAPI.addItem(player, material, amount);
                future.complete(result);
            } catch (StorageException e) {
                future.completeExceptionally(e);
            }
        });
        return future;
    }

    /**
     * Asynchronously remove item from player's storage
     * Note: The actual storage operation runs on main thread to properly fire events
     *
     * @param player   The player
     * @param material Material name
     * @param amount   Amount to remove
     * @return CompletableFuture with success status
     */
    @NotNull
    public static CompletableFuture<Boolean> removeItemAsync(@NotNull Player player, @NotNull String material, int amount) {
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        SchedulerUtil.runTask(StorageAPI.getPlugin(), player, () -> {
            try {
                boolean result = StorageAPI.removeItem(player, material, amount);
                future.complete(result);
            } catch (StorageException e) {
                future.completeExceptionally(e);
            }
        });
        return future;
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
        return supplyOnMainThread(() -> StorageAPI.getItemAmount(player, material));
    }

    /**
     * Asynchronously get all stored materials for player
     *
     * @param player The player
     * @return CompletableFuture with map of material -> amount
     */
    @NotNull
    public static CompletableFuture<Map<String, Integer>> getStoredMaterialsAsync(@NotNull Player player) {
        return supplyOnMainThread(() -> {
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
        CompletableFuture<Void> future = new CompletableFuture<>();
        SchedulerUtil.runTask(StorageAPI.getPlugin(), () -> {
            try {
                MineManager.loadPlayerData(player);
                future.complete(null);
            } catch (Exception e) {
                future.completeExceptionally(e);
            }
        });
        return future;
    }

    /**
     * Asynchronously save player data to database
     *
     * @param player The player
     * @return CompletableFuture that completes when data is saved
     */
    @NotNull
    public static CompletableFuture<Void> savePlayerDataAsync(@NotNull Player player) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        SchedulerUtil.runTask(StorageAPI.getPlugin(), () -> {
            try {
                MineManager.savePlayerData(player);
                future.complete(null);
            } catch (Exception e) {
                future.completeExceptionally(e);
            }
        });
        return future;
    }

    /**
     * Asynchronously transfer item between players
     * Note: The actual transfer operation runs on main thread to properly fire events
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
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        SchedulerUtil.runTask(StorageAPI.getPlugin(), sender, () -> {
            try {
                boolean result = StorageAPI.transferItem(sender, receiver, material, amount);
                future.complete(result);
            } catch (StorageException e) {
                future.completeExceptionally(e);
            }
        });
        return future;
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
        return supplyOnMainThread(() -> {
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
        return supplyOnMainThread(() -> {
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
        return supplyOnMainThread(() -> {
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
        CompletableFuture<Integer> future = new CompletableFuture<>();
        SchedulerUtil.runTask(StorageAPI.getPlugin(), () -> {
            int count = 0;
            for (Player online : Bukkit.getOnlinePlayers()) {
                try {
                    MineManager.savePlayerData(online);
                    count++;
                } catch (Exception e) {
                    // Continue saving other players
                }
            }
            future.complete(count);
        });
        return future;
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
        return supplyOnMainThread(() -> {
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
            SchedulerUtil.runTask(StorageAPI.getPlugin(), () -> {
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
