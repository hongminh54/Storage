package net.danh.storage.API;

import net.danh.storage.API.exceptions.StorageException;
import net.danh.storage.API.exceptions.StorageFullException;
import net.danh.storage.Manager.MythicStorageManager;
import net.danh.storage.MythicMobs.MythicMobsHelper;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * API for MythicMobs Storage system
 * Provides methods for external plugins to interact with MythicMobs item storage
 *
 * @author hongminh54
 * @version 2.3.3
 */
public class MythicStorageAPI {

    /**
     * Check if MythicStorage system is enabled and available
     *
     * @return true if system is enabled
     */
    public static boolean isSystemEnabled() {
        return MythicStorageManager.isSystemEnabled();
    }

    /**
     * Get MythicMobs item amount in player's storage
     *
     * @param player   The player
     * @param itemName MythicMobs item name
     * @return Amount stored
     */
    public static int getMythicItemAmount(@NotNull Player player, @NotNull String itemName) {
        if (!isSystemEnabled()) {
            return 0;
        }
        return MythicStorageManager.getPlayerItem(player, itemName);
    }

    /**
     * Add MythicMobs item to player's storage
     *
     * @param player   The player
     * @param itemName MythicMobs item name
     * @param amount   Amount to add
     * @return true if successfully added
     * @throws StorageException if operation fails
     */
    public static boolean addMythicItem(@NotNull Player player, @NotNull String itemName, int amount)
            throws StorageException {
        if (!isSystemEnabled()) {
            throw new IllegalStateException("MythicStorage system is not enabled");
        }
        if (amount <= 0) {
            throw new IllegalArgumentException("Amount must be positive");
        }
        if (!isValidMythicItem(itemName)) {
            throw new StorageException("Invalid MythicMobs item: " + itemName);
        }

        boolean result = MythicStorageManager.addItemAmount(player, itemName, amount, true);
        if (!result) {
            throw new StorageFullException("MythicStorage is full or invalid item");
        }
        return true;
    }

    /**
     * Add item without firing events (for internal use)
     */
    public static boolean addMythicItemSilent(@NotNull Player player, @NotNull String itemName, int amount) {
        if (!isSystemEnabled() || amount <= 0) return false;
        return MythicStorageManager.addItemAmount(player, itemName, amount, false);
    }

    /**
     * Remove MythicMobs item from player's storage
     *
     * @param player   The player
     * @param itemName MythicMobs item name
     * @param amount   Amount to remove
     * @return true if successfully removed
     * @throws StorageException if operation fails
     */
    public static boolean removeMythicItem(@NotNull Player player, @NotNull String itemName, int amount)
            throws StorageException {
        if (!isSystemEnabled()) {
            throw new IllegalStateException("MythicStorage system is not enabled");
        }
        if (amount <= 0) {
            throw new IllegalArgumentException("Amount must be positive");
        }

        return MythicStorageManager.removeItemAmount(player, itemName, amount, true);
    }

    /**
     * Remove item without firing events (for internal use)
     */
    public static boolean removeMythicItemSilent(@NotNull Player player, @NotNull String itemName, int amount) {
        if (!isSystemEnabled() || amount <= 0) return false;
        return MythicStorageManager.removeItemAmount(player, itemName, amount, false);
    }

    /**
     * Set exact amount of MythicMobs item in storage
     *
     * @param player   The player
     * @param itemName MythicMobs item name
     * @param amount   New amount
     */
    public static void setMythicItemAmount(@NotNull Player player, @NotNull String itemName, int amount) {
        if (!isSystemEnabled()) {
            return;
        }
        MythicStorageManager.setItemAmount(player, itemName, Math.max(0, amount));
    }

    /**
     * Check if player has MythicMobs item in storage
     *
     * @param player   The player
     * @param itemName MythicMobs item name
     * @return true if player has this item
     */
    public static boolean hasMythicItem(@NotNull Player player, @NotNull String itemName) {
        return getMythicItemAmount(player, itemName) > 0;
    }

    /**
     * Check if player has enough of specific MythicMobs item
     *
     * @param player   The player
     * @param itemName MythicMobs item name
     * @param amount   Required amount
     * @return true if player has enough
     */
    public static boolean hasEnoughMythicItem(@NotNull Player player, @NotNull String itemName, int amount) {
        return getMythicItemAmount(player, itemName) >= amount;
    }

    /**
     * Get all MythicMobs items stored by player
     *
     * @param player The player
     * @return Map of item name -> amount
     */
    @NotNull
    public static Map<String, Integer> getPlayerMythicItems(@NotNull Player player) {
        if (!isSystemEnabled()) {
            return new HashMap<>();
        }
        return MythicStorageManager.getPlayerAllItems(player);
    }

    /**
     * Get player's maximum MythicStorage capacity
     *
     * @param player The player
     * @return Maximum capacity
     */
    public static int getMaxMythicStorage(@NotNull Player player) {
        if (!isSystemEnabled()) {
            return 0;
        }
        return MythicStorageManager.getMaxStorage(player);
    }

    /**
     * Check if MythicMobs auto-pickup is enabled for player
     *
     * @param player The player
     * @return true if enabled
     */
    public static boolean isMythicAutoPickupEnabled(@NotNull Player player) {
        if (!isSystemEnabled()) {
            return false;
        }
        return MythicStorageManager.getToggleStatus(player);
    }

    /**
     * Set MythicMobs auto-pickup status for player
     *
     * @param player  The player
     * @param enabled New status
     */
    public static void setMythicAutoPickup(@NotNull Player player, boolean enabled) {
        if (!isSystemEnabled()) {
            return;
        }
        MythicStorageManager.setToggleStatus(player, enabled);
    }

    /**
     * Get all configured MythicMobs items that can be stored
     *
     * @return List of MythicMobs item names
     */
    @NotNull
    public static List<String> getConfiguredMythicItems() {
        if (!isSystemEnabled()) {
            return Collections.emptyList();
        }
        return MythicStorageManager.getConfiguredDrops();
    }

    /**
     * Check if MythicMobs item name is valid
     *
     * @param itemName MythicMobs item name
     * @return true if valid
     */
    public static boolean isValidMythicItem(@NotNull String itemName) {
        if (!isSystemEnabled()) {
            return false;
        }
        MythicMobsHelper helper = MythicStorageManager.getMythicMobsHelper();
        return helper != null && helper.isValidMythicItem(itemName);
    }

    /**
     * Check if MythicMobs item is configured to be stored
     *
     * @param itemName MythicMobs item name
     * @return true if configured
     */
    public static boolean isConfiguredDrop(@NotNull String itemName) {
        if (!isSystemEnabled()) {
            return false;
        }
        return MythicStorageManager.isConfiguredDrop(itemName);
    }

    /**
     * Get display name for MythicMobs item
     *
     * @param player   The player (for context)
     * @param itemName MythicMobs item name
     * @return Display name or item ID if not found
     */
    @NotNull
    public static String getMythicItemDisplayName(@NotNull Player player, @NotNull String itemName) {
        if (!isSystemEnabled()) {
            return itemName;
        }
        return MythicStorageManager.getItemDisplayNameOrId(itemName, player);
    }

    /**
     * Save player's MythicStorage data
     *
     * @param player The player
     */
    public static void saveMythicStorageData(@NotNull Player player) {
        if (!isSystemEnabled()) {
            return;
        }
        MythicStorageManager.savePlayerData(player);
    }

    /**
     * Load player's MythicStorage data
     *
     * @param player The player
     */
    public static void loadMythicStorageData(@NotNull Player player) {
        if (!isSystemEnabled()) {
            return;
        }
        MythicStorageManager.loadPlayerData(player);
    }
}
