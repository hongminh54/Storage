package net.danh.storage.GUI.Mythic;

import net.danh.storage.GUI.Crafting.RecipeEditorGUI;
import net.danh.storage.GUI.MaterialEditorGUI;
import net.danh.storage.GUI.manager.IGUI;
import net.danh.storage.GUI.manager.InteractiveItem;
import net.danh.storage.Manager.Crafting.CraftingManager;
import net.danh.storage.Manager.ItemManager;
import net.danh.storage.Manager.MineManager;
import net.danh.storage.Manager.Mythic.MythicStorageManager;
import net.danh.storage.Manager.SoundManager;
import net.danh.storage.MythicMobs.MythicMobsHelper;
import net.danh.storage.Recipe.Recipe;
import net.danh.storage.Utils.ChatUtils;
import net.danh.storage.Utils.File;
import net.danh.storage.Utils.Number;
import net.danh.storage.Utils.SoundContext;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class MythicMaterialSelectionGUI implements IGUI {

    private final Player player;
    private final Recipe recipe;
    private final int currentPage;
    private final FileConfiguration config;

    public MythicMaterialSelectionGUI(Player player, Recipe recipe) {
        this(player, recipe, 0);
    }

    public MythicMaterialSelectionGUI(Player player, Recipe recipe, int currentPage) {
        this.player = player;
        this.recipe = recipe;
        this.currentPage = Math.max(0, currentPage);
        this.config = File.getMythicMaterialSelectionGUIConfig();
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
                        config.getString("title"))
                .replace("#player#", player.getName()));

        Inventory inventory = Bukkit.createInventory(this,
                config.getInt("size") * 9,
                title);

        setupItems(inventory);
        return inventory;
    }

    private void setupItems(Inventory inventory) {
        setupBorderItems(inventory);

        if (!MythicStorageManager.isSystemEnabled()) {
            addBackButton(inventory);
            return;
        }

        setupMythicItems(inventory);
        addBackButton(inventory);
    }

    private void setupBorderItems(Inventory inventory) {
        String borderSlots = config.getString("items.border.slot");
        if (borderSlots == null) return;

        for (String slotStr : borderSlots.split(",")) {
            int slot = Number.getInteger(slotStr.trim());
            InteractiveItem borderItem = new InteractiveItem(
                    ItemManager.getItemConfig(Objects.requireNonNull(
                            config.getConfigurationSection("items.border"))),
                    slot
            );
            inventory.setItem(borderItem.getSlot(), borderItem);
        }
    }

    private void setupMythicItems(Inventory inventory) {
        List<String> items = MythicStorageManager.getConfiguredDrops();
        if (items.isEmpty()) {
            return;
        }

        String slotConfig = config.getString("items.mythic_item.slot");
        if (slotConfig == null) return;

        String[] slotArray = slotConfig.split(",");
        int itemsPerPage = slotArray.length;
        int startIndex = currentPage * itemsPerPage;
        int endIndex = Math.min(startIndex + itemsPerPage, items.size());

        MythicMobsHelper helper = MythicStorageManager.getMythicMobsHelper();
        if (helper == null || !helper.isInitialized()) {
            return;
        }

        for (int i = startIndex; i < endIndex; i++) {
            String itemId = items.get(i);
            int slotIndex = i - startIndex;
            if (slotIndex >= slotArray.length) {
                continue;
            }

            int slot = Number.getInteger(slotArray[slotIndex].trim());
            ItemStack display = createMythicItem(itemId, helper);
            if (display == null) {
                continue;
            }

            InteractiveItem interactiveItem = new InteractiveItem(display, slot)
                    .onLeftClick(p -> selectMythicItem(p, itemId));
            inventory.setItem(interactiveItem.getSlot(), interactiveItem);
        }

        addNavigationButtons(inventory, items.size(), itemsPerPage);
    }

    private ItemStack createMythicItem(String itemId, MythicMobsHelper helper) {
        ItemStack mythicItem = helper.getMythicItem(itemId);
        if (mythicItem == null) {
            return new ItemStack(Material.STONE);
        }

        ItemStack display = mythicItem.clone();
        ItemMeta meta = display.getItemMeta();
        if (meta != null) {
            if (meta.hasDisplayName()) {
                meta.setDisplayName(ChatUtils.colorizewp(meta.getDisplayName()));
            }

            List<String> lore = new ArrayList<>();
            for (String line : config.getStringList("items.mythic_item.lore")) {
                lore.add(ChatUtils.colorizewp(line.replace("#item_id#", itemId)));
            }
            meta.setLore(lore);
            display.setItemMeta(meta);
        }

        return display;
    }

    private void selectMythicItem(Player player, String itemId) {
        String materialKey = "mythic:" + itemId;
        String normalized = MineManager.normalizeMaterial(materialKey);

        recipe.getMaterialRequirements().put(normalized, 1);
        player.sendMessage(ChatUtils.colorize(
                File.getMessage().getString("crafting.requirement_added_gui")
                        .replace("#material#", itemId)
                        .replace("#amount#", "1")));

        CraftingManager.updateRecipe(recipe);
        RecipeEditorGUI.updateBackup(player.getUniqueId(), recipe);

        SoundManager.setShouldPlayCloseSound(player, false);
        player.openInventory(new MaterialEditorGUI(player, recipe)
                .getInventory(SoundContext.SILENT));
    }

    private void addNavigationButtons(Inventory inventory, int totalItems,
                                      int itemsPerPage) {
        int totalPages = (int) Math.ceil((double) totalItems / itemsPerPage);
        if (currentPage > 0) {
            addPreviousPageButton(inventory, totalPages);
        }
        if (currentPage < totalPages - 1) {
            addNextPageButton(inventory, totalPages);
        }
    }

    private void addPreviousPageButton(Inventory inventory, int totalPages) {
        ItemStack prevItem = ItemManager.getItemConfigWithPlaceholders(player,
                config.getConfigurationSection("items.previous_page"),
                "#current_page#", String.valueOf(currentPage + 1),
                "#total_pages#", String.valueOf(totalPages));

        if (prevItem == null) return;

        String slotConfig = config.getString("items.previous_page.slot", "45");
        if (slotConfig.contains(",")) {
            for (String slotStr : slotConfig.split(",")) {
                int slot = Number.getInteger(slotStr.trim());
                InteractiveItem prevButton = new InteractiveItem(prevItem.clone(), slot)
                        .onLeftClick(p -> {
                            SoundManager.setShouldPlayCloseSound(p, false);
                            p.openInventory(new MythicMaterialSelectionGUI(p, recipe,
                                    currentPage - 1)
                                    .getInventory(SoundContext.SILENT));
                        });
                inventory.setItem(prevButton.getSlot(), prevButton);
            }
        } else {
            int slot = Number.getInteger(slotConfig);
            InteractiveItem prevButton = new InteractiveItem(prevItem, slot)
                    .onLeftClick(p -> {
                        SoundManager.setShouldPlayCloseSound(p, false);
                        p.openInventory(new MythicMaterialSelectionGUI(p, recipe,
                                currentPage - 1)
                                .getInventory(SoundContext.SILENT));
                    });
            inventory.setItem(prevButton.getSlot(), prevButton);
        }
    }

    private void addNextPageButton(Inventory inventory, int totalPages) {
        ItemStack nextItem = ItemManager.getItemConfigWithPlaceholders(player,
                config.getConfigurationSection("items.next_page"),
                "#current_page#", String.valueOf(currentPage + 1),
                "#total_pages#", String.valueOf(totalPages));

        if (nextItem == null) return;

        String slotConfig = config.getString("items.next_page.slot", "53");
        if (slotConfig.contains(",")) {
            for (String slotStr : slotConfig.split(",")) {
                int slot = Number.getInteger(slotStr.trim());
                InteractiveItem nextButton = new InteractiveItem(nextItem.clone(), slot)
                        .onLeftClick(p -> {
                            SoundManager.setShouldPlayCloseSound(p, false);
                            p.openInventory(new MythicMaterialSelectionGUI(p, recipe,
                                    currentPage + 1)
                                    .getInventory(SoundContext.SILENT));
                        });
                inventory.setItem(nextButton.getSlot(), nextButton);
            }
        } else {
            int slot = Number.getInteger(slotConfig);
            InteractiveItem nextButton = new InteractiveItem(nextItem, slot)
                    .onLeftClick(p -> {
                        SoundManager.setShouldPlayCloseSound(p, false);
                        p.openInventory(new MythicMaterialSelectionGUI(p, recipe,
                                currentPage + 1)
                                .getInventory(SoundContext.SILENT));
                    });
            inventory.setItem(nextButton.getSlot(), nextButton);
        }
    }

    private void addBackButton(Inventory inventory) {
        ItemStack backItem = ItemManager.getItemConfig(
                config.getConfigurationSection("items.back"));
        if (backItem == null) return;

        String slotConfig = config.getString("items.back.slot", "49");
        if (slotConfig.contains(",")) {
            for (String slotStr : slotConfig.split(",")) {
                int slot = Number.getInteger(slotStr.trim());
                InteractiveItem backButton = new InteractiveItem(backItem.clone(), slot)
                        .onLeftClick(p -> {
                            SoundManager.setShouldPlayCloseSound(p, false);
                            p.openInventory(new MaterialEditorGUI(p, recipe)
                                    .getInventory(SoundContext.SILENT));
                        });
                inventory.setItem(backButton.getSlot(), backButton);
            }
        } else {
            int slot = Number.getInteger(slotConfig);
            InteractiveItem backButton = new InteractiveItem(backItem, slot)
                    .onLeftClick(p -> {
                        SoundManager.setShouldPlayCloseSound(p, false);
                        p.openInventory(new MaterialEditorGUI(p, recipe)
                                .getInventory(SoundContext.SILENT));
                    });
            inventory.setItem(backButton.getSlot(), backButton);
        }
    }
}
