package net.danh.storage.Utils;

import net.danh.storage.API.events.StoragePermissionRefreshEvent;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.util.Set;

/**
 * Utility class for handling permission-based storage limits
 */
public class PermissionStorageLimit {

    /**
     * Get the maximum storage limit for a player based on their permissions
     * Uses priority system - higher priority overrides lower priority
     *
     * @param player The player to check
     * @return The maximum storage limit for the player
     */
    public static int getPlayerStorageLimit(Player player) {
        ConfigurationSection storagePermissions = File.getConfig().getConfigurationSection("storage_permissions");

        // If no storage permissions are configured, use default
        if (storagePermissions == null) {
            return File.getConfig().getInt("settings.default_max_storage", 100000);
        }

        int selectedLimit = 0;
        int highestPriority = -1;
        boolean hasAnyStoragePermission = false;

        // Check all configured storage permissions
        Set<String> permissionKeys = storagePermissions.getKeys(false);
        for (String permissionKey : permissionKeys) {
            String permission = "storage.storage." + permissionKey;

            if (player.hasPermission(permission)) {
                hasAnyStoragePermission = true;
                int limit = storagePermissions.getInt(permissionKey + ".max_storage", 0);
                int priority = storagePermissions.getInt(permissionKey + ".priority", 0);

                // Use priority system - higher priority wins
                if (priority > highestPriority) {
                    highestPriority = priority;
                    selectedLimit = limit;
                }
            }
        }

        // If player has any storage permission, use the selected limit
        if (hasAnyStoragePermission) {
            return selectedLimit;
        }

        // If no storage permissions found, use default
        return File.getConfig().getInt("settings.default_max_storage", 100000);
    }

    /**
     * Refresh storage limit for a player (useful when permissions change)
     *
     * @param player The player to refresh
     */
    public static void refreshPlayerStorageLimit(Player player) {
        if (player == null || !player.isOnline()) {
            return;
        }

        int oldLimit = net.danh.storage.Manager.MineManager.playermaxdata.getOrDefault(player, 0);
        int newLimit = getPlayerStorageLimit(player);

        // Fire event to allow other plugins to modify or cancel the refresh
        StoragePermissionRefreshEvent event = new StoragePermissionRefreshEvent(player, oldLimit, newLimit);
        Bukkit.getPluginManager().callEvent(event);

        if (event.isCancelled()) {
            return;
        }

        // Use the potentially modified limit from the event
        net.danh.storage.Manager.MineManager.playermaxdata.put(player, event.getNewLimit());
    }

    /**
     * Check if storage permissions are configured
     *
     * @return true if storage permissions are configured, false otherwise
     */
    public static boolean hasStoragePermissionsConfigured() {
        ConfigurationSection storagePermissions = File.getConfig().getConfigurationSection("storage_permissions");
        return storagePermissions != null && !storagePermissions.getKeys(false).isEmpty();
    }
}