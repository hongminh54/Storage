package net.danh.storage.GUI;

import com.cryptomorin.xseries.XMaterial;
import net.danh.storage.GUI.manager.IGUI;
import net.danh.storage.GUI.manager.InteractiveItem;
import net.danh.storage.Manager.CraftingManager;
import net.danh.storage.Manager.ItemManager;
import net.danh.storage.Manager.SoundManager;
import net.danh.storage.Recipe.Recipe;
import net.danh.storage.Utils.Chat;
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public class RecipeEditorGUI implements IGUI {

    private final Player player;
    private final Recipe recipe;
    private final FileConfiguration config;

    public RecipeEditorGUI(Player player, Recipe recipe) {
        this.player = player;
        this.recipe = recipe;
        this.config = File.getFileSetting().get("GUI/recipe-editor.yml");
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

        String title = Chat.colorizewp(Objects.requireNonNull(config.getString("title"))
                .replace("#recipe_name#", recipe.getName()));
        Inventory inventory = Bukkit.createInventory(this, config.getInt("size") * 9, title);

        setupItems(inventory);
        return inventory;
    }

    private void setupItems(Inventory inventory) {
        // Add decorative items
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

        // Add result preview
        addResultPreview(inventory);
        
        // Add item configuration panel
        addItemConfigurationPanel(inventory);
        
        // Add requirements configuration panel
        addRequirementsPanel(inventory);
        
        // Add control buttons
        addControlButtons(inventory);
    }

    private void addResultPreview(Inventory inventory) {
        ItemStack previewItem = CraftingManager.createResultItem(recipe);
        if (previewItem == null) {
            previewItem = ItemManager.getItemConfig(Objects.requireNonNull(config.getConfigurationSection("items.result_preview")));
        }

        // Enhance the preview item with preview information
        if (previewItem != null) {
            ItemMeta meta = previewItem.getItemMeta();
            if (meta != null) {
                List<String> lore = meta.getLore();
                if (lore == null) lore = new ArrayList<>();

                // Add preview information to lore
                lore.add(Chat.colorizewp("&7"));
                lore.add(Chat.colorizewp("&e&lVẬT PHẨM XEM TRƯỚC"));
                lore.add(Chat.colorizewp("&7Nhấp để xem chi tiết"));
                lore.add(Chat.colorizewp("&c&oĐây chỉ là xem trước"));

                meta.setLore(lore);
                previewItem.setItemMeta(meta);
            }
        }

        InteractiveItem resultPreview = new InteractiveItem(previewItem, 13)
                .onLeftClick(p -> editResultItem(p));
        inventory.setItem(resultPreview.getSlot(), resultPreview);
    }

    private void addItemConfigurationPanel(Inventory inventory) {
        // Material editor
        ItemStack materialItem = createSafeConfigItem("items.edit_material",
                "#current_material#", recipe.getResultMaterial());
        if (materialItem != null) {
            InteractiveItem materialEditor = new InteractiveItem(materialItem, 10)
                    .onLeftClick(p -> editMaterial(p));
            inventory.setItem(materialEditor.getSlot(), materialEditor);
        }

        // Name editor
        ItemStack nameItem = createSafeConfigItem("items.edit_name",
                "#current_name#", recipe.getResultName());
        if (nameItem != null) {
            InteractiveItem nameEditor = new InteractiveItem(nameItem, 11)
                    .onLeftClick(p -> editName(p));
            inventory.setItem(nameEditor.getSlot(), nameEditor);
        }

        // Lore editor
        ItemStack loreItem = createSafeConfigItem("items.edit_lore",
                "#lore_count#", String.valueOf(recipe.getResultLore().size()));
        if (loreItem != null) {
            InteractiveItem loreEditor = new InteractiveItem(loreItem, 12)
                    .onClick((p, clickType) -> editLore(p, clickType));
            inventory.setItem(loreEditor.getSlot(), loreEditor);
        }

        // Amount editor
        ItemStack amountItem = createSafeConfigItem("items.edit_amount",
                "#current_amount#", String.valueOf(recipe.getResultAmount()));
        if (amountItem != null) {
            amountItem.setAmount(Math.max(1, Math.min(64, recipe.getResultAmount())));
            InteractiveItem amountEditor = new InteractiveItem(amountItem, 14)
                    .onClick((p, clickType) -> editAmount(p, clickType));
            inventory.setItem(amountEditor.getSlot(), amountEditor);
        }

        // Enchantments editor
        ItemStack enchantItem = createSafeConfigItem("items.edit_enchantments",
                "#enchant_count#", String.valueOf(recipe.getResultEnchantments().size()));
        if (enchantItem != null) {
            InteractiveItem enchantEditor = new InteractiveItem(enchantItem, 15)
                    .onClick((p, clickType) -> editEnchantments(p, clickType));
            inventory.setItem(enchantEditor.getSlot(), enchantEditor);
        }

        // Flags editor
        ItemStack flagsItem = createSafeConfigItem("items.edit_flags",
                "#flag_count#", String.valueOf(recipe.getResultFlags().size()));
        if (flagsItem != null) {
            InteractiveItem flagsEditor = new InteractiveItem(flagsItem, 16)
                    .onLeftClick(p -> editFlags(p));
            inventory.setItem(flagsEditor.getSlot(), flagsEditor);
        }
    }

    private ItemStack createSafeConfigItem(String configPath, String placeholder, String value) {
        try {
            return ItemManager.getItemConfigWithPlaceholders(player,
                    config.getConfigurationSection(configPath),
                    placeholder, value);
        } catch (Exception e) {
            // Fallback to basic item if config fails
            ItemStack fallback = XMaterial.STONE.parseItem();
            if (fallback != null) {
                ItemMeta meta = fallback.getItemMeta();
                if (meta != null) {
                    meta.setDisplayName(Chat.colorizewp("&cLỗi Cấu Hình"));
                    meta.setLore(Arrays.asList(Chat.colorizewp("&7Không thể tải: " + configPath)));
                    fallback.setItemMeta(meta);
                }
            }
            return fallback;
        }
    }

    private void addRequirementsPanel(Inventory inventory) {
        // Requirements title
        InteractiveItem requirementsTitle = new InteractiveItem(
                ItemManager.getItemConfig(Objects.requireNonNull(config.getConfigurationSection("items.requirements_title"))),
                19
        ).onLeftClick(p -> manageRequirements(p));
        inventory.setItem(requirementsTitle.getSlot(), requirementsTitle);

        // Add requirement button
        InteractiveItem addRequirement = new InteractiveItem(
                ItemManager.getItemConfig(Objects.requireNonNull(config.getConfigurationSection("items.add_requirement"))),
                28
        ).onLeftClick(p -> addRequirement(p));
        inventory.setItem(addRequirement.getSlot(), addRequirement);

        // Permission requirements button
        ItemStack permReqItem = createSafeConfigItem("items.permission_requirements",
                "#permission_count#", String.valueOf(recipe.getPermissionRequirements().size()));
        if (permReqItem != null) {
            InteractiveItem permReqButton = new InteractiveItem(permReqItem, 37)
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

        // Clear existing requirement slots first
        for (String slotStr : slotArray) {
            try {
                int slot = Integer.parseInt(slotStr.trim());
                inventory.setItem(slot, null);
            } catch (NumberFormatException e) {
                // Skip invalid slot numbers
            }
        }

        // Display all requirements up to available slots
        for (int i = 0; i < Math.min(requirements.size(), slotArray.length); i++) {
            Map.Entry<String, Integer> requirement = requirements.get(i);

            try {
                int slot = Integer.parseInt(slotArray[i].trim());

                String materialName = File.getConfig().getString("items." + requirement.getKey(),
                        requirement.getKey().split(";")[0]);

                ItemStack reqItem = createRequirementItem(requirement, materialName);
                if (reqItem != null) {
                    InteractiveItem requirementItem = new InteractiveItem(reqItem, slot)
                            .onClick((p, clickType) -> editRequirement(p, requirement.getKey(), clickType));
                    inventory.setItem(slot, requirementItem);
                }
            } catch (NumberFormatException e) {
                // Skip invalid slot numbers and continue with next requirement
                continue;
            }
        }
    }

    private ItemStack createRequirementItem(Map.Entry<String, Integer> requirement, String materialName) {
        try {
            // Try to get the item from config first
            ItemStack reqItem = ItemManager.getItemConfigWithPlaceholders(player,
                    config.getConfigurationSection("items.requirement_slot"),
                    "#material_name#", materialName,
                    "#material_type#", requirement.getKey().split(";")[0],
                    "#amount#", String.valueOf(requirement.getValue()),
                    "#display_amount#", String.valueOf(Math.min(64, requirement.getValue())));

            if (reqItem != null) {
                // Try to set the actual material type
                String materialType = requirement.getKey().split(";")[0];
                Optional<XMaterial> xMaterial = XMaterial.matchXMaterial(materialType);
                if (xMaterial.isPresent() && xMaterial.get().parseItem() != null) {
                    reqItem.setType(xMaterial.get().parseItem().getType());
                }
                return reqItem;
            }

            // Fallback: create item manually if config fails
            return createFallbackRequirementItem(requirement, materialName);

        } catch (Exception e) {
            // Last resort fallback
            return createFallbackRequirementItem(requirement, materialName);
        }
    }

    private ItemStack createFallbackRequirementItem(Map.Entry<String, Integer> requirement, String materialName) {
        String materialType = requirement.getKey().split(";")[0];
        Optional<XMaterial> xMaterial = XMaterial.matchXMaterial(materialType);

        ItemStack item;
        if (xMaterial.isPresent() && xMaterial.get().parseItem() != null) {
            item = xMaterial.get().parseItem();
        } else {
            // Ultimate fallback to stone
            item = XMaterial.STONE.parseItem();
        }

        if (item != null) {
            item.setAmount(Math.min(64, requirement.getValue()));
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(Chat.colorizewp("&e" + materialName));
                List<String> lore = new ArrayList<>();
                lore.add(Chat.colorizewp("&7Amount needed: &e" + requirement.getValue()));
                lore.add(Chat.colorizewp("&7"));
                lore.add(Chat.colorizewp("&eLeft-click to increase amount"));
                lore.add(Chat.colorizewp("&cRight-click to decrease amount"));
                lore.add(Chat.colorizewp("&6Shift-click to set custom amount"));
                lore.add(Chat.colorizewp("&4Shift-right-click to remove"));
                meta.setLore(lore);
                item.setItemMeta(meta);
            }
        }

        return item;
    }

    private void addControlButtons(Inventory inventory) {
        // Recipe settings
        String status = recipe.isEnabled() ? "&aEnabled" : "&cDisabled";
        ItemStack settingsItem = ItemManager.getItemConfigWithPlaceholders(player,
                Objects.requireNonNull(config.getConfigurationSection("items.recipe_settings")),
                "#recipe_name#", recipe.getName(),
                "#recipe_category#", recipe.getCategory(),
                "#recipe_status#", status);
        InteractiveItem settingsButton = new InteractiveItem(settingsItem, 38)
                .onLeftClick(p -> editRecipeSettings(p));
        inventory.setItem(settingsButton.getSlot(), settingsButton);

        // Cancel button
        InteractiveItem cancelButton = new InteractiveItem(
                ItemManager.getItemConfig(Objects.requireNonNull(config.getConfigurationSection("items.cancel_edit"))),
                39
        ).onLeftClick(p -> cancelEdit(p));
        inventory.setItem(cancelButton.getSlot(), cancelButton);

        // Save button
        InteractiveItem saveButton = new InteractiveItem(
                ItemManager.getItemConfig(Objects.requireNonNull(config.getConfigurationSection("items.save_recipe"))),
                40
        ).onLeftClick(p -> saveRecipe(p));
        inventory.setItem(saveButton.getSlot(), saveButton);

        // Delete button
        InteractiveItem deleteButton = new InteractiveItem(
                ItemManager.getItemConfig(Objects.requireNonNull(config.getConfigurationSection("items.delete_recipe"))),
                41
        ).onLeftClick(p -> deleteRecipe(p));
        inventory.setItem(deleteButton.getSlot(), deleteButton);
    }

    // Event handlers
    private void editResultItem(Player player) {
        // Show detailed preview information instead of giving the item
        ItemStack previewItem = CraftingManager.createResultItem(recipe);

        player.sendMessage(Chat.colorize("&e&l=== Xem Trước Kết Quả ==="));
        player.sendMessage(Chat.colorize("&7Material: &e" + recipe.getResultMaterial()));
        player.sendMessage(Chat.colorize("&7Tên: &e" + recipe.getResultName()));
        player.sendMessage(Chat.colorize("&7Số lượng: &e" + recipe.getResultAmount()));

        if (!recipe.getResultLore().isEmpty()) {
            player.sendMessage(Chat.colorize("&7Mô tả:"));
            for (String loreLine : recipe.getResultLore()) {
                player.sendMessage(Chat.colorize("  &7- &f" + loreLine));
            }
        }

        if (!recipe.getResultEnchantments().isEmpty()) {
            player.sendMessage(Chat.colorize("&7Phù phép:"));
            for (Map.Entry<String, Integer> enchant : recipe.getResultEnchantments().entrySet()) {
                player.sendMessage(Chat.colorize("  &7- &b" + enchant.getKey() + " " + enchant.getValue()));
            }
        }

        if (!recipe.getResultFlags().isEmpty()) {
            player.sendMessage(Chat.colorize("&7Flags: &d" + recipe.getResultFlags().size() + " cờ"));
        }

        player.sendMessage(Chat.colorize("&7"));
        player.sendMessage(Chat.colorize("&eSử dụng các nút bên dưới để chỉnh sửa thuộc tính vật phẩm."));
        player.sendMessage(Chat.colorize("&7&oLưu ý: Đây chỉ là xem trước - không có vật phẩm nào được thêm vào túi đồ của bạn."));
    }

    private void editMaterial(Player player) {
        SoundManager.setShouldPlayCloseSound(player, false);
        player.openInventory(new MaterialSelectionGUI(player, recipe, "result").getInventory(SoundContext.SILENT));
    }

    private void editName(Player player) {
        net.danh.storage.Manager.RecipeEditManager.requestNameEdit(player, recipe);
    }

    private void editLore(Player player, ClickType clickType) {
        if (clickType == ClickType.LEFT || clickType == ClickType.SHIFT_LEFT) {
            // Add new lore line
            net.danh.storage.Manager.RecipeEditManager.requestLoreAdd(player, recipe);
        } else if (clickType == ClickType.RIGHT || clickType == ClickType.SHIFT_RIGHT) {
            // Remove last lore line
            List<String> lore = recipe.getResultLore();
            if (!lore.isEmpty()) {
                net.danh.storage.Manager.RecipeEditManager.removeLoreLine(recipe, lore.size() - 1);
                CraftingManager.updateRecipe(recipe);
                refreshGUI(player);
                player.sendMessage(Chat.colorize("&aĐã xóa dòng lore cuối cùng."));
            } else {
                player.sendMessage(Chat.colorize("&cKhông có dòng lore nào để xóa!"));
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
            net.danh.storage.Manager.RecipeEditManager.requestAmountEdit(player, recipe);
        }
    }

    private void editEnchantments(Player player, ClickType clickType) {
        if (clickType == ClickType.LEFT || clickType == ClickType.SHIFT_LEFT) {
            // Add enchantment - open enchantment selection GUI
            openEnchantmentSelectionGUI(player);
        } else if (clickType == ClickType.RIGHT || clickType == ClickType.SHIFT_RIGHT) {
            // Remove enchantment - open enchantment removal GUI
            openEnchantmentRemovalGUI(player);
        }
    }

    private void editFlags(Player player) {
        // Open flags selection GUI
        openFlagsSelectionGUI(player);
    }

    private void manageRequirements(Player player) {
        player.sendMessage(Chat.colorize("&eUse the buttons below to add or edit material requirements."));
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
            net.danh.storage.Manager.RecipeEditManager.requestRequirementAmountEdit(player, recipe, material);
        } else if (clickType == ClickType.SHIFT_RIGHT) {
            // Remove requirement
            net.danh.storage.Manager.RecipeEditManager.removeRequirement(recipe, material);
            CraftingManager.updateRecipe(recipe);
            refreshGUI(player);
            player.sendMessage(Chat.colorize("&aRemoved requirement: " + material.split(";")[0]));
        }
    }

    private void editRecipeSettings(Player player) {
        openRecipeSettingsGUI(player);
    }

    private void saveRecipe(Player player) {
        CraftingManager.updateRecipe(recipe);
        player.sendMessage(Chat.colorize(File.getMessage().getString("recipe.editor_saved")
                .replace("#recipe#", recipe.getName())));
        
        SoundManager.setShouldPlayCloseSound(player, false);
        player.openInventory(new RecipeEditorListGUI(player).getInventory(SoundContext.SILENT));
    }

    private void cancelEdit(Player player) {
        SoundManager.setShouldPlayCloseSound(player, false);
        player.openInventory(new RecipeEditorListGUI(player).getInventory(SoundContext.SILENT));
    }

    private void deleteRecipe(Player player) {
        CraftingManager.removeRecipe(recipe.getId());
        player.sendMessage(Chat.colorize(File.getMessage().getString("recipe.editor_deleted")
                .replace("#recipe#", recipe.getName())));
        
        SoundManager.setShouldPlayCloseSound(player, false);
        player.openInventory(new RecipeEditorListGUI(player).getInventory(SoundContext.SILENT));
    }

    private void refreshGUI(Player player) {
        SoundManager.setShouldPlayCloseSound(player, false);
        player.openInventory(new RecipeEditorGUI(player, recipe).getInventory(SoundContext.SILENT));
    }

    // Helper GUI methods
    private void openEnchantmentSelectionGUI(Player player) {
        // Create a simple enchantment selection menu
        String[] commonEnchants = {
            "DAMAGE_ALL", "DIG_SPEED", "DURABILITY", "FIRE_ASPECT", "KNOCKBACK",
            "LOOTING", "FORTUNE", "SILK_TOUCH", "PROTECTION_ENVIRONMENTAL", "THORNS"
        };

        player.sendMessage(Chat.colorize("&eCommon enchantments:"));
        for (String enchant : commonEnchants) {
            player.sendMessage(Chat.colorize("&7- " + enchant));
        }

        // For now, use chat input for enchantment selection
        player.sendMessage(Chat.colorize("&eType the enchantment name you want to add:"));
        net.danh.storage.Listeners.Chat.chat_recipe_edit_type.put(player, "enchant_add");
        net.danh.storage.Listeners.Chat.chat_recipe_id.put(player, recipe.getId());
        net.danh.storage.Listeners.Chat.chat_recipe_field.put(player, "DAMAGE_ALL"); // Default
        player.closeInventory();
    }

    private void openEnchantmentRemovalGUI(Player player) {
        Map<String, Integer> enchants = recipe.getResultEnchantments();
        if (enchants.isEmpty()) {
            player.sendMessage(Chat.colorize("&cNo enchantments to remove!"));
            return;
        }

        player.sendMessage(Chat.colorize("&eCurrent enchantments:"));
        for (Map.Entry<String, Integer> entry : enchants.entrySet()) {
            player.sendMessage(Chat.colorize("&7- " + entry.getKey() + " " + entry.getValue()));
        }

        // Remove first enchantment for simplicity
        String firstEnchant = enchants.keySet().iterator().next();
        net.danh.storage.Manager.RecipeEditManager.removeEnchantment(recipe, firstEnchant);
        CraftingManager.updateRecipe(recipe);
        refreshGUI(player);
        player.sendMessage(Chat.colorize("&aRemoved enchantment: " + firstEnchant));
    }

    private void openFlagsSelectionGUI(Player player) {
        // Toggle common item flags
        org.bukkit.inventory.ItemFlag[] commonFlags = {
            org.bukkit.inventory.ItemFlag.HIDE_ENCHANTS,
            org.bukkit.inventory.ItemFlag.HIDE_ATTRIBUTES,
            org.bukkit.inventory.ItemFlag.HIDE_UNBREAKABLE,
            org.bukkit.inventory.ItemFlag.HIDE_DESTROYS,
            org.bukkit.inventory.ItemFlag.HIDE_PLACED_ON
        };

        // Toggle HIDE_ENCHANTS for simplicity
        net.danh.storage.Manager.RecipeEditManager.toggleItemFlag(recipe, org.bukkit.inventory.ItemFlag.HIDE_ENCHANTS);
        CraftingManager.updateRecipe(recipe);
        refreshGUI(player);

        boolean hasFlag = recipe.getResultFlags().contains(org.bukkit.inventory.ItemFlag.HIDE_ENCHANTS);
        player.sendMessage(Chat.colorize("&aToggled HIDE_ENCHANTS: " + (hasFlag ? "ON" : "OFF")));
    }

    private void openRecipeSettingsGUI(Player player) {
        player.sendMessage(Chat.colorize("&eRecipe Settings:"));
        player.sendMessage(Chat.colorize("&7Name: &e" + recipe.getName()));
        player.sendMessage(Chat.colorize("&7Category: &e" + recipe.getCategory()));
        player.sendMessage(Chat.colorize("&7Status: " + (recipe.isEnabled() ? "&aEnabled" : "&cDisabled")));
        player.sendMessage(Chat.colorize("&7"));
        player.sendMessage(Chat.colorize("&eChoose what to edit:"));
        player.sendMessage(Chat.colorize("&71. Recipe Name"));
        player.sendMessage(Chat.colorize("&72. Category"));
        player.sendMessage(Chat.colorize("&73. Toggle Status"));

        // For now, toggle status
        recipe.setEnabled(!recipe.isEnabled());
        CraftingManager.updateRecipe(recipe);
        refreshGUI(player);
        player.sendMessage(Chat.colorize("&aToggled recipe status: " + (recipe.isEnabled() ? "Enabled" : "Disabled")));
    }

    private void openPermissionRequirementsGUI(Player player) {
        SoundManager.setShouldPlayCloseSound(player, false);
        player.openInventory(new PermissionRequirementsGUI(player, recipe).getInventory(SoundContext.SILENT));
    }
}
