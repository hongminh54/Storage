package net.danh.storage.Enchant;

import com.cryptomorin.xseries.XSound;
import com.cryptomorin.xseries.particles.ParticleDisplay;
import com.cryptomorin.xseries.particles.XParticle;
import net.danh.storage.Manager.EnchantManager;
import net.danh.storage.Storage;
import net.danh.storage.Utils.File;
import net.danh.storage.WorldGuard.WorldGuard;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class HasteEnchant {

    private static final Map<UUID, Long> playerCooldowns = new HashMap<>();
    private static final Map<UUID, Long> playerLastHaste = new HashMap<>();

    public static void triggerHaste(Player player, int level) {
        EnchantManager.EnchantData enchantData = EnchantManager.getEnchantData("haste");
        if (enchantData == null) return;

        EnchantManager.EnchantLevelData levelData = enchantData.levels.get(level);
        if (levelData == null) return;

        UUID playerId = player.getUniqueId();
        long currentTime = System.currentTimeMillis();

        if (playerCooldowns.containsKey(playerId)) {
            long lastHaste = playerCooldowns.get(playerId);
            long cooldownMs = levelData.cooldownTicks * 50L;

            if (currentTime - lastHaste < cooldownMs) {
                return;
            }
        }

        applyHasteEffect(player, levelData, enchantData);
        playerCooldowns.put(playerId, currentTime);
    }

    private static void applyHasteEffect(Player player, EnchantManager.EnchantLevelData levelData,
                                         EnchantManager.EnchantData enchantData) {

        if (!player.isOnline()) return;

        if (Storage.isWorldGuardInstalled()) {
            if (!WorldGuard.handleForLocation(player, player.getLocation())) {
                return;
            }
        }

        if (File.getConfig().contains("blacklist_world")) {
            if (File.getConfig().getStringList("blacklist_world").contains(player.getWorld().getName())) {
                return;
            }
        }

        // Apply improved haste effect
        int hasteLevel = (int) levelData.hasteLevel;
        int duration = levelData.hasteDuration;

        // Check if player already has haste effect and refresh if needed
        UUID playerId = player.getUniqueId();
        long currentTime = System.currentTimeMillis();

        // Refresh logic: if player had haste recently (within 5 seconds), extend duration
        if (playerLastHaste.containsKey(playerId)) {
            long lastHasteTime = playerLastHaste.get(playerId);
            if (currentTime - lastHasteTime < 5000) { // 5 seconds
                duration = Math.max(duration, 300); // Minimum 15 seconds when refreshing
            }
        }

        PotionEffect hasteEffect = new PotionEffect(PotionEffectType.FAST_DIGGING, duration, hasteLevel - 1, false, false);
        player.addPotionEffect(hasteEffect, true);

        // Update last haste time
        playerLastHaste.put(playerId, currentTime);

        createCustomEffects(player.getLocation(), enchantData);
    }

    private static void createCustomEffects(Location location, EnchantManager.EnchantData enchantData) {
        if (!enchantData.particlesEnabled) return;

        new BukkitRunnable() {
            @Override
            public void run() {
                if (location.getWorld() == null) return;

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
                        Storage.getStorage().getLogger().warning("Failed to spawn Haste enchant particles: " + e.getMessage());
                    }
                }

                if (enchantData.soundsEnabled) {
                    try {
                        XSound sound = XSound.matchXSound(enchantData.explosionSound).orElse(XSound.ENTITY_GENERIC_EXPLODE);
                        sound.play(location, enchantData.soundVolume, enchantData.soundPitch);
                    } catch (Exception e) {
                        Storage.getStorage().getLogger().warning("Failed to play Haste enchant sound: " + e.getMessage());
                    }
                }
            }
        }.runTaskLater(Storage.getStorage(), enchantData.soundDelayTicks);
    }

    // Cleanup method to prevent memory leaks
    public static void cleanupOldEntries() {
        long currentTime = System.currentTimeMillis();
        long cleanupThreshold = 300000; // 5 minutes

        playerCooldowns.entrySet().removeIf(entry ->
                currentTime - entry.getValue() > cleanupThreshold);

        playerLastHaste.entrySet().removeIf(entry ->
                currentTime - entry.getValue() > cleanupThreshold);
    }
}
