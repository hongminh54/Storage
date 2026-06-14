package net.danh.storage.Action;

import net.danh.storage.API.events.MobStorageWithdrawEvent;
import net.danh.storage.Manager.Mob.MobStorageManager;
import net.danh.storage.NMS.NMSAssistant;
import net.danh.storage.Utils.ChatUtils;
import net.danh.storage.Utils.File;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Locale;

public class MobWithdraw {
    private final Player player;
    private final String itemName;
    private final long amount;

    public MobWithdraw(Player player, String itemName, long amount) {
        this.player = player;
        this.itemName = itemName;
        this.amount = amount;
    }

    public void doAction() {
        if (player == null || !player.isOnline() || itemName == null || itemName.trim().isEmpty() || amount <= 0) {
            return;
        }

        String upper = itemName.toUpperCase(Locale.ENGLISH);
        int currentAmount = MobStorageManager.getPlayerItem(player, upper);
        int requestedAmount = amount > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) amount;

        MobStorageWithdrawEvent event = new MobStorageWithdrawEvent(player, upper, requestedAmount);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled() || event.getAmount() <= 0) {
            return;
        }
        requestedAmount = event.getAmount();

        if (currentAmount < requestedAmount) {
            sendMessage("mobstorage.action.withdraw.not_enough", "#amount#", String.valueOf(currentAmount));
            return;
        }

        Material material = Material.getMaterial(upper);
        if (material == null) {
            sendMessage("mobstorage.invalid_item", "#item#", MobStorageManager.getItemDisplayName(upper));
            return;
        }

        int withdrawn = 0;
        int remaining = Math.min(requestedAmount, currentAmount);
        while (remaining > 0) {
            int batchSize = Math.min(remaining, material.getMaxStackSize());
            HashMap<Integer, ItemStack> notFit = player.getInventory().addItem(new ItemStack(material, batchSize));
            if (!notFit.isEmpty()) {
                int notAdded = 0;
                for (ItemStack item : notFit.values()) {
                    notAdded += item.getAmount();
                }
                withdrawn += batchSize - notAdded;
                sendMessage("mobstorage.action.withdraw.inventory_full");
                break;
            }
            withdrawn += batchSize;
            remaining -= batchSize;
        }

        if (withdrawn > 0) {
            MobStorageManager.removeItemAmount(player, upper, withdrawn, false);
            if (new NMSAssistant().isVersionLessThan(9)) {
                player.updateInventory();
            }
            String[] placeholders = {"#amount#", "#material#", "#item_amount#", "#max_storage#"};
            String[] replacements = {
                    String.valueOf(withdrawn),
                    MobStorageManager.getItemDisplayName(upper),
                    String.valueOf(MobStorageManager.getPlayerItem(player, upper)),
                    String.valueOf(MobStorageManager.getMaxStorage(player))
            };
            sendMessage("mobstorage.action.withdraw.withdraw_item", placeholders, replacements);
        }
    }

    private void sendMessage(String key) {
        String message = File.getMessage().getString(key, "");
        if (!message.isEmpty()) {
            player.sendMessage(ChatUtils.colorizewp(player, message));
        }
    }

    private void sendMessage(String key, String placeholder, String replacement) {
        String message = File.getMessage().getString(key, "");
        if (!message.isEmpty()) {
            player.sendMessage(ChatUtils.colorizewp(player, message.replace(placeholder, replacement)));
        }
    }

    private void sendMessage(String key, String[] placeholders, String[] replacements) {
        String message = File.getMessage().getString(key, "");
        if (!message.isEmpty()) {
            for (int i = 0; i < placeholders.length && i < replacements.length; i++) {
                message = message.replace(placeholders[i], replacements[i]);
            }
            player.sendMessage(ChatUtils.colorizewp(player, message));
        }
    }
}
