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
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

public class TransferGUI implements IGUI {
    private static final Map<Player, TransferGUI> activeGUIs = new HashMap<>();
    private static final Map<Player, Boolean> waitingForInput = new HashMap<>();
    private final Player player;
    private final String targetPlayer;
    private final String material;
    private final Inventory inventory;
    private int transferAmount;

    public TransferGUI(Player player, String targetPlayer, String material) {
        this.player = player;
        this.targetPlayer = targetPlayer;
        this.material = material;
        this.transferAmount = 1;

        FileConfiguration guiConfig = getTransferConfig();
        String title = Chat.colorizewp(guiConfig.getString("title", "&0Transfer to #player#")
                .replace("#player#", targetPlayer));
        int size = guiConfig.getInt("size", 6) * 9;

        this.inventory = Bukkit.createInventory(this, size, title);
        activeGUIs.put(player, this);
    }

    public static TransferGUI getActiveGUI(Player player) {
        return activeGUIs.get(player);
    }

    public static boolean isWaitingForInput(Player player) {
        return waitingForInput.getOrDefault(player, false);
    }

    public static void setWaitingForInput(Player player, boolean waiting) {
        if (waiting) {
            waitingForInput.put(player, true);
        } else {
            waitingForInput.remove(player);
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
        String slotString = section.getString("slot");
        if (slotString == null) return;

        String[] slots = slotString.split(",");
        for (String slotStr : slots) {
            try {
                int slot = Integer.parseInt(slotStr.trim());
                InteractiveItem item = createInteractiveItem(itemKey, section, slot);
                if (item != null) {
                    inventory.setItem(slot, item);
                }
            } catch (NumberFormatException ignored) {
            }
        }
    }

    private InteractiveItem createInteractiveItem(String itemKey, ConfigurationSection section, int slot) {
        switch (itemKey) {
            case "material_display":
                return createMaterialDisplayItem(section, slot);
            case "player_info":
                return createPlayerInfoItem(section, slot);
            case "confirm_transfer":
                return createConfirmItem(section, slot);
            case "cancel_transfer":
                return createCancelItem(section, slot);
            case "amount_decrease":
                return createAmountDecreaseItem(section, slot);
            case "amount_increase":
                return createAmountIncreaseItem(section, slot);
            case "amount_input":
                return createAmountInputItem(section, slot);
            case "amount_max":
                return createAmountMaxItem(section, slot);
            default:
                return createDecorativeItem(section, slot);
        }
    }

    private InteractiveItem createMaterialDisplayItem(ConfigurationSection section, int slot) {
        int currentAmount = MineManager.getPlayerBlock(player, material);
        String materialName = getMaterialDisplayName();

        try {
            ItemStack baseItem = ItemManager.getItemConfig(player, material, materialName, section);
            baseItem = ItemManager.replaceLore(baseItem, section.getStringList("lore"),
                    "#material_name#", materialName,
                    "#current_amount#", String.valueOf(currentAmount),
                    "#transfer_amount#", String.valueOf(transferAmount));

            InteractiveItem item = new InteractiveItem(baseItem, slot);
            item.onLeftClick(p -> {
                SoundManager.playItemSound(p, getTransferConfig(), "items.material_display", SoundContext.INITIAL_OPEN);
                handleClick("material_display");
            });
            return item;
        } catch (Exception e) {
            // Fallback to basic item if material creation fails
            ItemStack fallbackItem = ItemManager.getItemConfig(player, section);
            fallbackItem = ItemManager.replaceLore(fallbackItem, section.getStringList("lore"),
                    "#material_name#", materialName,
                    "#current_amount#", String.valueOf(currentAmount),
                    "#transfer_amount#", String.valueOf(transferAmount));

            InteractiveItem item = new InteractiveItem(fallbackItem, slot);
            item.onLeftClick(p -> {
                SoundManager.playItemSound(p, getTransferConfig(), "items.material_display", SoundContext.INITIAL_OPEN);
                handleClick("material_display");
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
        String materialName = getMaterialDisplayName();
        ItemStack baseItem = ItemManager.getItemConfig(player, section);
        baseItem = ItemManager.replaceLore(baseItem, section.getStringList("lore"),
                "#transfer_amount#", String.valueOf(transferAmount),
                "#material_name#", materialName,
                "#player#", targetPlayer);
        baseItem = ItemManager.replacePlaceholders(baseItem,
                "#transfer_amount#", String.valueOf(transferAmount),
                "#material_name#", materialName,
                "#player#", targetPlayer);

        InteractiveItem item = new InteractiveItem(baseItem, slot);
        item.onLeftClick(p -> {
            SoundManager.playItemSound(p, getTransferConfig(), "items.confirm_transfer", SoundContext.INITIAL_OPEN);
            handleClick("confirm_transfer");
        });
        return item;
    }

    private InteractiveItem createCancelItem(ConfigurationSection section, int slot) {
        ItemStack baseItem = ItemManager.getItemConfig(player, section);
        InteractiveItem item = new InteractiveItem(baseItem, slot);
        item.onLeftClick(p -> {
            SoundManager.playItemSound(p, getTransferConfig(), "items.cancel_transfer", SoundContext.INITIAL_OPEN);
            handleClick("cancel_transfer");
        });
        return item;
    }

    private InteractiveItem createAmountDecreaseItem(ConfigurationSection section, int slot) {
        ItemStack baseItem = ItemManager.getItemConfig(player, section);
        baseItem = ItemManager.replaceLore(baseItem, section.getStringList("lore"),
                "#transfer_amount#", String.valueOf(transferAmount));

        InteractiveItem item = new InteractiveItem(baseItem, slot);
        item.onLeftClick(p -> {
            SoundManager.playItemSound(p, getTransferConfig(), "items.amount_decrease", SoundContext.INITIAL_OPEN);
            handleClick("amount_decrease");
        });
        return item;
    }

    private InteractiveItem createAmountIncreaseItem(ConfigurationSection section, int slot) {
        ItemStack baseItem = ItemManager.getItemConfig(player, section);
        baseItem = ItemManager.replaceLore(baseItem, section.getStringList("lore"),
                "#transfer_amount#", String.valueOf(transferAmount));

        InteractiveItem item = new InteractiveItem(baseItem, slot);
        item.onLeftClick(p -> {
            SoundManager.playItemSound(p, getTransferConfig(), "items.amount_increase", SoundContext.INITIAL_OPEN);
            handleClick("amount_increase");
        });
        return item;
    }

    private InteractiveItem createAmountInputItem(ConfigurationSection section, int slot) {
        ItemStack baseItem = ItemManager.getItemConfig(player, section);
        InteractiveItem item = new InteractiveItem(baseItem, slot);
        item.onLeftClick(p -> {
            SoundManager.playItemSound(p, getTransferConfig(), "items.amount_input", SoundContext.INITIAL_OPEN);
            handleClick("amount_input");
        });
        return item;
    }

    private InteractiveItem createAmountMaxItem(ConfigurationSection section, int slot) {
        int currentAmount = MineManager.getPlayerBlock(player, material);
        ItemStack baseItem = ItemManager.getItemConfig(player, section);
        baseItem = ItemManager.replaceLore(baseItem, section.getStringList("lore"),
                "#current_amount#", String.valueOf(currentAmount));

        InteractiveItem item = new InteractiveItem(baseItem, slot);
        item.onLeftClick(p -> {
            SoundManager.playItemSound(p, getTransferConfig(), "items.amount_max", SoundContext.INITIAL_OPEN);
            handleClick("amount_max");
        });
        return item;
    }

    private InteractiveItem createDecorativeItem(ConfigurationSection section, int slot) {
        ItemStack baseItem = ItemManager.getItemConfig(player, section);
        return new InteractiveItem(baseItem, slot);
    }

    private void handleClick(String itemKey) {
        switch (itemKey) {
            case "amount_decrease":
                decreaseAmount();
                break;
            case "amount_increase":
                increaseAmount();
                break;
            case "amount_input":
                requestCustomAmount();
                break;
            case "amount_max":
                setMaxAmount();
                break;
            case "confirm_transfer":
                confirmTransfer();
                break;
            case "cancel_transfer":
                cancelTransfer();
                break;
        }
    }

    private void decreaseAmount() {
        if (transferAmount > 1) {
            transferAmount--;
            updateGUI();
        }
    }

    private void increaseAmount() {
        int currentAmount = MineManager.getPlayerBlock(player, material);
        int optimalAmount = TransferManager.getOptimalTransferAmount(player, targetPlayer, material, currentAmount);

        if (transferAmount < optimalAmount) {
            transferAmount++;
            updateGUI();
        }
    }

    private void requestCustomAmount() {
        player.closeInventory();
        player.sendMessage(Chat.colorize(File.getMessage().getString("transfer.gui_enter_amount")));
        waitingForInput.put(player, true);
    }

    private void setMaxAmount() {
        int currentAmount = MineManager.getPlayerBlock(player, material);
        int optimalAmount = TransferManager.getOptimalTransferAmount(player, targetPlayer, material, currentAmount);
        transferAmount = optimalAmount;
        updateGUI();
    }

    private void confirmTransfer() {
        player.closeInventory();
        TransferManager.executeTransfer(player, targetPlayer, material, transferAmount);
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

    public void updateGUI() {
        player.openInventory(getInventory(SoundContext.SILENT));
    }

    private void playOpenSound() {
        FileConfiguration guiConfig = getTransferConfig();
        ConfigurationSection soundSection = guiConfig.getConfigurationSection("gui_open_sound");
        if (soundSection != null && soundSection.getBoolean("enabled", true)) {
            String soundName = soundSection.getString("name");
            float volume = (float) soundSection.getDouble("volume", 0.8);
            float pitch = (float) soundSection.getDouble("pitch", 1.0);
            SoundManager.playSound(player, soundName, volume, pitch);
        }
    }

    private String getMaterialDisplayName() {
        FileConfiguration config = File.getConfig();
        String displayName = config.getString("items." + material);
        return displayName != null ? displayName : material.replace(";0", "").replace("_", " ");
    }

    private FileConfiguration getTransferConfig() {
        return File.getFileSetting().get("GUI/transfer.yml");
    }

    public void setTransferAmount(int amount) {
        int currentAmount = MineManager.getPlayerBlock(player, material);
        this.transferAmount = Math.min(Math.max(1, amount), currentAmount);
    }

    public void setTransferAmountAndUpdate(int amount) {
        setTransferAmount(amount);
        updateGUI();
    }

    public String getTargetPlayer() {
        return targetPlayer;
    }

    public String getMaterial() {
        return material;
    }
}
