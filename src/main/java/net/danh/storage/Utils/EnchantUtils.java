package net.danh.storage.Utils;

import de.tr7zw.changeme.nbtapi.NBTItem;
import net.danh.storage.Manager.EnchantManager;
import net.danh.storage.NMS.NMSAssistant;
import org.bukkit.ChatColor;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class EnchantUtils {

    private static final String NBT_KEY = "StorageEnchants";

    public static boolean isPickaxe(ItemStack item) {
        if (item == null || item.getType().name().equals("AIR")) {
            return false;
        }

        String materialName = item.getType().name();
        return materialName.endsWith("_PICKAXE");
    }

    public static ItemStack addCustomEnchant(ItemStack item, String enchantName, int level) {
        if (item == null || item.getType().name().equals("AIR") || item.getAmount() <= 0) return null;

        NBTItem nbtItem = new NBTItem(item);
        nbtItem.setInteger(NBT_KEY + "." + enchantName, level);

        ItemStack result = nbtItem.getItem();
        result = addGlowEffect(result);
        result = updateLore(result, enchantName, level);

        return result;
    }

    public static ItemStack removeCustomEnchant(ItemStack item, String enchantName) {
        if (item == null || item.getType().name().equals("AIR") || item.getAmount() <= 0) return null;

        NBTItem nbtItem = new NBTItem(item);
        if (nbtItem.hasKey(NBT_KEY + "." + enchantName)) {
            nbtItem.removeKey(NBT_KEY + "." + enchantName);
        }

        ItemStack result = nbtItem.getItem();

        if (!hasAnyCustomEnchant(result)) {
            result = removeGlowEffect(result);
        }

        result = removeLoreEnchant(result, enchantName);

        return result;
    }

    public static boolean hasCustomEnchant(ItemStack item, String enchantName) {
        if (item == null || item.getType().name().equals("AIR") || item.getAmount() <= 0) return false;

        NBTItem nbtItem = new NBTItem(item);
        return nbtItem.hasKey(NBT_KEY + "." + enchantName);
    }

    public static int getCustomEnchantLevel(ItemStack item, String enchantName) {
        if (item == null || item.getType().name().equals("AIR") || item.getAmount() <= 0) return 0;

        NBTItem nbtItem = new NBTItem(item);
        return nbtItem.getInteger(NBT_KEY + "." + enchantName);
    }

    public static boolean hasAnyCustomEnchant(ItemStack item) {
        if (item == null || item.getType().name().equals("AIR") || item.getAmount() <= 0) return false;

        NBTItem nbtItem = new NBTItem(item);
        return nbtItem.hasKey(NBT_KEY);
    }

    private static ItemStack addGlowEffect(ItemStack item) {
        if (item == null) return null;

        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;

        Enchantment durabilityEnchant = Enchantment.getByName("UNBREAKING");
        if (durabilityEnchant == null) {
            durabilityEnchant = Enchantment.getByName("DURABILITY");
        }

        if (new NMSAssistant().isVersionGreaterThanOrEqualTo(14)) {
            meta.addEnchant(durabilityEnchant, 1, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        } else {
            meta.addEnchant(durabilityEnchant, 1, true);
        }

        item.setItemMeta(meta);
        return item;
    }

    private static ItemStack removeGlowEffect(ItemStack item) {
        if (item == null) return null;

        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;

        Enchantment durabilityEnchant = Enchantment.getByName("UNBREAKING");
        if (durabilityEnchant == null) {
            durabilityEnchant = Enchantment.getByName("DURABILITY");
        }
        meta.removeEnchant(durabilityEnchant);
        if (new NMSAssistant().isVersionGreaterThanOrEqualTo(14)) {
            meta.removeItemFlags(ItemFlag.HIDE_ENCHANTS);
        }

        item.setItemMeta(meta);
        return item;
    }

    private static ItemStack updateLore(ItemStack item, String enchantName, int level) {
        if (item == null) return null;

        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;

        List<String> lore = meta.getLore();
        if (lore == null) {
            lore = new ArrayList<>();
        }

        String enchantLore = EnchantManager.getEnchantLoreFormat(enchantName, level);
        if (enchantLore != null) {
            removeLoreEnchantFromList(lore, enchantName);
            lore.add(Chat.colorize(enchantLore));
        }

        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private static ItemStack removeLoreEnchant(ItemStack item, String enchantName) {
        if (item == null) return null;

        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;

        List<String> lore = meta.getLore();
        if (lore == null) return item;

        removeLoreEnchantFromList(lore, enchantName);

        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private static void removeLoreEnchantFromList(List<String> lore, String enchantName) {
        String enchantDisplayName = EnchantManager.getEnchantDisplayName(enchantName);
        if (enchantDisplayName == null) return;
        lore.removeIf(line -> {
            String cleanLine = ChatColor.stripColor(line);
            return cleanLine.contains(enchantDisplayName);
        });
    }
}
