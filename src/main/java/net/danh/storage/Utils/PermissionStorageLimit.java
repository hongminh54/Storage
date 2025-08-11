package net.danh.storage.Utils;

import org.bukkit.entity.Player;
import org.bukkit.configuration.ConfigurationSection;

import java.util.Set;

/**
 * Utility class for handling permission-based storage limits
 */
public class PermissionStorageLimit {

    /**
     * Get the maximum storage limit for a player based on their permissions
     * @param player The player to check
     * @return The maximum storage limit for the player
     */
    public static int getPlayerStorageLimit(Player player) {
        ConfigurationSection storagePermissions = File.getConfig().getConfigurationSection("storage_permissions");
        
        // If no storage permissions are configured, use default
        if (storagePermissions == null) {
            return File.getConfig().getInt("settings.default_max_storage", 100000);
        }
        
        int highestLimit = 0;
        boolean hasAnyStoragePermission = false;
        
        // Check all configured storage permissions
        Set<String> permissionKeys = storagePermissions.getKeys(false);
        for (String permissionKey : permissionKeys) {
            String permission = "storage.storage." + permissionKey;
            
            if (player.hasPermission(permission)) {
                hasAnyStoragePermission = true;
                int limit = storagePermissions.getInt(permissionKey + ".max_storage", 0);
                if (limit > highestLimit) {
                    highestLimit = limit;
                }
            }
        }
        
        // If player has any storage permission, use the highest limit found
        if (hasAnyStoragePermission) {
            return highestLimit;
        }
        
        // If no storage permissions found, use default
        return File.getConfig().getInt("settings.default_max_storage", 100000);
    }
    
    /**
     * Check if storage permissions are configured
     * @return true if storage permissions are configured, false otherwise
     */
    public static boolean hasStoragePermissionsConfigured() {
        ConfigurationSection storagePermissions = File.getConfig().getConfigurationSection("storage_permissions");
        return storagePermissions != null && !storagePermissions.getKeys(false).isEmpty();
    }
}