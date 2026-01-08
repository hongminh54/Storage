package net.danh.storage.GUI;

import net.danh.storage.GUI.manager.IGUI;
import net.danh.storage.GUI.manager.InteractiveItem;
import net.danh.storage.Listeners.ChatListener;
import net.danh.storage.Manager.ItemManager;
import net.danh.storage.Manager.MythicStorageManager;
import net.danh.storage.Manager.MythicTransferManager;
import net.danh.storage.Manager.SoundManager;
import net.danh.storage.Utils.ChatUtils;
import net.danh.storage.Utils.File;
import net.danh.storage.Utils.SoundContext;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.logging.Level;
import java.util.stream.Collectors;

public class MythicTransferMultiGUI implements IGUI {
    private static final Set<String> LOGGED_KEYS = new HashSet<>();
    private static final Map<UUID, MythicTransferMultiGUI> activeGUIs = new HashMap<>();
    private final Player player;
    private final String targetPlayer;
    private final Map<String, Integer> selectedAmounts;
    private final Inventory inventory;
    private final int itemsPerPage = 21;
    private final Set<Integer> reservedSlots;
    private int currentPage;

    public MythicTransferMultiGUI(Player player, String targetPlayer) {
        this.player = player;
        this.targetPlayer = targetPlayer;
        this.selectedAmounts = new HashMap<>();
        this.currentPage = 0;
        this.reservedSlots = new HashSet<>();

        FileConfiguration guiConfig = getMythicTransferMultiConfig();
        String title = ChatUtils.colorizewp(player, guiConfig.getString(
                        "title", "&0Multi Transfer MythicMobs to #player#")
                .replace("#player#", targetPlayer));
        int size = guiConfig.getInt("size", 6) * 9;

        this.inventory = Bukkit.createInventory(this, size, title);
        initializeReservedSlots();
        activeGUIs.put(player.getUniqueId(), this);
    }

    public static MythicTransferMultiGUI getActiveGUI(Player player) {
        if (player == null) return null;
        return activeGUIs.get(player.getUniqueId());
    }

    public static void removeActiveGUI(Player player) {
        if (player == null) return;
        activeGUIs.remove(player.getUniqueId());
    }

    @NotNull
    @Override
    public Inventory getInventory() {
        return getInventory(SoundContext.INITIAL_OPEN);
    }

    @NotNull
    @Override
    public Inventory getInventory(SoundContext context) {
        setupGUI();
        if (context == SoundContext.INITIAL_OPEN) {
            playOpenSound();
        }
        return inventory;
    }

    private void setupGUI() {
        inventory.clear();

        FileConfiguration guiConfig = getMythicTransferMultiConfig();
        ConfigurationSection itemsSection = guiConfig.getConfigurationSection("items");

        if (itemsSection != null) {
            for (String itemKey : itemsSection.getKeys(false)) {
                if (!itemKey.equals("mythic_item_slots")) {
                    ConfigurationSection itemSection = itemsSection.getConfigurationSection(itemKey);
                    if (itemSection != null) {
                        setupStaticItem(itemKey, itemSection);
                    }
                }
            }
        }

        setupMythicItemSlots();
        updateNavigationItems();
    }

    private void initializeReservedSlots() {
        reservedSlots.clear();
        FileConfiguration guiConfig = getMythicTransferMultiConfig();
        ConfigurationSection itemsSection = guiConfig.getConfigurationSection("items");

        if (itemsSection != null) {
            for (String itemKey : itemsSection.getKeys(false)) {
                if (!itemKey.equals("mythic_item_slots") && !itemKey.equals("decorates")) {
                    ConfigurationSection itemSection = itemsSection.getConfigurationSection(itemKey);
                    if (itemSection != null && itemSection.contains("slot")) {
                        String slotString = itemSection.getString("slot");
                        if (slotString != null) {
                            try {
                                int slot = Integer.parseInt(slotString.trim());
                                reservedSlots.add(slot);
                            } catch (NumberFormatException ignored) {
                            }
                        }
                    }
                }
            }
        }
    }

    private void setupStaticItem(String itemKey, ConfigurationSection section) {
        String slotString = section.getString("slot");
        if (slotString == null) return;

        if (slotString.contains(",")) {
            for (String slotStr : slotString.split(",")) {
                try {
                    int slot = Integer.parseInt(slotStr.trim());
                    InteractiveItem item = createStaticItemByKey(itemKey, section, slot);
                    if (item != null) {
                        inventory.setItem(slot, item);
                    }
                } catch (NumberFormatException ignored) {
                }
            }
        } else {
            try {
                int slot = Integer.parseInt(slotString.trim());
                InteractiveItem item = createStaticItemByKey(itemKey, section, slot);
                if (item != null) {
                    inventory.setItem(slot, item);
                }
            } catch (NumberFormatException ignored) {
            }
        }
    }

    private InteractiveItem createStaticItemByKey(String itemKey, ConfigurationSection section, int slot) {
        switch (itemKey) {
            case "player_info":
                return createPlayerInfoItem(section, slot);
            case "confirm_transfer":
                return createConfirmItem(section, slot);
            case "cancel_transfer":
                return createCancelItem(section, slot);
            case "clear_selection":
                return createClearSelectionItem(section, slot);
            case "decorates":
                return createDecorativeItem(section, slot);
            default:
                return null;
        }
    }

    private void setupMythicItemSlots() {
        FileConfiguration guiConfig = getMythicTransferMultiConfig();
        ConfigurationSection mythicItemSection = guiConfig.getConfigurationSection("items.mythic_item_slots");
        if (mythicItemSection == null) return;

        String slotString = mythicItemSection.getString("slot", "");
        List<Integer> availableSlots = parseSlots(slotString).stream().filter(slot -> !reservedSlots.contains(slot)).collect(Collectors.toList());

        List<String> playerItems = getPlayerMythicItems();
        int startIndex = currentPage * itemsPerPage;
        int endIndex = Math.min(startIndex + itemsPerPage, playerItems.size());
        int maxSlots = Math.min(availableSlots.size(), endIndex - startIndex);

        for (int i = startIndex; i < endIndex; i++) {
            int slotIndex = i - startIndex;
            if (slotIndex < maxSlots) {
                int slot = availableSlots.get(slotIndex);
                String itemName = playerItems.get(i);
                InteractiveItem mythicItem = createMythicItem(itemName, mythicItemSection, slot);
                if (mythicItem != null) {
                    inventory.setItem(slot, mythicItem);
                }
            }
        }
    }

    private List<Integer> parseSlots(String slotString) {
        List<Integer> slots = new ArrayList<>();
        if (slotString == null || slotString.isEmpty()) return slots;

        String[] parts = slotString.split(",");
        for (String part : parts) {
            try {
                slots.add(Integer.parseInt(part.trim()));
            } catch (NumberFormatException ignored) {
            }
        }
        return slots;
    }

    private List<String> getPlayerMythicItems() {
        return MythicStorageManager.getConfiguredDrops().stream().filter(itemName -> MythicStorageManager.getPlayerItem(player, itemName) > 0).collect(Collectors.toList());
    }

    private InteractiveItem createMythicItem(String itemName, ConfigurationSection section, int slot) {
        int currentAmount = MythicStorageManager.getPlayerItem(player, itemName);
        int selectedAmount = selectedAmounts.getOrDefault(itemName, 0);
        String displayName = MythicStorageManager.getItemDisplayNameOrId(itemName, player);

        try {
            ItemStack mythicItem = MythicStorageManager.getMythicMobsHelper().getMythicItem(itemName);

            if (mythicItem != null) {
                mythicItem = ItemManager.replaceLore(mythicItem, section.getStringList("lore"), "#current_amount#", String.valueOf(currentAmount), "#selected_amount#", String.valueOf(selectedAmount));

                InteractiveItem item = new InteractiveItem(mythicItem, slot);
                item.onClick((p, clickType) -> {
                    SoundManager.playItemSound(p, getMythicTransferMultiConfig(), "items.mythic_item_slots", SoundContext.INITIAL_OPEN);
                    handleMythicItemClick(itemName, clickType);
                });
                return item;
            }
        } catch (Exception e) {
            synchronized (LOGGED_KEYS) {
                String key = "mythictransfer.multi.item." + itemName;
                if (LOGGED_KEYS.add(key)) {
                    net.danh.storage.Storage.getStorage().getLogger().log(Level.WARNING,
                            "[MythicStorage] Failed to build GUI icon for item '" + itemName
                                    + "' (fallback item will be used)",
                            e);
                }
            }
        }

        ItemStack fallbackItem = ItemManager.getItemConfigWithPlaceholders(player, section, "#current_amount#", String.valueOf(currentAmount), "#selected_amount#", String.valueOf(selectedAmount));

        InteractiveItem item = new InteractiveItem(fallbackItem, slot);
        item.onClick((p, clickType) -> {
            SoundManager.playItemSound(p, getMythicTransferMultiConfig(), "items.mythic_item_slots", SoundContext.INITIAL_OPEN);
            handleMythicItemClick(itemName, clickType);
        });
        return item;
    }

    private InteractiveItem createPlayerInfoItem(ConfigurationSection section, int slot) {
        ItemStack baseItem = ItemManager.getItemConfigWithPlaceholders(player, section, "#player#", targetPlayer);
        baseItem = ItemManager.setPlayerSkull(baseItem, targetPlayer);
        return new InteractiveItem(baseItem, slot);
    }

    private InteractiveItem createConfirmItem(ConfigurationSection section, int slot) {
        int selectedCount = selectedAmounts.values().stream().mapToInt(Integer::intValue).sum();
        ItemStack baseItem = ItemManager.getItemConfigWithPlaceholders(player, section, "#player#", targetPlayer, "#selected_count#", String.valueOf(selectedCount));

        InteractiveItem item = new InteractiveItem(baseItem, slot);
        item.onLeftClick(p -> {
            SoundManager.playItemSound(p, getMythicTransferMultiConfig(), "items.confirm_transfer", SoundContext.INITIAL_OPEN);
            confirmTransfer();
        });
        return item;
    }

    private InteractiveItem createCancelItem(ConfigurationSection section, int slot) {
        ItemStack baseItem = ItemManager.getItemConfig(player, section);
        InteractiveItem item = new InteractiveItem(baseItem, slot);
        item.onLeftClick(p -> {
            SoundManager.playItemSound(p, getMythicTransferMultiConfig(), "items.cancel_transfer", SoundContext.INITIAL_OPEN);
            cancelTransfer();
        });
        return item;
    }

    private InteractiveItem createClearSelectionItem(ConfigurationSection section, int slot) {
        ItemStack baseItem = ItemManager.getItemConfig(player, section);
        InteractiveItem item = new InteractiveItem(baseItem, slot);
        item.onLeftClick(p -> {
            SoundManager.playItemSound(p, getMythicTransferMultiConfig(), "items.clear_selection", SoundContext.INITIAL_OPEN);
            clearSelection();
        });
        return item;
    }

    private InteractiveItem createDecorativeItem(ConfigurationSection section, int slot) {
        ItemStack baseItem = ItemManager.getItemConfig(player, section);
        return new InteractiveItem(baseItem, slot);
    }

    private void updateNavigationItems() {
        if (!hasMultiplePages()) {
            return;
        }

        List<String> playerItems = getPlayerMythicItems();
        int totalPages = (int) Math.ceil((double) playerItems.size() / itemsPerPage);
        FileConfiguration guiConfig = getMythicTransferMultiConfig();

        if (currentPage > 0) {
            ConfigurationSection prevSection = guiConfig.getConfigurationSection("items.previous_page");
            if (prevSection != null) {
                String slotString = prevSection.getString("slot", "48");
                ItemStack prevPageItem = getNavigationItem("previous_page", currentPage, totalPages);
                if (prevPageItem != null) {
                    if (slotString.contains(",")) {
                        for (String slotStr : slotString.split(",")) {
                            try {
                                int slot = Integer.parseInt(slotStr.trim());
                                InteractiveItem prevItem = new InteractiveItem(prevPageItem.clone(), slot).onClick((p, clickType) -> {
                                    SoundManager.playItemSound(p, guiConfig, "items.previous_page", SoundContext.INITIAL_OPEN);
                                    previousPage();
                                });
                                inventory.setItem(slot, prevItem);
                            } catch (NumberFormatException ignored) {
                            }
                        }
                    } else {
                        try {
                            int slot = Integer.parseInt(slotString.trim());
                            InteractiveItem prevItem = new InteractiveItem(prevPageItem, slot).onClick((p, clickType) -> {
                                SoundManager.playItemSound(p, guiConfig, "items.previous_page", SoundContext.INITIAL_OPEN);
                                previousPage();
                            });
                            inventory.setItem(slot, prevItem);
                        } catch (NumberFormatException ignored) {
                        }
                    }
                }
            }
        }

        if (currentPage < totalPages - 1) {
            ConfigurationSection nextSection = guiConfig.getConfigurationSection("items.next_page");
            if (nextSection != null) {
                String slotString = nextSection.getString("slot", "50");
                ItemStack nextPageItem = getNavigationItem("next_page", currentPage, totalPages);
                if (nextPageItem != null) {
                    if (slotString.contains(",")) {
                        for (String slotStr : slotString.split(",")) {
                            try {
                                int slot = Integer.parseInt(slotStr.trim());
                                InteractiveItem nextItem = new InteractiveItem(nextPageItem.clone(), slot).onClick((p, clickType) -> {
                                    SoundManager.playItemSound(p, guiConfig, "items.next_page", SoundContext.INITIAL_OPEN);
                                    nextPage();
                                });
                                inventory.setItem(slot, nextItem);
                            } catch (NumberFormatException ignored) {
                            }
                        }
                    } else {
                        try {
                            int slot = Integer.parseInt(slotString.trim());
                            InteractiveItem nextItem = new InteractiveItem(nextPageItem, slot).onClick((p, clickType) -> {
                                SoundManager.playItemSound(p, guiConfig, "items.next_page", SoundContext.INITIAL_OPEN);
                                nextPage();
                            });
                            inventory.setItem(slot, nextItem);
                        } catch (NumberFormatException ignored) {
                        }
                    }
                }
            }
        }
    }

    private ItemStack getNavigationItem(String itemTag, int currentPage, int totalPages) {
        FileConfiguration guiConfig = getMythicTransferMultiConfig();
        ConfigurationSection section = guiConfig.getConfigurationSection("items." + itemTag);
        if (section == null) return null;

        return ItemManager.getItemConfigWithPlaceholders(player, section, "#current_page#", String.valueOf(currentPage + 1), "#total_pages#", String.valueOf(totalPages));
    }

    private void handleMythicItemClick(String itemName, ClickType clickType) {
        if (itemName == null || clickType == null) {
            return;
        }

        int currentAmount = MythicStorageManager.getPlayerItem(player, itemName);
        if (currentAmount <= 0) {
            return;
        }

        int selectedAmount = selectedAmounts.getOrDefault(itemName, 0);
        int newSelectedAmount = selectedAmount;

        switch (clickType) {
            case LEFT:
                if (selectedAmount < currentAmount) {
                    newSelectedAmount = selectedAmount + 1;
                }
                break;
            case RIGHT:
                if (selectedAmount + 10 <= currentAmount) {
                    newSelectedAmount = selectedAmount + 10;
                } else {
                    newSelectedAmount = currentAmount;
                }
                break;
            case SHIFT_LEFT:
                newSelectedAmount = Math.max(0, selectedAmount - 1);
                break;
            case SHIFT_RIGHT:
                newSelectedAmount = Math.max(0, selectedAmount - 10);
                break;
            case DROP:
                requestCustomAmount(itemName);
                return;
        }

        if (newSelectedAmount > 0) {
            selectedAmounts.put(itemName, newSelectedAmount);
        } else {
            selectedAmounts.remove(itemName);
        }

        setupGUI();
    }

    private void requestCustomAmount(String itemName) {
        player.closeInventory();
        ChatListener.chat_multi_mythic_transfer_item.put(player.getUniqueId(), itemName);
        ChatListener.chat_multi_mythic_transfer_target.put(player.getUniqueId(), targetPlayer);
        player.sendMessage(ChatUtils.colorize(
                File.getMessage().getString("mythicstorage.transfer.gui_enter_amount")));
    }

    public void setSelectedAmount(String itemName, int amount) {
        if (itemName == null || itemName.trim().isEmpty()) {
            return;
        }

        int currentAmount = MythicStorageManager.getPlayerItem(player, itemName);
        int newSelectedAmount = Math.min(Math.max(0, amount), currentAmount);

        if (newSelectedAmount > 0) {
            selectedAmounts.put(itemName, newSelectedAmount);
        } else {
            selectedAmounts.remove(itemName);
        }
    }

    private void confirmTransfer() {
        if (selectedAmounts.isEmpty()) {
            player.sendMessage(ChatUtils.colorize(File.getMessage().getString("mythicstorage.transfer.no_items_selected")));
            return;
        }

        boolean success = MythicTransferManager.executeMultiTransfer(player, targetPlayer, new HashMap<>(selectedAmounts));
        if (success) {
            player.closeInventory();
            activeGUIs.remove(player);
        }
    }

    private void cancelTransfer() {
        player.closeInventory();
        activeGUIs.remove(player);
    }

    private void clearSelection() {
        selectedAmounts.clear();
        setupGUI();
    }

    private void previousPage() {
        if (currentPage > 0) {
            currentPage--;
            setupGUI();
        }
    }

    private void nextPage() {
        List<String> playerItems = getPlayerMythicItems();
        int totalPages = (int) Math.ceil((double) playerItems.size() / itemsPerPage);
        if (currentPage < totalPages - 1) {
            currentPage++;
            setupGUI();
        }
    }

    private boolean hasMultiplePages() {
        List<String> playerItems = getPlayerMythicItems();
        return playerItems.size() > itemsPerPage;
    }

    private void playOpenSound() {
        FileConfiguration guiConfig = getMythicTransferMultiConfig();
        ConfigurationSection soundSection = guiConfig.getConfigurationSection("gui_open_sound");
        if (soundSection != null && soundSection.getBoolean("enabled", true)) {
            String soundName = soundSection.getString("name");
            SoundManager.playSound(player, soundName, (float) soundSection.getDouble("volume", 0.8), (float) soundSection.getDouble("pitch", 1.0));
        }
    }

    private FileConfiguration getMythicTransferMultiConfig() {
        return File.getFileSetting().get("GUI/mythictransfer-multi.yml");
    }
}
