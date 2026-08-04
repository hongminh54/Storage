package net.danh.storage.GUI.Mob;

import net.danh.storage.GUI.manager.IGUI;
import net.danh.storage.GUI.manager.InteractiveItem;
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
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class MobTransferGUI implements IGUI {
    private static final Map<UUID, MobTransferGUI> activeGUIs = new HashMap<>();
    private static final Map<UUID, Boolean> waitingForInput = new HashMap<>();
    private final Player player;
    private final String itemName;
    private final Inventory inventory;
    private final String targetPlayer;
    private int transferAmount;

    public MobTransferGUI(Player player, String targetPlayer, String itemName) {
        this.player = player;
        this.targetPlayer = targetPlayer != null ? targetPlayer : "";
        this.itemName = itemName;
        this.transferAmount = 1;

        FileConfiguration guiConfig = getTransferConfig();
        String title = ChatUtils.colorize(guiConfig.getString(
                        "title", "&0Transfer to #player#")
                .replace("#player#", this.targetPlayer));
        int size = guiConfig.getInt("size", 6) * 9;

        this.inventory = Bukkit.createInventory(this, size, title);
        activeGUIs.put(player.getUniqueId(), this);
    }

    public static MobTransferGUI getActiveGUI(Player player) {
        if (player == null) return null;
        return activeGUIs.get(player.getUniqueId());
    }

    public static void removeActiveGUI(Player player) {
        if (player == null) {
            return;
        }
        UUID playerId = player.getUniqueId();
        activeGUIs.remove(playerId);
        waitingForInput.remove(playerId);
    }

    public static boolean isWaitingForInput(Player player) {
        if (player == null) return false;
        return waitingForInput.getOrDefault(player.getUniqueId(), false);
    }

    public static void setWaitingForInput(Player player, boolean waiting) {
        if (player == null) return;
        UUID playerId = player.getUniqueId();
        if (waiting) {
            waitingForInput.put(playerId, true);
        } else {
            waitingForInput.remove(playerId);
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
        FileConfiguration guiConfig = getTransferConfig();
        ConfigurationSection itemsSection = guiConfig.getConfigurationSection("items");

        if (itemsSection != null) {
            for (String itemKey : itemsSection.getKeys(false)) {
                ConfigurationSection itemSection = itemsSection.getConfigurationSection(itemKey);
                if (itemSection != null) {
                    setupItem(itemKey, itemSection);
                }
            }
        }
    }

    private void setupItem(String itemKey, ConfigurationSection section) {
        for (int slot : getSlots(section)) {
            InteractiveItem item = createInteractiveItem(itemKey, section, slot);
            if (item != null) {
                inventory.setItem(slot, item);
            }
        }
    }

    private java.util.List<Integer> getSlots(ConfigurationSection section) {
        if (section == null) {
            return java.util.Collections.emptyList();
        }

        if (section.isList("slots")) {
            return section.getIntegerList("slots");
        }

        if (section.isString("slot")) {
            String slotString = section.getString("slot");
            if (slotString == null || slotString.trim().isEmpty()) {
                return java.util.Collections.emptyList();
            }

            java.util.List<Integer> slots = new java.util.ArrayList<>();
            for (String slotStr : slotString.split(",")) {
                try {
                    slots.add(Integer.parseInt(slotStr.trim()));
                } catch (NumberFormatException ignored) {
                }
            }
            return slots;
        }

        if (section.isInt("slot")) {
            return java.util.Collections.singletonList(section.getInt("slot"));
        }

        return java.util.Collections.emptyList();
    }

    private InteractiveItem createInteractiveItem(String itemKey, ConfigurationSection section, int slot) {
        switch (itemKey) {
            case "material_display":
                return createMaterialDisplayItem(section, slot);
            case "player_info":
                return createPlayerInfoItem(section, slot);
            case "confirm":
            case "confirm_transfer":
                return createConfirmItem(section, slot);
            case "cancel":
            case "cancel_transfer":
                return createCancelItem(section, slot);
            case "amount_decrease":
            case "decrease_1":
            case "decrease_10":
            case "decrease_64":
                return createAmountDecreaseItem(section, slot, itemKey);
            case "amount_increase":
            case "increase_1":
            case "increase_10":
            case "increase_64":
                return createAmountIncreaseItem(section, slot, itemKey);
            case "amount_input":
            case "custom_amount":
                return createCustomAmountItem(section, slot);
            case "amount_max":
            case "max_amount":
                return createMaxAmountItem(section, slot);
            default:
                return createDecorativeItem(section, slot);
        }
    }

    private InteractiveItem createMaterialDisplayItem(ConfigurationSection section, int slot) {
        int currentAmount = MobStorageManager.getPlayerItem(player, itemName);
        String displayName = MobStorageManager.getItemDisplayName(itemName);
        Material material = MobStorageManager.resolveMaterial(itemName);
        if (material == null) {
            material = Material.PAPER;
        }

        ItemStack baseItem = new ItemStack(material);
        baseItem = applyItemMeta(baseItem, section, displayName, currentAmount);

        InteractiveItem item = new InteractiveItem(baseItem, slot);
        item.onLeftClick(p -> {
            playClickSound();
            increaseAmount(1);
        });
        item.onRightClick(p -> {
            playClickSound();
            decreaseAmount(1);
        });
        return item;
    }

    private InteractiveItem createPlayerInfoItem(ConfigurationSection section, int slot) {
        ItemStack baseItem = createPlayerHead(targetPlayer == null || targetPlayer.isEmpty() ? "MHF_Question" : targetPlayer);
        baseItem = applyItemMeta(baseItem, section);

        return new InteractiveItem(baseItem, slot);
    }

    private InteractiveItem createConfirmItem(ConfigurationSection section, int slot) {
        String displayName = MobStorageManager.getItemDisplayName(itemName);
        int currentAmount = MobStorageManager.getPlayerItem(player, itemName);

        ItemStack baseItem = new ItemStack(Material.GREEN_STAINED_GLASS_PANE);
        baseItem = applyItemMeta(baseItem, section, displayName, currentAmount);

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

    private InteractiveItem createAmountDecreaseItem(ConfigurationSection section, int slot, String itemKey) {
        int decreaseAmount = getAmountFromKey(itemKey, 1);
        ItemStack baseItem = new ItemStack(Material.REDSTONE);
        baseItem = applyItemMeta(baseItem, section);

        InteractiveItem item = new InteractiveItem(baseItem, slot);
        item.onLeftClick(p -> {
            playClickSound();
            decreaseAmount(decreaseAmount);
        });
        return item;
    }

    private InteractiveItem createAmountIncreaseItem(ConfigurationSection section, int slot, String itemKey) {
        int increaseAmount = getAmountFromKey(itemKey, 1);
        ItemStack baseItem = new ItemStack(Material.EMERALD);
        baseItem = applyItemMeta(baseItem, section);

        InteractiveItem item = new InteractiveItem(baseItem, slot);
        item.onLeftClick(p -> {
            playClickSound();
            increaseAmount(increaseAmount);
        });
        return item;
    }

    private InteractiveItem createCustomAmountItem(ConfigurationSection section, int slot) {
        ItemStack baseItem = new ItemStack(Material.ANVIL);
        baseItem = applyItemMeta(baseItem, section);

        InteractiveItem item = new InteractiveItem(baseItem, slot);
        item.onLeftClick(p -> {
            playClickSound();
            requestCustomAmount();
        });
        return item;
    }

    private InteractiveItem createMaxAmountItem(ConfigurationSection section, int slot) {
        String displayName = MobStorageManager.getItemDisplayName(itemName);
        int currentAmount = MobStorageManager.getPlayerItem(player, itemName);
        Player receiver = Bukkit.getPlayer(targetPlayer);
        int optimalAmount = receiver != null ?
                MobTransferManager.getOptimalTransferAmount(player, targetPlayer, itemName, currentAmount) :
                currentAmount;

        ItemStack baseItem = new ItemStack(Material.BEACON);
        baseItem = applyItemMeta(baseItem, section, displayName, Math.max(0, optimalAmount));

        InteractiveItem item = new InteractiveItem(baseItem, slot);
        item.onLeftClick(p -> {
            playClickSound();
            setMaxAmount();
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

    private ItemStack applyItemMeta(ItemStack item, ConfigurationSection section, String displayName, int currentAmount) {
        if (item == null || section == null) return item;

        org.bukkit.inventory.meta.ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;

        String name = section.getString("name", "&fItem");
        name = ChatUtils.colorize(name
                .replace("#item_name#", displayName)
                .replace("#material_name#", displayName)
                .replace("#player#", player.getName())
                .replace("#receiver#", targetPlayer)
                .replace("#current_amount#", String.valueOf(currentAmount))
                .replace("#transfer_amount#", String.valueOf(transferAmount)));
        meta.setDisplayName(name);

        java.util.List<String> lore = section.getStringList("lore");
        if (!lore.isEmpty()) {
            java.util.List<String> processedLore = new java.util.ArrayList<>();
            for (String line : lore) {
                processedLore.add(ChatUtils.colorize(line
                        .replace("#item_name#", displayName)
                        .replace("#material_name#", displayName)
                        .replace("#player#", player.getName())
                        .replace("#receiver#", targetPlayer)
                        .replace("#current_amount#", String.valueOf(currentAmount))
                        .replace("#transfer_amount#", String.valueOf(transferAmount))));
            }
            meta.setLore(processedLore);
        }

        item.setItemMeta(meta);
        return item;
    }

    private ItemStack applyItemMeta(ItemStack item, ConfigurationSection section) {
        return applyItemMeta(item, section, "", 0);
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

    private int getAmountFromKey(String key, int defaultAmount) {
        if (key == null) return defaultAmount;
        try {
            String numPart = key.replaceAll("[^0-9]", "");
            return numPart.isEmpty() ? defaultAmount : Integer.parseInt(numPart);
        } catch (NumberFormatException e) {
            return defaultAmount;
        }
    }

    private void decreaseAmount(int amount) {
        if (transferAmount > amount) {
            transferAmount -= amount;
        } else {
            transferAmount = 1;
        }
        updateGUI();
    }

    private void increaseAmount(int amount) {
        int currentAmount = MobStorageManager.getPlayerItem(player, itemName);
        Player receiver = Bukkit.getPlayer(targetPlayer);
        int optimalAmount = receiver != null ?
                MobTransferManager.getOptimalTransferAmount(player, targetPlayer, itemName, currentAmount) :
                currentAmount;

        if (transferAmount + amount <= optimalAmount) {
            transferAmount += amount;
        } else {
            transferAmount = optimalAmount;
        }
        updateGUI();
    }

    private void requestCustomAmount() {
        player.closeInventory();
        player.sendMessage(ChatUtils.colorize(File.getMessage().getString("mobstorage.transfer.gui_enter_amount")));
        setWaitingForInput(player, true);
    }

    private void setMaxAmount() {
        int currentAmount = MobStorageManager.getPlayerItem(player, itemName);
        Player receiver = Bukkit.getPlayer(targetPlayer);
        int optimalAmount = receiver != null ?
                MobTransferManager.getOptimalTransferAmount(player, targetPlayer, itemName, currentAmount) :
                currentAmount;
        transferAmount = Math.max(1, optimalAmount);
        updateGUI();
    }

    private void confirmTransfer() {
        if (targetPlayer == null || targetPlayer.isEmpty()) {
            player.sendMessage(ChatUtils.colorize(File.getMessage().getString("mobstorage.transfer.failed_no_receiver")));
            return;
        }

        SoundManager.setShouldPlayCloseSound(player, false);
        player.closeInventory();
        MobTransferManager.executeTransfer(player, targetPlayer, itemName, transferAmount);
        activeGUIs.remove(player.getUniqueId());
        waitingForInput.remove(player.getUniqueId());
    }

    private void cancelTransfer() {
        SoundManager.setShouldPlayCloseSound(player, false);
        player.closeInventory();
        activeGUIs.remove(player.getUniqueId());
        waitingForInput.remove(player.getUniqueId());

        try {
            player.openInventory(new MobItemStorage(player, itemName, 0).getInventory());
        } catch (Exception e) {
            Storage.getStorage().getLogger().warning("Error opening mob item storage GUI: " + e.getMessage());
        }
    }

    public void updateGUI() {
        SoundManager.setShouldPlayCloseSound(player, false);
        player.openInventory(getInventory(SoundContext.SILENT));
    }

    private void playOpenSound() {
        FileConfiguration guiConfig = getTransferConfig();
        if (guiConfig.getBoolean("sound.enabled", true)) {
            String soundName = guiConfig.getString("sound.open", "BLOCK_CHEST_OPEN");
            float volume = (float) guiConfig.getDouble("sound.volume", 0.8);
            float pitch = (float) guiConfig.getDouble("sound.pitch", 1.0);
            SoundManager.playSound(player, soundName, volume, pitch);
        }
    }

    private void playClickSound() {
        FileConfiguration guiConfig = getTransferConfig();
        if (guiConfig.getBoolean("sound.enabled", true)) {
            String soundName = guiConfig.getString("sound.click", "UI_BUTTON_CLICK");
            SoundManager.playSound(player, soundName, 1.0f, 1.0f);
        }
    }

    private FileConfiguration getTransferConfig() {
        return File.getMobTransferGUIConfig();
    }

    public void setTransferAmount(int amount) {
        int currentAmount = MobStorageManager.getPlayerItem(player, itemName);
        this.transferAmount = Math.min(Math.max(1, amount), currentAmount);
    }

    public void setTransferAmountAndUpdate(int amount) {
        setTransferAmount(amount);
        updateGUI();
    }

    public String getTargetPlayer() {
        return targetPlayer;
    }

    public String getItemName() {
        return itemName;
    }
}
