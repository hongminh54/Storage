package net.danh.storage.Manager.SpecialMaterial;

import com.cryptomorin.xseries.XSound;
import com.cryptomorin.xseries.messages.ActionBar;
import com.cryptomorin.xseries.messages.Titles;
import net.danh.storage.Database.PlayerData;
import net.danh.storage.Enchant.MultiplierEnchant;
import net.danh.storage.Manager.Crop.CropStorageManager;
import net.danh.storage.Manager.EnchantManager;
import net.danh.storage.Manager.Mob.MobStorageManager;
import net.danh.storage.Manager.ParticleManager;
import net.danh.storage.NMS.NMSAssistant;
import net.danh.storage.Storage;
import net.danh.storage.Utils.ChatUtils;
import net.danh.storage.Utils.File;
import net.danh.storage.Utils.MaterialUtils;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.inventory.meta.ItemMeta;

import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

public class SpecialMaterialManager {

    public static final String SOURCE_BLOCK = "block";
    public static final String SOURCE_CROP = "crop";
    public static final String SOURCE_MOB = "mob";

    private static final String MODE_DROP = "DROP";
    private static final String MODE_STORAGE_REWARD = "STORAGE_REWARD";
    private static final String DAILY_PREFIX = "spmat:";

    private static final Map<String, SpecialMaterial> specialMaterials = new HashMap<>();
    private static final Map<UUID, Map<String, Long>> cooldowns = new ConcurrentHashMap<>();
    private static final Map<UUID, Map<String, DailyCounter>> dailyLimits = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> lastBlockedMessageAt = new ConcurrentHashMap<>();
    private static final long BLOCKED_MESSAGE_INTERVAL_MS = 5000L;
    private static boolean systemEnabled = false;

    public static String getMaterialDisplayName(String materialId) {
        SpecialMaterial material = specialMaterials.get(materialId);
        return material != null ? getDisplayName(material) : materialId;
    }

    private static String getDisplayName(SpecialMaterial material) {
        ItemMeta meta = material.item().getItemMeta();
        return meta != null && meta.hasDisplayName() ? meta.getDisplayName() : material.id();
    }

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

        ItemStack item = loadItem(section.getConfigurationSection("item"));
        if (item == null) return null;

        double dropChance = section.getDouble("drop_chance", 0.0);
        Map<String, Double> dropChanceOverrides = new HashMap<>();
        ConfigurationSection chanceSection = section.getConfigurationSection("drop_chance");
        if (chanceSection != null) {
            for (String key : chanceSection.getKeys(false)) {
                dropChanceOverrides.put(key.toLowerCase(Locale.ENGLISH), chanceSection.getDouble(key));
            }
        }

        List<String> sourceBlocks = normalizeKeys(section.getStringList("source_blocks"));
        List<String> sourceCrops = normalizeKeys(section.getStringList("source_crops"));
        List<String> sourceMobs = normalizeKeys(section.getStringList("source_mobs"));

        int minAmount = Math.max(1, section.getInt("amount.min", 1));
        int maxAmount = Math.max(minAmount, section.getInt("amount.max", minAmount));

        long cooldownSeconds = Math.max(0, section.getLong("cooldown_seconds", 0));
        int dailyLimit = Math.max(0, section.getInt("daily_limit", 0));
        boolean requireStored = section.getBoolean("require_stored", true);
        boolean requireAutoPickup = section.getBoolean("require_auto_pickup", false);
        String permission = section.getString("permission", null);
        List<String> worldBlacklist = normalizeKeys(section.getStringList("world_blacklist"));
        List<String> worldWhitelist = normalizeKeys(section.getStringList("world_whitelist"));
        List<String> commands = section.getStringList("commands");
        String mode = section.getString("mode", MODE_DROP).toUpperCase(Locale.ENGLISH);

        return new SpecialMaterial(id, item, dropChance, dropChanceOverrides, sourceBlocks, sourceCrops,
                sourceMobs, minAmount, maxAmount, loadEffects(section.getConfigurationSection("effects")),
                loadLootTable(section.getConfigurationSection("loot_table")),
                loadReward(section.getConfigurationSection("rewards")), cooldownSeconds, dailyLimit,
                requireStored, requireAutoPickup, permission, worldBlacklist, worldWhitelist, commands, mode);
    }

    private static ItemStack loadItem(ConfigurationSection itemSection) {
        if (itemSection == null) return null;

        String materialName = itemSection.getString("material");
        if (materialName == null) return null;

        ItemStack item = MaterialUtils.createItem(materialName);
        if (item == null) {
            Storage.getStorage().getLogger().warning("Invalid material for special material: " + materialName);
            return null;
        }

        ItemMeta meta = item.getItemMeta();
        if (meta == null) return null;

        String name = itemSection.getString("name");
        if (name != null) {
            meta.setDisplayName(ChatUtils.colorizewp(name));
        }

        List<String> lore = itemSection.getStringList("lore");
        if (!lore.isEmpty()) {
            meta.setLore(ChatUtils.colorizewp(lore));
        }

        if (itemSection.contains("custom_model_data")) {
            int customModelData = itemSection.getInt("custom_model_data");
            if (new NMSAssistant().isVersionGreaterThanOrEqualTo(14)) {
                meta.setCustomModelData(customModelData);
            }
        }

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
                    } else if (apply) {
                        try {
                            meta.addItemFlags(ItemFlag.valueOf(flagName));
                        } catch (IllegalArgumentException e) {
                            Storage.getStorage().getLogger().warning("Invalid item flag: " + flagName);
                        }
                    }
                }
            }
        }

        boolean glow = itemSection.getBoolean("glow", false);
        if (glow) {
            Enchantment durabilityEnchant = Enchantment.getByName("UNBREAKING");
            if (durabilityEnchant == null) {
                durabilityEnchant = Enchantment.getByName("DURABILITY");
            }
            meta.addEnchant(durabilityEnchant, 1, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        }

        item.setItemMeta(meta);
        return item;
    }

    private static SpecialMaterialEffects loadEffects(ConfigurationSection section) {
        if (section == null) return null;

        ConfigurationSection soundSection = section.getConfigurationSection("sound");
        SpecialMaterialSound sound = null;
        if (soundSection != null && soundSection.getBoolean("enabled", false)) {
            sound = new SpecialMaterialSound(soundSection.getString("name"),
                    (float) soundSection.getDouble("volume", 1.0), (float) soundSection.getDouble("pitch", 1.0));
        }

        ConfigurationSection particleSection = section.getConfigurationSection("particles");
        SpecialMaterialParticle particle = null;
        if (particleSection != null && particleSection.getBoolean("enabled", false)) {
            particle = new SpecialMaterialParticle(particleSection.getString("type"),
                    particleSection.getInt("count", 10), particleSection.getDouble("speed", 0.1),
                    particleSection.getString("animation", "burst"), particleSection.getDouble("radius", 1.5));
        }

        boolean lightning = section.getBoolean("lightning", false);

        ConfigurationSection fireworkSection = section.getConfigurationSection("firework");
        SpecialMaterialFirework firework = null;
        if (fireworkSection != null && fireworkSection.getBoolean("enabled", false)) {
            List<Color> colors = new ArrayList<>();
            for (String hex : fireworkSection.getStringList("colors")) {
                try {
                    colors.add(Color.fromRGB(Integer.parseInt(hex.replace("#", ""), 16)));
                } catch (NumberFormatException ignored) {
                }
            }
            firework = new SpecialMaterialFirework(Math.max(0, fireworkSection.getInt("power", 1)),
                    colors, fireworkSection.getBoolean("flicker", false), fireworkSection.getBoolean("trail", false));
        }

        ConfigurationSection titleSection = section.getConfigurationSection("title");
        SpecialMaterialTitle title = null;
        if (titleSection != null && titleSection.getBoolean("enabled", false)) {
            title = new SpecialMaterialTitle(titleSection.getString("title", ""), titleSection.getString("subtitle", ""));
        }

        String actionbar = null;
        ConfigurationSection actionbarSection = section.getConfigurationSection("actionbar");
        if (actionbarSection != null && actionbarSection.getBoolean("enabled", false)) {
            actionbar = actionbarSection.getString("message");
        }

        return new SpecialMaterialEffects(sound, particle, lightning, firework, title, actionbar);
    }

    private static List<LootEntry> loadLootTable(ConfigurationSection section) {
        if (section == null) return null;

        List<LootEntry> entries = new ArrayList<>();
        for (String key : section.getKeys(false)) {
            ConfigurationSection entrySection = section.getConfigurationSection(key);
            if (entrySection == null) continue;

            ItemStack item = loadItem(entrySection.getConfigurationSection("item"));
            if (item == null) continue;

            int min = Math.max(1, entrySection.getInt("min", 1));
            int max = Math.max(min, entrySection.getInt("max", min));
            int weight = Math.max(1, entrySection.getInt("weight", 1));
            entries.add(new LootEntry(item, weight, min, max));
        }
        return entries.isEmpty() ? null : entries;
    }

    private static SpecialMaterialReward loadReward(ConfigurationSection section) {
        if (section == null) return null;

        Map<String, RewardEntry> rewards = new HashMap<>();
        for (String sourceType : section.getKeys(false)) {
            ConfigurationSection rewardSection = section.getConfigurationSection(sourceType);
            if (rewardSection == null) continue;

            String item = rewardSection.getString("item");
            if (item == null || item.trim().isEmpty()) continue;

            rewards.put(sourceType.toLowerCase(Locale.ENGLISH),
                    new RewardEntry(item.toUpperCase(Locale.ENGLISH), Math.max(1, rewardSection.getInt("amount", 1))));
        }
        return rewards.isEmpty() ? null : new SpecialMaterialReward(rewards);
    }

    private static List<String> normalizeKeys(List<String> keys) {
        List<String> normalized = new ArrayList<>();
        if (keys == null) return normalized;
        for (String key : keys) {
            if (key != null && !key.trim().isEmpty()) {
                normalized.add(key.trim().toUpperCase(Locale.ENGLISH));
            }
        }
        return normalized;
    }

    public static void checkSpecialMaterialDrop(Player player, Block block) {
        checkSpecialMaterialDrop(player, block, null);
    }

    public static void checkSpecialMaterialDrop(Player player, Block block, String enchantType) {
        if (!systemEnabled || specialMaterials.isEmpty()) return;

        String blockKey = getBlockKey(block);
        for (SpecialMaterial material : specialMaterials.values()) {
            if (material.canDropFrom(SOURCE_BLOCK, blockKey)) {
                rollDrop(player, block.getLocation(), material, SOURCE_BLOCK, enchantType, true);
            }
        }
    }

    public static void checkSpecialMaterialDrop(Player player, String sourceType, String sourceKey, Location location, boolean stored) {
        if (!systemEnabled || specialMaterials.isEmpty() || location == null) return;

        if (SOURCE_CROP.equals(sourceType)) {
            for (SpecialMaterial material : specialMaterials.values()) {
                if (material.canDropFrom(SOURCE_CROP, sourceKey)) {
                    rollDrop(player, location, material, SOURCE_CROP, null, stored);
                }
            }
        } else if (SOURCE_MOB.equals(sourceType)) {
            for (SpecialMaterial material : specialMaterials.values()) {
                for (String key : MobStorageManager.getMobLookupKeys(sourceKey)) {
                    if (material.canDropFrom(SOURCE_MOB, key)) {
                        rollDrop(player, location, material, SOURCE_MOB, null, stored);
                        break;
                    }
                }
            }
        }
    }

    private static void rollDrop(Player player, Location location, SpecialMaterial material, String sourceType,
                                 String enchantType, boolean stored) {
        if (material.requireStored() && !stored) return;
        if (material.requireAutoPickup() && !isAutoPickupEnabled(player, sourceType)) return;
        if (material.permission() != null && !material.permission().isEmpty()
                && !player.hasPermission(material.permission())) return;
        if (!isWorldAllowed(player, material)) return;

        double dropChance = applyEnchantModifier(material.dropChanceFor(sourceType), enchantType);
        dropChance = applySourceBonus(dropChance, sourceType);

        if (ThreadLocalRandom.current().nextDouble(100.0) > dropChance) return;

        if (!isCooldownReady(player, material)) {
            sendCooldownMessage(player, material);
            return;
        }
        if (!hasDailyLimitLeft(player, material)) {
            sendDailyLimitMessage(player, material);
            return;
        }

        RewardEntry reward = material.reward() != null ? material.reward().rewardFor(sourceType) : null;
        boolean storageReward = MODE_STORAGE_REWARD.equals(material.mode()) && reward != null;
        LootEntry entry = storageReward ? null : material.rollLoot();
        int amount = entry != null
                ? ThreadLocalRandom.current().nextInt(entry.min(), entry.max() + 1)
                : ThreadLocalRandom.current().nextInt(material.minAmount(), material.maxAmount() + 1);
        amount = Math.max(1, amount);

        if (storageReward) {
            if (!giveStorageReward(player, sourceType, reward)) return;
        } else {
            amount = applyMultiplier(player, amount);
            if (entry != null) {
                dropItemStack(location, entry.item(), amount);
            } else {
                for (int i = 0; i < amount; i++) {
                    location.getWorld().dropItemNaturally(location, material.item().clone());
                }
            }
        }

        markCooldown(player, material);
        incrementDailyLimit(player, material);

        if (material.effects() != null) {
            playEffects(player, location, material.effects(), material);
        }
        runCommands(player, material, amount);
        sendFoundMessage(player, material, sourceType);
    }

    private static boolean giveStorageReward(Player player, String sourceType, RewardEntry reward) {
        if (SOURCE_CROP.equals(sourceType)) {
            return CropStorageManager.isConfiguredDrop(reward.item())
                    && CropStorageManager.addItemAmount(player, reward.item(), reward.amount());
        }
        if (SOURCE_MOB.equals(sourceType)) {
            return MobStorageManager.isConfiguredDrop(reward.item())
                    && MobStorageManager.addItemAmount(player, reward.item(), reward.amount());
        }
        return false;
    }

    private static int applyMultiplier(Player player, int amount) {
        ItemStack hand = player.getInventory().getItemInMainHand();
        if (hand != null && !hand.getType().name().equals("AIR") && hand.getAmount() > 0
                && EnchantManager.hasEnchant(hand, "multiplier")) {
            int multiplierLevel = EnchantManager.getEnchantLevel(hand, "multiplier");
            return MultiplierEnchant.calculateMultipliedAmount(player, amount, multiplierLevel);
        }
        return amount;
    }

    private static void dropItemStack(Location location, ItemStack item, int amount) {
        while (amount > 0) {
            int stackSize = Math.min(amount, item.getMaxStackSize());
            ItemStack drop = item.clone();
            drop.setAmount(stackSize);
            location.getWorld().dropItemNaturally(location, drop);
            amount -= stackSize;
        }
    }

    private static boolean isAutoPickupEnabled(Player player, String sourceType) {
        if (SOURCE_CROP.equals(sourceType)) return CropStorageManager.getToggleStatus(player);
        if (SOURCE_MOB.equals(sourceType)) return MobStorageManager.getToggleStatus(player);
        return true;
    }

    private static boolean isWorldAllowed(Player player, SpecialMaterial material) {
        String world = player.getWorld().getName();
        if (material.worldBlacklist().contains(world)) return false;
        return material.worldWhitelist().isEmpty() || material.worldWhitelist().contains(world);
    }

    private static boolean isCooldownReady(Player player, SpecialMaterial material) {
        if (material.cooldownSeconds() <= 0) return true;
        Map<String, Long> playerCooldowns = cooldowns.get(player.getUniqueId());
        if (playerCooldowns == null) return true;
        Long last = playerCooldowns.get(material.id());
        return last == null || System.currentTimeMillis() - last >= material.cooldownSeconds() * 1000L;
    }

    private static long getCooldownRemaining(Player player, SpecialMaterial material) {
        Map<String, Long> playerCooldowns = cooldowns.get(player.getUniqueId());
        if (playerCooldowns == null) return 0;
        Long last = playerCooldowns.get(material.id());
        if (last == null) return 0;
        return Math.max(0, material.cooldownSeconds() * 1000L - (System.currentTimeMillis() - last));
    }

    private static void markCooldown(Player player, SpecialMaterial material) {
        if (material.cooldownSeconds() <= 0) return;
        cooldowns.computeIfAbsent(player.getUniqueId(), key -> new ConcurrentHashMap<>())
                .put(material.id(), System.currentTimeMillis());
    }

    private static boolean hasDailyLimitLeft(Player player, SpecialMaterial material) {
        if (material.dailyLimit() <= 0) return true;
        Map<String, DailyCounter> playerLimits = dailyLimits
                .computeIfAbsent(player.getUniqueId(), key -> loadDailyLimits(player));
        DailyCounter counter = playerLimits.get(material.id());
        if (counter == null) return true;
        return !LocalDate.now().toString().equals(counter.date()) || counter.count() < material.dailyLimit();
    }

    private static Map<String, DailyCounter> loadDailyLimits(Player player) {
        Map<String, DailyCounter> loaded = new ConcurrentHashMap<>();
        PlayerData data = Storage.dataStorage.getData(player.getName());
        if (data == null || data.data() == null || data.data().isEmpty()) return loaded;

        for (String part : data.data().split(";")) {
            if (!part.startsWith(DAILY_PREFIX)) continue;
            String[] tokens = part.split(":");
            if (tokens.length != 4) continue;
            try {
                loaded.put(tokens[1], new DailyCounter(Integer.parseInt(tokens[2]), tokens[3]));
            } catch (NumberFormatException ignored) {
            }
        }
        return loaded;
    }

    private static void incrementDailyLimit(Player player, SpecialMaterial material) {
        if (material.dailyLimit() <= 0) return;
        Map<String, DailyCounter> playerLimits = dailyLimits
                .computeIfAbsent(player.getUniqueId(), key -> loadDailyLimits(player));
        String today = LocalDate.now().toString();
        DailyCounter counter = playerLimits.get(material.id());
        if (counter == null || !today.equals(counter.date())) {
            playerLimits.put(material.id(), new DailyCounter(1, today));
        } else {
            playerLimits.put(material.id(), new DailyCounter(counter.count() + 1, counter.date()));
        }
        persistDailyLimits(player, playerLimits);
    }

    // Persist daily limits immediately after an increment
    private static void persistDailyLimits(Player player, Map<String, DailyCounter> playerLimits) {
        PlayerData existing = Storage.dataStorage.getData(player.getName());
        if (existing == null) return;
        Storage.dataStorage.updateTable(new PlayerData(player.getName(),
                mergeDailyLimitsData(existing.data(), playerLimits), existing.max(), existing.autoPickup()));
    }

    // Merge the spmat: entries into the existing data string, preserving all other entries
    private static String mergeDailyLimitsData(String existingData, Map<String, DailyCounter> playerLimits) {
        StringBuilder data = new StringBuilder();
        if (existingData != null) {
            for (String part : existingData.split(";")) {
                if (part.isEmpty() || part.startsWith(DAILY_PREFIX)) continue;
                if (data.length() > 0) data.append(";");
                data.append(part);
            }
        }
        for (Map.Entry<String, DailyCounter> entry : playerLimits.entrySet()) {
            if (data.length() > 0) data.append(";");
            data.append(DAILY_PREFIX).append(entry.getKey()).append(":").append(entry.getValue().count())
                    .append(":").append(entry.getValue().date());
        }
        return data.toString();
    }

    // Called on quit / autosave so MineManager's full overwrite can't wipe the daily limits
    public static void savePlayerData(Player player) {
        if (player == null) return;
        Map<String, DailyCounter> playerLimits = dailyLimits.get(player.getUniqueId());
        if (playerLimits == null || playerLimits.isEmpty()) return;

        PlayerData existing = Storage.dataStorage.getData(player.getName());
        if (existing == null) return;
        Storage.dataStorage.updateTable(new PlayerData(player.getName(),
                mergeDailyLimitsData(existing.data(), playerLimits), existing.max(), existing.autoPickup()));
    }

    public static void cleanupPlayerData(Player player) {
        if (player == null) return;
        cooldowns.remove(player.getUniqueId());
        dailyLimits.remove(player.getUniqueId());
        lastBlockedMessageAt.remove(player.getUniqueId());
    }

    private static void playEffects(Player player, Location location, SpecialMaterialEffects effects,
                                    SpecialMaterial material) {
        if (effects.sound() != null) {
            SpecialMaterialSound sound = effects.sound();
            try {
                XSound xSound = XSound.matchXSound(sound.name()).orElse(XSound.UI_BUTTON_CLICK);
                xSound.play(player, sound.volume(), sound.pitch());
            } catch (Exception e) {
                Storage.getStorage().getLogger().warning("Failed to play special material sound: " + sound.name());
            }
        }

        if (effects.particle() != null) {
            SpecialMaterialParticle particle = effects.particle();
            ParticleManager.playSpecialMaterialParticle(location, particle.type(), particle.count(),
                    particle.speed(), particle.animation(), particle.radius());
        }

        if (effects.lightning() && location.getWorld() != null) {
            location.getWorld().strikeLightningEffect(location);
        }

        if (effects.firework() != null) {
            playFirework(location, effects.firework());
        }

        if (effects.title() != null) {
            SpecialMaterialTitle title = effects.title();
            String displayName = getDisplayName(material);
            Titles.sendTitle(player,
                    ChatUtils.colorize(player, title.title().replace("#material#", displayName)),
                    ChatUtils.colorize(player, title.subtitle().replace("#material#", displayName)));
        }

        if (effects.actionbar() != null) {
            String displayName = getDisplayName(material);
            ActionBar.sendActionBar(Storage.getStorage(), player,
                    ChatUtils.colorize(player, effects.actionbar().replace("#material#", displayName)));
        }
    }

    private static void playFirework(Location location, SpecialMaterialFirework firework) {
        try {
            Firework entity = location.getWorld().spawn(location, Firework.class);
            FireworkMeta meta = entity.getFireworkMeta();
            List<Color> colors = firework.colors().isEmpty()
                    ? Arrays.asList(Color.WHITE, Color.AQUA, Color.PURPLE)
                    : firework.colors();
            FireworkEffect effect = FireworkEffect.builder()
                    .with(FireworkEffect.Type.BURST)
                    .withColor(colors)
                    .flicker(firework.flicker())
                    .trail(firework.trail())
                    .build();
            meta.addEffect(effect);
            meta.setPower(firework.power());
            entity.setFireworkMeta(meta);
        } catch (Exception e) {
            Storage.getStorage().getLogger().warning("Failed to spawn special material firework: " + e.getMessage());
        }
    }

    private static void runCommands(Player player, SpecialMaterial material, int amount) {
        if (material.commands().isEmpty()) return;
        for (String command : material.commands()) {
            String cmd = command.replace("#player#", player.getName())
                    .replace("#amount#", String.valueOf(amount))
                    .replace("#world#", player.getWorld().getName());
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), ChatUtils.colorize(player, cmd));
        }
    }

    private static void sendFoundMessage(Player player, SpecialMaterial material, String sourceType) {
        String message = File.getMessage().getString("special_material.found",
                "#prefix# &aYou found a special material: &e#material#!");
        String sourceLabel = File.getMessage().getString("special_material.sources." + sourceType, sourceType);
        message = message.replace("#source#", sourceLabel).replace("#material#", getDisplayName(material));
        player.sendMessage(ChatUtils.colorizewp(message.replace("#prefix#", File.getConfig().getString("prefix", ""))));
    }

    private static void sendCooldownMessage(Player player, SpecialMaterial material) {
        if (!shouldSendBlockedMessage(player)) return;
        String message = File.getMessage().getString("special_material.cooldown",
                "#prefix# &cThe #material# &cisn't ready yet! (&e#time#&c)");
        message = message.replace("#material#", getDisplayName(material))
                .replace("#time#", String.format("%ds", getCooldownRemaining(player, material) / 1000L));
        player.sendMessage(ChatUtils.colorizewp(message.replace("#prefix#", File.getConfig().getString("prefix", ""))));
    }

    private static void sendDailyLimitMessage(Player player, SpecialMaterial material) {
        if (!shouldSendBlockedMessage(player)) return;
        String message = File.getMessage().getString("special_material.daily_limit_reached",
                "#prefix# &cYou've reached the daily limit for #material#!");
        message = message.replace("#material#", getDisplayName(material));
        player.sendMessage(ChatUtils.colorizewp(message.replace("#prefix#", File.getConfig().getString("prefix", ""))));
    }

    // Limit blocked messages (cooldown / daily limit) to once per 5 seconds per player
    private static boolean shouldSendBlockedMessage(Player player) {
        long now = System.currentTimeMillis();
        Long last = lastBlockedMessageAt.get(player.getUniqueId());
        if (last != null && now - last < BLOCKED_MESSAGE_INTERVAL_MS) {
            return false;
        }
        lastBlockedMessageAt.put(player.getUniqueId(), now);
        return true;
    }

    private static double applyEnchantModifier(double baseDropChance, String enchantType) {
        if (enchantType == null) return baseDropChance;

        FileConfiguration config = File.getSpecialMaterialConfig();
        if (config == null) return baseDropChance;

        ConfigurationSection bonusSection = config.getConfigurationSection("enchant_bonuses." + enchantType);
        if (bonusSection == null || !bonusSection.getBoolean("enabled", false)) {
            return baseDropChance;
        }

        switch (enchantType.toLowerCase(Locale.ENGLISH)) {
            case "veinminer":
                return baseDropChance + bonusSection.getDouble("bonus_drop_chance", 0.0);
            case "tnt":
                return baseDropChance * bonusSection.getDouble("drop_chance_modifier", 1.0);
            default:
                return baseDropChance;
        }
    }

    private static double applySourceBonus(double baseDropChance, String sourceType) {
        if (sourceType == null || SOURCE_BLOCK.equals(sourceType)) return baseDropChance;

        FileConfiguration config = File.getSpecialMaterialConfig();
        if (config == null) return baseDropChance;

        ConfigurationSection bonusSection = config.getConfigurationSection("enchant_bonuses." + sourceType);
        if (bonusSection == null || !bonusSection.getBoolean("enabled", false)) {
            return baseDropChance;
        }
        return baseDropChance + bonusSection.getDouble("bonus_drop_chance", 0.0);
    }

    private static String getBlockKey(Block block) {
        NMSAssistant nms = new NMSAssistant();
        return block.getType().name() + ";" + (nms.isVersionLessThanOrEqualTo(12) ? block.getData() : "0");
    }

    public static int getLoadedMaterialsCount() {
        return specialMaterials.size();
    }

    public static Set<String> getLoadedMaterialIds() {
        return new HashSet<>(specialMaterials.keySet());
    }

    public static boolean hasMaterial(String materialId) {
        return specialMaterials.containsKey(materialId);
    }

    public static String getMaterialInfo(String materialId) {
        SpecialMaterial material = specialMaterials.get(materialId);
        if (material == null) return null;

        return String.format("ID: %s, Mode: %s, Drop Chance: %.2f%%, Sources: %d/%d/%d, Amount: %d-%d%s",
                material.id(), material.mode(), material.dropChance(),
                material.sourceBlocks().size(), material.sourceCrops().size(), material.sourceMobs().size(),
                material.minAmount(), material.maxAmount(),
                material.cooldownSeconds() > 0 || material.dailyLimit() > 0
                        ? ", Cooldown: " + material.cooldownSeconds() + "s, Daily Limit: " + material.dailyLimit()
                        : "");
    }

    public static boolean giveSpecialMaterial(Player player, String materialId, int amount) {
        if (player == null || !player.isOnline()) return false;

        SpecialMaterial material = specialMaterials.get(materialId);
        if (material == null) return false;

        try {
            for (int i = 0; i < amount; i++) {
                ItemStack item = material.item().clone();
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

    private record SpecialMaterial(String id, ItemStack item, double dropChance,
                                   Map<String, Double> dropChanceOverrides,
                                   List<String> sourceBlocks, List<String> sourceCrops, List<String> sourceMobs,
                                   int minAmount, int maxAmount, SpecialMaterialEffects effects,
                                   List<LootEntry> lootTable,
                                   SpecialMaterialReward reward, long cooldownSeconds, int dailyLimit,
                                   boolean requireStored, boolean requireAutoPickup, String permission,
                                   List<String> worldBlacklist, List<String> worldWhitelist,
                                   List<String> commands, String mode) {

        public boolean canDropFrom(String sourceType, String sourceKey) {
            if (sourceKey == null) return false;
            return switch (sourceType) {
                case SOURCE_BLOCK -> sourceBlocks.contains(sourceKey);
                case SOURCE_CROP -> sourceCrops.contains(sourceKey.toUpperCase(Locale.ENGLISH));
                case SOURCE_MOB -> sourceMobs.contains(sourceKey.toUpperCase(Locale.ENGLISH));
                default -> false;
            };
        }

        public double dropChanceFor(String sourceType) {
            Double override = dropChanceOverrides.get(sourceType);
            return override != null ? override : dropChance;
        }

        public LootEntry rollLoot() {
            if (lootTable == null || lootTable.isEmpty()) return null;

            int total = 0;
            for (LootEntry entry : lootTable) total += entry.weight();

            int roll = ThreadLocalRandom.current().nextInt(total);
            int cumulative = 0;
            for (LootEntry entry : lootTable) {
                cumulative += entry.weight();
                if (roll < cumulative) return entry;
            }
            return lootTable.get(lootTable.size() - 1);
        }
    }

    private record SpecialMaterialEffects(SpecialMaterialSound sound, SpecialMaterialParticle particle,
                                          boolean lightning, SpecialMaterialFirework firework,
                                          SpecialMaterialTitle title, String actionbar) {
    }

    private record SpecialMaterialSound(String name, float volume, float pitch) {
    }

    private record SpecialMaterialParticle(String type, int count, double speed, String animation, double radius) {
    }

    private record SpecialMaterialFirework(int power, List<Color> colors, boolean flicker, boolean trail) {
    }

    private record SpecialMaterialTitle(String title, String subtitle) {
    }

    private record LootEntry(ItemStack item, int weight, int min, int max) {
    }

    private record SpecialMaterialReward(Map<String, RewardEntry> rewards) {
        public RewardEntry rewardFor(String sourceType) {
            return rewards.get(sourceType);
        }
    }

    private record RewardEntry(String item, int amount) {
    }

    private record DailyCounter(int count, String date) {
    }
}
