package net.danh.storage.Utils;

import com.cryptomorin.xseries.XMaterial;
import de.tr7zw.changeme.nbtapi.NBTItem;
import net.danh.storage.NMS.NMSAssistant;
import net.danh.storage.Recipe.Recipe;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

/**
 * Optimized utility for importing ItemStack data into Recipe objects
 * Uses efficient deep copying to preserve ALL item data including NBT
 */
public class ItemImportUtil {
    
    // Cache version check for performance
    private static final boolean IS_LEGACY_VERSION;
    private static final String[] CUSTOM_PLUGIN_TAGS = {
        "MMOITEMS_TYPE", "MMOITEMS_ID", "MMOITEMS_ITEM_ID",
        "MyItems", "itemsadder", "oraxen", "ExecutableItems",
        "CustomItems", "ItemJoin", "MythicMobs", "EliteMobs"
    };
    
    static {
        IS_LEGACY_VERSION = new NMSAssistant().isVersionLessThanOrEqualTo(12);
    }
    
    /**
     * Imports ItemStack data into Recipe using optimized deep copy approach
     */
    public static void importItemToRecipe(ItemStack item, Recipe recipe) {
        if (item == null || recipe == null) return;
        
        // Create deep copy to preserve ALL data
        ItemStack itemCopy = createDeepCopy(item);
        if (itemCopy == null) return;
        
        // Store complete item data efficiently
        storeItemData(itemCopy, recipe);
        setDefaultProperties(itemCopy, recipe);
    }
    
    /**
     * Creates exact ItemStack from Recipe data
     */
    public static ItemStack createExactItemFromRecipe(Recipe recipe) {
        if (recipe == null) return null;
        
        ItemStack item = createItemFromMaterial(recipe.getResultMaterial());
        if (item == null) return null;
        
        item.setAmount(recipe.getResultAmount());
        applyMetadata(item, recipe);
        applyCustomNBT(item, recipe);
        
        return item;
    }
    
    /**
     * Gets material string with proper version handling
     */
    public static String getMaterialString(ItemStack item) {
        if (item == null) return "STONE;0";
        
        if (IS_LEGACY_VERSION) {
            short dataValue = item.getDurability();
            return dataValue != 0 ? 
                item.getType().name() + ";" + dataValue :
                getXMaterialName(item.getType().name()) + ";0";
        } else {
            return getXMaterialName(item.getType().name()) + ";0";
        }
    }
    
    /**
     * Creates deep copy preserving all metadata and NBT
     */
    public static ItemStack createDeepCopy(ItemStack original) {
        if (original == null) return null;
        
        try {
            return new NBTItem(original).getItem().clone();
        } catch (Exception e) {
            return original.clone();
        }
    }
    
    // Private helper methods for better organization
    
    private static void storeItemData(ItemStack item, Recipe recipe) {
        recipe.setResultMaterial(getMaterialString(item));
        recipe.setResultAmount(item.getAmount());
        
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            storeBasicMeta(meta, recipe, item);
            storeEnchantments(meta, recipe);
            storeFlags(meta, recipe);
            storeCustomModelData(meta, recipe);
        }
        
        storeNBTData(item, recipe);
    }
    
    private static void storeBasicMeta(ItemMeta meta, Recipe recipe, ItemStack item) {
        // Display name
        recipe.setResultName(meta.hasDisplayName() ? 
            meta.getDisplayName() : generateDefaultName(item));
        
        // Lore
        if (meta.hasLore() && meta.getLore() != null) {
            recipe.setResultLore(new ArrayList<>(meta.getLore()));
        }
        
        // Unbreakable
        recipe.setResultUnbreakable(meta.isUnbreakable());
    }
    
    private static void storeEnchantments(ItemMeta meta, Recipe recipe) {
        Map<String, Integer> enchantments = new HashMap<>();
        for (Map.Entry<Enchantment, Integer> entry : meta.getEnchants().entrySet()) {
            enchantments.put(entry.getKey().getName(), entry.getValue());
        }
        recipe.setResultEnchantments(enchantments);
    }
    
    private static void storeFlags(ItemMeta meta, Recipe recipe) {
        recipe.setResultFlags(new HashSet<>(meta.getItemFlags()));
    }
    
    private static void storeCustomModelData(ItemMeta meta, Recipe recipe) {
        try {
            if (meta.hasCustomModelData()) {
                recipe.setResultCustomModelData(meta.getCustomModelData());
            }
        } catch (NoSuchMethodError ignored) {
            // Older versions don't support custom model data
        }
    }
    
    private static void storeNBTData(ItemStack item, Recipe recipe) {
        try {
            NBTItem nbtItem = new NBTItem(item);
            Map<String, String> customNBT = new HashMap<>();
            
            for (String tag : CUSTOM_PLUGIN_TAGS) {
                if (nbtItem.hasTag(tag)) {
                    customNBT.put(tag, nbtItem.getString(tag));
                }
            }
            
            if (!customNBT.isEmpty()) {
                recipe.setCustomNBTData(customNBT);
            }
        } catch (Exception ignored) {
            // Continue without NBT data if operations fail
        }
    }
    
    private static void setDefaultProperties(ItemStack item, Recipe recipe) {
        String itemName = recipe.getResultName();
        if (itemName.startsWith("&f")) {
            itemName = itemName.substring(2);
        }
        
        recipe.setName("&a" + itemName + " Recipe");
        recipe.setCategory(determineCategory(item));
        recipe.setEnabled(true);
        
        // Default requirement
        Map<String, Integer> requirements = new HashMap<>();
        requirements.put("DIAMOND;0", 1);
        recipe.setMaterialRequirements(requirements);
    }
    
    private static ItemStack createItemFromMaterial(String materialData) {
        if (materialData == null || materialData.isEmpty()) {
            materialData = "STONE;0";
        }
        
        String[] parts = materialData.split(";");
        String materialName = parts[0];
        short dataValue = 0;
        
        if (parts.length > 1) {
            try {
                dataValue = Short.parseShort(parts[1]);
            } catch (NumberFormatException ignored) {
                dataValue = 0;
            }
        }
        
        return createItemStack(materialName, dataValue);
    }
    
    private static ItemStack createItemStack(String materialName, short dataValue) {
        try {
            if (IS_LEGACY_VERSION) {
                return createLegacyItem(materialName, dataValue);
            } else {
                return createModernItem(materialName);
            }
        } catch (Exception e) {
            // Fallback to stone
            return new ItemStack(org.bukkit.Material.STONE, 1);
        }
    }
    
    private static ItemStack createLegacyItem(String materialName, short dataValue) {
        try {
            org.bukkit.Material material = org.bukkit.Material.valueOf(materialName);
            ItemStack item = new ItemStack(material, 1);
            if (dataValue != 0) {
                item.setDurability(dataValue);
            }
            return item;
        } catch (IllegalArgumentException e) {
            Optional<XMaterial> xMaterial = XMaterial.matchXMaterial(materialName);
            if (xMaterial.isPresent()) {
                ItemStack item = xMaterial.get().parseItem();
                if (item != null && dataValue != 0) {
                    item.setDurability(dataValue);
                }
                return item;
            }
            throw e;
        }
    }
    
    private static ItemStack createModernItem(String materialName) {
        Optional<XMaterial> xMaterial = XMaterial.matchXMaterial(materialName);
        if (xMaterial.isPresent()) {
            return xMaterial.get().parseItem();
        } else {
            org.bukkit.Material material = org.bukkit.Material.valueOf(materialName);
            return new ItemStack(material, 1);
        }
    }
    
    private static void applyMetadata(ItemStack item, Recipe recipe) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;
        
        // Display name
        if (recipe.getResultName() != null && !recipe.getResultName().isEmpty()) {
            meta.setDisplayName(recipe.getResultName());
        }
        
        // Lore
        if (recipe.getResultLore() != null && !recipe.getResultLore().isEmpty()) {
            meta.setLore(new ArrayList<>(recipe.getResultLore()));
        }
        
        // Enchantments
        for (Map.Entry<String, Integer> enchant : recipe.getResultEnchantments().entrySet()) {
            try {
                Enchantment enchantment = Enchantment.getByName(enchant.getKey());
                if (enchantment != null) {
                    meta.addEnchant(enchantment, enchant.getValue(), true);
                }
            } catch (Exception ignored) {
                // Skip invalid enchantments
            }
        }
        
        // Flags
        if (recipe.getResultFlags() != null) {
            for (ItemFlag flag : recipe.getResultFlags()) {
                meta.addItemFlags(flag);
            }
        }
        
        // Unbreakable
        meta.setUnbreakable(recipe.isResultUnbreakable());
        
        // Custom model data
        if (recipe.getResultCustomModelData() > 0) {
            try {
                meta.setCustomModelData(recipe.getResultCustomModelData());
            } catch (NoSuchMethodError ignored) {
                // Older versions don't support custom model data
            }
        }
        
        item.setItemMeta(meta);
    }
    
    private static void applyCustomNBT(ItemStack item, Recipe recipe) {
        if (recipe.getCustomNBTData() == null || recipe.getCustomNBTData().isEmpty()) {
            return;
        }
        
        try {
            NBTItem nbtItem = new NBTItem(item);
            for (Map.Entry<String, String> entry : recipe.getCustomNBTData().entrySet()) {
                nbtItem.setString(entry.getKey(), entry.getValue());
            }
            // Note: NBTItem modifies the original item, no need to reassign
        } catch (Exception ignored) {
            // Continue without NBT data if operations fail
        }
    }
    
    private static String generateDefaultName(ItemStack item) {
        String materialName = item.getType().name().toLowerCase().replace("_", " ");
        return "&f" + capitalizeWords(materialName);
    }
    
    private static String determineCategory(ItemStack item) {
        String materialName = item.getType().name().toLowerCase();
        
        if (materialName.contains("sword") || materialName.contains("axe") || 
            materialName.contains("pickaxe") || materialName.contains("shovel") || 
            materialName.contains("hoe") || materialName.contains("bow") || 
            materialName.contains("crossbow") || materialName.contains("trident")) {
            return "tools";
        }
        
        if (materialName.contains("helmet") || materialName.contains("chestplate") || 
            materialName.contains("leggings") || materialName.contains("boots") || 
            materialName.contains("armor")) {
            return "armor";
        }
        
        if (materialName.contains("block") || materialName.contains("stone") || 
            materialName.contains("wood") || materialName.contains("plank") || 
            materialName.contains("brick") || materialName.contains("concrete")) {
            return "blocks";
        }
        
        return "misc";
    }
    
    private static String capitalizeWords(String str) {
        if (str == null || str.isEmpty()) return str;
        
        String[] words = str.split(" ");
        StringBuilder result = new StringBuilder();
        
        for (int i = 0; i < words.length; i++) {
            if (i > 0) result.append(" ");
            if (!words[i].isEmpty()) {
                result.append(Character.toUpperCase(words[i].charAt(0)));
                if (words[i].length() > 1) {
                    result.append(words[i].substring(1));
                }
            }
        }
        
        return result.toString();
    }
    
    private static String getXMaterialName(String materialName) {
        Optional<XMaterial> xMaterial = XMaterial.matchXMaterial(materialName);
        return xMaterial.isPresent() ? xMaterial.get().name() : materialName;
    }
}