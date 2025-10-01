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
        int currentAmount = MythicStorageManager.getPlayerItem(player, itemName);
        
        if (currentAmount < amount) {
            player.sendMessage(Chat.colorize(File.getMessage().getString("mythicstorage.action.withdraw.not_enough")
                .replace("#amount#", String.valueOf(currentAmount))));
            return;
        }

        MythicMobsHelper helper = MythicStorageManager.getMythicMobsHelper();
        if (helper == null || !helper.isInitialized()) {
            player.sendMessage(Chat.colorize(File.getMessage().getString("mythicstorage.system_disabled")));
            return;
        }

        ItemStack mythicItem = helper.getMythicItem(itemName);
        if (mythicItem == null) {
            player.sendMessage(Chat.colorize(File.getMessage().getString("mythicstorage.invalid_item")
                .replace("#item#", itemName)));
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
                player.sendMessage(Chat.colorize(File.getMessage().getString("mythicstorage.action.withdraw.inventory_full")));
                break;
            }
            
            remaining -= toGive;
            given += toGive;
        }
        
        if (given > 0) {
            MythicStorageManager.removeItemAmount(player, itemName, (int) given);
            String message = File.getMessage().getString("mythicstorage.action.withdraw.withdraw_item")
                .replace("#amount#", String.valueOf(given))
                .replace("#material#", itemName)
                .replace("#item_amount#", String.valueOf(MythicStorageManager.getPlayerItem(player, itemName)))
                .replace("#max_storage#", String.valueOf(MythicStorageManager.getMaxStorage(player)));
            player.sendMessage(Chat.colorize(message));
        }
    }
}
