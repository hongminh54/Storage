package net.danh.storage.Manager;

import com.cryptomorin.xseries.XEnchantment;
import com.cryptomorin.xseries.XMaterial;
import net.danh.storage.Recipe.Recipe;
import net.danh.storage.Utils.Chat;
import net.danh.storage.Utils.File;
import net.danh.storage.Utils.SoundContext;
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

        String materialData = recipe.getResultMaterial();
        if (materialData == null || materialData.isEmpty()) {
            materialData = "STONE;0";
        }

        String[] parts = materialData.split(";");
        String materialName = parts[0];

        Optional<XMaterial> xMaterial = XMaterial.matchXMaterial(materialName);
        if (!xMaterial.isPresent()) {
            // Fallback to stone if material is invalid
            xMaterial = XMaterial.matchXMaterial("STONE");
            if (!xMaterial.isPresent()) return null;
        }

        ItemStack item = xMaterial.get().parseItem();
        if (item == null) return null;
        
        item.setAmount(recipe.getResultAmount());
        
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;
        
        // Set display name
        if (recipe.getResultName() != null && !recipe.getResultName().isEmpty()) {
            meta.setDisplayName(Chat.colorizewp(recipe.getResultName()));
        }
        
        // Set lore
        if (!recipe.getResultLore().isEmpty()) {
            List<String> coloredLore = recipe.getResultLore().stream()
                    .map(Chat::colorizewp)
                    .collect(Collectors.toList());
            meta.setLore(coloredLore);
        }
        
        // Set unbreakable
        meta.setUnbreakable(recipe.isResultUnbreakable());
        
        // Set custom model data
        if (recipe.getResultCustomModelData() > 0) {
            try {
                meta.setCustomModelData(recipe.getResultCustomModelData());
            } catch (NoSuchMethodError ignored) {
                // For older versions that don't support custom model data
            }
        }
        
        // Add enchantments
        for (Map.Entry<String, Integer> enchant : recipe.getResultEnchantments().entrySet()) {
            Optional<XEnchantment> xEnchant = XEnchantment.matchXEnchantment(enchant.getKey());
            if (xEnchant.isPresent() && xEnchant.get().getEnchant() != null) {
                meta.addEnchant(xEnchant.get().getEnchant(), enchant.getValue(), true);
            }
        }
        
        // Add item flags
        for (ItemFlag flag : recipe.getResultFlags()) {
            meta.addItemFlags(flag);
        }
        
        item.setItemMeta(meta);

        // Apply custom NBT data for plugin compatibility
        if (!recipe.getCustomNBTData().isEmpty()) {
            try {
                de.tr7zw.changeme.nbtapi.NBTItem nbtItem = new de.tr7zw.changeme.nbtapi.NBTItem(item);
                for (Map.Entry<String, String> entry : recipe.getCustomNBTData().entrySet()) {
                    nbtItem.setString(entry.getKey(), entry.getValue());
                }
                item = nbtItem.getItem();
            } catch (Exception e) {
                // NBT operations might fail on some versions, continue without NBT data
            }
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
            return;
        }

        if (!hasPermissions(player, recipe)) {
            player.sendMessage(Chat.colorize(File.getMessage().getString("recipe.no_permission")
                    .replace("#recipe#", recipe.getName())));
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
                return;
            }

            Recipe recipe = recipes.get(recipeId);
            if (recipe == null) {
                player.sendMessage(Chat.colorize(File.getMessage().getString("recipe.recipe_not_found")
                        .replace("#recipe#", recipeId)));
                return;
            }

            if (craftRecipe(player, recipeId, amount)) {
                // Success message is handled in craftRecipe method
                player.openInventory(new net.danh.storage.GUI.RecipeListGUI(player, 0, "all").getInventory(SoundContext.SILENT));
            }

        } catch (NumberFormatException e) {
            player.sendMessage(Chat.colorize(File.getMessage().getString("recipe.invalid_craft_amount")));
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
}
