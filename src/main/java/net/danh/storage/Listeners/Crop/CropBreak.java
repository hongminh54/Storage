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
import org.bukkit.block.data.Waterlogged;
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

        boolean canDisableDrops = new NMSAssistant().isVersionGreaterThanOrEqualTo(12);

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

        // Special handling for tall column crops: sugar cane, cactus, bamboo, kelp.
        // When breaking any block of the column, store the whole column and prevent
        // the rest of the column from dropping items on the ground.
        if (isTallColumnCrop(block.getType(), dropItem)) {
            String scanMode = File.getCropStorageConfig().getString("tall_crops.scan_mode", "any_segment");
            boolean baseOnly = "base_only".equalsIgnoreCase(scanMode);
            boolean aboveOnly = "above_only".equalsIgnoreCase(scanMode);

            if (baseOnly && !isTallColumnBase(block)) {
                return;
            }

            Block scanStart;
            if (aboveOnly) {
                scanStart = block;
            } else {
                scanStart = findTallColumnBase(block);
            }

            if (scanStart == null) {
                return;
            }

            int columnCount = countTallColumnBlocks(scanStart);
            if (columnCount <= 0) {
                return;
            }

            int currentAmount = CropStorageManager.getPlayerItem(player, dropItem);
            int maxStorage = CropStorageManager.getMaxStorage(player);
            if (currentAmount + columnCount > maxStorage) {
                // Not enough storage capacity -> let vanilla drops happen.
                return;
            }

            boolean stored = CropStorageManager.addItemAmount(player, dropItem, columnCount);
            if (!stored) {
                return;
            }

            // Remove the scanned column blocks without dropping items.
            removeTallColumnBlocks(scanStart, columnCount, block, canDisableDrops);

            if (canDisableDrops) {
                e.setDropItems(false);
            } else {
                // 1.8 - 1.11: prevent vanilla drops by cancelling the break.
                e.setCancelled(true);
            }

            // Send notification once.
            sendNotification(player, dropItem, columnCount);
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
            if ("WHEAT".equalsIgnoreCase(dropItem) || "BEETROOT".equalsIgnoreCase(dropItem)) {
                dropExtraDrops(block, player, Material.getMaterial(dropItem));
            }

            if (canDisableDrops) {
                e.setDropItems(false);
            } else {
                // 1.8 - 1.11: cancel and remove the block manually to prevent dup drops.
                e.setCancelled(true);
                setBlockToAirNoDrops(block);
            }

            // Send notification
            sendNotification(player, dropItem, amount);
        }
        // If storage is full, let the items drop normally
    }

    private boolean isTallColumnCrop(@NotNull Material blockType,
            @NotNull String dropItem) {
        String upper = dropItem.toUpperCase();
        if ("SUGAR_CANE".equals(upper) || "CACTUS".equals(upper) || "BAMBOO".equals(upper)) {
            return true;
        }
        // Kelp: block can be KELP or KELP_PLANT, drop is KELP
        if ("KELP".equals(upper)) {
            return blockType.name().equalsIgnoreCase("KELP")
                    || blockType.name().equalsIgnoreCase("KELP_PLANT");
        }
        return false;
    }

    private Block findTallColumnBase(@NotNull Block start) {
        Material type = start.getType();
        Block base = start;
        for (int i = 0; i < 256; i++) {
            Block below = base.getRelative(0, -1, 0);
            Material belowType = below.getType();
            if (!isSameTallCropType(type, belowType)) {
                break;
            }
            base = below;
        }
        return base;
    }

    private boolean isTallColumnBase(@NotNull Block block) {
        Material type = block.getType();
        Material belowType = block.getRelative(0, -1, 0).getType();
        return !isSameTallCropType(type, belowType);
    }

    private boolean isSameTallCropType(@NotNull Material reference,
            @NotNull Material candidate) {
        // Kelp: both KELP and KELP_PLANT are part of the same column.
        if (reference.name().equalsIgnoreCase("KELP") || reference.name().equalsIgnoreCase("KELP_PLANT")) {
            return candidate.name().equalsIgnoreCase("KELP")
                    || candidate.name().equalsIgnoreCase("KELP_PLANT");
        }

        return candidate == reference;
    }

    private int countTallColumnBlocks(@NotNull Block start) {
        Material type = start.getType();
        int count = 0;
        for (int i = 0; i < 256; i++) {
            Block b = start.getRelative(0, i, 0);
            Material t = b.getType();

            if (type.name().equalsIgnoreCase("KELP") || type.name().equalsIgnoreCase("KELP_PLANT")) {
                if (!(t.name().equalsIgnoreCase("KELP") || t.name().equalsIgnoreCase("KELP_PLANT"))) {
                    break;
                }
            } else {
                if (t != type) {
                    break;
                }
            }
            count++;
        }
        return count;
    }

    private void removeTallColumnBlocks(@NotNull Block start, int count, @NotNull Block brokenBlock,
            boolean canDisableDrops) {
        for (int i = count - 1; i >= 0; i--) {
            Block b = start.getRelative(0, i, 0);
            if (canDisableDrops && b.equals(brokenBlock)) {
                continue;
            }

            setBlockToAirNoDrops(b);
        }
    }

    private void setBlockToAirNoDrops(@NotNull Block block) {
        try {
            String name = block.getType().name();
            // Kelp and seagrass are water blocks
            if (name.contains("KELP") || name.equals("SEAGRASS") || name.equals("TALL_SEAGRASS")) {
                block.setType(Material.valueOf("WATER"), false);
                return;
            }

            // Check if block is waterlogged (1.13+)
            try {
                BlockData data = block.getBlockData();
                if (data instanceof Waterlogged) {
                    if (((Waterlogged) data).isWaterlogged()) {
                        block.setType(Material.valueOf("WATER"), false);
                        return;
                    }
                }
            } catch (Throwable ignored) {
            }

            // 1.13+ has setType(Material, boolean) to control physics
            block.setType(Material.AIR, false);
        } catch (Throwable ignored) {
            block.setType(Material.AIR);
        }
    }

    private void dropExtraDrops(@NotNull Block block,
            @NotNull Player player,
            Material mainDrop) {
        if (mainDrop == null) {
            return;
        }

        ItemStack tool = player.getInventory().getItemInMainHand();

        try {
            for (ItemStack drop : (tool != null ? block.getDrops(tool) : block.getDrops())) {
                if (drop == null) {
                    continue;
                }
                if (drop.getType() == mainDrop) {
                    continue;
                }
                if (drop.getAmount() <= 0) {
                    continue;
                }
                block.getWorld().dropItemNaturally(block.getLocation(), drop.clone());
            }
        } catch (Exception ignored) {
            // If getDrops(tool) fails in some versions/edge cases, just do nothing.
        }
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
