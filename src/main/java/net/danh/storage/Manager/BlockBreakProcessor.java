package net.danh.storage.Manager;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class BlockBreakProcessor {

    private static final Map<UUID, Boolean> autoPickupCache = new HashMap<>();
    private static final Map<String, Object> configCache = new HashMap<>();
    private static final long CACHE_DURATION = 5000; // 5 seconds
    private static long lastCacheUpdate = 0;
    
    // Cache config values to avoid repeated File.getConfig() calls
    private static Boolean actionBarEnabled = null;
    private static Boolean titleEnabled = null;
    private static String actionBarTemplate = null;
    private static String titleTemplate = null;
    private static String subtitleTemplate = null;
    private static long configLastUpdate = 0;

    public static void initialize() {
        // Simple initialization - no background tasks
    }

    public static boolean isAutoPickupEnabled(Player player) {
        UUID uuid = player.getUniqueId();
        long currentTime = System.currentTimeMillis();

        if (currentTime - lastCacheUpdate > CACHE_DURATION) {
            autoPickupCache.clear();
            lastCacheUpdate = currentTime;
        }

        return autoPickupCache.computeIfAbsent(uuid, k -> MineManager.isAutoPickupEnabled(player));
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

    // Cache and optimize config access
    public static void updateConfigCache() {
        long currentTime = System.currentTimeMillis();
        if (currentTime - configLastUpdate > CACHE_DURATION) {
            actionBarEnabled = net.danh.storage.Utils.File.getConfig().getBoolean("mine.actionbar.enable");
            titleEnabled = net.danh.storage.Utils.File.getConfig().getBoolean("mine.title.enable");
            actionBarTemplate = net.danh.storage.Utils.File.getConfig().getString("mine.actionbar.action");
            titleTemplate = net.danh.storage.Utils.File.getConfig().getString("mine.title.title");
            subtitleTemplate = net.danh.storage.Utils.File.getConfig().getString("mine.title.subtitle");
            configLastUpdate = currentTime;
        }
    }
    
    // Optimized notification processing with minimal string operations
    public static void sendOptimizedNotifications(Player player, String drop, int actualAmount, int bonusAmount) {
        updateConfigCache();
        
        if (!actionBarEnabled && !titleEnabled) return;
        
        // Cache heavy calculations
        String itemName = net.danh.storage.Utils.File.getConfig().getString("items." + drop);
        if (itemName == null) itemName = drop.replace("_", " ");
        
        String displayAmount = bonusAmount > 0 ? 
            actualAmount + " (+" + Math.min(bonusAmount, actualAmount) + " bonus)" : 
            String.valueOf(actualAmount);
            
        int currentStorage = MineManager.getPlayerBlock(player, drop);
        int maxStorage = MineManager.getMaxBlock(player);
        
        // Send actionbar if enabled
        if (actionBarEnabled && actionBarTemplate != null) {
            String message = actionBarTemplate
                .replace("#item#", itemName)
                .replace("#amount#", displayAmount)
                .replace("#storage#", String.valueOf(currentStorage))
                .replace("#max#", String.valueOf(maxStorage));
            com.cryptomorin.xseries.messages.ActionBar.sendActionBar(
                net.danh.storage.Storage.getStorage(), player, 
                net.danh.storage.Utils.Chat.colorizewp(message)
            );
        }
        
        // Send title if enabled  
        if (titleEnabled && titleTemplate != null && subtitleTemplate != null) {
            String title = titleTemplate
                .replace("#item#", itemName)
                .replace("#amount#", displayAmount)
                .replace("#storage#", String.valueOf(currentStorage))
                .replace("#max#", String.valueOf(maxStorage));
                
            String subtitle = subtitleTemplate
                .replace("#item#", itemName)
                .replace("#amount#", displayAmount)
                .replace("#storage#", String.valueOf(currentStorage))
                .replace("#max#", String.valueOf(maxStorage));
                
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