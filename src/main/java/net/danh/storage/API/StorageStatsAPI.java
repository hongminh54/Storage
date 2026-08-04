package net.danh.storage.API;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.stream.Collectors;

/**
 * API for storage statistics and analytics
 * Provides methods for tracking and analyzing storage data
 *
 * @author hongminh54
 * @version 2.3.3
 */
public class StorageStatsAPI {

    /**
     * Get total items stored by player across all materials
     *
     * @param player The player
     * @return Total items count
     */
    public static long getTotalItemsStored(@NotNull Player player) {
        StoragePlayer storagePlayer = StorageAPI.getStoragePlayer(player);
        return storagePlayer.getTotalStoredItems();
    }

    /**
     * Get player's storage usage percentage
     *
     * @param player The player
     * @return Usage percentage (0.0 to 1.0)
     */
    public static double getStorageUsagePercentage(@NotNull Player player) {
        StoragePlayer storagePlayer = StorageAPI.getStoragePlayer(player);
        return storagePlayer.getStorageUsagePercentage();
    }

    /**
     * Get player's most stored material
     *
     * @param player The player
     * @return Material name with highest amount, or null if storage is empty
     */
    @NotNull
    public static Optional<String> getMostStoredMaterial(@NotNull Player player) {
        StoragePlayer storagePlayer = StorageAPI.getStoragePlayer(player);
        Map<String, Integer> materials = storagePlayer.getStoredMaterialsWithAmounts();

        return materials.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey);
    }

    /**
     * Get player's top N stored materials
     *
     * @param player The player
     * @param limit  Maximum number of materials to return
     * @return Map of material -> amount, sorted by amount descending
     */
    @NotNull
    public static Map<String, Integer> getTopStoredMaterials(@NotNull Player player, int limit) {
        StoragePlayer storagePlayer = StorageAPI.getStoragePlayer(player);
        Map<String, Integer> materials = storagePlayer.getStoredMaterialsWithAmounts();

        return materials.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(limit)
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (e1, e2) -> e1,
                        LinkedHashMap::new
                ));
    }

    /**
     * Get number of unique materials stored by player
     *
     * @param player The player
     * @return Count of unique materials
     */
    public static int getUniqueMaterialsCount(@NotNull Player player) {
        StoragePlayer storagePlayer = StorageAPI.getStoragePlayer(player);
        return storagePlayer.getStoredMaterials().size();
    }

    /**
     * Get server-wide total for specific material
     *
     * @param material Material name
     * @return Total amount across all online players
     */
    public static long getServerTotal(@NotNull String material) {
        long total = 0;
        for (Player player : Bukkit.getOnlinePlayers()) {
            total += StorageAPI.getItemAmount(player, material);
        }
        return total;
    }

    /**
     * Get server-wide totals for all materials
     *
     * @return Map of material -> total amount
     */
    @NotNull
    public static Map<String, Long> getServerTotals() {
        Map<String, Long> totals = new HashMap<>();
        List<String> materials = StorageAPI.getStorableMaterials();

        for (String material : materials) {
            long total = getServerTotal(material);
            if (total > 0) {
                totals.put(material, total);
            }
        }

        return totals;
    }

    /**
     * Get top players by specific material
     *
     * @param material Material name
     * @param limit    Maximum number of players to return
     * @return Map of player name -> amount, sorted by amount descending
     */
    @NotNull
    public static Map<String, Integer> getTopPlayersByMaterial(@NotNull String material, int limit) {
        Map<String, Integer> playerAmounts = new HashMap<>();

        for (Player player : Bukkit.getOnlinePlayers()) {
            int amount = StorageAPI.getItemAmount(player, material);
            if (amount > 0) {
                playerAmounts.put(player.getName(), amount);
            }
        }

        return playerAmounts.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(limit)
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (e1, e2) -> e1,
                        LinkedHashMap::new
                ));
    }

    /**
     * Get top players by total storage usage
     *
     * @param limit Maximum number of players to return
     * @return Map of player name -> total items, sorted descending
     */
    @NotNull
    public static Map<String, Long> getTopPlayersByTotal(int limit) {
        Map<String, Long> playerTotals = new HashMap<>();

        for (Player player : Bukkit.getOnlinePlayers()) {
            long total = getTotalItemsStored(player);
            if (total > 0) {
                playerTotals.put(player.getName(), total);
            }
        }

        return playerTotals.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(limit)
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (e1, e2) -> e1,
                        LinkedHashMap::new
                ));
    }

    /**
     * Get material distribution across all online players
     *
     * @param material Material name
     * @return Map of player name -> amount
     */
    @NotNull
    public static Map<String, Integer> getMaterialDistribution(@NotNull String material) {
        Map<String, Integer> distribution = new HashMap<>();

        for (Player player : Bukkit.getOnlinePlayers()) {
            int amount = StorageAPI.getItemAmount(player, material);
            if (amount > 0) {
                distribution.put(player.getName(), amount);
            }
        }

        return distribution;
    }

    /**
     * Get average storage usage across all online players
     *
     * @return Average usage percentage (0.0 to 1.0)
     */
    public static double getAverageStorageUsage() {
        Collection<? extends Player> players = Bukkit.getOnlinePlayers();
        if (players.isEmpty()) {
            return 0.0;
        }

        double totalUsage = 0.0;
        for (Player player : players) {
            totalUsage += getStorageUsagePercentage(player);
        }

        return totalUsage / players.size();
    }

    /**
     * Get player ranking by specific material
     *
     * @param player   The player
     * @param material Material name
     * @return Player's rank (1-based), or 0 if player has none
     */
    public static int getPlayerRank(@NotNull Player player, @NotNull String material) {
        int playerAmount = StorageAPI.getItemAmount(player, material);
        if (playerAmount == 0) {
            return 0;
        }

        List<Integer> amounts = new ArrayList<>();
        for (Player p : Bukkit.getOnlinePlayers()) {
            int amount = StorageAPI.getItemAmount(p, material);
            if (amount > 0) {
                amounts.add(amount);
            }
        }

        amounts.sort(Collections.reverseOrder());
        return amounts.indexOf(playerAmount) + 1;
    }

    /**
     * Get player ranking by total storage
     *
     * @param player The player
     * @return Player's rank (1-based), or 0 if storage is empty
     */
    public static int getPlayerTotalRank(@NotNull Player player) {
        long playerTotal = getTotalItemsStored(player);
        if (playerTotal == 0) {
            return 0;
        }

        List<Long> totals = new ArrayList<>();
        for (Player p : Bukkit.getOnlinePlayers()) {
            long total = getTotalItemsStored(p);
            if (total > 0) {
                totals.add(total);
            }
        }

        totals.sort(Collections.reverseOrder());
        return totals.indexOf(playerTotal) + 1;
    }

    /**
     * Get storage statistics summary for player
     *
     * @param player The player
     * @return StorageStats object with comprehensive data
     */
    @NotNull
    public static StorageStats getPlayerStats(@NotNull Player player) {
        StoragePlayer storagePlayer = StorageAPI.getStoragePlayer(player);

        return new StorageStats(
                player.getName(),
                storagePlayer.getTotalStoredItems(),
                storagePlayer.getMaxStorage(),
                storagePlayer.getStorageUsagePercentage(),
                storagePlayer.getStoredMaterials().size(),
                getMostStoredMaterial(player).orElse("None"),
                getPlayerTotalRank(player)
        );
    }

    /**
     * Get server-wide storage statistics
     *
     * @return ServerStats object with comprehensive data
     */
    @NotNull
    public static ServerStats getServerStats() {
        long totalItems = 0;
        long totalCapacity = 0;
        int totalPlayers = 0;
        Set<String> uniqueMaterials = new HashSet<>();

        for (Player player : Bukkit.getOnlinePlayers()) {
            StoragePlayer storagePlayer = StorageAPI.getStoragePlayer(player);
            totalItems += storagePlayer.getTotalStoredItems();
            totalCapacity += storagePlayer.getMaxStorage();
            totalPlayers++;
            uniqueMaterials.addAll(storagePlayer.getStoredMaterials());
        }

        double averageUsage = totalPlayers > 0 ? (double) totalItems / totalCapacity : 0.0;

        return new ServerStats(
                totalItems,
                totalCapacity,
                averageUsage,
                totalPlayers,
                uniqueMaterials.size()
        );
    }

    /**
     * Storage statistics data class
     */
    public record StorageStats(String playerName, long totalItems, int maxCapacity, double usagePercentage,
                               int uniqueMaterials, String topMaterial, int rank) {

        @Override
        public String toString() {
            return String.format("StorageStats{player=%s, items=%d/%d (%.1f%%), materials=%d, top=%s, rank=%d}",
                    playerName, totalItems, maxCapacity, usagePercentage * 100, uniqueMaterials, topMaterial, rank);
        }
    }

    /**
     * Server statistics data class
     */
    public record ServerStats(long totalItems, long totalCapacity, double averageUsage, int totalPlayers,
                              int uniqueMaterials) {

        @Override
        public String toString() {
            return String.format("ServerStats{items=%d/%d (%.1f%%), players=%d, materials=%d}",
                    totalItems, totalCapacity, averageUsage * 100, totalPlayers, uniqueMaterials);
        }
    }
}
