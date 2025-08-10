package net.danh.storage.Utils;

import com.cryptomorin.xseries.XEnchantment;
import com.cryptomorin.xseries.XMaterial;
import de.tr7zw.changeme.nbtapi.NBTItem;
import net.danh.storage.NMS.NMSAssistant;
import net.danh.storage.Recipe.Recipe;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;
import java.util.Optional;

/**
 * Utility class for importing ItemStack data into Recipe objects
 * Handles comprehensive item copying including NBT data from custom plugins
 * 
 * LEGACY SUPPORT:
 * - For Minecraft versions <= 1.12.2: Preserves data values from item durability
 * - For Minecraft versions >= 1.13: Uses 0 as data value (flattening update)
 * - Avoids XMaterial conversion for legacy items with data != 0 to preserve original material names
 * 
 * EXAMPLES:
 * - Legacy (1.12.2): Bone Meal (INK_SACK;15) -> "INK_SACK;15" (preserves original material name)
 * - Legacy (1.12.2): Red Wool (WOOL;14) -> "WOOL;14" (preserves original material name)
 * - Legacy (1.12.2): Blue Dye (INK_SACK;4) -> "INK_SACK;4" (preserves original material name)
 * - Legacy (1.12.2): Stone (STONE;0) -> "STONE;0" (can use XMaterial safely)
 * - Modern (1.13+): Bone Meal (BONE_MEAL) -> "BONE_MEAL;0"
 * - Modern (1.13+): Red Wool (RED_WOOL) -> "RED_WOOL;0"
 * 
 * This fixes the issue where imported items would have incorrect materials
 * (e.g., bone meal becoming ink sac, red wool becoming white wool) due to XMaterial conversion changing material names.
 */
public class ItemImportUtil {
    
    /**
     * Imports all data from an ItemStack into a Recipe object
     * Preserves ALL metadata including custom NBT data
     * 
     * @param item The ItemStack to import from
     * @param recipe The Recipe to import data into
     */
    public static void importItemToRecipe(ItemStack item, Recipe recipe) {
        if (item == null || recipe == null) return;
        
        // Import basic material data
        importMaterialData(item, recipe);
        
        // Import item metadata
        importItemMeta(item, recipe);
        
        // Import NBT data for custom plugins
        importNBTData(item, recipe);
        
        // Set default recipe properties
        setDefaultRecipeProperties(item, recipe);
    }
    
    /**
     * Imports material type and data value with proper legacy support
     * Handles data values correctly for versions <= 1.12.2
     */
    private static void importMaterialData(ItemStack item, Recipe recipe) {
        String materialString = getMaterialString(item);
        recipe.setResultMaterial(materialString);
        recipe.setResultAmount(item.getAmount());
        
        // Debug logging for troubleshooting
        NMSAssistant nms = new NMSAssistant();
        if (nms.isVersionLessThanOrEqualTo(12)) {
            System.out.println("[ItemImport] Legacy version detected - Material: " + item.getType().name() + 
                             ", Data: " + item.getDurability() + ", Result: " + materialString);
        } else {
            System.out.println("[ItemImport] Modern version detected - Material: " + item.getType().name() + 
                             ", Result: " + materialString);
        }
        
        // Additional debug for common legacy materials
        if (nms.isVersionLessThanOrEqualTo(12) && item.getDurability() != 0) {
            System.out.println("[ItemImport] Legacy material with data value detected - this will preserve original material name");
        }
    }
    
    /**
     * Gets the correct material string with data value for cross-version compatibility
     * Uses the same logic as the storage system for consistency
     * 
     * @param item The ItemStack to get material string from
     * @return Material string in format "MATERIAL;dataValue"
     */
    public static String getMaterialString(ItemStack item) {
        if (item == null) return "STONE;0";
        
        NMSAssistant nms = new NMSAssistant();
        
        // For legacy versions (<=1.12.2), preserve data value from durability
        if (nms.isVersionLessThanOrEqualTo(12)) {
            short dataValue = item.getDurability();
            
            // For legacy versions with data value != 0, use original material name
            // to avoid XMaterial conversion issues (e.g., INK_SACK;15 -> INK_SAC;15)
            if (dataValue != 0) {
                return item.getType().name() + ";" + dataValue;
            } else {
                // Only use XMaterial for data value 0 items
                Optional<XMaterial> xMaterial = XMaterial.matchXMaterial(item.getType().name());
                if (xMaterial.isPresent()) {
                    return xMaterial.get().name() + ";" + dataValue;
                } else {
                    return item.getType().name() + ";" + dataValue;
                }
            }
        } else {
            // For modern versions (>=1.13), use XMaterial for better compatibility
            Optional<XMaterial> xMaterial = XMaterial.matchXMaterial(item.getType().name());
            if (xMaterial.isPresent()) {
                return xMaterial.get().name() + ";0";
            } else {
                return item.getType().name() + ";0";
            }
        }
    }
    
    /**
     * Imports ItemMeta data including name, lore, enchantments, flags
     */
    private static void importItemMeta(ItemStack item, Recipe recipe) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;
        
        // Import display name
        if (meta.hasDisplayName()) {
            recipe.setResultName(meta.getDisplayName());
        } else {
            // Generate name from material
            String materialName = item.getType().name().toLowerCase()
                    .replace("_", " ");
            materialName = capitalizeWords(materialName);
            recipe.setResultName("&f" + materialName);
        }
        
        // Import lore
        if (meta.hasLore() && meta.getLore() != null) {
            recipe.setResultLore(new ArrayList<>(meta.getLore()));
        }
        
        // Import enchantments
        Map<String, Integer> enchantments = new HashMap<>();
        for (Map.Entry<Enchantment, Integer> entry : meta.getEnchants().entrySet()) {
            Optional<XEnchantment> xEnchant = XEnchantment.matchXEnchantment(entry.getKey().getName());
            if (xEnchant.isPresent()) {
                enchantments.put(xEnchant.get().name(), entry.getValue());
            } else {
                enchantments.put(entry.getKey().getName(), entry.getValue());
            }
        }
        recipe.setResultEnchantments(enchantments);
        
        // Import item flags
        Set<ItemFlag> flags = new HashSet<>(meta.getItemFlags());
        recipe.setResultFlags(flags);
        
        // Import unbreakable status
        recipe.setResultUnbreakable(meta.isUnbreakable());
        
        // Import custom model data (1.14+)
        try {
            if (meta.hasCustomModelData()) {
                recipe.setResultCustomModelData(meta.getCustomModelData());
            }
        } catch (NoSuchMethodError ignored) {
            // Older versions don't support custom model data
        }
    }
    
    /**
     * Imports NBT data for compatibility with custom item plugins
     * Handles MMOItems, MyItems, and other custom plugins
     */
    private static void importNBTData(ItemStack item, Recipe recipe) {
        try {
            NBTItem nbtItem = new NBTItem(item);
            
            // Store important NBT tags for custom plugins
            Map<String, String> customNBT = new HashMap<>();
            
            // MMOItems support
            if (nbtItem.hasTag("MMOITEMS_TYPE")) {
                customNBT.put("MMOITEMS_TYPE", nbtItem.getString("MMOITEMS_TYPE"));
            }
            if (nbtItem.hasTag("MMOITEMS_ID")) {
                customNBT.put("MMOITEMS_ID", nbtItem.getString("MMOITEMS_ID"));
            }
            
            // MyItems support (1.12.2)
            if (nbtItem.hasTag("MyItems")) {
                customNBT.put("MyItems", nbtItem.getString("MyItems"));
            }
            
            // ItemsAdder support
            if (nbtItem.hasTag("itemsadder")) {
                customNBT.put("itemsadder", nbtItem.getString("itemsadder"));
            }
            
            // Oraxen support
            if (nbtItem.hasTag("oraxen")) {
                customNBT.put("oraxen", nbtItem.getString("oraxen"));
            }
            
            // Store custom NBT data in recipe (if any custom tags found)
            if (!customNBT.isEmpty()) {
                recipe.setCustomNBTData(customNBT);
            }
            
        } catch (Exception e) {
            // NBT operations might fail on some versions, continue without NBT data
        }
    }
    
    /**
     * Sets default recipe properties based on imported item
     */
    private static void setDefaultRecipeProperties(ItemStack item, Recipe recipe) {
        // Generate recipe name from item
        String itemName = recipe.getResultName();
        if (itemName.startsWith("&f")) {
            itemName = itemName.substring(2); // Remove color prefix
        }
        recipe.setName("&a" + itemName + " Recipe");
        
        // Set default category based on material type
        String category = determineCategory(item);
        recipe.setCategory(category);
        
        // Enable by default
        recipe.setEnabled(true);
        
        // Add default material requirements (can be edited later)
        Map<String, Integer> defaultRequirements = new HashMap<>();
        defaultRequirements.put("DIAMOND;0", 1); // Placeholder requirement
        recipe.setMaterialRequirements(defaultRequirements);
    }
    
    /**
     * Determines appropriate category based on item type
     */
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
    
    /**
     * Capitalizes the first letter of each word
     */
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
    
    /**
     * Creates a deep copy of an ItemStack preserving all metadata
     * Used for precise item copying to avoid "different item error"
     */
    public static ItemStack createDeepCopy(ItemStack original) {
        if (original == null) return null;
        
        try {
            // Use NBT for most accurate copying
            NBTItem nbtOriginal = new NBTItem(original);
            return nbtOriginal.getItem().clone();
        } catch (Exception e) {
            // Fallback to standard cloning
            return original.clone();
        }
    }
}
