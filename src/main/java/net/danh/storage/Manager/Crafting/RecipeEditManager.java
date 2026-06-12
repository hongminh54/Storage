package net.danh.storage.Manager.Crafting;

import com.cryptomorin.xseries.XEnchantment;
import net.danh.storage.GUI.Crafting.RecipeEditorGUI;
import net.danh.storage.GUI.MaterialEditorGUI;
import net.danh.storage.Manager.MineManager;
import net.danh.storage.Manager.SoundManager;
import net.danh.storage.Recipe.Recipe;
import net.danh.storage.Storage;
import net.danh.storage.Utils.*;
import net.danh.storage.Utils.Number;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;

import java.util.*;

public class RecipeEditManager {

    private static final Map<UUID, String> editType = new HashMap<>();
    private static final Map<UUID, String> editRecipeId = new HashMap<>();
    private static final Map<UUID, String> editField = new HashMap<>();
    private static final Map<UUID, String> previousGUI = new HashMap<>();
    private static final Map<String, CategoryInfo> DEFAULT_CATEGORIES = new LinkedHashMap<>();
    private static List<ItemFlag> cachedAvailableFlags = null;

    static {
        // Core categories
        DEFAULT_CATEGORIES.put("all", new CategoryInfo("&f&lAll Items", "NETHER_STAR", "&7All recipes"));
        DEFAULT_CATEGORIES.put("default", new CategoryInfo("&7Default", "CRAFTING_TABLE", "&7Uncategorized recipes"));

        // Item type categories
        DEFAULT_CATEGORIES.put("tools", new CategoryInfo("&6Tools & Weapons", "DIAMOND_SWORD", "&7Swords, pickaxes, axes, etc."));
        DEFAULT_CATEGORIES.put("armor", new CategoryInfo("&bArmor & Protection", "DIAMOND_CHESTPLATE", "&7Helmets, chestplates, leggings, boots"));
        DEFAULT_CATEGORIES.put("blocks", new CategoryInfo("&aBlocks & Building", "STONE", "&7Building and decorative blocks"));
        DEFAULT_CATEGORIES.put("food", new CategoryInfo("&cFood & Consumables", "GOLDEN_APPLE", "&7Food items and potions"));
        DEFAULT_CATEGORIES.put("materials", new CategoryInfo("&eMaterials & Resources", "IRON_INGOT", "&7Raw materials and ingots"));
        DEFAULT_CATEGORIES.put("misc", new CategoryInfo("&dMiscellaneous", "CHEST", "&7Various useful items"));
        DEFAULT_CATEGORIES.put("special", new CategoryInfo("&5&lSpecial Items", "END_CRYSTAL", "&7Rare and unique items"));
        DEFAULT_CATEGORIES.put("custom", new CategoryInfo("&3Custom", "COMMAND_BLOCK", "&7Custom category recipes"));
    }

    public static Map<String, CategoryInfo> getDefaultCategories() {
        return Collections.unmodifiableMap(DEFAULT_CATEGORIES);
    }

    public static List<String> getCategoryNames() {
        return new ArrayList<>(DEFAULT_CATEGORIES.keySet());
    }

    public static CategoryInfo getCategoryInfo(String category) {
        return DEFAULT_CATEGORIES.getOrDefault(category.toLowerCase(),
                new CategoryInfo("&7" + category, "PAPER", "&7Custom category"));
    }

    public static String suggestCategory(String material) {
        if (material == null || material.isEmpty()) return "default";

        String mat = material.toUpperCase();

        if (mat.contains("SWORD") || mat.contains("AXE") || mat.contains("PICKAXE") ||
                mat.contains("SHOVEL") || mat.contains("HOE") || mat.contains("BOW") ||
                mat.contains("CROSSBOW") || mat.contains("TRIDENT") || mat.contains("FISHING_ROD") ||
                mat.contains("SHEARS") || mat.contains("FLINT_AND_STEEL")) {
            return "tools";
        }

        if (mat.contains("HELMET") || mat.contains("CHESTPLATE") || mat.contains("LEGGINGS") ||
                mat.contains("BOOTS") || mat.contains("SHIELD") || mat.contains("ELYTRA") ||
                mat.contains("HORSE_ARMOR")) {
            return "armor";
        }

        if (mat.contains("APPLE") || mat.contains("BREAD") || mat.contains("COOKED") ||
                mat.contains("STEAK") || mat.contains("PORKCHOP") || mat.contains("CHICKEN") ||
                mat.contains("MUTTON") || mat.contains("RABBIT") || mat.contains("COD") ||
                mat.contains("SALMON") || mat.contains("CAKE") || mat.contains("COOKIE") ||
                mat.contains("PIE") || mat.contains("SOUP") || mat.contains("STEW") ||
                mat.contains("POTION") || mat.contains("CARROT") || mat.contains("POTATO") ||
                mat.contains("BEETROOT") || mat.contains("MELON") || mat.contains("CHORUS")) {
            return "food";
        }

        if (mat.contains("INGOT") || mat.contains("NUGGET") || mat.contains("RAW_") ||
                mat.contains("DIAMOND") || mat.contains("EMERALD") || mat.contains("LAPIS") ||
                mat.contains("REDSTONE") || mat.contains("COAL") || mat.contains("QUARTZ") ||
                mat.contains("AMETHYST") || mat.contains("COPPER") || mat.contains("NETHERITE") ||
                mat.contains("GOLD") || mat.contains("IRON") || mat.contains("LEATHER") ||
                mat.contains("STRING") || mat.contains("FEATHER") || mat.contains("BONE") ||
                mat.contains("GUNPOWDER") || mat.contains("BLAZE") || mat.contains("ENDER") ||
                mat.contains("GHAST") || mat.contains("SLIME") || mat.contains("PHANTOM") ||
                mat.contains("SCUTE") || mat.contains("MEMBRANE") || mat.contains("SHELL")) {
            return "materials";
        }

        if (mat.contains("STONE") || mat.contains("BRICK") || mat.contains("WOOD") ||
                mat.contains("PLANKS") || mat.contains("LOG") || mat.contains("GLASS") ||
                mat.contains("CONCRETE") || mat.contains("TERRACOTTA") || mat.contains("WOOL") ||
                mat.contains("CARPET") || mat.contains("STAIRS") || mat.contains("SLAB") ||
                mat.contains("FENCE") || mat.contains("WALL") || mat.contains("DOOR") ||
                mat.contains("TRAPDOOR") || mat.contains("GATE") || mat.contains("LANTERN") ||
                mat.contains("TORCH") || mat.contains("CANDLE") || mat.contains("BED") ||
                mat.contains("BANNER") || mat.contains("SIGN") || mat.contains("DEEPSLATE") ||
                mat.contains("COPPER_BLOCK") || mat.contains("AMETHYST_BLOCK") ||
                mat.contains("OBSIDIAN") || mat.contains("CRYING") || mat.contains("PRISMARINE") ||
                mat.contains("PURPUR") || mat.contains("END_STONE") || mat.contains("SANDSTONE") ||
                mat.contains("BLACKSTONE") || mat.contains("BASALT") || mat.contains("CALCITE") ||
                mat.contains("DRIPSTONE") || mat.contains("TUFF") || mat.contains("MUD")) {
            return "blocks";
        }

        if (mat.contains("NETHER_STAR") || mat.contains("BEACON") || mat.contains("DRAGON") ||
                mat.contains("END_CRYSTAL") || mat.contains("TOTEM") || mat.contains("ENCHANTED") ||
                mat.contains("HEART_OF_THE_SEA") || mat.contains("CONDUIT") || mat.contains("LODESTONE") ||
                mat.contains("RESPAWN_ANCHOR") || mat.contains("NETHERITE")) {
            return "special";
        }

        return "misc";
    }

    public static List<String> getSuggestedCategories(String material) {
        List<String> suggestions = new ArrayList<>();
        String primary = suggestCategory(material);
        suggestions.add(primary);

        if (!primary.equals("default")) suggestions.add("default");
        if (!primary.equals("misc")) suggestions.add("misc");
        if (!primary.equals("custom")) suggestions.add("custom");

        return suggestions;
    }

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
        editType.put(player.getUniqueId(), "category_edit");
        editRecipeId.put(player.getUniqueId(), recipe.getId());
        player.closeInventory();

        player.sendMessage(ChatUtils.colorize(
                File.getMessage().getString("crafting.edit_category_prompt")));
        player.sendMessage(ChatUtils.colorize(
                File.getMessage().getString("crafting.edit_category_hint")));

        displayAvailableCategories(player);

        displayCurrentCategory(player, recipe);
    }

    private static void displayAvailableCategories(Player player) {
        StringBuilder categories = new StringBuilder();
        int count = 0;
        for (String cat : DEFAULT_CATEGORIES.keySet()) {
            if (cat.equals("all")) continue; // Skip "all" vì nó chỉ dùng cho filter
            if (count > 0) categories.append("&7, ");
            categories.append("&e").append(cat);
            count++;
        }
        player.sendMessage(ChatUtils.colorize(
                File.getMessage().getString("crafting.edit_category_available")
                        .replace("#categories#", categories.toString())));
    }

    private static void displayCurrentCategory(Player player, Recipe recipe) {
        String current = recipe.getCategory();
        CategoryInfo info = getCategoryInfo(current);

        player.sendMessage(ChatUtils.colorize(
                File.getMessage().getString("crafting.edit_category_current")
                        .replace("#current#", current)
                        .replace("#display#", info.displayName)));

        String suggested = suggestCategory(recipe.getResultMaterial());
        if (!suggested.equals(current)) {
            player.sendMessage(ChatUtils.colorize(
                    File.getMessage().getString("crafting.edit_category_suggested")
                            .replace("#suggested#", suggested)
                            .replace("#material#", recipe.getResultMaterial())));
        }
    }

    private static void promptCategoryRetry(Player player, Recipe recipe) {
        player.sendMessage(ChatUtils.colorize(
                File.getMessage().getString("crafting.edit_category_prompt")));
        player.sendMessage(ChatUtils.colorize(
                File.getMessage().getString("crafting.edit_category_hint")));
        displayAvailableCategories(player);
        displayCurrentCategory(player, recipe);
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
        } else if (!"material".equals(type) && !"enchant_edit".equals(type) && !"flag_edit".equals(type) && !"lore_edit".equals(type) && !"permission_edit".equals(type) && !"category_edit".equals(type)) {
            clearEditData(playerId);
        }
        return true;
    }

    private static void updateRecipeAndReopenGUI(Player player, Recipe recipe, String editType) {
        CraftingManager.updateRecipe(recipe);
        RecipeEditorGUI.updateBackup(player.getUniqueId(), recipe);

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

                    Material resolvedMaterial = MaterialUtils.matchMaterial(input);
                    if (resolvedMaterial == null) {
                        player.sendMessage(ChatUtils.colorize(
                                File.getMessage().getString("crafting.edit_material_invalid")
                                        .replace("#material#", input)));
                        promptMaterialRetry(player, recipe);
                        return false;
                    }
                    recipe.setResultMaterial(resolvedMaterial.name());
                    player.sendMessage(ChatUtils.colorize(
                            File.getMessage().getString("crafting.edit_material_success")
                                    .replace("#material#", resolvedMaterial.name())));
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

                case "category_edit":
                    return processCategoryEdit(player, recipe, input);

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
                lore.remove(lore.size() - 1);
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

    private static boolean processCategoryEdit(Player player, Recipe recipe, String input) {
        String[] parts = input.trim().split("\\s+", 2);
        if (parts.length == 0 || parts[0].trim().isEmpty()) {
            player.sendMessage(ChatUtils.colorize(
                    File.getMessage().getString("crafting.edit_category_hint")));
            promptCategoryRetry(player, recipe);
            return false;
        }

        String command = parts[0].toLowerCase();

        if ("set".equalsIgnoreCase(command)) {
            if (parts.length < 2 || parts[1].trim().isEmpty()) {
                player.sendMessage(ChatUtils.colorize(
                        File.getMessage().getString("crafting.edit_category_set_invalid")));
                promptCategoryRetry(player, recipe);
                return false;
            }

            String newCategory = parts[1].trim().toLowerCase();
            if (newCategory.equals("all")) {
                player.sendMessage(ChatUtils.colorize(
                        File.getMessage().getString("crafting.edit_category_all_reserved")));
                promptCategoryRetry(player, recipe);
                return false;
            }

            if (!CraftingManager.isValidCategory(newCategory)) {
                player.sendMessage(ChatUtils.colorize(
                        File.getMessage().getString("crafting.edit_category_invalid")
                                .replace("#category#", newCategory)));
                promptCategoryRetry(player, recipe);
                return false;
            }

            recipe.setCategory(newCategory);
            String displayName = CraftingManager.getCategoryDisplayName(newCategory);
            player.sendMessage(ChatUtils.colorize(
                    File.getMessage().getString("crafting.edit_category_set_success")
                            .replace("#category#", newCategory)
                            .replace("#display#", displayName)));
            return true;
        }

        if ("auto".equalsIgnoreCase(command)) {
            String suggested = suggestCategory(recipe.getResultMaterial());
            recipe.setCategory(suggested);
            String displayName = CraftingManager.getCategoryDisplayName(suggested);
            player.sendMessage(ChatUtils.colorize(
                    File.getMessage().getString("crafting.edit_category_auto_success")
                            .replace("#category#", suggested)
                            .replace("#display#", displayName)
                            .replace("#material#", recipe.getResultMaterial())));
            return true;
        }

        if ("list".equalsIgnoreCase(command)) {
            player.sendMessage(ChatUtils.colorize(
                    File.getMessage().getString("crafting.edit_category_list_header")));
            for (String key : CraftingManager.getValidCategoryKeys()) {
                String displayName = CraftingManager.getCategoryDisplayName(key);
                CategoryInfo info = getCategoryInfo(key);
                String current = recipe.getCategory().equalsIgnoreCase(key) ? " &a✓" : "";
                player.sendMessage(ChatUtils.colorize(
                        File.getMessage().getString("crafting.edit_category_list_entry")
                                .replace("#key#", key)
                                .replace("#name#", displayName)
                                .replace("#desc#", info.description)
                                .replace("#current#", current)));
            }
            promptCategoryRetry(player, recipe);
            return false;
        }

        if ("suggest".equalsIgnoreCase(command)) {
            List<String> suggestions = getSuggestedCategories(recipe.getResultMaterial());
            player.sendMessage(ChatUtils.colorize(
                    File.getMessage().getString("crafting.edit_category_suggest_header")
                            .replace("#material#", recipe.getResultMaterial())));
            for (int i = 0; i < suggestions.size(); i++) {
                String cat = suggestions.get(i);
                String displayName = CraftingManager.getCategoryDisplayName(cat);
                String prefix = i == 0 ? "&a★ " : "&7• ";
                player.sendMessage(ChatUtils.colorize(prefix + "&e" + cat + " &7- " + displayName));
            }
            promptCategoryRetry(player, recipe);
            return false;
        }

        String directCategory = input.trim().toLowerCase();
        if (!directCategory.isEmpty()) {
            if (directCategory.equals("all")) {
                player.sendMessage(ChatUtils.colorize(
                        File.getMessage().getString("crafting.edit_category_all_reserved")));
                promptCategoryRetry(player, recipe);
                return false;
            }

            if (!CraftingManager.isValidCategory(directCategory)) {
                player.sendMessage(ChatUtils.colorize(
                        File.getMessage().getString("crafting.edit_category_invalid")
                                .replace("#category#", directCategory)));
                promptCategoryRetry(player, recipe);
                return false;
            }

            recipe.setCategory(directCategory);
            String displayName = CraftingManager.getCategoryDisplayName(directCategory);
            player.sendMessage(ChatUtils.colorize(
                    File.getMessage().getString("crafting.edit_category_set_success")
                            .replace("#category#", directCategory)
                            .replace("#display#", displayName)));
            return true;
        }

        player.sendMessage(ChatUtils.colorize(
                File.getMessage().getString("crafting.edit_category_hint")));
        promptCategoryRetry(player, recipe);
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

    // Category info holder
    public static class CategoryInfo {
        public final String displayName;
        public final String icon;
        public final String description;

        public CategoryInfo(String displayName, String icon, String description) {
            this.displayName = displayName;
            this.icon = icon;
            this.description = description;
        }
    }
}
