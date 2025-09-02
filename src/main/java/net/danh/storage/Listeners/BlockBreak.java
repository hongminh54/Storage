package net.danh.storage.Listeners;

import com.cryptomorin.xseries.XEnchantment;
import com.cryptomorin.xseries.messages.ActionBar;
import com.cryptomorin.xseries.messages.Titles;
import net.danh.storage.Enchant.HasteEnchant;
import net.danh.storage.Enchant.MultiplierEnchant;
import net.danh.storage.Enchant.TNTEnchant;
import net.danh.storage.Enchant.VeinMinerEnchant;
import net.danh.storage.Manager.*;
import net.danh.storage.NMS.NMSAssistant;
import net.danh.storage.Storage;
import net.danh.storage.Utils.Chat;
import net.danh.storage.Utils.File;
import net.danh.storage.Utils.Number;
import net.danh.storage.WorldGuard.WorldGuard;
import org.bukkit.block.Block;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.metadata.MetadataValue;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Objects;

public class BlockBreak implements Listener {
    
    // Cache NMS version check to avoid repeated instantiation
    private static Boolean isHigherThan12 = null;

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGH)
    public void onBreak(@NotNull BlockBreakEvent e) {
        Player p = e.getPlayer();
        Block block = e.getBlock();
        
        
        if (Storage.isWorldGuardInstalled()) {
            if (!WorldGuard.handleForLocation(p, block.getLocation())) {
                return;
            }
        }
        if (File.getConfig().getBoolean("prevent_rebreak")) {
            if (isPlacedBlock(block)) return;
        }
        if (File.getConfig().contains("blacklist_world")) {
            if (File.getConfig().getStringList("blacklist_world").contains(p.getWorld().getName())) return;
        }
        processBlockBreakOptimized(e, p, block);
    }

    private void processBlockBreakOptimized(@NotNull BlockBreakEvent e, Player p, Block block) {
        // Skip processing in creative mode to preserve vanilla behavior
        if (p.getGameMode() == org.bukkit.GameMode.CREATIVE) {
            return;
        }
        
        boolean inv_full = !BlockBreakProcessor.hasEmptySlot(p);
        if (BlockBreakProcessor.isAutoPickupEnabled(p)) {
            if (inv_full) {
                BlockBreakProcessor.processInventoryBulk(p);
            }
            
            if (MineManager.checkBreak(block)) {
                String drop = MineManager.getDrop(block);
                int amount;
                ItemStack hand = p.getInventory().getItemInMainHand();
                Enchantment fortune = XEnchantment.FORTUNE.get() != null ? XEnchantment.FORTUNE.get() : Objects.requireNonNull(XEnchantment.of(Enchantment.LOOT_BONUS_BLOCKS).get());
                if (hand == null || hand.getType().name().equals("AIR") || hand.getAmount() <= 0 || !hand.containsEnchantment(fortune)) {
                    amount = getDropAmount(block);
                } else {
                    // Normalize block type for fortune check (same as in MineManager)
                    String blockTypeForFortune = block.getType().name();
                    if ("LIT_REDSTONE_ORE".equals(blockTypeForFortune) || "GLOWING_REDSTONE_ORE".equals(blockTypeForFortune)) {
                        blockTypeForFortune = "REDSTONE_ORE";
                    }
                    
                    if (File.getConfig().getStringList("whitelist_fortune").contains(blockTypeForFortune)) {
                        amount = Number.getRandomInteger(getDropAmount(block), getDropAmount(block) + hand.getEnchantmentLevel(fortune) + 2);
                    } else amount = getDropAmount(block);
                }

                // Apply multiplier enchant if present
                if (hand != null && !hand.getType().name().equals("AIR") && hand.getAmount() > 0 && EnchantManager.hasEnchant(hand, "multiplier")) {
                    int multiplierLevel = EnchantManager.getEnchantLevel(hand, "multiplier");
                    amount = MultiplierEnchant.calculateMultipliedAmount(p, amount, multiplierLevel);
                }

                int bonusAmount = EventManager.calculateDoubleDropBonus(amount);
                int totalAmount = amount + bonusAmount;

                int actualAmount = MineManager.addBlockAmountWithPartial(p, drop, totalAmount);

                if (actualAmount > 0) {
                    EventManager.onPlayerMine(p, drop, amount);
                    
                    // Use optimized notification system
                    BlockBreakProcessor.sendOptimizedNotifications(p, drop, actualAmount, bonusAmount);

                    // Prevent vanilla drops - use both methods for maximum compatibility
                    e.setDropItems(false);
                    e.getBlock().getDrops().clear();
                }

                if (actualAmount < totalAmount) {
                    StorageFullNotificationManager.sendStorageFullNotification(p, drop);
                }
            }
        }

        // Optimized enchant processing - check hand once and batch enchant operations
        if (MineManager.checkBreak(block)) {
            ItemStack hand = p.getInventory().getItemInMainHand();
            if (hand != null && !hand.getType().name().equals("AIR") && hand.getAmount() > 0) {
                processEnchantsBatch(p, hand, block);
            }
        }

        // Check for special material drops
        if (MineManager.checkBreak(block)) {
            SpecialMaterialManager.checkSpecialMaterialDrop(p, block);
        }
    }

    public void removeItems(Player player, ItemStack itemStack, long amount) {
        final PlayerInventory inv = player.getInventory();
        final ItemStack[] items = inv.getContents();
        int c = 0;
        for (int i = 0; i < items.length; ++i) {
            final ItemStack is = items[i];
            if (is != null) {
                if (itemStack != null) {
                    if (is.isSimilar(itemStack)) {
                        if (c + is.getAmount() > amount) {
                            final long canDelete = amount - c;
                            is.setAmount((int) (is.getAmount() - canDelete));
                            items[i] = is;
                            break;
                        }
                        c += is.getAmount();
                        items[i] = null;
                    }
                }
            }
        }
        inv.setContents(items);
        player.updateInventory();
    }

    private int getDropAmount(Block block) {
        // Calculate vanilla drops for proper amount handling
        int amount = 0;
        if (block != null) {
            for (ItemStack itemStack : block.getDrops()) {
                if (itemStack != null) amount += itemStack.getAmount();
            }
        }
        return Math.max(amount, 1); // Ensure at least 1 item
    }

    // Cache NMS version check
    private static boolean isVersionHigherThan12() {
        if (isHigherThan12 == null) {
            isHigherThan12 = new NMSAssistant().isVersionGreaterThanOrEqualTo(12);
        }
        return isHigherThan12;
    }
    
    // Batch process enchants to reduce repeated method calls
    private void processEnchantsBatch(Player player, ItemStack hand, Block block) {
        // Check all enchants in one pass to minimize EnchantManager calls
        boolean hasTnt = EnchantManager.hasEnchant(hand, "tnt");
        boolean hasHaste = EnchantManager.hasEnchant(hand, "haste");
        boolean hasVeinMiner = EnchantManager.hasEnchant(hand, "veinminer");
        
        if (hasTnt) {
            int level = EnchantManager.getEnchantLevel(hand, "tnt");
            TNTEnchant.triggerExplosion(player, block.getLocation(), level);
        }
        
        if (hasHaste) {
            int level = EnchantManager.getEnchantLevel(hand, "haste");
            HasteEnchant.triggerHaste(player, level);
        }
        
        if (hasVeinMiner) {
            int level = EnchantManager.getEnchantLevel(hand, "veinminer");
            VeinMinerEnchant.triggerVeinMiner(player, block.getLocation(), level);
        }
    }

    public boolean isPlacedBlock(Block b) {
        List<MetadataValue> metaDataValues = b.getMetadata("PlacedBlock");
        for (MetadataValue value : metaDataValues) {
            return value.asBoolean();
        }
        return false;
    }

}
