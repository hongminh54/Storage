package net.danh.storage.GUI;

import com.cryptomorin.xseries.XEnchantment;
import com.cryptomorin.xseries.XMaterial;
import net.danh.storage.GUI.manager.IGUI;
import net.danh.storage.GUI.manager.InteractiveItem;
import net.danh.storage.Manager.CraftingManager;
import net.danh.storage.Manager.ItemManager;
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
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.stream.Collectors;

public class EnchantmentSelectionGUI implements IGUI {

    private static final int ITEMS_PER_PAGE = 28;
    private final Player player;
    private final Recipe recipe;
    private final FileConfiguration config;
    private final int currentPage;

    public EnchantmentSelectionGUI(Player player, Recipe recipe) {
        this(player, recipe, 0);
    }

    public EnchantmentSelectionGUI(Player player, Recipe recipe, int page) {
        this.player = player;
        this.recipe = recipe;
        this.config = File.getEnchantmentSelectionGUIConfig();
        this.currentPage = page;
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

        String title = ChatUtils.colorizewp(Objects.requireNonNull(config.getString("title")));
        Inventory inventory = Bukkit.createInventory(this, config.getInt("size") * 9, title);

        setupItems(inventory);
        return inventory;
    }

    private void setupItems(Inventory inventory) {
        setupDecorativeItems(inventory);
        displayEnchantments(inventory);
        addNavigationButtons(inventory);
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
        List<XEnchantment> allEnchants = Arrays.stream(XEnchantment.VALUES)
                .filter(xe -> xe.getEnchant() != null)
                .filter(xe -> !recipe.getResultEnchantments().containsKey(xe.name()))
                .sorted(Comparator.comparing(XEnchantment::name))
                .collect(Collectors.toList());

        int totalPages = (int) Math.ceil((double) allEnchants.size() / ITEMS_PER_PAGE);
        int startIndex = currentPage * ITEMS_PER_PAGE;
        int endIndex = Math.min(startIndex + ITEMS_PER_PAGE, allEnchants.size());

        // Display slots: 10-16, 19-25, 28-34, 37-43 (28 slots)
        int[] displaySlots = {
                10, 11, 12, 13, 14, 15, 16,
                19, 20, 21, 22, 23, 24, 25,
                28, 29, 30, 31, 32, 33, 34,
                37, 38, 39, 40, 41, 42, 43
        };

        for (int i = startIndex; i < endIndex; i++) {
            XEnchantment xEnchant = allEnchants.get(i);
            int slotIndex = i - startIndex;

            if (slotIndex < displaySlots.length) {
                ItemStack enchantItem = createEnchantmentItem(xEnchant);
                if (enchantItem != null) {
                    InteractiveItem interactiveItem = new InteractiveItem(enchantItem, displaySlots[slotIndex])
                            .onLeftClick(p -> selectEnchantment(p, xEnchant));
                    inventory.setItem(interactiveItem.getSlot(), interactiveItem);
                }
            }
        }
    }

    private ItemStack createEnchantmentItem(XEnchantment xEnchant) {
        try {
            ItemStack item = XMaterial.ENCHANTED_BOOK.parseItem();

            String displayName = xEnchant.name().replace("_", " ");
            Enchantment enchant = xEnchant.getEnchant();
            String maxLevel = enchant != null ? String.valueOf(enchant.getMaxLevel()) : "?";
            
            item = ItemManager.getItemConfigWithPlaceholders(player,
                    config.getConfigurationSection("items.enchantment_item"),
                    "#enchant#", displayName,
                    "#max_level#", maxLevel);

            return item;
        } catch (Exception e) {
            return null;
        }
    }

    private void addNavigationButtons(Inventory inventory) {
        List<XEnchantment> allEnchants = Arrays.stream(XEnchantment.VALUES)
                .filter(xe -> xe.getEnchant() != null)
                .filter(xe -> !recipe.getResultEnchantments().containsKey(xe.name()))
                .collect(Collectors.toList());

        int totalPages = (int) Math.ceil((double) allEnchants.size() / ITEMS_PER_PAGE);

        // Previous page button
        if (currentPage > 0) {
            ItemStack prevItem = ItemManager.getItemConfigWithPlaceholders(player,
                    config.getConfigurationSection("items.previous_page"),
                    "#current_page#", String.valueOf(currentPage + 1),
                    "#total_pages#", String.valueOf(totalPages));

            if (prevItem != null) {
                InteractiveItem prevButton = new InteractiveItem(prevItem, 48)
                        .onLeftClick(p -> previousPage(p));
                inventory.setItem(prevButton.getSlot(), prevButton);
            }
        }

        // Next page button
        if (currentPage < totalPages - 1) {
            ItemStack nextItem = ItemManager.getItemConfigWithPlaceholders(player,
                    config.getConfigurationSection("items.next_page"),
                    "#current_page#", String.valueOf(currentPage + 1),
                    "#total_pages#", String.valueOf(totalPages));

            if (nextItem != null) {
                InteractiveItem nextButton = new InteractiveItem(nextItem, 50)
                        .onLeftClick(p -> nextPage(p));
                inventory.setItem(nextButton.getSlot(), nextButton);
            }
        }

        // Back button
        InteractiveItem backButton = new InteractiveItem(
                ItemManager.getItemConfig(Objects.requireNonNull(config.getConfigurationSection("items.back"))),
                49
        ).onLeftClick(p -> backToEditor(p));
        inventory.setItem(backButton.getSlot(), backButton);
    }

    private void selectEnchantment(Player player, XEnchantment xEnchant) {
        Map<String, Integer> enchantments = recipe.getResultEnchantments();

        // Add enchantment with level 1
        enchantments.put(xEnchant.name(), 1);
        recipe.setResultEnchantments(enchantments);
        CraftingManager.updateRecipe(recipe);

        String displayName = xEnchant.name().replace("_", " ");
        player.sendMessage(ChatUtils.colorize(
                File.getMessage().getString("crafting.enchant_added")
                        .replace("#enchant#", displayName)
                        .replace("#level#", "I")));

        SoundManager.playSound(player, SoundManager.SoundType.ACTION_SUCCESS);
        SoundManager.setShouldPlayCloseSound(player, false);
        player.openInventory(new EnchantmentsEditorGUI(player, recipe).getInventory(SoundContext.SILENT));
    }

    private void previousPage(Player player) {
        SoundManager.setShouldPlayCloseSound(player, false);
        player.openInventory(new EnchantmentSelectionGUI(player, recipe, currentPage - 1).getInventory(SoundContext.SILENT));
    }

    private void nextPage(Player player) {
        SoundManager.setShouldPlayCloseSound(player, false);
        player.openInventory(new EnchantmentSelectionGUI(player, recipe, currentPage + 1).getInventory(SoundContext.SILENT));
    }

    private void backToEditor(Player player) {
        SoundManager.setShouldPlayCloseSound(player, false);
        player.openInventory(new EnchantmentsEditorGUI(player, recipe).getInventory(SoundContext.SILENT));
    }
}
