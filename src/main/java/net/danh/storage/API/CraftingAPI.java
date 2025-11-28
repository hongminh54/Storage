package net.danh.storage.API;

import net.danh.storage.Manager.CraftingManager;
import net.danh.storage.Recipe.Recipe;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * API for Custom Crafting System
 * Provides methods for external plugins to interact with custom recipes
 *
 * @author hongminh54
 * @version 2.3.4
 */
public class CraftingAPI {

    // ==================== RECIPE MANAGEMENT ====================

    /**
     * Get recipe by ID
     *
     * @param recipeId Recipe ID
     * @return Recipe or null if not found
     */
    @Nullable
    public static Recipe getRecipe(@NotNull String recipeId) {
        return CraftingManager.getRecipe(recipeId);
    }

    /**
     * Get all registered recipes
     *
     * @return Collection of all recipes
     */
    @NotNull
    public static Collection<Recipe> getAllRecipes() {
        return CraftingManager.getAllRecipes();
    }

    /**
     * Get recipes by category
     *
     * @param category Category name
     * @return List of recipes in category
     */
    @NotNull
    public static List<Recipe> getRecipesByCategory(@NotNull String category) {
        return CraftingManager.getRecipesByCategory(category);
    }

    /**
     * Get all recipe categories
     *
     * @return Set of category names
     */
    @NotNull
    public static Set<String> getCategories() {
        return CraftingManager.getCategories();
    }

    /**
     * Get recipes available to player (enabled + has permission)
     *
     * @param player The player
     * @return List of available recipes
     */
    @NotNull
    public static List<Recipe> getAvailableRecipes(@NotNull Player player) {
        return CraftingManager.getAvailableRecipes(player);
    }

    /**
     * Get recipes player can craft (has materials)
     *
     * @param player The player
     * @return List of craftable recipes
     */
    @NotNull
    public static List<Recipe> getCraftableRecipes(@NotNull Player player) {
        return CraftingManager.getCraftableRecipes(player);
    }

    /**
     * Check if recipe exists
     *
     * @param recipeId Recipe ID
     * @return true if exists
     */
    public static boolean recipeExists(@NotNull String recipeId) {
        return CraftingManager.recipeExists(recipeId);
    }

    /**
     * Add new recipe programmatically
     *
     * @param recipe Recipe to add
     * @return true if successfully added
     */
    public static boolean addRecipe(@NotNull Recipe recipe) {
        CraftingManager.addRecipe(recipe);
        return true;
    }

    /**
     * Remove recipe
     *
     * @param recipeId Recipe ID to remove
     * @return true if successfully removed
     */
    public static boolean removeRecipe(@NotNull String recipeId) {
        return CraftingManager.removeRecipe(recipeId);
    }

    /**
     * Update existing recipe
     *
     * @param recipe Recipe to update
     */
    public static void updateRecipe(@NotNull Recipe recipe) {
        CraftingManager.updateRecipe(recipe);
    }

    /**
     * Duplicate recipe with new ID
     *
     * @param sourceId Source recipe ID
     * @param newId    New recipe ID
     * @return Duplicated recipe or null if failed
     */
    @Nullable
    public static Recipe duplicateRecipe(@NotNull String sourceId, @NotNull String newId) {
        return CraftingManager.duplicateRecipe(sourceId, newId);
    }

    // ==================== CRAFTING OPERATIONS ====================

    /**
     * Check if player can craft recipe
     *
     * @param player   The player
     * @param recipeId Recipe ID
     * @return true if can craft
     */
    public static boolean canCraft(@NotNull Player player, @NotNull String recipeId) {
        return CraftingManager.canCraft(player, recipeId);
    }

    /**
     * Craft recipe for player (amount = 1)
     *
     * @param player   The player
     * @param recipeId Recipe ID
     * @return true if crafting started/completed
     */
    public static boolean craftRecipe(@NotNull Player player, @NotNull String recipeId) {
        return CraftingManager.craftRecipe(player, recipeId);
    }

    /**
     * Craft recipe with specific amount
     *
     * @param player   The player
     * @param recipeId Recipe ID
     * @param amount   Amount to craft
     * @return true if crafting started/completed
     */
    public static boolean craftRecipe(@NotNull Player player, @NotNull String recipeId, int amount) {
        return CraftingManager.craftRecipe(player, recipeId, amount);
    }

    /**
     * Get maximum craftable amount for recipe
     *
     * @param player The player
     * @param recipe The recipe
     * @return Maximum amount player can craft
     */
    public static int getMaxCraftableAmount(@NotNull Player player, @NotNull Recipe recipe) {
        return CraftingManager.getMaxCraftableAmount(player, recipe);
    }

    /**
     * Check if player is currently crafting
     *
     * @param player The player
     * @return true if crafting in progress
     */
    public static boolean isCraftingInProgress(@NotNull Player player) {
        return CraftingManager.isCraftingInProgress(player);
    }

    /**
     * Cancel player's current crafting
     *
     * @param player The player
     * @return true if cancelled
     */
    public static boolean cancelCrafting(@NotNull Player player) {
        return CraftingManager.cancelCrafting(player);
    }

    /**
     * Create result ItemStack from recipe
     *
     * @param recipe The recipe
     * @return Result ItemStack or null if failed
     */
    @Nullable
    public static ItemStack createResultItem(@NotNull Recipe recipe) {
        return CraftingManager.createResultItem(recipe);
    }

    // ==================== STATISTICS ====================

    /**
     * Get total number of recipes
     *
     * @return Total recipes
     */
    public static int getTotalRecipes() {
        return CraftingManager.getTotalRecipes();
    }

    /**
     * Get number of enabled recipes
     *
     * @return Enabled recipes count
     */
    public static int getEnabledRecipesCount() {
        return CraftingManager.getEnabledRecipesCount();
    }

    /**
     * Get number of recipes in category
     *
     * @param category Category name
     * @return Recipe count in category
     */
    public static int getRecipeCountByCategory(@NotNull String category) {
        return CraftingManager.getRecipesByCategory(category).size();
    }

    // ==================== UTILITY ====================

    /**
     * Generate unique recipe ID
     *
     * @return Unique ID string
     */
    @NotNull
    public static String generateUniqueId() {
        return CraftingManager.generateUniqueId();
    }

    /**
     * Reload all recipes from config
     */
    public static void reloadRecipes() {
        CraftingManager.loadRecipes();
    }

    /**
     * Save all recipes to config
     */
    public static void saveRecipes() {
        CraftingManager.saveRecipes();
    }
}
