package net.danh.storage.GUI;

import net.danh.storage.GUI.manager.IGUI;
import net.danh.storage.GUI.manager.InteractiveItem;
import net.danh.storage.Manager.ItemManager;
import net.danh.storage.Manager.MythicStorageManager;
import net.danh.storage.Manager.SoundManager;
import net.danh.storage.MythicMobs.MythicMobsHelper;
import net.danh.storage.Utils.Chat;
import net.danh.storage.Utils.File;
import net.danh.storage.Utils.Number;
import net.danh.storage.Utils.SoundContext;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class MythicStorageGUI implements IGUI {

    public static HashMap<Player, Integer> playerCurrentPage = new HashMap<>();
    private final Player player;
    private final FileConfiguration config;
    private final int currentPage;

    public MythicStorageGUI(Player player) {
        this(player, 0);
    }

    public MythicStorageGUI(Player player, int page) {
        this.player = player;
        this.currentPage = Math.max(0, page);
        this.config = File.getMythicStorageGUIConfig();
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
        
        Inventory inventory = Bukkit.createInventory(this, config.getInt("size") * 9, title);
        
        List<String> configuredDrops = MythicStorageManager.getConfiguredDrops();
        String slotConfig = Objects.requireNonNull(config.getString("items.mythic_item.slot")).replace(" ", "");
        int itemsPerPage = slotConfig.split(",").length;
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

        MythicMobsHelper helper = MythicStorageManager.getMythicMobsHelper();
        if (helper == null || !helper.isInitialized()) {
            return inventory;
        }

        for (String itemTag : Objects.requireNonNull(config.getConfigurationSection("items")).getKeys(false)) {
            String slot = Objects.requireNonNull(config.getString("items." + itemTag + ".slot")).replace(" ", "");
            
            if (itemTag.equalsIgnoreCase("mythic_item")) {
                if (slot.contains(",")) {
                    List<String> slotList = new ArrayList<>(Arrays.asList(slot.split(",")));
                    int startIndex = currentPage * itemsPerPage;
                    int endIndex = Math.min(startIndex + itemsPerPage, configuredDrops.size());
                    
                    for (int i = startIndex; i < endIndex; i++) {
                        int slotIndex = i - startIndex;
                        if (slotIndex < slotList.size()) {
                            String itemName = configuredDrops.get(i);
                            ItemStack mythicItem = helper.getMythicItem(itemName);
                            
                            if (mythicItem != null) {
                                ItemStack displayItem = mythicItem.clone();
                                ItemMeta meta = displayItem.getItemMeta();
                                
                                if (meta != null) {
                                    int amount = MythicStorageManager.getPlayerItem(player, itemName);
                                    int maxStorage = MythicStorageManager.getMaxStorage(player);
                                    
                                    List<String> lore = new ArrayList<>();
                                    for (String line : config.getStringList("items.mythic_item.lore")) {
                                        lore.add(Chat.colorize(line
                                            .replace("#item_amount#", String.valueOf(amount))
                                            .replace("#max_storage#", String.valueOf(maxStorage))));
                                    }
                                    
                                    meta.setLore(lore);
                                    displayItem.setItemMeta(meta);
                                }
                                
                                InteractiveItem interactiveItem = new InteractiveItem(displayItem, Number.getInteger(slotList.get(slotIndex)))
                                    .onClick((p, clickType) -> handleItemClick(p, itemName, clickType));
                                inventory.setItem(interactiveItem.getSlot(), interactiveItem);
                            }
                        }
                    }
                }
            } else if (itemTag.equalsIgnoreCase("previous_page")) {
                if (hasMultiplePages && currentPage > 0) {
                    ItemStack prevItem = getNavigationItem(itemTag, currentPage, totalPages);
                    if (prevItem != null) {
                        InteractiveItem item = new InteractiveItem(prevItem, Number.getInteger(slot))
                            .onClick((p, clickType) -> {
                                SoundManager.playItemSound(p, config, "items." + itemTag, SoundContext.INITIAL_OPEN);
                                SoundManager.setShouldPlayCloseSound(p, false);
                                p.openInventory(new MythicStorageGUI(p, currentPage - 1).getInventory(SoundContext.SILENT));
                            });
                        inventory.setItem(item.getSlot(), item);
                    }
                }
            } else if (itemTag.equalsIgnoreCase("next_page")) {
                if (hasMultiplePages && currentPage < totalPages - 1) {
                    ItemStack nextItem = getNavigationItem(itemTag, currentPage, totalPages);
                    if (nextItem != null) {
                        InteractiveItem item = new InteractiveItem(nextItem, Number.getInteger(slot))
                            .onClick((p, clickType) -> {
                                SoundManager.playItemSound(p, config, "items." + itemTag, SoundContext.INITIAL_OPEN);
                                SoundManager.setShouldPlayCloseSound(p, false);
                                p.openInventory(new MythicStorageGUI(p, currentPage + 1).getInventory(SoundContext.SILENT));
                            });
                        inventory.setItem(item.getSlot(), item);
                    }
                }
            } else {
                if (slot.contains(",")) {
                    for (String slotString : slot.split(",")) {
                        int slotNumber = Number.getInteger(slotString);
                        if (hasMultiplePages && navigationSlots.contains(slotNumber)) {
                            continue;
                        }
                        ConfigurationSection section = config.getConfigurationSection("items." + itemTag);
                        if (section != null) {
                            InteractiveItem item = new InteractiveItem(
                                ItemManager.getItemConfig(Objects.requireNonNull(section)), 
                                slotNumber
                            );
                            if (itemTag.equalsIgnoreCase("close")) {
                                item.onClick((p, clickType) -> {
                                    SoundManager.playItemSound(p, config, "items." + itemTag, SoundContext.INITIAL_OPEN);
                                    p.closeInventory();
                                });
                            }
                            inventory.setItem(item.getSlot(), item);
                        }
                    }
                } else {
                    int slotNumber = Number.getInteger(slot);
                    if (!(hasMultiplePages && navigationSlots.contains(slotNumber))) {
                        ConfigurationSection section = config.getConfigurationSection("items." + itemTag);
                        if (section != null) {
                            InteractiveItem item = new InteractiveItem(
                                ItemManager.getItemConfig(Objects.requireNonNull(section)), 
                                slotNumber
                            );
                            if (itemTag.equalsIgnoreCase("close")) {
                                item.onClick((p, clickType) -> {
                                    SoundManager.playItemSound(p, config, "items." + itemTag, SoundContext.INITIAL_OPEN);
                                    p.closeInventory();
                                });
                            }
                            inventory.setItem(item.getSlot(), item);
                        }
                    }
                }
            }
        }

        return inventory;
    }

    private void handleItemClick(Player player, String itemName, ClickType clickType) {
        int currentAmount = MythicStorageManager.getPlayerItem(player, itemName);
        
        if (clickType == ClickType.LEFT) {
            net.danh.storage.Listeners.Chat.chat_mythic_withdraw.put(player, itemName);
            net.danh.storage.Listeners.Chat.chat_return_page.put(player, currentPage);
            player.sendMessage(Chat.colorize(File.getMessage().getString("mythicstorage.action.withdraw.chat_number")));
            SoundManager.setShouldPlayCloseSound(player, false);
            player.closeInventory();
            
        } else if (clickType == ClickType.SHIFT_LEFT) {
            if (currentAmount <= 0) {
                player.sendMessage(Chat.colorize(File.getMessage().getString("mythicstorage.action.withdraw.not_enough")));
                return;
            }
            
            MythicMobsHelper helper = MythicStorageManager.getMythicMobsHelper();
            if (helper == null || !helper.isInitialized()) return;

            ItemStack mythicItem = helper.getMythicItem(itemName);
            if (mythicItem == null) return;

            int maxStackSize = mythicItem.getMaxStackSize();
            int remaining = currentAmount;
            
            while (remaining > 0) {
                int toGive = Math.min(remaining, maxStackSize);
                mythicItem.setAmount(toGive);
                
                HashMap<Integer, ItemStack> notFit = player.getInventory().addItem(mythicItem.clone());
                if (!notFit.isEmpty()) {
                    player.sendMessage(Chat.colorize(File.getMessage().getString("mythicstorage.action.withdraw.inventory_full")));
                    break;
                }
                
                remaining -= toGive;
                MythicStorageManager.removeItemAmount(player, itemName, toGive);
            }
            
            if (remaining < currentAmount) {
                SoundManager.playItemSound(player, config, "items.mythic_item.sound", SoundContext.SILENT);
                String message = File.getMessage().getString("mythicstorage.action.withdraw.withdraw_item")
                    .replace("#amount#", String.valueOf(currentAmount - remaining))
                    .replace("#material#", itemName)
                    .replace("#item_amount#", String.valueOf(MythicStorageManager.getPlayerItem(player, itemName)))
                    .replace("#max_storage#", String.valueOf(MythicStorageManager.getMaxStorage(player)));
                player.sendMessage(Chat.colorize(message));
            }
            
            SoundManager.setShouldPlayCloseSound(player, false);
            player.openInventory(new MythicStorageGUI(player, currentPage).getInventory(SoundContext.SILENT));
            
        } else if (clickType == ClickType.RIGHT) {
            net.danh.storage.Listeners.Chat.chat_mythic_deposit.put(player, itemName);
            net.danh.storage.Listeners.Chat.chat_return_page.put(player, currentPage);
            player.sendMessage(Chat.colorize(File.getMessage().getString("mythicstorage.action.deposit.chat_number")));
            SoundManager.setShouldPlayCloseSound(player, false);
            player.closeInventory();
            
        } else if (clickType == ClickType.SHIFT_RIGHT) {
            MythicMobsHelper helper = MythicStorageManager.getMythicMobsHelper();
            if (helper == null || !helper.isInitialized()) return;

            int deposited = 0;
            ItemStack[] contents = player.getInventory().getContents();
            
            for (int i = 0; i < contents.length; i++) {
                ItemStack item = contents[i];
                if (item == null) continue;
                
                String foundItemName = helper.getMythicItemInternalName(item);
                if (foundItemName != null && foundItemName.equals(itemName)) {
                    int amount = item.getAmount();
                    if (MythicStorageManager.addItemAmount(player, itemName, amount)) {
                        deposited += amount;
                        contents[i] = null;
                    } else {
                        player.sendMessage(Chat.colorize(File.getMessage().getString("mythicstorage.action.deposit.full_storage")
                            .replace("#item_amount#", String.valueOf(MythicStorageManager.getPlayerItem(player, itemName)))
                            .replace("#max_storage#", String.valueOf(MythicStorageManager.getMaxStorage(player)))
                            .replace("#amount#", String.valueOf(amount))
                            .replace("#material#", itemName)));
                        break;
                    }
                }
            }
            
            if (deposited > 0) {
                player.getInventory().setContents(contents);
                SoundManager.playItemSound(player, config, "items.mythic_item.sound", SoundContext.SILENT);
                String message = File.getMessage().getString("mythicstorage.action.deposit.deposit_item")
                    .replace("#amount#", String.valueOf(deposited))
                    .replace("#material#", itemName)
                    .replace("#item_amount#", String.valueOf(MythicStorageManager.getPlayerItem(player, itemName)))
                    .replace("#max_storage#", String.valueOf(MythicStorageManager.getMaxStorage(player)));
                player.sendMessage(Chat.colorize(message));
            } else {
                player.sendMessage(Chat.colorize(File.getMessage().getString("mythicstorage.action.deposit.no_items")));
            }
            
            SoundManager.setShouldPlayCloseSound(player, false);
            player.openInventory(new MythicStorageGUI(player, currentPage).getInventory(SoundContext.SILENT));
        }
    }

    private ItemStack getNavigationItem(String itemTag, int currentPage, int totalPages) {
        ConfigurationSection section = config.getConfigurationSection("items." + itemTag);
        if (section == null) return null;

        return ItemManager.getItemConfigWithPlaceholders(player, section,
            "#current_page#", String.valueOf(currentPage + 1),
            "#total_pages#", String.valueOf(totalPages));
    }

    public Player getPlayer() {
        return player;
    }

    public int getCurrentPage() {
        return currentPage;
    }
}
