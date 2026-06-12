package net.danh.storage.GUI.Crafting;

import net.danh.storage.GUI.ConfirmationGUI;
import net.danh.storage.GUI.MaterialEditorGUI;
import net.danh.storage.GUI.manager.IGUI;
import net.danh.storage.GUI.manager.InteractiveItem;
import net.danh.storage.Manager.Crafting.CraftingManager;
import net.danh.storage.Manager.Crafting.RecipeEditManager;
import net.danh.storage.Manager.ItemManager;
import net.danh.storage.Manager.SoundManager;
import net.danh.storage.Recipe.Recipe;
import net.danh.storage.Storage;
import net.danh.storage.Utils.*;
import net.danh.storage.Utils.Number;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class RecipeEditorGUI implements IGUI {

    private static final Map<UUID, RecipeBackup> recipeBackups = new HashMap<>();
    private static final Map<UUID, Boolean> shouldRestoreOnClose = new HashMap<>();
    private static final Map<UUID, String> editingRecipeIds = new HashMap<>();

    private final Player player;
    private final Recipe recipe;
    private final FileConfiguration config;

    public RecipeEditorGUI(Player player, Recipe recipe) {
        this.player = player;
        this.recipe = recipe;
        this.config = File.getRecipeEditorGUIConfig();

        createBackup();
        shouldRestoreOnClose.put(player.getUniqueId(), true);
        editingRecipeIds.put(player.getUniqueId(), recipe.getId());
    }

    public static void cleanupBackup(UUID playerUUID) {
        recipeBackups.remove(playerUUID);
        shouldRestoreOnClose.remove(playerUUID);
        editingRecipeIds.remove(playerUUID);
    }

    public static void setShouldRestoreOnClose(Player player, boolean shouldRestore) {
        shouldRestoreOnClose.put(player.getUniqueId(), shouldRestore);
    }

    public static boolean getShouldRestoreOnClose(Player player) {
        return shouldRestoreOnClose.getOrDefault(player.getUniqueId(), false);
    }

    public static boolean hasActiveSession(UUID playerUUID) {
        return recipeBackups.containsKey(playerUUID);
    }

    public static String getBackupRecipeName(UUID playerUUID) {
        RecipeBackup backup = recipeBackups.get(playerUUID);
        return backup != null ? backup.name : "Unknown";
    }

    public static boolean restoreAndSaveBackup(UUID playerUUID) {
        RecipeBackup backup = recipeBackups.get(playerUUID);
        String recipeId = editingRecipeIds.get(playerUUID);

        if (backup == null || recipeId == null) {
            return false;
        }

        Recipe recipe = CraftingManager.getRecipe(recipeId);
        if (recipe == null) {
            return false;
        }

        recipe.setName(backup.name);
        recipe.setCategory(backup.category);
        recipe.setEnabled(backup.enabled);
        recipe.setResultMaterial(backup.resultMaterial);
        recipe.setResultName(backup.resultName);
        recipe.setResultLore(new ArrayList<>(backup.resultLore));
        recipe.setResultEnchantments(new HashMap<>(backup.resultEnchantments));
        recipe.setResultAmount(backup.resultAmount);
        recipe.setResultCustomModelData(backup.resultCustomModelData);
        recipe.setResultUnbreakable(backup.resultUnbreakable);
        recipe.setResultFlags(new HashSet<>(backup.resultFlags));
        recipe.setMaterialRequirements(new HashMap<>(backup.materialRequirements));
        recipe.setPermissionRequirement(backup.permissionRequirement);

        CraftingManager.saveRecipes();

        return true;
    }

    private static RecipeBackup createBackupFromRecipe(Recipe recipe) {
        return new RecipeBackup(
                recipe.getName(),
                recipe.getCategory(),
                recipe.isEnabled(),
                recipe.getResultMaterial(),
                recipe.getResultName(),
                new ArrayList<>(recipe.getResultLore()),
                new HashMap<>(recipe.getResultEnchantments()),
                recipe.getResultAmount(),
                recipe.getResultCustomModelData(),
                recipe.isResultUnbreakable(),
                new HashSet<>(recipe.getResultFlags()),
                new HashMap<>(recipe.getMaterialRequirements()),
                recipe.getPermissionRequirement()
        );
    }

    public static void updateBackup(UUID playerUUID, Recipe recipe) {
        if (recipeBackups.containsKey(playerUUID)) {
            recipeBackups.put(playerUUID, createBackupFromRecipe(recipe));
        }
    }

    private void createBackup() {
        if (recipeBackups.containsKey(player.getUniqueId())) {
            return;
        }

        recipeBackups.put(player.getUniqueId(), createBackupFromRecipe(recipe));
    }

    private void restoreBackup() {
        RecipeBackup backup = recipeBackups.get(player.getUniqueId());
        if (backup != null) {
            recipe.setName(backup.name);
            recipe.setCategory(backup.category);
            recipe.setEnabled(backup.enabled);
            recipe.setResultMaterial(backup.resultMaterial);
            recipe.setResultName(backup.resultName);
            recipe.setResultLore(new ArrayList<>(backup.resultLore));
            recipe.setResultEnchantments(new HashMap<>(backup.resultEnchantments));
            recipe.setResultAmount(backup.resultAmount);
            recipe.setResultCustomModelData(backup.resultCustomModelData);
            recipe.setResultUnbreakable(backup.resultUnbreakable);
            recipe.setResultFlags(new HashSet<>(backup.resultFlags));
            recipe.setMaterialRequirements(new HashMap<>(backup.materialRequirements));
            recipe.setPermissionRequirement(backup.permissionRequirement);
        }
    }

    private void clearBackup() {
        recipeBackups.remove(player.getUniqueId());
    }

    @NotNull
    @Override
    public Inventory getInventory() {
        return getInventory(SoundContext.INITIAL_OPEN);
    }

    @NotNull
    @Override
    public Inventory getInventory(SoundContext context) {
        SoundManager.playItemSound(player, config, "gui_open_sound", context);

        String title = ChatUtils.colorizewp(player, Objects.requireNonNull(
                config.getString("title")).replace("#recipe_name#",
                recipe.getName()));
        Inventory inventory = Bukkit.createInventory(this, config.getInt("size") * 9, title);

        setupItems(inventory);
        return inventory;
    }

    private void setupItems(Inventory inventory) {
        setupDecorativeItems(inventory);
        addResultPreview(inventory);
        addItemConfigurationPanel(inventory);
        addRequirementsPanel(inventory);
        addControlButtons(inventory);
    }

    private void setupDecorativeItems(Inventory inventory) {
        String decorateSlots = config.getString("items.decorates.slot");
        if (decorateSlots != null) {
            for (String slotStr : decorateSlots.split(",")) {
                int slot = Number.getInteger(slotStr.trim());
                InteractiveItem decorateItem = new InteractiveItem(
                        ItemManager.getItemConfig(Objects.requireNonNull(config.getConfigurationSection("items.decorates"))),
                        slot
                );
                inventory.setItem(decorateItem.getSlot(), decorateItem);
            }
        }
    }

    private void addResultPreview(Inventory inventory) {
        ItemStack previewItem = CraftingManager.createResultItem(recipe);
        if (previewItem == null) {
            previewItem = ItemManager.getItemConfig(Objects.requireNonNull(config.getConfigurationSection("items.result_preview")));
        }

        // Preview item already has lore from config, no need to add more

        String slotConfig = config.getString("items.result_preview.slot", "13");
        final ItemStack finalPreviewItem = previewItem;
        if (slotConfig.contains(",")) {
            for (String slotStr : slotConfig.split(",")) {
                int slot = Number.getInteger(slotStr.trim());
                InteractiveItem resultPreview = new InteractiveItem(finalPreviewItem.clone(), slot);
                inventory.setItem(resultPreview.getSlot(), resultPreview);
            }
        } else {
            int slot = Number.getInteger(slotConfig);
            InteractiveItem resultPreview = new InteractiveItem(finalPreviewItem, slot);
            inventory.setItem(resultPreview.getSlot(), resultPreview);
        }
    }

    private void addItemConfigurationPanel(Inventory inventory) {
        // Material editor - dynamically shows the recipe's material as icon
        String rawMaterial = recipe.getResultMaterial();
        String displayMaterial = rawMaterial;
        if (displayMaterial != null) {
            displayMaterial = displayMaterial.trim();
            if (displayMaterial.contains(";")) {
                displayMaterial = displayMaterial.split(";", 2)[0];
            }
            if (displayMaterial.contains(":")) {
                displayMaterial = displayMaterial.split(":", 2)[0];
            }
        }
        ItemStack materialItem = createDynamicMaterialItem("items.edit_material",
                displayMaterial,
                "#current_material#", displayMaterial);
        if (materialItem != null) {
            setupInteractiveItem(inventory, "items.edit_material", 10, materialItem,
                    item -> item.onLeftClick(p -> editMaterial(p)));
        }

        // Name editor
        ItemStack nameItem = createConfigItem("items.edit_name",
                "#current_name#", recipe.getResultName());
        if (nameItem != null) {
            setupInteractiveItem(inventory, "items.edit_name", 11, nameItem,
                    item -> item.onLeftClick(p -> editName(p)));
        }

        // Lore editor
        ItemStack loreItem = createConfigItem("items.edit_lore",
                "#lore_count#", String.valueOf(recipe.getResultLore().size()));
        if (loreItem != null) {
            setupInteractiveItem(inventory, "items.edit_lore", 12, loreItem,
                    item -> item.onClick((p, clickType) -> editLore(p, clickType)));
        }

        // Amount editor
        ItemStack amountItem = createConfigItem("items.edit_amount",
                "#current_amount#", String.valueOf(recipe.getResultAmount()));
        if (amountItem != null) {
            amountItem.setAmount(Math.max(1, Math.min(64, recipe.getResultAmount())));
            setupInteractiveItem(inventory, "items.edit_amount", 14, amountItem,
                    item -> item.onClick((p, clickType) -> editAmount(p, clickType)));
        }

        // Enchantments editor
        ItemStack enchantItem = createConfigItem("items.edit_enchantments",
                "#enchant_count#", String.valueOf(recipe.getResultEnchantments().size()));
        if (enchantItem != null) {
            setupInteractiveItem(inventory, "items.edit_enchantments", 15, enchantItem,
                    item -> item.onLeftClick(p -> editEnchantments(p)));
        }

        // Flags editor
        ItemStack flagsItem = createConfigItem("items.edit_flags",
                "#flag_count#", String.valueOf(recipe.getResultFlags().size()));
        if (flagsItem != null) {
            setupInteractiveItem(inventory, "items.edit_flags", 16, flagsItem,
                    item -> item.onLeftClick(p -> editFlags(p)));
        }

        // Custom Model Data editor
        ItemStack cmdItem = createConfigItem("items.edit_custom_model_data",
                "#current_cmd#", String.valueOf(recipe.getResultCustomModelData()));
        if (cmdItem != null) {
            setupInteractiveItem(inventory, "items.edit_custom_model_data", 19, cmdItem,
                    item -> item.onLeftClick(p -> editCustomModelData(p)));
        }

        // Unbreakable toggle
        ItemStack unbreakableItem = createConfigItem("items.toggle_unbreakable",
                "#status#", recipe.isResultUnbreakable() ? "&aEnabled" : "&cDisabled");
        if (unbreakableItem != null) {
            setupInteractiveItem(inventory, "items.toggle_unbreakable", 20, unbreakableItem,
                    item -> item.onLeftClick(p -> toggleUnbreakable(p)));
        }
    }

    private void addRequirementsPanel(Inventory inventory) {
        String permissionStatus = recipe.getPermissionRequirement() == null || recipe.getPermissionRequirement().trim().isEmpty()
                ? "None" : recipe.getPermissionRequirement();
        ItemStack permReqItem = createConfigItem("items.permission_requirements",
                "#permission#", permissionStatus);
        if (permReqItem != null) {
            setupInteractiveItem(inventory, "items.permission_requirements", 28, permReqItem,
                    item -> item.onClick((p, clickType) -> handlePermissionClick(p, clickType)));
        }

        ItemStack addMaterialItem = ItemManager.getItemConfig(Objects.requireNonNull(config.getConfigurationSection("items.add_material")));
        setupInteractiveItem(inventory, "items.add_material", 37, addMaterialItem,
                item -> item.onLeftClick(p -> openMaterialEditor(p)));

        displayRequirements(inventory);
    }

    private void displayRequirements(Inventory inventory) {
        String requirementSlots = config.getString("items.requirement_slot.slot");
        if (requirementSlots == null) return;

        String[] slotArray = requirementSlots.split(",");
        List<Map.Entry<String, Integer>> requirements = new ArrayList<>(recipe.getMaterialRequirements().entrySet());

        for (int i = 0; i < Math.min(requirements.size(), slotArray.length); i++) {
            Map.Entry<String, Integer> requirement = requirements.get(i);

            try {
                int slot = Integer.parseInt(slotArray[i].trim());

                String materialKey = requirement.getKey();
                String materialName = materialKey;
                if (materialKey.contains(";")) {
                    materialName = materialKey.split(";")[0];
                }

                String displayName = File.getConfig().getString("items." + materialKey, materialName);

                ItemStack reqItem = ItemManager.getItemConfigWithPlaceholders(player,
                        config.getConfigurationSection("items.requirement_slot"),
                        "#material#", displayName,
                        "#amount#", String.valueOf(requirement.getValue()));

                if (reqItem != null) {
                    reqItem.setAmount(Math.min(64, requirement.getValue()));
                    InteractiveItem interactiveItem = new InteractiveItem(reqItem, slot)
                            .onClick((p, clickType) -> editRequirement(p, requirement.getKey(), clickType));
                    inventory.setItem(interactiveItem.getSlot(), interactiveItem);
                }
            } catch (NumberFormatException e) {
                // Skip invalid slot numbers
            }
        }
    }

    private void addControlButtons(Inventory inventory) {
        // Recipe settings button
        ItemStack settingsItem = createConfigItem("items.recipe_settings",
                "#category#", recipe.getCategory(),
                "#status#", recipe.isEnabled() ? "&aEnabled" : "&cDisabled");
        if (settingsItem != null) {
            setupInteractiveItem(inventory, "items.recipe_settings", 48, settingsItem,
                    item -> item.onLeftClick(p -> editRecipeSettings(p)));
        }

        // Save button
        ItemStack saveItem = ItemManager.getItemConfig(Objects.requireNonNull(config.getConfigurationSection("items.save")));
        setupInteractiveItem(inventory, "items.save", 50, saveItem,
                item -> item.onLeftClick(p -> saveRecipe(p)));

        // Back button
        ItemStack backItem = ItemManager.getItemConfig(Objects.requireNonNull(config.getConfigurationSection("items.back")));
        setupInteractiveItem(inventory, "items.back", 49, backItem,
                item -> item.onLeftClick(p -> backToList(p)));

        // Delete button
        ItemStack deleteItem = ItemManager.getItemConfig(Objects.requireNonNull(config.getConfigurationSection("items.delete")));
        setupInteractiveItem(inventory, "items.delete", 53, deleteItem,
                item -> item.onLeftClick(p -> deleteRecipe(p)));
    }

    private void setupInteractiveItem(Inventory inventory, String configPath, int defaultSlot, ItemStack itemStack,
                                      java.util.function.Consumer<InteractiveItem> clickHandler) {
        String slotConfig = config.getString(configPath + ".slot", String.valueOf(defaultSlot));
        if (slotConfig.contains(",")) {
            for (String slotStr : slotConfig.split(",")) {
                int slot = Number.getInteger(slotStr.trim());
                InteractiveItem item = new InteractiveItem(itemStack.clone(), slot);
                clickHandler.accept(item);
                inventory.setItem(item.getSlot(), item);
            }
        } else {
            int slot = Number.getInteger(slotConfig);
            InteractiveItem item = new InteractiveItem(itemStack, slot);
            clickHandler.accept(item);
            inventory.setItem(item.getSlot(), item);
        }
    }

    private ItemStack createConfigItem(String configPath, String... placeholders) {
        try {
            org.bukkit.configuration.ConfigurationSection section = config.getConfigurationSection(configPath);
            if (section == null) {
                Storage.getStorage().getLogger().warning("Missing config section: " + configPath);
                return createFallbackItem(configPath);
            }

            ItemStack item = ItemManager.getItemConfigWithPlaceholders(player, section, placeholders);
            if (item == null) {
                return createFallbackItem(configPath);
            }
            return item;
        } catch (Exception e) {
            Storage.getStorage().getLogger().warning("Error loading config item: " + configPath + " - " + e.getMessage());
            return createFallbackItem(configPath);
        }
    }

    private ItemStack createFallbackItem(String configPath) {
        ItemStack fallback = new ItemStack(Material.STONE);
        if (fallback != null) {
            ItemMeta meta = fallback.getItemMeta();
            if (meta != null) {
                String errorMsg = File.getMessage().getString("crafting.config_error", "&cConfig Error");
                String errorDetail = File.getMessage().getString("crafting.config_error_detail", "&7Cannot load: #path#")
                        .replace("#path#", configPath);
                meta.setDisplayName(ChatUtils.colorizewp(errorMsg));
                meta.setLore(Collections.singletonList(ChatUtils.colorizewp(errorDetail)));
                fallback.setItemMeta(meta);
            }
        }
        return fallback;
    }

    private ItemStack createDynamicMaterialItem(String configPath, String recipeMaterial, String... placeholders) {
        try {
            org.bukkit.configuration.ConfigurationSection section = config.getConfigurationSection(configPath);
            if (section == null) {
                Storage.getStorage().getLogger().warning("Missing config section: " + configPath);
                return createFallbackItem(configPath);
            }

            ItemStack item = null;
            if (recipeMaterial != null && !recipeMaterial.isEmpty()) {
                String materialName = recipeMaterial.contains(";") ? recipeMaterial.split(";")[0] : recipeMaterial;
                item = MaterialUtils.createItem(materialName);
            }

            if (item == null) {
                item = ItemManager.getItemConfigWithPlaceholders(player, section, placeholders);
                return item != null ? item : createFallbackItem(configPath);
            }

            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                String displayName = section.getString("name");
                if (displayName != null) {
                    for (int i = 0; i < placeholders.length - 1; i += 2) {
                        displayName = displayName.replace(placeholders[i], placeholders[i + 1]);
                    }
                    meta.setDisplayName(ChatUtils.colorizewp(displayName));
                }

                List<String> lore = section.getStringList("lore");
                if (lore != null && !lore.isEmpty()) {
                    List<String> processedLore = new ArrayList<>();
                    for (String line : lore) {
                        for (int i = 0; i < placeholders.length - 1; i += 2) {
                            line = line.replace(placeholders[i], placeholders[i + 1]);
                        }
                        processedLore.add(ChatUtils.colorizewp(line));
                    }
                    meta.setLore(processedLore);
                }

                item.setItemMeta(meta);
            }

            return item;
        } catch (Exception e) {
            Storage.getStorage().getLogger().warning("Error creating material item: " + configPath + " - " + e.getMessage());
            return createFallbackItem(configPath);
        }
    }

    private void editMaterial(Player player) {
        setShouldRestoreOnClose(player, false);
        RecipeEditManager.requestMaterialEdit(player, recipe);
    }

    private void editName(Player player) {
        setShouldRestoreOnClose(player, false);
        RecipeEditManager.requestNameEdit(player, recipe);
    }

    private void editLore(Player player, ClickType clickType) {
        setShouldRestoreOnClose(player, false);
        RecipeEditManager.requestLoreEdit(player, recipe);
    }

    private void editAmount(Player player, ClickType clickType) {
        if (clickType == ClickType.LEFT) {
            recipe.setResultAmount(Math.min(64, recipe.getResultAmount() + 1));
            CraftingManager.updateRecipe(recipe);
            updateBackup(player.getUniqueId(), recipe);
            refreshGUI(player);
        } else if (clickType == ClickType.RIGHT) {
            recipe.setResultAmount(Math.max(1, recipe.getResultAmount() - 1));
            CraftingManager.updateRecipe(recipe);
            updateBackup(player.getUniqueId(), recipe);
            refreshGUI(player);
        } else if (clickType == ClickType.SHIFT_LEFT || clickType == ClickType.SHIFT_RIGHT) {
            setShouldRestoreOnClose(player, false);
            RecipeEditManager.requestAmountEdit(player, recipe);
        }
    }

    private void editEnchantments(Player player) {
        setShouldRestoreOnClose(player, false);
        RecipeEditManager.requestEnchantmentEdit(player, recipe);
    }

    private void editFlags(Player player) {
        setShouldRestoreOnClose(player, false);
        RecipeEditManager.requestFlagEdit(player, recipe);
    }

    private void editCustomModelData(Player player) {
        setShouldRestoreOnClose(player, false);
        RecipeEditManager.requestCustomModelDataEdit(player, recipe);
    }

    private void toggleUnbreakable(Player player) {
        recipe.setResultUnbreakable(!recipe.isResultUnbreakable());
        CraftingManager.updateRecipe(recipe);
        updateBackup(player.getUniqueId(), recipe);
        refreshGUI(player);
        String statusKey = recipe.isResultUnbreakable() ? "crafting.status_on" : "crafting.status_off";
        String status = File.getMessage().getString(statusKey);
        player.sendMessage(ChatUtils.colorize(
                File.getMessage().getString("crafting.unbreakable_toggled")
                        .replace("#status#", status)));
    }

    private void openMaterialEditor(Player player) {
        setShouldRestoreOnClose(player, false);
        SoundManager.setShouldPlayCloseSound(player, false);
        player.openInventory(new MaterialEditorGUI(player, recipe).getInventory(SoundContext.SILENT));
    }

    private void editRequirement(Player player, String material, ClickType clickType) {
        if (clickType == ClickType.LEFT) {
            // Increase amount
            Map<String, Integer> requirements = recipe.getMaterialRequirements();
            int currentAmount = requirements.getOrDefault(material, 1);
            requirements.put(material, currentAmount + 1);
            recipe.setMaterialRequirements(requirements);
            CraftingManager.updateRecipe(recipe);
            updateBackup(player.getUniqueId(), recipe);
            refreshGUI(player);
        } else if (clickType == ClickType.RIGHT) {
            // Decrease amount
            Map<String, Integer> requirements = recipe.getMaterialRequirements();
            int currentAmount = requirements.getOrDefault(material, 1);
            if (currentAmount > 1) {
                requirements.put(material, currentAmount - 1);
                recipe.setMaterialRequirements(requirements);
                CraftingManager.updateRecipe(recipe);
                updateBackup(player.getUniqueId(), recipe);
                refreshGUI(player);
            }
        } else if (clickType == ClickType.SHIFT_LEFT) {
            // Custom amount
            setShouldRestoreOnClose(player, false);
            RecipeEditManager.requestRequirementAmountEdit(player, recipe, material);
        } else if (clickType == ClickType.SHIFT_RIGHT) {
            // Remove requirement
            RecipeEditManager.removeRequirement(recipe, material);
            CraftingManager.updateRecipe(recipe);
            updateBackup(player.getUniqueId(), recipe);
            refreshGUI(player);
            String materialName = material.contains(";") ? material.split(";")[0] : material;
            player.sendMessage(ChatUtils.colorize(File.getMessage().getString("crafting.requirement_removed_gui")
                    .replace("#material#", materialName)));
        }
    }

    private void editRecipeSettings(Player player) {
        setShouldRestoreOnClose(player, false);
        RecipeEditManager.requestCategoryEdit(player, recipe);
    }

    private void handlePermissionClick(Player player, ClickType clickType) {
        if (clickType == ClickType.LEFT) {
            setShouldRestoreOnClose(player, false);
            RecipeEditManager.requestPermissionEdit(player, recipe);
        } else if (clickType == ClickType.RIGHT) {
            recipe.setPermissionRequirement(null);
            CraftingManager.updateRecipe(recipe);
            updateBackup(player.getUniqueId(), recipe);
            SoundManager.playSound(player, SoundManager.SoundType.ACTION_SUCCESS);
            refreshGUI(player);
        }
    }

    private void saveRecipe(Player player) {
        setShouldRestoreOnClose(player, false);
        CraftingManager.updateRecipe(recipe);
        clearBackup();

        player.sendMessage(ChatUtils.colorize(File.getMessage().getString("crafting.editor_saved")
                .replace("#recipe#", recipe.getName())));

        SoundManager.setShouldPlayCloseSound(player, false);
        player.openInventory(new RecipeEditorListGUI(player).getInventory(SoundContext.SILENT));
    }

    private void backToList(Player player) {
        setShouldRestoreOnClose(player, false);
        restoreBackup();
        CraftingManager.saveRecipes();
        clearBackup();

        player.sendMessage(ChatUtils.colorize(File.getMessage().getString("crafting.editor_discarded")
                .replace("#recipe#", recipe.getName())));

        SoundManager.setShouldPlayCloseSound(player, false);
        player.openInventory(new RecipeEditorListGUI(player).getInventory(SoundContext.SILENT));
    }

    private void deleteRecipe(Player player) {
        // Mark as navigating to confirmation GUI
        setShouldRestoreOnClose(player, false);
        String message = "Delete recipe: &e" + recipe.getName() + "&7?";

        SoundManager.setShouldPlayCloseSound(player, false);
        player.openInventory(new ConfirmationGUI(player, message,
                () -> {
                    // On confirm - recipe deleted, no need to restore
                    CraftingManager.removeRecipe(recipe.getId());
                    clearBackup();
                    player.sendMessage(ChatUtils.colorize(File.getMessage().getString("crafting.editor_deleted")
                            .replace("#recipe#", recipe.getName())));
                    SoundManager.setShouldPlayCloseSound(player, false);
                    player.openInventory(new RecipeEditorListGUI(player).getInventory(SoundContext.SILENT));
                },
                () -> {
                    // On cancel - return to editor, re-enable restore on close
                    SoundManager.setShouldPlayCloseSound(player, false);
                    player.openInventory(new RecipeEditorGUI(player, recipe).getInventory(SoundContext.SILENT));
                }
        ).getInventory(SoundContext.SILENT));
    }

    private void refreshGUI(Player player) {
        setShouldRestoreOnClose(player, false);
        SoundManager.setShouldPlayCloseSound(player, false);
        player.openInventory(new RecipeEditorGUI(player, recipe).getInventory(SoundContext.SILENT));
    }

    private static class RecipeBackup {
        final String name;
        final String category;
        final boolean enabled;
        final String resultMaterial;
        final String resultName;
        final List<String> resultLore;
        final Map<String, Integer> resultEnchantments;
        final int resultAmount;
        final int resultCustomModelData;
        final boolean resultUnbreakable;
        final Set<org.bukkit.inventory.ItemFlag> resultFlags;
        final Map<String, Integer> materialRequirements;
        final String permissionRequirement;

        RecipeBackup(String name, String category, boolean enabled,
                     String resultMaterial, String resultName, List<String> resultLore,
                     Map<String, Integer> resultEnchantments, int resultAmount,
                     int resultCustomModelData, boolean resultUnbreakable,
                     Set<org.bukkit.inventory.ItemFlag> resultFlags,
                     Map<String, Integer> materialRequirements,
                     String permissionRequirement) {
            this.name = name;
            this.category = category;
            this.enabled = enabled;
            this.resultMaterial = resultMaterial;
            this.resultName = resultName;
            this.resultLore = resultLore;
            this.resultEnchantments = resultEnchantments;
            this.resultAmount = resultAmount;
            this.resultCustomModelData = resultCustomModelData;
            this.resultUnbreakable = resultUnbreakable;
            this.resultFlags = resultFlags;
            this.materialRequirements = materialRequirements;
            this.permissionRequirement = permissionRequirement;
        }
    }
}
