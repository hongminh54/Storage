package net.danh.storage.GUI;

import net.danh.storage.GUI.manager.IGUI;
import net.danh.storage.GUI.manager.InteractiveItem;
import net.danh.storage.Manager.ItemManager;
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
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class ConfirmationGUI implements IGUI {

    private final Player player;
    private final String message;
    private final Runnable onConfirm;
    private final Runnable onCancel;
    private final FileConfiguration config;

    public ConfirmationGUI(Player player, String message, Runnable onConfirm, Runnable onCancel) {
        this.player = player;
        this.message = message;
        this.onConfirm = onConfirm;
        this.onCancel = onCancel;
        this.config = File.getConfirmationGUIConfig();
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

        String title = ChatUtils.colorizewp(player, Objects.requireNonNull(
                config.getString("title")));
        Inventory inventory = Bukkit.createInventory(this, config.getInt("size") * 9, title);

        setupItems(inventory);
        return inventory;
    }

    private void setupItems(Inventory inventory) {
        setupDecorativeItems(inventory);
        addMessageDisplay(inventory);
        addConfirmButton(inventory);
        addCancelButton(inventory);
    }

    private void setupDecorativeItems(Inventory inventory) {
        String decorateSlots = config.getString("items.decorates.slot");
        if (decorateSlots != null) {
            for (String slotStr : decorateSlots.split(",")) {
                int slot = Number.getInteger(slotStr.trim());
                InteractiveItem decorateItem = new InteractiveItem(
                        ItemManager.getItemConfig(Objects.requireNonNull(config.getConfigurationSection("items.decorates"))),
                        slot
                );
                inventory.setItem(decorateItem.getSlot(), decorateItem);
            }
        }
    }

    private void addMessageDisplay(Inventory inventory) {
        ItemStack messageItem = ItemManager.getItemConfig(
                Objects.requireNonNull(config.getConfigurationSection("items.message"))
        );

        if (messageItem != null) {
            ItemMeta meta = messageItem.getItemMeta();
            if (meta != null) {
                List<String> lore = meta.getLore();
                if (lore == null) lore = new ArrayList<>();

                List<String> processedLore = new ArrayList<>();
                for (String line : lore) {
                    processedLore.add(ChatUtils.colorizewp(line.replace("#message#", message)));
                }
                meta.setLore(processedLore);
                messageItem.setItemMeta(meta);
            }
        }

        String slotConfig = config.getString("items.message.slot", "4");
        for (String slotStr : slotConfig.split(",")) {
            int slot = Number.getInteger(slotStr.trim());
            InteractiveItem messageDisplay = new InteractiveItem(messageItem, slot);
            inventory.setItem(messageDisplay.getSlot(), messageDisplay);
        }
    }

    private void addConfirmButton(Inventory inventory) {
        String slotConfig = config.getString("items.confirm.slot", "11");
        for (String slotStr : slotConfig.split(",")) {
            int slot = Number.getInteger(slotStr.trim());
            InteractiveItem confirmButton = new InteractiveItem(
                    ItemManager.getItemConfig(Objects.requireNonNull(config.getConfigurationSection("items.confirm"))),
                    slot
            ).onLeftClick(p -> confirm(p));
            inventory.setItem(confirmButton.getSlot(), confirmButton);
        }
    }

    private void addCancelButton(Inventory inventory) {
        String slotConfig = config.getString("items.cancel.slot", "15");
        for (String slotStr : slotConfig.split(",")) {
            int slot = Number.getInteger(slotStr.trim());
            InteractiveItem cancelButton = new InteractiveItem(
                    ItemManager.getItemConfig(Objects.requireNonNull(config.getConfigurationSection("items.cancel"))),
                    slot
            ).onLeftClick(p -> cancel(p));
            inventory.setItem(cancelButton.getSlot(), cancelButton);
        }
    }

    private void confirm(Player player) {
        SoundManager.playSound(player, SoundManager.SoundType.ACTION_SUCCESS);
        player.closeInventory();
        if (onConfirm != null) {
            onConfirm.run();
        }
    }

    private void cancel(Player player) {
        SoundManager.playSound(player, SoundManager.SoundType.ACTION_ERROR);
        player.closeInventory();
        if (onCancel != null) {
            onCancel.run();
        }
    }
}
