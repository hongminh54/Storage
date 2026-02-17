package net.danh.storage.Listeners.Crop;

import com.cryptomorin.xseries.messages.ActionBar;
import com.cryptomorin.xseries.messages.Titles;
import net.danh.storage.Manager.Crop.CropStorageManager;
import net.danh.storage.NMS.NMSAssistant;
import net.danh.storage.Storage;
import net.danh.storage.Utils.ChatUtils;
import net.danh.storage.Utils.File;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.Ageable;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

/**
 * Listens for crop block break events and stores drops in CropStorage.
 * Mirrors BlockBreak logic but specifically for vanilla crop harvesting.
 */
public class CropBreak implements Listener {

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onCropBreak(@NotNull BlockBreakEvent e) {
        Player player = e.getPlayer();
        Block block = e.getBlock();

        // Check if CropStorage is enabled
        if (!CropStorageManager.isSystemEnabled()) {
            return;
        }

        // Check if player has auto-pickup enabled
        if (!CropStorageManager.getToggleStatus(player)) {
            return;
        }

        // Check blacklisted worlds
        if (File.getCropStorageConfig().contains("blacklist_world")) {
            if (File.getCropStorageConfig().getStringList("blacklist_world")
                    .contains(player.getWorld().getName())) {
                return;
            }
        }

        // Get the crop block mapping
        Map<String, String> cropMapping = CropStorageManager.getCropBlockMapping();
        String blockType = block.getType().name();
        String dropItem = cropMapping.get(blockType);

        // Not a mapped crop block
        if (dropItem == null) {
            return;
        }

        // Check if this specific item has auto-pickup disabled
        if (CropStorageManager.isItemAutoPickupDisabled(player, dropItem)) {
            return;
        }

        // Check if the drop item is a configured drop
        if (!CropStorageManager.isConfiguredDrop(dropItem)) {
            return;
        }

        // For ageable crops (wheat, carrots, potatoes, beetroot, nether wart),
        // only harvest when fully grown
        if (isAgeableCrop(block) && !isFullyGrown(block)) {
            return;
        }

        // Calculate drop amount from block drops
        int amount = calculateDropAmount(block, player, dropItem);
        if (amount <= 0) {
            amount = 1; // Minimum 1 drop
        }

        // Try to store the crop
        boolean stored = CropStorageManager.addItemAmount(player, dropItem, amount);
        if (stored) {
            // Cancel drops since we stored them
            if (new NMSAssistant().isVersionGreaterThanOrEqualTo(12)) {
                e.setDropItems(false);
            }

            // Send notification
            sendNotification(player, dropItem, amount);
        }
        // If storage is full, let the items drop normally
    }

    /**
     * Check if the block is an ageable crop.
     */
    private boolean isAgeableCrop(@NotNull Block block) {
        try {
            BlockData data = block.getBlockData();
            return data instanceof Ageable;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Check if an ageable crop is fully grown.
     */
    private boolean isFullyGrown(@NotNull Block block) {
        try {
            BlockData data = block.getBlockData();
            if (data instanceof Ageable) {
                Ageable ageable = (Ageable) data;
                return ageable.getAge() >= ageable.getMaximumAge();
            }
            return true; // Non-ageable blocks are always "grown"
        } catch (Exception e) {
            return true;
        }
    }

    /**
     * Calculate the number of drops from breaking a crop block.
     */
    private int calculateDropAmount(@NotNull Block block, @NotNull Player player, @NotNull String dropItem) {
        Material dropMaterial = Material.getMaterial(dropItem);
        if (dropMaterial == null) {
            return 1;
        }

        ItemStack tool = player.getInventory().getItemInMainHand();
        int total = 0;

        try {
            // Get the actual drops from the block
            for (ItemStack drop : (tool != null ? block.getDrops(tool) : block.getDrops())) {
                if (drop != null && drop.getType() == dropMaterial) {
                    total += drop.getAmount();
                }
            }
        } catch (Exception e) {
            // Fallback: try getDrops() without tool
            for (ItemStack drop : block.getDrops()) {
                if (drop != null && drop.getType() == dropMaterial) {
                    total += drop.getAmount();
                }
            }
        }

        return total > 0 ? total : 1;
    }

    /**
     * Send action bar and/or title notification when crops are stored.
     */
    private void sendNotification(@NotNull Player player, @NotNull String dropItem, int amount) {
        String displayName = CropStorageManager.getItemDisplayName(dropItem);
        int currentAmount = CropStorageManager.getPlayerItem(player, dropItem);
        int maxStorage = CropStorageManager.getMaxStorage(player);

        // ActionBar notification
        boolean actionBarEnabled = File.getCropStorageConfig().getBoolean("notification.actionbar.enable", false);
        if (actionBarEnabled) {
            String template = File.getCropStorageConfig().getString("notification.actionbar.item_added");
            if (template != null) {
                String msg = template
                        .replace("#item#", displayName)
                        .replace("#amount#", String.valueOf(amount))
                        .replace("#storage#", String.valueOf(currentAmount))
                        .replace("#max#", String.valueOf(maxStorage));
                ActionBar.sendActionBar(Storage.getStorage(), player, ChatUtils.colorizewp(msg));
            }
        }

        // Title notification
        boolean titleEnabled = File.getCropStorageConfig().getBoolean("notification.title.enable", false);
        if (titleEnabled) {
            String titleTemplate = File.getCropStorageConfig().getString("notification.title.item_added.title");
            String subtitleTemplate = File.getCropStorageConfig().getString("notification.title.item_added.subtitle");
            if (titleTemplate != null && subtitleTemplate != null) {
                String title = titleTemplate
                        .replace("#item#", displayName)
                        .replace("#amount#", String.valueOf(amount))
                        .replace("#storage#", String.valueOf(currentAmount))
                        .replace("#max#", String.valueOf(maxStorage));
                String subtitle = subtitleTemplate
                        .replace("#item#", displayName)
                        .replace("#amount#", String.valueOf(amount))
                        .replace("#storage#", String.valueOf(currentAmount))
                        .replace("#max#", String.valueOf(maxStorage));
                Titles.sendTitle(player, ChatUtils.colorizewp(title),
                        ChatUtils.colorizewp(subtitle));
            }
        }
    }
}
