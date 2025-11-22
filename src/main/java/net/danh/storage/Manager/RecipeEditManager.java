package net.danh.storage.Manager;

import com.cryptomorin.xseries.XMaterial;
import net.danh.storage.GUI.EnchantmentsEditorGUI;
import net.danh.storage.GUI.RecipeEditorGUI;
import net.danh.storage.Recipe.Recipe;
import net.danh.storage.Storage;
import net.danh.storage.Utils.*;
import net.danh.storage.Utils.Number;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;

import java.util.*;

public class RecipeEditManager {

    private static final Map<UUID, String> editType = new HashMap<>();
    private static final Map<UUID, String> editRecipeId = new HashMap<>();
    private static final Map<UUID, String> editField = new HashMap<>();

    public static void requestMaterialEdit(Player player, Recipe recipe) {
        editType.put(player.getUniqueId(), "material");
        editRecipeId.put(player.getUniqueId(), recipe.getId());
        player.closeInventory();
        player.sendMessage(ChatUtils.colorize(
                File.getMessage().getString("crafting.edit_material_prompt")));
        player.sendMessage(ChatUtils.colorize(
                File.getMessage().getString("crafting.edit_material_current")
                        .replace("#current#", recipe.getResultMaterial())));
        player.sendMessage(ChatUtils.colorize(
                File.getMessage().getString("crafting.edit_material_hint")));
    }

    public static void requestNameEdit(Player player, Recipe recipe) {
        editType.put(player.getUniqueId(), "name");
        editRecipeId.put(player.getUniqueId(), recipe.getId());
        player.closeInventory();
        player.sendMessage(ChatUtils.colorize(
                File.getMessage().getString("crafting.edit_name_prompt")));
        player.sendMessage(ChatUtils.colorize(
                File.getMessage().getString("crafting.edit_name_current")
                        .replace("#current#", recipe.getName())));
    }

    public static void requestAmountEdit(Player player, Recipe recipe) {
        editType.put(player.getUniqueId(), "amount");
        editRecipeId.put(player.getUniqueId(), recipe.getId());
        player.closeInventory();
        player.sendMessage(ChatUtils.colorize(
                File.getMessage().getString("crafting.edit_amount_prompt")));
        player.sendMessage(ChatUtils.colorize(
                File.getMessage().getString("crafting.edit_amount_current")
                        .replace("#current#", String.valueOf(recipe.getResultAmount()))));
    }

    public static void requestLoreAdd(Player player, Recipe recipe) {
        editType.put(player.getUniqueId(), "lore_add");
        editRecipeId.put(player.getUniqueId(), recipe.getId());
        player.closeInventory();
        player.sendMessage(ChatUtils.colorize(
                File.getMessage().getString("crafting.edit_lore_prompt")));
        player.sendMessage(ChatUtils.colorize(
                File.getMessage().getString("crafting.edit_lore_hint")));
    }

    public static void requestRequirementAdd(Player player, Recipe recipe) {
        editType.put(player.getUniqueId(), "requirement_add");
        editRecipeId.put(player.getUniqueId(), recipe.getId());
        player.closeInventory();
        player.sendMessage(ChatUtils.colorize(
                File.getMessage().getString("crafting.edit_requirement_prompt")));
        player.sendMessage(ChatUtils.colorize(
                File.getMessage().getString("crafting.edit_requirement_hint")));
    }

    public static void requestRequirementAmountEdit(Player player, Recipe recipe, String material) {
        editType.put(player.getUniqueId(), "requirement_amount");
        editRecipeId.put(player.getUniqueId(), recipe.getId());
        editField.put(player.getUniqueId(), material);

        String displayName = File.getConfig().getString("items." + material, material);
        if (material.contains(";")) {
            displayName = File.getConfig().getString("items." + material, material.split(";")[0]);
        }

        player.closeInventory();
        player.sendMessage(ChatUtils.colorize(
                File.getMessage().getString("crafting.edit_requirement_amount_prompt")
                        .replace("#material#", displayName)));
    }

    public static void requestPermissionAdd(Player player, Recipe recipe) {
        editType.put(player.getUniqueId(), "permission_add");
        editRecipeId.put(player.getUniqueId(), recipe.getId());
        player.closeInventory();
        player.sendMessage(ChatUtils.colorize(
                File.getMessage().getString("crafting.edit_permission_prompt")));
        player.sendMessage(ChatUtils.colorize(
                File.getMessage().getString("crafting.edit_permission_hint")));
    }

    public static void requestCategoryEdit(Player player, Recipe recipe) {
        editType.put(player.getUniqueId(), "category");
        editRecipeId.put(player.getUniqueId(), recipe.getId());
        player.closeInventory();
        player.sendMessage(ChatUtils.colorize(
                File.getMessage().getString("crafting.edit_category_prompt")));
        player.sendMessage(ChatUtils.colorize(
                File.getMessage().getString("crafting.edit_category_current")
                        .replace("#current#", recipe.getCategory())));
    }

    public static void requestEnchantmentAdd(Player player, Recipe recipe) {
        editType.put(player.getUniqueId(), "enchant_add");
        editRecipeId.put(player.getUniqueId(), recipe.getId());
        player.closeInventory();
        player.sendMessage(ChatUtils.colorize(
                File.getMessage().getString("crafting.edit_enchant_prompt")));
        player.sendMessage(ChatUtils.colorize(
                File.getMessage().getString("crafting.edit_enchant_hint")));
    }

    public static void requestEnchantmentLevelEdit(Player player, Recipe recipe, String enchantName) {
        editType.put(player.getUniqueId(), "enchant_level");
        editRecipeId.put(player.getUniqueId(), recipe.getId());
        editField.put(player.getUniqueId(), enchantName);
        player.closeInventory();
        player.sendMessage(ChatUtils.colorize(
                File.getMessage().getString("crafting.edit_enchant_level_prompt")));
        player.sendMessage(ChatUtils.colorize(
                File.getMessage().getString("crafting.edit_enchant_level_current")
                        .replace("#current#", String.valueOf(recipe.getResultEnchantments().getOrDefault(enchantName, 1)))));
    }

    public static void requestCustomModelDataEdit(Player player, Recipe recipe) {
        editType.put(player.getUniqueId(), "custom_model_data");
        editRecipeId.put(player.getUniqueId(), recipe.getId());
        player.closeInventory();
        player.sendMessage(ChatUtils.colorize(
                File.getMessage().getString("crafting.edit_cmd_prompt")));
        player.sendMessage(ChatUtils.colorize(
                File.getMessage().getString("crafting.edit_cmd_current")
                        .replace("#current#", String.valueOf(recipe.getResultCustomModelData()))));
    }

    public static boolean handleChatInput(Player player, String message) {
        UUID playerId = player.getUniqueId();
        if (!editType.containsKey(playerId)) {
            return false;
        }

        String type = editType.get(playerId);
        String recipeId = editRecipeId.get(playerId);
        Recipe recipe = CraftingManager.getRecipe(recipeId);

        if (recipe == null) {
            player.sendMessage(ChatUtils.colorize(
                    File.getMessage().getString("crafting.edit_recipe_not_found")));
            clearEditData(playerId);
            return true;
        }

        boolean success = processEdit(player, recipe, type, message);
        if (success) {
            CraftingManager.updateRecipe(recipe);
            reopenRecipeEditor(player, recipe, type);
        }

        clearEditData(playerId);
        return true;
    }

    private static void reopenRecipeEditor(Player player, Recipe recipe, String editType) {
        SchedulerUtil.runTask(Storage.getStorage(), () -> {
            SoundManager.setShouldPlayCloseSound(player, false);

            if (editType.equals("enchant_level")) {
                player.openInventory(new EnchantmentsEditorGUI(player, recipe).getInventory(SoundContext.SILENT));
            } else {
                player.openInventory(new RecipeEditorGUI(player, recipe).getInventory(SoundContext.SILENT));
            }
        });
    }

    private static boolean processEdit(Player player, Recipe recipe, String type, String input) {
        try {
            switch (type) {
                case "material":
                    if (input.trim().isEmpty()) {
                        player.sendMessage(ChatUtils.colorize(
                                File.getMessage().getString("crafting.edit_material_empty")));
                        return false;
                    }
                    Optional<XMaterial> xMaterial = com.cryptomorin.xseries.XMaterial.matchXMaterial(input.toUpperCase());
                    if (!xMaterial.isPresent()) {
                        player.sendMessage(ChatUtils.colorize(
                                File.getMessage().getString("crafting.edit_material_invalid")
                                        .replace("#material#", input)));
                        return false;
                    }
                    recipe.setResultMaterial(xMaterial.get().name());
                    player.sendMessage(ChatUtils.colorize(
                            File.getMessage().getString("crafting.edit_material_success")
                                    .replace("#material#", xMaterial.get().name())));
                    return true;

                case "name":
                    if (input.trim().isEmpty()) {
                        player.sendMessage(ChatUtils.colorize(
                                File.getMessage().getString("crafting.edit_name_empty")));
                        return false;
                    }
                    recipe.setName(input);
                    player.sendMessage(ChatUtils.colorize(
                            File.getMessage().getString("crafting.edit_name_success")));
                    return true;

                case "amount":
                    int amount = Number.getInteger(input);
                    if (amount < 1 || amount > 64) {
                        player.sendMessage(ChatUtils.colorize(
                                File.getMessage().getString("crafting.edit_amount_invalid")));
                        return false;
                    }
                    recipe.setResultAmount(amount);
                    player.sendMessage(ChatUtils.colorize(
                            File.getMessage().getString("crafting.edit_amount_success")));
                    return true;

                case "lore_add":
                    if (input.trim().isEmpty()) {
                        player.sendMessage(ChatUtils.colorize(
                                File.getMessage().getString("crafting.edit_lore_empty")));
                        return false;
                    }
                    List<String> lore = new ArrayList<>(recipe.getResultLore());
                    lore.add(input);
                    recipe.setResultLore(lore);
                    player.sendMessage(ChatUtils.colorize(
                            File.getMessage().getString("crafting.edit_lore_success")));
                    return true;

                case "requirement_add":
                    String normalizedMaterial = MineManager.normalizeMaterial(input);
                    recipe.getMaterialRequirements().put(normalizedMaterial, 1);
                    player.sendMessage(ChatUtils.colorize(
                            File.getMessage().getString("crafting.edit_requirement_success")
                                    .replace("#material#", input).replace("#amount#", "1")));
                    return true;

                case "requirement_amount":
                    String material = editField.get(player.getUniqueId());
                    int reqAmount = Number.getInteger(input);
                    if (reqAmount < 1) {
                        player.sendMessage(ChatUtils.colorize(
                                File.getMessage().getString("crafting.edit_requirement_amount_invalid")));
                        return false;
                    }
                    recipe.getMaterialRequirements().put(material, reqAmount);
                    player.sendMessage(ChatUtils.colorize(
                            File.getMessage().getString("crafting.edit_requirement_amount_success")));
                    return true;

                case "permission_add":
                    if (input.trim().isEmpty()) {
                        player.sendMessage(ChatUtils.colorize(
                                File.getMessage().getString("crafting.edit_permission_empty")));
                        return false;
                    }
                    List<String> permissions = new ArrayList<>(recipe.getPermissionRequirements());
                    String permission = input.trim();
                    if (!permissions.contains(permission)) {
                        permissions.add(permission);
                        recipe.setPermissionRequirements(permissions);
                        player.sendMessage(ChatUtils.colorize(
                                File.getMessage().getString("crafting.edit_permission_success")
                                        .replace("#permission#", permission)));
                        return true;
                    } else {
                        player.sendMessage(ChatUtils.colorize(
                                File.getMessage().getString("crafting.edit_permission_exists")));
                        return false;
                    }

                case "category":
                    if (input.trim().isEmpty()) {
                        player.sendMessage(ChatUtils.colorize(
                                File.getMessage().getString("crafting.edit_category_empty")));
                        return false;
                    }
                    recipe.setCategory(input);
                    player.sendMessage(ChatUtils.colorize(
                            File.getMessage().getString("crafting.edit_category_success")));
                    return true;

                case "enchant_add":
                    player.sendMessage(ChatUtils.colorize(
                            File.getMessage().getString("crafting.edit_enchant_level_range")));
                    editType.put(player.getUniqueId(), "enchant_level");
                    editField.put(player.getUniqueId(), input);
                    return false;

                case "enchant_level":
                    String enchantName = editField.get(player.getUniqueId());
                    int level = Number.getInteger(input);
                    if (level < 1 || level > 10) {
                        player.sendMessage(ChatUtils.colorize(
                                File.getMessage().getString("crafting.edit_enchant_level_invalid")
                                        .replace("#max#", "10")));
                        return false;
                    }
                    Map<String, Integer> enchants = new HashMap<>(recipe.getResultEnchantments());
                    enchants.put(enchantName, level);
                    recipe.setResultEnchantments(enchants);
                    player.sendMessage(ChatUtils.colorize(
                            File.getMessage().getString("crafting.edit_enchant_level_success")));
                    return true;

                case "custom_model_data":
                    int cmd = Number.getInteger(input);
                    recipe.setResultCustomModelData(cmd);
                    player.sendMessage(ChatUtils.colorize(
                            File.getMessage().getString("crafting.edit_cmd_success")));
                    return true;

                default:
                    return false;
            }
        } catch (NumberFormatException e) {
            player.sendMessage(ChatUtils.colorize(
                    File.getMessage().getString("crafting.edit_cmd_invalid")));
            return false;
        } catch (Exception e) {
            return false;
        }
    }

    public static void removeLoreLine(Recipe recipe, int index) {
        List<String> lore = new ArrayList<>(recipe.getResultLore());
        if (index >= 0 && index < lore.size()) {
            lore.remove(index);
            recipe.setResultLore(lore);
        }
    }

    public static void removeRequirement(Recipe recipe, String material) {
        recipe.getMaterialRequirements().remove(material);
    }

    public static void removeEnchantment(Recipe recipe, String enchantment) {
        Map<String, Integer> enchants = new HashMap<>(recipe.getResultEnchantments());
        enchants.remove(enchantment);
        recipe.setResultEnchantments(enchants);
    }

    public static void toggleItemFlag(Recipe recipe, ItemFlag flag) {
        Set<ItemFlag> flags = new HashSet<>(recipe.getResultFlags());
        if (flags.contains(flag)) {
            flags.remove(flag);
        } else {
            flags.add(flag);
        }
        recipe.setResultFlags(flags);
    }

    public static boolean isEditing(Player player) {
        return editType.containsKey(player.getUniqueId());
    }

    public static void cancelEdit(Player player) {
        clearEditData(player.getUniqueId());
    }

    private static void clearEditData(UUID playerId) {
        editType.remove(playerId);
        editRecipeId.remove(playerId);
        editField.remove(playerId);
    }

    public static String getEditType(UUID playerId) {
        return editType.get(playerId);
    }

    public static String getEditRecipeId(UUID playerId) {
        return editRecipeId.get(playerId);
    }
}
