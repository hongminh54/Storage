package net.danh.storage.GUI;

import net.danh.storage.GUI.manager.IGUI;
import net.danh.storage.GUI.manager.InteractiveItem;
import net.danh.storage.Manager.CraftingManager;
import net.danh.storage.Manager.ItemManager;
import net.danh.storage.Manager.SoundManager;
import net.danh.storage.Recipe.Recipe;
import net.danh.storage.Utils.Chat;
import net.danh.storage.Utils.File;
import net.danh.storage.Utils.Number;
import net.danh.storage.Utils.SoundContext;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

public class RecipeEditorListGUI implements IGUI {

    private final Player player;
    private final FileConfiguration config;
    private final int currentPage;

    public RecipeEditorListGUI(Player player) {
        this(player, 0);
    }

    public RecipeEditorListGUI(Player player, int currentPage) {
        this.player = player;
        this.currentPage = currentPage;
        this.config = File.getFileSetting().get("GUI/recipe-editor-list.yml");
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

        // Add create new recipe button
        InteractiveItem createButton = new InteractiveItem(
                ItemManager.getItemConfig(Objects.requireNonNull(config.getConfigurationSection("items.create_recipe"))),
                4
        ).onLeftClick(this::createNewRecipe);
        inventory.setItem(createButton.getSlot(), createButton);

        // Get all recipes
        Collection<Recipe> allRecipes = CraftingManager.getAllRecipes();
        List<Recipe> recipeList = new ArrayList<>(allRecipes);

        if (recipeList.isEmpty()) {
            // Show no recipes message
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
        int totalPages = Math.max(1, (int) Math.ceil((double) recipeList.size() / itemsPerPage));
        
        // Add recipe items
        int startIndex = currentPage * itemsPerPage;
        int endIndex = Math.min(startIndex + itemsPerPage, recipeList.size());
        
        for (int i = startIndex; i < endIndex; i++) {
            Recipe recipe = recipeList.get(i);
            int slotIndex = i - startIndex;
            if (slotIndex < slotArray.length) {
                int slot = Number.getInteger(slotArray[slotIndex].trim());
                
                ItemStack recipeItem = createRecipeEditorItem(recipe);
                if (recipeItem != null) {
                    InteractiveItem interactiveItem = new InteractiveItem(recipeItem, slot)
                            .onClick((p, clickType) -> handleRecipeClick(p, recipe, clickType));
                    
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

        // Add close button
        addCloseButton(inventory);
    }

    private ItemStack createRecipeEditorItem(Recipe recipe) {
        ItemStack resultItem = CraftingManager.createResultItem(recipe);
        if (resultItem == null) return null;

        String status = recipe.isEnabled() ? "&aEnabled" : "&cDisabled";
        int requirementCount = recipe.getMaterialRequirements().size();

        // Apply placeholders to lore
        List<String> lore = config.getStringList("items.recipe_item.lore");
        List<String> processedLore = new ArrayList<>();
        
        for (String line : lore) {
            String processed = line
                    .replace("#recipe_id#", recipe.getId())
                    .replace("#recipe_name#", recipe.getName())
                    .replace("#category#", recipe.getCategory())
                    .replace("#status#", status)
                    .replace("#requirement_count#", String.valueOf(requirementCount))
                    .replace("#result_amount#", String.valueOf(recipe.getResultAmount()))
                    .replace("#result_name#", recipe.getResultName());
            processedLore.add(Chat.colorizewp(processed));
        }

        if (resultItem.getItemMeta() != null) {
            ItemMeta meta = resultItem.getItemMeta();
            meta.setLore(processedLore);
            resultItem.setItemMeta(meta);
        }
        return resultItem;
    }

    private void handleRecipeClick(Player player, Recipe recipe, ClickType clickType) {
        SoundManager.playItemSound(player, config, "items.recipe_item", SoundContext.SILENT);

        if (clickType == ClickType.LEFT) {
            // Edit recipe
            editRecipe(player, recipe);
        } else if (clickType == ClickType.RIGHT) {
            // Delete recipe
            deleteRecipe(player, recipe);
        } else if (clickType == ClickType.SHIFT_LEFT) {
            // Toggle enabled status
            toggleRecipeStatus(player, recipe);
        } else if (clickType == ClickType.SHIFT_RIGHT) {
            // Delete recipe (alternative)
            deleteRecipe(player, recipe);
        }
    }

    private void editRecipe(Player player, Recipe recipe) {
        SoundManager.setShouldPlayCloseSound(player, false);
        player.openInventory(new RecipeEditorGUI(player, recipe).getInventory(SoundContext.SILENT));
    }

    private void deleteRecipe(Player player, Recipe recipe) {
        CraftingManager.removeRecipe(recipe.getId());
        player.sendMessage(Chat.colorize(File.getMessage().getString("recipe.editor_deleted")
                .replace("#recipe#", recipe.getName())));
        
        SoundManager.setShouldPlayCloseSound(player, false);
        player.openInventory(new RecipeEditorListGUI(player, currentPage).getInventory(SoundContext.SILENT));
    }

    private void toggleRecipeStatus(Player player, Recipe recipe) {
        recipe.setEnabled(!recipe.isEnabled());
        CraftingManager.updateRecipe(recipe);

        String messageKey = recipe.isEnabled() ? "recipe.editor_enabled" : "recipe.editor_disabled";
        player.sendMessage(Chat.colorize(File.getMessage().getString(messageKey)
                .replace("#recipe#", recipe.getName())));

        SoundManager.setShouldPlayCloseSound(player, false);
        player.openInventory(new RecipeEditorListGUI(player, currentPage).getInventory(SoundContext.SILENT));
    }

    private void createNewRecipe(Player player) {
        String newRecipeId = CraftingManager.generateUniqueId();
        Recipe newRecipe = new Recipe(newRecipeId);
        newRecipe.setName("New Recipe");
        
        CraftingManager.addRecipe(newRecipe);
        player.sendMessage(Chat.colorize(File.getMessage().getString("recipe.editor_created")
                .replace("#recipe#", newRecipe.getName())));
        
        SoundManager.setShouldPlayCloseSound(player, false);
        player.openInventory(new RecipeEditorGUI(player, newRecipe).getInventory(SoundContext.SILENT));
    }

    private void addPreviousPageButton(Inventory inventory, int totalPages) {
        ItemStack prevItem = ItemManager.getItemConfigWithPlaceholders(player,
                Objects.requireNonNull(config.getConfigurationSection("items.previous_page")),
                "#current_page#", String.valueOf(currentPage + 1),
                "#total_pages#", String.valueOf(totalPages));
        
        InteractiveItem prevButton = new InteractiveItem(prevItem, 45)
                .onLeftClick(p -> {
                    SoundManager.setShouldPlayCloseSound(p, false);
                    p.openInventory(new RecipeEditorListGUI(p, currentPage - 1).getInventory(SoundContext.SILENT));
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
                    p.openInventory(new RecipeEditorListGUI(p, currentPage + 1).getInventory(SoundContext.SILENT));
                });
        inventory.setItem(nextButton.getSlot(), nextButton);
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
