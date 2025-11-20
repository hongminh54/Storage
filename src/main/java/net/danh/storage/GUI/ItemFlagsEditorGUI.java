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
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public class ItemFlagsEditorGUI implements IGUI {

    private static final List<ItemFlag> ALL_FLAGS = getAvailableFlags();
    private final Player player;
    private final Recipe recipe;
    private final FileConfiguration config;

    public ItemFlagsEditorGUI(Player player, Recipe recipe) {
        this.player = player;
        this.recipe = recipe;
        this.config = File.getItemFlagsEditorGUIConfig();
    }

    private static List<ItemFlag> getAvailableFlags() {
        List<ItemFlag> flags = new ArrayList<>();
        flags.add(ItemFlag.HIDE_ENCHANTS);
        flags.add(ItemFlag.HIDE_ATTRIBUTES);
        flags.add(ItemFlag.HIDE_UNBREAKABLE);
        flags.add(ItemFlag.HIDE_DESTROYS);
        flags.add(ItemFlag.HIDE_PLACED_ON);

        // HIDE_POTION_EFFECTS added in 1.9+
        try {
            flags.add(ItemFlag.valueOf("HIDE_POTION_EFFECTS"));
        } catch (IllegalArgumentException ignored) {
        }

        // HIDE_DYE added in 1.20+
        try {
            flags.add(ItemFlag.valueOf("HIDE_DYE"));
        } catch (IllegalArgumentException ignored) {
        }

        // HIDE_ARMOR_TRIM added in 1.20+
        try {
            flags.add(ItemFlag.valueOf("HIDE_ARMOR_TRIM"));
        } catch (IllegalArgumentException ignored) {
        }

        // HIDE_ADDITIONAL_TOOLTIP added in 1.20.5+
        try {
            flags.add(ItemFlag.valueOf("HIDE_ADDITIONAL_TOOLTIP"));
        } catch (IllegalArgumentException ignored) {
        }

        return flags;
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
        addFlagItems(inventory);
        addBackButton(inventory);
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

    private void addFlagItems(Inventory inventory) {
        Set<ItemFlag> currentFlags = recipe.getResultFlags();

        for (ItemFlag flag : ALL_FLAGS) {
            String flagKey = "flag_" + flag.name().toLowerCase();
            ConfigurationSection flagSection = config.getConfigurationSection("items." + flagKey);

            if (flagSection != null) {
                boolean isEnabled = currentFlags.contains(flag);
                ItemStack flagItem = createFlagItem(flagSection, flag, isEnabled);

                if (flagItem != null) {
                    int slot = flagSection.getInt("slot", 10);
                    InteractiveItem interactiveItem = new InteractiveItem(flagItem, slot)
                            .onLeftClick(p -> toggleFlag(p, flag));
                    inventory.setItem(interactiveItem.getSlot(), interactiveItem);
                }
            }
        }
    }

    private ItemStack createFlagItem(ConfigurationSection section, ItemFlag flag, boolean isEnabled) {
        String statusKey = isEnabled ? "crafting.status_enabled" : "crafting.status_disabled";
        String status = File.getMessage().getString(statusKey);
        return ItemManager.getItemConfigWithPlaceholders(player, section,
                "#status#", status);
    }

    private void addBackButton(Inventory inventory) {
        InteractiveItem backButton = new InteractiveItem(
                ItemManager.getItemConfig(Objects.requireNonNull(config.getConfigurationSection("items.back"))),
                22
        ).onLeftClick(p -> backToEditor(p));
        inventory.setItem(backButton.getSlot(), backButton);
    }

    private void toggleFlag(Player player, ItemFlag flag) {
        RecipeEditManager.toggleItemFlag(recipe, flag);
        CraftingManager.updateRecipe(recipe);

        boolean isEnabled = recipe.getResultFlags().contains(flag);
        String statusKey = isEnabled ? "crafting.status_enabled_lower" : "crafting.status_disabled_lower";
        String status = File.getMessage().getString(statusKey);
        player.sendMessage(ChatUtils.colorize(File.getMessage().getString("crafting.flag_toggled")
                .replace("#flag#", flag.name())
                .replace("#status#", status)));

        SoundManager.playSound(player, SoundManager.SoundType.ACTION_SUCCESS);
        refreshGUI(player);
    }

    private void backToEditor(Player player) {
        SoundManager.setShouldPlayCloseSound(player, false);
        player.openInventory(new RecipeEditorGUI(player, recipe).getInventory(SoundContext.SILENT));
    }

    private void refreshGUI(Player player) {
        SoundManager.setShouldPlayCloseSound(player, false);
        player.openInventory(new ItemFlagsEditorGUI(player, recipe).getInventory(SoundContext.SILENT));
    }
}
