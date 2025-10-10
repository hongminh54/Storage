package net.danh.storage.Action;

import net.danh.storage.Manager.MythicStorageManager;
import net.danh.storage.MythicMobs.MythicMobsHelper;
import net.danh.storage.Utils.Chat;
import net.danh.storage.Utils.File;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;

public class MythicWithdraw {
    private final Player player;
    private final String itemName;
    private final long amount;

    public MythicWithdraw(Player player, String itemName, long amount) {
        this.player = player;
        this.itemName = itemName;
        this.amount = amount;
    }

    public void doAction() {
        if (player == null || !player.isOnline()) return;
        if (itemName == null || itemName.trim().isEmpty()) return;
        if (amount <= 0) return;

        int currentAmount = MythicStorageManager.getPlayerItem(player, itemName);

        if (currentAmount < amount) {
            String message = File.getMessage().getString("mythicstorage.action.withdraw.not_enough", "")
                    .replace("#amount#", String.valueOf(currentAmount));
            if (!message.isEmpty()) {
                player.sendMessage(Chat.colorize(message));
            }
            return;
        }

        MythicMobsHelper helper = MythicStorageManager.getMythicMobsHelper();
        if (helper == null || !helper.isInitialized()) {
            String message = File.getMessage().getString("mythicstorage.system_disabled", "");
            if (!message.isEmpty()) {
                player.sendMessage(Chat.colorize(message));
            }
            return;
        }

        ItemStack mythicItem = helper.getMythicItem(itemName);
        if (mythicItem == null) {
            String displayName = helper.getItemDisplayName(itemName);
            if (displayName == null || displayName.trim().isEmpty()) displayName = itemName;
            String message = File.getMessage().getString("mythicstorage.invalid_item", "")
                    .replace("#item#", displayName);
            if (!message.isEmpty()) {
                player.sendMessage(Chat.colorize(message));
            }
            return;
        }

        int maxStackSize = mythicItem.getMaxStackSize();
        long remaining = amount;
        long given = 0;

        while (remaining > 0) {
            int toGive = (int) Math.min(remaining, maxStackSize);
            mythicItem.setAmount(toGive);

            HashMap<Integer, ItemStack> notFit = player.getInventory().addItem(mythicItem.clone());
            if (!notFit.isEmpty()) {
                String message = File.getMessage().getString("mythicstorage.action.withdraw.inventory_full", "");
                if (!message.isEmpty()) {
                    player.sendMessage(Chat.colorize(message));
                }
                break;
            }

            remaining -= toGive;
            given += toGive;
        }

        if (given > 0) {
            MythicStorageManager.removeItemAmount(player, itemName, (int) given);
            String displayName = helper.getItemDisplayName(itemName);
            if (displayName == null || displayName.trim().isEmpty()) displayName = itemName;
            String message = File.getMessage().getString("mythicstorage.action.withdraw.withdraw_item", "")
                    .replace("#amount#", String.valueOf(given))
                    .replace("#material#", displayName)
                    .replace("#item_amount#", String.valueOf(MythicStorageManager.getPlayerItem(player, itemName)))
                    .replace("#max_storage#", String.valueOf(MythicStorageManager.getMaxStorage(player)));
            if (!message.isEmpty()) {
                player.sendMessage(Chat.colorize(message));
            }
        }
    }
}
