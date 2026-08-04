package net.danh.storage.GUI.Mob;

import net.danh.storage.GUI.manager.IGUI;
import net.danh.storage.GUI.manager.InteractiveItem;
import net.danh.storage.Listeners.ChatListener;
import net.danh.storage.Manager.Mob.MobStorageManager;
import net.danh.storage.Manager.Mob.MobTransferManager;
import net.danh.storage.Manager.SoundManager;
import net.danh.storage.Storage;
import net.danh.storage.Utils.ChatUtils;
import net.danh.storage.Utils.File;
import net.danh.storage.Utils.SoundContext;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.logging.Level;
import java.util.stream.Collectors;

public class MobTransferMultiGUI implements IGUI {
    private static final Set<String> LOGGED_KEYS = new HashSet<>();
    private static final Map<UUID, MobTransferMultiGUI> activeGUIs = new HashMap<>();
    private static final Map<UUID, Boolean> waitingForReceiver = new HashMap<>();
    private final Player player;
    private final Map<String, Integer> selectedAmounts;
    private final Inventory inventory;
    private final int itemsPerPage = 21;
    private final Set<Integer> reservedSlots;
    private String targetPlayer;
    private int currentPage;

    public MobTransferMultiGUI(Player player, String targetPlayer) {
        this.player = player;
        this.targetPlayer = targetPlayer != null ? targetPlayer : "";
        this.selectedAmounts = new HashMap<>();
        this.currentPage = 0;
        this.reservedSlots = new HashSet<>();

        FileConfiguration guiConfig = getTransferMultiConfig();
        String title = ChatUtils.colorize(guiConfig.getString(
                        "title", "&0Multi Transfer to #player#")
                .replace("#player#", this.targetPlayer));
        int size = guiConfig.getInt("size", 6) * 9;

        this.inventory = Bukkit.createInventory(this, size, title);
        initializeReservedSlots();
        activeGUIs.put(player.getUniqueId(), this);
    }

    public static MobTransferMultiGUI getActiveGUI(Player player) {
        if (player == null) return null;
        return activeGUIs.get(player.getUniqueId());
    }

    public static void removeActiveGUI(Player player) {
        if (player == null) return;
        activeGUIs.remove(player.getUniqueId());
    }

    public static boolean isWaitingForReceiver(Player player) {
        if (player == null) return false;
        return waitingForReceiver.getOrDefault(player.getUniqueId(), false);
    }

    public static void setWaitingForReceiver(Player player, boolean waiting) {
        if (player == null) return;
        UUID playerId = player.getUniqueId();
        if (waiting) {
            waitingForReceiver.put(playerId, true);
        } else {
            waitingForReceiver.remove(playerId);
        }
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

    private void initializeReservedSlots() {
        reservedSlots.clear();
        FileConfiguration guiConfig = getTransferMultiConfig();
        ConfigurationSection itemsSection = guiConfig.getConfigurationSection("items");

        if (itemsSection != null) {
            for (String itemKey : itemsSection.getKeys(false)) {
                if (!itemKey.equals("material_slots") && !itemKey.equals("decorates")) {
                    ConfigurationSection itemSection = itemsSection.getConfigurationSection(itemKey);
                    if (itemSection != null) {
                        reservedSlots.addAll(getSlots(itemSection));
                    }
                }
            }
        }
    }

    private List<Integer> getAvailableMaterialSlots() {
        List<Integer> availableSlots = new ArrayList<>();
        FileConfiguration guiConfig = getTransferMultiConfig();
        ConfigurationSection materialSection = guiConfig.getConfigurationSection("items.material_slots");

        if (materialSection != null) {
            for (int slot : getSlots(materialSection)) {
                if (!reservedSlots.contains(slot)) {
                    availableSlots.add(slot);
                }
            }
        }
        return availableSlots;
    }

    private boolean hasMultiplePages() {
        List<String> playerItems = getPlayerItems();
        return Math.ceil((double) playerItems.size() / itemsPerPage) > 1;
    }

    private void setupStaticItem(String itemKey, ConfigurationSection section) {
        for (int slot : getSlots(section)) {
            InteractiveItem item = createStaticItem(itemKey, section, slot);
            if (item != null) {
                inventory.setItem(slot, item);
            }
        }
    }

    private List<Integer> getSlots(ConfigurationSection section) {
        if (section == null) {
            return Collections.emptyList();
        }

        if (section.isList("slots")) {
            return section.getIntegerList("slots");
        }

        if (section.isString("slot")) {
            String slotString = section.getString("slot");
            if (slotString == null || slotString.trim().isEmpty()) {
                return Collections.emptyList();
            }

            List<Integer> slots = new ArrayList<>();
            for (String slotStr : slotString.split(",")) {
                try {
                    slots.add(Integer.parseInt(slotStr.trim()));
                } catch (NumberFormatException ignored) {
                }
            }
            return slots;
        }

        if (section.isInt("slot")) {
            return Collections.singletonList(section.getInt("slot"));
        }

        return Collections.emptyList();
    }

    private InteractiveItem createStaticItem(String itemKey, ConfigurationSection section, int slot) {
        switch (itemKey) {
            case "player_info":
                return createPlayerInfoItem(section, slot);
            case "receiver_input":
                return createReceiverInputItem(section, slot);
            case "confirm":
            case "confirm_transfer":
                return createConfirmItem(section, slot);
            case "cancel":
            case "cancel_transfer":
                return createCancelItem(section, slot);
            case "clear_selection":
                return createClearSelectionItem(section, slot);
            case "prev_page":
            case "next_page":
            case "previous_page":
                return null;
            default:
                return createDecorativeItem(section, slot);
        }
    }

    private void setupMaterialSlots() {
        List<String> playerItems = getPlayerItems();
        List<Integer> availableSlots = getAvailableMaterialSlots();

        if (availableSlots.isEmpty()) {
            return;
        }

        FileConfiguration guiConfig = getTransferMultiConfig();
        ConfigurationSection materialSection = guiConfig.getConfigurationSection("items.material_slots");
        if (materialSection == null) {
            return;
        }

        int startIndex = currentPage * itemsPerPage;
        int endIndex = Math.min(startIndex + itemsPerPage, playerItems.size());
        int maxSlots = Math.min(availableSlots.size(), itemsPerPage);

        for (int i = startIndex; i < endIndex; i++) {
            int slotIndex = i - startIndex;
            if (slotIndex < maxSlots) {
                int slot = availableSlots.get(slotIndex);
                String itemName = playerItems.get(i);
                InteractiveItem materialItem = createMaterialItem(itemName, materialSection, slot);
                if (materialItem != null) {
                    inventory.setItem(slot, materialItem);
                }
            }
        }
    }

    private List<String> getPlayerItems() {
        return MobStorageManager.getConfiguredDrops().stream()
                .filter(itemName -> MobStorageManager.getPlayerItem(player, itemName) > 0)
                .collect(Collectors.toList());
    }

    private InteractiveItem createMaterialItem(String itemName, ConfigurationSection section, int slot) {
        int currentAmount = MobStorageManager.getPlayerItem(player, itemName);
        int selectedAmount = selectedAmounts.getOrDefault(itemName, 0);
        Player receiver = Bukkit.getPlayer(targetPlayer);
        int maxTransferable = receiver != null ?
                MobTransferManager.getOptimalTransferAmount(player, targetPlayer, itemName, currentAmount) :
                currentAmount;
        String displayName = MobStorageManager.getItemDisplayName(itemName);
        Material material = MobStorageManager.resolveMaterial(itemName);

        try {
            ItemStack baseItem = new ItemStack(material);
            baseItem = applyItemMeta(baseItem, section, displayName, currentAmount, selectedAmount, maxTransferable);

            InteractiveItem item = new InteractiveItem(baseItem, slot);
            item.onClick((p, clickType) -> {
                playClickSound();
                handleMaterialClick(itemName, clickType);
            });
            return item;
        } catch (Exception e) {
            synchronized (LOGGED_KEYS) {
                String key = "mobtransfer.multi.material_item." + itemName;
                if (LOGGED_KEYS.add(key)) {
                    Storage.getStorage().getLogger().log(Level.WARNING,
                            "[Storage] Failed to build MobTransferMultiGUI icon for item '"
                                    + itemName + "' (fallback item will be used)",
                            e);
                }
            }
            ItemStack fallbackItem = new ItemStack(material);
            fallbackItem = applyItemMeta(fallbackItem, section, displayName, currentAmount, selectedAmount, maxTransferable);

            InteractiveItem item = new InteractiveItem(fallbackItem, slot);
            item.onClick((p, clickType) -> {
                playClickSound();
                handleMaterialClick(itemName, clickType);
            });
            return item;
        }
    }

    private InteractiveItem createPlayerInfoItem(ConfigurationSection section, int slot) {
        int selectedCount = selectedAmounts.size();
        int totalAmount = selectedAmounts.values().stream().mapToInt(Integer::intValue).sum();

        ItemStack baseItem = createPlayerHead(player.getName());
        baseItem = applyItemMeta(baseItem, section, "", 0, 0, 0, selectedCount, totalAmount);

        return new InteractiveItem(baseItem, slot);
    }

    private InteractiveItem createReceiverInputItem(ConfigurationSection section, int slot) {
        ItemStack baseItem = createPlayerHead(targetPlayer.isEmpty() ? "MHF_Question" : targetPlayer);
        baseItem = applyItemMeta(baseItem, section);

        InteractiveItem item = new InteractiveItem(baseItem, slot);
        item.onLeftClick(p -> {
            playClickSound();
            requestReceiverInput();
        });
        return item;
    }

    private InteractiveItem createConfirmItem(ConfigurationSection section, int slot) {
        int selectedCount = selectedAmounts.size();
        int totalAmount = selectedAmounts.values().stream().mapToInt(Integer::intValue).sum();

        ItemStack baseItem = new ItemStack(Material.GREEN_STAINED_GLASS_PANE);
        baseItem = applyItemMeta(baseItem, section, "", 0, 0, 0, selectedCount, totalAmount);

        InteractiveItem item = new InteractiveItem(baseItem, slot);
        item.onLeftClick(p -> {
            playClickSound();
            confirmTransfer();
        });
        return item;
    }

    private InteractiveItem createCancelItem(ConfigurationSection section, int slot) {
        ItemStack baseItem = new ItemStack(Material.RED_STAINED_GLASS_PANE);
        baseItem = applyItemMeta(baseItem, section);

        InteractiveItem item = new InteractiveItem(baseItem, slot);
        item.onLeftClick(p -> {
            playClickSound();
            cancelTransfer();
        });
        return item;
    }

    private InteractiveItem createClearSelectionItem(ConfigurationSection section, int slot) {
        ItemStack baseItem = new ItemStack(Material.BARRIER);
        baseItem = applyItemMeta(baseItem, section);

        InteractiveItem item = new InteractiveItem(baseItem, slot);
        item.onLeftClick(p -> {
            playClickSound();
            clearSelection();
        });
        return item;
    }

    private InteractiveItem createDecorativeItem(ConfigurationSection section, int slot) {
        String materialName = section.getString("material", "GRAY_STAINED_GLASS_PANE");
        Material material;
        try {
            material = Material.valueOf(materialName.toUpperCase());
        } catch (IllegalArgumentException e) {
            material = Material.GRAY_STAINED_GLASS_PANE;
        }

        ItemStack baseItem = new ItemStack(material);
        baseItem = applyItemMeta(baseItem, section);
        return new InteractiveItem(baseItem, slot);
    }

    private void updateNavigationItems() {
        if (!hasMultiplePages()) {
            return;
        }

        List<String> playerItems = getPlayerItems();
        int totalPages = (int) Math.ceil((double) playerItems.size() / itemsPerPage);
        FileConfiguration guiConfig = getTransferMultiConfig();

        if (currentPage > 0) {
            ConfigurationSection prevSection = guiConfig.getConfigurationSection("items.previous_page");
            if (prevSection == null) {
                prevSection = guiConfig.getConfigurationSection("items.prev_page");
            }
            if (prevSection != null) {
                int slot = prevSection.getInt("slot", 45);
                ItemStack prevPageItem = getNavigationItem(prevSection, currentPage, totalPages);
                if (prevPageItem != null) {
                    InteractiveItem prevItem = new InteractiveItem(prevPageItem, slot).onClick((p, clickType) -> {
                        playClickSound();
                        previousPage();
                    });
                    inventory.setItem(slot, prevItem);
                }
            }
        }

        if (currentPage < totalPages - 1) {
            ConfigurationSection nextSection = guiConfig.getConfigurationSection("items.next_page");
            if (nextSection != null) {
                int slot = nextSection.getInt("slot", 53);
                ItemStack nextPageItem = getNavigationItem(nextSection, currentPage, totalPages);
                if (nextPageItem != null) {
                    InteractiveItem nextItem = new InteractiveItem(nextPageItem, slot).onClick((p, clickType) -> {
                        playClickSound();
                        nextPage();
                    });
                    inventory.setItem(slot, nextItem);
                }
            }
        }
    }

    private ItemStack getNavigationItem(ConfigurationSection section, int currentPage, int totalPages) {
        if (section == null) return null;

        String materialName = section.getString("material", "ARROW");
        Material material;
        try {
            material = Material.valueOf(materialName.toUpperCase());
        } catch (IllegalArgumentException e) {
            material = Material.ARROW;
        }

        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            String name = section.getString("name", "&6Page");
            name = ChatUtils.colorize(name
                    .replace("#current_page#", String.valueOf(currentPage + 1))
                    .replace("#total_pages#", String.valueOf(totalPages)));
            meta.setDisplayName(name);

            List<String> lore = section.getStringList("lore");
            if (!lore.isEmpty()) {
                List<String> processedLore = new ArrayList<>();
                for (String line : lore) {
                    processedLore.add(ChatUtils.colorize(line
                            .replace("#current_page#", String.valueOf(currentPage + 1))
                            .replace("#total_pages#", String.valueOf(totalPages))));
                }
                meta.setLore(processedLore);
            }
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack applyItemMeta(ItemStack item, ConfigurationSection section, String displayName,
                                    int currentAmount, int selectedAmount, int maxTransferable) {
        return applyItemMeta(item, section, displayName, currentAmount, selectedAmount, maxTransferable, 0, 0);
    }

    private ItemStack applyItemMeta(ItemStack item, ConfigurationSection section, String displayName,
                                    int currentAmount, int selectedAmount, int maxTransferable,
                                    int selectedCount, int totalAmount) {
        if (item == null || section == null) return item;

        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;

        String nameTemplate = section.getString("name");
        String name = (nameTemplate == null || nameTemplate.isEmpty()) ? "#item_name#" : nameTemplate;
        name = ChatUtils.colorize(name
                .replace("#item_name#", displayName)
                .replace("#player#", player.getName())
                .replace("#receiver#", targetPlayer)
                .replace("#current_amount#", String.valueOf(currentAmount))
                .replace("#selected_amount#", String.valueOf(selectedAmount))
                .replace("#max_transferable#", String.valueOf(maxTransferable))
                .replace("#selected_count#", String.valueOf(selectedCount))
                .replace("#total_amount#", String.valueOf(totalAmount)));
        meta.setDisplayName(name);

        List<String> lore = section.getStringList("lore");
        if (!lore.isEmpty()) {
            List<String> processedLore = new ArrayList<>();
            for (String line : lore) {
                processedLore.add(ChatUtils.colorize(line
                        .replace("#item_name#", displayName)
                        .replace("#player#", player.getName())
                        .replace("#receiver#", targetPlayer)
                        .replace("#current_amount#", String.valueOf(currentAmount))
                        .replace("#selected_amount#", String.valueOf(selectedAmount))
                        .replace("#max_transferable#", String.valueOf(maxTransferable))
                        .replace("#selected_count#", String.valueOf(selectedCount))
                        .replace("#total_amount#", String.valueOf(totalAmount))));
            }
            meta.setLore(processedLore);
        }

        item.setItemMeta(meta);
        return item;
    }

    private ItemStack applyItemMeta(ItemStack item, ConfigurationSection section) {
        return applyItemMeta(item, section, "", 0, 0, 0, 0, 0);
    }

    private ItemStack createPlayerHead(String playerName) {
        ItemStack skull = new ItemStack(Material.PLAYER_HEAD);
        if (playerName == null || playerName.isEmpty() || playerName.equalsIgnoreCase("MHF_Question")) {
            return skull;
        }

        SkullMeta meta = (SkullMeta) skull.getItemMeta();
        if (meta != null) {
            meta.setOwner(playerName);
            skull.setItemMeta(meta);
        }
        return skull;
    }

    private void handleMaterialClick(String itemName, ClickType clickType) {
        if (itemName == null || clickType == null) {
            return;
        }

        int currentAmount = MobStorageManager.getPlayerItem(player, itemName);
        if (currentAmount <= 0) {
            return;
        }

        int selectedAmount = selectedAmounts.getOrDefault(itemName, 0);
        Player receiver = Bukkit.getPlayer(targetPlayer);
        int optimalAmount = receiver != null ?
                MobTransferManager.getOptimalTransferAmount(player, targetPlayer, itemName, currentAmount) :
                currentAmount;
        int newSelectedAmount = selectedAmount;

        switch (clickType) {
            case LEFT:
                if (selectedAmount < optimalAmount) {
                    newSelectedAmount = selectedAmount + 1;
                }
                break;
            case RIGHT:
                newSelectedAmount = Math.min(selectedAmount + 10, optimalAmount);
                break;
            case SHIFT_LEFT:
                newSelectedAmount = Math.max(selectedAmount - 1, 0);
                break;
            case SHIFT_RIGHT:
                newSelectedAmount = Math.max(selectedAmount - 10, 0);
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

        updateGUI();
    }

    private void requestCustomAmount(String itemName) {
        player.closeInventory();
        ChatListener.chat_mob_multi_transfer_item.put(player.getUniqueId(), itemName);
        ChatListener.chat_mob_multi_transfer_target.put(player.getUniqueId(), targetPlayer);
        player.sendMessage(ChatUtils.colorize(
                File.getMessage().getString("mobstorage.transfer.gui_enter_amount")));
    }

    private void requestReceiverInput() {
        player.closeInventory();
        setWaitingForReceiver(player, true);
        player.sendMessage(ChatUtils.colorize(
                File.getMessage().getString("mobstorage.transfer.gui_enter_receiver")));
    }

    public void setSelectedAmount(String itemName, int amount) {
        if (itemName == null || itemName.trim().isEmpty()) {
            return;
        }

        int currentAmount = MobStorageManager.getPlayerItem(player, itemName);
        Player receiver = Bukkit.getPlayer(targetPlayer);
        int optimalAmount = receiver != null ?
                MobTransferManager.getOptimalTransferAmount(player, targetPlayer, itemName, currentAmount) :
                currentAmount;

        int newSelectedAmount = Math.min(Math.max(0, amount), optimalAmount);

        if (newSelectedAmount > 0) {
            selectedAmounts.put(itemName, newSelectedAmount);
        } else {
            selectedAmounts.remove(itemName);
        }
    }

    private void confirmTransfer() {
        if (targetPlayer == null || targetPlayer.isEmpty()) {
            player.sendMessage(ChatUtils.colorize(
                    File.getMessage().getString("mobstorage.transfer.failed_no_receiver")));
            return;
        }

        if (selectedAmounts.isEmpty()) {
            player.sendMessage(ChatUtils.colorize(
                    File.getMessage().getString("mobstorage.transfer.failed_no_selection")));
            return;
        }

        SoundManager.setShouldPlayCloseSound(player, false);
        player.closeInventory();

        if (!MobTransferManager.executeMultiTransfer(player, targetPlayer, selectedAmounts)) {
            player.sendMessage(ChatUtils.colorize(
                    File.getMessage().getString("mobstorage.transfer.failed_multi_all")));
        }

        activeGUIs.remove(player.getUniqueId());
        waitingForReceiver.remove(player.getUniqueId());
    }

    private void cancelTransfer() {
        SoundManager.setShouldPlayCloseSound(player, false);
        player.closeInventory();
        activeGUIs.remove(player.getUniqueId());
        waitingForReceiver.remove(player.getUniqueId());

        try {
            player.openInventory(new MobStorageGUI(player, 0).getInventory());
        } catch (Exception e) {
            Storage.getStorage().getLogger().warning("Error opening mob storage GUI: " + e.getMessage());
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
        List<String> playerItems = getPlayerItems();
        int totalPages = (int) Math.ceil((double) playerItems.size() / itemsPerPage);

        if (currentPage < totalPages - 1) {
            currentPage++;
            updateGUI();
        }
    }

    public void updateGUI() {
        SoundManager.setShouldPlayCloseSound(player, false);
        player.openInventory(getInventory(SoundContext.SILENT));
    }

    private void playOpenSound() {
        FileConfiguration guiConfig = getTransferMultiConfig();
        if (guiConfig.getBoolean("sound.enabled", true)) {
            String soundName = guiConfig.getString("sound.open", "BLOCK_CHEST_OPEN");
            float volume = (float) guiConfig.getDouble("sound.volume", 0.8);
            float pitch = (float) guiConfig.getDouble("sound.pitch", 1.0);
            SoundManager.playSound(player, soundName, volume, pitch);
        }
    }

    private void playClickSound() {
        FileConfiguration guiConfig = getTransferMultiConfig();
        if (guiConfig.getBoolean("sound.enabled", true)) {
            String soundName = guiConfig.getString("sound.click", "UI_BUTTON_CLICK");
            SoundManager.playSound(player, soundName, 1.0f, 1.0f);
        }
    }

    private FileConfiguration getTransferMultiConfig() {
        return File.getMobTransferMultiGUIConfig();
    }

    public String getTargetPlayer() {
        return targetPlayer;
    }

    public void setTargetPlayer(String targetPlayer) {
        this.targetPlayer = targetPlayer != null ? targetPlayer : "";
    }

    public void setTargetPlayerAndUpdate(String targetPlayer) {
        setTargetPlayer(targetPlayer);
        updateGUI();
    }
}
