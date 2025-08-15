package net.danh.storage.Manager;

import net.danh.storage.Utils.EnchantUtils;
import net.danh.storage.Utils.File;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;

import java.util.*;

public class EnchantManager {

    private static final Map<String, EnchantData> enchants = new HashMap<>();
    private static final int MAX_ALLOWED_LEVEL = 255;

    public static void loadEnchants() {
        enchants.clear();

        ConfigurationSection enchantsSection = File.getEnchantsConfig().getConfigurationSection("enchants");
        if (enchantsSection == null) return;

        for (String enchantKey : enchantsSection.getKeys(false)) {
            ConfigurationSection enchantSection = enchantsSection.getConfigurationSection(enchantKey);
            if (enchantSection == null) continue;

            EnchantData enchantData = new EnchantData();
            enchantData.key = enchantKey;
            enchantData.name = enchantSection.getString("name", enchantKey);
            enchantData.description = enchantSection.getString("description", "");
            enchantData.maxLevel = Math.min(enchantSection.getInt("max_level", 1), MAX_ALLOWED_LEVEL);
            enchantData.applicableItems = enchantSection.getStringList("applicable_items");

            ConfigurationSection levelsSection = enchantSection.getConfigurationSection("levels");
            if (levelsSection != null) {
                for (String levelKey : levelsSection.getKeys(false)) {
                    // Support range format (e.g., "1-10") and individual levels
                    if (levelKey.contains("-")) {
                        parseRangeLevel(levelKey, levelsSection, enchantData);
                    } else {
                        parseIndividualLevel(levelKey, levelsSection, enchantData);
                    }
                }
            }

            ConfigurationSection settingsSection = enchantSection.getConfigurationSection("settings");
            if (settingsSection != null) {
                enchantData.breakBlocks = settingsSection.getBoolean("break_blocks", true);
                enchantData.damageEntities = settingsSection.getBoolean("damage_entities", false);
                enchantData.createFire = settingsSection.getBoolean("create_fire", false);
                enchantData.storageIntegration = settingsSection.getBoolean("storage_integration", true);
                enchantData.glowEffect = settingsSection.getBoolean("glow_effect", true);
                enchantData.showInLore = settingsSection.getBoolean("show_in_lore", true);
                enchantData.loreFormat = settingsSection.getString("lore_format", "&6%name% &7Level %level%");
            }

            ConfigurationSection particlesSection = enchantSection.getConfigurationSection("particles");
            if (particlesSection != null) {
                enchantData.particlesEnabled = particlesSection.getBoolean("enabled", true);
                enchantData.particleType = particlesSection.getString("type", "EXPLOSION_LARGE");
                enchantData.particleCount = particlesSection.getInt("count", 10);
                enchantData.particleOffsetX = particlesSection.getDouble("offset_x", 1.0);
                enchantData.particleOffsetY = particlesSection.getDouble("offset_y", 1.0);
                enchantData.particleOffsetZ = particlesSection.getDouble("offset_z", 1.0);
                enchantData.particleExtra = particlesSection.getDouble("extra", 0.1);
                enchantData.particleDelayTicks = particlesSection.getInt("delay_ticks", 0);
            }

            ConfigurationSection soundsSection = enchantSection.getConfigurationSection("sounds");
            if (soundsSection != null) {
                enchantData.soundsEnabled = soundsSection.getBoolean("enabled", true);
                enchantData.explosionSound = soundsSection.getString("explosion_sound", "ENTITY_GENERIC_EXPLODE");
                enchantData.soundVolume = (float) soundsSection.getDouble("volume", 1.0);
                enchantData.soundPitch = (float) soundsSection.getDouble("pitch", 1.0);
                enchantData.soundDelayTicks = soundsSection.getInt("delay_ticks", 0);
            }

            // Generate missing levels if needed
            generateMissingLevels(enchantData);

            enchants.put(enchantKey, enchantData);
        }
    }

    public static boolean hasEnchant(ItemStack item, String enchantName) {
        return EnchantUtils.hasCustomEnchant(item, enchantName);
    }

    public static int getEnchantLevel(ItemStack item, String enchantName) {
        return EnchantUtils.getCustomEnchantLevel(item, enchantName);
    }

    public static ItemStack addEnchant(ItemStack item, String enchantName, int level) {
        if (!isValidEnchant(enchantName)) return item;
        if (!isValidLevel(enchantName, level)) return item;
        if (!isApplicableItem(item, enchantName)) return item;

        return EnchantUtils.addCustomEnchant(item, enchantName, level);
    }

    public static ItemStack removeEnchant(ItemStack item, String enchantName) {
        return EnchantUtils.removeCustomEnchant(item, enchantName);
    }

    public static boolean isValidEnchant(String enchantName) {
        return enchants.containsKey(enchantName);
    }

    public static boolean isValidLevel(String enchantName, int level) {
        EnchantData enchantData = enchants.get(enchantName);
        return enchantData != null && level >= 1 && level <= enchantData.maxLevel;
    }

    public static boolean isApplicableItem(ItemStack item, String enchantName) {
        if (item == null) return false;

        EnchantData enchantData = enchants.get(enchantName);
        if (enchantData == null) return false;

        String itemType = item.getType().name();
        return enchantData.applicableItems.contains(itemType);
    }

    public static EnchantData getEnchantData(String enchantName) {
        return enchants.get(enchantName);
    }

    public static Set<String> getAvailableEnchants() {
        return enchants.keySet();
    }

    public static String getEnchantDisplayName(String enchantName) {
        EnchantData enchantData = enchants.get(enchantName);
        return enchantData != null ? enchantData.name : null;
    }

    public static String getEnchantLoreFormat(String enchantName, int level) {
        EnchantData enchantData = enchants.get(enchantName);
        if (enchantData == null || !enchantData.showInLore) return null;

        return enchantData.loreFormat
                .replace("%name%", enchantData.name)
                .replace("%level%", String.valueOf(level));
    }

    public static boolean updateEnchantMaxLevel(String enchantName, int newMaxLevel) {
        if (!isValidEnchant(enchantName) || newMaxLevel < 1 || newMaxLevel > MAX_ALLOWED_LEVEL) {
            return false;
        }

        EnchantData enchantData = enchants.get(enchantName);
        enchantData.maxLevel = newMaxLevel;

        // Regenerate levels
        generateMissingLevels(enchantData);

        // Update config file
        ConfigurationSection enchantSection = File.getEnchantsConfig().getConfigurationSection("enchants." + enchantName);
        if (enchantSection != null) {
            enchantSection.set("max_level", newMaxLevel);

            // Update levels section in config using efficient range format
            ConfigurationSection levelsSection = enchantSection.getConfigurationSection("levels");
            if (levelsSection != null) {
                // Clear existing levels
                for (String key : levelsSection.getKeys(false)) {
                    levelsSection.set(key, null);
                }

                // Save levels using 1-10 range format to minimize config size
                saveRangeFormat(levelsSection, enchantName, newMaxLevel);
            }

            File.getFileSetting().save("enchants.yml");
        }

        return true;
    }

    // Helper methods for range format support
    private static void parseRangeLevel(String levelKey, ConfigurationSection levelsSection, EnchantData enchantData) {
        try {
            String[] parts = levelKey.split("-");
            if (parts.length != 2) return;

            int startLevel = Integer.parseInt(parts[0].trim());
            int endLevel = Integer.parseInt(parts[1].trim());

            if (startLevel > endLevel || startLevel < 1) return;

            ConfigurationSection rangeSection = levelsSection.getConfigurationSection(levelKey);
            if (rangeSection == null) return;

            // Load base configuration from the range
            EnchantLevelData baseData = new EnchantLevelData();
            loadLevelDataFromConfig(rangeSection, baseData);

            // Generate data for each level in range using scaling
            for (int level = startLevel; level <= endLevel; level++) {
                EnchantLevelData scaledData = scaleDataForLevel(enchantData.key, baseData, level, startLevel, endLevel);
                enchantData.levels.put(level, scaledData);
            }
        } catch (NumberFormatException e) {
            // Invalid range format, skip silently
        }
    }

    private static void parseIndividualLevel(String levelKey, ConfigurationSection levelsSection, EnchantData enchantData) {
        try {
            int level = Integer.parseInt(levelKey);
            ConfigurationSection levelSection = levelsSection.getConfigurationSection(levelKey);
            if (levelSection != null) {
                EnchantLevelData levelData = new EnchantLevelData();
                loadLevelDataFromConfig(levelSection, levelData);
                enchantData.levels.put(level, levelData);
            }
        } catch (NumberFormatException e) {
            // Invalid level format, skip silently
        }
    }

    private static void loadLevelDataFromConfig(ConfigurationSection section, EnchantLevelData levelData) {
        levelData.explosionPower = section.getDouble("explosion_power", levelData.explosionPower);
        levelData.cooldownTicks = section.getInt("cooldown_ticks", levelData.cooldownTicks);
        levelData.radius = section.getInt("radius", levelData.radius);
        levelData.hasteLevel = section.getDouble("haste_level", levelData.hasteLevel);
        levelData.hasteDuration = section.getInt("haste_duration", levelData.hasteDuration);
        levelData.multiplierValue = section.getDouble("multiplier_value", levelData.multiplierValue);
        levelData.maxBlocks = section.getDouble("max_blocks", levelData.maxBlocks);
    }

    private static EnchantLevelData scaleDataForLevel(String enchantType, EnchantLevelData baseData, int currentLevel, int startLevel, int endLevel) {
        EnchantLevelData scaledData = new EnchantLevelData();
        double progress = endLevel > startLevel ? (double) (currentLevel - startLevel) / (endLevel - startLevel) : 0.0;

        switch (enchantType.toLowerCase()) {
            case "tnt":
                scaledData.explosionPower = baseData.explosionPower + progress * 2.0;
                scaledData.cooldownTicks = Math.max(10, (int) (baseData.cooldownTicks - progress * 50));
                scaledData.radius = baseData.radius + (int) (progress * (endLevel - startLevel));
                break;
            case "haste":
                scaledData.hasteLevel = Math.min(5, baseData.hasteLevel + progress * 2.0);
                scaledData.hasteDuration = (int) (baseData.hasteDuration + progress * 1200);
                scaledData.cooldownTicks = 0;
                break;
            case "multiplier":
                scaledData.multiplierValue = baseData.multiplierValue + progress * (endLevel - startLevel);
                scaledData.cooldownTicks = 0;
                break;
            case "veinminer":
                scaledData.maxBlocks = Math.min(64.0, baseData.maxBlocks + progress * 32.0);
                scaledData.cooldownTicks = Math.max(10, (int) (baseData.cooldownTicks - progress * 30));
                break;
            default:
                scaledData.explosionPower = baseData.explosionPower;
                scaledData.cooldownTicks = Math.max(10, (int) (baseData.cooldownTicks - progress * 20));
                scaledData.radius = baseData.radius;
                scaledData.hasteLevel = baseData.hasteLevel;
                scaledData.hasteDuration = baseData.hasteDuration;
                scaledData.multiplierValue = baseData.multiplierValue;
                scaledData.maxBlocks = baseData.maxBlocks;
                break;
        }
        return scaledData;
    }

    private static void saveRangeFormat(ConfigurationSection levelsSection, String enchantName, int maxLevel) {
        // Use 10-level ranges for optimal config organization (1-10, 11-20, etc.)
        int rangeSize = 10;

        for (int start = 1; start <= maxLevel; start += rangeSize) {
            int end = Math.min(start + rangeSize - 1, maxLevel);
            String rangeKey = (start == end) ? String.valueOf(start) : start + "-" + end;

            ConfigurationSection rangeSection = levelsSection.createSection(rangeKey);
            EnchantLevelData baseData = generateLevelData(enchantName, start);
            saveLevelDataToConfig(rangeSection, enchantName, baseData);
        }
    }

    private static void generateMissingLevels(EnchantData enchantData) {
        for (int level = 1; level <= enchantData.maxLevel; level++) {
            if (!enchantData.levels.containsKey(level)) {
                EnchantLevelData levelData = generateLevelData(enchantData.key, level);
                enchantData.levels.put(level, levelData);
            }
        }
    }

    private static EnchantLevelData generateLevelData(String enchantType, int level) {
        EnchantLevelData levelData = new EnchantLevelData();

        switch (enchantType.toLowerCase()) {
            case "tnt":
                levelData.explosionPower = 2.0 + (level - 1) * 0.5;
                levelData.cooldownTicks = Math.max(10, 60 - (level - 1) * 10);
                levelData.radius = 3 + (level - 1);
                break;

            case "haste":
                // Improved scaling: Level 1-2 = Haste I, Level 3-4 = Haste II, Level 5 = Haste III
                levelData.hasteLevel = Math.min(3, (level + 1) / 2);
                // Duration scaling: 30 seconds base + 15 seconds per level
                levelData.hasteDuration = 600 + (level - 1) * 300; // 30s + 15s per level
                levelData.cooldownTicks = 0;
                break;

            case "multiplier":
                levelData.multiplierValue = 2.0 + (level - 1);
                levelData.cooldownTicks = 0;
                break;

            case "veinminer":
                levelData.maxBlocks = Math.min(64.0, 8.0 * Math.pow(2, level - 1));
                levelData.cooldownTicks = Math.max(10, 40 - (level - 1) * 10);
                break;

            case "test_enchant":
                levelData.cooldownTicks = Math.max(10, 30 - (level - 1) * 10);
                break;

            default:
                // Default scaling for unknown enchants
                levelData.cooldownTicks = Math.max(10, 60 - (level - 1) * 5);
                break;
        }

        return levelData;
    }

    private static void saveLevelDataToConfig(ConfigurationSection levelSection, String enchantType, EnchantLevelData levelData) {
        switch (enchantType.toLowerCase()) {
            case "tnt":
                levelSection.set("explosion_power", levelData.explosionPower);
                levelSection.set("cooldown_ticks", levelData.cooldownTicks);
                levelSection.set("radius", levelData.radius);
                break;

            case "haste":
                levelSection.set("haste_level", levelData.hasteLevel);
                levelSection.set("haste_duration", levelData.hasteDuration);
                levelSection.set("cooldown_ticks", levelData.cooldownTicks);
                break;

            case "multiplier":
                levelSection.set("multiplier_value", levelData.multiplierValue);
                levelSection.set("cooldown_ticks", levelData.cooldownTicks);
                break;

            case "veinminer":
                levelSection.set("max_blocks", levelData.maxBlocks);
                levelSection.set("cooldown_ticks", levelData.cooldownTicks);
                break;

            case "test_enchant":
                levelSection.set("cooldown_ticks", levelData.cooldownTicks);
                break;

            default:
                levelSection.set("cooldown_ticks", levelData.cooldownTicks);
                break;
        }
    }

    public static int getMaxAllowedLevel() {
        return MAX_ALLOWED_LEVEL;
    }

    public static class EnchantData {
        public String key;
        public String name;
        public String description;
        public int maxLevel;
        public List<String> applicableItems = new ArrayList<>();
        public Map<Integer, EnchantLevelData> levels = new HashMap<>();
        public boolean breakBlocks = true;
        public boolean damageEntities = false;
        public boolean createFire = false;
        public boolean storageIntegration = true;
        public boolean glowEffect = true;
        public boolean showInLore = true;
        public String loreFormat = "&6%name% &7Level %level%";

        public boolean particlesEnabled = true;
        public String particleType = "EXPLOSION_LARGE";
        public int particleCount = 10;
        public double particleOffsetX = 1.0;
        public double particleOffsetY = 1.0;
        public double particleOffsetZ = 1.0;
        public double particleExtra = 0.1;
        public int particleDelayTicks = 0;

        public boolean soundsEnabled = true;
        public String explosionSound = "ENTITY_GENERIC_EXPLODE";
        public float soundVolume = 1.0f;
        public float soundPitch = 1.0f;
        public int soundDelayTicks = 0;
    }

    public static class EnchantLevelData {
        // Common fields
        public double explosionPower = 2.0; // Used by TNT enchant
        public int cooldownTicks = 60; // Used by all enchants
        public int radius = 3; // Used by TNT enchant only

        // Specific fields for different enchants
        public double hasteLevel = 1.0; // Used by Haste enchant
        public int hasteDuration = 600; // Used by Haste enchant - duration in ticks
        public double multiplierValue = 2.0; // Used by Multiplier enchant
        public double maxBlocks = 8.0; // Used by Vein Miner enchant
    }
}
