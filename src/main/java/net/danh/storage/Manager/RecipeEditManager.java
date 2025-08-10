package net.danh.storage.Manager;

import com.cryptomorin.xseries.XEnchantment;
import com.cryptomorin.xseries.XMaterial;
import net.danh.storage.GUI.RecipeEditorGUI;
import net.danh.storage.Recipe.Recipe;
import net.danh.storage.Utils.Chat;
import net.danh.storage.Utils.File;
import net.danh.storage.Utils.Number;
import net.danh.storage.Utils.SoundContext;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;

import java.util.*;

public class RecipeEditManager {

    public static void handleChatInput(Player player, String editType, String recipeId, String field, String input) {
        Recipe recipe = CraftingManager.getRecipe(recipeId);
        if (recipe == null) {
            player.sendMessage(Chat.colorize(File.getMessage().getString("recipe.recipe_not_found")
                    .replace("#recipe#", recipeId)));
            SoundManager.playSound(player, SoundManager.SoundType.ACTION_ERROR);
            return;
        }

        try {
            switch (editType) {
                case "name":
                    handleNameEdit(player, recipe, input);
                    break;
                case "lore_add":
                    handleLoreAdd(player, recipe, input);
                    break;
                case "amount":
                    handleAmountEdit(player, recipe, input);
                    break;
                case "material":
                    handleMaterialEdit(player, recipe, input);
                    break;
                case "enchant_add":
                    handleEnchantAdd(player, recipe, field, input);
                    break;
                case "requirement_add":
                    handleRequirementAdd(player, recipe, input);
                    break;
                case "requirement_amount":
                    handleRequirementAmountEdit(player, recipe, field, input);
                    break;
                case "category":
                    handleCategoryEdit(player, recipe, input);
                    break;
                case "recipe_name":
                    handleRecipeNameEdit(player, recipe, input);
                    break;
                case "permission_add":
                    handlePermissionAdd(player, recipe, input);
                    break;
                default:
                    player.sendMessage(Chat.colorize("&cUnknown edit type: " + editType));
                    SoundManager.playSound(player, SoundManager.SoundType.ACTION_ERROR);
                    return;
            }

            // Save recipe and refresh GUI
            CraftingManager.updateRecipe(recipe);
            SoundManager.setShouldPlayCloseSound(player, false);
            player.openInventory(new RecipeEditorGUI(player, recipe).getInventory(SoundContext.SILENT));

        } catch (Exception e) {
            player.sendMessage(Chat.colorize("&cError processing input: " + e.getMessage()));
            SoundManager.playSound(player, SoundManager.SoundType.ACTION_ERROR);
            SoundManager.setShouldPlayCloseSound(player, false);
            player.openInventory(new RecipeEditorGUI(player, recipe).getInventory(SoundContext.SILENT));
        }
    }

    private static void handleNameEdit(Player player, Recipe recipe, String input) {
        if (input.trim().isEmpty()) {
            player.sendMessage(Chat.colorize("&cName cannot be empty!"));
            SoundManager.playSound(player, SoundManager.SoundType.ACTION_ERROR);
            return;
        }
        
        recipe.setResultName(Chat.colorizewp(input));
        player.sendMessage(Chat.colorize("&aResult item name updated to: " + input));
        SoundManager.playSound(player, SoundManager.SoundType.ACTION_SUCCESS);
    }

    private static void handleLoreAdd(Player player, Recipe recipe, String input) {
        if (input.trim().isEmpty()) {
            player.sendMessage(Chat.colorize("&cLore line cannot be empty!"));
            SoundManager.playSound(player, SoundManager.SoundType.ACTION_ERROR);
            return;
        }
        
        List<String> lore = new ArrayList<>(recipe.getResultLore());
        lore.add(input);
        recipe.setResultLore(lore);
        player.sendMessage(Chat.colorize("&aLore line added: " + input));
        SoundManager.playSound(player, SoundManager.SoundType.ACTION_SUCCESS);
    }

    private static void handleAmountEdit(Player player, Recipe recipe, String input) {
        int amount = Number.getInteger(input);
        if (amount <= 0 || amount > 64) {
            player.sendMessage(Chat.colorize(File.getMessage().getString("recipe.invalid_amount")
                    .replace("#amount#", input)));
            SoundManager.playSound(player, SoundManager.SoundType.ACTION_ERROR);
            return;
        }
        
        recipe.setResultAmount(amount);
        player.sendMessage(Chat.colorize("&aResult amount updated to: " + amount));
        SoundManager.playSound(player, SoundManager.SoundType.ACTION_SUCCESS);
    }

    private static void handleMaterialEdit(Player player, Recipe recipe, String input) {
        String materialName = input.toUpperCase().trim();
        
        Optional<XMaterial> xMaterial = XMaterial.matchXMaterial(materialName);
        if (!xMaterial.isPresent()) {
            player.sendMessage(Chat.colorize(File.getMessage().getString("recipe.invalid_material")
                    .replace("#material#", input)));
            SoundManager.playSound(player, SoundManager.SoundType.ACTION_ERROR);
            return;
        }
        
        recipe.setResultMaterial(materialName + ";0");
        player.sendMessage(Chat.colorize("&aResult material updated to: " + materialName));
        SoundManager.playSound(player, SoundManager.SoundType.ACTION_SUCCESS);
    }

    private static void handleEnchantAdd(Player player, Recipe recipe, String enchantName, String input) {
        int level = Number.getInteger(input);
        if (level <= 0) {
            player.sendMessage(Chat.colorize(File.getMessage().getString("recipe.invalid_enchantment")
                    .replace("#enchantment#", enchantName)));
            SoundManager.playSound(player, SoundManager.SoundType.ACTION_ERROR);
            return;
        }
        
        Optional<XEnchantment> xEnchant = XEnchantment.matchXEnchantment(enchantName);
        if (!xEnchant.isPresent()) {
            player.sendMessage(Chat.colorize(File.getMessage().getString("recipe.invalid_enchantment")
                    .replace("#enchantment#", enchantName)));
            SoundManager.playSound(player, SoundManager.SoundType.ACTION_ERROR);
            return;
        }
        
        Map<String, Integer> enchants = new HashMap<>(recipe.getResultEnchantments());
        enchants.put(enchantName.toUpperCase(), level);
        recipe.setResultEnchantments(enchants);
        player.sendMessage(Chat.colorize("&aEnchantment added: " + enchantName + " " + level));
        SoundManager.playSound(player, SoundManager.SoundType.ACTION_SUCCESS);
    }

    private static void handleRequirementAdd(Player player, Recipe recipe, String input) {
        String materialName = input.toUpperCase().trim();
        
        // Check if material exists in storage system
        if (!MineManager.getPluginBlocks().contains(materialName + ";0")) {
            player.sendMessage(Chat.colorize("&cMaterial '" + materialName + "' is not available in storage system!"));
            SoundManager.playSound(player, SoundManager.SoundType.ACTION_ERROR);
            return;
        }
        
        Map<String, Integer> requirements = new HashMap<>(recipe.getMaterialRequirements());
        requirements.put(materialName + ";0", 1);
        recipe.setMaterialRequirements(requirements);
        player.sendMessage(Chat.colorize("&aRequirement added: " + materialName + " x1"));
        SoundManager.playSound(player, SoundManager.SoundType.ACTION_SUCCESS);
    }

    private static void handleRequirementAmountEdit(Player player, Recipe recipe, String material, String input) {
        int amount = Number.getInteger(input);
        if (amount <= 0) {
            player.sendMessage(Chat.colorize(File.getMessage().getString("recipe.invalid_amount")
                    .replace("#amount#", input)));
            SoundManager.playSound(player, SoundManager.SoundType.ACTION_ERROR);
            return;
        }
        
        Map<String, Integer> requirements = new HashMap<>(recipe.getMaterialRequirements());
        if (requirements.containsKey(material)) {
            requirements.put(material, amount);
            recipe.setMaterialRequirements(requirements);
            player.sendMessage(Chat.colorize("&aRequirement amount updated: " + material + " x" + amount));
            SoundManager.playSound(player, SoundManager.SoundType.ACTION_SUCCESS);
        } else {
            player.sendMessage(Chat.colorize("&cRequirement not found: " + material));
            SoundManager.playSound(player, SoundManager.SoundType.ACTION_ERROR);
        }
    }

    private static void handleCategoryEdit(Player player, Recipe recipe, String input) {
        if (input.trim().isEmpty()) {
            player.sendMessage(Chat.colorize("&cCategory cannot be empty!"));
            SoundManager.playSound(player, SoundManager.SoundType.ACTION_ERROR);
            return;
        }
        
        recipe.setCategory(input.toLowerCase().trim());
        player.sendMessage(Chat.colorize("&aCategory updated to: " + input));
        SoundManager.playSound(player, SoundManager.SoundType.ACTION_SUCCESS);
    }

    private static void handleRecipeNameEdit(Player player, Recipe recipe, String input) {
        if (input.trim().isEmpty()) {
            player.sendMessage(Chat.colorize("&cRecipe name cannot be empty!"));
            SoundManager.playSound(player, SoundManager.SoundType.ACTION_ERROR);
            return;
        }

        recipe.setName(input);
        player.sendMessage(Chat.colorize("&aRecipe name updated to: " + input));
        SoundManager.playSound(player, SoundManager.SoundType.ACTION_SUCCESS);
    }

    private static void handlePermissionAdd(Player player, Recipe recipe, String input) {
        if (input.trim().isEmpty()) {
            player.sendMessage(Chat.colorize("&cPermission cannot be empty!"));
            SoundManager.playSound(player, SoundManager.SoundType.ACTION_ERROR);
            return;
        }

        String permission = input.trim();
        List<String> permissions = new ArrayList<>(recipe.getPermissionRequirements());
        if (!permissions.contains(permission)) {
            permissions.add(permission);
            recipe.setPermissionRequirements(permissions);
            player.sendMessage(Chat.colorize("&aAdded permission requirement: " + permission));
            SoundManager.playSound(player, SoundManager.SoundType.ACTION_SUCCESS);
        } else {
            player.sendMessage(Chat.colorize("&cPermission already exists: " + permission));
            SoundManager.playSound(player, SoundManager.SoundType.ACTION_ERROR);
        }
    }

    // Helper methods for GUI interactions
    public static void requestNameEdit(Player player, Recipe recipe) {
        net.danh.storage.Listeners.Chat.chat_recipe_edit_type.put(player, "name");
        net.danh.storage.Listeners.Chat.chat_recipe_id.put(player, recipe.getId());
        player.closeInventory();
        player.sendMessage(Chat.colorize(File.getMessage().getString("recipe.enter_recipe_name")));
    }

    public static void requestLoreAdd(Player player, Recipe recipe) {
        net.danh.storage.Listeners.Chat.chat_recipe_edit_type.put(player, "lore_add");
        net.danh.storage.Listeners.Chat.chat_recipe_id.put(player, recipe.getId());
        player.closeInventory();
        player.sendMessage(Chat.colorize("&eEnter new lore line in chat:"));
    }

    public static void requestAmountEdit(Player player, Recipe recipe) {
        net.danh.storage.Listeners.Chat.chat_recipe_edit_type.put(player, "amount");
        net.danh.storage.Listeners.Chat.chat_recipe_id.put(player, recipe.getId());
        player.closeInventory();
        player.sendMessage(Chat.colorize(File.getMessage().getString("recipe.enter_custom_amount")));
    }

    public static void requestMaterialEdit(Player player, Recipe recipe) {
        net.danh.storage.Listeners.Chat.chat_recipe_edit_type.put(player, "material");
        net.danh.storage.Listeners.Chat.chat_recipe_id.put(player, recipe.getId());
        player.closeInventory();
        player.sendMessage(Chat.colorize("&eEnter material name (e.g., DIAMOND, STONE):"));
    }

    public static void requestEnchantAdd(Player player, Recipe recipe, String enchantName) {
        net.danh.storage.Listeners.Chat.chat_recipe_edit_type.put(player, "enchant_add");
        net.danh.storage.Listeners.Chat.chat_recipe_id.put(player, recipe.getId());
        net.danh.storage.Listeners.Chat.chat_recipe_field.put(player, enchantName);
        player.closeInventory();
        player.sendMessage(Chat.colorize(File.getMessage().getString("recipe.enter_enchant_level")
                .replace("#enchantment#", enchantName)));
    }

    public static void requestRequirementAdd(Player player, Recipe recipe) {
        net.danh.storage.Listeners.Chat.chat_recipe_edit_type.put(player, "requirement_add");
        net.danh.storage.Listeners.Chat.chat_recipe_id.put(player, recipe.getId());
        player.closeInventory();
        player.sendMessage(Chat.colorize("&eEnter material name for requirement (e.g., DIAMOND, IRON_INGOT):"));
    }

    public static void requestRequirementAmountEdit(Player player, Recipe recipe, String material) {
        net.danh.storage.Listeners.Chat.chat_recipe_edit_type.put(player, "requirement_amount");
        net.danh.storage.Listeners.Chat.chat_recipe_id.put(player, recipe.getId());
        net.danh.storage.Listeners.Chat.chat_recipe_field.put(player, material);
        player.closeInventory();
        player.sendMessage(Chat.colorize(File.getMessage().getString("recipe.enter_material_amount")
                .replace("#material#", material.split(";")[0])));
    }

    public static void requestCategoryEdit(Player player, Recipe recipe) {
        net.danh.storage.Listeners.Chat.chat_recipe_edit_type.put(player, "category");
        net.danh.storage.Listeners.Chat.chat_recipe_id.put(player, recipe.getId());
        player.closeInventory();
        player.sendMessage(Chat.colorize("&eEnter category name (e.g., tools, blocks, misc):"));
    }

    public static void requestRecipeNameEdit(Player player, Recipe recipe) {
        net.danh.storage.Listeners.Chat.chat_recipe_edit_type.put(player, "recipe_name");
        net.danh.storage.Listeners.Chat.chat_recipe_id.put(player, recipe.getId());
        player.closeInventory();
        player.sendMessage(Chat.colorize(File.getMessage().getString("recipe.enter_recipe_name")));
    }

    // Helper methods for item flags
    public static void toggleItemFlag(Recipe recipe, ItemFlag flag) {
        Set<ItemFlag> flags = new HashSet<>(recipe.getResultFlags());
        if (flags.contains(flag)) {
            flags.remove(flag);
        } else {
            flags.add(flag);
        }
        recipe.setResultFlags(flags);
    }

    public static void removeLoreLine(Recipe recipe, int lineIndex) {
        List<String> lore = new ArrayList<>(recipe.getResultLore());
        if (lineIndex >= 0 && lineIndex < lore.size()) {
            lore.remove(lineIndex);
            recipe.setResultLore(lore);
        }
    }

    public static void removeEnchantment(Recipe recipe, String enchantName) {
        Map<String, Integer> enchants = new HashMap<>(recipe.getResultEnchantments());
        enchants.remove(enchantName);
        recipe.setResultEnchantments(enchants);
    }

    public static void removeRequirement(Recipe recipe, String material) {
        Map<String, Integer> requirements = new HashMap<>(recipe.getMaterialRequirements());
        requirements.remove(material);
        recipe.setMaterialRequirements(requirements);
    }
}
