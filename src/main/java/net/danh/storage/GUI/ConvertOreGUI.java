package net.danh.storage.GUI;

import net.danh.storage.Action.ConvertOre;
import net.danh.storage.GUI.manager.IGUI;
import net.danh.storage.GUI.manager.InteractiveItem;
import net.danh.storage.Manager.ConvertOreManager;
import net.danh.storage.Manager.ItemManager;
import net.danh.storage.Manager.MineManager;
import net.danh.storage.Manager.SoundManager;
import net.danh.storage.Utils.Chat;
import net.danh.storage.Utils.File;
import net.danh.storage.Utils.Number;
import net.danh.storage.Utils.SoundContext;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class ConvertOreGUI implements IGUI {

    public static HashMap<Player, Integer> playerCurrentPage = new HashMap<>();
    private final Player player;
    private final FileConfiguration config;
    private final int currentPage;

    public ConvertOreGUI(Player player) {
        this(player, 0);
    }

    public ConvertOreGUI(Player player, int page) {
        this.player = player;
        this.currentPage = Math.max(0, page);
        this.config = File.getConvertOreConfig();
        playerCurrentPage.put(player, this.currentPage);
    }

    public static int getPlayerCurrentPage(Player player) {
        return playerCurrentPage.getOrDefault(player, 0);
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
        Inventory inventory = Bukkit.createInventory(player, config.getInt("size") * 9, title);

        List<String> materialList = ConvertOreManager.getConvertibleMaterials();
        int itemsPerPage = Objects.requireNonNull(config.getString("items.material_item.slot")).split(",").length;
        int totalPages = Math.max(1, (int) Math.ceil((double) materialList.size() / itemsPerPage));
        boolean hasMultiplePages = totalPages > 1;

        for (String itemTag : Objects.requireNonNull(config.getConfigurationSection("items")).getKeys(false)) {
            String slot = Objects.requireNonNull(config.getString("items." + itemTag + ".slot")).replace(" ", "");

            if (itemTag.equalsIgnoreCase("material_item")) {
                setupMaterialItems(inventory, materialList, slot, itemsPerPage);
            } else if (itemTag.equalsIgnoreCase("previous_page") && hasMultiplePages && currentPage > 0) {
                setupNavigationItem(inventory, itemTag, slot, currentPage + 1, totalPages, true);
            } else if (itemTag.equalsIgnoreCase("next_page") && hasMultiplePages && currentPage < totalPages - 1) {
                setupNavigationItem(inventory, itemTag, slot, currentPage + 1, totalPages, false);
            } else if (itemTag.equalsIgnoreCase("decorates")) {
                setupDecorativeItems(inventory, itemTag, slot);
            } else if (!itemTag.equalsIgnoreCase("previous_page") && !itemTag.equalsIgnoreCase("next_page")) {
                setupStaticItem(inventory, itemTag, slot);
            }
        }

        return inventory;
    }

    private void setupMaterialItems(Inventory inventory, List<String> materialList, String slot, int itemsPerPage) {
        if (slot.contains(",")) {
            List<String> slotList = new ArrayList<>(Arrays.asList(slot.split(",")));
            int startIndex = currentPage * itemsPerPage;
            int endIndex = Math.min(startIndex + itemsPerPage, materialList.size());
            
            for (int i = startIndex; i < endIndex; i++) {
                int slotIndex = i - startIndex;
                if (slotIndex < slotList.size()) {
                    String material = materialList.get(i);
                    String materialName = File.getConfig().getString("items." + material, material.split(";")[0]);
                    int playerAmount = MineManager.getPlayerBlock(player, material);
                    
                    ItemStack itemStack = ItemManager.getItemConfigWithPlaceholders(player, material, materialName,
                        config.getConfigurationSection("items.material_item"),
                        "#material#", materialName,
                        "#amount#", String.valueOf(playerAmount));
                    
                    InteractiveItem interactiveItem = new InteractiveItem(itemStack, Number.getInteger(slotList.get(slotIndex)))
                        .onClick((clickPlayer, clickType) -> {
                            SoundManager.playItemSound(clickPlayer, config, "items.material_item", SoundContext.INITIAL_OPEN);
                            clickPlayer.openInventory(new ConvertOptionGUI(clickPlayer, material, currentPage).getInventory(SoundContext.SILENT));
                        });
                    inventory.setItem(interactiveItem.getSlot(), interactiveItem);
                }
            }
        }
    }

    private void setupNavigationItem(Inventory inventory, String itemTag, String slot, int currentPageDisplay, int totalPages, boolean isPrevious) {
        ConfigurationSection section = config.getConfigurationSection("items." + itemTag);
        if (section != null) {
            ItemStack itemStack = ItemManager.getItemConfigWithPlaceholders(player, section,
                "#current_page#", String.valueOf(currentPageDisplay),
                "#total_pages#", String.valueOf(totalPages));
            
            InteractiveItem interactiveItem = new InteractiveItem(itemStack, Number.getInteger(slot))
                .onClick((clickPlayer, clickType) -> {
                    SoundManager.playItemSound(clickPlayer, config, "items." + itemTag, SoundContext.INITIAL_OPEN);
                    int newPage = isPrevious ? currentPage - 1 : currentPage + 1;
                    clickPlayer.openInventory(new ConvertOreGUI(clickPlayer, newPage).getInventory(SoundContext.SILENT));
                });
            inventory.setItem(interactiveItem.getSlot(), interactiveItem);
        }
    }

    private void setupDecorativeItems(Inventory inventory, String itemTag, String slot) {
        ConfigurationSection section = config.getConfigurationSection("items." + itemTag);
        if (section != null) {
            if (slot.contains(",")) {
                String[] slots = slot.split(",");
                for (String slotString : slots) {
                    ItemStack itemStack = ItemManager.getItemConfig(player, section);
                    InteractiveItem interactiveItem = new InteractiveItem(itemStack, Number.getInteger(slotString.trim()));
                    inventory.setItem(interactiveItem.getSlot(), interactiveItem);
                }
            } else {
                ItemStack itemStack = ItemManager.getItemConfig(player, section);
                InteractiveItem interactiveItem = new InteractiveItem(itemStack, Number.getInteger(slot));
                inventory.setItem(interactiveItem.getSlot(), interactiveItem);
            }
        }
    }

    private void setupStaticItem(Inventory inventory, String itemTag, String slot) {
        ConfigurationSection section = config.getConfigurationSection("items." + itemTag);
        if (section != null) {
            ItemStack itemStack = ItemManager.getItemConfig(player, section);

            InteractiveItem interactiveItem = new InteractiveItem(itemStack, Number.getInteger(slot))
                .onClick((clickPlayer, clickType) -> {
                    if (itemTag.equalsIgnoreCase("back")) {
                        SoundManager.playItemSound(clickPlayer, config, "items." + itemTag, SoundContext.INITIAL_OPEN);
                        clickPlayer.openInventory(new PersonalStorage(clickPlayer, PersonalStorage.getPlayerCurrentPage(clickPlayer)).getInventory(SoundContext.SILENT));
                    }
                });
            inventory.setItem(interactiveItem.getSlot(), interactiveItem);
        }
    }
}
