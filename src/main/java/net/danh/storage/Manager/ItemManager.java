package net.danh.storage.Manager;

import com.cryptomorin.xseries.XEnchantment;
import net.danh.storage.NMS.NMSAssistant;
import net.danh.storage.Storage;
import net.danh.storage.Utils.ChatUtils;
import net.danh.storage.Utils.File;
import net.danh.storage.Utils.MaterialUtils;
import net.danh.storage.Utils.PlaceholderUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class ItemManager {

    private static final NMSAssistant NMS = new NMSAssistant();
    private static final Method MATERIAL_IS_ITEM = resolveMaterialIsItem();
    private static final Set<String> INVALID_MATERIAL_LOGGED = new HashSet<>();

    private static String getStorageAutoSellSymbol(@NotNull Player player, @NotNull String materialData) {
        boolean enabled = MineManager.isAutoSellEnabledForItem(player, materialData);
        return File.getMessage().getString(
                enabled ? "user.autosell.yes" : "user.autosell.no",
                enabled ? "&a✔" : "&c✘"
        );
    }

    private static String getStorageSellableSymbol(String materialData) {
        ConfigurationSection worthSection = File.getConfig().getConfigurationSection("worth");
        if (worthSection == null || materialData == null || materialData.trim().isEmpty()) {
            return File.getMessage().getString("user.sellable.no", "&c✘");
        }

        String worthKey = resolveWorthKey(worthSection, materialData);
        if (worthKey == null) {
            return File.getMessage().getString("user.sellable.no", "&c✘");
        }

        boolean sellable = parseWorthValue(worthSection, worthKey) > 0;
        return File.getMessage().getString(
                sellable ? "user.sellable.yes" : "user.sellable.no",
                sellable ? "&a✔" : "&c✘"
        );
    }

    private static double parseWorthValue(ConfigurationSection section, String worthKey) {
        if (section == null || worthKey == null) {
            return 0D;
        }

        Object raw = section.get(worthKey);
        if (raw instanceof java.lang.Number) {
            return ((java.lang.Number) raw).doubleValue();
        }

        if (raw instanceof String) {
            String value = ((String) raw).trim();
            if (value.isEmpty()) {
                return 0D;
            }
            String[] parts = value.split(";", -1);
            try {
                return Double.parseDouble(parts[0].trim());
            } catch (NumberFormatException ignored) {
                return 0D;
            }
        }

        return section.getDouble(worthKey);
    }

    private static String resolveWorthKey(ConfigurationSection section, String materialData) {
        if (section == null || materialData == null) {
            return null;
        }

        String normalized = materialData.replace(":", ";");

        if (section.contains(normalized)) {
            return normalized;
        }

        if (normalized.endsWith(";0")) {
            String noData = normalized.substring(0, normalized.length() - 2);
            if (section.contains(noData)) {
                return noData;
            }
        }

        String withZero = normalized + ";0";
        if (section.contains(withZero)) {
            return withZero;
        }

        return null;
    }

    private static Method resolveMaterialIsItem() {
        try {
            return Material.class.getMethod("isItem");
        } catch (NoSuchMethodException e) {
            return null;
        }
    }

    private static boolean isItemMaterial(Material material) {
        if (material == null) return false;
        if (MATERIAL_IS_ITEM != null) {
            try {
                return (boolean) MATERIAL_IS_ITEM.invoke(material);
            } catch (Exception ignored) {
                return false;
            }
        }
        return material != Material.AIR;
    }

    private static ItemStack createBaseItem(ConfigurationSection section, String materialOverride) {
        if (section == null) return null;

        String materialString = materialOverride != null ? materialOverride : section.getString("material");
        ItemStack itemStack = MaterialUtils.createItem(materialString != null ? materialString : "BLACK_STAINED_GLASS_PANE");

        if (itemStack == null) {
            logInvalidMaterial(materialString, section, "unknown material");
            return new ItemStack(Material.AIR);
        }

        Material material = itemStack.getType();
        if (!isItemMaterial(material)) {
            logInvalidMaterial(materialString, section, "not an item material");
            return new ItemStack(Material.AIR);
        }

        ItemMeta meta = itemStack.getItemMeta();
        if (meta == null) return itemStack;

        if (NMS.isVersionLessThanOrEqualTo(13)) {
            itemStack.setDurability((short) section.getInt("damage"));
        }
        if (NMS.isVersionGreaterThanOrEqualTo(14)) {
            meta.setCustomModelData(section.getInt("custom-model-data"));
        }

        itemStack.setAmount(section.getInt("amount", 1));
        meta.setUnbreakable(section.getBoolean("unbreakable"));

        if (section.contains("enchants")) {
            ConfigurationSection enchantsSection = section.getConfigurationSection("enchants");
            if (enchantsSection != null) {
                for (String enchant_name : enchantsSection.getKeys(false)) {
                    int level = section.getInt("enchants." + enchant_name);
                    Optional<XEnchantment> enchantment = XEnchantment.matchXEnchantment(enchant_name);
                    if (enchantment.isPresent() && enchantment.get().getEnchant() != null) {
                        meta.addEnchant(enchantment.get().getEnchant(), level, false);
                    }
                }
            }
        }

        if (section.contains("flags")) {
            ConfigurationSection flagsSection = section.getConfigurationSection("flags");
            if (flagsSection != null) {
                for (String flag_name : flagsSection.getKeys(false)) {
                    boolean apply = section.getBoolean("flags." + flag_name);
                    if (flag_name.equalsIgnoreCase("ALL")) {
                        if (apply) {
                            meta.addItemFlags(ItemFlag.HIDE_UNBREAKABLE, ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_DESTROYS, ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_PLACED_ON);
                            try {
                                meta.addItemFlags(ItemFlag.valueOf("HIDE_POTION_EFFECTS"));
                            } catch (IllegalArgumentException ignored) {
                                // Not available in this version
                            }
                            break;
                        }
                    } else {
                        try {
                            meta.addItemFlags(ItemFlag.valueOf(flag_name));
                        } catch (IllegalArgumentException ignored) {
                        }
                    }
                }
            }
        }

        itemStack.setItemMeta(meta);
        return itemStack;
    }

    private static void logInvalidMaterial(String material, ConfigurationSection section, String reason) {
        String safeMaterial = material == null ? "null" : material;
        String path = section == null ? "unknown" : section.getCurrentPath();
        String key = safeMaterial + "|" + path + "|" + reason;
        synchronized (INVALID_MATERIAL_LOGGED) {
            if (!INVALID_MATERIAL_LOGGED.add(key)) return;
        }
        Storage.getStorage().getLogger().warning(
                "[Storage] Invalid GUI material '" + safeMaterial
                        + "' at '" + path + "' (" + reason + ")"
        );
    }

    private static ItemStack applyPlaceholders(Player player, ItemStack item, List<String> loreTemplate, String displayNameTemplate, String... replacements) {
        if (item == null) return item;

        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;

        if (displayNameTemplate != null) {
            String displayName = displayNameTemplate;
            if (replacements != null && replacements.length >= 2) {
                for (int i = 0; i < replacements.length - 1; i += 2) {
                    displayName = displayName.replace(replacements[i], replacements[i + 1]);
                }
            }
            displayName = PlaceholderUtils.setPlaceholders(player, displayName);
            meta.setDisplayName(ChatUtils.colorizewp(displayName));
        }

        if (loreTemplate != null) {
            List<String> newLore = loreTemplate.stream()
                    .map(line -> {
                        String result = line;
                        if (replacements != null && replacements.length >= 2) {
                            for (int i = 0; i < replacements.length - 1; i += 2) {
                                result = result.replace(replacements[i], replacements[i + 1]);
                            }
                        }
                        result = PlaceholderUtils.setPlaceholders(player, result);
                        return result;
                    })
                    .collect(Collectors.toList());
            meta.setLore(ChatUtils.colorizewp(newLore));
        }

        item.setItemMeta(meta);
        return item;
    }

    public static ItemStack getItemConfig(ConfigurationSection section) {
        ItemStack item = createBaseItem(section, null);
        if (item == null) return null;

        return applyPlaceholders(null, item, section.getStringList("lore"), section.getString("name"));
    }

    public static ItemStack getItemConfig(Player p, String material, ConfigurationSection section) {
        ItemStack item = createBaseItem(section, null);
        if (item == null) return null;

        String materialName = File.getConfig().getString("items." + material, material.split(";")[0]);

        return applyPlaceholders(p, item, section.getStringList("lore"), section.getString("name"),
                "#item_amount#", String.valueOf(MineManager.getPlayerBlock(p, material)),
                "#max_storage#", String.valueOf(MineManager.getMaxBlock(p)),
                "#material#", materialName,
                "#sellable#", getStorageSellableSymbol(material),
                "#autosell#", getStorageAutoSellSymbol(p, material));
    }

    public static String getStatus(Player p) {
        if (MineManager.getToggleStatus(p)) {
            return ChatUtils.colorizewp(File.getMessage().getString("user.status.status_on"));
        } else return ChatUtils.colorizewp(File.getMessage().getString("user.status.status_off"));
    }

    public static ItemStack getItemConfig(Player p, ConfigurationSection section) {
        ItemStack item = createBaseItem(section, null);
        if (item == null) return null;

        return applyPlaceholders(p, item, section.getStringList("lore"), section.getString("name"),
                "#status#", getStatus(p));
    }

    public static ItemStack getItemConfig(Player p, String material, String name, ConfigurationSection section) {
        ItemStack item = createBaseItem(section, material.split(";")[0]);
        if (item == null) return null;

        return applyPlaceholders(p, item, section.getStringList("lore"), name,
                "#item_amount#", String.valueOf(MineManager.getPlayerBlock(p, material)),
                "#max_storage#", String.valueOf(MineManager.getMaxBlock(p)),
                "#sellable#", getStorageSellableSymbol(material),
                "#autosell#", getStorageAutoSellSymbol(p, material));
    }

    public static ItemStack getItemConfig(String playerName, String material, String name, ConfigurationSection section) {
        ItemStack item = createBaseItem(section, material.split(";")[0]);
        if (item == null) return null;

        return applyPlaceholders(null, item, section.getStringList("lore"), name,
                "#item_amount#", String.valueOf(MineManager.getPlayerBlock(playerName, material)),
                "#max_storage#", String.valueOf(MineManager.getMaxStorage()),
                "#sellable#", getStorageSellableSymbol(material));
    }

    public static ItemStack getItemConfigWithPlaceholders(Player p, ConfigurationSection section, String... placeholders) {
        ItemStack item = createBaseItem(section, null);
        if (item == null) return null;

        return applyPlaceholders(p, item, section.getStringList("lore"), section.getString("name"), placeholders);
    }

    public static ItemStack getItemConfigWithPlaceholders(Player p, String material, String name, ConfigurationSection section, String... placeholders) {
        ItemStack item = createBaseItem(section, material.split(";")[0]);
        if (item == null) return null;

        int baseLength = 8;
        int extraLength = placeholders == null ? 0 : placeholders.length;
        String[] merged = new String[baseLength + extraLength];
        merged[0] = "#item_amount#";
        merged[1] = String.valueOf(MineManager.getPlayerBlock(p, material));
        merged[2] = "#max_storage#";
        merged[3] = String.valueOf(MineManager.getMaxBlock(p));
        merged[4] = "#sellable#";
        merged[5] = getStorageSellableSymbol(material);
        merged[6] = "#autosell#";
        merged[7] = getStorageAutoSellSymbol(p, material);
        if (extraLength > 0) {
            System.arraycopy(placeholders, 0, merged, baseLength, extraLength);
        }

        return applyPlaceholders(p, item, section.getStringList("lore"), name,
                merged);
    }

    public static ItemStack getItemConfig(String playerName, String material, String name, ConfigurationSection section, String... placeholders) {
        ItemStack item = createBaseItem(section, material.split(";")[0]);
        if (item == null) return null;

        String[] basePlaceholders = new String[]{
                "#item_amount#", String.valueOf(MineManager.getPlayerBlock(playerName, material)),
                "#max_storage#", String.valueOf(MineManager.getMaxStorage()),
                "#sellable#", getStorageSellableSymbol(material)
        };

        String[] merged;
        if (placeholders != null && placeholders.length > 0) {
            merged = new String[basePlaceholders.length + placeholders.length];
            System.arraycopy(basePlaceholders, 0, merged, 0, basePlaceholders.length);
            System.arraycopy(placeholders, 0, merged, basePlaceholders.length, placeholders.length);
        } else {
            merged = basePlaceholders;
        }

        return applyPlaceholders(null, item, section.getStringList("lore"), name, merged);
    }

    public static ItemStack getItemConfigWithPlaceholders(String playerName, String material, String name, ConfigurationSection section, String... placeholders) {
        ItemStack item = createBaseItem(section, material.split(";")[0]);
        if (item == null) return null;

        int baseLength = 6;
        int extraLength = placeholders == null ? 0 : placeholders.length;
        String[] merged = new String[baseLength + extraLength];
        merged[0] = "#item_amount#";
        merged[1] = String.valueOf(MineManager.getPlayerBlock(playerName, material));
        merged[2] = "#max_storage#";
        merged[3] = String.valueOf(MineManager.getMaxStorage());
        merged[4] = "#sellable#";
        merged[5] = getStorageSellableSymbol(material);
        if (extraLength > 0) {
            System.arraycopy(placeholders, 0, merged, baseLength, extraLength);
        }

        return applyPlaceholders(null, item, section.getStringList("lore"), name, merged);
    }

    @Deprecated
    public static ItemStack replaceLore(ItemStack item, List<String> loreTemplate, String... replacements) {
        return applyPlaceholders(null, item, loreTemplate, null, replacements);
    }

    @Deprecated
    public static ItemStack replacePlaceholders(ItemStack item, String... replacements) {
        if (item == null) return item;

        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;

        return applyPlaceholders(null, item, meta.getLore(), meta.getDisplayName(), replacements);
    }

    public static ItemStack setPlayerSkull(ItemStack item, String playerName) {
        if (item == null || playerName == null || playerName.isEmpty()) return item;

        ItemMeta meta = item.getItemMeta();
        if (!(meta instanceof SkullMeta skullMeta)) return item;

        try {
            if (NMS.isVersionGreaterThanOrEqualTo(12)) {
                OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(playerName);
                skullMeta.setOwningPlayer(offlinePlayer);
            } else {
                skullMeta.setOwner(playerName);
            }
            item.setItemMeta(skullMeta);
        } catch (Exception ignored) {
            try {
                skullMeta.setOwner(playerName);
                item.setItemMeta(skullMeta);
            } catch (Exception e) {
                // Silent fail - return original item
            }
        }

        return item;
    }
}
