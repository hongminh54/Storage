package net.danh.storage.API.interfaces;

import net.danh.storage.API.exceptions.StorageException;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;

/**
 * Interface for managing MythicMobs item storage
 * Provides methods for MythicMobs item operations and queries
 *
 * @author VoChiDanh, hongminh54
 * @version 2.3.4
 */
public interface IMythicStorageManager {

    /**
     * Check if MythicStorage system is enabled
     *
     * @return true if system is enabled and MythicMobs is available
     */
    boolean isSystemEnabled();

    /**
     * Add MythicMobs item to player's storage
     *
     * @param player   The player
     * @param itemName MythicMobs item name
     * @param amount   Amount to add
     * @return true if successfully added
     * @throws StorageException if operation fails
     */
    boolean addItem(@NotNull Player player, @NotNull String itemName, int amount) throws StorageException;

    /**
     * Remove MythicMobs item from player's storage
     *
     * @param player   The player
     * @param itemName MythicMobs item name
     * @param amount   Amount to remove
     * @return true if successfully removed
     * @throws StorageException if operation fails
     */
    boolean removeItem(@NotNull Player player, @NotNull String itemName, int amount) throws StorageException;

    /**
     * Set exact amount of MythicMobs item in storage
     *
     * @param player   The player
     * @param itemName MythicMobs item name
     * @param amount   New amount
     */
    void setItem(@NotNull Player player, @NotNull String itemName, int amount);

    /**
     * Get MythicMobs item amount in player's storage
     *
     * @param player   The player
     * @param itemName MythicMobs item name
     * @return Amount stored
     */
    int getItemAmount(@NotNull Player player, @NotNull String itemName);

    /**
     * Check if player has MythicMobs item in storage
     *
     * @param player   The player
     * @param itemName MythicMobs item name
     * @return true if player has this item
     */
    boolean hasItem(@NotNull Player player, @NotNull String itemName);

    /**
     * Check if player has enough of specific MythicMobs item
     *
     * @param player   The player
     * @param itemName MythicMobs item name
     * @param amount   Required amount
     * @return true if player has enough
     */
    boolean hasEnoughItem(@NotNull Player player, @NotNull String itemName, int amount);

    /**
     * Get all MythicMobs items stored by player
     *
     * @param player The player
     * @return Map of item name -> amount
     */
    @NotNull
    Map<String, Integer> getAllItems(@NotNull Player player);

    /**
     * Get total items count in player's MythicStorage
     *
     * @param player The player
     * @return Total items count
     */
    int getTotalItems(@NotNull Player player);

    /**
     * Get player's maximum MythicStorage capacity
     *
     * @param player The player
     * @return Maximum capacity
     */
    int getMaxStorage(@NotNull Player player);

    /**
     * Set player's maximum MythicStorage capacity
     *
     * @param player The player
     * @param amount New maximum capacity
     */
    void setMaxStorage(@NotNull Player player, int amount);

    /**
     * Get remaining space in player's MythicStorage
     *
     * @param player The player
     * @return Remaining space
     */
    int getRemainingSpace(@NotNull Player player);

    /**
     * Check if player's MythicStorage is full
     *
     * @param player The player
     * @return true if storage is full
     */
    boolean isStorageFull(@NotNull Player player);

    /**
     * Check if MythicMobs auto-pickup is enabled for player
     *
     * @param player The player
     * @return true if enabled
     */
    boolean isAutoPickupEnabled(@NotNull Player player);

    /**
     * Set MythicMobs auto-pickup status for player
     *
     * @param player  The player
     * @param enabled New status
     */
    void setAutoPickup(@NotNull Player player, boolean enabled);

    /**
     * Clear all MythicMobs items from player's storage
     *
     * @param player The player
     */
    void clearStorage(@NotNull Player player);

    /**
     * Clear specific MythicMobs item from player's storage
     *
     * @param player   The player
     * @param itemName MythicMobs item name
     */
    void clearItem(@NotNull Player player, @NotNull String itemName);

    /**
     * Get all configured MythicMobs items that can be stored
     *
     * @return List of MythicMobs item names
     */
    @NotNull
    List<String> getConfiguredItems();

    /**
     * Check if MythicMobs item name is valid
     *
     * @param itemName MythicMobs item name
     * @return true if valid
     */
    boolean isValidItem(@NotNull String itemName);

    /**
     * Check if MythicMobs item is configured to be stored
     *
     * @param itemName MythicMobs item name
     * @return true if configured
     */
    boolean isConfiguredItem(@NotNull String itemName);

    /**
     * Get display name for MythicMobs item
     *
     * @param player   The player (for context)
     * @param itemName MythicMobs item name
     * @return Display name or item ID if not found
     */
    @NotNull
    String getItemDisplayName(@NotNull Player player, @NotNull String itemName);

    /**
     * Get list of invalid/misconfigured items
     *
     * @return List of invalid item names
     */
    @NotNull
    List<String> getInvalidItems();

    /**
     * Check if there are invalid items in configuration
     *
     * @return true if there are invalid items
     */
    boolean hasInvalidItems();

    /**
     * Save player's MythicStorage data
     *
     * @param player The player
     */
    void savePlayerData(@NotNull Player player);

    /**
     * Load player's MythicStorage data
     *
     * @param player The player
     */
    void loadPlayerData(@NotNull Player player);

    /**
     * Reload MythicStorage configuration
     */
    void reload();
}
