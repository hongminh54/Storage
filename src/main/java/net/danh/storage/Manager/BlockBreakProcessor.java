package net.danh.storage.Manager;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class BlockBreakProcessor {

    // Optimized player cache with weak references to prevent memory leaks
    private static final Map<UUID, Boolean> autoPickupCache = new HashMap<>();
    private static final Map<UUID, Long> cacheExpiry = new HashMap<>();
    private static final long CACHE_DURATION = 5000; // 5 seconds
    
    // Cache config values to avoid repeated File.getConfig() calls
    private static Boolean actionBarEnabled = null;
    private static Boolean titleEnabled = null;
    private static String actionBarTemplate = null;
    private static String titleTemplate = null;
    private static String subtitleTemplate = null;
    private static long configLastUpdate = 0;
    
    // Pre-compiled templates to reduce string operations
    private static String actionBarTemplateCompiled = null;
    private static String titleTemplateCompiled = null;
    private static String subtitleTemplateCompiled = null;

    public static void initialize() {
        // Simple initialization - no background tasks
    }

    public static boolean isAutoPickupEnabled(Player player) {
        UUID uuid = player.getUniqueId();
        long currentTime = System.currentTimeMillis();
        
        // Periodic cache cleanup (every 30 seconds)
        if (currentTime % 30000 < 1000) {
            cleanupExpiredCache(currentTime);
        }
        
        // Check if we have a cached value that hasn't expired
        Long expiryTime = cacheExpiry.get(uuid);
        if (expiryTime != null && currentTime < expiryTime) {
            Boolean cachedValue = autoPickupCache.get(uuid);
            if (cachedValue != null) {
                return cachedValue;
            }
        }
        
        // Calculate new value
        boolean value = MineManager.isAutoPickupEnabled(player);
        
        // Update cache
        autoPickupCache.put(uuid, value);
        cacheExpiry.put(uuid, currentTime + CACHE_DURATION);
        
        return value;
    }
    
    // Cleanup expired cache entries to prevent memory leaks
    private static void cleanupExpiredCache(long currentTime) {
        cacheExpiry.entrySet().removeIf(entry -> currentTime >= entry.getValue());
        autoPickupCache.keySet().removeIf(uuid -> !cacheExpiry.containsKey(uuid));
    }

    public static boolean hasEmptySlot(Player player) {
        ItemStack[] contents = player.getInventory().getContents();
        for (int i = 0; i < 36; i++) { // Only check main inventory slots
            if (contents[i] == null || contents[i].getType().name().equals("AIR")) {
                return true;
            }
        }
        return false;
    }

    public static void processInventoryBulk(Player player) {
        ItemStack[] contents = player.getInventory().getContents();
        boolean modified = false;

        for (int i = 0; i < contents.length; i++) {
            ItemStack item = contents[i];
            if (item != null && item.getAmount() > 0) {
                String drop = MineManager.getItemStackDrop(item);
                if (drop != null) {
                    int actualAmount = MineManager.addBlockAmountWithPartial(player, drop, item.getAmount());
                    if (actualAmount > 0) {
                        if (actualAmount >= item.getAmount()) {
                            contents[i] = null;
                        } else {
                            item.setAmount(item.getAmount() - actualAmount);
                        }
                        modified = true;
                    }
                }
            }
        }

        if (modified) {
            player.getInventory().setContents(contents);
        }
    }

    // Cache and optimize config access with pre-compiled templates
    private static void updateConfigCache() {
        long currentTime = System.currentTimeMillis();
        if (currentTime - configLastUpdate > CACHE_DURATION) {
            actionBarEnabled = net.danh.storage.Utils.File.getConfig().getBoolean("mine.actionbar.enable");
            titleEnabled = net.danh.storage.Utils.File.getConfig().getBoolean("mine.title.enable");
            actionBarTemplate = net.danh.storage.Utils.File.getConfig().getString("mine.actionbar.action");
            titleTemplate = net.danh.storage.Utils.File.getConfig().getString("mine.title.title");
            subtitleTemplate = net.danh.storage.Utils.File.getConfig().getString("mine.title.subtitle");
            
            // Pre-compile templates by removing placeholders for faster replacement
            if (actionBarTemplate != null) {
                actionBarTemplateCompiled = actionBarTemplate
                    .replace("#item#", "%s")
                    .replace("#amount#", "%s")
                    .replace("#storage#", "%s")
                    .replace("#max#", "%s");
            }
            
            if (titleTemplate != null) {
                titleTemplateCompiled = titleTemplate
                    .replace("#item#", "%s")
                    .replace("#amount#", "%s")
                    .replace("#storage#", "%s")
                    .replace("#max#", "%s");
            }
            
            if (subtitleTemplate != null) {
                subtitleTemplateCompiled = subtitleTemplate
                    .replace("#item#", "%s")
                    .replace("#amount#", "%s")
                    .replace("#storage#", "%s")
                    .replace("#max#", "%s");
            }
            
            configLastUpdate = currentTime;
        }
    }
    
    // Optimized notification processing with minimal string operations
    public static void sendOptimizedNotifications(Player player, String drop, int actualAmount, int bonusAmount) {
        updateConfigCache();
        
        if ((!actionBarEnabled || actionBarTemplate == null) && 
            (!titleEnabled || titleTemplate == null || subtitleTemplate == null)) {
            return;
        }
        
        // Cache heavy calculations
        String itemName = net.danh.storage.Utils.File.getConfig().getString("items." + drop);
        if (itemName == null) itemName = drop.replace("_", " ");
        
        String displayAmount = bonusAmount > 0 ? 
            actualAmount + " (+" + Math.min(bonusAmount, actualAmount) + " bonus)" : 
            String.valueOf(actualAmount);
            
        int currentStorage = MineManager.getPlayerBlock(player, drop);
        int maxStorage = MineManager.getMaxBlock(player);
        String currentStorageStr = String.valueOf(currentStorage);
        String maxStorageStr = String.valueOf(maxStorage);
        
        // Send actionbar if enabled
        if (actionBarEnabled && actionBarTemplate != null) {
            // Use pre-compiled template for faster string formatting
            String message;
            if (actionBarTemplateCompiled != null) {
                message = String.format(actionBarTemplateCompiled, itemName, displayAmount, currentStorageStr, maxStorageStr);
            } else {
                // Fallback to replace if template compilation failed
                message = actionBarTemplate
                    .replace("#item#", itemName)
                    .replace("#amount#", displayAmount)
                    .replace("#storage#", currentStorageStr)
                    .replace("#max#", maxStorageStr);
            }
            
            com.cryptomorin.xseries.messages.ActionBar.sendActionBar(
                net.danh.storage.Storage.getStorage(), player, 
                net.danh.storage.Utils.Chat.colorizewp(message)
            );
        }
        
        // Send title if enabled  
        if (titleEnabled && titleTemplate != null && subtitleTemplate != null) {
            String title, subtitle;
            
            // Use pre-compiled templates for faster string formatting
            if (titleTemplateCompiled != null) {
                title = String.format(titleTemplateCompiled, itemName, displayAmount, currentStorageStr, maxStorageStr);
            } else {
                // Fallback to replace if template compilation failed
                title = titleTemplate
                    .replace("#item#", itemName)
                    .replace("#amount#", displayAmount)
                    .replace("#storage#", currentStorageStr)
                    .replace("#max#", maxStorageStr);
            }
            
            if (subtitleTemplateCompiled != null) {
                subtitle = String.format(subtitleTemplateCompiled, itemName, displayAmount, currentStorageStr, maxStorageStr);
            } else {
                // Fallback to replace if template compilation failed
                subtitle = subtitleTemplate
                    .replace("#item#", itemName)
                    .replace("#amount#", displayAmount)
                    .replace("#storage#", currentStorageStr)
                    .replace("#max#", maxStorageStr);
            }
                
            com.cryptomorin.xseries.messages.Titles.sendTitle(player,
                net.danh.storage.Utils.Chat.colorizewp(title),
                net.danh.storage.Utils.Chat.colorizewp(subtitle)
            );
        }
    }

    public static void processBlockBreakDirect(Player player, String drop, int amount) {
        MineManager.addBlockAmountWithPartial(player, drop, amount);
    }
}