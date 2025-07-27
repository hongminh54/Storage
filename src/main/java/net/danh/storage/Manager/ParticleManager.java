package net.danh.storage.Manager;

import net.danh.storage.NMS.NMSAssistant;
import net.danh.storage.Storage;
import net.danh.storage.Utils.File;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;

public class ParticleManager {

    private static final Map<Player, Map<String, Long>> particleCooldowns = new HashMap<>();
    private static final long PARTICLE_COOLDOWN_MS = 500;

    public static void playParticle(Player player, ParticleType particleType) {
        if (player == null || !player.isOnline()) return;

        FileConfiguration config = File.getConfig();

        if (!config.getBoolean("transfer.particles.enabled", true)) return;

        String particleName = config.getString(particleType.getConfigPath() + ".type");
        if (particleName == null || particleName.equalsIgnoreCase("none")) return;

        int count = config.getInt(particleType.getConfigPath() + ".count", 10);
        double speed = config.getDouble(particleType.getConfigPath() + ".speed", 0.1);

        if (!canPlayParticle(player, particleType.name())) return;

        try {
            Location location = player.getLocation().add(0, 1, 0);
            playParticleEffect(player, location, particleName, count, speed);
            recordParticlePlayed(player, particleType.name());
        } catch (Exception e) {
            Storage.getStorage().getLogger().warning("Failed to play particle: " + particleName + " for player: " + player.getName());
        }
    }

    public static void playParticleAtLocation(Location location, ParticleType particleType) {
        if (location == null || location.getWorld() == null) return;

        FileConfiguration config = File.getConfig();

        if (!config.getBoolean("transfer.particles.enabled", true)) return;

        String particleName = config.getString(particleType.getConfigPath() + ".type");
        if (particleName == null || particleName.equalsIgnoreCase("none")) return;

        int count = config.getInt(particleType.getConfigPath() + ".count", 10);
        double speed = config.getDouble(particleType.getConfigPath() + ".speed", 0.1);

        try {
            for (Player player : location.getWorld().getPlayers()) {
                if (player.getLocation().distance(location) <= 32) {
                    playParticleEffect(player, location, particleName, count, speed);
                }
            }
        } catch (Exception e) {
            Storage.getStorage().getLogger().warning("Failed to play particle at location: " + particleName);
        }
    }

    private static void playParticleEffect(Player player, Location location, String particleName, int count, double speed) {
        NMSAssistant nms = new NMSAssistant();

        try {
            if (nms.isVersionGreaterThanOrEqualTo(13)) {
                Particle particle = getModernParticle(particleName);
                if (particle != null) {
                    player.spawnParticle(particle, location, count, 0.5, 0.5, 0.5, speed);
                }
            } else {
                playLegacyParticle(player, location, particleName, count, speed);
            }
        } catch (Exception e) {
            playFallbackParticle(player, location, count, speed);
        }
    }

    private static Particle getModernParticle(String particleName) {
        try {
            return Particle.valueOf(particleName.toUpperCase());
        } catch (IllegalArgumentException e) {
            Map<String, Particle> particleMap = new HashMap<>();
            NMSAssistant nms = new NMSAssistant();

            if (nms.isVersionGreaterThanOrEqualTo(13)) {
                try {
                    particleMap.put("VILLAGER_HAPPY", Particle.valueOf("HAPPY_VILLAGER"));
                } catch (IllegalArgumentException ex) {
                    try {
                        particleMap.put("VILLAGER_HAPPY", Particle.valueOf("VILLAGER_HAPPY"));
                    } catch (IllegalArgumentException ex2) {
                        // Fallback for very old versions
                    }
                }

                try {
                    particleMap.put("HEART", Particle.valueOf("HEART"));
                    particleMap.put("CRIT", Particle.valueOf("CRIT"));
                    particleMap.put("SMOKE_NORMAL", Particle.valueOf("SMOKE_NORMAL"));
                    particleMap.put("FIREWORKS_SPARK", Particle.valueOf("FIREWORKS_SPARK"));
                } catch (IllegalArgumentException ex) {
                    // Some particles might not exist in certain versions
                }

                try {
                    particleMap.put("ENCHANTMENT_TABLE", Particle.valueOf("ENCHANTMENT_TABLE"));
                } catch (IllegalArgumentException ex) {
                    try {
                        particleMap.put("ENCHANTMENT_TABLE", Particle.valueOf("ENCHANT"));
                    } catch (IllegalArgumentException ex2) {
                        // Fallback
                    }
                }
            }

            return particleMap.get(particleName.toUpperCase());
        }
    }

    @SuppressWarnings("deprecation")
    private static void playLegacyParticle(Player player, Location location, String particleName, int count, double speed) {
        try {
            org.bukkit.Effect effect = getLegacyEffect(particleName);
            if (effect != null) {
                player.playEffect(location, effect, null);
            }
        } catch (Exception e) {
            playFallbackParticle(player, location, count, speed);
        }
    }

    private static org.bukkit.Effect getLegacyEffect(String particleName) {
        Map<String, org.bukkit.Effect> effectMap = new HashMap<>();
        effectMap.put("VILLAGER_HAPPY", org.bukkit.Effect.VILLAGER_PLANT_GROW);
        effectMap.put("SMOKE_NORMAL", org.bukkit.Effect.SMOKE);

        // These effects don't exist in legacy Effect enum, use fallback
        effectMap.put("HEART", org.bukkit.Effect.VILLAGER_PLANT_GROW);
        effectMap.put("CRIT", org.bukkit.Effect.VILLAGER_PLANT_GROW);
        effectMap.put("FIREWORKS_SPARK", org.bukkit.Effect.VILLAGER_PLANT_GROW);

        return effectMap.get(particleName.toUpperCase());
    }

    private static void playFallbackParticle(Player player, Location location, int count, double speed) {
        NMSAssistant nms = new NMSAssistant();
        try {
            if (nms.isVersionGreaterThanOrEqualTo(13)) {
                player.spawnParticle(Particle.VILLAGER_HAPPY, location, count, 0.5, 0.5, 0.5, speed);
            } else {
                player.playEffect(location, org.bukkit.Effect.VILLAGER_PLANT_GROW, null);
            }
        } catch (Exception ignored) {
        }
    }

    private static boolean canPlayParticle(Player player, String particleType) {
        long currentTime = System.currentTimeMillis();

        Map<String, Long> playerCooldowns = particleCooldowns.computeIfAbsent(player, k -> new HashMap<>());
        Long lastPlayed = playerCooldowns.get(particleType);

        return lastPlayed == null || (currentTime - lastPlayed) >= PARTICLE_COOLDOWN_MS;
    }

    private static void recordParticlePlayed(Player player, String particleType) {
        long currentTime = System.currentTimeMillis();

        Map<String, Long> playerCooldowns = particleCooldowns.computeIfAbsent(player, k -> new HashMap<>());
        playerCooldowns.put(particleType, currentTime);
    }

    public static void playTransferSuccessParticle(Player player) {
        playParticle(player, ParticleType.TRANSFER_SUCCESS);
    }

    public static void playTransferReceiveParticle(Player player) {
        playParticle(player, ParticleType.TRANSFER_RECEIVE);
    }

    public static void playTransferFailedParticle(Player player) {
        playParticle(player, ParticleType.TRANSFER_FAILED);
    }

    public enum ParticleType {
        TRANSFER_SUCCESS("transfer.particles.success"),
        TRANSFER_RECEIVE("transfer.particles.receive"),
        TRANSFER_FAILED("transfer.particles.failed");

        private final String configPath;

        ParticleType(String configPath) {
            this.configPath = configPath;
        }

        public String getConfigPath() {
            return configPath;
        }
    }
}
