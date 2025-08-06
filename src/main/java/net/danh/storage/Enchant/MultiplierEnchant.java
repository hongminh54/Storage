package net.danh.storage.Enchant;

import net.danh.storage.Manager.EnchantManager;
import net.danh.storage.Storage;
import net.danh.storage.Utils.File;
import net.danh.storage.WorldGuard.WorldGuard;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class MultiplierEnchant {

    private static final Map<UUID, Long> playerCooldowns = new HashMap<>();

    public static int calculateMultipliedAmount(Player player, int originalAmount, int level) {
        EnchantManager.EnchantData enchantData = EnchantManager.getEnchantData("multiplier");
        if (enchantData == null) return originalAmount;

        EnchantManager.EnchantLevelData levelData = enchantData.levels.get(level);
        if (levelData == null) return originalAmount;

        UUID playerId = player.getUniqueId();
        long currentTime = System.currentTimeMillis();

        if (playerCooldowns.containsKey(playerId)) {
            long lastMultiplier = playerCooldowns.get(playerId);
            long cooldownMs = levelData.cooldownTicks * 50L;

            if (currentTime - lastMultiplier < cooldownMs) {
                return originalAmount;
            }
        }

        if (!canUseMultiplier(player)) {
            return originalAmount;
        }

        double multiplier = levelData.multiplierValue; // Using multiplierValue field
        int multipliedAmount = (int) (originalAmount * multiplier);

        createCustomEffects(player.getLocation(), enchantData);
        playerCooldowns.put(playerId, currentTime);

        return multipliedAmount;
    }

    private static boolean canUseMultiplier(Player player) {
        if (!player.isOnline()) return false;

        if (Storage.isWorldGuardInstalled()) {
            if (!WorldGuard.handleForLocation(player, player.getLocation())) {
                return false;
            }
        }

        if (File.getConfig().contains("blacklist_world")) {
            if (File.getConfig().getStringList("blacklist_world").contains(player.getWorld().getName())) {
                return false;
            }
        }

        return true;
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
}
