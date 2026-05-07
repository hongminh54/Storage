package net.danh.storage.Placeholder;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import net.danh.storage.Manager.Crafting.CraftingManager;
import net.danh.storage.Recipe.Recipe;
import net.danh.storage.Storage;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class CraftingPlaceholder extends PlaceholderExpansion {

    private final Storage plugin;

    public CraftingPlaceholder(Storage plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "storagecraft";
    }

    @Override
    public @NotNull String getAuthor() {
        return plugin.getDescription().getAuthors().toString();
    }

    @Override
    public @NotNull String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onPlaceholderRequest(Player player, @NotNull String params) {
        if (player == null) {
            return "";
        }

        // %storagecraft_total_recipes%
        if (params.equalsIgnoreCase("total_recipes")) {
            return String.valueOf(CraftingManager.getTotalRecipes());
        }

        // %storagecraft_enabled_recipes%
        if (params.equalsIgnoreCase("enabled_recipes")) {
            return String.valueOf(CraftingManager.getEnabledRecipesCount());
        }

        // %storagecraft_available_recipes%
        if (params.equalsIgnoreCase("available_recipes")) {
            return String.valueOf(CraftingManager.getAvailableRecipes(player).size());
        }

        // %storagecraft_craftable_recipes%
        if (params.equalsIgnoreCase("craftable_recipes")) {
            return String.valueOf(CraftingManager.getCraftableRecipes(player).size());
        }

        // %storagecraft_is_crafting%
        if (params.equalsIgnoreCase("is_crafting")) {
            return Boolean.toString(CraftingManager.isCraftingInProgress(player));
        }

        // %storagecraft_recipe_<recipeId>_exists%
        if (params.startsWith("recipe_") && params.endsWith("_exists")) {
            String recipeId = params.substring(7, params.length() - 7);
            return Boolean.toString(CraftingManager.recipeExists(recipeId));
        }

        // %storagecraft_recipe_<recipeId>_enabled%
        if (params.startsWith("recipe_") && params.endsWith("_enabled")) {
            String recipeId = params.substring(7, params.length() - 8);
            Recipe recipe = CraftingManager.getRecipe(recipeId);
            return Boolean.toString(recipe != null && recipe.isEnabled());
        }

        // %storagecraft_recipe_<recipeId>_can_craft%
        if (params.startsWith("recipe_") && params.endsWith("_can_craft")) {
            String recipeId = params.substring(7, params.length() - 10);
            return Boolean.toString(CraftingManager.canCraft(player, recipeId));
        }

        // %storagecraft_recipe_<recipeId>_max_amount%
        if (params.startsWith("recipe_") && params.endsWith("_max_amount")) {
            String recipeId = params.substring(7, params.length() - 11);
            Recipe recipe = CraftingManager.getRecipe(recipeId);
            if (recipe != null) {
                return String.valueOf(CraftingManager.getMaxCraftableAmount(player, recipe));
            }
        }

        // %storagecraft_recipe_<recipeId>_name%
        if (params.startsWith("recipe_") && params.endsWith("_name")) {
            String recipeId = params.substring(7, params.length() - 5);
            Recipe recipe = CraftingManager.getRecipe(recipeId);
            return recipe != null ? recipe.getName() : "Unknown";
        }

        return null;
    }
}
