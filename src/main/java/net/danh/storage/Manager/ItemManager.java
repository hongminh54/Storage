package net.danh.storage.Manager;

import com.cryptomorin.xseries.XEnchantment;
import com.cryptomorin.xseries.XMaterial;
import net.danh.storage.NMS.NMSAssistant;
import net.danh.storage.Utils.Chat;
import net.danh.storage.Utils.File;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class ItemManager {

    private static final NMSAssistant NMS = new NMSAssistant();

    private static ItemStack createBaseItem(ConfigurationSection section, String materialOverride) {
        if (section == null) return null;

        String materialString = materialOverride != null ? materialOverride : section.getString("material");
        Optional<XMaterial> xMaterialOptional = XMaterial.matchXMaterial(materialString != null ? materialString : "BLACK_STAINED_GLASS_PANE");

        if (!xMaterialOptional.isPresent() || xMaterialOptional.get().parseItem() == null) {
            return null;
        }

        ItemStack itemStack = xMaterialOptional.get().parseItem();
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
                            meta.addItemFlags(ItemFlag.HIDE_UNBREAKABLE, ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_DESTROYS, ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_PLACED_ON, ItemFlag.HIDE_POTION_EFFECTS);
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

    private static ItemStack applyPlaceholders(ItemStack item, List<String> loreTemplate, String displayNameTemplate, String... replacements) {
        if (item == null || replacements.length % 2 != 0) return item;

        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;

        if (displayNameTemplate != null) {
            String displayName = displayNameTemplate;
            for (int i = 0; i < replacements.length - 1; i += 2) {
                displayName = displayName.replace(replacements[i], replacements[i + 1]);
            }
            meta.setDisplayName(Chat.colorizewp(displayName));
        }

        if (loreTemplate != null) {
            List<String> newLore = loreTemplate.stream()
                    .map(line -> {
                        String result = line;
                        for (int i = 0; i < replacements.length - 1; i += 2) {
                            result = result.replace(replacements[i], replacements[i + 1]);
                        }
                        return result;
                    })
                    .collect(Collectors.toList());
            meta.setLore(Chat.colorizewp(newLore));
        }

        item.setItemMeta(meta);
        return item;
    }

    public static ItemStack getItemConfig(ConfigurationSection section) {
        ItemStack item = createBaseItem(section, null);
        if (item == null) return null;

        return applyPlaceholders(item, section.getStringList("lore"), section.getString("name"));
    }

    public static ItemStack getItemConfig(Player p, String material, ConfigurationSection section) {
        ItemStack item = createBaseItem(section, null);
        if (item == null) return null;

        String materialName = File.getConfig().getString("items." + material, material.split(";")[0]);

        return applyPlaceholders(item, section.getStringList("lore"), section.getString("name"),
                "#item_amount#", String.valueOf(MineManager.getPlayerBlock(p, material)),
                "#max_storage#", String.valueOf(MineManager.getMaxBlock(p)),
                "#material#", materialName);
    }

    public static String getStatus(Player p) {
        if (MineManager.getToggleStatus(p)) {
            return Chat.colorizewp(File.getMessage().getString("user.status.status_on"));
        } else return Chat.colorizewp(File.getMessage().getString("user.status.status_off"));
    }

    public static ItemStack getItemConfig(Player p, ConfigurationSection section) {
        ItemStack item = createBaseItem(section, null);
        if (item == null) return null;

        return applyPlaceholders(item, section.getStringList("lore"), section.getString("name"),
                "#status#", getStatus(p));
    }

    public static ItemStack getItemConfig(Player p, String material, String name, ConfigurationSection section) {
        ItemStack item = createBaseItem(section, material.split(";")[0]);
        if (item == null) return null;

        return applyPlaceholders(item, section.getStringList("lore"), name,
                "#item_amount#", String.valueOf(MineManager.getPlayerBlock(p, material)),
                "#max_storage#", String.valueOf(MineManager.getMaxBlock(p)));
    }

    public static ItemStack getItemConfigWithPlaceholders(Player p, ConfigurationSection section, String... placeholders) {
        ItemStack item = createBaseItem(section, null);
        if (item == null) return null;

        return applyPlaceholders(item, section.getStringList("lore"), section.getString("name"), placeholders);
    }

    public static ItemStack getItemConfigWithPlaceholders(Player p, String material, String name, ConfigurationSection section, String... placeholders) {
        ItemStack item = createBaseItem(section, material.split(";")[0]);
        if (item == null) return null;

        return applyPlaceholders(item, section.getStringList("lore"), name, placeholders);
    }

    @Deprecated
    public static ItemStack replaceLore(ItemStack item, List<String> loreTemplate, String... replacements) {
        return applyPlaceholders(item, loreTemplate, null, replacements);
    }

    @Deprecated
    public static ItemStack replacePlaceholders(ItemStack item, String... replacements) {
        if (item == null || replacements.length % 2 != 0) return item;

        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;

        return applyPlaceholders(item, meta.getLore(), meta.getDisplayName(), replacements);
    }
}
