package net.danh.storage.API;

import net.danh.storage.API.exceptions.StorageException;
import net.danh.storage.API.exceptions.StorageFullException;
import net.danh.storage.Manager.Crop.CropStorageManager;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * API for CropStorage system.
 * Provides methods for external plugins to interact with vanilla crop storage.
 *
 * @author hongminh54
 * @version 2.3.6
 */
public class CropStorageAPI {

    /**
     * Check if CropStorage system is enabled.
     */
    public static boolean isSystemEnabled() {
        return CropStorageManager.isSystemEnabled();
    }

    /**
     * Get crop item amount in player's CropStorage.
     */
    public static int getCropItemAmount(@NotNull Player player, @NotNull String itemName) {
        if (!isSystemEnabled()) {
            return 0;
        }
        return CropStorageManager.getPlayerItem(player, itemName);
    }

    /**
     * Add crop item to player's CropStorage.
     */
    public static boolean addCropItem(@NotNull Player player, @NotNull String itemName, int amount)
            throws StorageException {
        if (!isSystemEnabled()) {
            throw new IllegalStateException("CropStorage system is not enabled");
        }
        if (amount <= 0) {
            throw new IllegalArgumentException("Amount must be positive");
        }
        if (!CropStorageManager.isConfiguredDrop(itemName)) {
            throw new StorageException("Invalid crop item: " + itemName);
        }

        boolean result = CropStorageManager.addItemAmount(player, itemName, amount, true);
        if (!result) {
            throw new StorageFullException("CropStorage is full or invalid item");
        }
        return true;
    }

    /**
     * Add crop item without firing events (for internal use).
     */
    public static boolean addCropItemSilent(@NotNull Player player, @NotNull String itemName, int amount) {
        if (!isSystemEnabled() || amount <= 0) {
            return false;
        }
        return CropStorageManager.addItemAmount(player, itemName, amount, false);
    }

    /**
     * Remove crop item from player's CropStorage.
     */
    public static boolean removeCropItem(@NotNull Player player, @NotNull String itemName, int amount)
            throws StorageException {
        if (!isSystemEnabled()) {
            throw new IllegalStateException("CropStorage system is not enabled");
        }
        if (amount <= 0) {
            throw new IllegalArgumentException("Amount must be positive");
        }
        if (!CropStorageManager.isConfiguredDrop(itemName)) {
            throw new StorageException("Invalid crop item: " + itemName);
        }

        return CropStorageManager.removeItemAmount(player, itemName, amount, true);
    }

    /**
     * Remove crop item without firing events (for internal use).
     */
    public static boolean removeCropItemSilent(@NotNull Player player, @NotNull String itemName, int amount) {
        if (!isSystemEnabled() || amount <= 0) {
            return false;
        }
        return CropStorageManager.removeItemAmount(player, itemName, amount, false);
    }

    /**
     * Set exact amount of crop item in CropStorage.
     */
    public static void setCropItemAmount(@NotNull Player player, @NotNull String itemName, int amount) {
        if (!isSystemEnabled()) {
            return;
        }
        if (!CropStorageManager.isConfiguredDrop(itemName)) {
            return;
        }
        CropStorageManager.setItemAmount(player, itemName, Math.max(0, amount));
    }

    /**
     * Get player's maximum CropStorage capacity.
     */
    public static int getMaxCropStorage(@NotNull Player player) {
        if (!isSystemEnabled()) {
            return 0;
        }
        return CropStorageManager.getMaxStorage(player);
    }

    /**
     * Check if CropStorage auto-pickup is enabled for player.
     */
    public static boolean isCropAutoPickupEnabled(@NotNull Player player) {
        if (!isSystemEnabled()) {
            return false;
        }
        return CropStorageManager.getToggleStatus(player);
    }

    /**
     * Check if auto-pickup is enabled for a specific crop item.
     */
    public static boolean isCropAutoPickupEnabledForItem(@NotNull Player player, @NotNull String itemName) {
        if (!isSystemEnabled()) {
            return false;
        }
        return CropStorageManager.isAutoPickupEnabledForItem(player, itemName);
    }

    /**
     * Toggle auto-pickup for a specific crop item.
     * Fires CropStorageItemToggleEvent.
     */
    public static boolean toggleCropAutoPickupForItem(@NotNull Player player, @NotNull String itemName) {
        if (!isSystemEnabled()) {
            return false;
        }
        return CropStorageManager.toggleItemAutoPickup(player, itemName, true);
    }

    /**
     * Set CropStorage auto-pickup status for player.
     */
    public static void setCropAutoPickup(@NotNull Player player, boolean enabled) {
        if (!isSystemEnabled()) {
            return;
        }
        CropStorageManager.setToggleStatus(player, enabled, true);
    }

    /**
     * Get all configured crop items that can be stored.
     */
    @NotNull
    public static List<String> getConfiguredCropItems() {
        if (!isSystemEnabled()) {
            return java.util.Collections.emptyList();
        }
        return CropStorageManager.getConfiguredDrops();
    }

    /**
     * Get all crop items stored by player.
     */
    @NotNull
    public static Map<String, Integer> getPlayerCropItems(@NotNull Player player) {
        if (!isSystemEnabled()) {
            return new HashMap<>();
        }
        return CropStorageManager.getPlayerAllItems(player);
    }
}
