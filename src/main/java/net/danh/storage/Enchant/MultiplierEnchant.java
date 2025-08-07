package net.danh.storage.Enchant;

import com.cryptomorin.xseries.XSound;
import com.cryptomorin.xseries.particles.XParticle;
import com.cryptomorin.xseries.particles.ParticleDisplay;
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
            return !File.getConfig().getStringList("blacklist_world").contains(player.getWorld().getName());
        }

        return true;
    }

    private static void createCustomEffects(Location location, EnchantManager.EnchantData enchantData) {
        if (!enchantData.particlesEnabled) return;

        new BukkitRunnable() {
            @Override
            public void run() {
                if (location.getWorld() == null) return;

                // Spawn particles using XSeries
                if (enchantData.particlesEnabled) {
                    try {
                        XParticle particle = XParticle.of(enchantData.particleType).orElse(null);
                        if (particle != null) {
                            ParticleDisplay.of(particle)
                                .withLocation(location.add(0, 1, 0))
                                .withCount(enchantData.particleCount)
                                .offset(enchantData.particleOffsetX, enchantData.particleOffsetY, enchantData.particleOffsetZ)
                                .withExtra(enchantData.particleExtra)
                                .spawn();
                        }
                    } catch (Exception e) {
                        Storage.getStorage().getLogger().warning("Failed to spawn Multiplier enchant particles: " + e.getMessage());
                    }
                }

                // Play sounds using XSeries
                if (enchantData.soundsEnabled) {
                    try {
                        XSound sound = XSound.matchXSound(enchantData.explosionSound).orElse(XSound.ENTITY_GENERIC_EXPLODE);
                        sound.play(location, enchantData.soundVolume, enchantData.soundPitch);
                    } catch (Exception e) {
                        Storage.getStorage().getLogger().warning("Failed to play Multiplier enchant sound: " + e.getMessage());
                    }
                }
            }
        }.runTaskLater(Storage.getStorage(), enchantData.soundDelayTicks);
    }
}
