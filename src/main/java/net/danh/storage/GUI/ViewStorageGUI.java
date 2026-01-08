package net.danh.storage.GUI;

import net.danh.storage.GUI.manager.IGUI;
import net.danh.storage.GUI.manager.InteractiveItem;
import net.danh.storage.Manager.ItemManager;
import net.danh.storage.Manager.MineManager;
import net.danh.storage.Manager.SoundManager;
import net.danh.storage.Utils.ChatUtils;
import net.danh.storage.Utils.File;
import net.danh.storage.Utils.Number;
import net.danh.storage.Utils.SoundContext;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class ViewStorageGUI implements IGUI {

    public static HashMap<UUID, Integer> playerCurrentPage = new HashMap<>();
    private final Player viewer; // Player viewing the storage
    private final Player target;  // Player whose storage is being viewed
    private final String targetName;
    private final FileConfiguration config;
    private final int currentPage;

    public ViewStorageGUI(Player viewer, Player target) {
        this(viewer, target, 0);
    }

    public ViewStorageGUI(Player viewer, Player target, int page) {
        this.viewer = viewer;
        this.target = target;
        this.targetName = target.getName();
        this.currentPage = Math.max(0, page);
        config = File.getViewStorageConfig();
        playerCurrentPage.put(viewer.getUniqueId(), this.currentPage);
    }

    public ViewStorageGUI(Player viewer, String targetName) {
        this(viewer, targetName, 0);
    }

    public ViewStorageGUI(Player viewer, String targetName, int page) {
        this.viewer = viewer;
        this.target = null;
        this.targetName = targetName;
        this.currentPage = Math.max(0, page);
        config = File.getViewStorageConfig();
        playerCurrentPage.put(viewer.getUniqueId(), this.currentPage);
    }

    public static int getPlayerCurrentPage(Player player) {
        if (player == null) return 0;
        return playerCurrentPage.getOrDefault(player.getUniqueId(), 0);
    }

    @NotNull
    @Override
    public Inventory getInventory() {
        return getInventory(SoundContext.INITIAL_OPEN);
    }

    @NotNull
    @Override
    public Inventory getInventory(SoundContext context) {
        SoundManager.playItemSound(viewer, config, "gui_open_sound", context);

        // Create inventory with target player's name
        String title = ChatUtils.colorizewp(viewer, Objects.requireNonNull(
                config.getString("title")).replace("#player#", targetName));

        Inventory inventory = Bukkit.createInventory(this, config.getInt("size") * 9, title);

        List<String> item_list = new ArrayList<>(MineManager.getOrderedPluginBlocks());
        int itemsPerPage = Objects.requireNonNull(config.getString("items.storage_item.slot")).split(",").length;
        int totalPages = Math.max(1, (int) Math.ceil((double) item_list.size() / itemsPerPage));
        boolean hasMultiplePages = totalPages > 1;

        Set<Integer> navigationSlots = new HashSet<>();
        if (hasMultiplePages) {
            if (config.contains("items.previous_page.slot")) {
                String prevSlot = config.getString("items.previous_page.slot");
                if (prevSlot != null) {
                    for (String s : prevSlot.split(",")) {
                        navigationSlots.add(Number.getInteger(s.trim()));
                    }
                }
            }
            if (config.contains("items.next_page.slot")) {
                String nextSlot = config.getString("items.next_page.slot");
                if (nextSlot != null) {
                    for (String s : nextSlot.split(",")) {
                        navigationSlots.add(Number.getInteger(s.trim()));
                    }
                }
            }
        }

        for (String item_tag : Objects.requireNonNull(config.getConfigurationSection("items")).getKeys(false)) {
            String slot = Objects.requireNonNull(config.getString("items." + item_tag + ".slot")).replace(" ", "");

            if (item_tag.equalsIgnoreCase("storage_item")) {
                setupStorageItems(inventory, slot, item_list, itemsPerPage);
            } else if (item_tag.equalsIgnoreCase("previous_page")) {
                setupPreviousPage(inventory, slot, hasMultiplePages, totalPages);
            } else if (item_tag.equalsIgnoreCase("next_page")) {
                setupNextPage(inventory, slot, hasMultiplePages, totalPages);
            } else if (item_tag.equalsIgnoreCase("view_info")) {
                setupViewInfo(inventory, slot);
            } else if (item_tag.equalsIgnoreCase("back_button")) {
                setupBackButton(inventory, slot);
            } else {
                setupDecorativeItems(inventory, slot, item_tag, hasMultiplePages, navigationSlots);
            }
        }
        return inventory;
    }

    public Player getTarget() {
        return target;
    }

    public String getTargetName() {
        return targetName;
    }

    public FileConfiguration getConfig() {
        return config;
    }

    public int getCurrentPage() {
        return currentPage;
    }

    private ItemStack getNavigationItem(String itemTag, int currentPage, int totalPages) {
        return ItemManager.getItemConfigWithPlaceholders(viewer,
                Objects.requireNonNull(config.getConfigurationSection("items." + itemTag)),
                "#current_page#", String.valueOf(currentPage + 1),
                "#total_pages#", String.valueOf(totalPages));
    }

    private void setupStorageItems(Inventory inventory, String slot, List<String> item_list, int itemsPerPage) {
        if (slot.contains(",")) {
            List<String> slot_list = new ArrayList<>(Arrays.asList(slot.split(",")));
            int startIndex = currentPage * itemsPerPage;
            int endIndex = Math.min(startIndex + itemsPerPage, item_list.size());

            for (int i = startIndex; i < endIndex; i++) {
                int slotIndex = i - startIndex;
                if (slotIndex < slot_list.size()) {
                    String material = MineManager.getMaterial(item_list.get(i));
                    String name = File.getConfig().getString("items." + item_list.get(i));

                    // Use target player's data for item amount
                    ItemStack itemStack = ItemManager.getItemConfig(targetName, material,
                            name != null ? name : item_list.get(i).split(";")[0],
                            config.getConfigurationSection("items.storage_item"));

                    // Make item read-only - no click action
                    InteractiveItem interactiveItem = new InteractiveItem(itemStack,
                            Number.getInteger(slot_list.get(slotIndex)));
                    inventory.setItem(interactiveItem.getSlot(), interactiveItem);
                }
            }
        }
    }

    private void setupPreviousPage(Inventory inventory, String slot, boolean hasMultiplePages, int totalPages) {
        if (hasMultiplePages && currentPage > 0) {
            ItemStack prevPageItem = getNavigationItem("previous_page", currentPage, totalPages);
            if (prevPageItem != null) {
                if (slot.contains(",")) {
                    for (String slotString : slot.split(",")) {
                        InteractiveItem item = new InteractiveItem(prevPageItem.clone(), Number.getInteger(slotString.trim()))
                                .onClick((player, clickType) -> {
                                    SoundManager.playItemSound(player, config, "items.previous_page", SoundContext.INITIAL_OPEN);
                                    SoundManager.setShouldPlayCloseSound(player, false);
                                    player.openInventory(new ViewStorageGUI(viewer, targetName, currentPage - 1)
                                            .getInventory(SoundContext.SILENT));
                                });
                        inventory.setItem(item.getSlot(), item);
                    }
                } else {
                    InteractiveItem item = new InteractiveItem(prevPageItem, Number.getInteger(slot))
                            .onClick((player, clickType) -> {
                                SoundManager.playItemSound(player, config, "items.previous_page", SoundContext.INITIAL_OPEN);
                                SoundManager.setShouldPlayCloseSound(player, false);
                                player.openInventory(new ViewStorageGUI(viewer, targetName, currentPage - 1)
                                        .getInventory(SoundContext.SILENT));
                            });
                    inventory.setItem(item.getSlot(), item);
                }
            }
        }
    }

    private void setupNextPage(Inventory inventory, String slot, boolean hasMultiplePages, int totalPages) {
        if (hasMultiplePages && currentPage < totalPages - 1) {
            ItemStack nextPageItem = getNavigationItem("next_page", currentPage, totalPages);
            if (nextPageItem != null) {
                if (slot.contains(",")) {
                    for (String slotString : slot.split(",")) {
                        InteractiveItem item = new InteractiveItem(nextPageItem.clone(), Number.getInteger(slotString.trim()))
                                .onClick((player, clickType) -> {
                                    SoundManager.playItemSound(player, config, "items.next_page", SoundContext.INITIAL_OPEN);
                                    SoundManager.setShouldPlayCloseSound(player, false);
                                    player.openInventory(new ViewStorageGUI(viewer, targetName, currentPage + 1)
                                            .getInventory(SoundContext.SILENT));
                                });
                        inventory.setItem(item.getSlot(), item);
                    }
                } else {
                    InteractiveItem item = new InteractiveItem(nextPageItem, Number.getInteger(slot))
                            .onClick((player, clickType) -> {
                                SoundManager.playItemSound(player, config, "items.next_page", SoundContext.INITIAL_OPEN);
                                SoundManager.setShouldPlayCloseSound(player, false);
                                player.openInventory(new ViewStorageGUI(viewer, targetName, currentPage + 1)
                                        .getInventory(SoundContext.SILENT));
                            });
                    inventory.setItem(item.getSlot(), item);
                }
            }
        }
    }

    private void setupViewInfo(Inventory inventory, String slot) {
        ItemStack viewInfoItem = ItemManager.getItemConfigWithPlaceholders(viewer,
                Objects.requireNonNull(config.getConfigurationSection("items.view_info")),
                "#player#", targetName);

        if (slot.contains(",")) {
            for (String slotString : slot.split(",")) {
                InteractiveItem item = new InteractiveItem(viewInfoItem.clone(), Number.getInteger(slotString.trim()));
                inventory.setItem(item.getSlot(), item);
            }
        } else {
            InteractiveItem item = new InteractiveItem(viewInfoItem, Number.getInteger(slot));
            inventory.setItem(item.getSlot(), item);
        }
    }

    private void setupBackButton(Inventory inventory, String slot) {
        ItemStack backItem = ItemManager.getItemConfig(
                Objects.requireNonNull(config.getConfigurationSection("items.back_button")));

        if (slot.contains(",")) {
            for (String slotString : slot.split(",")) {
                InteractiveItem item = new InteractiveItem(backItem.clone(), Number.getInteger(slotString.trim()))
                        .onClick((player, clickType) -> {
                            SoundManager.playItemSound(player, config, "items.back_button", SoundContext.INITIAL_OPEN);
                            SoundManager.setShouldPlayCloseSound(player, false);
                            int viewerCurrentPage = PersonalStorage.getPlayerCurrentPage(viewer);
                            player.openInventory(new PersonalStorage(viewer, viewerCurrentPage).getInventory(SoundContext.SILENT));
                        });
                inventory.setItem(item.getSlot(), item);
            }
        } else {
            InteractiveItem item = new InteractiveItem(backItem, Number.getInteger(slot))
                    .onClick((player, clickType) -> {
                        SoundManager.playItemSound(player, config, "items.back_button", SoundContext.INITIAL_OPEN);
                        SoundManager.setShouldPlayCloseSound(player, false);
                        int viewerCurrentPage = PersonalStorage.getPlayerCurrentPage(viewer);
                        player.openInventory(new PersonalStorage(viewer, viewerCurrentPage).getInventory(SoundContext.SILENT));
                    });
            inventory.setItem(item.getSlot(), item);
        }
    }

    private void setupDecorativeItems(Inventory inventory, String slot, String item_tag,
                                      boolean hasMultiplePages, Set<Integer> navigationSlots) {
        if (slot.contains(",")) {
            for (String slot_string : slot.split(",")) {
                int slotNumber = Number.getInteger(slot_string);
                if (hasMultiplePages && navigationSlots.contains(slotNumber)) {
                    continue;
                }
                InteractiveItem item = new InteractiveItem(
                        ItemManager.getItemConfig(Objects.requireNonNull(
                                config.getConfigurationSection("items." + item_tag))), slotNumber);
                inventory.setItem(item.getSlot(), item);
            }
        } else {
            int slotNumber = Number.getInteger(slot);
            if (!(hasMultiplePages && navigationSlots.contains(slotNumber))) {
                InteractiveItem item = new InteractiveItem(
                        ItemManager.getItemConfig(Objects.requireNonNull(
                                config.getConfigurationSection("items." + item_tag))), slotNumber);
                inventory.setItem(item.getSlot(), item);
            }
        }
    }
}
