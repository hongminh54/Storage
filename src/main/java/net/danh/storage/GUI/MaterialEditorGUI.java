package net.danh.storage.GUI;

import net.danh.storage.GUI.Crafting.RecipeEditorGUI;
import net.danh.storage.GUI.Mythic.MythicMaterialSelectionGUI;
import net.danh.storage.GUI.manager.IGUI;
import net.danh.storage.GUI.manager.InteractiveItem;
import net.danh.storage.Manager.Crafting.CraftingManager;
import net.danh.storage.Manager.Crafting.RecipeEditManager;
import net.danh.storage.Manager.ItemManager;
import net.danh.storage.Manager.MineManager;
import net.danh.storage.Manager.Mythic.MythicStorageManager;
import net.danh.storage.Manager.SoundManager;
import net.danh.storage.MythicMobs.MythicMobsHelper;
import net.danh.storage.Recipe.Recipe;
import net.danh.storage.Utils.*;
import net.danh.storage.Utils.Number;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

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

        String title = ChatUtils.colorizewp(player, Objects.requireNonNull(
                config.getString("title")).replace("#recipe_name#",
                recipe.getName()));
        Inventory inventory = Bukkit.createInventory(this, config.getInt("size") * 9, title);

        setupItems(inventory);
        return inventory;
    }

    private void setupItems(Inventory inventory) {
        setupDecorativeItems(inventory);
        addTitleItem(inventory);
        addAddMaterialButton(inventory);
        addAddMythicMaterialButton(inventory);
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
            String slotConfig = config.getString("items.title.slot", "4");
            if (slotConfig.contains(",")) {
                for (String slotStr : slotConfig.split(",")) {
                    int slot = Number.getInteger(slotStr.trim());
                    InteractiveItem title = new InteractiveItem(titleItem.clone(), slot);
                    inventory.setItem(title.getSlot(), title);
                }
            } else {
                int slot = Number.getInteger(slotConfig);
                InteractiveItem title = new InteractiveItem(titleItem, slot);
                inventory.setItem(title.getSlot(), title);
            }
        }
    }

    private void addAddMaterialButton(Inventory inventory) {
        int materialCount = recipe.getMaterialRequirements().size();
        String slotConfig = config.getString("items.add_material.slot", "40");

        if (materialCount >= MAX_MATERIALS) {
            ItemStack fullItem = ItemManager.getItemConfig(config.getConfigurationSection("items.material_full"));
            if (fullItem != null) {
                if (slotConfig.contains(",")) {
                    for (String slotStr : slotConfig.split(",")) {
                        int slot = Number.getInteger(slotStr.trim());
                        InteractiveItem fullButton = new InteractiveItem(fullItem.clone(), slot);
                        inventory.setItem(fullButton.getSlot(), fullButton);
                    }
                } else {
                    int slot = Number.getInteger(slotConfig);
                    InteractiveItem fullButton = new InteractiveItem(fullItem, slot);
                    inventory.setItem(fullButton.getSlot(), fullButton);
                }
            }
        } else {
            ItemStack addItem = ItemManager.getItemConfig(Objects.requireNonNull(config.getConfigurationSection("items.add_material")));
            if (slotConfig.contains(",")) {
                for (String slotStr : slotConfig.split(",")) {
                    int slot = Number.getInteger(slotStr.trim());
                    InteractiveItem addButton = new InteractiveItem(addItem.clone(), slot)
                            .onLeftClick(p -> openMaterialSelection(p));
                    inventory.setItem(addButton.getSlot(), addButton);
                }
            } else {
                int slot = Number.getInteger(slotConfig);
                InteractiveItem addButton = new InteractiveItem(addItem, slot)
                        .onLeftClick(p -> openMaterialSelection(p));
                inventory.setItem(addButton.getSlot(), addButton);
            }
        }
    }

    private void addAddMythicMaterialButton(Inventory inventory) {
        String slotConfig = config.getString("items.add_mythic_material.slot", "51");
        int materialCount = recipe.getMaterialRequirements().size();

        ItemStack item;
        boolean canUse = MythicStorageManager.isSystemEnabled()
                && materialCount < MAX_MATERIALS;

        if (canUse) {
            item = ItemManager.getItemConfig(Objects.requireNonNull(
                    config.getConfigurationSection("items.add_mythic_material")));
        } else {
            item = ItemManager.getItemConfig(
                    config.getConfigurationSection("items.add_mythic_material_disabled"));
        }

        if (item == null) {
            return;
        }

        if (slotConfig.contains(",")) {
            for (String slotStr : slotConfig.split(",")) {
                int slot = Number.getInteger(slotStr.trim());
                InteractiveItem button = new InteractiveItem(item.clone(), slot);
                if (canUse) {
                    button.onLeftClick(this::openMythicMaterialSelection);
                }
                inventory.setItem(button.getSlot(), button);
            }
        } else {
            int slot = Number.getInteger(slotConfig);
            InteractiveItem button = new InteractiveItem(item, slot);
            if (canUse) {
                button.onLeftClick(this::openMythicMaterialSelection);
            }
            inventory.setItem(button.getSlot(), button);
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
        String normalized = MineManager.normalizeMaterial(materialName);
        String mythicId = getMythicItemId(normalized);
        if (mythicId != null) {
            return createMythicMaterialItem(mythicId, normalized, amount);
        }

        String displayMaterial = normalized;
        if (normalized.contains(";")) {
            displayMaterial = normalized.split(";", 2)[0];
        }

        String displayName = File.getConfig().getString("items." + normalized,
                displayMaterial);

        ItemStack item = MaterialUtils.createItem(displayMaterial);
        if (item == null) {
            item = new ItemStack(Material.STONE);
        }
        if (item == null) {
            return null;
        }

        applyMaterialMeta(item, displayName, amount);
        return item;
    }

    private ItemStack createMythicMaterialItem(String mythicId,
                                               String normalizedKey,
                                               int amount) {
        String displayName = MythicStorageManager.getItemDisplayNameOrId(
                mythicId, player);

        ItemStack item = null;
        if (MythicStorageManager.isSystemEnabled()) {
            MythicMobsHelper helper = MythicStorageManager.getMythicMobsHelper();
            if (helper != null && helper.isInitialized()) {
                item = helper.getMythicItem(mythicId);
            }
        }

        if (item == null) {
            item = new ItemStack(Material.STONE);
            if (item == null) {
                return null;
            }
        } else {
            item = item.clone();
        }

        String configName = File.getConfig().getString("items." + normalizedKey,
                displayName);
        applyMaterialMeta(item, configName, amount);
        return item;
    }

    private void applyMaterialMeta(ItemStack item, String displayName,
                                   int amount) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return;
        }

        meta.setDisplayName(ChatUtils.colorizewp(config.getString(
                        "items.material_slot.name", "&e#material#")
                .replace("#material#", displayName)));

        List<String> lore = new ArrayList<>();
        for (String line : config.getStringList("items.material_slot.lore")) {
            lore.add(ChatUtils.colorizewp(line.replace("#amount#",
                    String.valueOf(amount))));
        }
        meta.setLore(lore);
        item.setItemMeta(meta);
        item.setAmount(Math.min(64, Math.max(1, amount)));
    }

    private String getMythicItemId(String normalizedKey) {
        if (normalizedKey == null || normalizedKey.isEmpty()) {
            return null;
        }
        if (!normalizedKey.startsWith("mythic;")) {
            return null;
        }
        String[] parts = normalizedKey.split(";", 3);
        if (parts.length < 2) {
            return null;
        }
        String id = parts[1];
        if (id == null || id.trim().isEmpty()) {
            return null;
        }
        return id.trim();
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
            RecipeEditorGUI.updateBackup(player.getUniqueId(), recipe);
            refreshGUI(player);
        } else if (clickType == ClickType.RIGHT) {
            if (currentAmount > 1) {
                requirements.put(material, currentAmount - 1);
                CraftingManager.updateRecipe(recipe);
                RecipeEditorGUI.updateBackup(player.getUniqueId(), recipe);
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
            RecipeEditorGUI.updateBackup(player.getUniqueId(), recipe);
            refreshGUI(player);
        }
    }

    private void openMaterialSelection(Player player) {
        SoundManager.setShouldPlayCloseSound(player, false);
        player.openInventory(new MaterialSelectionGUI(player, recipe, "requirement").getInventory(SoundContext.SILENT));
    }

    private void openMythicMaterialSelection(Player player) {
        SoundManager.setShouldPlayCloseSound(player, false);
        player.openInventory(new MythicMaterialSelectionGUI(player, recipe)
                .getInventory(SoundContext.SILENT));
    }

    private void addControlButtons(Inventory inventory) {
        // Back button
        ItemStack backItem = ItemManager.getItemConfig(Objects.requireNonNull(config.getConfigurationSection("items.back")));
        String backSlotConfig = config.getString("items.back.slot", "49");
        if (backSlotConfig.contains(",")) {
            for (String slotStr : backSlotConfig.split(",")) {
                int slot = Number.getInteger(slotStr.trim());
                InteractiveItem backButton = new InteractiveItem(backItem.clone(), slot)
                        .onLeftClick(p -> backToEditor(p));
                inventory.setItem(backButton.getSlot(), backButton);
            }
        } else {
            int slot = Number.getInteger(backSlotConfig);
            InteractiveItem backButton = new InteractiveItem(backItem, slot)
                    .onLeftClick(p -> backToEditor(p));
            inventory.setItem(backButton.getSlot(), backButton);
        }

        // Clear all button
        if (recipe.getMaterialRequirements().size() > 0) {
            ItemStack clearItem = ItemManager.getItemConfig(Objects.requireNonNull(config.getConfigurationSection("items.clear_all")));
            String clearSlotConfig = config.getString("items.clear_all.slot", "48");
            if (clearSlotConfig.contains(",")) {
                for (String slotStr : clearSlotConfig.split(",")) {
                    int slot = Number.getInteger(slotStr.trim());
                    InteractiveItem clearButton = new InteractiveItem(clearItem.clone(), slot)
                            .onLeftClick(p -> clearAllMaterials(p));
                    inventory.setItem(clearButton.getSlot(), clearButton);
                }
            } else {
                int slot = Number.getInteger(clearSlotConfig);
                InteractiveItem clearButton = new InteractiveItem(clearItem, slot)
                        .onLeftClick(p -> clearAllMaterials(p));
                inventory.setItem(clearButton.getSlot(), clearButton);
            }
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
                    RecipeEditorGUI.updateBackup(player.getUniqueId(), recipe);
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
