package net.danh.storage.Manager;

import net.danh.storage.Recipe.Recipe;
import net.danh.storage.Storage;
import net.danh.storage.Utils.Chat;
import net.danh.storage.Utils.File;
import net.danh.storage.Utils.ItemImportUtil;
import net.danh.storage.Utils.SoundContext;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Optimized manager for custom recipe system with simple inventory checking
 */
public class CraftingManager {
    
    private static final Map<String, Recipe> recipes = new HashMap<>();
    private static final Map<String, List<Recipe>> recipesByCategory = new HashMap<>();
    private static final Map<String, BukkitRunnable> activeCrafting = new HashMap<>();
    
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
                    addRecipeToMaps(recipe);
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
    
    // Public API methods
    
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
    
    // Crafting methods
    
    public static boolean canCraft(Player player, String recipeId) {
        Recipe recipe = recipes.get(recipeId);
        return recipe != null && recipe.isEnabled() && 
               hasPermissions(player, recipe) && hasMaterials(player, recipe);
    }
    
    public static boolean craftRecipe(Player player, String recipeId) {
        return craftRecipe(player, recipeId, 1);
    }

    public static boolean craftRecipe(Player player, String recipeId, int amount) {
        Recipe recipe = recipes.get(recipeId);
        if (recipe == null || !recipe.isEnabled()) return false;

        if (!hasPermissions(player, recipe)) {
            sendMessage(player, "recipe.no_permission", "#recipe#", recipe.getName());
            SoundManager.playSound(player, SoundManager.SoundType.ACTION_ERROR);
            return false;
        }

        // Check if player already has crafting in progress
        if (isCraftingInProgress(player)) {
            sendMessage(player, "recipe.craft_in_progress");
            SoundManager.playSound(player, SoundManager.SoundType.ACTION_ERROR);
            return false;
        }

        int maxCraftable = getMaxCraftableAmount(player, recipe);
        if (maxCraftable <= 0) {
            sendMessage(player, "recipe.insufficient_materials", "#recipe#", recipe.getName());
            SoundManager.playSound(player, SoundManager.SoundType.ACTION_ERROR);
            return false;
        }

        int actualAmount = Math.min(amount, maxCraftable);

        // Check if inventory has space before removing materials
        ItemStack resultItem = createResultItem(recipe);
        if (resultItem == null) {
            sendMessage(player, "recipe.craft_failed", "#recipe#", recipe.getName());
            SoundManager.playSound(player, SoundManager.SoundType.ACTION_ERROR);
            return false;
        }

        // Check if inventory has enough space for result items
        int totalItemsNeeded = recipe.getResultAmount() * actualAmount;
        int availableSpace = calculateInventorySpace(player, resultItem);

        if (availableSpace < totalItemsNeeded) {
            sendMessage(player, "recipe.inventory_full", "#recipe#", recipe.getName());
            SoundManager.playSound(player, SoundManager.SoundType.ACTION_ERROR);
            return false;
        }

        // Check if crafting delay is enabled
        FileConfiguration config = File.getConfig();
        if (config.getBoolean("crafting.enabled", true)) {
            int craftingDelay = config.getInt("crafting.delay", 3);
            if (craftingDelay > 0) {
                // Start crafting process with delay
                startCraftingProcess(player, recipe, actualAmount);
                return true;
            }
        }

        // Immediate crafting (no delay)
        return completeCrafting(player, recipe, actualAmount);
    }
    
    /**
     * Creates result item with proper colorization
     */
    public static ItemStack createResultItem(Recipe recipe) {
        if (recipe == null) return null;

        ItemStack item = ItemImportUtil.createExactItemFromRecipe(recipe);
        if (item == null) return null;
        
        // Apply color codes efficiently
        colorizeItem(item);
        return item;
    }
    
    // Recipe management methods
    
    public static void addRecipe(Recipe recipe) {
        addRecipeToMaps(recipe);
        saveRecipes();
    }
    
    public static void removeRecipe(String id) {
        Recipe recipe = recipes.remove(id);
        if (recipe != null) {
            removeRecipeFromCategory(recipe);
            saveRecipes();
        }
    }
    
    public static void updateRecipe(Recipe recipe) {
        // Remove from old category
        removeRecipeFromAllCategories(recipe.getId());
        
        // Add to new category
        addRecipeToMaps(recipe);
        saveRecipes();
    }
    
    // Permission and material checking
    
    public static boolean hasPermissions(Player player, Recipe recipe) {
        return recipe.getPermissionRequirements().stream()
                .allMatch(player::hasPermission);
    }

    public static boolean hasMaterials(Player player, Recipe recipe) {
        return getMaxCraftableAmount(player, recipe) > 0;
    }

    public static int getMaxCraftableAmount(Player player, Recipe recipe) {
        return recipe.getMaterialRequirements().entrySet().stream()
                .filter(entry -> entry.getValue() > 0)
                .mapToInt(entry -> {
                    int playerAmount = MineManager.getPlayerBlock(player, entry.getKey());
                    return playerAmount / entry.getValue();
                })
                .min()
                .orElse(0);
    }

    // Chat input handling
    
    public static void requestCraftAmount(Player player, String recipeId) {
        Recipe recipe = recipes.get(recipeId);
        if (recipe == null || !recipe.isEnabled()) {
            sendMessage(player, "recipe.recipe_not_found", "#recipe#", recipeId);
            SoundManager.playSound(player, SoundManager.SoundType.ACTION_ERROR);
            return;
        }

        if (!hasPermissions(player, recipe)) {
            sendMessage(player, "recipe.no_permission", "#recipe#", recipe.getName());
            SoundManager.playSound(player, SoundManager.SoundType.ACTION_ERROR);
            return;
        }

        int maxCraftable = getMaxCraftableAmount(player, recipe);
        if (maxCraftable <= 0) {
            sendMessage(player, "recipe.insufficient_materials", "#recipe#", recipe.getName());
            return;
        }

        net.danh.storage.Listeners.Chat.chat_recipe_craft.put(player, recipeId);
        player.closeInventory();
        sendMessage(player, "recipe.enter_craft_amount", "#max#", String.valueOf(maxCraftable));
    }

    public static void handleCraftAmountInput(Player player, String recipeId, String input) {
        try {
            int amount = Integer.parseInt(input.trim());

            if (amount <= 0 || amount > 999) {
                sendMessage(player, "recipe.invalid_craft_amount");
                SoundManager.playSound(player, SoundManager.SoundType.ACTION_ERROR);
                return;
            }

            Recipe recipe = recipes.get(recipeId);
            if (recipe == null) {
                sendMessage(player, "recipe.recipe_not_found", "#recipe#", recipeId);
                SoundManager.playSound(player, SoundManager.SoundType.ACTION_ERROR);
                return;
            }

            if (craftRecipe(player, recipeId, amount)) {
                // Only reopen GUI if crafting was completed immediately (no delay)
                if (!isCraftingInProgress(player)) {
                    player.openInventory(new net.danh.storage.GUI.RecipeListGUI(player, 0, "all")
                            .getInventory(SoundContext.SILENT));
                }
                // If crafting is in progress, GUI will remain closed during delay
            }

        } catch (NumberFormatException e) {
            sendMessage(player, "recipe.invalid_craft_amount");
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
    
    // Crafting delay system methods
    
    public static boolean isCraftingInProgress(Player player) {
        return activeCrafting.containsKey(player.getName());
    }
    
    public static void cancelCrafting(Player player) {
        BukkitRunnable task = activeCrafting.remove(player.getName());
        if (task != null) {
            task.cancel();
            sendMessage(player, "recipe.craft_cancelled");
        }
        // Stop any processing animation
        ParticleManager.stopCraftingProcessingAnimation(player);
    }
    
    public static void cancelAllCrafting() {
        for (BukkitRunnable task : activeCrafting.values()) {
            task.cancel();
        }
        activeCrafting.clear();
    }
    
    private static void startCraftingProcess(Player player, Recipe recipe, int amount) {
        FileConfiguration config = File.getConfig();
        int craftingDelay = config.getInt("crafting.delay", 3);
        
        // Notify player that crafting is starting
        sendMessage(player, "recipe.processing", 
            new String[]{"#amount#", "#recipe#", "#time#"}, 
            new String[]{String.valueOf(amount), recipe.getName(), String.valueOf(craftingDelay)});
        
        // Cancel any existing crafting for this player
        cancelCrafting(player);
        
        // Start processing animation
        ParticleManager.playCraftingProcessingAnimation(player, craftingDelay);
        
        // Create and start the crafting task
        BukkitRunnable craftingTask = new BukkitRunnable() {
            @Override
            public void run() {
                // Stop processing animation
                ParticleManager.stopCraftingProcessingAnimation(player);
                
                if (completeCrafting(player, recipe, amount)) {
                    // Play success particles and sounds
                    ParticleManager.playCraftingSuccessParticle(player);
                    playCraftingSuccessSound(player);
                } else {
                    // Play failed particles and sounds
                    ParticleManager.playCraftingFailedParticle(player);
                    playCraftingFailedSound(player);
                }
                
                activeCrafting.remove(player.getName());
            }
        };
        
        activeCrafting.put(player.getName(), craftingTask);
        craftingTask.runTaskLater(Storage.getStorage(), craftingDelay * 20L); // Convert seconds to ticks
    }
    
    private static boolean completeCrafting(Player player, Recipe recipe, int amount) {
        // Double-check conditions before completing crafting
        if (!player.isOnline()) {
            return false;
        }
        
        // Re-check materials (player might have used them during delay)
        int maxCraftable = getMaxCraftableAmount(player, recipe);
        if (maxCraftable < amount) {
            sendMessage(player, "recipe.processing_failed", "#recipe#", recipe.getName());
            return false;
        }
        
        // Re-check inventory space
        ItemStack resultItem = createResultItem(recipe);
        if (resultItem == null) {
            sendMessage(player, "recipe.craft_failed", "#recipe#", recipe.getName());
            return false;
        }
        
        int totalItemsNeeded = recipe.getResultAmount() * amount;
        int availableSpace = calculateInventorySpace(player, resultItem);
        
        if (availableSpace < totalItemsNeeded) {
            sendMessage(player, "recipe.inventory_full", "#recipe#", recipe.getName());
            return false;
        }
        
        // Remove materials from storage
        if (!removeMaterials(player, recipe, amount)) {
            return false;
        }
        
        // Give result items
        giveResultItems(player, resultItem, recipe.getResultAmount() * amount);
        
        sendMessage(player, "recipe.craft_success", 
            new String[]{"#recipe#", "#amount#"}, 
            new String[]{recipe.getName(), String.valueOf(amount)});
        
        return true;
    }
    
    private static void playCraftingSuccessSound(Player player) {
        FileConfiguration config = File.getConfig();
        if (config.getBoolean("crafting.sounds.enabled", true)) {
            String soundName = config.getString("crafting.sounds.success.name");
            if (soundName != null && !soundName.equalsIgnoreCase("none")) {
                float volume = (float) config.getDouble("crafting.sounds.success.volume", 0.7);
                float pitch = (float) config.getDouble("crafting.sounds.success.pitch", 1.2);
                SoundManager.playSound(player, soundName, volume, pitch);
            }
        }
    }
    
    private static void playCraftingFailedSound(Player player) {
        FileConfiguration config = File.getConfig();
        if (config.getBoolean("crafting.sounds.enabled", true)) {
            String soundName = config.getString("crafting.sounds.failed.name");
            if (soundName != null && !soundName.equalsIgnoreCase("none")) {
                float volume = (float) config.getDouble("crafting.sounds.failed.volume", 0.8);
                float pitch = (float) config.getDouble("crafting.sounds.failed.pitch", 0.8);
                SoundManager.playSound(player, soundName, volume, pitch);
            }
        }
    }

    // Private helper methods
    
    private static void addRecipeToMaps(Recipe recipe) {
        recipes.put(recipe.getId(), recipe);
        String category = recipe.getCategory();
        recipesByCategory.computeIfAbsent(category, k -> new ArrayList<>()).add(recipe);
    }
    
    private static void removeRecipeFromCategory(Recipe recipe) {
        List<Recipe> categoryRecipes = recipesByCategory.get(recipe.getCategory());
        if (categoryRecipes != null) {
            categoryRecipes.remove(recipe);
            if (categoryRecipes.isEmpty()) {
                recipesByCategory.remove(recipe.getCategory());
            }
        }
    }
    
    private static void removeRecipeFromAllCategories(String recipeId) {
        for (List<Recipe> categoryRecipes : recipesByCategory.values()) {
            categoryRecipes.removeIf(r -> r.getId().equals(recipeId));
        }
    }
    
    private static boolean removeMaterials(Player player, Recipe recipe, int amount) {
        for (Map.Entry<String, Integer> requirement : recipe.getMaterialRequirements().entrySet()) {
            int totalRequired = requirement.getValue() * amount;
            if (!MineManager.removeBlockAmount(player, requirement.getKey(), totalRequired)) {
                return false;
            }
        }
        return true;
    }
    

    
    /**
     * Give items to player using Bukkit's addItem method
     * Simple and reliable approach
     */
    private static void giveResultItems(Player player, ItemStack item, int amount) {
        int remaining = amount;
        int maxStackSize = item.getMaxStackSize();

        while (remaining > 0) {
            int stackAmount = Math.min(remaining, maxStackSize);
            ItemStack stack = item.clone();
            stack.setAmount(stackAmount);
            player.getInventory().addItem(stack);
            remaining -= stackAmount;
        }
    }

    /**
     * Calculate available inventory space for specific item
     */
    private static int calculateInventorySpace(Player player, ItemStack itemStack) {
        int availableSpace = 0;
        ItemStack template = itemStack.clone();
        template.setAmount(1);

        for (ItemStack slot : player.getInventory().getStorageContents()) {
            if (slot == null || slot.getType() == Material.AIR) {
                // Empty slot can hold full stack
                availableSpace += template.getMaxStackSize();
            } else if (slot.isSimilar(template)) {
                // Existing similar item can be stacked
                int spaceLeft = slot.getMaxStackSize() - slot.getAmount();
                if (spaceLeft > 0) {
                    availableSpace += spaceLeft;
                }
            }
        }

        return availableSpace;
    }

    private static void colorizeItem(ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;
        
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
    
    private static void sendMessage(Player player, String messageKey) {
        String message = File.getMessage().getString(messageKey);
        if (message != null) {
            player.sendMessage(Chat.colorize(message));
        }
    }
    
    private static void sendMessage(Player player, String messageKey, String placeholder, String replacement) {
        String message = File.getMessage().getString(messageKey);
        if (message != null) {
            player.sendMessage(Chat.colorize(message.replace(placeholder, replacement)));
        }
    }
    
    private static void sendMessage(Player player, String messageKey, String[] placeholders, String[] replacements) {
        String message = File.getMessage().getString(messageKey);
        if (message != null) {
            for (int i = 0; i < placeholders.length && i < replacements.length; i++) {
                message = message.replace(placeholders[i], replacements[i]);
            }
            player.sendMessage(Chat.colorize(message));
        }
    }
}