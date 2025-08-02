package net.danh.storage.Manager;

import net.danh.storage.NMS.NMSAssistant;
import net.danh.storage.Particles.ParticleAnimation;
import net.danh.storage.Storage;
import net.danh.storage.Utils.File;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ParticleManager {

    private static final Map<Player, Map<String, Long>> particleCooldowns = new HashMap<>();
    private static final Map<String, BukkitTask> activeAnimations = new ConcurrentHashMap<>();
    private static final long PARTICLE_COOLDOWN_MS = 500;
    private static final double MAX_PARTICLE_DISTANCE = 32.0;

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
        playEnhancedParticle(player, ParticleType.TRANSFER_SUCCESS);
    }

    public static void playTransferReceiveParticle(Player player) {
        playEnhancedParticle(player, ParticleType.TRANSFER_RECEIVE);
    }

    public static void playTransferFailedParticle(Player player) {
        playEnhancedParticle(player, ParticleType.TRANSFER_FAILED);
    }

    private static void playEnhancedParticle(Player player, ParticleType particleType) {
        if (player == null || !player.isOnline()) return;

        FileConfiguration config = File.getConfig();
        if (!config.getBoolean("transfer.particles.enabled", true)) return;

        String configPath = particleType.getConfigPath();
        ParticleAnimation animation = ParticleAnimation.fromString(
                config.getString(configPath + ".animation", "none"));

        if (animation == ParticleAnimation.NONE) {
            playParticle(player, particleType);
            return;
        }

        String animationKey = particleType.name().toLowerCase() + "_" + player.getName();
        stopAnimation(animationKey);

        BukkitTask task = new BukkitRunnable() {
            private final int maxTicks = 20;
            private int ticks = 0;

            @Override
            public void run() {
                if (!player.isOnline() || ticks >= maxTicks) {
                    cancel();
                    activeAnimations.remove(animationKey);
                    return;
                }

                playAnimationFrame(player, animation, ticks, configPath);
                ticks++;
            }
        }.runTaskTimer(Storage.getStorage(), 0L, 1L);

        activeAnimations.put(animationKey, task);
    }

    public static void playTransferProcessingAnimation(Player player, int durationSeconds) {
        String animationKey = "processing_" + player.getName();
        stopAnimation(animationKey);

        FileConfiguration config = File.getConfig();
        if (!config.getBoolean("transfer.particles.enabled", true)) return;

        ParticleAnimation animation = ParticleAnimation.fromString(
                config.getString("transfer.particles.processing.animation", "circle"));

        if (animation == ParticleAnimation.NONE) return;

        BukkitTask task = new BukkitRunnable() {
            private final int maxTicks = durationSeconds * 20;
            private int ticks = 0;

            @Override
            public void run() {
                if (!player.isOnline() || ticks >= maxTicks) {
                    cancel();
                    activeAnimations.remove(animationKey);
                    return;
                }

                playAnimationFrame(player, animation, ticks, "transfer.particles.processing");
                ticks++;
            }
        }.runTaskTimer(Storage.getStorage(), 0L, 1L);

        activeAnimations.put(animationKey, task);
    }

    public static void playTransferBeamEffect(Player sender, Player receiver) {
        if (sender == null || receiver == null || !sender.isOnline() || !receiver.isOnline()) return;

        FileConfiguration config = File.getConfig();
        if (!config.getBoolean("transfer.particles.enabled", true)) return;

        String animationKey = "beam_" + sender.getName() + "_" + receiver.getName();
        stopAnimation(animationKey);

        BukkitTask task = new BukkitRunnable() {
            private final int maxTicks = config.getInt("transfer.particles.beam.duration", 20);
            private int ticks = 0;

            @Override
            public void run() {
                if (!sender.isOnline() || !receiver.isOnline() || ticks >= maxTicks) {
                    cancel();
                    activeAnimations.remove(animationKey);
                    return;
                }

                playBeamAnimation(sender.getLocation().add(0, 1, 0),
                        receiver.getLocation().add(0, 1, 0),
                        ticks, maxTicks, "transfer.particles.beam");
                ticks++;
            }
        }.runTaskTimer(Storage.getStorage(), 0L, 2L);

        activeAnimations.put(animationKey, task);
    }

    public static void stopTransferProcessingAnimation(Player player) {
        stopAnimation("processing_" + player.getName());
    }

    public static void playConvertParticle(Player player) {
        if (player == null || !player.isOnline()) return;

        FileConfiguration config = File.getConfig();
        if (!config.getBoolean("convert.particles.enabled", true)) return;

        String animationKey = "convert_" + player.getName();
        stopAnimation(animationKey);

        ParticleAnimation animation = ParticleAnimation.fromString(
                config.getString("convert.particles.animation", "helix"));

        if (animation == ParticleAnimation.NONE) {
            String particleName = config.getString("convert.particles.type", "ENCHANTMENT_TABLE");
            int count = config.getInt("convert.particles.count", 12);
            double speed = config.getDouble("convert.particles.speed", 0.1);

            Location location = player.getLocation().add(0, 1, 0);
            playParticleEffect(player, location, particleName, count, speed);
            return;
        }

        int duration = config.getInt("convert.particles.duration", 2);
        BukkitTask task = new BukkitRunnable() {
            private final int maxTicks = duration * 20;
            private int ticks = 0;

            @Override
            public void run() {
                if (!player.isOnline() || ticks >= maxTicks) {
                    cancel();
                    activeAnimations.remove(animationKey);
                    return;
                }

                playAnimationFrame(player, animation, ticks, "convert.particles");
                ticks++;
            }
        }.runTaskTimer(Storage.getStorage(), 0L, 1L);

        activeAnimations.put(animationKey, task);
    }

    private static void stopAnimation(String animationKey) {
        BukkitTask task = activeAnimations.remove(animationKey);
        if (task != null && !task.isCancelled()) {
            task.cancel();
        }
    }

    private static void playAnimationFrame(Player player, ParticleAnimation animation, int tick, String configPath) {
        FileConfiguration config = File.getConfig();
        Location center = player.getLocation().add(0, 1, 0);

        String particleName = config.getString(configPath + ".type", "VILLAGER_HAPPY");
        int count = config.getInt(configPath + ".count", 5);
        double speed = config.getDouble(configPath + ".speed", 0.1);
        double radius = config.getDouble(configPath + ".radius", 1.5);

        switch (animation) {
            case CIRCLE:
                playCircleAnimation(center, tick, radius, particleName, count, speed);
                break;
            case SPIRAL:
                playSpiralAnimation(center, tick, radius, particleName, count, speed);
                break;
            case HELIX:
                playHelixAnimation(center, tick, radius, particleName, count, speed);
                break;
            case WAVE:
                playWaveAnimation(center, tick, radius, particleName, count, speed);
                break;
            case BURST:
                if (tick % 10 == 0) {
                    playBurstAnimation(center, particleName, count * 2, speed);
                }
                break;
        }
    }

    private static void playCircleAnimation(Location center, int tick, double radius, String particleName, int count, double speed) {
        double angle = (tick * 0.2) % (2 * Math.PI);
        for (int i = 0; i < count; i++) {
            double currentAngle = angle + (i * 2 * Math.PI / count);
            double x = center.getX() + radius * Math.cos(currentAngle);
            double z = center.getZ() + radius * Math.sin(currentAngle);
            Location particleLocation = new Location(center.getWorld(), x, center.getY(), z);

            spawnParticleForNearbyPlayers(particleLocation, particleName, 1, speed);
        }
    }

    private static void playSpiralAnimation(Location center, int tick, double radius, String particleName, int count, double speed) {
        double angle = (tick * 0.3) % (2 * Math.PI);
        double height = (tick * 0.05) % 2.0;

        for (int i = 0; i < count; i++) {
            double currentAngle = angle + (i * 2 * Math.PI / count);
            double currentRadius = radius * (0.5 + 0.5 * Math.sin(tick * 0.1));
            double x = center.getX() + currentRadius * Math.cos(currentAngle);
            double z = center.getZ() + currentRadius * Math.sin(currentAngle);
            Location particleLocation = new Location(center.getWorld(), x, center.getY() + height, z);

            spawnParticleForNearbyPlayers(particleLocation, particleName, 1, speed);
        }
    }

    private static void playHelixAnimation(Location center, int tick, double radius, String particleName, int count, double speed) {
        double angle = (tick * 0.4) % (2 * Math.PI);
        double height = (tick * 0.1) % 3.0;

        for (int i = 0; i < 2; i++) {
            double helixAngle = angle + (i * Math.PI);
            double x = center.getX() + radius * Math.cos(helixAngle);
            double z = center.getZ() + radius * Math.sin(helixAngle);
            Location particleLocation = new Location(center.getWorld(), x, center.getY() + height, z);

            spawnParticleForNearbyPlayers(particleLocation, particleName, count / 2, speed);
        }
    }

    private static void playWaveAnimation(Location center, int tick, double radius, String particleName, int count, double speed) {
        for (int i = 0; i < count; i++) {
            double angle = (i * 2 * Math.PI / count) + (tick * 0.2);
            double waveRadius = radius + 0.5 * Math.sin(tick * 0.3 + i);
            double x = center.getX() + waveRadius * Math.cos(angle);
            double z = center.getZ() + waveRadius * Math.sin(angle);
            double y = center.getY() + 0.3 * Math.sin(tick * 0.2 + i);
            Location particleLocation = new Location(center.getWorld(), x, y, z);

            spawnParticleForNearbyPlayers(particleLocation, particleName, 1, speed);
        }
    }

    private static void playBurstAnimation(Location center, String particleName, int count, double speed) {
        for (int i = 0; i < count; i++) {
            double angle = Math.random() * 2 * Math.PI;
            double distance = Math.random() * 2.0;
            double x = center.getX() + distance * Math.cos(angle);
            double z = center.getZ() + distance * Math.sin(angle);
            double y = center.getY() + (Math.random() - 0.5);
            Location particleLocation = new Location(center.getWorld(), x, y, z);

            spawnParticleForNearbyPlayers(particleLocation, particleName, 1, speed * 2);
        }
    }

    private static void playBeamAnimation(Location start, Location end, int tick, int maxTicks, String configPath) {
        FileConfiguration config = File.getConfig();
        String particleName = config.getString(configPath + ".type", "FIREWORKS_SPARK");
        int count = config.getInt(configPath + ".count", 3);
        double speed = config.getDouble(configPath + ".speed", 0.1);

        double progress = (double) tick / maxTicks;
        int particles = 10;

        for (int i = 0; i <= particles; i++) {
            double ratio = (double) i / particles;
            if (ratio > progress) break;

            double x = start.getX() + (end.getX() - start.getX()) * ratio;
            double y = start.getY() + (end.getY() - start.getY()) * ratio;
            double z = start.getZ() + (end.getZ() - start.getZ()) * ratio;
            Location particleLocation = new Location(start.getWorld(), x, y, z);

            spawnParticleForNearbyPlayers(particleLocation, particleName, count, speed);
        }
    }

    private static void spawnParticleForNearbyPlayers(Location location, String particleName, int count, double speed) {
        if (location.getWorld() == null) return;

        for (Player player : location.getWorld().getPlayers()) {
            if (player.getLocation().distance(location) <= MAX_PARTICLE_DISTANCE) {
                playParticleEffect(player, location, particleName, count, speed);
            }
        }
    }

    public enum ParticleType {
        TRANSFER_SUCCESS("transfer.particles.success"),
        TRANSFER_RECEIVE("transfer.particles.receive"),
        TRANSFER_FAILED("transfer.particles.failed"),
        TRANSFER_PROCESSING("transfer.particles.processing"),
        TRANSFER_BEAM("transfer.particles.beam");

        private final String configPath;

        ParticleType(String configPath) {
            this.configPath = configPath;
        }

        public String getConfigPath() {
            return configPath;
        }
    }
}
