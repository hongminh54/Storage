package net.danh.storage.GUI;

import com.cryptomorin.xseries.XMaterial;
import net.danh.storage.GUI.manager.IGUI;
import net.danh.storage.GUI.manager.InteractiveItem;
import net.danh.storage.Manager.*;
import net.danh.storage.Recipe.Recipe;
import net.danh.storage.Utils.ChatUtils;
import net.danh.storage.Utils.File;
import net.danh.storage.Utils.Number;
import net.danh.storage.Utils.SoundContext;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class MaterialSelectionGUI implements IGUI {

    private final Player player;
    private final Recipe recipe;
    private final String selectionType;
    private final int currentPage;
    private final int currentCategory;
    private final FileConfiguration config;

    public MaterialSelectionGUI(Player player, Recipe recipe, String selectionType) {
        this(player, recipe, selectionType, 0, 0);
    }

    public MaterialSelectionGUI(Player player, Recipe recipe, String selectionType, int currentPage) {
        this(player, recipe, selectionType, currentPage, 0);
    }

    public MaterialSelectionGUI(Player player, Recipe recipe, String selectionType, int currentPage, int currentCategory) {
        this.player = player;
        this.recipe = recipe;
        this.selectionType = selectionType;
        this.currentPage = currentPage;
        this.currentCategory = currentCategory;
        this.config = File.getMaterialSelectionGUIConfig();
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
                .replace("#category_name#", "Available Materials")
                .replace("#player#", player.getName()));

        Inventory inventory = Bukkit.createInventory(this, config.getInt("size") * 9, title);
        setupItems(inventory);
        return inventory;
    }

    private void setupItems(Inventory inventory) {
        setupBorderItems(inventory);
        setupMaterialItems(inventory);
        addBackButton(inventory);
        addSearchButton(inventory);
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

    private void setupMaterialItems(Inventory inventory) {
        List<String> configMaterials = getPlayerStorageMaterials();

        if (configMaterials.isEmpty()) {
            return;
        }

        String materialSlots = config.getString("items.material_item.slot");
        if (materialSlots == null) return;

        String[] slotArray = materialSlots.split(",");
        int itemsPerPage = slotArray.length;
        int startIndex = currentPage * itemsPerPage;
        int endIndex = Math.min(startIndex + itemsPerPage, configMaterials.size());

        for (int i = startIndex; i < endIndex; i++) {
            String materialName = configMaterials.get(i);
            int slotIndex = i - startIndex;
            if (slotIndex < slotArray.length) {
                int slot = Number.getInteger(slotArray[slotIndex].trim());
                ItemStack materialItem = createMaterialItem(materialName);
                if (materialItem != null) {
                    InteractiveItem interactiveItem = new InteractiveItem(materialItem, slot)
                            .onLeftClick(p -> selectMaterial(p, materialName));
                    inventory.setItem(interactiveItem.getSlot(), interactiveItem);
                }
            }
        }

        addNavigationButtons(inventory, configMaterials.size(), itemsPerPage);
    }

    private void addNavigationButtons(Inventory inventory, int totalItems, int itemsPerPage) {
        int totalPages = (int) Math.ceil((double) totalItems / itemsPerPage);
        if (currentPage > 0) {
            addPreviousPageButton(inventory, totalPages);
        }
        if (currentPage < totalPages - 1) {
            addNextPageButton(inventory, totalPages);
        }
    }

    private ItemStack createMaterialItem(String materialName) {
        String baseMaterial = materialName.contains(";") ? materialName.split(";")[0] : materialName;

        String displayName = File.getConfig().getString("items." + materialName, baseMaterial);

        Optional<XMaterial> xMaterialOpt = XMaterial.matchXMaterial(baseMaterial);
        if (!xMaterialOpt.isPresent()) {
            xMaterialOpt = Optional.of(XMaterial.STONE);
        }
        ItemStack item = xMaterialOpt.get().parseItem();
        if (item == null) return null;

        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatUtils.colorizewp(config.getString("items.material_item.name", "&e#material_name#")
                    .replace("#material_name#", displayName)));

            List<String> lore = new ArrayList<>();
            String selectionLore = config.getString("selection_lore." + selectionType, "");
            for (String line : config.getStringList("items.material_item.lore")) {
                lore.add(ChatUtils.colorizewp(line.replace("#selection_type_lore#", selectionLore)));
            }
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    private List<String> getPlayerStorageMaterials() {
        List<String> materials = new ArrayList<>();

        FileConfiguration config = File.getConfig();
        if (config.contains("items")) {
            Set<String> itemKeys = config.getConfigurationSection("items").getKeys(false);
            materials.addAll(itemKeys);
        }

        Collections.sort(materials);
        return materials;
    }

    private void selectMaterial(Player player, String materialName) {
        if (selectionType.equals("result")) {
            recipe.setResultMaterial(materialName);
            player.sendMessage(ChatUtils.colorize(
                    File.getMessage().getString("crafting.edit_material_success")));
            CraftingManager.updateRecipe(recipe);
            returnToRecipeEditor(player);
        } else if (selectionType.equals("requirement")) {
            String normalizedMaterial = MineManager.normalizeMaterial(materialName);
            recipe.getMaterialRequirements().put(normalizedMaterial, 1);
            player.sendMessage(ChatUtils.colorize(
                    File.getMessage().getString("crafting.requirement_added_gui")
                            .replace("#material#", materialName)
                            .replace("#amount#", "1")));
            CraftingManager.updateRecipe(recipe);
            returnToMaterialEditor(player);
        }
    }

    private void returnToRecipeEditor(Player player) {
        SoundManager.setShouldPlayCloseSound(player, false);
        player.openInventory(new RecipeEditorGUI(player, recipe).getInventory(SoundContext.SILENT));
    }

    private void returnToMaterialEditor(Player player) {
        SoundManager.setShouldPlayCloseSound(player, false);
        player.openInventory(new MaterialEditorGUI(player, recipe).getInventory(SoundContext.SILENT));
    }

    private void addPreviousPageButton(Inventory inventory, int totalPages) {
        ItemStack prevItem = ItemManager.getItemConfigWithPlaceholders(player,
                config.getConfigurationSection("items.previous_page"),
                "#current_page#", String.valueOf(currentPage + 1),
                "#total_pages#", String.valueOf(totalPages));

        if (prevItem != null) {
            InteractiveItem prevButton = new InteractiveItem(prevItem, config.getInt("items.previous_page.slot", 45))
                    .onLeftClick(p -> {
                        SoundManager.setShouldPlayCloseSound(p, false);
                        p.openInventory(new MaterialSelectionGUI(p, recipe, selectionType, currentPage - 1).getInventory(SoundContext.SILENT));
                    });
            inventory.setItem(prevButton.getSlot(), prevButton);
        }
    }

    private void addNextPageButton(Inventory inventory, int totalPages) {
        ItemStack nextItem = ItemManager.getItemConfigWithPlaceholders(player,
                config.getConfigurationSection("items.next_page"),
                "#current_page#", String.valueOf(currentPage + 1),
                "#total_pages#", String.valueOf(totalPages));

        if (nextItem != null) {
            InteractiveItem nextButton = new InteractiveItem(nextItem, config.getInt("items.next_page.slot", 53))
                    .onLeftClick(p -> {
                        SoundManager.setShouldPlayCloseSound(p, false);
                        p.openInventory(new MaterialSelectionGUI(p, recipe, selectionType, currentPage + 1).getInventory(SoundContext.SILENT));
                    });
            inventory.setItem(nextButton.getSlot(), nextButton);
        }
    }

    private void addBackButton(Inventory inventory) {
        ItemStack backItem = ItemManager.getItemConfig(config.getConfigurationSection("items.back"));
        if (backItem != null) {
            InteractiveItem backButton = new InteractiveItem(backItem, config.getInt("items.back.slot", 49))
                    .onLeftClick(p -> {
                        if (selectionType.equals("requirement")) {
                            returnToMaterialEditor(p);
                        } else {
                            returnToRecipeEditor(p);
                        }
                    });
            inventory.setItem(backButton.getSlot(), backButton);
        }
    }

    private void addSearchButton(Inventory inventory) {
        ItemStack searchItem = ItemManager.getItemConfig(config.getConfigurationSection("items.search"));
        if (searchItem != null) {
            InteractiveItem searchButton = new InteractiveItem(searchItem, config.getInt("items.search.slot", 4))
                    .onLeftClick(p -> {
                        if (selectionType.equals("result")) {
                            RecipeEditManager.requestMaterialEdit(p, recipe);
                        } else {
                            RecipeEditManager.requestRequirementAdd(p, recipe);
                        }
                    });
            inventory.setItem(searchButton.getSlot(), searchButton);
        }
    }

}
