package net.danh.storage.GUI;

import net.danh.storage.GUI.manager.IGUI;
import net.danh.storage.GUI.manager.InteractiveItem;
import net.danh.storage.Manager.ItemManager;
import net.danh.storage.Manager.MineManager;
import net.danh.storage.Manager.SoundManager;
import net.danh.storage.Manager.TransferManager;
import net.danh.storage.Utils.Chat;
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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TransferMultiGUI implements IGUI {
    private static final Map<Player, TransferMultiGUI> activeGUIs = new HashMap<>();
    private final Player player;
    private final String targetPlayer;
    private final Map<String, Integer> selectedAmounts;
    private final Inventory inventory;
    private final int itemsPerPage = 21;
    private int currentPage;

    public TransferMultiGUI(Player player, String targetPlayer) {
        this.player = player;
        this.targetPlayer = targetPlayer;
        this.selectedAmounts = new HashMap<>();
        this.currentPage = 0;

        FileConfiguration guiConfig = getTransferMultiConfig();
        String title = Chat.colorizewp(guiConfig.getString("title", "&0Multi Transfer to #player#")
                .replace("#player#", targetPlayer));
        int size = guiConfig.getInt("size", 6) * 9;

        this.inventory = Bukkit.createInventory(this, size, title);
        activeGUIs.put(player, this);
    }

    public static TransferMultiGUI getActiveGUI(Player player) {
        return activeGUIs.get(player);
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

        FileConfiguration guiConfig = getTransferMultiConfig();
        ConfigurationSection itemsSection = guiConfig.getConfigurationSection("items");

        if (itemsSection != null) {
            for (String itemKey : itemsSection.getKeys(false)) {
                if (!itemKey.equals("material_slots")) {
                    ConfigurationSection itemSection = itemsSection.getConfigurationSection(itemKey);
                    if (itemSection != null) {
                        setupStaticItem(itemKey, itemSection);
                    }
                }
            }
        }

        setupMaterialSlots();
        updateNavigationItems();
    }

    private void setupStaticItem(String itemKey, ConfigurationSection section) {
        String slotString = section.getString("slot");
        if (slotString == null) return;

        String[] slots = slotString.split(",");
        for (String slotStr : slots) {
            try {
                int slot = Integer.parseInt(slotStr.trim());
                InteractiveItem item = createStaticItem(itemKey, section, slot);
                if (item != null) {
                    inventory.setItem(slot, item);
                }
            } catch (NumberFormatException ignored) {
            }
        }
    }

    private InteractiveItem createStaticItem(String itemKey, ConfigurationSection section, int slot) {
        switch (itemKey) {
            case "player_info":
                return createPlayerInfoItem(section, slot);
            case "confirm_transfer":
                return createConfirmItem(section, slot);
            case "cancel_transfer":
                return createCancelItem(section, slot);
            case "clear_selection":
                return createClearSelectionItem(section, slot);
            case "previous_page":
                return createPreviousPageItem(section, slot);
            case "next_page":
                return createNextPageItem(section, slot);
            default:
                return createDecorativeItem(section, slot);
        }
    }

    private void setupMaterialSlots() {
        FileConfiguration guiConfig = getTransferMultiConfig();
        ConfigurationSection materialSection = guiConfig.getConfigurationSection("items.material_slots");
        if (materialSection == null) return;

        String slotString = materialSection.getString("slot");
        if (slotString == null) return;

        String[] slots = slotString.split(",");
        List<String> playerMaterials = getPlayerMaterials();

        int startIndex = currentPage * itemsPerPage;
        int endIndex = Math.min(startIndex + itemsPerPage, playerMaterials.size());

        for (int i = 0; i < slots.length && (startIndex + i) < endIndex; i++) {
            try {
                int slot = Integer.parseInt(slots[i].trim());
                String material = playerMaterials.get(startIndex + i);
                InteractiveItem item = createMaterialItem(material, materialSection, slot);
                if (item != null) {
                    inventory.setItem(slot, item);
                }
            } catch (NumberFormatException ignored) {
            }
        }
    }

    private List<String> getPlayerMaterials() {
        return MineManager.getPluginBlocks().stream()
                .filter(material -> MineManager.getPlayerBlock(player, material) > 0)
                .collect(java.util.stream.Collectors.toList());
    }

    private InteractiveItem createMaterialItem(String material, ConfigurationSection section, int slot) {
        int currentAmount = MineManager.getPlayerBlock(player, material);
        int selectedAmount = selectedAmounts.getOrDefault(material, 0);
        String materialName = getMaterialDisplayName(material);

        try {
            ItemStack baseItem = ItemManager.getItemConfig(player, material, materialName, section);
            baseItem = ItemManager.replaceLore(baseItem, section.getStringList("lore"),
                    "#current_amount#", String.valueOf(currentAmount),
                    "#selected_amount#", String.valueOf(selectedAmount));

            InteractiveItem item = new InteractiveItem(baseItem, slot);
            item.onClick((p, clickType) -> {
                SoundManager.playItemSound(p, getTransferMultiConfig(), "items.material_slots", SoundContext.INITIAL_OPEN);
                handleMaterialClick(material, clickType);
            });
            return item;
        } catch (Exception e) {
            // Fallback to basic item if material creation fails
            ItemStack fallbackItem = ItemManager.getItemConfig(player, section);
            fallbackItem = ItemManager.replaceLore(fallbackItem, section.getStringList("lore"),
                    "#current_amount#", String.valueOf(currentAmount),
                    "#selected_amount#", String.valueOf(selectedAmount));

            InteractiveItem item = new InteractiveItem(fallbackItem, slot);
            item.onClick((p, clickType) -> {
                SoundManager.playItemSound(p, getTransferMultiConfig(), "items.material_slots", SoundContext.INITIAL_OPEN);
                handleMaterialClick(material, clickType);
            });
            return item;
        }
    }

    private InteractiveItem createPlayerInfoItem(ConfigurationSection section, int slot) {
        ItemStack baseItem = ItemManager.getItemConfig(player, section);
        baseItem = ItemManager.replaceLore(baseItem, section.getStringList("lore"),
                "#player#", targetPlayer);
        baseItem = ItemManager.replacePlaceholders(baseItem, "#player#", targetPlayer);

        return new InteractiveItem(baseItem, slot);
    }

    private InteractiveItem createConfirmItem(ConfigurationSection section, int slot) {
        int selectedCount = selectedAmounts.values().stream().mapToInt(Integer::intValue).sum();
        ItemStack baseItem = ItemManager.getItemConfig(player, section);
        baseItem = ItemManager.replaceLore(baseItem, section.getStringList("lore"),
                "#player#", targetPlayer,
                "#selected_count#", String.valueOf(selectedCount));
        baseItem = ItemManager.replacePlaceholders(baseItem,
                "#player#", targetPlayer,
                "#selected_count#", String.valueOf(selectedCount));

        InteractiveItem item = new InteractiveItem(baseItem, slot);
        item.onLeftClick(p -> {
            SoundManager.playItemSound(p, getTransferMultiConfig(), "items.confirm_transfer", SoundContext.INITIAL_OPEN);
            handleClick("confirm_transfer", null, ClickType.LEFT);
        });
        return item;
    }

    private InteractiveItem createCancelItem(ConfigurationSection section, int slot) {
        ItemStack baseItem = ItemManager.getItemConfig(player, section);
        InteractiveItem item = new InteractiveItem(baseItem, slot);
        item.onLeftClick(p -> {
            SoundManager.playItemSound(p, getTransferMultiConfig(), "items.cancel_transfer", SoundContext.INITIAL_OPEN);
            handleClick("cancel_transfer", null, ClickType.LEFT);
        });
        return item;
    }

    private InteractiveItem createClearSelectionItem(ConfigurationSection section, int slot) {
        ItemStack baseItem = ItemManager.getItemConfig(player, section);
        InteractiveItem item = new InteractiveItem(baseItem, slot);
        item.onLeftClick(p -> {
            SoundManager.playItemSound(p, getTransferMultiConfig(), "items.clear_selection", SoundContext.INITIAL_OPEN);
            handleClick("clear_selection", null, ClickType.LEFT);
        });
        return item;
    }

    private InteractiveItem createPreviousPageItem(ConfigurationSection section, int slot) {
        List<String> playerMaterials = getPlayerMaterials();
        int totalPages = (int) Math.ceil((double) playerMaterials.size() / itemsPerPage);

        ItemStack baseItem = ItemManager.getItemConfig(player, section);
        baseItem = ItemManager.replaceLore(baseItem, section.getStringList("lore"),
                "#current_page#", String.valueOf(currentPage + 1),
                "#total_pages#", String.valueOf(totalPages));

        InteractiveItem item = new InteractiveItem(baseItem, slot);
        item.onLeftClick(p -> {
            SoundManager.playItemSound(p, getTransferMultiConfig(), "items.previous_page", SoundContext.INITIAL_OPEN);
            handleClick("previous_page", null, ClickType.LEFT);
        });
        return item;
    }

    private InteractiveItem createNextPageItem(ConfigurationSection section, int slot) {
        List<String> playerMaterials = getPlayerMaterials();
        int totalPages = (int) Math.ceil((double) playerMaterials.size() / itemsPerPage);

        ItemStack baseItem = ItemManager.getItemConfig(player, section);
        baseItem = ItemManager.replaceLore(baseItem, section.getStringList("lore"),
                "#current_page#", String.valueOf(currentPage + 1),
                "#total_pages#", String.valueOf(totalPages));

        InteractiveItem item = new InteractiveItem(baseItem, slot);
        item.onLeftClick(p -> {
            SoundManager.playItemSound(p, getTransferMultiConfig(), "items.next_page", SoundContext.INITIAL_OPEN);
            handleClick("next_page", null, ClickType.LEFT);
        });
        return item;
    }

    private InteractiveItem createDecorativeItem(ConfigurationSection section, int slot) {
        ItemStack baseItem = ItemManager.getItemConfig(player, section);
        return new InteractiveItem(baseItem, slot);
    }

    private void updateNavigationItems() {
        List<String> playerMaterials = getPlayerMaterials();
        int totalPages = (int) Math.ceil((double) playerMaterials.size() / itemsPerPage);

        FileConfiguration guiConfig = getTransferMultiConfig();

        if (currentPage > 0) {
            ConfigurationSection prevSection = guiConfig.getConfigurationSection("items.previous_page");
            if (prevSection != null) {
                InteractiveItem prevItem = createPreviousPageItem(prevSection, 48);
                inventory.setItem(48, prevItem);
            }
        }

        if (currentPage < totalPages - 1) {
            ConfigurationSection nextSection = guiConfig.getConfigurationSection("items.next_page");
            if (nextSection != null) {
                InteractiveItem nextItem = createNextPageItem(nextSection, 50);
                inventory.setItem(50, nextItem);
            }
        }
    }

    public void handleClick(String itemKey, String material, ClickType clickType) {
        switch (itemKey) {
            case "material_click":
                handleMaterialClick(material, clickType);
                break;
            case "confirm_transfer":
                confirmTransfer();
                break;
            case "cancel_transfer":
                cancelTransfer();
                break;
            case "clear_selection":
                clearSelection();
                break;
            case "previous_page":
                previousPage();
                break;
            case "next_page":
                nextPage();
                break;
        }
    }

    private void handleMaterialClick(String material, ClickType clickType) {
        if (material == null || clickType == null) {
            return;
        }

        int currentAmount = MineManager.getPlayerBlock(player, material);
        if (currentAmount <= 0) {
            return; // No items to select
        }

        int selectedAmount = selectedAmounts.getOrDefault(material, 0);
        int newSelectedAmount = selectedAmount;

        switch (clickType) {
            case LEFT:
                if (selectedAmount < currentAmount) {
                    newSelectedAmount = selectedAmount + 1;
                }
                break;
            case RIGHT:
                newSelectedAmount = Math.min(selectedAmount + 10, currentAmount);
                break;
            case SHIFT_LEFT:
                if (selectedAmount > 0) {
                    newSelectedAmount = selectedAmount - 1;
                }
                break;
            case SHIFT_RIGHT:
                newSelectedAmount = Math.max(selectedAmount - 10, 0);
                break;
            case DROP:
                newSelectedAmount = currentAmount;
                break;
        }

        // Update the selected amount or remove if 0
        if (newSelectedAmount > 0) {
            selectedAmounts.put(material, newSelectedAmount);
        } else {
            selectedAmounts.remove(material);
        }

        updateGUI();
    }

    private void confirmTransfer() {
        if (selectedAmounts.isEmpty()) {
            player.sendMessage(Chat.colorize("&cNo materials selected for transfer!"));
            return;
        }

        player.closeInventory();

        // Use multi transfer method instead of individual transfers
        if (TransferManager.executeMultiTransfer(player, targetPlayer, selectedAmounts)) {
            // Transfer initiated successfully
        } else {
            player.sendMessage(Chat.colorize("&cFailed to initiate multi transfer!"));
        }

        activeGUIs.remove(player);
    }

    private void cancelTransfer() {
        player.closeInventory();
        activeGUIs.remove(player);
        try {
            int currentPage = PersonalStorage.getPlayerCurrentPage(player);
            player.openInventory(new PersonalStorage(player, currentPage).getInventory());
        } catch (Exception e) {
            player.sendMessage(Chat.colorize("&cError opening storage GUI"));
        }
    }

    private void clearSelection() {
        selectedAmounts.clear();
        updateGUI();
    }

    private void previousPage() {
        if (currentPage > 0) {
            currentPage--;
            updateGUI();
        }
    }

    private void nextPage() {
        List<String> playerMaterials = getPlayerMaterials();
        int totalPages = (int) Math.ceil((double) playerMaterials.size() / itemsPerPage);

        if (currentPage < totalPages - 1) {
            currentPage++;
            updateGUI();
        }
    }

    private void updateGUI() {
        player.openInventory(getInventory(SoundContext.SILENT));
    }

    private void playOpenSound() {
        FileConfiguration guiConfig = getTransferMultiConfig();
        ConfigurationSection soundSection = guiConfig.getConfigurationSection("gui_open_sound");
        if (soundSection != null && soundSection.getBoolean("enabled", true)) {
            String soundName = soundSection.getString("name");
            float volume = (float) soundSection.getDouble("volume", 0.8);
            float pitch = (float) soundSection.getDouble("pitch", 1.0);
            SoundManager.playSound(player, soundName, volume, pitch);
        }
    }

    private String getMaterialDisplayName(String material) {
        FileConfiguration config = File.getConfig();
        String displayName = config.getString("items." + material);
        return displayName != null ? displayName : material.replace(";0", "").replace("_", " ");
    }

    private FileConfiguration getTransferMultiConfig() {
        return File.getFileSetting().get("GUI/transfer-multi.yml");
    }

    public String getTargetPlayer() {
        return targetPlayer;
    }
}
