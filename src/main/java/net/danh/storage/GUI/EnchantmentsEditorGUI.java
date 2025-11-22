package net.danh.storage.GUI;

import com.cryptomorin.xseries.XEnchantment;
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
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class EnchantmentsEditorGUI implements IGUI {

    private final Player player;
    private final Recipe recipe;
    private final FileConfiguration config;

    public EnchantmentsEditorGUI(Player player, Recipe recipe) {
        this.player = player;
        this.recipe = recipe;
        this.config = File.getEnchantmentsEditorGUIConfig();
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
        displayEnchantments(inventory);
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

    private void displayEnchantments(Inventory inventory) {
        String enchantmentSlots = config.getString("items.enchantment_slot.slot");
        if (enchantmentSlots == null) return;

        String[] slotArray = enchantmentSlots.split(",");
        List<Map.Entry<String, Integer>> enchantments = new ArrayList<>(recipe.getResultEnchantments().entrySet());

        for (int i = 0; i < Math.min(enchantments.size(), slotArray.length); i++) {
            Map.Entry<String, Integer> entry = enchantments.get(i);

            int slot = Number.getInteger(slotArray[i].trim());
            if (slot >= 0) {
                ItemStack enchantItem = createEnchantmentItem(entry.getKey(), entry.getValue());

                if (enchantItem != null) {
                    InteractiveItem interactiveItem = new InteractiveItem(enchantItem, slot)
                            .onClick((p, clickType) -> editEnchantment(p, entry.getKey(), clickType));
                    inventory.setItem(interactiveItem.getSlot(), interactiveItem);
                }
            }
        }
    }

    private ItemStack createEnchantmentItem(String enchantName, int level) {
        String displayName = getEnchantmentDisplayName(enchantName);
        return ItemManager.getItemConfigWithPlaceholders(player,
                config.getConfigurationSection("items.enchantment_slot"),
                "#enchant#", displayName,
                "#level#", String.valueOf(level));
    }

    private String getEnchantmentDisplayName(String enchantName) {
        Optional<XEnchantment> xEnchant = XEnchantment.matchXEnchantment(enchantName);
        if (xEnchant.isPresent()) {
            Enchantment enchant = xEnchant.get().getEnchant();
            if (enchant != null) {
                return xEnchant.get().name().replace("_", " ");
            }
        }
        return enchantName.replace("_", " ");
    }

    private void addControlButtons(Inventory inventory) {
        // Add enchantment button
        InteractiveItem addButton = new InteractiveItem(
                ItemManager.getItemConfig(Objects.requireNonNull(config.getConfigurationSection("items.add_enchantment"))),
                48
        ).onLeftClick(p -> addEnchantment(p));
        inventory.setItem(addButton.getSlot(), addButton);

        // Clear all button
        InteractiveItem clearButton = new InteractiveItem(
                ItemManager.getItemConfig(Objects.requireNonNull(config.getConfigurationSection("items.clear_all"))),
                49
        ).onLeftClick(p -> clearAllEnchantments(p));
        inventory.setItem(clearButton.getSlot(), clearButton);

        // Back button
        InteractiveItem backButton = new InteractiveItem(
                ItemManager.getItemConfig(Objects.requireNonNull(config.getConfigurationSection("items.back"))),
                50
        ).onLeftClick(p -> backToEditor(p));
        inventory.setItem(backButton.getSlot(), backButton);
    }

    private void editEnchantment(Player player, String enchantName, ClickType clickType) {
        Map<String, Integer> enchantments = recipe.getResultEnchantments();
        int currentLevel = enchantments.getOrDefault(enchantName, 1);

        if (clickType == ClickType.LEFT) {
            // Increase level
            int maxLevel = getMaxLevel(enchantName);
            if (currentLevel < maxLevel) {
                enchantments.put(enchantName, currentLevel + 1);
                recipe.setResultEnchantments(enchantments);
                CraftingManager.updateRecipe(recipe);
                SoundManager.playSound(player, SoundManager.SoundType.ACTION_SUCCESS);
                refreshGUI(player);
            } else {
                SoundManager.playSound(player, SoundManager.SoundType.ACTION_ERROR);
            }
        } else if (clickType == ClickType.RIGHT) {
            // Decrease level
            if (currentLevel > 1) {
                enchantments.put(enchantName, currentLevel - 1);
                recipe.setResultEnchantments(enchantments);
                CraftingManager.updateRecipe(recipe);
                SoundManager.playSound(player, SoundManager.SoundType.ACTION_SUCCESS);
                refreshGUI(player);
            }
        } else if (clickType == ClickType.SHIFT_LEFT) {
            // Custom level
            RecipeEditManager.requestEnchantmentLevelEdit(player, recipe, enchantName);
        } else if (clickType == ClickType.SHIFT_RIGHT) {
            // Remove enchantment
            enchantments.remove(enchantName);
            recipe.setResultEnchantments(enchantments);
            CraftingManager.updateRecipe(recipe);
            SoundManager.playSound(player, SoundManager.SoundType.ACTION_SUCCESS);
            refreshGUI(player);
        }
    }

    private int getMaxLevel(String enchantName) {
        Optional<XEnchantment> xEnchant = XEnchantment.matchXEnchantment(enchantName);
        if (xEnchant.isPresent()) {
            Enchantment enchant = xEnchant.get().getEnchant();
            if (enchant != null) {
                return enchant.getMaxLevel();
            }
        }
        return 5; // Default max level
    }

    private void addEnchantment(Player player) {
        SoundManager.setShouldPlayCloseSound(player, false);
        player.openInventory(new EnchantmentSelectionGUI(player, recipe).getInventory(SoundContext.SILENT));
    }

    private void clearAllEnchantments(Player player) {
        String message = File.getMessage().getString("crafting.enchant_clear_confirm");

        SoundManager.setShouldPlayCloseSound(player, false);
        player.openInventory(new ConfirmationGUI(player, message,
                () -> {
                    // On confirm
                    recipe.getResultEnchantments().clear();
                    CraftingManager.updateRecipe(recipe);
                    player.sendMessage(ChatUtils.colorize(
                            File.getMessage().getString("crafting.enchant_cleared")));
                    SoundManager.setShouldPlayCloseSound(player, false);
                    player.openInventory(new EnchantmentsEditorGUI(player, recipe).getInventory(SoundContext.SILENT));
                },
                () -> {
                    // On cancel
                    SoundManager.setShouldPlayCloseSound(player, false);
                    player.openInventory(new EnchantmentsEditorGUI(player, recipe).getInventory(SoundContext.SILENT));
                }
        ).getInventory(SoundContext.SILENT));
    }

    private void backToEditor(Player player) {
        SoundManager.setShouldPlayCloseSound(player, false);
        player.openInventory(new RecipeEditorGUI(player, recipe).getInventory(SoundContext.SILENT));
    }

    private void refreshGUI(Player player) {
        SoundManager.setShouldPlayCloseSound(player, false);
        player.openInventory(new EnchantmentsEditorGUI(player, recipe).getInventory(SoundContext.SILENT));
    }
}
