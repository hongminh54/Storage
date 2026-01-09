package net.danh.storage.Action;

import net.danh.storage.Manager.Mythic.MythicStorageManager;
import net.danh.storage.MythicMobs.MythicMobsHelper;
import net.danh.storage.NMS.NMSAssistant;
import net.danh.storage.Utils.ChatUtils;
import net.danh.storage.Utils.File;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

public class MythicDeposit {
    private final Player player;
    private final String itemName;
    private final long amount;

    public MythicDeposit(Player player, String itemName, long amount) {
        this.player = player;
        this.itemName = itemName;
        this.amount = amount;
    }

    public void doAction() {
        if (player == null || !player.isOnline()) return;
        if (itemName == null || itemName.trim().isEmpty()) return;
        if (amount <= 0) return;

        MythicMobsHelper helper = MythicStorageManager.getMythicMobsHelper();
        if (helper == null || !helper.isInitialized()) {
            player.sendMessage(ChatUtils.colorizewp(File.getMessage().getString("mythicstorage.system_disabled", "")));
            return;
        }

        PlayerInventory inv = player.getInventory();
        ItemStack[] contents = inv.getContents();
        if (contents == null) return;

        long deposited = 0;
        long toDeposit = amount;

        for (int i = 0; i < contents.length && toDeposit > 0; i++) {
            ItemStack item = contents[i];
            if (item == null) continue;

            String foundItemName = helper.getMythicItemInternalName(item);
            if (foundItemName != null && foundItemName.equals(itemName)) {
                int itemAmount = item.getAmount();
                int canDeposit = (int) Math.min(itemAmount, toDeposit);

                if (MythicStorageManager.addItemAmount(player, itemName, canDeposit)) {
                    if (canDeposit == itemAmount) {
                        contents[i] = null;
                    } else {
                        item.setAmount(itemAmount - canDeposit);
                    }
                    deposited += canDeposit;
                    toDeposit -= canDeposit;
                } else {
                    String displayName = helper.getItemDisplayName(itemName);
                    if (displayName == null || displayName.trim().isEmpty()) displayName = itemName;
                    String message = File.getMessage().getString("mythicstorage.action.deposit.full_storage", "")
                            .replace("#item_amount#", String.valueOf(MythicStorageManager.getPlayerItem(player, itemName)))
                            .replace("#max_storage#", String.valueOf(MythicStorageManager.getMaxStorage(player)))
                            .replace("#amount#", String.valueOf(canDeposit))
                            .replace("#material#", displayName);
                    if (!message.isEmpty()) {
                        player.sendMessage(ChatUtils.colorizewp(message));
                    }
                    break;
                }
            }
        }

        if (deposited > 0) {
            inv.setContents(contents);
            if (new NMSAssistant().isVersionLessThan(9)) {
                player.updateInventory();
            }

            String displayName = helper.getItemDisplayName(itemName);
            if (displayName == null || displayName.trim().isEmpty()) displayName = itemName;
            String message = File.getMessage().getString("mythicstorage.action.deposit.deposit_item", "")
                    .replace("#amount#", String.valueOf(deposited))
                    .replace("#material#", displayName)
                    .replace("#item_amount#", String.valueOf(MythicStorageManager.getPlayerItem(player, itemName)))
                    .replace("#max_storage#", String.valueOf(MythicStorageManager.getMaxStorage(player)));
            if (!message.isEmpty()) {
                player.sendMessage(ChatUtils.colorizewp(message));
            }
        } else {
            String message = File.getMessage().getString("mythicstorage.action.deposit.no_items", "");
            if (!message.isEmpty()) {
                player.sendMessage(ChatUtils.colorizewp(message));
            }
        }
    }
}
