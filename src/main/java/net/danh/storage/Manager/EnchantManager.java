package net.danh.storage.Manager;

import net.danh.storage.Utils.EnchantUtils;
import net.danh.storage.Utils.File;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;

import java.util.*;

public class EnchantManager {

    private static final Map<String, EnchantData> enchants = new HashMap<>();

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
            enchantData.maxLevel = enchantSection.getInt("max_level", 1);
            enchantData.applicableItems = enchantSection.getStringList("applicable_items");

            ConfigurationSection levelsSection = enchantSection.getConfigurationSection("levels");
            if (levelsSection != null) {
                for (String levelKey : levelsSection.getKeys(false)) {
                    int level = Integer.parseInt(levelKey);
                    ConfigurationSection levelSection = levelsSection.getConfigurationSection(levelKey);
                    if (levelSection != null) {
                        EnchantLevelData levelData = new EnchantLevelData();
                        levelData.explosionPower = levelSection.getDouble("explosion_power", 2.0);
                        levelData.cooldownTicks = levelSection.getInt("cooldown_ticks", 60);
                        levelData.radius = levelSection.getInt("radius", 3);
                        enchantData.levels.put(level, levelData);
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
        public double explosionPower = 2.0;
        public int cooldownTicks = 60;
        public int radius = 3;
    }
}
