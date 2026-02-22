package net.danh.storage.Action;

import net.danh.storage.API.events.CropStorageDepositEvent;
import net.danh.storage.Manager.Crop.CropStorageManager;
import net.danh.storage.NMS.NMSAssistant;
import net.danh.storage.Utils.ChatUtils;
import net.danh.storage.Utils.File;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

public class CropDeposit {
    private final Player player;
    private final String itemName;
    private final long amount;

    public CropDeposit(Player player, String itemName, long amount) {
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

        Material material;
        try {
            material = Material.valueOf(itemName.toUpperCase());
        } catch (IllegalArgumentException e) {
            String displayName = CropStorageManager.getItemDisplayName(itemName);
            player.sendMessage(ChatUtils.colorizewp(
                    File.getMessage().getString("cropstorage.invalid_item", "")
                            .replace("#item#", displayName)));
            return;
        }

        PlayerInventory inv = player.getInventory();
        ItemStack[] contents = inv.getContents();
        if (contents == null)
            return;

        String displayName = CropStorageManager.getItemDisplayName(itemName);
        long deposited = 0;
        long toDeposit = amount;

        int requestedAmount;
        if (toDeposit > Integer.MAX_VALUE) {
            requestedAmount = Integer.MAX_VALUE;
        } else {
            requestedAmount = (int) toDeposit;
        }

        CropStorageDepositEvent event = new CropStorageDepositEvent(player, itemName.toUpperCase(), requestedAmount);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            return;
        }

        requestedAmount = event.getAmount();
        if (requestedAmount <= 0) {
            return;
        }
        toDeposit = requestedAmount;

        for (int i = 0; i < contents.length && toDeposit > 0; i++) {
            ItemStack item = contents[i];
            if (item == null)
                continue;
            if (item.getType() != material)
                continue;
            // Only deposit items without custom metadata (vanilla items only)
            if (item.hasItemMeta() && item.getItemMeta().hasDisplayName())
                continue;

            int itemAmount = item.getAmount();
            int canDeposit = (int) Math.min(itemAmount, toDeposit);

            if (CropStorageManager.addItemAmount(player, itemName, canDeposit, false)) {
                if (canDeposit == itemAmount) {
                    contents[i] = null;
                } else {
                    item.setAmount(itemAmount - canDeposit);
                }
                deposited += canDeposit;
                toDeposit -= canDeposit;
            } else {
                String message = File.getMessage().getString("cropstorage.action.deposit.full_storage", "")
                        .replace("#item_amount#", String.valueOf(CropStorageManager.getPlayerItem(player, itemName)))
                        .replace("#max_storage#", String.valueOf(CropStorageManager.getMaxStorage(player)))
                        .replace("#amount#", String.valueOf(canDeposit))
                        .replace("#material#", displayName);
                if (!message.isEmpty()) {
                    player.sendMessage(ChatUtils.colorizewp(message));
                }
                break;
            }
        }

        if (deposited > 0) {
            inv.setContents(contents);
            if (new NMSAssistant().isVersionLessThan(9)) {
                player.updateInventory();
            }

            String message = File.getMessage().getString("cropstorage.action.deposit.deposit_item", "")
                    .replace("#amount#", String.valueOf(deposited))
                    .replace("#material#", displayName)
                    .replace("#item_amount#", String.valueOf(CropStorageManager.getPlayerItem(player, itemName)))
                    .replace("#max_storage#", String.valueOf(CropStorageManager.getMaxStorage(player)));
            if (!message.isEmpty()) {
                player.sendMessage(ChatUtils.colorizewp(message));
            }
        } else {
            String message = File.getMessage().getString("cropstorage.action.deposit.no_items", "");
            if (!message.isEmpty()) {
                player.sendMessage(ChatUtils.colorizewp(message));
            }
        }
    }
}
