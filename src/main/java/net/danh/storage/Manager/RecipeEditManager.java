package net.danh.storage.Manager;

import com.cryptomorin.xseries.XEnchantment;
import com.cryptomorin.xseries.XMaterial;
import net.danh.storage.GUI.MaterialEditorGUI;
import net.danh.storage.GUI.RecipeEditorGUI;
import net.danh.storage.Recipe.Recipe;
import net.danh.storage.Storage;
import net.danh.storage.Utils.*;
import net.danh.storage.Utils.Number;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;

import java.util.*;

public class RecipeEditManager {

    private static final Map<UUID, String> editType = new HashMap<>();
    private static final Map<UUID, String> editRecipeId = new HashMap<>();
    private static final Map<UUID, String> editField = new HashMap<>();
    private static final Map<UUID, String> previousGUI = new HashMap<>();
    private static List<ItemFlag> cachedAvailableFlags = null;

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

    public static void requestLoreEdit(Player player, Recipe recipe) {
        editType.put(player.getUniqueId(), "lore_edit");
        editRecipeId.put(player.getUniqueId(), recipe.getId());
        player.closeInventory();
        player.sendMessage(ChatUtils.colorize(
                File.getMessage().getString("crafting.edit_lore_prompt")));
        player.sendMessage(ChatUtils.colorize(
                File.getMessage().getString("crafting.edit_lore_hint")));
        displayCurrentLore(player, recipe);
    }

    public static void requestRequirementAdd(Player player, Recipe recipe) {
        editType.put(player.getUniqueId(), "requirement_add");
        editRecipeId.put(player.getUniqueId(), recipe.getId());
        previousGUI.put(player.getUniqueId(), "MaterialEditorGUI");
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
        previousGUI.put(player.getUniqueId(), "MaterialEditorGUI");

        String displayName = File.getConfig().getString("items." + material, material);
        if (material.contains(";")) {
            displayName = File.getConfig().getString("items." + material, material.split(";")[0]);
        }

        player.closeInventory();
        player.sendMessage(ChatUtils.colorize(
                File.getMessage().getString("crafting.edit_requirement_amount_prompt")
                        .replace("#material#", displayName)));
    }

    public static void requestPermissionEdit(Player player, Recipe recipe) {
        editType.put(player.getUniqueId(), "permission_edit");
        editRecipeId.put(player.getUniqueId(), recipe.getId());
        player.closeInventory();
        player.sendMessage(ChatUtils.colorize(
                File.getMessage().getString("crafting.edit_permission_prompt")));
        String currentPerm = recipe.getPermissionRequirement();
        if (currentPerm != null && !currentPerm.trim().isEmpty()) {
            player.sendMessage(ChatUtils.colorize(
                    File.getMessage().getString("crafting.edit_permission_current")
                            .replace("#current#", currentPerm)));
        }
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

    public static void requestEnchantmentEdit(Player player, Recipe recipe) {
        editType.put(player.getUniqueId(), "enchant_edit");
        editRecipeId.put(player.getUniqueId(), recipe.getId());
        player.closeInventory();
        player.sendMessage(ChatUtils.colorize(
                File.getMessage().getString("crafting.edit_enchant_prompt")));
        player.sendMessage(ChatUtils.colorize(
                File.getMessage().getString("crafting.edit_enchant_hint")));
        displayCurrentEnchantments(player, recipe);
    }

    public static void requestFlagEdit(Player player, Recipe recipe) {
        editType.put(player.getUniqueId(), "flag_edit");
        editRecipeId.put(player.getUniqueId(), recipe.getId());
        player.closeInventory();
        player.sendMessage(ChatUtils.colorize(
                File.getMessage().getString("crafting.edit_flag_prompt")));
        player.sendMessage(ChatUtils.colorize(
                File.getMessage().getString("crafting.edit_flag_hint")));

        // Display available flags
        List<ItemFlag> availableFlags = getAvailableFlags();
        String flagsStr = availableFlags.stream()
                .map(ItemFlag::name)
                .reduce((a, b) -> a + ", " + b)
                .orElse("None");
        player.sendMessage(ChatUtils.colorize(
                File.getMessage().getString("crafting.edit_flag_available")
                        .replace("#flags#", flagsStr)));

        displayCurrentFlags(player, recipe);
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

    public static void handleCancel(Player player) {
        UUID playerId = player.getUniqueId();
        if (!editType.containsKey(playerId)) {
            return;
        }

        player.sendMessage(ChatUtils.colorize(
                File.getMessage().getString("user.chat_input_cancelled")));

        String type = editType.get(playerId);
        String recipeId = editRecipeId.get(playerId);
        Recipe recipe = CraftingManager.getRecipe(recipeId);

        if (recipe != null) {
            String prevGUI = previousGUI.get(playerId);
            clearEditData(playerId);
            SchedulerUtil.runTask(Storage.getStorage(), () ->
                    reopenRecipeEditorSync(player, recipe, type, prevGUI));
        } else {
            clearEditData(playerId);
        }
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
            updateRecipeAndReopenGUI(player, recipe, type);
        } else if (!"material".equals(type) && !"enchant_edit".equals(type) && !"flag_edit".equals(type) && !"lore_edit".equals(type) && !"permission_edit".equals(type)) {
            clearEditData(playerId);
        }
        return true;
    }

    private static void updateRecipeAndReopenGUI(Player player, Recipe recipe, String editType) {
        CraftingManager.updateRecipe(recipe);

        SchedulerUtil.runTaskLater(Storage.getStorage(), () -> {
            String prevGUI = previousGUI.get(player.getUniqueId());
            clearEditData(player.getUniqueId());
            reopenRecipeEditorSync(player, recipe, editType, prevGUI);
        }, 3L);
    }

    private static void reopenRecipeEditorSync(Player player, Recipe recipe, String editType, String prevGUI) {
        SoundManager.setShouldPlayCloseSound(player, false);

        if ("MaterialEditorGUI".equals(prevGUI)) {
            player.openInventory(new MaterialEditorGUI(player, recipe).getInventory(SoundContext.SILENT));
        } else {
            player.openInventory(new RecipeEditorGUI(player, recipe).getInventory(SoundContext.SILENT));
        }
    }

    private static boolean processEdit(Player player, Recipe recipe, String type, String input) {
        try {
            switch (type) {
                case "material":
                    if (input.trim().isEmpty()) {
                        player.sendMessage(ChatUtils.colorize(
                                File.getMessage().getString("crafting.edit_material_empty")));
                        promptMaterialRetry(player, recipe);
                        return false;
                    }
                    Optional<XMaterial> xMaterial = com.cryptomorin.xseries.XMaterial.matchXMaterial(input.toUpperCase());
                    if (!xMaterial.isPresent()) {
                        player.sendMessage(ChatUtils.colorize(
                                File.getMessage().getString("crafting.edit_material_invalid")
                                        .replace("#material#", input)));
                        promptMaterialRetry(player, recipe);
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
                    recipe.setResultName(input);
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

                case "lore_edit":
                    return processLoreEdit(player, recipe, input);

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

                case "permission_edit":
                    String perm = input.trim();
                    if (perm.isEmpty() || perm.equalsIgnoreCase("none")) {
                        recipe.setPermissionRequirement(null);
                        player.sendMessage(ChatUtils.colorize(
                                File.getMessage().getString("crafting.permission_cleared")));
                        return true;
                    }
                    recipe.setPermissionRequirement(perm);
                    player.sendMessage(ChatUtils.colorize(
                            File.getMessage().getString("crafting.edit_permission_success")
                                    .replace("#permission#", perm)));
                    return true;

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

                case "enchant_edit":
                    return processEnchantmentEdit(player, recipe, input);

                case "flag_edit":
                    return processFlagEdit(player, recipe, input);

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
        previousGUI.remove(playerId);
    }

    public static String getEditType(UUID playerId) {
        return editType.get(playerId);
    }

    public static String getEditRecipeId(UUID playerId) {
        return editRecipeId.get(playerId);
    }

    private static void promptMaterialRetry(Player player, Recipe recipe) {
        player.sendMessage(ChatUtils.colorize(
                File.getMessage().getString("crafting.edit_material_prompt")));
        player.sendMessage(ChatUtils.colorize(
                File.getMessage().getString("crafting.edit_material_hint")));
    }

    private static void displayCurrentLore(Player player, Recipe recipe) {
        List<String> lore = recipe.getResultLore();
        if (lore == null || lore.isEmpty()) {
            player.sendMessage(ChatUtils.colorize(
                    File.getMessage().getString("crafting.edit_lore_list_empty")));
            return;
        }

        player.sendMessage(ChatUtils.colorize(
                File.getMessage().getString("crafting.edit_lore_current_header")));

        for (int i = 0; i < lore.size(); i++) {
            player.sendMessage(ChatUtils.colorize(
                    File.getMessage().getString("crafting.edit_lore_list")
                            .replace("#index#", String.valueOf(i + 1))
                            .replace("#text#", lore.get(i))));
        }
    }

    private static void promptLoreRetry(Player player, Recipe recipe) {
        player.sendMessage(ChatUtils.colorize(
                File.getMessage().getString("crafting.edit_lore_prompt")));
        player.sendMessage(ChatUtils.colorize(
                File.getMessage().getString("crafting.edit_lore_hint")));
        displayCurrentLore(player, recipe);
    }

    private static boolean processLoreEdit(Player player, Recipe recipe, String input) {
        String[] parts = input.trim().split("\\s+", 2);
        if (parts.length == 0 || parts[0].trim().isEmpty()) {
            player.sendMessage(ChatUtils.colorize(
                    File.getMessage().getString("crafting.edit_lore_hint")));
            promptLoreRetry(player, recipe);
            return false;
        }

        String command = parts[0].toLowerCase();
        List<String> lore = new ArrayList<>(recipe.getResultLore());

        if ("add".equalsIgnoreCase(command)) {
            if (parts.length < 2 || parts[1].trim().isEmpty()) {
                player.sendMessage(ChatUtils.colorize(
                        File.getMessage().getString("crafting.edit_lore_empty")));
                promptLoreRetry(player, recipe);
                return false;
            }

            lore.add(parts[1]);
            recipe.setResultLore(lore);
            player.sendMessage(ChatUtils.colorize(
                    File.getMessage().getString("crafting.edit_lore_add_success")));
            return true;

        } else if ("edit".equalsIgnoreCase(command)) {
            if (parts.length < 2) {
                player.sendMessage(ChatUtils.colorize(
                        File.getMessage().getString("crafting.edit_lore_edit_invalid")));
                promptLoreRetry(player, recipe);
                return false;
            }

            String[] args = parts[1].split("\\s+", 2);
            if (args.length < 2) {
                player.sendMessage(ChatUtils.colorize(
                        File.getMessage().getString("crafting.edit_lore_edit_invalid")));
                promptLoreRetry(player, recipe);
                return false;
            }

            int lineNum = Number.getInteger(args[0]);
            if (lineNum < 1 || lineNum > lore.size()) {
                player.sendMessage(ChatUtils.colorize(
                        File.getMessage().getString("crafting.edit_lore_invalid_line")));
                promptLoreRetry(player, recipe);
                return false;
            }

            lore.set(lineNum - 1, args[1]);
            recipe.setResultLore(lore);
            player.sendMessage(ChatUtils.colorize(
                    File.getMessage().getString("crafting.edit_lore_edit_success")
                            .replace("#line#", String.valueOf(lineNum))));
            return true;

        } else if ("remove".equalsIgnoreCase(command)) {
            if (lore.isEmpty()) {
                player.sendMessage(ChatUtils.colorize(
                        File.getMessage().getString("crafting.edit_lore_remove_empty")));
                promptLoreRetry(player, recipe);
                return false;
            }

            if (parts.length < 2) {
                // Remove last line
                String removed = lore.remove(lore.size() - 1);
                recipe.setResultLore(lore);
                player.sendMessage(ChatUtils.colorize(
                        File.getMessage().getString("crafting.edit_lore_remove_last_success")));
                return true;
            }

            int lineNum = Number.getInteger(parts[1]);
            if (lineNum < 1 || lineNum > lore.size()) {
                player.sendMessage(ChatUtils.colorize(
                        File.getMessage().getString("crafting.edit_lore_invalid_line")));
                promptLoreRetry(player, recipe);
                return false;
            }

            lore.remove(lineNum - 1);
            recipe.setResultLore(lore);
            player.sendMessage(ChatUtils.colorize(
                    File.getMessage().getString("crafting.edit_lore_remove_success")
                            .replace("#line#", String.valueOf(lineNum))));
            return true;

        } else if ("clear".equalsIgnoreCase(command)) {
            if (parts.length >= 2 && "all".equalsIgnoreCase(parts[1].trim())) {
                lore.clear();
                recipe.setResultLore(lore);
                player.sendMessage(ChatUtils.colorize(
                        File.getMessage().getString("crafting.edit_lore_clear_success")));
                return true;
            }
            player.sendMessage(ChatUtils.colorize(
                    File.getMessage().getString("crafting.edit_lore_hint")));
            promptLoreRetry(player, recipe);
            return false;
        }

        player.sendMessage(ChatUtils.colorize(
                File.getMessage().getString("crafting.edit_lore_hint")));
        promptLoreRetry(player, recipe);
        return false;
    }

    private static boolean processEnchantmentEdit(Player player, Recipe recipe, String input) {
        String[] parts = input.trim().split("\\s+", 2);
        if (parts.length == 0) {
            player.sendMessage(ChatUtils.colorize(
                    File.getMessage().getString("crafting.edit_enchant_hint")));
            return false;
        }

        String command = parts[0].toLowerCase();
        Map<String, Integer> enchants = new HashMap<>(recipe.getResultEnchantments());

        if ("add".equalsIgnoreCase(command)) {
            if (parts.length < 2) {
                player.sendMessage(ChatUtils.colorize(
                        File.getMessage().getString("crafting.edit_enchant_add_invalid")));
                return false;
            }
            String[] args = parts[1].split("\\s+");
            if (args.length < 2) {
                player.sendMessage(ChatUtils.colorize(
                        File.getMessage().getString("crafting.edit_enchant_add_invalid")));
                return false;
            }

            String enchantName = args[0].toUpperCase();
            Optional<XEnchantment> xEnchant = XEnchantment.matchXEnchantment(enchantName);
            if (!xEnchant.isPresent()) {
                player.sendMessage(ChatUtils.colorize(
                        File.getMessage().getString("crafting.edit_enchant_invalid")));
                return false;
            }

            int level = Number.getInteger(args[1]);
            int maxLevel = getMaxEnchantLevel(enchantName);
            if (level < 1 || level > maxLevel) {
                player.sendMessage(ChatUtils.colorize(
                        File.getMessage().getString("crafting.edit_enchant_level_invalid")
                                .replace("#max#", String.valueOf(maxLevel))));
                return false;
            }

            enchants.put(enchantName, level);
            recipe.setResultEnchantments(enchants);
            player.sendMessage(ChatUtils.colorize(
                    File.getMessage().getString("crafting.edit_enchant_add_success")
                            .replace("#enchant#", enchantName)
                            .replace("#level#", String.valueOf(level))));
            return true;

        } else if ("remove".equalsIgnoreCase(command)) {
            if (enchants.isEmpty()) {
                player.sendMessage(ChatUtils.colorize(
                        File.getMessage().getString("crafting.edit_enchant_empty")));
                return false;
            }

            if (parts.length < 2) {
                List<String> enchantList = new ArrayList<>(enchants.keySet());
                String lastEnchant = enchantList.get(enchantList.size() - 1);
                enchants.remove(lastEnchant);
                recipe.setResultEnchantments(enchants);
                player.sendMessage(ChatUtils.colorize(
                        File.getMessage().getString("crafting.edit_enchant_remove_success")
                                .replace("#line#", String.valueOf(enchantList.size()))));
                return true;
            }

            int lineNum = Number.getInteger(parts[1]);
            List<String> enchantList = new ArrayList<>(enchants.keySet());
            if (lineNum < 1 || lineNum > enchantList.size()) {
                player.sendMessage(ChatUtils.colorize(
                        File.getMessage().getString("crafting.edit_enchant_remove_invalid")));
                return false;
            }

            String enchantToRemove = enchantList.get(lineNum - 1);
            enchants.remove(enchantToRemove);
            recipe.setResultEnchantments(enchants);
            player.sendMessage(ChatUtils.colorize(
                    File.getMessage().getString("crafting.edit_enchant_remove_success")
                            .replace("#line#", String.valueOf(lineNum))));
            return true;

        } else if ("clear".equalsIgnoreCase(command)) {
            enchants.clear();
            recipe.setResultEnchantments(enchants);
            player.sendMessage(ChatUtils.colorize(
                    File.getMessage().getString("crafting.edit_enchant_clear_success")));
            return true;
        }

        player.sendMessage(ChatUtils.colorize(
                File.getMessage().getString("crafting.edit_enchant_hint")));
        return false;
    }

    private static void displayCurrentEnchantments(Player player, Recipe recipe) {
        Map<String, Integer> enchants = recipe.getResultEnchantments();
        if (enchants.isEmpty()) {
            player.sendMessage(ChatUtils.colorize(
                    File.getMessage().getString("crafting.edit_enchant_empty")));
            return;
        }

        List<Map.Entry<String, Integer>> enchantList = new ArrayList<>(enchants.entrySet());
        for (int i = 0; i < enchantList.size(); i++) {
            Map.Entry<String, Integer> entry = enchantList.get(i);
            player.sendMessage(ChatUtils.colorize(
                    File.getMessage().getString("crafting.edit_enchant_list")
                            .replace("#index#", String.valueOf(i + 1))
                            .replace("#enchant#", entry.getKey())
                            .replace("#level#", String.valueOf(entry.getValue()))));
        }
    }

    private static int getMaxEnchantLevel(String enchantName) {
        Optional<XEnchantment> xEnchant = XEnchantment.matchXEnchantment(enchantName);
        if (xEnchant.isPresent()) {
            Enchantment enchant = xEnchant.get().getEnchant();
            if (enchant != null) {
                return enchant.getMaxLevel();
            }
        }
        return 5;
    }

    public static List<ItemFlag> getAvailableFlags() {
        if (cachedAvailableFlags != null) {
            return new ArrayList<>(cachedAvailableFlags);
        }

        cachedAvailableFlags = new ArrayList<>();
        Collections.addAll(cachedAvailableFlags, ItemFlag.values());

        return new ArrayList<>(cachedAvailableFlags);
    }

    public static void clearFlagCache() {
        cachedAvailableFlags = null;
    }

    public static ItemFlag parseFlag(String flagName) {
        if (flagName == null || flagName.trim().isEmpty()) {
            return null;
        }

        String normalizedName = flagName.toUpperCase().trim();

        List<ItemFlag> availableFlags = getAvailableFlags();
        for (ItemFlag flag : availableFlags) {
            if (flag.name().equals(normalizedName)) {
                return flag;
            }
        }

        return null;
    }

    public static boolean isFlagVersionIncompatible(String flagName) {
        if (flagName == null || flagName.trim().isEmpty()) {
            return false;
        }

        String normalizedName = flagName.toUpperCase().trim();

        try {
            ItemFlag.valueOf(normalizedName);
            List<ItemFlag> availableFlags = getAvailableFlags();
            for (ItemFlag flag : availableFlags) {
                if (flag.name().equals(normalizedName)) {
                    return false; // Flag is available
                }
            }
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private static void displayCurrentFlags(Player player, Recipe recipe) {
        Set<ItemFlag> flags = recipe.getResultFlags();
        if (flags == null || flags.isEmpty()) {
            player.sendMessage(ChatUtils.colorize(
                    File.getMessage().getString("crafting.edit_flag_empty")));
            return;
        }

        player.sendMessage(ChatUtils.colorize(
                File.getMessage().getString("crafting.edit_flag_current_header")));

        List<ItemFlag> flagList = new ArrayList<>(flags);
        for (int i = 0; i < flagList.size(); i++) {
            player.sendMessage(ChatUtils.colorize(
                    File.getMessage().getString("crafting.edit_flag_list")
                            .replace("#index#", String.valueOf(i + 1))
                            .replace("#flag#", flagList.get(i).name())));
        }
    }

    private static void promptFlagRetry(Player player, Recipe recipe) {
        player.sendMessage(ChatUtils.colorize(
                File.getMessage().getString("crafting.edit_flag_prompt")));
        player.sendMessage(ChatUtils.colorize(
                File.getMessage().getString("crafting.edit_flag_hint")));

        List<ItemFlag> availableFlags = getAvailableFlags();
        String flagsStr = availableFlags.stream()
                .map(ItemFlag::name)
                .reduce((a, b) -> a + ", " + b)
                .orElse("None");
        player.sendMessage(ChatUtils.colorize(
                File.getMessage().getString("crafting.edit_flag_available")
                        .replace("#flags#", flagsStr)));

        displayCurrentFlags(player, recipe);
    }

    private static boolean processFlagEdit(Player player, Recipe recipe, String input) {
        String[] parts = input.trim().split("\\s+", 2);
        if (parts.length == 0) {
            player.sendMessage(ChatUtils.colorize(
                    File.getMessage().getString("crafting.edit_flag_hint")));
            promptFlagRetry(player, recipe);
            return false;
        }

        String command = parts[0].toLowerCase();
        Set<ItemFlag> flags = new HashSet<>(recipe.getResultFlags());

        if ("add".equalsIgnoreCase(command)) {
            if (parts.length < 2) {
                player.sendMessage(ChatUtils.colorize(
                        File.getMessage().getString("crafting.edit_flag_hint")));
                promptFlagRetry(player, recipe);
                return false;
            }

            String flagName = parts[1].toUpperCase().trim();
            ItemFlag flag = parseFlag(flagName);

            if (flag == null) {
                if (isFlagVersionIncompatible(flagName)) {
                    player.sendMessage(ChatUtils.colorize(
                            File.getMessage().getString("crafting.edit_flag_version_incompatible")
                                    .replace("#flag#", flagName)));
                } else {
                    player.sendMessage(ChatUtils.colorize(
                            File.getMessage().getString("crafting.edit_flag_invalid")
                                    .replace("#flag#", flagName)));
                }
                promptFlagRetry(player, recipe);
                return false;
            }

            if (flags.contains(flag)) {
                player.sendMessage(ChatUtils.colorize(
                        File.getMessage().getString("crafting.edit_flag_add_duplicate")
                                .replace("#flag#", flag.name())));
                promptFlagRetry(player, recipe);
                return false;
            }

            flags.add(flag);
            recipe.setResultFlags(flags);
            player.sendMessage(ChatUtils.colorize(
                    File.getMessage().getString("crafting.edit_flag_add_success")
                            .replace("#flag#", flag.name())));
            return true;

        } else if ("remove".equalsIgnoreCase(command)) {
            if (flags.isEmpty()) {
                player.sendMessage(ChatUtils.colorize(
                        File.getMessage().getString("crafting.edit_flag_remove_empty")));
                promptFlagRetry(player, recipe);
                return false;
            }

            if (parts.length < 2) {
                List<ItemFlag> flagList = new ArrayList<>(flags);
                ItemFlag lastFlag = flagList.get(flagList.size() - 1);
                flags.remove(lastFlag);
                recipe.setResultFlags(flags);
                player.sendMessage(ChatUtils.colorize(
                        File.getMessage().getString("crafting.edit_flag_remove_last_success")
                                .replace("#flag#", lastFlag.name())));
                return true;
            }

            String arg = parts[1].trim();

            try {
                int lineNum = Integer.parseInt(arg);
                List<ItemFlag> flagList = new ArrayList<>(flags);
                if (lineNum < 1 || lineNum > flagList.size()) {
                    player.sendMessage(ChatUtils.colorize(
                            File.getMessage().getString("crafting.edit_flag_remove_invalid_line")));
                    promptFlagRetry(player, recipe);
                    return false;
                }

                ItemFlag flagToRemove = flagList.get(lineNum - 1);
                flags.remove(flagToRemove);
                recipe.setResultFlags(flags);
                player.sendMessage(ChatUtils.colorize(
                        File.getMessage().getString("crafting.edit_flag_remove_success")
                                .replace("#flag#", flagToRemove.name())));
                return true;
            } catch (NumberFormatException e) {
                String flagName = arg.toUpperCase();
                ItemFlag flag = parseFlag(flagName);

                if (flag == null || !flags.contains(flag)) {
                    player.sendMessage(ChatUtils.colorize(
                            File.getMessage().getString("crafting.edit_flag_remove_not_found")
                                    .replace("#flag#", flagName)));
                    promptFlagRetry(player, recipe);
                    return false;
                }

                flags.remove(flag);
                recipe.setResultFlags(flags);
                player.sendMessage(ChatUtils.colorize(
                        File.getMessage().getString("crafting.edit_flag_remove_success")
                                .replace("#flag#", flag.name())));
                return true;
            }

        } else if ("clear".equalsIgnoreCase(command)) {
            if (parts.length >= 2 && "all".equalsIgnoreCase(parts[1].trim())) {
                flags.clear();
                recipe.setResultFlags(flags);
                player.sendMessage(ChatUtils.colorize(
                        File.getMessage().getString("crafting.edit_flag_clear_success")));
                return true;
            }
            player.sendMessage(ChatUtils.colorize(
                    File.getMessage().getString("crafting.edit_flag_hint")));
            promptFlagRetry(player, recipe);
            return false;
        }

        player.sendMessage(ChatUtils.colorize(
                File.getMessage().getString("crafting.edit_flag_hint")));
        promptFlagRetry(player, recipe);
        return false;
    }
}
