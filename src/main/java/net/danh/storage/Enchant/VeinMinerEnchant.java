package net.danh.storage.Enchant;

import com.cryptomorin.xseries.XEnchantment;
import com.cryptomorin.xseries.XMaterial;
import net.danh.storage.Enchant.MultiplierEnchant;
import net.danh.storage.Manager.EnchantManager;
import net.danh.storage.Manager.EventManager;
import net.danh.storage.Manager.MineManager;
import net.danh.storage.NMS.NMSAssistant;
import net.danh.storage.Storage;
import net.danh.storage.Utils.File;
import net.danh.storage.Utils.Number;
import net.danh.storage.WorldGuard.WorldGuard;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

public class VeinMinerEnchant {

    private static final Map<UUID, Long> playerCooldowns = new HashMap<>();

    public static void triggerVeinMiner(Player player, Location location, int level) {
        EnchantManager.EnchantData enchantData = EnchantManager.getEnchantData("veinminer");
        if (enchantData == null) return;

        EnchantManager.EnchantLevelData levelData = enchantData.levels.get(level);
        if (levelData == null) return;

        UUID playerId = player.getUniqueId();
        long currentTime = System.currentTimeMillis();

        if (playerCooldowns.containsKey(playerId)) {
            long lastVeinMiner = playerCooldowns.get(playerId);
            long cooldownMs = levelData.cooldownTicks * 50L;

            if (currentTime - lastVeinMiner < cooldownMs) {
                return;
            }
        }

        mineVein(player, location, levelData, enchantData);
        playerCooldowns.put(playerId, currentTime);
    }

    private static void mineVein(Player player, Location location, 
                                EnchantManager.EnchantLevelData levelData,
                                EnchantManager.EnchantData enchantData) {

        if (!player.isOnline()) return;

        if (Storage.isWorldGuardInstalled()) {
            if (!WorldGuard.handleForLocation(player, location)) {
                return;
            }
        }

        if (File.getConfig().contains("blacklist_world")) {
            if (File.getConfig().getStringList("blacklist_world").contains(location.getWorld().getName())) {
                return;
            }
        }

        Block centerBlock = location.getBlock();
        Material targetMaterial = centerBlock.getType();
        
        // Only mine ores and stone-like blocks
        if (!isValidVeinBlock(targetMaterial)) {
            return;
        }

        int maxBlocks = (int) levelData.maxBlocks; // Using maxBlocks field
        List<Block> veinBlocks = findConnectedBlocks(centerBlock, targetMaterial, maxBlocks);

        createCustomEffects(location, enchantData);

        if (enchantData.breakBlocks && enchantData.storageIntegration) {
            processVeinBlocks(player, veinBlocks, enchantData);
        }
    }

    private static boolean isValidVeinBlock(Material material) {
        String materialName = material.name();
        return materialName.contains("_ORE") || 
               materialName.equals("COAL_BLOCK") ||
               materialName.equals("IRON_BLOCK") ||
               materialName.equals("GOLD_BLOCK") ||
               materialName.equals("DIAMOND_BLOCK") ||
               materialName.equals("EMERALD_BLOCK") ||
               materialName.equals("REDSTONE_BLOCK") ||
               materialName.equals("LAPIS_BLOCK") ||
               materialName.equals("STONE") ||
               materialName.equals("COBBLESTONE");
    }

    private static List<Block> findConnectedBlocks(Block startBlock, Material targetMaterial, int maxBlocks) {
        List<Block> result = new ArrayList<>();
        Set<Block> visited = new HashSet<>();
        Queue<Block> queue = new LinkedList<>();
        
        queue.add(startBlock);
        visited.add(startBlock);
        
        while (!queue.isEmpty() && result.size() < maxBlocks) {
            Block current = queue.poll();
            result.add(current);
            
            // Check 6 adjacent blocks (up, down, north, south, east, west)
            for (int[] offset : new int[][]{{0,1,0}, {0,-1,0}, {1,0,0}, {-1,0,0}, {0,0,1}, {0,0,-1}}) {
                Block adjacent = current.getRelative(offset[0], offset[1], offset[2]);
                
                if (!visited.contains(adjacent) && 
                    adjacent.getType() == targetMaterial &&
                    MineManager.checkBreak(adjacent)) {
                    
                    visited.add(adjacent);
                    queue.add(adjacent);
                }
            }
        }
        
        return result;
    }

    private static void processVeinBlocks(Player player, List<Block> blocks, EnchantManager.EnchantData enchantData) {
        if (!player.isOnline()) return;

        ItemStack hand = player.getInventory().getItemInMainHand();
        Enchantment fortune = XEnchantment.FORTUNE.get() != null ? XEnchantment.FORTUNE.get() :
                Objects.requireNonNull(XEnchantment.of(Enchantment.LOOT_BONUS_BLOCKS).get());

        for (Block block : blocks) {
            if (MineManager.checkBreak(block)) {
                if (File.getConfig().getBoolean("prevent_rebreak")) {
                    if (isPlacedBlock(block)) continue;
                }

                String drop = MineManager.getDrop(block);
                if (drop != null) {
                    int amount = calculateDropAmount(player, block, hand, fortune);
                    int bonusAmount = EventManager.calculateDoubleDropBonus(amount);
                    int totalAmount = amount + bonusAmount;

                    if (totalAmount > 0) {
                        // Check storage integration setting
                        if (enchantData.storageIntegration && MineManager.toggle.get(player)) {
                            // Add to storage if autopickup is enabled and storage integration is true
                            if (MineManager.addBlockAmount(player, drop, totalAmount)) {
                                EventManager.onPlayerMine(player, drop, amount);
                                block.setType(XMaterial.AIR.parseMaterial());
                            }
                        } else {
                            // Drop items vanilla style when storage integration is false or autopickup is disabled
                            EventManager.onPlayerMine(player, drop, amount);
                            block.setType(XMaterial.AIR.parseMaterial());
                            dropItemsVanilla(block.getLocation(), drop, totalAmount);
                        }
                    }
                }
            }
        }
    }

    private static boolean isPlacedBlock(Block block) {
        if (block.hasMetadata("placed")) {
            for (MetadataValue value : block.getMetadata("placed")) {
                if (value.asBoolean()) return true;
            }
        }
        return false;
    }

    private static int calculateDropAmount(Player player, Block block, ItemStack hand, Enchantment fortune) {
        int baseAmount = getDropAmount(block);

        // Apply Fortune enchant
        if (hand != null && !hand.getType().name().equals("AIR") && hand.getAmount() > 0 && hand.containsEnchantment(fortune)) {
            if (File.getConfig().getStringList("whitelist_fortune").contains(block.getType().name())) {
                baseAmount = Number.getRandomInteger(baseAmount, baseAmount + hand.getEnchantmentLevel(fortune) + 2);
            }
        }

        // Apply Multiplier enchant if present
        if (hand != null && !hand.getType().name().equals("AIR") && hand.getAmount() > 0 && EnchantManager.hasEnchant(hand, "multiplier")) {
            int multiplierLevel = EnchantManager.getEnchantLevel(hand, "multiplier");
            baseAmount = MultiplierEnchant.calculateMultipliedAmount(player, baseAmount, multiplierLevel);
        }

        return baseAmount;
    }

    private static int getDropAmount(Block block) {
        NMSAssistant nms = new NMSAssistant();
        if (nms.isVersionLessThanOrEqualTo(12)) {
            return 1;
        }
        
        Collection<ItemStack> drops = block.getDrops();
        return drops.isEmpty() ? 1 : drops.iterator().next().getAmount();
    }

    private static void createCustomEffects(Location location, EnchantManager.EnchantData enchantData) {
        if (!enchantData.particlesEnabled) return;

        new BukkitRunnable() {
            @Override
            public void run() {
                if (location.getWorld() == null) return;

                try {
                    location.getWorld().spawnParticle(
                            org.bukkit.Particle.valueOf(enchantData.particleType),
                            location.add(0, 1, 0),
                            enchantData.particleCount,
                            enchantData.particleOffsetX,
                            enchantData.particleOffsetY,
                            enchantData.particleOffsetZ
                    );
                } catch (Exception ignored) {
                    // Fallback for older versions
                }

                if (enchantData.soundsEnabled && location.getWorld() != null) {
                    try {
                        location.getWorld().playSound(location, 
                                org.bukkit.Sound.valueOf(enchantData.explosionSound),
                                enchantData.soundVolume, 
                                enchantData.soundPitch);
                    } catch (Exception ignored) {
                        // Fallback for older versions
                    }
                }
            }
        }.runTaskLater(Storage.getStorage(), enchantData.soundDelayTicks);
    }

    private static void dropItemsVanilla(Location location, String drop, int amount) {
        if (location.getWorld() == null || amount <= 0) return;

        try {
            String[] dropData = drop.split(";");
            String materialName = dropData[0];

            Optional<XMaterial> xMaterial = XMaterial.matchXMaterial(materialName);
            if (xMaterial.isPresent()) {
                ItemStack itemStack = xMaterial.get().parseItem();
                if (itemStack != null) {
                    itemStack.setAmount(Math.min(amount, itemStack.getMaxStackSize()));

                    // Drop items in stacks if amount exceeds max stack size
                    int remaining = amount;
                    while (remaining > 0) {
                        int dropAmount = Math.min(remaining, itemStack.getMaxStackSize());
                        ItemStack dropItem = itemStack.clone();
                        dropItem.setAmount(dropAmount);
                        location.getWorld().dropItemNaturally(location, dropItem);
                        remaining -= dropAmount;
                    }
                }
            }
        } catch (Exception e) {
            // Fallback: drop as generic item if parsing fails
            Storage.getStorage().getLogger().warning("Failed to drop items for: " + drop);
        }
    }
}
