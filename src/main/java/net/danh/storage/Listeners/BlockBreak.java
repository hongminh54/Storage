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
    // Cache frequently accessed config values
    private static Boolean preventRebreak = null;
    private static List<String> blacklistWorlds = null;
    private static long configCacheTime = 0;
    private static final long CONFIG_CACHE_DURATION = 30000; // 30 seconds
    
    // Cache for frequently used enchantments
    private static Enchantment fortuneEnchant = null;

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGH)
    public void onBreak(@NotNull BlockBreakEvent e) {
        Player p = e.getPlayer();
        Block block = e.getBlock();
        
        // Quick early returns to minimize processing
        if (p.getGameMode() == org.bukkit.GameMode.CREATIVE) {
            return;
        }
        
        // Cache frequently accessed config values
        long currentTime = System.currentTimeMillis();
        if (currentTime - configCacheTime > CONFIG_CACHE_DURATION) {
            preventRebreak = File.getConfig().getBoolean("prevent_rebreak");
            blacklistWorlds = File.getConfig().getStringList("blacklist_world");
            configCacheTime = currentTime;
            
            // Cache fortune enchantment
            fortuneEnchant = XEnchantment.FORTUNE.get() != null ? 
                XEnchantment.FORTUNE.get() : 
                Objects.requireNonNull(XEnchantment.of(Enchantment.LOOT_BONUS_BLOCKS).get());
        }
        
        if (Storage.isWorldGuardInstalled()) {
            if (!WorldGuard.handleForLocation(p, block.getLocation())) {
                return;
            }
        }
        
        if (preventRebreak && isPlacedBlock(block)) {
            return;
        }
        
        if (blacklistWorlds.contains(p.getWorld().getName())) {
            return;
        }
        
        processBlockBreakOptimized(e, p, block);
    }

    private void processBlockBreakOptimized(@NotNull BlockBreakEvent e, Player p, Block block) {
        // Single check for auto pickup enabled
        boolean autoPickupEnabled = BlockBreakProcessor.isAutoPickupEnabled(p);
        
        // Early return if auto pickup is disabled
        if (!autoPickupEnabled && !MineManager.checkBreak(block)) {
            return;
        }
        
        // Process auto pickup if enabled
        if (autoPickupEnabled) {
            processAutoPickup(e, p, block);
        }
        
        // Process enchants and special drops only if block is valid for storage
        if (MineManager.checkBreak(block)) {
            ItemStack hand = p.getInventory().getItemInMainHand();
            if (hand != null && !hand.getType().name().equals("AIR") && hand.getAmount() > 0) {
                // Batch process enchants to reduce repeated method calls
                processEnchantsBatch(p, hand, block);
            }
            
            // Check for special material drops
            SpecialMaterialManager.checkSpecialMaterialDrop(p, block);
        }
    }
    
    private void processAutoPickup(@NotNull BlockBreakEvent e, Player p, Block block) {
        // Only check if block is valid for storage once
        if (MineManager.checkBreak(block)) {
            String drop = MineManager.getDrop(block);
            if (drop != null) { // Additional null check for safety
                int amount = 1; // Default amount
                ItemStack hand = p.getInventory().getItemInMainHand();
                
                // Only calculate drop amount if we need it
                boolean needsDropCalculation = hand != null && 
                    !hand.getType().name().equals("AIR") && 
                    hand.getAmount() > 0;
                
                if (needsDropCalculation) {
                    if (hand.containsEnchantment(fortuneEnchant)) {
                        // Normalize block type for fortune check (same as in MineManager)
                        String blockTypeForFortune = block.getType().name();
                        if ("LIT_REDSTONE_ORE".equals(blockTypeForFortune) || "GLOWING_REDSTONE_ORE".equals(blockTypeForFortune)) {
                            blockTypeForFortune = "REDSTONE_ORE";
                        }
                        
                        if (File.getConfig().getStringList("whitelist_fortune").contains(blockTypeForFortune)) {
                            amount = Number.getRandomInteger(getDropAmount(block), 
                                getDropAmount(block) + hand.getEnchantmentLevel(fortuneEnchant) + 2);
                        } else {
                            amount = getDropAmount(block);
                        }
                    } else {
                        amount = getDropAmount(block);
                    }

                    // Apply multiplier enchant if present
                    if (EnchantManager.hasEnchant(hand, "multiplier")) {
                        int multiplierLevel = EnchantManager.getEnchantLevel(hand, "multiplier");
                        amount = MultiplierEnchant.calculateMultipliedAmount(p, amount, multiplierLevel);
                    }
                } else {
                    amount = getDropAmount(block);
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
                }

                if (actualAmount < totalAmount) {
                    StorageFullNotificationManager.sendStorageFullNotification(p, drop);
                }
            }
        }
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
        // Single check for all enchantments to minimize EnchantManager calls
        int tntLevel = EnchantManager.getEnchantLevel(hand, "tnt");
        int hasteLevel = EnchantManager.getEnchantLevel(hand, "haste");
        int veinMinerLevel = EnchantManager.getEnchantLevel(hand, "veinminer");
        
        // Process enchants only if they exist
        if (tntLevel > 0) {
            TNTEnchant.triggerExplosion(player, block.getLocation(), tntLevel);
        }
        
        if (hasteLevel > 0) {
            HasteEnchant.triggerHaste(player, hasteLevel);
        }
        
        if (veinMinerLevel > 0) {
            VeinMinerEnchant.triggerVeinMiner(player, block.getLocation(), veinMinerLevel);
        }
    }

    public boolean isPlacedBlock(Block b) {
        // Optimized metadata check - early return if no metadata
        if (!b.hasMetadata("PlacedBlock")) {
            return false;
        }
        
        List<MetadataValue> metaDataValues = b.getMetadata("PlacedBlock");
        // Return early if no metadata values
        if (metaDataValues.isEmpty()) {
            return false;
        }
        
        // Only check first value for performance
        return metaDataValues.get(0).asBoolean();
    }

}
