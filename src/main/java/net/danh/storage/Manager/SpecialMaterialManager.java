package net.danh.storage.Manager;

import com.cryptomorin.xseries.XMaterial;
import com.cryptomorin.xseries.XSound;
import net.danh.storage.Enchant.MultiplierEnchant;
import net.danh.storage.NMS.NMSAssistant;
import net.danh.storage.Storage;
import net.danh.storage.Utils.Chat;
import net.danh.storage.Utils.File;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public class SpecialMaterialManager {

    private static final Map<String, SpecialMaterial> specialMaterials = new HashMap<>();
    private static boolean systemEnabled = false;

    public static void loadSpecialMaterials() {
        specialMaterials.clear();

        FileConfiguration config = File.getSpecialMaterialConfig();
        if (config == null) {
            Storage.getStorage().getLogger().warning("Could not load special_material.yml!");
            return;
        }

        systemEnabled = config.getBoolean("settings.enabled", true);

        if (!systemEnabled) {
            Storage.getStorage().getLogger().info("Special Material system is disabled");
            return;
        }

        ConfigurationSection materialsSection = config.getConfigurationSection("special_materials");
        if (materialsSection == null) {
            Storage.getStorage().getLogger().warning("No special materials configured!");
            return;
        }

        int loadedCount = 0;
        for (String materialId : materialsSection.getKeys(false)) {
            try {
                SpecialMaterial material = loadSpecialMaterial(materialId, materialsSection.getConfigurationSection(materialId));
                if (material != null) {
                    specialMaterials.put(materialId, material);
                    loadedCount++;
                }
            } catch (Exception e) {
                Storage.getStorage().getLogger().warning("Failed to load special material: " + materialId + " - " + e.getMessage());
            }
        }

        Storage.getStorage().getLogger().info("Loaded " + loadedCount + " special materials");
    }

    private static SpecialMaterial loadSpecialMaterial(String id, ConfigurationSection section) {
        if (section == null) return null;

        // Load item configuration
        ConfigurationSection itemSection = section.getConfigurationSection("item");
        if (itemSection == null) return null;

        String materialName = itemSection.getString("material");
        if (materialName == null) return null;

        Optional<XMaterial> xMaterial = XMaterial.matchXMaterial(materialName);
        if (!xMaterial.isPresent()) {
            Storage.getStorage().getLogger().warning("Invalid material for special material " + id + ": " + materialName);
            return null;
        }

        ItemStack item = xMaterial.get().parseItem();
        if (item == null) return null;

        ItemMeta meta = item.getItemMeta();
        if (meta == null) return null;

        // Set display name
        String name = itemSection.getString("name");
        if (name != null) {
            meta.setDisplayName(Chat.colorizewp(name));
        }

        // Set lore
        List<String> lore = itemSection.getStringList("lore");
        if (!lore.isEmpty()) {
            List<String> coloredLore = new ArrayList<>();
            for (String line : lore) {
                coloredLore.add(Chat.colorizewp(line));
            }
            meta.setLore(coloredLore);
        }

        // Set custom model data
        if (itemSection.contains("custom_model_data")) {
            int customModelData = itemSection.getInt("custom_model_data");
            if (new NMSAssistant().isVersionGreaterThanOrEqualTo(14)) {
                meta.setCustomModelData(customModelData);
            }
        }

        // Set item flags
        if (itemSection.contains("flags")) {
            ConfigurationSection flagsSection = itemSection.getConfigurationSection("flags");
            if (flagsSection != null) {
                for (String flagName : flagsSection.getKeys(false)) {
                    boolean apply = flagsSection.getBoolean(flagName);
                    if (flagName.equalsIgnoreCase("ALL")) {
                        if (apply) {
                            meta.addItemFlags(ItemFlag.HIDE_UNBREAKABLE, ItemFlag.HIDE_ATTRIBUTES,
                                    ItemFlag.HIDE_DESTROYS, ItemFlag.HIDE_ENCHANTS,
                                    ItemFlag.HIDE_PLACED_ON);
                            try {
                                meta.addItemFlags(ItemFlag.valueOf("HIDE_POTION_EFFECTS"));
                            } catch (IllegalArgumentException ignored) {
                                // Not available in this version
                            }
                            break;
                        }
                    } else {
                        if (apply) {
                            try {
                                ItemFlag flag = ItemFlag.valueOf(flagName);
                                meta.addItemFlags(flag);
                            } catch (IllegalArgumentException e) {
                                Storage.getStorage().getLogger().warning("Invalid item flag for " + id + ": " + flagName);
                            }
                        }
                    }
                }
            }
        }

        // Set glow effect
        boolean glow = itemSection.getBoolean("glow", false);
        if (glow) {
            Enchantment durabilityEnchant = Enchantment.getByName("UNBREAKING");
            if (durabilityEnchant == null) {
                durabilityEnchant = Enchantment.getByName("DURABILITY"); // Fallback for older versions
            }
            meta.addEnchant(durabilityEnchant, 1, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        }

        item.setItemMeta(meta);

        // Load drop configuration
        double dropChance = section.getDouble("drop_chance", 0.0);
        List<String> sourceBlocks = section.getStringList("source_blocks");
        int minAmount = section.getInt("amount.min", 1);
        int maxAmount = section.getInt("amount.max", 1);

        // Load effects configuration
        ConfigurationSection effectsSection = section.getConfigurationSection("effects");
        SpecialMaterialEffects effects = null;
        if (effectsSection != null) {
            effects = loadEffects(effectsSection);
        }

        return new SpecialMaterial(id, item, dropChance, sourceBlocks, minAmount, maxAmount, effects);
    }

    private static SpecialMaterialEffects loadEffects(ConfigurationSection section) {
        // Load sound effects
        ConfigurationSection soundSection = section.getConfigurationSection("sound");
        SpecialMaterialSound sound = null;
        if (soundSection != null && soundSection.getBoolean("enabled", false)) {
            String soundName = soundSection.getString("name");
            float volume = (float) soundSection.getDouble("volume", 1.0);
            float pitch = (float) soundSection.getDouble("pitch", 1.0);
            sound = new SpecialMaterialSound(soundName, volume, pitch);
        }

        // Load particle effects
        ConfigurationSection particleSection = section.getConfigurationSection("particles");
        SpecialMaterialParticle particle = null;
        if (particleSection != null && particleSection.getBoolean("enabled", false)) {
            String type = particleSection.getString("type");
            int count = particleSection.getInt("count", 10);
            double speed = particleSection.getDouble("speed", 0.1);
            String animation = particleSection.getString("animation", "burst");
            double radius = particleSection.getDouble("radius", 1.5);
            particle = new SpecialMaterialParticle(type, count, speed, animation, radius);
        }

        return new SpecialMaterialEffects(sound, particle);
    }

    public static void checkSpecialMaterialDrop(Player player, Block block) {
        checkSpecialMaterialDrop(player, block, null);
    }

    public static void checkSpecialMaterialDrop(Player player, Block block, String enchantType) {
        if (!systemEnabled || specialMaterials.isEmpty()) return;

        String blockKey = getBlockKey(block);

        for (SpecialMaterial material : specialMaterials.values()) {
            if (material.canDropFrom(blockKey)) {
                double dropChance = material.getDropChance();

                // Apply enchant bonuses/penalties
                dropChance = applyEnchantModifier(dropChance, enchantType);

                if (ThreadLocalRandom.current().nextDouble(100.0) <= dropChance) {
                    dropSpecialMaterial(player, block.getLocation(), material);
                }
            }
        }
    }

    private static void dropSpecialMaterial(Player player, Location location, SpecialMaterial material) {
        int amount = ThreadLocalRandom.current().nextInt(material.getMinAmount(), material.getMaxAmount() + 1);

        // Apply Multiplier enchant if present
        ItemStack hand = player.getInventory().getItemInMainHand();
        if (hand != null && !hand.getType().name().equals("AIR") && hand.getAmount() > 0 && EnchantManager.hasEnchant(hand, "multiplier")) {
            int multiplierLevel = EnchantManager.getEnchantLevel(hand, "multiplier");
            amount = MultiplierEnchant.calculateMultipliedAmount(player, amount, multiplierLevel);
        }

        for (int i = 0; i < amount; i++) {
            ItemStack item = material.getItem().clone();
            location.getWorld().dropItemNaturally(location, item);
        }

        // Play effects
        if (material.getEffects() != null) {
            playEffects(player, location, material.getEffects());
        }

        // Send message
        String message = File.getMessage().getString("special_material.found", "#prefix# &aYou found a special material: &e#material#!");
        message = message.replace("#material#", material.getItem().getItemMeta().getDisplayName());
        player.sendMessage(Chat.colorizewp(message.replace("#prefix#", File.getConfig().getString("prefix", ""))));
    }

    private static void playEffects(Player player, Location location, SpecialMaterialEffects effects) {
        // Play sound
        if (effects.getSound() != null) {
            SpecialMaterialSound sound = effects.getSound();
            try {
                XSound xSound = XSound.matchXSound(sound.getName()).orElse(XSound.UI_BUTTON_CLICK);
                xSound.play(player, sound.getVolume(), sound.getPitch());
            } catch (Exception e) {
                Storage.getStorage().getLogger().warning("Failed to play special material sound: " + sound.getName());
            }
        }

        // Play particles
        if (effects.getParticle() != null) {
            SpecialMaterialParticle particle = effects.getParticle();
            ParticleManager.playSpecialMaterialParticle(location, particle.getType(), particle.getCount(),
                    particle.getSpeed(), particle.getAnimation(), particle.getRadius());
        }
    }

    private static double applyEnchantModifier(double baseDropChance, String enchantType) {
        if (enchantType == null) return baseDropChance;

        FileConfiguration config = File.getSpecialMaterialConfig();
        if (config == null) return baseDropChance;

        ConfigurationSection bonusSection = config.getConfigurationSection("enchant_bonuses." + enchantType);
        if (bonusSection == null || !bonusSection.getBoolean("enabled", false)) {
            return baseDropChance;
        }

        switch (enchantType.toLowerCase()) {
            case "veinminer":
                double bonusChance = bonusSection.getDouble("bonus_drop_chance", 0.0);
                return baseDropChance + bonusChance;

            case "tnt":
                double modifier = bonusSection.getDouble("drop_chance_modifier", 1.0);
                return baseDropChance * modifier;

            default:
                return baseDropChance;
        }
    }

    private static String getBlockKey(Block block) {
        NMSAssistant nms = new NMSAssistant();
        return block.getType().name() + ";" + (nms.isVersionLessThanOrEqualTo(12) ? block.getData() : "0");
    }

    // Utility methods for external access
    public static int getLoadedMaterialsCount() {
        return specialMaterials.size();
    }

    public static Set<String> getLoadedMaterialIds() {
        return new HashSet<>(specialMaterials.keySet());
    }

    public static boolean hasMaterial(String materialId) {
        return specialMaterials.containsKey(materialId);
    }

    // Get material info for debugging/admin commands
    public static String getMaterialInfo(String materialId) {
        SpecialMaterial material = specialMaterials.get(materialId);
        if (material == null) return null;

        return String.format("ID: %s, Drop Chance: %.2f%%, Source Blocks: %d, Amount: %d-%d",
                material.getId(),
                material.getDropChance(),
                material.getSourceBlocks().size(),
                material.getMinAmount(),
                material.getMaxAmount());
    }

    // Give special material to player
    public static boolean giveSpecialMaterial(Player player, String materialId, int amount) {
        if (player == null || !player.isOnline()) return false;

        SpecialMaterial material = specialMaterials.get(materialId);
        if (material == null) return false;

        try {
            for (int i = 0; i < amount; i++) {
                ItemStack item = material.getItem().clone();
                if (player.getInventory().firstEmpty() != -1) {
                    player.getInventory().addItem(item);
                } else {
                    player.getWorld().dropItemNaturally(player.getLocation(), item);
                }
            }
            return true;
        } catch (Exception e) {
            Storage.getStorage().getLogger().warning("Failed to give special material " + materialId + " to " + player.getName() + ": " + e.getMessage());
            return false;
        }
    }

    // Inner classes for data structure
    private static class SpecialMaterial {
        private final String id;
        private final ItemStack item;
        private final double dropChance;
        private final List<String> sourceBlocks;
        private final int minAmount;
        private final int maxAmount;
        private final SpecialMaterialEffects effects;

        public SpecialMaterial(String id, ItemStack item, double dropChance, List<String> sourceBlocks,
                               int minAmount, int maxAmount, SpecialMaterialEffects effects) {
            this.id = id;
            this.item = item;
            this.dropChance = dropChance;
            this.sourceBlocks = sourceBlocks;
            this.minAmount = minAmount;
            this.maxAmount = maxAmount;
            this.effects = effects;
        }

        public boolean canDropFrom(String blockKey) {
            return sourceBlocks.contains(blockKey);
        }

        // Getters
        public String getId() {
            return id;
        }

        public ItemStack getItem() {
            return item;
        }

        public double getDropChance() {
            return dropChance;
        }

        public List<String> getSourceBlocks() {
            return sourceBlocks;
        }

        public int getMinAmount() {
            return minAmount;
        }

        public int getMaxAmount() {
            return maxAmount;
        }

        public SpecialMaterialEffects getEffects() {
            return effects;
        }
    }

    private static class SpecialMaterialEffects {
        private final SpecialMaterialSound sound;
        private final SpecialMaterialParticle particle;

        public SpecialMaterialEffects(SpecialMaterialSound sound, SpecialMaterialParticle particle) {
            this.sound = sound;
            this.particle = particle;
        }

        public SpecialMaterialSound getSound() {
            return sound;
        }

        public SpecialMaterialParticle getParticle() {
            return particle;
        }
    }

    private static class SpecialMaterialSound {
        private final String name;
        private final float volume;
        private final float pitch;

        public SpecialMaterialSound(String name, float volume, float pitch) {
            this.name = name;
            this.volume = volume;
            this.pitch = pitch;
        }

        public String getName() {
            return name;
        }

        public float getVolume() {
            return volume;
        }

        public float getPitch() {
            return pitch;
        }
    }

    private static class SpecialMaterialParticle {
        private final String type;
        private final int count;
        private final double speed;
        private final String animation;
        private final double radius;

        public SpecialMaterialParticle(String type, int count, double speed, String animation, double radius) {
            this.type = type;
            this.count = count;
            this.speed = speed;
            this.animation = animation;
            this.radius = radius;
        }

        public String getType() {
            return type;
        }

        public int getCount() {
            return count;
        }

        public double getSpeed() {
            return speed;
        }

        public String getAnimation() {
            return animation;
        }

        public double getRadius() {
            return radius;
        }
    }
}
