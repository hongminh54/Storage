package net.danh.storage.GUI;

import net.danh.storage.GUI.manager.IGUI;
import net.danh.storage.GUI.manager.InteractiveItem;
import net.danh.storage.Manager.CraftingManager;
import net.danh.storage.Manager.ItemManager;
import net.danh.storage.Manager.MineManager;
import net.danh.storage.Manager.SoundManager;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public class RecipeListGUI implements IGUI {

    private final Player player;
    private final FileConfiguration config;
    private final int currentPage;
    private final String currentCategory;
    private List<Recipe> cachedFilteredRecipes;

    public RecipeListGUI(Player player) {
        this(player, 0, "all");
    }

    public RecipeListGUI(Player player, int currentPage, String category) {
        this.player = player;
        this.currentPage = currentPage;
        this.currentCategory = category;
        this.config = File.getRecipeListGUIConfig();
    }

    @NotNull
    @Override
    public Inventory getInventory() {
        return getInventory(SoundContext.INITIAL_OPEN);
    }

    @NotNull
    public Inventory getInventory(SoundContext context) {
        SoundManager.playItemSound(player, config, "gui_open_sound", context);

        String title = ChatUtils.colorizewp(Objects.requireNonNull(config.getString("title", "&6Crafting Menu"))
                .replace("#player#", player.getName()));
        Inventory inventory = Bukkit.createInventory(this, config.getInt("size", 6) * 9, title);

        setupItems(inventory);
        return inventory;
    }

    private void setupItems(Inventory inventory) {
        setupDecorativeItems(inventory);
        setupRecipeItems(inventory);
        setupNavigationButtons(inventory);
        setupCategoryFilter(inventory);
        setupCloseButton(inventory);
    }

    private void setupDecorativeItems(Inventory inventory) {
        String decorateSlots = config.getString("items.decorates.slot");
        if (decorateSlots != null) {
            for (String slotStr : decorateSlots.split(",")) {
                int slot = Number.getInteger(slotStr.trim());
                if (slot >= 0 && slot < inventory.getSize()) {
                    InteractiveItem decorateItem = new InteractiveItem(
                            ItemManager.getItemConfig(Objects.requireNonNull(config.getConfigurationSection("items.decorates"))),
                            slot
                    );
                    inventory.setItem(decorateItem.getSlot(), decorateItem);
                }
            }
        }
    }

    private void setupRecipeItems(Inventory inventory) {
        List<Recipe> availableRecipes = getFilteredRecipes();

        if (availableRecipes.isEmpty()) {
            addNoRecipesItem(inventory);
            return;
        }

        String recipeSlots = config.getString("items.recipe_item.slot");
        if (recipeSlots == null) return;

        String[] slotArray = recipeSlots.split(",");
        int itemsPerPage = slotArray.length;
        int startIndex = currentPage * itemsPerPage;
        int endIndex = Math.min(startIndex + itemsPerPage, availableRecipes.size());

        for (int i = startIndex; i < endIndex; i++) {
            Recipe recipe = availableRecipes.get(i);
            int slotIndex = i - startIndex;
            if (slotIndex < slotArray.length) {
                int slot = Number.getInteger(slotArray[slotIndex].trim());
                ItemStack recipeItem = createRecipeItem(recipe);
                if (recipeItem != null) {
                    InteractiveItem interactiveItem = new InteractiveItem(recipeItem, slot)
                            .onClick((p, clickType) -> handleRecipeClick(p, recipe, clickType));
                    inventory.setItem(interactiveItem.getSlot(), interactiveItem);
                }
            }
        }
    }

    private void addNoRecipesItem(Inventory inventory) {
        String slotConfig = config.getString("items.no_recipes.slot", "22");
        ItemStack noRecipesItem = ItemManager.getItemConfig(Objects.requireNonNull(config.getConfigurationSection("items.no_recipes")));

        if (slotConfig.contains(",")) {
            for (String slotStr : slotConfig.split(",")) {
                int slot = Number.getInteger(slotStr.trim());
                InteractiveItem item = new InteractiveItem(noRecipesItem.clone(), slot);
                inventory.setItem(item.getSlot(), item);
            }
        } else {
            int slot = Number.getInteger(slotConfig);
            InteractiveItem item = new InteractiveItem(noRecipesItem, slot);
            inventory.setItem(item.getSlot(), item);
        }
    }

    private void handleRecipeClick(Player p, Recipe recipe, org.bukkit.event.inventory.ClickType clickType) {
        if (clickType == org.bukkit.event.inventory.ClickType.LEFT) {
            craftAllPossible(p, recipe);
        } else if (clickType == org.bukkit.event.inventory.ClickType.RIGHT) {
            craftOne(p, recipe);
        } else if (clickType == org.bukkit.event.inventory.ClickType.SHIFT_LEFT) {
            craftCustomAmount(p, recipe);
        }
    }

    private void setupNavigationButtons(Inventory inventory) {
        List<Recipe> availableRecipes = getFilteredRecipes();
        String recipeSlots = config.getString("items.recipe_item.slot");
        if (recipeSlots == null) return;

        int itemsPerPage = recipeSlots.split(",").length;
        int totalPages = Math.max(1, (int) Math.ceil((double) availableRecipes.size() / itemsPerPage));

        if (totalPages > 1) {
            if (currentPage > 0) {
                addPreviousPageButton(inventory, totalPages);
            }
            if (currentPage < totalPages - 1) {
                addNextPageButton(inventory, totalPages);
            }
        }
    }

    private List<Recipe> getFilteredRecipes() {
        if (cachedFilteredRecipes == null) {
            List<Recipe> recipes = CraftingManager.getAvailableRecipes(player);
            if (!currentCategory.equals("all")) {
                cachedFilteredRecipes = recipes.stream()
                        .filter(recipe -> recipe.getCategory().equals(currentCategory))
                        .collect(Collectors.toList());
            } else {
                cachedFilteredRecipes = recipes;
            }
        }
        return cachedFilteredRecipes;
    }

    private ItemStack createRecipeItem(Recipe recipe) {
        ItemStack resultItem = CraftingManager.createResultItem(recipe);
        if (resultItem == null) return null;

        List<String> lore = config.getStringList("items.recipe_item.lore");
        List<String> processedLore = new ArrayList<>();

        for (String line : lore) {
            if (line.contains("#requirements#")) {
                List<String> requirementLines = createRequirementLines(recipe);
                if (requirementLines.isEmpty()) {
                    processedLore.add(ChatUtils.colorizewp(line.replace("#requirements#", "&7No requirements")));
                } else {
                    for (String reqLine : requirementLines) {
                        processedLore.add(ChatUtils.colorizewp(reqLine));
                    }
                }
            } else {
                String categoryDisplayName = CraftingManager.getCategoryDisplayName(recipe.getCategory());
                String processed = line
                        .replace("#recipe_name#", recipe.getName())
                        .replace("#category#", categoryDisplayName)
                        .replace("#result_amount#", String.valueOf(recipe.getResultAmount()))
                        .replace("#result_name#", recipe.getResultName());
                processedLore.add(ChatUtils.colorizewp(processed));
            }
        }

        if (resultItem.getItemMeta() != null) {
            ItemMeta meta = resultItem.getItemMeta();
            meta.setLore(processedLore);
            resultItem.setItemMeta(meta);
        }
        return resultItem;
    }

    private List<String> createRequirementLines(Recipe recipe) {
        List<String> requirementLines = new ArrayList<>();

        for (Map.Entry<String, Integer> req : recipe.getMaterialRequirements().entrySet()) {
            String materialKey = req.getKey();
            String normalizedMaterial = MineManager.normalizeMaterial(materialKey);
            int playerAmount = MineManager.getPlayerBlock(player, normalizedMaterial);

            String displayName = File.getConfig().getString("items." + materialKey, materialKey);
            if (materialKey.contains(";")) {
                displayName = File.getConfig().getString("items." + materialKey, materialKey.split(";")[0]);
            }

            String color = playerAmount >= req.getValue() ? "&a" : "&c";
            requirementLines.add("  " + color + req.getValue() + "x " + displayName + " &7(" + playerAmount + ")");
        }

        return requirementLines;
    }

    private void craftOne(Player player, Recipe recipe) {
        if (!validateRecipe(player, recipe)) return;

        if (CraftingManager.craftRecipe(player, recipe.getId(), 1)) {
            handlePostCraft(player);
        }
    }

    private void craftAllPossible(Player player, Recipe recipe) {
        if (!validateRecipe(player, recipe)) return;

        int maxCraftable = CraftingManager.getMaxCraftableAmount(player, recipe);
        if (maxCraftable <= 0) {
            player.sendMessage(ChatUtils.colorize(File.getMessage().getString("crafting.insufficient_materials")
                    .replace("#recipe#", recipe.getName())));
            SoundManager.playSound(player, SoundManager.SoundType.ACTION_ERROR);
            return;
        }

        int maxCraftableBySpace = calculateMaxCraftableByInventorySpace(player, recipe);
        int safeCraftAmount = Math.min(maxCraftable, maxCraftableBySpace);

        if (CraftingManager.craftRecipe(player, recipe.getId(), safeCraftAmount)) {
            handlePostCraft(player);
        }
    }

    private void craftCustomAmount(Player player, Recipe recipe) {
        if (!validateRecipe(player, recipe)) return;
        CraftingManager.requestCraftAmount(player, recipe.getId());
    }

    private boolean validateRecipe(Player player, Recipe recipe) {
        if (recipe == null || !recipe.isEnabled()) {
            player.sendMessage(ChatUtils.colorize(File.getMessage().getString("crafting.recipe_disabled")
                    .replace("#recipe#", recipe != null ? recipe.getName() : "Unknown")));
            SoundManager.playSound(player, SoundManager.SoundType.ACTION_ERROR);
            return false;
        }
        return true;
    }

    private void handlePostCraft(Player player) {
        if (!CraftingManager.isCraftingInProgress(player)) {
            SoundManager.setShouldPlayCloseSound(player, false);
            player.openInventory(new RecipeListGUI(player, currentPage, currentCategory).getInventory(SoundContext.SILENT));
        } else {
            player.closeInventory();
        }
    }

    private void addPreviousPageButton(Inventory inventory, int totalPages) {
        String slotConfig = config.getString("items.previous_page.slot", "45");
        ItemStack prevItem = ItemManager.getItemConfigWithPlaceholders(player,
                Objects.requireNonNull(config.getConfigurationSection("items.previous_page")),
                "#current_page#", String.valueOf(currentPage + 1),
                "#total_pages#", String.valueOf(totalPages));

        if (slotConfig.contains(",")) {
            for (String slotStr : slotConfig.split(",")) {
                int slot = Number.getInteger(slotStr.trim());
                InteractiveItem prevButton = new InteractiveItem(prevItem.clone(), slot)
                        .onLeftClick(p -> {
                            SoundManager.playItemSound(p, config, "items.previous_page", SoundContext.SILENT);
                            SoundManager.setShouldPlayCloseSound(p, false);
                            p.openInventory(new RecipeListGUI(p, currentPage - 1, currentCategory).getInventory(SoundContext.SILENT));
                        });
                inventory.setItem(prevButton.getSlot(), prevButton);
            }
        } else {
            int slot = Number.getInteger(slotConfig);
            InteractiveItem prevButton = new InteractiveItem(prevItem, slot)
                    .onLeftClick(p -> {
                        SoundManager.playItemSound(p, config, "items.previous_page", SoundContext.SILENT);
                        SoundManager.setShouldPlayCloseSound(p, false);
                        p.openInventory(new RecipeListGUI(p, currentPage - 1, currentCategory).getInventory(SoundContext.SILENT));
                    });
            inventory.setItem(prevButton.getSlot(), prevButton);
        }
    }

    private void addNextPageButton(Inventory inventory, int totalPages) {
        String slotConfig = config.getString("items.next_page.slot", "53");
        ItemStack nextItem = ItemManager.getItemConfigWithPlaceholders(player,
                Objects.requireNonNull(config.getConfigurationSection("items.next_page")),
                "#current_page#", String.valueOf(currentPage + 1),
                "#total_pages#", String.valueOf(totalPages));

        if (slotConfig.contains(",")) {
            for (String slotStr : slotConfig.split(",")) {
                int slot = Number.getInteger(slotStr.trim());
                InteractiveItem nextButton = new InteractiveItem(nextItem.clone(), slot)
                        .onLeftClick(p -> {
                            SoundManager.playItemSound(p, config, "items.next_page", SoundContext.SILENT);
                            SoundManager.setShouldPlayCloseSound(p, false);
                            p.openInventory(new RecipeListGUI(p, currentPage + 1, currentCategory).getInventory(SoundContext.SILENT));
                        });
                inventory.setItem(nextButton.getSlot(), nextButton);
            }
        } else {
            int slot = Number.getInteger(slotConfig);
            InteractiveItem nextButton = new InteractiveItem(nextItem, slot)
                    .onLeftClick(p -> {
                        SoundManager.playItemSound(p, config, "items.next_page", SoundContext.SILENT);
                        SoundManager.setShouldPlayCloseSound(p, false);
                        p.openInventory(new RecipeListGUI(p, currentPage + 1, currentCategory).getInventory(SoundContext.SILENT));
                    });
            inventory.setItem(nextButton.getSlot(), nextButton);
        }
    }

    private void setupCategoryFilter(Inventory inventory) {
        if (!config.contains("items.category_filter")) return;

        String slotConfig = config.getString("items.category_filter.slot", "49");
        String categoryDisplayName = CraftingManager.getCategoryDisplayName(currentCategory);
        ItemStack filterItem = ItemManager.getItemConfigWithPlaceholders(player,
                Objects.requireNonNull(config.getConfigurationSection("items.category_filter")),
                "#current_category#", categoryDisplayName);

        if (slotConfig.contains(",")) {
            for (String slotStr : slotConfig.split(",")) {
                int slot = Number.getInteger(slotStr.trim());
                InteractiveItem filterButton = new InteractiveItem(filterItem.clone(), slot)
                        .onLeftClick(p -> cycleCategory(p));
                inventory.setItem(filterButton.getSlot(), filterButton);
            }
        } else {
            int slot = Number.getInteger(slotConfig);
            InteractiveItem filterButton = new InteractiveItem(filterItem, slot)
                    .onLeftClick(p -> cycleCategory(p));
            inventory.setItem(filterButton.getSlot(), filterButton);
        }
    }

    private void cycleCategory(Player p) {
        List<String> categories = new ArrayList<>(CraftingManager.getCategories());
        categories.add(0, "all");

        int currentIndex = categories.indexOf(currentCategory);
        int nextIndex = (currentIndex + 1) % categories.size();
        String nextCategory = categories.get(nextIndex);

        SoundManager.playItemSound(p, config, "items.category_filter", SoundContext.SILENT);
        SoundManager.setShouldPlayCloseSound(p, false);
        p.openInventory(new RecipeListGUI(p, 0, nextCategory).getInventory(SoundContext.SILENT));
    }

    private void setupCloseButton(Inventory inventory) {
        String slotConfig = config.getString("items.close.slot", "48");
        ItemStack closeItem = ItemManager.getItemConfig(Objects.requireNonNull(config.getConfigurationSection("items.close")));

        if (slotConfig.contains(",")) {
            for (String slotStr : slotConfig.split(",")) {
                int slot = Number.getInteger(slotStr.trim());
                InteractiveItem closeButton = new InteractiveItem(closeItem.clone(), slot)
                        .onLeftClick(p -> {
                            SoundManager.playItemSound(p, config, "items.close", SoundContext.INITIAL_OPEN);
                            p.closeInventory();
                        });
                inventory.setItem(closeButton.getSlot(), closeButton);
            }
        } else {
            int slot = Number.getInteger(slotConfig);
            InteractiveItem closeButton = new InteractiveItem(closeItem, slot)
                    .onLeftClick(p -> {
                        SoundManager.playItemSound(p, config, "items.close", SoundContext.INITIAL_OPEN);
                        p.closeInventory();
                    });
            inventory.setItem(closeButton.getSlot(), closeButton);
        }
    }

    private int calculateMaxCraftableByInventorySpace(Player player, Recipe recipe) {
        ItemStack resultItem = CraftingManager.createResultItem(recipe);
        if (resultItem == null) return 0;

        int availableSpace = CraftingManager.calculateInventorySpace(player, resultItem);
        int itemsPerCraft = recipe.getResultAmount();

        return availableSpace / itemsPerCraft;
    }
}
