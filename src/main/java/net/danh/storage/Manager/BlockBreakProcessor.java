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

    public static void processBlockBreakDirect(Player player, String drop, int amount) {
        MineManager.addBlockAmountWithPartial(player, drop, amount);
    }
}