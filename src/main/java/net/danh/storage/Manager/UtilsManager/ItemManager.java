package net.danh.storage.Manager.UtilsManager;

import com.cryptomorin.xseries.XEnchantment;
import com.cryptomorin.xseries.XMaterial;
import net.danh.storage.Manager.GameManager.ChatManager;
import net.danh.storage.Manager.GameManager.MineManager;
import net.danh.storage.NMS.NMSAssistant;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

public class ItemManager {

    public static ItemStack getItemConfig(ConfigurationSection section) {
        ItemStack itemStack = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta meta = itemStack.getItemMeta();
        String m_s = section.getString("material");
        Optional<XMaterial> xMaterialOptional = XMaterial.matchXMaterial(m_s != null ? m_s : "BLACK_STAINED_GLASS_PANE");
        if (xMaterialOptional.isPresent() && xMaterialOptional.get().parseItem() != null) {
            itemStack = xMaterialOptional.get().parseItem();
        }
        NMSAssistant nms = new NMSAssistant();
        if (nms.isVersionLessThanOrEqualTo(13)) {
            itemStack.setDurability((short) section.getInt("damage"));
        }
        if (nms.isVersionGreaterThanOrEqualTo(14)) {
            meta.setCustomModelData(section.getInt("custom-model-data"));
        }
        itemStack.setAmount(section.getInt("amount"));
        meta.setUnbreakable(section.getBoolean("unbreakable"));
        meta.setLore(ChatManager.colorizewp(section.getStringList("lore")));
        meta.setDisplayName(ChatManager.colorizewp(section.getString("name")));
        if (section.contains("enchants")) {
            for (String enchant_name : Objects.requireNonNull(section.getConfigurationSection("enchants")).getKeys(false)) {
                int level = section.getInt("enchants." + enchant_name);
                Optional<XEnchantment> enchantment = XEnchantment.matchXEnchantment(enchant_name);
                if (enchantment.isPresent() && enchantment.get().getEnchant() != null) {
                    meta.addEnchant(enchantment.get().getEnchant(), level, false);
                }
            }
        }
        if (section.contains("flags")) {
            for (String flag_name : Objects.requireNonNull(section.getConfigurationSection("flags")).getKeys(false)) {
                boolean apply = section.getBoolean("flags." + flag_name);
                if (flag_name.equalsIgnoreCase("ALL")) {
                    if (apply) {
                        meta.addItemFlags(ItemFlag.HIDE_UNBREAKABLE, ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_DESTROYS, ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_PLACED_ON, ItemFlag.HIDE_POTION_EFFECTS);
                        break;
                    }
                } else {
                    meta.addItemFlags(ItemFlag.valueOf(flag_name));
                }
            }
        }
        itemStack.setItemMeta(meta);
        return itemStack;
    }

    public static ItemStack getItemConfig(Player p, String material, String name, ConfigurationSection section) {
        ItemStack itemStack = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta meta = itemStack.getItemMeta();
        Optional<XMaterial> xMaterialOptional = XMaterial.matchXMaterial(material.split(";")[0] != null ? material.split(";")[0] : "BLACK_STAINED_GLASS_PANE");
        if (xMaterialOptional.isPresent() && xMaterialOptional.get().parseItem() != null) {
            itemStack = xMaterialOptional.get().parseItem();
        }
        NMSAssistant nms = new NMSAssistant();
        if (nms.isVersionLessThanOrEqualTo(13)) {
            itemStack.setDurability((short) section.getInt("damage"));
        }
        if (nms.isVersionGreaterThanOrEqualTo(14)) {
            meta.setCustomModelData(section.getInt("custom-model-data"));
        }
        itemStack.setAmount(section.getInt("amount"));
        meta.setUnbreakable(section.getBoolean("unbreakable"));
        meta.setLore(ChatManager.colorizewp(section.getStringList("lore")
                .stream().map(s -> s.replace("#item_amount#", String.valueOf(MineManager.getPlayerBlock(p, material)))
                        .replace("#max_storage#", String.valueOf(MineManager.getMaxBlock(p)))).collect(Collectors.toList())));
        meta.setDisplayName(ChatManager.colorizewp(name));
        if (section.contains("enchants")) {
            for (String enchant_name : Objects.requireNonNull(section.getConfigurationSection("enchants")).getKeys(false)) {
                int level = section.getInt("enchants." + enchant_name);
                Optional<XEnchantment> enchantment = XEnchantment.matchXEnchantment(enchant_name);
                if (enchantment.isPresent() && enchantment.get().getEnchant() != null) {
                    meta.addEnchant(enchantment.get().getEnchant(), level, false);
                }
            }
        }
        if (section.contains("flags")) {
            for (String flag_name : Objects.requireNonNull(section.getConfigurationSection("flags")).getKeys(false)) {
                boolean apply = section.getBoolean("flags." + flag_name);
                if (flag_name.equalsIgnoreCase("ALL")) {
                    if (apply) {
                        meta.addItemFlags(ItemFlag.HIDE_UNBREAKABLE, ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_DESTROYS, ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_PLACED_ON, ItemFlag.HIDE_POTION_EFFECTS);
                        break;
                    }
                } else {
                    meta.addItemFlags(ItemFlag.valueOf(flag_name));
                }
            }
        }
        itemStack.setItemMeta(meta);
        return itemStack;
    }
}
