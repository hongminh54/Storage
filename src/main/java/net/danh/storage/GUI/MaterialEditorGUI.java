package net.danh.storage.GUI;

import com.cryptomorin.xseries.XMaterial;
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
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class MaterialEditorGUI implements IGUI {

    private static final int MAX_MATERIALS = 10;
    private final Player player;
    private final Recipe recipe;
    private final FileConfiguration config;

    public MaterialEditorGUI(Player player, Recipe recipe) {
        this.player = player;
        this.recipe = recipe;
        this.config = File.getMaterialEditorGUIConfig();
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
        addTitleItem(inventory);
        addAddMaterialButton(inventory);
        displayMaterials(inventory);
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

    private void addTitleItem(Inventory inventory) {
        int materialCount = recipe.getMaterialRequirements().size();
        ItemStack titleItem = ItemManager.getItemConfigWithPlaceholders(player,
                config.getConfigurationSection("items.title"),
                "#material_count#", String.valueOf(materialCount),
                "#max_materials#", String.valueOf(MAX_MATERIALS));

        if (titleItem != null) {
            InteractiveItem title = new InteractiveItem(titleItem, config.getInt("items.title.slot", 4));
            inventory.setItem(title.getSlot(), title);
        }
    }

    private void addAddMaterialButton(Inventory inventory) {
        int materialCount = recipe.getMaterialRequirements().size();

        if (materialCount >= MAX_MATERIALS) {
            ItemStack fullItem = ItemManager.getItemConfig(config.getConfigurationSection("items.material_full"));
            if (fullItem != null) {
                InteractiveItem fullButton = new InteractiveItem(fullItem, config.getInt("items.add_material.slot", 40));
                inventory.setItem(fullButton.getSlot(), fullButton);
            }
        } else {
            InteractiveItem addButton = new InteractiveItem(
                    ItemManager.getItemConfig(Objects.requireNonNull(config.getConfigurationSection("items.add_material"))),
                    config.getInt("items.add_material.slot", 40)
            ).onLeftClick(p -> openMaterialSelection(p));
            inventory.setItem(addButton.getSlot(), addButton);
        }
    }

    private void displayMaterials(Inventory inventory) {
        String materialSlots = config.getString("items.material_slot.slot");
        if (materialSlots == null) return;

        String[] slotArray = materialSlots.split(",");
        List<Map.Entry<String, Integer>> materials = new ArrayList<>(recipe.getMaterialRequirements().entrySet());

        for (int i = 0; i < Math.min(materials.size(), slotArray.length); i++) {
            Map.Entry<String, Integer> material = materials.get(i);
            try {
                int slot = Integer.parseInt(slotArray[i].trim());
                ItemStack materialItem = createMaterialItem(material.getKey(), material.getValue());

                if (materialItem != null) {
                    InteractiveItem interactiveItem = new InteractiveItem(materialItem, slot)
                            .onClick((p, clickType) -> handleMaterialClick(p, material.getKey(), clickType));
                    inventory.setItem(interactiveItem.getSlot(), interactiveItem);
                }
            } catch (NumberFormatException e) {
                // Skip invalid slot
            }
        }
    }

    private ItemStack createMaterialItem(String materialName, int amount) {
        String displayMaterial = materialName;
        if (materialName.contains(";")) {
            displayMaterial = materialName.split(";")[0];
        }

        String displayName = File.getConfig().getString("items." + materialName, displayMaterial);

        Optional<XMaterial> xMaterialOpt = XMaterial.matchXMaterial(displayMaterial);
        if (!xMaterialOpt.isPresent()) {
            xMaterialOpt = Optional.of(XMaterial.STONE);
        }

        ItemStack item = xMaterialOpt.get().parseItem();
        if (item == null) return null;

        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatUtils.colorizewp(config.getString("items.material_slot.name", "&e#material#")
                    .replace("#material#", displayName)));

            List<String> lore = new ArrayList<>();
            for (String line : config.getStringList("items.material_slot.lore")) {
                lore.add(ChatUtils.colorizewp(line.replace("#amount#", String.valueOf(amount))));
            }
            meta.setLore(lore);
            item.setItemMeta(meta);
            item.setAmount(Math.min(64, Math.max(1, amount)));
        }
        return item;
    }

    private void handleMaterialClick(Player player, String material, ClickType clickType) {
        Map<String, Integer> requirements = recipe.getMaterialRequirements();
        int currentAmount = requirements.getOrDefault(material, 1);

        String displayName = File.getConfig().getString("items." + material, material);
        if (material.contains(";")) {
            displayName = File.getConfig().getString("items." + material, material.split(";")[0]);
        }

        if (clickType == ClickType.LEFT) {
            requirements.put(material, currentAmount + 1);
            CraftingManager.updateRecipe(recipe);
            refreshGUI(player);
        } else if (clickType == ClickType.RIGHT) {
            if (currentAmount > 1) {
                requirements.put(material, currentAmount - 1);
                CraftingManager.updateRecipe(recipe);
                refreshGUI(player);
            } else {
                player.sendMessage(ChatUtils.colorize(
                        File.getMessage().getString("crafting.material_amount_min")));
                SoundManager.playSound(player, SoundManager.SoundType.ACTION_ERROR);
            }
        } else if (clickType == ClickType.SHIFT_LEFT) {
            RecipeEditManager.requestRequirementAmountEdit(player, recipe, material);
            SoundManager.setShouldPlayCloseSound(player, false);
        } else if (clickType == ClickType.SHIFT_RIGHT) {
            RecipeEditManager.removeRequirement(recipe, material);
            CraftingManager.updateRecipe(recipe);
            refreshGUI(player);
        }
    }

    private void openMaterialSelection(Player player) {
        SoundManager.setShouldPlayCloseSound(player, false);
        player.openInventory(new MaterialSelectionGUI(player, recipe, "requirement").getInventory(SoundContext.SILENT));
    }

    private void addControlButtons(Inventory inventory) {
        // Back button
        InteractiveItem backButton = new InteractiveItem(
                ItemManager.getItemConfig(Objects.requireNonNull(config.getConfigurationSection("items.back"))),
                config.getInt("items.back.slot", 49)
        ).onLeftClick(p -> backToEditor(p));
        inventory.setItem(backButton.getSlot(), backButton);

        // Clear all button
        if (recipe.getMaterialRequirements().size() > 0) {
            InteractiveItem clearButton = new InteractiveItem(
                    ItemManager.getItemConfig(Objects.requireNonNull(config.getConfigurationSection("items.clear_all"))),
                    config.getInt("items.clear_all.slot", 48)
            ).onLeftClick(p -> clearAllMaterials(p));
            inventory.setItem(clearButton.getSlot(), clearButton);
        }
    }

    private void clearAllMaterials(Player player) {
        String message = "Clear all materials?";

        SoundManager.setShouldPlayCloseSound(player, false);
        player.openInventory(new ConfirmationGUI(player, message,
                () -> {
                    // On confirm
                    recipe.getMaterialRequirements().clear();
                    CraftingManager.updateRecipe(recipe);
                    player.sendMessage(ChatUtils.colorize(
                            File.getMessage().getString("crafting.materials_cleared")));
                    SoundManager.setShouldPlayCloseSound(player, false);
                    player.openInventory(new MaterialEditorGUI(player, recipe).getInventory(SoundContext.SILENT));
                },
                () -> {
                    // On cancel
                    SoundManager.setShouldPlayCloseSound(player, false);
                    player.openInventory(new MaterialEditorGUI(player, recipe).getInventory(SoundContext.SILENT));
                }
        ).getInventory(SoundContext.SILENT));
    }

    private void backToEditor(Player player) {
        SoundManager.setShouldPlayCloseSound(player, false);
        player.openInventory(new RecipeEditorGUI(player, recipe).getInventory(SoundContext.SILENT));
    }

    private void refreshGUI(Player player) {
        SoundManager.setShouldPlayCloseSound(player, false);
        player.openInventory(new MaterialEditorGUI(player, recipe).getInventory(SoundContext.SILENT));
    }
}
