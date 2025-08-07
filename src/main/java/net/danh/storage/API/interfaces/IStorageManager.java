package net.danh.storage.API.interfaces;

import net.danh.storage.API.StoragePlayer;
import net.danh.storage.API.exceptions.StorageException;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Interface for managing player storage operations
 * Provides methods for storage manipulation and queries
 *
 * @author VoChiDanh, hongminh54
 * @version 2.3.2
 */
public interface IStorageManager {

    /**
     * Get StoragePlayer wrapper for a player
     *
     * @param player The player
     * @return StoragePlayer instance
     */
    @NotNull
    StoragePlayer getStoragePlayer(@NotNull Player player);

    /**
     * Add item to player's storage
     *
     * @param player   The player
     * @param material Material name
     * @param amount   Amount to add
     * @return true if successfully added
     * @throws StorageException if operation fails
     */
    boolean addItem(@NotNull Player player, @NotNull String material, int amount) throws StorageException;

    /**
     * Remove item from player's storage
     *
     * @param player   The player
     * @param material Material name
     * @param amount   Amount to remove
     * @return true if successfully removed
     * @throws StorageException if operation fails
     */
    boolean removeItem(@NotNull Player player, @NotNull String material, int amount) throws StorageException;

    /**
     * Set exact amount of material in storage
     *
     * @param player   The player
     * @param material Material name
     * @param amount   New amount
     */
    void setItem(@NotNull Player player, @NotNull String material, int amount);

    /**
     * Get amount of specific material in storage
     *
     * @param player   The player
     * @param material Material name
     * @return Amount stored
     */
    int getItemAmount(@NotNull Player player, @NotNull String material);

    /**
     * Check if player has specific material
     *
     * @param player   The player
     * @param material Material name
     * @return true if player has this material
     */
    boolean hasItem(@NotNull Player player, @NotNull String material);

    /**
     * Check if player has enough of specific material
     *
     * @param player   The player
     * @param material Material name
     * @param amount   Required amount
     * @return true if player has enough
     */
    boolean hasEnoughItem(@NotNull Player player, @NotNull String material, int amount);

    /**
     * Get all materials stored by player
     *
     * @param player The player
     * @return Set of material names
     */
    @NotNull
    Set<String> getStoredMaterials(@NotNull Player player);

    /**
     * Get all stored materials with amounts
     *
     * @param player The player
     * @return Map of material -> amount
     */
    @NotNull
    Map<String, Integer> getStoredMaterialsWithAmounts(@NotNull Player player);

    /**
     * Get total number of items stored
     *
     * @param player The player
     * @return Total items count
     */
    int getTotalStoredItems(@NotNull Player player);

    /**
     * Get maximum storage capacity
     *
     * @param player The player
     * @return Maximum capacity
     */
    int getMaxStorage(@NotNull Player player);

    /**
     * Set maximum storage capacity
     *
     * @param player The player
     * @param amount New maximum capacity
     */
    void setMaxStorage(@NotNull Player player, int amount);

    /**
     * Get remaining storage space
     *
     * @param player The player
     * @return Available space
     */
    int getRemainingSpace(@NotNull Player player);

    /**
     * Check if storage is full
     *
     * @param player The player
     * @return true if storage is full
     */
    boolean isStorageFull(@NotNull Player player);

    /**
     * Check if storage is empty
     *
     * @param player The player
     * @return true if storage is empty
     */
    boolean isStorageEmpty(@NotNull Player player);

    /**
     * Get storage usage percentage
     *
     * @param player The player
     * @return Usage percentage (0.0 to 1.0)
     */
    double getStorageUsagePercentage(@NotNull Player player);

    /**
     * Check if auto-pickup is enabled
     *
     * @param player The player
     * @return true if enabled
     */
    boolean isStorageEnabled(@NotNull Player player);

    /**
     * Toggle auto-pickup on/off
     *
     * @param player  The player
     * @param enabled New state
     */
    void setStorageEnabled(@NotNull Player player, boolean enabled);

    /**
     * Clear all items from storage
     *
     * @param player The player
     */
    void clearStorage(@NotNull Player player);

    /**
     * Clear specific material from storage
     *
     * @param player   The player
     * @param material Material to clear
     */
    void clearMaterial(@NotNull Player player, @NotNull String material);

    /**
     * Get all storable materials
     *
     * @return List of storable materials
     */
    @NotNull
    List<String> getStorableMaterials();

    /**
     * Check if material can be stored
     *
     * @param material Material name
     * @return true if storable
     */
    boolean isStorableMaterial(@NotNull String material);

    /**
     * Save player data to database
     *
     * @param player The player
     */
    void savePlayerData(@NotNull Player player);

    /**
     * Load player data from database
     *
     * @param player The player
     */
    void loadPlayerData(@NotNull Player player);
}
