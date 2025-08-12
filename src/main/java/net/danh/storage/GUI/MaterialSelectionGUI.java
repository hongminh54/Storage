package net.danh.storage.GUI;

import com.cryptomorin.xseries.XMaterial;
import net.danh.storage.GUI.manager.IGUI;
import net.danh.storage.GUI.manager.InteractiveItem;
import net.danh.storage.Manager.SoundManager;
import net.danh.storage.Recipe.Recipe;
import net.danh.storage.Utils.Chat;
import net.danh.storage.Utils.SoundContext;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

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
    private final String selectionType; // "result" or "requirement"
    private final int currentPage;
    private final int currentCategory;

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
    }

    @NotNull
    @Override
    public Inventory getInventory() {
        return getInventory(SoundContext.INITIAL_OPEN);
    }

    @NotNull
    @Override
    public Inventory getInventory(SoundContext context) {
        String categoryName = currentCategory < CATEGORY_NAMES.length ? CATEGORY_NAMES[currentCategory] : "All Materials";
        String title = Chat.colorizewp("&0" + categoryName + " | " + player.getName());
        Inventory inventory = Bukkit.createInventory(this, 54, title);

        setupItems(inventory);
        return inventory;
    }

    private void setupItems(Inventory inventory) {
        // Add decorative border
        ItemStack borderItem = createBorderItem();
        int[] borderSlots = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 17, 18, 26, 27, 35, 36, 44, 45, 46, 47, 48, 49, 50, 51, 52, 53};
        for (int slot : borderSlots) {
            inventory.setItem(slot, borderItem);
        }

        // Get materials for current category
        String[] materialsToShow = currentCategory < MATERIAL_CATEGORIES.length ?
                MATERIAL_CATEGORIES[currentCategory] : getAllMaterials();

        // Add material items
        int itemsPerPage = 28; // 4 rows of 7 items
        int startIndex = currentPage * itemsPerPage;
        int endIndex = Math.min(startIndex + itemsPerPage, materialsToShow.length);

        int[] materialSlots = {10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23, 24, 25, 28, 29, 30, 31, 32, 33, 34, 37, 38, 39, 40, 41, 42, 43};

        for (int i = startIndex; i < endIndex; i++) {
            String materialName = materialsToShow[i];
            int slotIndex = i - startIndex;
            if (slotIndex < materialSlots.length) {
                ItemStack materialItem = createMaterialItem(materialName);
                if (materialItem != null) {
                    InteractiveItem interactiveItem = new InteractiveItem(materialItem, materialSlots[slotIndex])
                            .onLeftClick(p -> selectMaterial(p, materialName));
                    inventory.setItem(interactiveItem.getSlot(), interactiveItem);
                }
            }
        }

        // Add navigation buttons
        int totalPages = (int) Math.ceil((double) materialsToShow.length / itemsPerPage);
        if (currentPage > 0) {
            addPreviousPageButton(inventory, totalPages);
        }
        if (currentPage < totalPages - 1) {
            addNextPageButton(inventory, totalPages);
        }

        // Add category navigation
        addCategoryButtons(inventory);

        // Add back button
        addBackButton(inventory);

        // Add search button (uses chat input)
        addSearchButton(inventory);
    }

    private ItemStack createBorderItem() {
        ItemStack item = new ItemStack(XMaterial.BLACK_STAINED_GLASS_PANE.parseMaterial());
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(Chat.colorizewp("&7 "));
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createMaterialItem(String materialName) {
        XMaterial xMaterial = XMaterial.matchXMaterial(materialName).orElse(XMaterial.STONE);
        ItemStack item = xMaterial.parseItem();
        if (item == null) return null;

        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(Chat.colorizewp("&e" + materialName));
            List<String> lore = new ArrayList<>();
            lore.add(Chat.colorizewp("&7Click to select this material"));
            if (selectionType.equals("result")) {
                lore.add(Chat.colorizewp("&7for the result item"));
            } else {
                lore.add(Chat.colorizewp("&7as a requirement"));
            }
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    private void selectMaterial(Player player, String materialName) {
        if (selectionType.equals("result")) {
            recipe.setResultMaterial(materialName + ";0");
            player.sendMessage(Chat.colorize("&aResult material set to: " + materialName));
        } else if (selectionType.equals("requirement")) {
            recipe.getMaterialRequirements().put(materialName + ";0", 1);
            player.sendMessage(Chat.colorize("&aAdded requirement: " + materialName + " x1"));
        }

        // Save and return to recipe editor
        net.danh.storage.Manager.CraftingManager.updateRecipe(recipe);
        returnToRecipeEditor(player);
    }

    private void returnToRecipeEditor(Player player) {
        SoundManager.setShouldPlayCloseSound(player, false);
        player.openInventory(new RecipeEditorGUI(player, recipe).getInventory(SoundContext.SILENT));
    }

    private void addPreviousPageButton(Inventory inventory, int totalPages) {
        ItemStack prevItem = new ItemStack(XMaterial.ARROW.parseMaterial());
        ItemMeta meta = prevItem.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(Chat.colorizewp("&aTrang Trước"));
            List<String> lore = Arrays.asList(
                    Chat.colorizewp("&7Trang " + (currentPage + 1) + " / " + totalPages),
                    Chat.colorizewp("&eNhấp để đi đến trang trước")
            );
            meta.setLore(lore);
            prevItem.setItemMeta(meta);
        }

        InteractiveItem prevButton = new InteractiveItem(prevItem, 45)
                .onLeftClick(p -> {
                    SoundManager.setShouldPlayCloseSound(p, false);
                    p.openInventory(new MaterialSelectionGUI(p, recipe, selectionType, currentPage - 1, currentCategory).getInventory(SoundContext.SILENT));
                });
        inventory.setItem(prevButton.getSlot(), prevButton);
    }

    private void addNextPageButton(Inventory inventory, int totalPages) {
        ItemStack nextItem = new ItemStack(XMaterial.ARROW.parseMaterial());
        ItemMeta meta = nextItem.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(Chat.colorizewp("&aTrang sau"));
            List<String> lore = Arrays.asList(
                    Chat.colorizewp("&7Trang " + (currentPage + 1) + " / " + totalPages),
                    Chat.colorizewp("&eNhấp để đi đến trang sau")
            );
            meta.setLore(lore);
            nextItem.setItemMeta(meta);
        }

        InteractiveItem nextButton = new InteractiveItem(nextItem, 53)
                .onLeftClick(p -> {
                    SoundManager.setShouldPlayCloseSound(p, false);
                    p.openInventory(new MaterialSelectionGUI(p, recipe, selectionType, currentPage + 1, currentCategory).getInventory(SoundContext.SILENT));
                });
        inventory.setItem(nextButton.getSlot(), nextButton);
    }

    private void addBackButton(Inventory inventory) {
        ItemStack backItem = new ItemStack(XMaterial.BARRIER.parseMaterial());
        ItemMeta meta = backItem.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(Chat.colorizewp("&cQuay Lại"));
            List<String> lore = Collections.singletonList(
                    Chat.colorizewp("&7Quay lại trình chỉnh sửa công thức")
            );
            meta.setLore(lore);
            backItem.setItemMeta(meta);
        }

        InteractiveItem backButton = new InteractiveItem(backItem, 49)
                .onLeftClick(this::returnToRecipeEditor);
        inventory.setItem(backButton.getSlot(), backButton);
    }

    private void addSearchButton(Inventory inventory) {
        ItemStack searchItem = new ItemStack(XMaterial.COMPASS.parseMaterial());
        ItemMeta meta = searchItem.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(Chat.colorizewp("&6Chọn Material"));
            List<String> lore = Arrays.asList(
                    Chat.colorizewp("&7Nhấp để lựa chọn"),
                    Chat.colorizewp("&7material cụ thể theo tên")
            );
            meta.setLore(lore);
            searchItem.setItemMeta(meta);
        }

        InteractiveItem searchButton = new InteractiveItem(searchItem, 4)
                .onLeftClick(p -> {
                    if (selectionType.equals("result")) {
                        net.danh.storage.Manager.RecipeEditManager.requestMaterialEdit(p, recipe);
                    } else {
                        net.danh.storage.Manager.RecipeEditManager.requestRequirementAdd(p, recipe);
                    }
                });
        inventory.setItem(searchButton.getSlot(), searchButton);
    }

    private String[] getAllMaterials() {
        List<String> allMaterials = new ArrayList<>();
        for (String[] category : MATERIAL_CATEGORIES) {
            allMaterials.addAll(Arrays.asList(category));
        }
        return allMaterials.toArray(new String[0]);
    }

    private void addCategoryButtons(Inventory inventory) {
        // Previous category button
        if (currentCategory > 0) {
            ItemStack prevCatItem = new ItemStack(XMaterial.ARROW.parseMaterial());
            ItemMeta meta = prevCatItem.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(Chat.colorizewp("&eDanh mục trước"));
                meta.setLore(Arrays.asList(
                        Chat.colorizewp("&7Hiện tại: &e" + CATEGORY_NAMES[currentCategory]),
                        Chat.colorizewp("&eNhấp để đi đến danh mục trước")
                ));
                prevCatItem.setItemMeta(meta);
            }
            InteractiveItem prevCatButton = new InteractiveItem(prevCatItem, 0)
                    .onLeftClick(p -> {
                        SoundManager.setShouldPlayCloseSound(p, false);
                        p.openInventory(new MaterialSelectionGUI(p, recipe, selectionType, 0, currentCategory - 1).getInventory(SoundContext.SILENT));
                    });
            inventory.setItem(prevCatButton.getSlot(), prevCatButton);
        }

        // Next category button
        if (currentCategory < CATEGORY_NAMES.length - 1) {
            ItemStack nextCatItem = new ItemStack(XMaterial.ARROW.parseMaterial());
            ItemMeta meta = nextCatItem.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(Chat.colorizewp("&eDanh mục sau"));
                meta.setLore(Arrays.asList(
                        Chat.colorizewp("&7Hiện tại: &e" + CATEGORY_NAMES[currentCategory]),
                        Chat.colorizewp("&eNhấp để đi đến danh mục sau")
                ));
                nextCatItem.setItemMeta(meta);
            }
            InteractiveItem nextCatButton = new InteractiveItem(nextCatItem, 8)
                    .onLeftClick(p -> {
                        SoundManager.setShouldPlayCloseSound(p, false);
                        p.openInventory(new MaterialSelectionGUI(p, recipe, selectionType, 0, currentCategory + 1).getInventory(SoundContext.SILENT));
                    });
            inventory.setItem(nextCatButton.getSlot(), nextCatButton);
        }

        // Category info button
        ItemStack categoryInfoItem = new ItemStack(XMaterial.BOOK.parseMaterial());
        ItemMeta meta = categoryInfoItem.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(Chat.colorizewp("&6" + CATEGORY_NAMES[currentCategory]));
            List<String> lore = new ArrayList<>();
            lore.add(Chat.colorizewp("&7Category " + (currentCategory + 1) + " of " + CATEGORY_NAMES.length));
            lore.add(Chat.colorizewp("&7"));
            lore.add(Chat.colorizewp("&eClick to cycle categories"));
            meta.setLore(lore);
            categoryInfoItem.setItemMeta(meta);
        }
        InteractiveItem categoryInfoButton = new InteractiveItem(categoryInfoItem, 4)
                .onLeftClick(p -> {
                    int nextCategory = (currentCategory + 1) % CATEGORY_NAMES.length;
                    SoundManager.setShouldPlayCloseSound(p, false);
                    p.openInventory(new MaterialSelectionGUI(p, recipe, selectionType, 0, nextCategory).getInventory(SoundContext.SILENT));
                });
        inventory.setItem(categoryInfoButton.getSlot(), categoryInfoButton);
    }
}
