package net.danh.storage.GUI;

import net.danh.storage.GUI.manager.IGUI;
import net.danh.storage.GUI.manager.InteractiveItem;
import net.danh.storage.Manager.CraftingManager;
import net.danh.storage.Manager.ItemManager;
import net.danh.storage.Manager.MineManager;
import net.danh.storage.Manager.SoundManager;
import net.danh.storage.Recipe.Recipe;
import net.danh.storage.Utils.Chat;
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

    public RecipeListGUI(Player player) {
        this(player, 0, "all");
    }

    public RecipeListGUI(Player player, int currentPage, String category) {
        this.player = player;
        this.currentPage = currentPage;
        this.currentCategory = category;
        this.config = File.getFileSetting().get("GUI/recipe-list.yml");
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
                .replace("#player#", player.getName()));
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

        // Get available recipes
        List<Recipe> availableRecipes = CraftingManager.getAvailableRecipes(player);
        
        // Filter by category if not "all"
        if (!currentCategory.equals("all")) {
            availableRecipes = availableRecipes.stream()
                    .filter(recipe -> recipe.getCategory().equals(currentCategory))
                    .collect(Collectors.toList());
        }

        if (availableRecipes.isEmpty()) {
            // Show no recipes available message
            InteractiveItem noRecipesItem = new InteractiveItem(
                    ItemManager.getItemConfig(Objects.requireNonNull(config.getConfigurationSection("items.no_recipes"))),
                    22
            );
            inventory.setItem(noRecipesItem.getSlot(), noRecipesItem);
            return;
        }

        // Setup pagination
        String recipeSlots = config.getString("items.recipe_item.slot");
        if (recipeSlots == null) return;
        
        String[] slotArray = recipeSlots.split(",");
        int itemsPerPage = slotArray.length;
        int totalPages = Math.max(1, (int) Math.ceil((double) availableRecipes.size() / itemsPerPage));
        
        // Add recipe items
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
                            .onLeftClick(p -> craftAllPossible(p, recipe))
                            .onRightClick(p -> craftCustomAmount(p, recipe));

                    SoundManager.playItemSound(player, config, "items.recipe_item", SoundContext.SILENT);
                    inventory.setItem(interactiveItem.getSlot(), interactiveItem);
                }
            }
        }

        // Add navigation buttons
        if (totalPages > 1) {
            if (currentPage > 0) {
                addPreviousPageButton(inventory, totalPages);
            }
            if (currentPage < totalPages - 1) {
                addNextPageButton(inventory, totalPages);
            }
        }

        // Add category filter if multiple categories exist
        if (CraftingManager.getCategories().size() > 1) {
            addCategoryFilterButton(inventory);
        }

        // Add close button
        addCloseButton(inventory);
    }

    private ItemStack createRecipeItem(Recipe recipe) {
        ItemStack resultItem = CraftingManager.createResultItem(recipe);
        if (resultItem == null) return null;

        // Apply placeholders to lore
        List<String> lore = config.getStringList("items.recipe_item.lore");
        List<String> processedLore = new ArrayList<>();

        for (String line : lore) {
            if (line.contains("#requirements#")) {
                // Replace requirements placeholder with actual requirement lines
                List<String> requirementLines = createRequirementLines(recipe);
                if (requirementLines.isEmpty()) {
                    processedLore.add(Chat.colorizewp(line.replace("#requirements#", "&7None")));
                } else {
                    // Add each requirement as a separate line
                    for (String reqLine : requirementLines) {
                        processedLore.add(Chat.colorizewp(reqLine));
                    }
                }
            } else {
                String processed = line
                        .replace("#recipe_name#", recipe.getName())
                        .replace("#category#", recipe.getCategory())
                        .replace("#result_amount#", String.valueOf(recipe.getResultAmount()))
                        .replace("#result_name#", recipe.getResultName());
                processedLore.add(Chat.colorizewp(processed));
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

        // Add material requirements only
        for (Map.Entry<String, Integer> req : recipe.getMaterialRequirements().entrySet()) {
            String materialName = req.getKey().split(";")[0];
            int playerAmount = MineManager.getPlayerBlock(player, req.getKey());
            String color = playerAmount >= req.getValue() ? "&a" : "&c";
            requirementLines.add("  " + color + req.getValue() + "x " + materialName + " &7(" + playerAmount + ")");
        }

        return requirementLines;
    }

    private void craftAllPossible(Player player, Recipe recipe) {
        if (recipe == null || !recipe.isEnabled()) {
            player.sendMessage(Chat.colorize(File.getMessage().getString("recipe.recipe_disabled")
                    .replace("#recipe#", recipe != null ? recipe.getName() : "Unknown")));
            SoundManager.playSound(player, SoundManager.SoundType.ACTION_ERROR);
            return;
        }

        int maxCraftable = CraftingManager.getMaxCraftableAmount(player, recipe);
        if (maxCraftable <= 0) {
            player.sendMessage(Chat.colorize(File.getMessage().getString("recipe.insufficient_materials")
                    .replace("#recipe#", recipe.getName())));
            SoundManager.playSound(player, SoundManager.SoundType.ACTION_ERROR);
            return;
        }

        if (CraftingManager.craftRecipe(player, recipe.getId(), maxCraftable)) {
            SoundManager.setShouldPlayCloseSound(player, false);
            player.openInventory(new RecipeListGUI(player, currentPage, currentCategory).getInventory(SoundContext.SILENT));
        }
    }

    private void craftCustomAmount(Player player, Recipe recipe) {
        if (recipe == null || !recipe.isEnabled()) {
            player.sendMessage(Chat.colorize(File.getMessage().getString("recipe.recipe_disabled")
                    .replace("#recipe#", recipe != null ? recipe.getName() : "Unknown")));
            SoundManager.playSound(player, SoundManager.SoundType.ACTION_ERROR);
            return;
        }

        CraftingManager.requestCraftAmount(player, recipe.getId());
    }

    private void addPreviousPageButton(Inventory inventory, int totalPages) {
        ItemStack prevItem = ItemManager.getItemConfigWithPlaceholders(player,
                Objects.requireNonNull(config.getConfigurationSection("items.previous_page")),
                "#current_page#", String.valueOf(currentPage + 1),
                "#total_pages#", String.valueOf(totalPages));
        
        InteractiveItem prevButton = new InteractiveItem(prevItem, 45)
                .onLeftClick(p -> {
                    SoundManager.setShouldPlayCloseSound(p, false);
                    p.openInventory(new RecipeListGUI(p, currentPage - 1, currentCategory).getInventory(SoundContext.SILENT));
                });
        inventory.setItem(prevButton.getSlot(), prevButton);
    }

    private void addNextPageButton(Inventory inventory, int totalPages) {
        ItemStack nextItem = ItemManager.getItemConfigWithPlaceholders(player,
                Objects.requireNonNull(config.getConfigurationSection("items.next_page")),
                "#current_page#", String.valueOf(currentPage + 1),
                "#total_pages#", String.valueOf(totalPages));
        
        InteractiveItem nextButton = new InteractiveItem(nextItem, 53)
                .onLeftClick(p -> {
                    SoundManager.setShouldPlayCloseSound(p, false);
                    p.openInventory(new RecipeListGUI(p, currentPage + 1, currentCategory).getInventory(SoundContext.SILENT));
                });
        inventory.setItem(nextButton.getSlot(), nextButton);
    }

    private void addCategoryFilterButton(Inventory inventory) {
        ItemStack categoryItem = ItemManager.getItemConfigWithPlaceholders(player,
                Objects.requireNonNull(config.getConfigurationSection("items.category_filter")),
                "#current_category#", currentCategory.equals("all") ? "All" : currentCategory);
        
        InteractiveItem categoryButton = new InteractiveItem(categoryItem, 4)
                .onLeftClick(p -> cycleCategoryFilter(p));
        inventory.setItem(categoryButton.getSlot(), categoryButton);
    }

    private void cycleCategoryFilter(Player player) {
        List<String> categories = new ArrayList<>(CraftingManager.getCategories());
        categories.add(0, "all");
        
        int currentIndex = categories.indexOf(currentCategory);
        int nextIndex = (currentIndex + 1) % categories.size();
        String nextCategory = categories.get(nextIndex);
        
        SoundManager.setShouldPlayCloseSound(player, false);
        player.openInventory(new RecipeListGUI(player, 0, nextCategory).getInventory(SoundContext.SILENT));
    }

    private void addCloseButton(Inventory inventory) {
        InteractiveItem closeButton = new InteractiveItem(
                ItemManager.getItemConfig(Objects.requireNonNull(config.getConfigurationSection("items.close"))),
                49
        ).onLeftClick(p -> {
            SoundManager.setShouldPlayCloseSound(p, false);
            p.closeInventory();
        });
        inventory.setItem(closeButton.getSlot(), closeButton);
    }

}
