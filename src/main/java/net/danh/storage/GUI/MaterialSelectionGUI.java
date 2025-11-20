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

    // Comprehensive material categories
    private static final String[][] MATERIAL_CATEGORIES = {
            // Building Blocks
            {"STONE", "COBBLESTONE", "DIRT", "GRASS_BLOCK", "SAND", "GRAVEL", "CLAY", "TERRACOTTA", "CONCRETE", "WOOL", "GLASS", "OBSIDIAN", "BEDROCK", "NETHERRACK", "END_STONE"},
            // Ores & Ingots
            {"COAL", "IRON_INGOT", "GOLD_INGOT", "DIAMOND", "EMERALD", "REDSTONE", "LAPIS_LAZULI", "QUARTZ", "NETHERITE_INGOT", "COPPER_INGOT", "IRON_ORE", "GOLD_ORE", "DIAMOND_ORE", "EMERALD_ORE", "COAL_ORE"},
            // Wood & Plants
            {"OAK_LOG", "BIRCH_LOG", "SPRUCE_LOG", "JUNGLE_LOG", "ACACIA_LOG", "DARK_OAK_LOG", "CRIMSON_STEM", "WARPED_STEM", "OAK_PLANKS", "BIRCH_PLANKS", "SPRUCE_PLANKS", "JUNGLE_PLANKS", "ACACIA_PLANKS", "DARK_OAK_PLANKS", "BAMBOO"},
            // Food & Agriculture
            {"WHEAT", "CARROT", "POTATO", "BEETROOT", "SUGAR_CANE", "CACTUS", "MELON", "PUMPKIN", "APPLE", "BREAD", "COOKED_BEEF", "COOKED_PORK", "COOKED_CHICKEN", "COOKED_MUTTON", "COOKED_RABBIT"},
            // Combat & Tools
            {"DIAMOND_SWORD", "IRON_SWORD", "GOLDEN_SWORD", "STONE_SWORD", "WOODEN_SWORD", "BOW", "CROSSBOW", "ARROW", "SHIELD", "DIAMOND_PICKAXE", "IRON_PICKAXE", "GOLDEN_PICKAXE", "STONE_PICKAXE", "WOODEN_PICKAXE"},
            // Mob Drops
            {"LEATHER", "BEEF", "PORK", "CHICKEN", "MUTTON", "RABBIT", "STRING", "FEATHER", "BONE", "GUNPOWDER", "BLAZE_POWDER", "ENDER_PEARL", "SLIME_BALL", "MAGMA_CREAM", "GHAST_TEAR"},
            // Redstone & Mechanisms
            {"REDSTONE", "REDSTONE_TORCH", "LEVER", "BUTTON", "PRESSURE_PLATE", "TRIPWIRE_HOOK", "PISTON", "STICKY_PISTON", "DISPENSER", "DROPPER", "HOPPER", "COMPARATOR", "REPEATER", "OBSERVER", "TARGET"},
            // Nether & End
            {"NETHERRACK", "SOUL_SAND", "SOUL_SOIL", "NETHER_BRICKS", "NETHER_WART", "BLAZE_ROD", "GHAST_TEAR", "MAGMA_CREAM", "NETHER_STAR", "END_STONE", "ENDER_PEARL", "CHORUS_FRUIT", "SHULKER_SHELL", "ELYTRA", "DRAGON_EGG"}
    };

    private static final String[] CATEGORY_NAMES = {
            "Building Blocks", "Ores & Ingots", "Wood & Plants", "Food & Agriculture",
            "Combat & Tools", "Mob Drops", "Redstone & Mechanisms", "Nether & End"
    };

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

        String categoryName = currentCategory < CATEGORY_NAMES.length ? CATEGORY_NAMES[currentCategory] : "All Materials";
        String title = ChatUtils.colorizewp(Objects.requireNonNull(config.getString("title"))
                .replace("#category_name#", categoryName)
                .replace("#player#", player.getName()));

        Inventory inventory = Bukkit.createInventory(this, config.getInt("size") * 9, title);
        setupItems(inventory);
        return inventory;
    }

    private void setupItems(Inventory inventory) {
        setupBorderItems(inventory);
        setupMaterialItems(inventory);
        addCategoryButtons(inventory);
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
        String[] materialsToShow = currentCategory < MATERIAL_CATEGORIES.length ?
                MATERIAL_CATEGORIES[currentCategory] : getAllMaterials();

        String materialSlots = config.getString("items.material_item.slot");
        if (materialSlots == null) return;

        String[] slotArray = materialSlots.split(",");
        int itemsPerPage = slotArray.length;
        int startIndex = currentPage * itemsPerPage;
        int endIndex = Math.min(startIndex + itemsPerPage, materialsToShow.length);

        for (int i = startIndex; i < endIndex; i++) {
            String materialName = materialsToShow[i];
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

        addNavigationButtons(inventory, materialsToShow.length, itemsPerPage);
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
        Optional<XMaterial> xMaterialOpt = XMaterial.matchXMaterial(materialName);
        if (!xMaterialOpt.isPresent()) {
            xMaterialOpt = Optional.of(XMaterial.STONE);
        }
        ItemStack item = xMaterialOpt.get().parseItem();
        if (item == null) return null;

        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatUtils.colorizewp(config.getString("items.material_item.name", "&e#material_name#")
                    .replace("#material_name#", materialName)));

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

    private void selectMaterial(Player player, String materialName) {
        if (selectionType.equals("result")) {
            recipe.setResultMaterial(materialName);
            player.sendMessage(ChatUtils.colorize(
                    File.getMessage().getString("crafting.edit_material_success")));
        } else if (selectionType.equals("requirement")) {
            String normalizedMaterial = MineManager.normalizeMaterial(materialName);
            recipe.getMaterialRequirements().put(normalizedMaterial, 1);
            player.sendMessage(ChatUtils.colorize(
                    File.getMessage().getString("crafting.requirement_added_gui")
                            .replace("#material#", materialName)
                            .replace("#amount#", "1")));
        }

        CraftingManager.updateRecipe(recipe);
        returnToRecipeEditor(player);
    }

    private void returnToRecipeEditor(Player player) {
        SoundManager.setShouldPlayCloseSound(player, false);
        player.openInventory(new RecipeEditorGUI(player, recipe).getInventory(SoundContext.SILENT));
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
                        p.openInventory(new MaterialSelectionGUI(p, recipe, selectionType, currentPage - 1, currentCategory).getInventory(SoundContext.SILENT));
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
                        p.openInventory(new MaterialSelectionGUI(p, recipe, selectionType, currentPage + 1, currentCategory).getInventory(SoundContext.SILENT));
                    });
            inventory.setItem(nextButton.getSlot(), nextButton);
        }
    }

    private void addBackButton(Inventory inventory) {
        ItemStack backItem = ItemManager.getItemConfig(config.getConfigurationSection("items.back"));
        if (backItem != null) {
            InteractiveItem backButton = new InteractiveItem(backItem, config.getInt("items.back.slot", 49))
                    .onLeftClick(this::returnToRecipeEditor);
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

    private String[] getAllMaterials() {
        List<String> allMaterials = new ArrayList<>();
        for (String[] category : MATERIAL_CATEGORIES) {
            allMaterials.addAll(Arrays.asList(category));
        }
        return allMaterials.toArray(new String[0]);
    }

    private void addCategoryButtons(Inventory inventory) {
        String categoryName = CATEGORY_NAMES[currentCategory];

        // Previous category button
        if (currentCategory > 0) {
            ItemStack prevCatItem = ItemManager.getItemConfigWithPlaceholders(player,
                    config.getConfigurationSection("items.previous_category"),
                    "#category_name#", categoryName);

            if (prevCatItem != null) {
                InteractiveItem prevCatButton = new InteractiveItem(prevCatItem, config.getInt("items.previous_category.slot", 0))
                        .onLeftClick(p -> {
                            SoundManager.setShouldPlayCloseSound(p, false);
                            p.openInventory(new MaterialSelectionGUI(p, recipe, selectionType, 0, currentCategory - 1).getInventory(SoundContext.SILENT));
                        });
                inventory.setItem(prevCatButton.getSlot(), prevCatButton);
            }
        }

        // Next category button
        if (currentCategory < CATEGORY_NAMES.length - 1) {
            ItemStack nextCatItem = ItemManager.getItemConfigWithPlaceholders(player,
                    config.getConfigurationSection("items.next_category"),
                    "#category_name#", categoryName);

            if (nextCatItem != null) {
                InteractiveItem nextCatButton = new InteractiveItem(nextCatItem, config.getInt("items.next_category.slot", 8))
                        .onLeftClick(p -> {
                            SoundManager.setShouldPlayCloseSound(p, false);
                            p.openInventory(new MaterialSelectionGUI(p, recipe, selectionType, 0, currentCategory + 1).getInventory(SoundContext.SILENT));
                        });
                inventory.setItem(nextCatButton.getSlot(), nextCatButton);
            }
        }

        // Category info button
        ItemStack categoryInfoItem = ItemManager.getItemConfigWithPlaceholders(player,
                config.getConfigurationSection("items.category_info"),
                "#category_name#", categoryName,
                "#category_number#", String.valueOf(currentCategory + 1),
                "#total_categories#", String.valueOf(CATEGORY_NAMES.length));

        if (categoryInfoItem != null) {
            InteractiveItem categoryInfoButton = new InteractiveItem(categoryInfoItem, config.getInt("items.category_info.slot", 4))
                    .onLeftClick(p -> {
                        int nextCategory = (currentCategory + 1) % CATEGORY_NAMES.length;
                        SoundManager.setShouldPlayCloseSound(p, false);
                        p.openInventory(new MaterialSelectionGUI(p, recipe, selectionType, 0, nextCategory).getInventory(SoundContext.SILENT));
                    });
            inventory.setItem(categoryInfoButton.getSlot(), categoryInfoButton);
        }
    }
}
