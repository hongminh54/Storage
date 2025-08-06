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

    public static void playSpecialMaterialParticle(Location location, String particleType, int count,
                                                   double speed, String animation, double radius) {
        if (location == null || location.getWorld() == null) return;

        ParticleAnimation particleAnimation = ParticleAnimation.fromString(animation);

        if (particleAnimation == ParticleAnimation.NONE) {
            // Play simple particle effect
            playSimpleParticleAtLocation(location, particleType, count, speed);
            return;
        }

        // Play animated particle effect
        String animationKey = "special_material_" + location.hashCode();
        stopAnimation(animationKey);

        BukkitTask task = new BukkitRunnable() {
            private final int maxTicks = 20; // 1 second animation
            private int ticks = 0;

            @Override
            public void run() {
                if (ticks >= maxTicks) {
                    cancel();
                    activeAnimations.remove(animationKey);
                    return;
                }

                playSpecialMaterialAnimationFrame(location, particleAnimation, ticks, particleType, count, speed, radius);
                ticks++;
            }
        }.runTaskTimer(Storage.getStorage(), 0L, 1L);

        activeAnimations.put(animationKey, task);
    }

    private static void playSimpleParticleAtLocation(Location location, String particleType, int count, double speed) {
        try {
            NMSAssistant nms = new NMSAssistant();

            if (nms.isVersionGreaterThanOrEqualTo(13)) {
                Particle particle = getModernParticle(particleType);
                if (particle != null && location.getWorld() != null) {
                    location.getWorld().spawnParticle(particle, location, count, 0.5, 0.5, 0.5, speed);
                }
            } else {
                org.bukkit.Effect effect = getLegacyEffect(particleType);
                if (effect != null && location.getWorld() != null) {
                    location.getWorld().playEffect(location, effect, null);
                }
            }
        } catch (Exception e) {
            // Fallback - play simple effect
            if (location.getWorld() != null) {
                try {
                    NMSAssistant nms = new NMSAssistant();
                    if (nms.isVersionGreaterThanOrEqualTo(13)) {
                        location.getWorld().spawnParticle(Particle.VILLAGER_HAPPY, location, count, 0.5, 0.5, 0.5, speed);
                    } else {
                        location.getWorld().playEffect(location, org.bukkit.Effect.VILLAGER_PLANT_GROW, null);
                    }
                } catch (Exception ignored) {
                }
            }
        }
    }

    private static void playSpecialMaterialAnimationFrame(Location center, ParticleAnimation animation, int tick,
                                                          String particleType, int count, double speed, double radius) {
        switch (animation) {
            case CIRCLE:
                playCircleAnimationAtLocation(center, tick, radius, particleType, count, speed);
                break;
            case SPIRAL:
                playSpiralAnimationAtLocation(center, tick, radius, particleType, count, speed);
                break;
            case HELIX:
                playHelixAnimationAtLocation(center, tick, radius, particleType, count, speed);
                break;
            case WAVE:
                playWaveAnimationAtLocation(center, tick, radius, particleType, count, speed);
                break;
            case BURST:
                if (tick % 10 == 0) {
                    playBurstAnimationAtLocation(center, particleType, count * 2, speed);
                }
                break;
            case DNA_HELIX:
                playDNAHelixAnimationAtLocation(center, tick, radius, particleType, count, speed);
                break;
            case GALAXY:
                playGalaxyAnimationAtLocation(center, tick, radius, particleType, count, speed);
                break;
            case TORNADO:
                playTornadoAnimationAtLocation(center, tick, radius, particleType, count, speed);
                break;
            case LIGHTNING:
                playLightningAnimationAtLocation(center, tick, radius, particleType, count, speed);
                break;
            case GEOMETRIC_STAR:
                playGeometricStarAnimationAtLocation(center, tick, radius, particleType, count, speed);
                break;
        }
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
            case DNA_HELIX:
                playDNAHelixAnimation(center, tick, radius, particleName, count, speed);
                break;
            case GALAXY:
                playGalaxyAnimation(center, tick, radius, particleName, count, speed);
                break;
            case TORNADO:
                playTornadoAnimation(center, tick, radius, particleName, count, speed);
                break;
            case LIGHTNING:
                playLightningAnimation(center, tick, radius, particleName, count, speed);
                break;
            case GEOMETRIC_STAR:
                playGeometricStarAnimation(center, tick, radius, particleName, count, speed);
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

    // Advanced Geometric Pattern Animations
    private static void playDNAHelixAnimation(Location center, int tick, double radius, String particleName, int count, double speed) {
        double angle = (tick * 0.3) % (2 * Math.PI);
        double height = (tick * 0.08) % 4.0;

        // Create double helix strands
        for (int strand = 0; strand < 2; strand++) {
            double strandAngle = angle + (strand * Math.PI);
            double x = center.getX() + radius * Math.cos(strandAngle);
            double z = center.getZ() + radius * Math.sin(strandAngle);
            double y = center.getY() + height - 2.0;

            Location particleLocation = new Location(center.getWorld(), x, y, z);
            spawnParticleForNearbyPlayers(particleLocation, particleName, count / 4, speed);

            // Cross-links between strands
            if (tick % 8 == 0) {
                double oppositeX = center.getX() + radius * Math.cos(strandAngle + Math.PI);
                double oppositeZ = center.getZ() + radius * Math.sin(strandAngle + Math.PI);
                Location crossLink = new Location(center.getWorld(),
                        (x + oppositeX) / 2, y, (z + oppositeZ) / 2);
                spawnParticleForNearbyPlayers(crossLink, particleName, 1, speed);
            }
        }
    }

    private static void playGalaxyAnimation(Location center, int tick, double radius, String particleName, int count, double speed) {
        double baseAngle = tick * 0.05;
        int spiralArms = 4;

        for (int arm = 0; arm < spiralArms; arm++) {
            double armAngle = (2 * Math.PI * arm / spiralArms) + baseAngle;

            for (int i = 0; i < count / spiralArms; i++) {
                double distance = (radius * i) / (count / spiralArms);
                double spiralAngle = armAngle + (distance * 2);

                double x = center.getX() + distance * Math.cos(spiralAngle);
                double z = center.getZ() + distance * Math.sin(spiralAngle);
                double y = center.getY() + Math.sin(distance + tick * 0.1) * 0.3;

                Location particleLocation = new Location(center.getWorld(), x, y, z);
                spawnParticleForNearbyPlayers(particleLocation, particleName, 1, speed);
            }
        }
    }

    private static void playTornadoAnimation(Location center, int tick, double radius, String particleName, int count, double speed) {
        double height = 4.0;
        double rotationSpeed = 0.4;

        for (int i = 0; i < count; i++) {
            double heightRatio = (double) i / count;
            double currentRadius = radius * (1.0 - heightRatio * 0.7); // Narrower at top
            double angle = (tick * rotationSpeed + i * 0.5) % (2 * Math.PI);

            double x = center.getX() + currentRadius * Math.cos(angle);
            double z = center.getZ() + currentRadius * Math.sin(angle);
            double y = center.getY() + (heightRatio * height);

            Location particleLocation = new Location(center.getWorld(), x, y, z);
            spawnParticleForNearbyPlayers(particleLocation, particleName, 1, speed);
        }
    }

    private static void playLightningAnimation(Location center, int tick, double radius, String particleName, int count, double speed) {
        if (tick % 15 != 0) return; // Lightning strikes every 15 ticks

        double height = 3.0;
        int segments = 8;
        double lastX = center.getX();
        double lastZ = center.getZ();

        for (int i = 0; i <= segments; i++) {
            double heightRatio = (double) i / segments;
            double randomOffset = (Math.random() - 0.5) * radius * 0.8;

            double x = lastX + randomOffset;
            double z = lastZ + randomOffset;
            double y = center.getY() + (height * heightRatio);

            Location particleLocation = new Location(center.getWorld(), x, y, z);
            spawnParticleForNearbyPlayers(particleLocation, particleName, count / 4, speed * 2);

            lastX = x;
            lastZ = z;
        }
    }

    private static void playGeometricStarAnimation(Location center, int tick, double radius, String particleName, int count, double speed) {
        double angle = (tick * 0.1) % (2 * Math.PI);
        int starPoints = 5;

        for (int point = 0; point < starPoints * 2; point++) {
            double pointAngle = (2 * Math.PI * point / (starPoints * 2)) + angle;
            double currentRadius = (point % 2 == 0) ? radius : radius * 0.5; // Alternating inner/outer points

            double x = center.getX() + currentRadius * Math.cos(pointAngle);
            double z = center.getZ() + currentRadius * Math.sin(pointAngle);

            Location particleLocation = new Location(center.getWorld(), x, center.getY(), z);
            spawnParticleForNearbyPlayers(particleLocation, particleName, count / 10, speed);

            // Connect lines between points
            if (point < starPoints * 2 - 1) {
                double nextPointAngle = (2 * Math.PI * (point + 1) / (starPoints * 2)) + angle;
                double nextRadius = ((point + 1) % 2 == 0) ? radius : radius * 0.5;

                for (int line = 1; line < 5; line++) {
                    double lineRatio = (double) line / 5;
                    double lineX = x + (center.getX() + nextRadius * Math.cos(nextPointAngle) - x) * lineRatio;
                    double lineZ = z + (center.getZ() + nextRadius * Math.sin(nextPointAngle) - z) * lineRatio;

                    Location lineLocation = new Location(center.getWorld(), lineX, center.getY(), lineZ);
                    spawnParticleForNearbyPlayers(lineLocation, particleName, 1, speed);
                }
            }
        }
    }

    // Location-based animation methods for special materials
    private static void playCircleAnimationAtLocation(Location center, int tick, double radius, String particleName, int count, double speed) {
        double angle = (tick * 0.3) % (2 * Math.PI);
        Location particleLocation = center.clone().add(
                Math.cos(angle) * radius,
                0,
                Math.sin(angle) * radius
        );
        playSimpleParticleAtLocation(particleLocation, particleName, count, speed);
    }

    private static void playSpiralAnimationAtLocation(Location center, int tick, double radius, String particleName, int count, double speed) {
        double angle = tick * 0.3;
        double currentRadius = (tick % 20) * radius / 20.0;
        Location particleLocation = center.clone().add(
                Math.cos(angle) * currentRadius,
                (tick % 20) * 0.1,
                Math.sin(angle) * currentRadius
        );
        playSimpleParticleAtLocation(particleLocation, particleName, count, speed);
    }

    private static void playHelixAnimationAtLocation(Location center, int tick, double radius, String particleName, int count, double speed) {
        double angle = tick * 0.4;
        double height = (tick % 40) * 0.05;

        Location loc1 = center.clone().add(
                Math.cos(angle) * radius,
                height,
                Math.sin(angle) * radius
        );
        Location loc2 = center.clone().add(
                Math.cos(angle + Math.PI) * radius,
                height,
                Math.sin(angle + Math.PI) * radius
        );

        playSimpleParticleAtLocation(loc1, particleName, count / 2, speed);
        playSimpleParticleAtLocation(loc2, particleName, count / 2, speed);
    }

    private static void playWaveAnimationAtLocation(Location center, int tick, double radius, String particleName, int count, double speed) {
        for (int i = 0; i < 8; i++) {
            double angle = (i * Math.PI / 4) + (tick * 0.2);
            double waveRadius = radius * (1 + 0.3 * Math.sin(tick * 0.5 + i));
            Location particleLocation = center.clone().add(
                    Math.cos(angle) * waveRadius,
                    Math.sin(tick * 0.3 + i) * 0.5,
                    Math.sin(angle) * waveRadius
            );
            playSimpleParticleAtLocation(particleLocation, particleName, 1, speed);
        }
    }

    private static void playBurstAnimationAtLocation(Location center, String particleName, int count, double speed) {
        for (int i = 0; i < count; i++) {
            double angle = (2 * Math.PI * i) / count;
            double distance = 0.5 + Math.random() * 1.5;
            Location particleLocation = center.clone().add(
                    Math.cos(angle) * distance,
                    Math.random() - 0.5,
                    Math.sin(angle) * distance
            );
            playSimpleParticleAtLocation(particleLocation, particleName, 1, speed);
        }
    }

    private static void playDNAHelixAnimationAtLocation(Location center, int tick, double radius, String particleName, int count, double speed) {
        double angle1 = tick * 0.4;
        double angle2 = angle1 + Math.PI;
        double height = (tick % 40) * 0.05;

        for (int i = 0; i < 3; i++) {
            double offset = i * 0.3;
            Location loc1 = center.clone().add(
                    Math.cos(angle1) * radius,
                    height + offset,
                    Math.sin(angle1) * radius
            );
            Location loc2 = center.clone().add(
                    Math.cos(angle2) * radius,
                    height + offset,
                    Math.sin(angle2) * radius
            );

            playSimpleParticleAtLocation(loc1, particleName, 1, speed);
            playSimpleParticleAtLocation(loc2, particleName, 1, speed);
        }
    }

    private static void playGalaxyAnimationAtLocation(Location center, int tick, double radius, String particleName, int count, double speed) {
        for (int arm = 0; arm < 3; arm++) {
            double armAngle = (arm * 2 * Math.PI / 3) + (tick * 0.1);
            for (int i = 0; i < 5; i++) {
                double distance = (i + 1) * radius / 5;
                double angle = armAngle + (i * 0.5);
                Location particleLocation = center.clone().add(
                        Math.cos(angle) * distance,
                        Math.sin(tick * 0.2 + i) * 0.2,
                        Math.sin(angle) * distance
                );
                playSimpleParticleAtLocation(particleLocation, particleName, 1, speed);
            }
        }
    }

    private static void playTornadoAnimationAtLocation(Location center, int tick, double radius, String particleName, int count, double speed) {
        for (int i = 0; i < count; i++) {
            double height = (i * 2.0) / count;
            double angle = (tick * 0.5) + (i * 0.3);
            double currentRadius = radius * (1 - height / 2.0);

            Location particleLocation = center.clone().add(
                    Math.cos(angle) * currentRadius,
                    height,
                    Math.sin(angle) * currentRadius
            );
            playSimpleParticleAtLocation(particleLocation, particleName, 1, speed);
        }
    }

    private static void playLightningAnimationAtLocation(Location center, int tick, double radius, String particleName, int count, double speed) {
        if (tick % 5 == 0) {
            for (int i = 0; i < 3; i++) {
                double angle = Math.random() * 2 * Math.PI;
                double distance = Math.random() * radius;
                Location particleLocation = center.clone().add(
                        Math.cos(angle) * distance,
                        Math.random() * 2.0 - 1.0,
                        Math.sin(angle) * distance
                );
                playSimpleParticleAtLocation(particleLocation, particleName, count / 3, speed);
            }
        }
    }

    private static void playGeometricStarAnimationAtLocation(Location center, int tick, double radius, String particleName, int count, double speed) {
        int points = 5;
        for (int i = 0; i < points * 2; i++) {
            double angle = (i * Math.PI / points) + (tick * 0.1);
            double distance = (i % 2 == 0) ? radius : radius * 0.5;
            Location particleLocation = center.clone().add(
                    Math.cos(angle) * distance,
                    Math.sin(tick * 0.3) * 0.3,
                    Math.sin(angle) * distance
            );
            playSimpleParticleAtLocation(particleLocation, particleName, 1, speed);
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
