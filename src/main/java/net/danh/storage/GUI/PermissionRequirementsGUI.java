package net.danh.storage.GUI;

import net.danh.storage.GUI.manager.IGUI;
import net.danh.storage.GUI.manager.InteractiveItem;
import net.danh.storage.Manager.CraftingManager;
import net.danh.storage.Manager.ItemManager;
import net.danh.storage.Manager.RecipeEditManager;
import net.danh.storage.Manager.SoundManager;
import net.danh.storage.Recipe.Recipe;
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
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class PermissionRequirementsGUI implements IGUI {

    private final Player player;
    private final Recipe recipe;
    private final FileConfiguration config;

    public PermissionRequirementsGUI(Player player, Recipe recipe) {
        this.player = player;
        this.recipe = recipe;
        this.config = File.getPermissionRequirementsGUIConfig();
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
        setupBorderItems(inventory);
        displayCurrentPermissions(inventory);
        addPermissionSuggestions(inventory);
        addControlButtons(inventory);
    }

    private void setupBorderItems(Inventory inventory) {
        String borderSlots = config.getString("items.border.slot");
        if (borderSlots != null) {
            for (String slotStr : borderSlots.split(",")) {
                int slot = Number.getInteger(slotStr.trim());
                InteractiveItem borderItem = new InteractiveItem(
                        ItemManager.getItemConfig(Objects.requireNonNull(config.getConfigurationSection("items.border"))),
                        slot
                );
                inventory.setItem(borderItem.getSlot(), borderItem);
            }
        }
    }

    private void displayCurrentPermissions(Inventory inventory) {
        List<String> currentPerms = recipe.getPermissionRequirements();
        String permSlots = config.getString("items.current_permission.slot");

        if (permSlots != null) {
            String[] slotArray = permSlots.split(",");

            for (int i = 0; i < Math.min(currentPerms.size(), slotArray.length); i++) {
                String permission = currentPerms.get(i);
                int slot = Number.getInteger(slotArray[i].trim());

                ItemStack permItem = createPermissionItem(permission, true);
                InteractiveItem interactiveItem = new InteractiveItem(permItem, slot)
                        .onClick((p, clickType) -> handlePermissionClick(p, permission, clickType));
                inventory.setItem(interactiveItem.getSlot(), interactiveItem);
            }
        }

        // Show "No permissions" if empty
        if (currentPerms.isEmpty()) {
            ItemStack noPermsItem = ItemManager.getItemConfig(config.getConfigurationSection("items.no_permissions"));
            if (noPermsItem != null) {
                inventory.setItem(config.getInt("items.no_permissions.slot", 13), noPermsItem);
            }
        }
    }

    private void addPermissionSuggestions(Inventory inventory) {
        List<String> commonPermissions = config.getStringList("common_permissions");
        String suggestionSlots = config.getString("items.permission_suggestion.slot");

        if (suggestionSlots != null && !commonPermissions.isEmpty()) {
            String[] slotArray = suggestionSlots.split(",");

            int index = 0;
            for (String permission : commonPermissions) {
                if (index >= slotArray.length) break;

                boolean alreadyAdded = recipe.getPermissionRequirements().contains(permission);
                if (!alreadyAdded) {
                    int slot = Number.getInteger(slotArray[index].trim());
                    ItemStack suggestionItem = createPermissionItem(permission, false);

                    InteractiveItem interactiveItem = new InteractiveItem(suggestionItem, slot)
                            .onLeftClick(p -> addPermission(p, permission));
                    inventory.setItem(interactiveItem.getSlot(), interactiveItem);
                    index++;
                }
            }
        }
    }

    private ItemStack createPermissionItem(String permission, boolean isRequired) {
        String configPath = isRequired ? "items.current_permission" : "items.permission_suggestion";
        ItemStack item = ItemManager.getItemConfigWithPlaceholders(player,
                config.getConfigurationSection(configPath),
                "#permission#", permission);

        return item;
    }

    private void handlePermissionClick(Player player, String permission, ClickType clickType) {
        if (clickType == ClickType.RIGHT || clickType == ClickType.SHIFT_RIGHT) {
            removePermission(player, permission);
        }
    }

    private void addPermission(Player player, String permission) {
        List<String> permissions = new ArrayList<>(recipe.getPermissionRequirements());
        if (!permissions.contains(permission)) {
            permissions.add(permission);
            recipe.setPermissionRequirements(permissions);
            CraftingManager.updateRecipe(recipe);
            player.sendMessage(ChatUtils.colorize(File.getMessage().getString("crafting.permission_added")
                    .replace("#permission#", permission)));
            refreshGUI(player);
        }
    }

    private void removePermission(Player player, String permission) {
        List<String> permissions = new ArrayList<>(recipe.getPermissionRequirements());
        if (permissions.remove(permission)) {
            recipe.setPermissionRequirements(permissions);
            CraftingManager.updateRecipe(recipe);
            player.sendMessage(ChatUtils.colorize(File.getMessage().getString("crafting.permission_removed")
                    .replace("#permission#", permission)));
            refreshGUI(player);
        }
    }

    private void addControlButtons(Inventory inventory) {
        // Add custom permission button
        ItemStack addCustomItem = ItemManager.getItemConfig(config.getConfigurationSection("items.add_custom"));
        if (addCustomItem != null) {
            InteractiveItem addCustomButton = new InteractiveItem(addCustomItem, config.getInt("items.add_custom.slot", 37))
                    .onLeftClick(this::requestCustomPermission);
            inventory.setItem(addCustomButton.getSlot(), addCustomButton);
        }

        // Clear all permissions button
        ItemStack clearAllItem = ItemManager.getItemConfig(config.getConfigurationSection("items.clear_all"));
        if (clearAllItem != null) {
            InteractiveItem clearAllButton = new InteractiveItem(clearAllItem, config.getInt("items.clear_all.slot", 43))
                    .onLeftClick(this::clearAllPermissions);
            inventory.setItem(clearAllButton.getSlot(), clearAllButton);
        }

        // Back button
        ItemStack backItem = ItemManager.getItemConfig(config.getConfigurationSection("items.back"));
        if (backItem != null) {
            InteractiveItem backButton = new InteractiveItem(backItem, config.getInt("items.back.slot", 40))
                    .onLeftClick(this::returnToRecipeEditor);
            inventory.setItem(backButton.getSlot(), backButton);
        }
    }

    private void requestCustomPermission(Player player) {
        RecipeEditManager.requestPermissionAdd(player, recipe);
    }

    private void clearAllPermissions(Player player) {
        recipe.setPermissionRequirements(new ArrayList<>());
        CraftingManager.updateRecipe(recipe);
        player.sendMessage(ChatUtils.colorize(File.getMessage().getString("crafting.permission_cleared")));
        refreshGUI(player);
    }

    private void returnToRecipeEditor(Player player) {
        SoundManager.setShouldPlayCloseSound(player, false);
        player.openInventory(new RecipeEditorGUI(player, recipe).getInventory(SoundContext.SILENT));
    }

    private void refreshGUI(Player player) {
        SoundManager.setShouldPlayCloseSound(player, false);
        player.openInventory(new PermissionRequirementsGUI(player, recipe).getInventory(SoundContext.SILENT));
    }
}
