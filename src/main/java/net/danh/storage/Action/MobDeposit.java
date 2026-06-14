package net.danh.storage.Action;

import net.danh.storage.API.events.MobStorageDepositEvent;
import net.danh.storage.Manager.Mob.MobStorageManager;
import net.danh.storage.NMS.NMSAssistant;
import net.danh.storage.Utils.ChatUtils;
import net.danh.storage.Utils.File;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.util.Locale;

public class MobDeposit {
    private final Player player;
    private final String itemName;
    private final long amount;

    public MobDeposit(Player player, String itemName, long amount) {
        this.player = player;
        this.itemName = itemName;
        this.amount = amount;
    }

    public void doAction() {
        if (player == null || !player.isOnline() || itemName == null || itemName.trim().isEmpty() || amount <= 0) {
            return;
        }

        String upper = itemName.toUpperCase(Locale.ENGLISH);
        Material material = Material.getMaterial(upper);
        if (material == null) {
            sendMessage("mobstorage.invalid_item", "#item#", MobStorageManager.getItemDisplayName(upper));
            return;
        }

        int requestedAmount = amount > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) amount;
        MobStorageDepositEvent event = new MobStorageDepositEvent(player, upper, requestedAmount);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled() || event.getAmount() <= 0) {
            return;
        }

        requestedAmount = event.getAmount();
        PlayerInventory inv = player.getInventory();
        ItemStack[] contents = inv.getContents();
        if (contents == null) {
            return;
        }

        long deposited = 0;
        long toDeposit = requestedAmount;

        for (int i = 0; i < contents.length && toDeposit > 0; i++) {
            ItemStack item = contents[i];
            if (item == null || item.getType() != material) {
                continue;
            }
            if (!File.getMobStorageConfig().getBoolean("settings.allow_custom_item_meta", false)
                    && item.hasItemMeta()) {
                continue;
            }

            int canDeposit = (int) Math.min(item.getAmount(), toDeposit);
            if (MobStorageManager.addItemAmount(player, upper, canDeposit, false)) {
                if (canDeposit == item.getAmount()) {
                    contents[i] = null;
                } else {
                    item.setAmount(item.getAmount() - canDeposit);
                }
                deposited += canDeposit;
                toDeposit -= canDeposit;
            } else {
                sendFullMessage(upper, canDeposit);
                break;
            }
        }

        if (deposited > 0) {
            inv.setContents(contents);
            if (new NMSAssistant().isVersionLessThan(9)) {
                player.updateInventory();
            }
            String[] placeholders = {"#amount#", "#material#", "#item_amount#", "#max_storage#"};
            String[] replacements = {
                    String.valueOf(deposited),
                    MobStorageManager.getItemDisplayName(upper),
                    String.valueOf(MobStorageManager.getPlayerItem(player, upper)),
                    String.valueOf(MobStorageManager.getMaxStorage(player))
            };
            sendMessage("mobstorage.action.deposit.deposit_item", placeholders, replacements);
        } else {
            sendMessage("mobstorage.action.deposit.no_items");
        }
    }

    private void sendFullMessage(String itemName, int amount) {
        String[] placeholders = {"#amount#", "#material#", "#item_amount#", "#max_storage#"};
        String[] replacements = {
                String.valueOf(amount),
                MobStorageManager.getItemDisplayName(itemName),
                String.valueOf(MobStorageManager.getPlayerItem(player, itemName)),
                String.valueOf(MobStorageManager.getMaxStorage(player))
        };
        sendMessage("mobstorage.action.deposit.full_storage", placeholders, replacements);
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
