package net.danh.storage.Enchant;

import com.cryptomorin.xseries.XEnchantment;
import com.cryptomorin.xseries.XMaterial;
import com.cryptomorin.xseries.XSound;
import net.danh.storage.Manager.EnchantManager;
import net.danh.storage.Manager.EventManager;
import net.danh.storage.Manager.MineManager;
import net.danh.storage.NMS.NMSAssistant;
import net.danh.storage.Storage;
import net.danh.storage.Utils.File;
import net.danh.storage.Utils.Number;
import net.danh.storage.WorldGuard.WorldGuard;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.block.Block;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

public class TNTEnchant {

    private static final Map<UUID, Long> playerCooldowns = new HashMap<>();

    public static void triggerExplosion(Player player, Location location, int level) {
        EnchantManager.EnchantData enchantData = EnchantManager.getEnchantData("tnt");
        if (enchantData == null) return;

        EnchantManager.EnchantLevelData levelData = enchantData.levels.get(level);
        if (levelData == null) return;

        UUID playerId = player.getUniqueId();
        long currentTime = System.currentTimeMillis();

        if (playerCooldowns.containsKey(playerId)) {
            long lastExplosion = playerCooldowns.get(playerId);
            long cooldownMs = levelData.cooldownTicks * 50L; // Convert ticks to milliseconds

            if (currentTime - lastExplosion < cooldownMs) {
                return; // Still in cooldown
            }
        }

        createExplosion(player, location, levelData, enchantData);
        playerCooldowns.put(playerId, currentTime);
    }

    private static void createExplosion(Player player, Location location,
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

        List<Block> blocksToBreak = getBlocksInRadius(location, levelData.radius);

        createCustomEffects(location, enchantData);

        if (enchantData.breakBlocks && enchantData.storageIntegration) {
            processBlocksWithStorage(player, blocksToBreak, enchantData);
        }
    }

    private static void createCustomEffects(Location location, EnchantManager.EnchantData enchantData) {
        if (location.getWorld() == null) return;

        if (enchantData.soundsEnabled) {
            if (enchantData.soundDelayTicks > 0) {
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        playCustomSound(location, enchantData);
                    }
                }.runTaskLater(Storage.getStorage(), enchantData.soundDelayTicks);
            } else {
                playCustomSound(location, enchantData);
            }
        }

        if (enchantData.particlesEnabled) {
            if (enchantData.particleDelayTicks > 0) {
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        spawnCustomParticles(location, enchantData);
                    }
                }.runTaskLater(Storage.getStorage(), enchantData.particleDelayTicks);
            } else {
                spawnCustomParticles(location, enchantData);
            }
        }
    }

    private static void playCustomSound(Location location, EnchantManager.EnchantData enchantData) {
        try {
            XSound sound = XSound.matchXSound(enchantData.explosionSound).orElse(XSound.ENTITY_GENERIC_EXPLODE);
            sound.play(location, enchantData.soundVolume, enchantData.soundPitch);
        } catch (Exception e) {
            XSound.ENTITY_GENERIC_EXPLODE.play(location, enchantData.soundVolume, enchantData.soundPitch);
        }
    }

    private static void spawnCustomParticles(Location location, EnchantManager.EnchantData enchantData) {
        if (location.getWorld() == null) return;

        try {
            if (new NMSAssistant().isVersionGreaterThanOrEqualTo(13)) {
                Particle particle = getParticleFromString(enchantData.particleType);
                if (particle != null) {
                    location.getWorld().spawnParticle(particle, location,
                            enchantData.particleCount,
                            enchantData.particleOffsetX,
                            enchantData.particleOffsetY,
                            enchantData.particleOffsetZ,
                            enchantData.particleExtra);
                } else {
                    location.getWorld().spawnParticle(Particle.EXPLOSION_HUGE, location,
                            enchantData.particleCount,
                            enchantData.particleOffsetX,
                            enchantData.particleOffsetY,
                            enchantData.particleOffsetZ,
                            enchantData.particleExtra);
                }
            }
        } catch (Exception ignored) {
        }
    }

    private static Particle getParticleFromString(String particleName) {
        try {
            return Particle.valueOf(particleName.toUpperCase());
        } catch (Exception e) {
            return null;
        }
    }

    private static List<Block> getBlocksInRadius(Location center, int radius) {
        List<Block> blocks = new ArrayList<>();

        for (int x = -radius; x <= radius; x++) {
            for (int y = -radius; y <= radius; y++) {
                for (int z = -radius; z <= radius; z++) {
                    if (x * x + y * y + z * z <= radius * radius) {
                        Location blockLoc = center.clone().add(x, y, z);
                        Block block = blockLoc.getBlock();

                        if (block != null && !block.getType().name().equals("AIR")) {
                            blocks.add(block);
                        }
                    }
                }
            }
        }

        return blocks;
    }

    private static void processBlocksWithStorage(Player player, List<Block> blocks, EnchantManager.EnchantData enchantData) {
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

    private static int calculateDropAmount(Player player, Block block, ItemStack hand, Enchantment fortune) {
        int baseAmount = getBlockDropAmount(block);

        // Apply Fortune enchant
        if (fortune != null && hand != null && hand.containsEnchantment(fortune)) {
            if (File.getConfig().getStringList("whitelist_fortune").contains(block.getType().name())) {
                int fortuneLevel = hand.getEnchantmentLevel(fortune);
                baseAmount = Number.getRandomInteger(baseAmount, baseAmount + fortuneLevel + 2);
            }
        }

        // Apply Multiplier enchant if present
        if (hand != null && !hand.getType().name().equals("AIR") && hand.getAmount() > 0 && EnchantManager.hasEnchant(hand, "multiplier")) {
            int multiplierLevel = EnchantManager.getEnchantLevel(hand, "multiplier");
            baseAmount = MultiplierEnchant.calculateMultipliedAmount(player, baseAmount, multiplierLevel);
        }

        return baseAmount;
    }

    private static int getBlockDropAmount(Block block) {
        int amount = 0;
        if (block != null && block.getDrops() != null) {
            for (ItemStack itemStack : block.getDrops()) {
                if (itemStack != null) {
                    amount += itemStack.getAmount();
                }
            }
        }
        return amount;
    }


    public static void clearPlayerCooldown(Player player) {
        playerCooldowns.remove(player.getUniqueId());
    }

    public static boolean isPlayerOnCooldown(Player player, int level) {
        EnchantManager.EnchantData enchantData = EnchantManager.getEnchantData("tnt");
        if (enchantData == null) return false;

        EnchantManager.EnchantLevelData levelData = enchantData.levels.get(level);
        if (levelData == null) return false;

        UUID playerId = player.getUniqueId();
        if (!playerCooldowns.containsKey(playerId)) return false;

        long currentTime = System.currentTimeMillis();
        long lastExplosion = playerCooldowns.get(playerId);
        long cooldownMs = levelData.cooldownTicks * 50L;

        return currentTime - lastExplosion < cooldownMs;
    }

    public static long getRemainingCooldown(Player player, int level) {
        EnchantManager.EnchantData enchantData = EnchantManager.getEnchantData("tnt");
        if (enchantData == null) return 0;

        EnchantManager.EnchantLevelData levelData = enchantData.levels.get(level);
        if (levelData == null) return 0;

        UUID playerId = player.getUniqueId();
        if (!playerCooldowns.containsKey(playerId)) return 0;

        long currentTime = System.currentTimeMillis();
        long lastExplosion = playerCooldowns.get(playerId);
        long cooldownMs = levelData.cooldownTicks * 50L;
        long remaining = cooldownMs - (currentTime - lastExplosion);

        return Math.max(0, remaining);
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

    private static boolean isPlacedBlock(Block block) {
        List<MetadataValue> metaDataValues = block.getMetadata("PlacedBlock");
        for (MetadataValue value : metaDataValues) {
            return value.asBoolean();
        }
        return false;
    }
}
