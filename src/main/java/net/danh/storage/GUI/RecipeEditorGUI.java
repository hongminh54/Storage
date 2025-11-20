package net.danh.storage.GUI;

import com.cryptomorin.xseries.XMaterial;
import net.danh.storage.GUI.manager.IGUI;
import net.danh.storage.GUI.manager.InteractiveItem;
import net.danh.storage.Manager.CraftingManager;
import net.danh.storage.Manager.ItemManager;
import net.danh.storage.Manager.RecipeEditManager;
import net.danh.storage.Manager.SoundManager;
import net.danh.storage.Recipe.Recipe;
import net.danh.storage.Storage;
import net.danh.storage.Utils.ChatUtils;
import net.danh.storage.Utils.File;
import net.danh.storage.Utils.Number;
import net.danh.storage.Utils.SoundContext;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class RecipeEditorGUI implements IGUI {

    private final Player player;
    private final Recipe recipe;
    private final FileConfiguration config;

    public RecipeEditorGUI(Player player, Recipe recipe) {
        this.player = player;
        this.recipe = recipe;
        this.config = File.getRecipeEditorGUIConfig();
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

        String title = ChatUtils.colorizewp(Objects.requireNonNull(config.getString("title"))
                .replace("#recipe_name#", recipe.getName()));
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

        InteractiveItem resultPreview = new InteractiveItem(previewItem, 13);
        inventory.setItem(resultPreview.getSlot(), resultPreview);
    }

    private void addItemConfigurationPanel(Inventory inventory) {
        // Material editor
        ItemStack materialItem = createConfigItem("items.edit_material",
                "#current_material#", recipe.getResultMaterial());
        if (materialItem != null) {
            InteractiveItem materialEditor = new InteractiveItem(materialItem, 10)
                    .onLeftClick(p -> editMaterial(p));
            inventory.setItem(materialEditor.getSlot(), materialEditor);
        }

        // Name editor
        ItemStack nameItem = createConfigItem("items.edit_name",
                "#current_name#", recipe.getResultName());
        if (nameItem != null) {
            InteractiveItem nameEditor = new InteractiveItem(nameItem, 11)
                    .onLeftClick(p -> editName(p));
            inventory.setItem(nameEditor.getSlot(), nameEditor);
        }

        // Lore editor
        ItemStack loreItem = createConfigItem("items.edit_lore",
                "#lore_count#", String.valueOf(recipe.getResultLore().size()));
        if (loreItem != null) {
            InteractiveItem loreEditor = new InteractiveItem(loreItem, 12)
                    .onClick((p, clickType) -> editLore(p, clickType));
            inventory.setItem(loreEditor.getSlot(), loreEditor);
        }

        // Amount editor
        ItemStack amountItem = createConfigItem("items.edit_amount",
                "#current_amount#", String.valueOf(recipe.getResultAmount()));
        if (amountItem != null) {
            amountItem.setAmount(Math.max(1, Math.min(64, recipe.getResultAmount())));
            InteractiveItem amountEditor = new InteractiveItem(amountItem, 14)
                    .onClick((p, clickType) -> editAmount(p, clickType));
            inventory.setItem(amountEditor.getSlot(), amountEditor);
        }

        // Enchantments editor
        ItemStack enchantItem = createConfigItem("items.edit_enchantments",
                "#enchant_count#", String.valueOf(recipe.getResultEnchantments().size()));
        if (enchantItem != null) {
            InteractiveItem enchantEditor = new InteractiveItem(enchantItem, 15)
                    .onLeftClick(p -> editEnchantments(p));
            inventory.setItem(enchantEditor.getSlot(), enchantEditor);
        }

        // Flags editor
        ItemStack flagsItem = createConfigItem("items.edit_flags",
                "#flag_count#", String.valueOf(recipe.getResultFlags().size()));
        if (flagsItem != null) {
            InteractiveItem flagsEditor = new InteractiveItem(flagsItem, 16)
                    .onLeftClick(p -> editFlags(p));
            inventory.setItem(flagsEditor.getSlot(), flagsEditor);
        }

        // Custom Model Data editor
        ItemStack cmdItem = createConfigItem("items.edit_custom_model_data",
                "#current_cmd#", String.valueOf(recipe.getResultCustomModelData()));
        if (cmdItem != null) {
            InteractiveItem cmdEditor = new InteractiveItem(cmdItem, 19)
                    .onLeftClick(p -> editCustomModelData(p));
            inventory.setItem(cmdEditor.getSlot(), cmdEditor);
        }

        // Unbreakable toggle
        ItemStack unbreakableItem = createConfigItem("items.toggle_unbreakable",
                "#status#", recipe.isResultUnbreakable() ? "&aEnabled" : "&cDisabled");
        if (unbreakableItem != null) {
            InteractiveItem unbreakableToggle = new InteractiveItem(unbreakableItem, 20)
                    .onLeftClick(p -> toggleUnbreakable(p));
            inventory.setItem(unbreakableToggle.getSlot(), unbreakableToggle);
        }
    }

    private void addRequirementsPanel(Inventory inventory) {
        // Requirements title
        InteractiveItem requirementsTitle = new InteractiveItem(
                ItemManager.getItemConfig(Objects.requireNonNull(config.getConfigurationSection("items.requirements_title"))),
                28
        );
        inventory.setItem(requirementsTitle.getSlot(), requirementsTitle);

        // Add requirement button
        InteractiveItem addRequirement = new InteractiveItem(
                ItemManager.getItemConfig(Objects.requireNonNull(config.getConfigurationSection("items.add_requirement"))),
                37
        ).onLeftClick(p -> addRequirement(p));
        inventory.setItem(addRequirement.getSlot(), addRequirement);

        // Permission requirements button
        ItemStack permReqItem = createConfigItem("items.permission_requirements",
                "#permission_count#", String.valueOf(recipe.getPermissionRequirements().size()));
        if (permReqItem != null) {
            InteractiveItem permReqButton = new InteractiveItem(permReqItem, 46)
                    .onLeftClick(p -> openPermissionRequirementsGUI(p));
            inventory.setItem(permReqButton.getSlot(), permReqButton);
        }

        // Display current requirements
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

                String materialName = requirement.getKey();
                if (materialName.contains(";")) {
                    materialName = materialName.split(";")[0];
                }

                // Use config for requirement item
                ItemStack reqItem = ItemManager.getItemConfigWithPlaceholders(player,
                        config.getConfigurationSection("items.requirement_slot"),
                        "#material#", materialName,
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
            InteractiveItem settingsButton = new InteractiveItem(settingsItem, 48)
                    .onLeftClick(p -> editRecipeSettings(p));
            inventory.setItem(settingsButton.getSlot(), settingsButton);
        }

        // Save button
        InteractiveItem saveButton = new InteractiveItem(
                ItemManager.getItemConfig(Objects.requireNonNull(config.getConfigurationSection("items.save"))),
                50
        ).onLeftClick(p -> saveRecipe(p));
        inventory.setItem(saveButton.getSlot(), saveButton);

        // Back button
        InteractiveItem backButton = new InteractiveItem(
                ItemManager.getItemConfig(Objects.requireNonNull(config.getConfigurationSection("items.back"))),
                49
        ).onLeftClick(p -> backToList(p));
        inventory.setItem(backButton.getSlot(), backButton);

        // Delete button
        InteractiveItem deleteButton = new InteractiveItem(
                ItemManager.getItemConfig(Objects.requireNonNull(config.getConfigurationSection("items.delete"))),
                53
        ).onLeftClick(p -> deleteRecipe(p));
        inventory.setItem(deleteButton.getSlot(), deleteButton);
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
        ItemStack fallback = XMaterial.STONE.parseItem();
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

    // Edit methods
    private void editMaterial(Player player) {
        SoundManager.setShouldPlayCloseSound(player, false);
        player.openInventory(new MaterialSelectionGUI(player, recipe, "result").getInventory(SoundContext.SILENT));
    }

    private void editName(Player player) {
        RecipeEditManager.requestNameEdit(player, recipe);
    }

    private void editLore(Player player, ClickType clickType) {
        if (clickType == ClickType.LEFT || clickType == ClickType.SHIFT_LEFT) {
            // Add new lore line
            RecipeEditManager.requestLoreAdd(player, recipe);
        } else if (clickType == ClickType.RIGHT || clickType == ClickType.SHIFT_RIGHT) {
            // Remove last lore line
            List<String> lore = recipe.getResultLore();
            if (!lore.isEmpty()) {
                RecipeEditManager.removeLoreLine(recipe, lore.size() - 1);
                CraftingManager.updateRecipe(recipe);
                refreshGUI(player);
                player.sendMessage(ChatUtils.colorize(File.getMessage().getString("crafting.lore_removed")));
            } else {
                player.sendMessage(ChatUtils.colorize(File.getMessage().getString("crafting.lore_no_lines")));
            }
        }
    }

    private void editAmount(Player player, ClickType clickType) {
        if (clickType == ClickType.LEFT) {
            recipe.setResultAmount(Math.min(64, recipe.getResultAmount() + 1));
            CraftingManager.updateRecipe(recipe);
            refreshGUI(player);
        } else if (clickType == ClickType.RIGHT) {
            recipe.setResultAmount(Math.max(1, recipe.getResultAmount() - 1));
            CraftingManager.updateRecipe(recipe);
            refreshGUI(player);
        } else if (clickType == ClickType.SHIFT_LEFT || clickType == ClickType.SHIFT_RIGHT) {
            // Custom amount input
            RecipeEditManager.requestAmountEdit(player, recipe);
        }
    }

    private void editEnchantments(Player player) {
        SoundManager.setShouldPlayCloseSound(player, false);
        player.openInventory(new EnchantmentsEditorGUI(player, recipe).getInventory(SoundContext.SILENT));
    }

    private void editFlags(Player player) {
        SoundManager.setShouldPlayCloseSound(player, false);
        player.openInventory(new ItemFlagsEditorGUI(player, recipe).getInventory(SoundContext.SILENT));
    }

    private void editCustomModelData(Player player) {
        RecipeEditManager.requestCustomModelDataEdit(player, recipe);
    }

    private void toggleUnbreakable(Player player) {
        recipe.setResultUnbreakable(!recipe.isResultUnbreakable());
        CraftingManager.updateRecipe(recipe);
        refreshGUI(player);
        String statusKey = recipe.isResultUnbreakable() ? "crafting.status_on" : "crafting.status_off";
        String status = File.getMessage().getString(statusKey);
        player.sendMessage(ChatUtils.colorize(
                File.getMessage().getString("crafting.unbreakable_toggled")
                        .replace("#status#", status)));
    }

    private void addRequirement(Player player) {
        SoundManager.setShouldPlayCloseSound(player, false);
        player.openInventory(new MaterialSelectionGUI(player, recipe, "requirement").getInventory(SoundContext.SILENT));
    }

    private void editRequirement(Player player, String material, ClickType clickType) {
        if (clickType == ClickType.LEFT) {
            // Increase amount
            Map<String, Integer> requirements = recipe.getMaterialRequirements();
            int currentAmount = requirements.getOrDefault(material, 1);
            requirements.put(material, currentAmount + 1);
            recipe.setMaterialRequirements(requirements);
            CraftingManager.updateRecipe(recipe);
            refreshGUI(player);
        } else if (clickType == ClickType.RIGHT) {
            // Decrease amount
            Map<String, Integer> requirements = recipe.getMaterialRequirements();
            int currentAmount = requirements.getOrDefault(material, 1);
            if (currentAmount > 1) {
                requirements.put(material, currentAmount - 1);
                recipe.setMaterialRequirements(requirements);
                CraftingManager.updateRecipe(recipe);
                refreshGUI(player);
            }
        } else if (clickType == ClickType.SHIFT_LEFT) {
            // Custom amount
            RecipeEditManager.requestRequirementAmountEdit(player, recipe, material);
        } else if (clickType == ClickType.SHIFT_RIGHT) {
            // Remove requirement
            RecipeEditManager.removeRequirement(recipe, material);
            CraftingManager.updateRecipe(recipe);
            refreshGUI(player);
            String materialName = material.contains(";") ? material.split(";")[0] : material;
            player.sendMessage(ChatUtils.colorize(File.getMessage().getString("crafting.requirement_removed_gui")
                    .replace("#material#", materialName)));
        }
    }

    private void editRecipeSettings(Player player) {
        RecipeEditManager.requestCategoryEdit(player, recipe);
    }

    private void openPermissionRequirementsGUI(Player player) {
        SoundManager.setShouldPlayCloseSound(player, false);
        player.openInventory(new PermissionRequirementsGUI(player, recipe).getInventory(SoundContext.SILENT));
    }

    private void saveRecipe(Player player) {
        CraftingManager.updateRecipe(recipe);
        player.sendMessage(ChatUtils.colorize(File.getMessage().getString("crafting.editor_saved")
                .replace("#recipe#", recipe.getName())));

        SoundManager.setShouldPlayCloseSound(player, false);
        player.openInventory(new RecipeEditorListGUI(player).getInventory(SoundContext.SILENT));
    }

    private void backToList(Player player) {
        SoundManager.setShouldPlayCloseSound(player, false);
        player.openInventory(new RecipeEditorListGUI(player).getInventory(SoundContext.SILENT));
    }

    private void deleteRecipe(Player player) {
        String message = "Delete recipe: &e" + recipe.getName() + "&7?";

        SoundManager.setShouldPlayCloseSound(player, false);
        player.openInventory(new ConfirmationGUI(player, message,
                () -> {
                    // On confirm
                    CraftingManager.removeRecipe(recipe.getId());
                    player.sendMessage(ChatUtils.colorize(File.getMessage().getString("crafting.editor_deleted")
                            .replace("#recipe#", recipe.getName())));
                    SoundManager.setShouldPlayCloseSound(player, false);
                    player.openInventory(new RecipeEditorListGUI(player).getInventory(SoundContext.SILENT));
                },
                () -> {
                    // On cancel
                    SoundManager.setShouldPlayCloseSound(player, false);
                    player.openInventory(new RecipeEditorGUI(player, recipe).getInventory(SoundContext.SILENT));
                }
        ).getInventory(SoundContext.SILENT));
    }

    private void refreshGUI(Player player) {
        SoundManager.setShouldPlayCloseSound(player, false);
        player.openInventory(new RecipeEditorGUI(player, recipe).getInventory(SoundContext.SILENT));
    }
}
