package net.danh.storage.Action;

import net.danh.storage.Manager.MythicStorageManager;
import net.danh.storage.MythicMobs.MythicMobsHelper;
import net.danh.storage.NMS.NMSAssistant;
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
                player.sendMessage(Chat.colorizewp(message));
            }
            return;
        }

        MythicMobsHelper helper = MythicStorageManager.getMythicMobsHelper();
        if (helper == null || !helper.isInitialized()) {
            String message = File.getMessage().getString("mythicstorage.system_disabled", "");
            if (!message.isEmpty()) {
                player.sendMessage(Chat.colorizewp(message));
            }
            return;
        }

        String displayName = helper.getItemDisplayName(itemName);
        if (displayName == null || displayName.trim().isEmpty()) displayName = itemName;

        ItemStack mythicItem = helper.getMythicItem(itemName);
        if (mythicItem == null) {
            String message = File.getMessage().getString("mythicstorage.invalid_item", "")
                    .replace("#item#", displayName);
            if (!message.isEmpty()) {
                player.sendMessage(Chat.colorizewp(message));
            }
            return;
        }

        int withdrawn = 0;
        int toWithdraw = (int) Math.min(amount, currentAmount);

        for (int i = 0; i < toWithdraw; i++) {
            ItemStack itemToGive = mythicItem.clone();
            itemToGive.setAmount(1);

            HashMap<Integer, ItemStack> notFit = player.getInventory().addItem(itemToGive);

            if (!notFit.isEmpty()) {
                String message = File.getMessage().getString("mythicstorage.action.withdraw.inventory_full", "");
                if (!message.isEmpty()) {
                    player.sendMessage(Chat.colorizewp(message));
                }
                break;
            }

            withdrawn++;
        }

        if (withdrawn > 0) {
            MythicStorageManager.removeItemAmount(player, itemName, withdrawn);

            if (new NMSAssistant().isVersionLessThan(9)) {
                player.updateInventory();
            }

            String message = File.getMessage().getString("mythicstorage.action.withdraw.withdraw_item", "")
                    .replace("#amount#", String.valueOf(withdrawn))
                    .replace("#material#", displayName)
                    .replace("#item_amount#", String.valueOf(MythicStorageManager.getPlayerItem(player, itemName)))
                    .replace("#max_storage#", String.valueOf(MythicStorageManager.getMaxStorage(player)));
            if (!message.isEmpty()) {
                player.sendMessage(Chat.colorizewp(message));
            }
        }
    }
}
