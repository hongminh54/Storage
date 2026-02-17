package net.danh.storage.Action;

import net.danh.storage.Manager.Crop.CropStorageManager;
import net.danh.storage.NMS.NMSAssistant;
import net.danh.storage.Utils.ChatUtils;
import net.danh.storage.Utils.File;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;

public class CropWithdraw {
    private final Player player;
    private final String itemName;
    private final long amount;

    public CropWithdraw(Player player, String itemName, long amount) {
        this.player = player;
        this.itemName = itemName;
        this.amount = amount;
    }

    public void doAction() {
        if (player == null || !player.isOnline())
            return;
        if (itemName == null || itemName.trim().isEmpty())
            return;
        if (amount <= 0)
            return;

        int currentAmount = CropStorageManager.getPlayerItem(player, itemName);

        if (currentAmount < amount) {
            String message = File.getMessage().getString("cropstorage.action.withdraw.not_enough", "")
                    .replace("#amount#", String.valueOf(currentAmount));
            if (!message.isEmpty()) {
                player.sendMessage(ChatUtils.colorizewp(message));
            }
            return;
        }

        Material material;
        try {
            material = Material.valueOf(itemName.toUpperCase());
        } catch (IllegalArgumentException e) {
            String displayName = CropStorageManager.getItemDisplayName(itemName);
            String message = File.getMessage().getString("cropstorage.invalid_item", "")
                    .replace("#item#", displayName);
            if (!message.isEmpty()) {
                player.sendMessage(ChatUtils.colorizewp(message));
            }
            return;
        }

        String displayName = CropStorageManager.getItemDisplayName(itemName);

        int withdrawn = 0;
        int toWithdraw = (int) Math.min(amount, currentAmount);

        int remaining = toWithdraw;
        while (remaining > 0) {
            int batchSize = Math.min(remaining, material.getMaxStackSize());
            ItemStack itemToGive = new ItemStack(material, batchSize);

            HashMap<Integer, ItemStack> notFit = player.getInventory().addItem(itemToGive);

            if (!notFit.isEmpty()) {
                int notAdded = notFit.values().stream().mapToInt(ItemStack::getAmount).sum();
                withdrawn += (batchSize - notAdded);
                String message = File.getMessage().getString("cropstorage.action.withdraw.inventory_full", "");
                if (!message.isEmpty()) {
                    player.sendMessage(ChatUtils.colorizewp(message));
                }
                break;
            }

            withdrawn += batchSize;
            remaining -= batchSize;
        }

        if (withdrawn > 0) {
            CropStorageManager.removeItemAmount(player, itemName, withdrawn);

            if (new NMSAssistant().isVersionLessThan(9)) {
                player.updateInventory();
            }

            String message = File.getMessage().getString("cropstorage.action.withdraw.withdraw_item", "")
                    .replace("#amount#", String.valueOf(withdrawn))
                    .replace("#material#", displayName)
                    .replace("#item_amount#", String.valueOf(CropStorageManager.getPlayerItem(player, itemName)))
                    .replace("#max_storage#", String.valueOf(CropStorageManager.getMaxStorage(player)));
            if (!message.isEmpty()) {
                player.sendMessage(ChatUtils.colorizewp(message));
            }
        }
    }
}
