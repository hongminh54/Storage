package net.danh.storage.GUI;

import net.danh.storage.GUI.manager.IGUI;
import net.danh.storage.GUI.manager.InteractiveItem;
import net.danh.storage.Manager.ItemManager;
import net.danh.storage.Manager.MythicStorageManager;
import net.danh.storage.Manager.SoundManager;
import net.danh.storage.MythicMobs.MythicMobsHelper;
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

public class ViewMythicStorageGUI implements IGUI {

    public static HashMap<Player, Integer> playerCurrentPage = new HashMap<>();
    private final Player viewer;
    private final Player target;
    private final String targetName;
    private final FileConfiguration config;
    private final int currentPage;

    public ViewMythicStorageGUI(Player viewer, Player target) {
        this(viewer, target, 0);
    }

    public ViewMythicStorageGUI(Player viewer, Player target, int page) {
        this.viewer = viewer;
        this.target = target;
        this.targetName = target.getName();
        this.currentPage = Math.max(0, page);
        this.config = File.getViewMythicStorageGUIConfig();
        playerCurrentPage.put(viewer, this.currentPage);
    }

    public ViewMythicStorageGUI(Player viewer, String targetName) {
        this(viewer, targetName, 0);
    }

    public ViewMythicStorageGUI(Player viewer, String targetName, int page) {
        this.viewer = viewer;
        this.target = null;
        this.targetName = targetName;
        this.currentPage = Math.max(0, page);
        this.config = File.getViewMythicStorageGUIConfig();
        playerCurrentPage.put(viewer, this.currentPage);
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
        SoundManager.playItemSound(viewer, config, "gui_open_sound", context);

        String title = ChatUtils.colorizewp(Objects.requireNonNull(config.getString("title")).replace("#player#", targetName));

        Inventory inventory = Bukkit.createInventory(this, config.getInt("size") * 9, title);

        List<String> configuredDrops = MythicStorageManager.getConfiguredDrops();
        int itemsPerPage = Objects.requireNonNull(config.getString("items.mythic_item.slot")).split(",").length;
        int totalPages = Math.max(1, (int) Math.ceil((double) configuredDrops.size() / itemsPerPage));
        boolean hasMultiplePages = totalPages > 1;

        Set<Integer> navigationSlots = new HashSet<>();
        if (hasMultiplePages) {
            if (config.contains("items.previous_page.slot")) {
                navigationSlots.add(Integer.parseInt(config.getString("items.previous_page.slot")));
            }
            if (config.contains("items.next_page.slot")) {
                navigationSlots.add(Integer.parseInt(config.getString("items.next_page.slot")));
            }
        }

        // Notify admin if there are invalid items
        if (MythicStorageManager.hasInvalidItems() && viewer.hasPermission("storage.mythicstorage.admin")) {
            viewer.sendMessage(ChatUtils.colorizewp("&c&l[!] MythicStorage Warning:"));
            viewer.sendMessage(ChatUtils.colorizewp("&e" + MythicStorageManager.getInvalidItems().size() + " &7invalid item(s) detected: &c" + String.join(", ", MythicStorageManager.getInvalidItems())));
            viewer.sendMessage(ChatUtils.colorizewp("&7Use &e/mythicstorage reload &7to see detailed errors"));
        }

        for (String itemTag : Objects.requireNonNull(config.getConfigurationSection("items")).getKeys(false)) {
            String slot = Objects.requireNonNull(config.getString("items." + itemTag + ".slot")).replace(" ", "");

            if (itemTag.equalsIgnoreCase("mythic_item")) {
                setupMythicItems(inventory, slot, configuredDrops, itemsPerPage);
            } else if (itemTag.equalsIgnoreCase("previous_page")) {
                setupPreviousPage(inventory, slot, hasMultiplePages, totalPages);
            } else if (itemTag.equalsIgnoreCase("next_page")) {
                setupNextPage(inventory, slot, hasMultiplePages, totalPages);
            } else if (itemTag.equalsIgnoreCase("view_info")) {
                setupViewInfo(inventory, slot);
            } else if (itemTag.equalsIgnoreCase("back_button")) {
                setupBackButton(inventory, slot);
            } else {
                setupDecorativeItems(inventory, slot, itemTag, hasMultiplePages, navigationSlots);
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
        return ItemManager.getItemConfigWithPlaceholders(viewer, Objects.requireNonNull(config.getConfigurationSection("items." + itemTag)), "#current_page#", String.valueOf(currentPage + 1), "#total_pages#", String.valueOf(totalPages));
    }

    private void setupMythicItems(Inventory inventory, String slot, List<String> configuredDrops, int itemsPerPage) {
        if (slot.contains(",")) {
            List<String> slotList = new ArrayList<>(Arrays.asList(slot.split(",")));
            int startIndex = currentPage * itemsPerPage;
            int endIndex = Math.min(startIndex + itemsPerPage, configuredDrops.size());

            MythicMobsHelper helper = MythicStorageManager.getMythicMobsHelper();
            if (helper == null || !helper.isInitialized()) {
                return;
            }

            for (int i = startIndex; i < endIndex; i++) {
                int slotIndex = i - startIndex;
                if (slotIndex < slotList.size()) {
                    String itemName = configuredDrops.get(i);
                    ItemStack mythicItem = helper.getMythicItem(itemName);

                    if (mythicItem != null) {
                        ItemStack displayItem = mythicItem.clone();
                        ItemMeta meta = displayItem.getItemMeta();

                        if (meta != null) {
                            int amount = MythicStorageManager.getPlayerItem(targetName, itemName);
                            int maxStorage = MythicStorageManager.getMaxStorage(targetName);

                            if (meta.hasDisplayName()) {
                                meta.setDisplayName(ChatUtils.colorizewp(meta.getDisplayName()));
                            }

                            List<String> lore = new ArrayList<>();
                            for (String line : config.getStringList("items.mythic_item.lore")) {
                                lore.add(ChatUtils.colorizewp(line.replace("#item_amount#", String.valueOf(amount)).replace("#max_storage#", String.valueOf(maxStorage))));
                            }

                            meta.setLore(lore);
                            displayItem.setItemMeta(meta);
                        }

                        InteractiveItem interactiveItem = new InteractiveItem(displayItem, Number.getInteger(slotList.get(slotIndex)));
                        inventory.setItem(interactiveItem.getSlot(), interactiveItem);
                    }
                }
            }
        }
    }

    private void setupPreviousPage(Inventory inventory, String slot, boolean hasMultiplePages, int totalPages) {
        if (hasMultiplePages && currentPage > 0) {
            ItemStack prevPageItem = getNavigationItem("previous_page", currentPage, totalPages);
            if (prevPageItem != null) {
                InteractiveItem item = new InteractiveItem(prevPageItem, Number.getInteger(slot)).onClick((player, clickType) -> {
                    SoundManager.playItemSound(player, config, "items.previous_page", SoundContext.INITIAL_OPEN);
                    SoundManager.setShouldPlayCloseSound(player, false);
                    player.openInventory(new ViewMythicStorageGUI(viewer, targetName, currentPage - 1).getInventory(SoundContext.SILENT));
                });
                inventory.setItem(item.getSlot(), item);
            }
        }
    }

    private void setupNextPage(Inventory inventory, String slot, boolean hasMultiplePages, int totalPages) {
        if (hasMultiplePages && currentPage < totalPages - 1) {
            ItemStack nextPageItem = getNavigationItem("next_page", currentPage, totalPages);
            if (nextPageItem != null) {
                InteractiveItem item = new InteractiveItem(nextPageItem, Number.getInteger(slot)).onClick((player, clickType) -> {
                    SoundManager.playItemSound(player, config, "items.next_page", SoundContext.INITIAL_OPEN);
                    SoundManager.setShouldPlayCloseSound(player, false);
                    player.openInventory(new ViewMythicStorageGUI(viewer, targetName, currentPage + 1).getInventory(SoundContext.SILENT));
                });
                inventory.setItem(item.getSlot(), item);
            }
        }
    }

    private void setupViewInfo(Inventory inventory, String slot) {
        ItemStack viewInfoItem = ItemManager.getItemConfigWithPlaceholders(viewer, Objects.requireNonNull(config.getConfigurationSection("items.view_info")), "#player#", targetName);

        InteractiveItem item = new InteractiveItem(viewInfoItem, Number.getInteger(slot));
        inventory.setItem(item.getSlot(), item);
    }

    private void setupBackButton(Inventory inventory, String slot) {
        ItemStack backItem = ItemManager.getItemConfig(Objects.requireNonNull(config.getConfigurationSection("items.back_button")));

        InteractiveItem item = new InteractiveItem(backItem, Number.getInteger(slot)).onClick((player, clickType) -> {
            SoundManager.playItemSound(player, config, "items.back_button", SoundContext.INITIAL_OPEN);
            SoundManager.setShouldPlayCloseSound(player, false);
            int viewerCurrentPage = MythicStorageGUI.getPlayerCurrentPage(viewer);
            player.openInventory(new MythicStorageGUI(viewer, viewerCurrentPage).getInventory(SoundContext.SILENT));
        });
        inventory.setItem(item.getSlot(), item);
    }

    private void setupDecorativeItems(Inventory inventory, String slot, String itemTag, boolean hasMultiplePages, Set<Integer> navigationSlots) {
        if (slot.contains(",")) {
            for (String slotString : slot.split(",")) {
                int slotNumber = Number.getInteger(slotString);
                if (hasMultiplePages && navigationSlots.contains(slotNumber)) {
                    continue;
                }
                InteractiveItem item = new InteractiveItem(ItemManager.getItemConfig(Objects.requireNonNull(config.getConfigurationSection("items." + itemTag))), slotNumber);
                inventory.setItem(item.getSlot(), item);
            }
        } else {
            int slotNumber = Number.getInteger(slot);
            if (!(hasMultiplePages && navigationSlots.contains(slotNumber))) {
                InteractiveItem item = new InteractiveItem(ItemManager.getItemConfig(Objects.requireNonNull(config.getConfigurationSection("items." + itemTag))), slotNumber);
                inventory.setItem(item.getSlot(), item);
            }
        }
    }
}
