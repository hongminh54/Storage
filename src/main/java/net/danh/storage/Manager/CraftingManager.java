package net.danh.storage.Manager;

import com.cryptomorin.xseries.XEnchantment;
import com.cryptomorin.xseries.XMaterial;
import net.danh.storage.NMS.NMSAssistant;
import net.danh.storage.Recipe.Recipe;
import net.danh.storage.Utils.Chat;
import net.danh.storage.Utils.File;
import net.danh.storage.Utils.SoundContext;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;
import java.util.stream.Collectors;

public class CraftingManager {
    
    private static final Map<String, Recipe> recipes = new HashMap<>();
    private static final Map<String, List<Recipe>> recipesByCategory = new HashMap<>();
    
    public static void loadRecipes() {
        recipes.clear();
        recipesByCategory.clear();
        
        FileConfiguration config = File.getCustomRecipesConfig();
        if (config == null) return;
        
        ConfigurationSection recipesSection = config.getConfigurationSection("recipes");
        if (recipesSection == null) return;
        
        for (String recipeId : recipesSection.getKeys(false)) {
            try {
                ConfigurationSection recipeSection = recipesSection.getConfigurationSection(recipeId);
                if (recipeSection != null) {
                    Recipe recipe = new Recipe(recipeId, recipeSection);
                    recipes.put(recipeId, recipe);

                    String category = recipe.getCategory();
                    recipesByCategory.computeIfAbsent(category, k -> new ArrayList<>()).add(recipe);
                }
            } catch (Exception e) {
                // Log error but continue loading other recipes
                System.err.println("Error loading recipe '" + recipeId + "': " + e.getMessage());
            }
        }
    }
    
    public static void saveRecipes() {
        FileConfiguration config = File.getCustomRecipesConfig();
        if (config == null) return;
        
        // Clear existing recipes
        config.set("recipes", null);
        
        ConfigurationSection recipesSection = config.createSection("recipes");
        for (Recipe recipe : recipes.values()) {
            ConfigurationSection recipeSection = recipesSection.createSection(recipe.getId());
            recipe.saveToConfig(recipeSection);
        }
        
        File.saveCustomRecipesConfig();
    }
    
    public static Recipe getRecipe(String id) {
        return recipes.get(id);
    }
    
    public static Collection<Recipe> getAllRecipes() {
        return recipes.values();
    }
    
    public static List<Recipe> getRecipesByCategory(String category) {
        return recipesByCategory.getOrDefault(category, new ArrayList<>());
    }
    
    public static Set<String> getCategories() {
        return recipesByCategory.keySet();
    }
    
    public static List<Recipe> getAvailableRecipes(Player player) {
        return recipes.values().stream()
                .filter(Recipe::isEnabled)
                .filter(recipe -> hasPermissions(player, recipe))
                .collect(Collectors.toList());
    }
    
    public static boolean canCraft(Player player, String recipeId) {
        Recipe recipe = recipes.get(recipeId);
        if (recipe == null || !recipe.isEnabled()) return false;

        if (!hasPermissions(player, recipe)) return false;

        return hasMaterials(player, recipe);
    }
    
    public static boolean craftRecipe(Player player, String recipeId) {
        return craftRecipe(player, recipeId, 1);
    }

    public static boolean craftRecipe(Player player, String recipeId, int amount) {
        Recipe recipe = recipes.get(recipeId);
        if (recipe == null || !recipe.isEnabled()) return false;

        if (!hasPermissions(player, recipe)) {
            player.sendMessage(Chat.colorize(File.getMessage().getString("recipe.no_permission")
                    .replace("#recipe#", recipe.getName())));
            SoundManager.playSound(player, SoundManager.SoundType.ACTION_ERROR);
            return false;
        }

        // Calculate maximum craftable amount
        int maxCraftable = getMaxCraftableAmount(player, recipe);
        if (maxCraftable <= 0) {
            player.sendMessage(Chat.colorize(File.getMessage().getString("recipe.insufficient_materials")
                    .replace("#recipe#", recipe.getName())));
            SoundManager.playSound(player, SoundManager.SoundType.ACTION_ERROR);
            return false;
        }

        // Limit amount to what's possible
        int actualAmount = Math.min(amount, maxCraftable);

        // Remove materials from storage
        for (Map.Entry<String, Integer> requirement : recipe.getMaterialRequirements().entrySet()) {
            int totalRequired = requirement.getValue() * actualAmount;
            if (!MineManager.removeBlockAmount(player, requirement.getKey(), totalRequired)) {
                return false;
            }
        }

        // Give result items
        ItemStack resultItem = createResultItem(recipe);
        if (resultItem != null) {
            int totalResultAmount = recipe.getResultAmount() * actualAmount;

            // Split into stacks if needed
            while (totalResultAmount > 0) {
                int stackAmount = Math.min(totalResultAmount, resultItem.getMaxStackSize());
                ItemStack stack = resultItem.clone();
                stack.setAmount(stackAmount);
                player.getInventory().addItem(stack);
                totalResultAmount -= stackAmount;
            }

            player.sendMessage(Chat.colorize(File.getMessage().getString("recipe.craft_success")
                    .replace("#recipe#", recipe.getName())
                    .replace("#amount#", String.valueOf(actualAmount))));
            SoundManager.playSound(player, SoundManager.SoundType.ACTION_SUCCESS);
            return true;
        }

        return false;
    }
    
    public static ItemStack createResultItem(Recipe recipe) {
        if (recipe == null) return null;

        // Use the improved ItemImportUtil method for exact item creation
        ItemStack item = net.danh.storage.Utils.ItemImportUtil.createExactItemFromRecipe(recipe);
        if (item == null) return null;
        
        // Apply color codes to display name and lore
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            // Colorize display name
            if (meta.hasDisplayName()) {
                meta.setDisplayName(Chat.colorizewp(meta.getDisplayName()));
            }
            
            // Colorize lore
            if (meta.hasLore() && meta.getLore() != null) {
                List<String> coloredLore = meta.getLore().stream()
                        .map(Chat::colorizewp)
                        .collect(Collectors.toList());
                meta.setLore(coloredLore);
            }
            
            item.setItemMeta(meta);
        }

        return item;
    }
    
    public static void addRecipe(Recipe recipe) {
        recipes.put(recipe.getId(), recipe);
        String category = recipe.getCategory();
        recipesByCategory.computeIfAbsent(category, k -> new ArrayList<>()).add(recipe);
        saveRecipes();
    }
    
    public static void removeRecipe(String id) {
        Recipe recipe = recipes.remove(id);
        if (recipe != null) {
            List<Recipe> categoryRecipes = recipesByCategory.get(recipe.getCategory());
            if (categoryRecipes != null) {
                categoryRecipes.remove(recipe);
                if (categoryRecipes.isEmpty()) {
                    recipesByCategory.remove(recipe.getCategory());
                }
            }
            saveRecipes();
        }
    }
    
    public static void updateRecipe(Recipe recipe) {
        recipes.put(recipe.getId(), recipe);
        
        // Update category mapping
        for (List<Recipe> categoryRecipes : recipesByCategory.values()) {
            categoryRecipes.removeIf(r -> r.getId().equals(recipe.getId()));
        }
        
        String category = recipe.getCategory();
        recipesByCategory.computeIfAbsent(category, k -> new ArrayList<>()).add(recipe);
        saveRecipes();
    }
    
    public static boolean hasPermissions(Player player, Recipe recipe) {
        for (String permission : recipe.getPermissionRequirements()) {
            if (!player.hasPermission(permission)) {
                return false;
            }
        }
        return true;
    }

    public static boolean hasMaterials(Player player, Recipe recipe) {
        return getMaxCraftableAmount(player, recipe) > 0;
    }

    public static int getMaxCraftableAmount(Player player, Recipe recipe) {
        int maxCraftable = Integer.MAX_VALUE;

        for (Map.Entry<String, Integer> requirement : recipe.getMaterialRequirements().entrySet()) {
            int playerAmount = MineManager.getPlayerBlock(player, requirement.getKey());
            int requiredAmount = requirement.getValue();

            if (requiredAmount <= 0) continue;

            int possibleCrafts = playerAmount / requiredAmount;
            maxCraftable = Math.min(maxCraftable, possibleCrafts);
        }

        return maxCraftable == Integer.MAX_VALUE ? 0 : maxCraftable;
    }

    public static void requestCraftAmount(Player player, String recipeId) {
        Recipe recipe = recipes.get(recipeId);
        if (recipe == null || !recipe.isEnabled()) {
            player.sendMessage(Chat.colorize(File.getMessage().getString("recipe.recipe_not_found")
                    .replace("#recipe#", recipeId)));
            SoundManager.playSound(player, SoundManager.SoundType.ACTION_ERROR);
            return;
        }

        if (!hasPermissions(player, recipe)) {
            player.sendMessage(Chat.colorize(File.getMessage().getString("recipe.no_permission")
                    .replace("#recipe#", recipe.getName())));
            SoundManager.playSound(player, SoundManager.SoundType.ACTION_ERROR);
            return;
        }

        int maxCraftable = getMaxCraftableAmount(player, recipe);
        if (maxCraftable <= 0) {
            player.sendMessage(Chat.colorize(File.getMessage().getString("recipe.insufficient_materials")
                    .replace("#recipe#", recipe.getName())));
            return;
        }

        net.danh.storage.Listeners.Chat.chat_recipe_craft.put(player, recipeId);
        player.closeInventory();
        player.sendMessage(Chat.colorize(File.getMessage().getString("recipe.enter_craft_amount")
                .replace("#max#", String.valueOf(maxCraftable))));
    }

    public static void handleCraftAmountInput(Player player, String recipeId, String input) {
        try {
            int amount = Integer.parseInt(input.trim());

            if (amount <= 0 || amount > 999) {
                player.sendMessage(Chat.colorize(File.getMessage().getString("recipe.invalid_craft_amount")));
                SoundManager.playSound(player, SoundManager.SoundType.ACTION_ERROR);
                return;
            }

            Recipe recipe = recipes.get(recipeId);
            if (recipe == null) {
                player.sendMessage(Chat.colorize(File.getMessage().getString("recipe.recipe_not_found")
                        .replace("#recipe#", recipeId)));
                SoundManager.playSound(player, SoundManager.SoundType.ACTION_ERROR);
                return;
            }

            if (craftRecipe(player, recipeId, amount)) {
                // Success message is handled in craftRecipe method
                player.openInventory(new net.danh.storage.GUI.RecipeListGUI(player, 0, "all").getInventory(SoundContext.SILENT));
            }

        } catch (NumberFormatException e) {
            player.sendMessage(Chat.colorize(File.getMessage().getString("recipe.invalid_craft_amount")));
            SoundManager.playSound(player, SoundManager.SoundType.ACTION_ERROR);
        }
    }
    
    public static String generateUniqueId() {
        String baseId = "recipe_" + System.currentTimeMillis();
        int counter = 1;
        String id = baseId;
        
        while (recipes.containsKey(id)) {
            id = baseId + "_" + counter;
            counter++;
        }
        
        return id;
    }
    
    /**
     * Creates an ItemStack with proper data value handling for cross-version compatibility
     * This method handles the core issue where legacy items with data values were not created correctly
     */
    private static ItemStack createItemWithDataValue(String materialName, short dataValue) {
        NMSAssistant nms = new NMSAssistant();
        
        try {
            // For legacy versions (<=1.12.2), handle data values properly
            if (nms.isVersionLessThanOrEqualTo(12)) {
                // Try to get material by name first
                Material material;
                try {
                    material = Material.valueOf(materialName);
                } catch (IllegalArgumentException e) {
                    // If material name doesn't exist, try XMaterial fallback
                    Optional<XMaterial> xMaterial = XMaterial.matchXMaterial(materialName);
                    if (xMaterial.isPresent()) {
                        ItemStack item = xMaterial.get().parseItem();
                        if (item != null && dataValue != 0) {
                            item.setDurability(dataValue);
                        }
                        return item;
                    }
                    return null;
                }
                
                // Create ItemStack with material and set data value via durability
                ItemStack item = new ItemStack(material, 1);
                if (dataValue != 0) {
                    item.setDurability(dataValue);
                }
                
                System.out.println("[CraftingManager] Legacy item created - Material: " + materialName + 
                                 ", Data: " + dataValue + ", Result: " + item.getType().name() + 
                                 " with durability " + item.getDurability());
                
                return item;
            } else {
                // For modern versions (>=1.13), use XMaterial for better compatibility
                Optional<XMaterial> xMaterial = XMaterial.matchXMaterial(materialName);
                if (xMaterial.isPresent()) {
                    return xMaterial.get().parseItem();
                } else {
                    // Fallback to direct material creation
                    try {
                        Material material = Material.valueOf(materialName);
                        return new ItemStack(material, 1);
                    } catch (IllegalArgumentException e) {
                        return null;
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("[CraftingManager] Error creating item with material: " + materialName + 
                             ", data: " + dataValue + " - " + e.getMessage());
            
            // Final fallback to stone
            Optional<XMaterial> stoneMaterial = XMaterial.matchXMaterial("STONE");
            return stoneMaterial.map(XMaterial::parseItem).orElse(null);
        }
    }
}
