package net.danh.storage.GUI;

import net.danh.storage.GUI.manager.IGUI;
import net.danh.storage.GUI.manager.InteractiveItem;
import net.danh.storage.Manager.ItemManager;
import net.danh.storage.Manager.MineManager;
import net.danh.storage.Manager.SoundManager;
import net.danh.storage.Storage;
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

public class PersonalStorage implements IGUI {

    public static HashMap<Player, Integer> playerCurrentPage = new HashMap<>();
    private final Player p;
    private final FileConfiguration config;
    private final int currentPage;

    public PersonalStorage(Player p) {
        this(p, 0);
    }

    public PersonalStorage(Player p, int page) {
        this.p = p;
        this.currentPage = Math.max(0, page);
        config = File.getGUIStorage();
        playerCurrentPage.put(p, this.currentPage);
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
        SoundManager.playItemSound(p, config, "gui_open_sound", context);
        Inventory inventory = Bukkit.createInventory(this, config.getInt("size") * 9, Chat.colorizewp(Objects.requireNonNull(config.getString("title")).replace("#player#", p.getName())));
        List<String> item_list = new ArrayList<>(MineManager.getOrderedPluginBlocks());
        int itemsPerPage = Objects.requireNonNull(config.getString("items.storage_item.slot")).split(",").length;
        int totalPages = Math.max(1, (int) Math.ceil((double) item_list.size() / itemsPerPage));
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

        for (String item_tag : Objects.requireNonNull(config.getConfigurationSection("items")).getKeys(false)) {
            String slot = Objects.requireNonNull(config.getString("items." + item_tag + ".slot")).replace(" ", "");
            if (item_tag.equalsIgnoreCase("storage_item")) {
                if (slot.contains(",")) {
                    List<String> slot_list = new ArrayList<>(Arrays.asList(slot.split(",")));
                    int startIndex = currentPage * itemsPerPage;
                    int endIndex = Math.min(startIndex + itemsPerPage, item_list.size());
                    for (int i = startIndex; i < endIndex; i++) {
                        int slotIndex = i - startIndex;
                        if (slotIndex < slot_list.size()) {
                            String material = MineManager.getMaterial(item_list.get(i));
                            String name = File.getConfig().getString("items." + item_list.get(i));
                            ItemStack itemStack = ItemManager.getItemConfig(p, material, name != null ? name : item_list.get(i).split(";")[0], config.getConfigurationSection("items.storage_item"));
                            InteractiveItem interactiveItem = new InteractiveItem(itemStack, Number.getInteger(slot_list.get(slotIndex))).onClick((player, clickType) -> {
                                SoundManager.playItemSound(player, config, "items.storage_item", SoundContext.INITIAL_OPEN);
                                SoundManager.setShouldPlayCloseSound(player, false);
                                player.openInventory(new ItemStorage(p, material, currentPage).getInventory(SoundContext.SILENT));
                            });
                            inventory.setItem(interactiveItem.getSlot(), interactiveItem);
                        }
                    }
                }
            } else if (item_tag.equalsIgnoreCase("toggle_item")) {
                if (slot.contains(",")) {
                    for (String slot_string : slot.split(",")) {
                        InteractiveItem item = new InteractiveItem(ItemManager.getItemConfig(p, Objects.requireNonNull(config.getConfigurationSection("items." + item_tag))), Number.getInteger(slot_string)).onClick((player, clickType) -> {
                            SoundManager.playItemSound(player, config, "items." + item_tag, SoundContext.INITIAL_OPEN);
                            boolean newStatus = !MineManager.isAutoPickupEnabled(p);
                            MineManager.toggle.replace(p, newStatus);
                            Storage.db.updateAutoPickup(p.getName(), newStatus);
                            p.sendMessage(Chat.colorize(Objects.requireNonNull(File.getMessage().getString("user.status.toggle")).replace("#status#", ItemManager.getStatus(p))));
                            SoundManager.setShouldPlayCloseSound(p, false);
                            p.openInventory(new PersonalStorage(p, currentPage).getInventory(SoundContext.SILENT));
                        });
                        inventory.setItem(item.getSlot(), item);
                    }
                } else {
                    InteractiveItem item = new InteractiveItem(ItemManager.getItemConfig(p, Objects.requireNonNull(config.getConfigurationSection("items." + item_tag))), Number.getInteger(slot)).onClick((player, clickType) -> {
                        SoundManager.playItemSound(player, config, "items." + item_tag, SoundContext.INITIAL_OPEN);
                        boolean newStatus = !MineManager.isAutoPickupEnabled(p);
                        MineManager.toggle.replace(p, newStatus);
                        Storage.db.updateAutoPickup(p.getName(), newStatus);
                        p.sendMessage(Chat.colorize(Objects.requireNonNull(File.getMessage().getString("user.status.toggle")).replace("#status#", ItemManager.getStatus(p))));
                        SoundManager.setShouldPlayCloseSound(p, false);
                        p.openInventory(new PersonalStorage(p, currentPage).getInventory(SoundContext.SILENT));
                    });
                    inventory.setItem(item.getSlot(), item);
                }
            } else if (item_tag.equalsIgnoreCase("convert_button")) {
                if (p.hasPermission("storage.convert")) {
                    if (slot.contains(",")) {
                        for (String slot_string : slot.split(",")) {
                            InteractiveItem item = new InteractiveItem(ItemManager.getItemConfig(p, Objects.requireNonNull(config.getConfigurationSection("items." + item_tag))), Number.getInteger(slot_string)).onClick((player, clickType) -> {
                                if (!player.hasPermission("storage.convert")) {
                                    player.sendMessage(Chat.colorize(Objects.requireNonNull(File.getMessage().getString("admin.no_permission"))));
                                    return;
                                }
                                SoundManager.playItemSound(player, config, "items." + item_tag, SoundContext.INITIAL_OPEN);
                                SoundManager.setShouldPlayCloseSound(player, false);
                                player.openInventory(new ConvertOreGUI(p, 0).getInventory(SoundContext.SILENT));
                            });
                            inventory.setItem(item.getSlot(), item);
                        }
                    } else {
                        InteractiveItem item = new InteractiveItem(ItemManager.getItemConfig(p, Objects.requireNonNull(config.getConfigurationSection("items." + item_tag))), Number.getInteger(slot)).onClick((player, clickType) -> {
                            if (!player.hasPermission("storage.convert")) {
                                player.sendMessage(Chat.colorize(Objects.requireNonNull(File.getMessage().getString("admin.no_permission"))));
                                return;
                            }
                            SoundManager.playItemSound(player, config, "items." + item_tag, SoundContext.INITIAL_OPEN);
                            SoundManager.setShouldPlayCloseSound(player, false);
                            player.openInventory(new ConvertOreGUI(p, 0).getInventory(SoundContext.SILENT));
                        });
                        inventory.setItem(item.getSlot(), item);
                    }
                }
            } else if (item_tag.equalsIgnoreCase("previous_page")) {
                if (hasMultiplePages && currentPage > 0) {
                    ItemStack prevPageItem = getNavigationItem(item_tag, currentPage, totalPages);
                    if (prevPageItem != null) {
                        InteractiveItem item = new InteractiveItem(prevPageItem, Number.getInteger(slot)).onClick((player, clickType) -> {
                            SoundManager.playItemSound(player, config, "items.previous_page", SoundContext.INITIAL_OPEN);
                            SoundManager.setShouldPlayCloseSound(player, false);
                            player.openInventory(new PersonalStorage(p, currentPage - 1).getInventory(SoundContext.SILENT));
                        });
                        inventory.setItem(item.getSlot(), item);
                    }
                }
            } else if (item_tag.equalsIgnoreCase("next_page")) {
                if (hasMultiplePages && currentPage < totalPages - 1) {
                    ItemStack nextPageItem = getNavigationItem(item_tag, currentPage, totalPages);
                    if (nextPageItem != null) {
                        InteractiveItem item = new InteractiveItem(nextPageItem, Number.getInteger(slot)).onClick((player, clickType) -> {
                            SoundManager.playItemSound(player, config, "items.next_page", SoundContext.INITIAL_OPEN);
                            SoundManager.setShouldPlayCloseSound(player, false);
                            player.openInventory(new PersonalStorage(p, currentPage + 1).getInventory(SoundContext.SILENT));
                        });
                        inventory.setItem(item.getSlot(), item);
                    }
                }
            } else {
                if (slot.contains(",")) {
                    for (String slot_string : slot.split(",")) {
                        int slotNumber = Number.getInteger(slot_string);
                        if (hasMultiplePages && navigationSlots.contains(slotNumber)) {
                            continue;
                        }
                        InteractiveItem item = new InteractiveItem(ItemManager.getItemConfig(Objects.requireNonNull(config.getConfigurationSection("items." + item_tag))), slotNumber);
                        inventory.setItem(item.getSlot(), item);
                    }
                } else {
                    int slotNumber = Number.getInteger(slot);
                    if (!(hasMultiplePages && navigationSlots.contains(slotNumber))) {
                        InteractiveItem item = new InteractiveItem(ItemManager.getItemConfig(Objects.requireNonNull(config.getConfigurationSection("items." + item_tag))), slotNumber);
                        inventory.setItem(item.getSlot(), item);
                    }
                }
            }
        }
        return inventory;
    }

    public Player getPlayer() {
        return p;
    }

    public FileConfiguration getConfig() {
        return config;
    }

    public int getCurrentPage() {
        return currentPage;
    }

    private ItemStack getNavigationItem(String itemTag, int currentPage, int totalPages) {
        ConfigurationSection section = config.getConfigurationSection("items." + itemTag);
        if (section == null) return null;

        return ItemManager.getItemConfigWithPlaceholders(p, section,
                "#current_page#", String.valueOf(currentPage + 1),
                "#total_pages#", String.valueOf(totalPages));
    }
}
