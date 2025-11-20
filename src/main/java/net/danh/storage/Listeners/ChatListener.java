package net.danh.storage.Listeners;

import net.danh.storage.Action.*;
import net.danh.storage.GUI.*;
import net.danh.storage.Manager.CraftingManager;
import net.danh.storage.Manager.RecipeEditManager;
import net.danh.storage.Manager.SoundManager;
import net.danh.storage.Storage;
import net.danh.storage.Utils.*;
import net.danh.storage.Utils.Number;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Objects;
import java.util.UUID;

public class ChatListener implements Listener {
    public static HashMap<Player, String> chat_deposit = new HashMap<>();
    public static HashMap<Player, String> chat_withdraw = new HashMap<>();
    public static HashMap<Player, String> chat_sell = new HashMap<>();
    public static HashMap<Player, String> chat_mythic_withdraw = new HashMap<>();
    public static HashMap<Player, String> chat_mythic_deposit = new HashMap<>();
    public static HashMap<Player, String> chat_convert_from = new HashMap<>();
    public static HashMap<Player, String> chat_convert_to = new HashMap<>();
    public static HashMap<Player, Integer> chat_return_page = new HashMap<>();
    public static HashMap<UUID, String> craftingRequests = new HashMap<>();

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onChat(@NotNull AsyncPlayerChatEvent e) {
        Player p = e.getPlayer();
        String message = ChatColor.stripColor(e.getMessage());
        if (chat_deposit.containsKey(p) && chat_deposit.get(p) != null) {
            if (Number.getInteger(message) > 0) {
                new Deposit(p, chat_deposit.get(p), (long) Number.getInteger(message)).doAction();
                SoundManager.playChatDepositSound(p);
                int returnPage = chat_return_page.getOrDefault(p, PersonalStorage.getPlayerCurrentPage(p));
                SchedulerUtil.runTask(Storage.getStorage(), () -> p.openInventory(new PersonalStorage(p, returnPage).getInventory(SoundContext.SILENT)));
            } else {
                SoundManager.playChatErrorSound(p);
                p.sendMessage(ChatUtils.colorize(Objects.requireNonNull(File.getMessage().getString("user.unknown_number")).replace("<number>", message)));
            }
            chat_deposit.remove(p);
            chat_return_page.remove(p);
            e.setCancelled(true);
        }
        if (chat_withdraw.containsKey(p) && chat_withdraw.get(p) != null) {
            if (Number.getInteger(message) > 0) {
                new Withdraw(p, chat_withdraw.get(p), Number.getInteger(message)).doAction();
                SoundManager.playChatWithdrawSound(p);
                int returnPage = chat_return_page.getOrDefault(p, PersonalStorage.getPlayerCurrentPage(p));
                SchedulerUtil.runTask(Storage.getStorage(), () -> p.openInventory(new PersonalStorage(p, returnPage).getInventory(SoundContext.SILENT)));
            } else {
                SoundManager.playChatErrorSound(p);
                p.sendMessage(ChatUtils.colorize(Objects.requireNonNull(File.getMessage().getString("user.unknown_number")).replace("<number>", message)));
            }
            chat_withdraw.remove(p);
            chat_return_page.remove(p);
            e.setCancelled(true);
        }
        if (chat_sell.containsKey(p) && chat_sell.get(p) != null) {
            if (Number.getInteger(message) > 0) {
                new Sell(p, chat_sell.get(p), Number.getInteger(message)).doAction();
                SoundManager.playChatSellSound(p);
                int returnPage = chat_return_page.getOrDefault(p, PersonalStorage.getPlayerCurrentPage(p));
                SchedulerUtil.runTask(Storage.getStorage(), () -> p.openInventory(new PersonalStorage(p, returnPage).getInventory(SoundContext.SILENT)));
            } else {
                SoundManager.playChatErrorSound(p);
                p.sendMessage(ChatUtils.colorize(Objects.requireNonNull(File.getMessage().getString("user.unknown_number")).replace("<number>", message)));
            }
            chat_sell.remove(p);
            chat_return_page.remove(p);
            e.setCancelled(true);
        }

        if (chat_mythic_withdraw.containsKey(p) && chat_mythic_withdraw.get(p) != null) {
            if (Number.getInteger(message) > 0) {
                new MythicWithdraw(p, chat_mythic_withdraw.get(p), Number.getInteger(message)).doAction();
                SoundManager.playChatDepositSound(p);
                int returnPage = chat_return_page.getOrDefault(p, MythicStorageGUI.getPlayerCurrentPage(p));
                SchedulerUtil.runTask(Storage.getStorage(), () -> p.openInventory(new MythicStorageGUI(p, returnPage).getInventory(SoundContext.SILENT)));
            } else {
                SoundManager.playChatErrorSound(p);
                p.sendMessage(ChatUtils.colorize(Objects.requireNonNull(File.getMessage().getString("user.unknown_number")).replace("<number>", message)));
            }
            chat_mythic_withdraw.remove(p);
            chat_return_page.remove(p);
            e.setCancelled(true);
        }

        if (chat_mythic_deposit.containsKey(p) && chat_mythic_deposit.get(p) != null) {
            if (Number.getInteger(message) > 0) {
                new MythicDeposit(p, chat_mythic_deposit.get(p), Number.getInteger(message)).doAction();
                SoundManager.playChatDepositSound(p);
                int returnPage = chat_return_page.getOrDefault(p, MythicStorageGUI.getPlayerCurrentPage(p));
                SchedulerUtil.runTask(Storage.getStorage(), () -> p.openInventory(new MythicStorageGUI(p, returnPage).getInventory(SoundContext.SILENT)));
            } else {
                SoundManager.playChatErrorSound(p);
                p.sendMessage(ChatUtils.colorize(Objects.requireNonNull(File.getMessage().getString("user.unknown_number")).replace("<number>", message)));
            }
            chat_mythic_deposit.remove(p);
            chat_return_page.remove(p);
            e.setCancelled(true);
        }

        // Handle convert amount input
        if (chat_convert_from.containsKey(p) && chat_convert_from.get(p) != null) {
            if (Number.getInteger(message) > 0) {
                String fromMaterial = chat_convert_from.get(p);
                String toMaterial = chat_convert_to.get(p);
                int amount = Number.getInteger(message);
                int returnPage = chat_return_page.getOrDefault(p, 0);

                new ConvertOre(p, fromMaterial, toMaterial, amount).doAction();
                SoundManager.playItemSound(p, net.danh.storage.Utils.File.getConvertOreConfig(), "option_items.convert_option", SoundContext.INITIAL_OPEN);
                SchedulerUtil.runTask(Storage.getStorage(), () -> p.openInventory(new ConvertOptionGUI(p, fromMaterial, returnPage).getInventory(SoundContext.SILENT)));
            } else {
                SoundManager.playChatErrorSound(p);
                p.sendMessage(ChatUtils.colorize(Objects.requireNonNull(File.getMessage().getString("user.unknown_number")).replace("<number>", message)));
            }
            chat_convert_from.remove(p);
            chat_convert_to.remove(p);
            chat_return_page.remove(p);
            e.setCancelled(true);
        }

        // Handle transfer amount input
        if (TransferGUI.isWaitingForInput(p)) {
            if (Number.getInteger(message) > 0) {
                TransferGUI activeGUI = TransferGUI.getActiveGUI(p);
                if (activeGUI != null) {
                    int amount = Number.getInteger(message);
                    activeGUI.setTransferAmount(amount);
                    SchedulerUtil.runTask(Storage.getStorage(), () -> {
                        activeGUI.updateGUI();
                        p.sendMessage(ChatUtils.colorize("&aTransfer amount set to " + amount));
                    });
                }
            } else {
                SoundManager.playChatErrorSound(p);
                p.sendMessage(ChatUtils.colorize(Objects.requireNonNull(File.getMessage().getString("user.unknown_number")).replace("<number>", message)));
            }
            TransferGUI.setWaitingForInput(p, false);
            e.setCancelled(true);
        }

        // Handle MythicStorage transfer amount input
        if (MythicTransferGUI.isWaitingForInput(p)) {
            if (message.equalsIgnoreCase("cancel")) {
                MythicTransferGUI.setWaitingForInput(p, false);
                p.sendMessage(ChatUtils.colorize(File.getMessage().getString("mythicstorage.transfer.cancelled")));
                e.setCancelled(true);
                return;
            }

            if (Number.getInteger(message) > 0) {
                MythicTransferGUI activeGUI = MythicTransferGUI.getActiveGUI(p);
                if (activeGUI != null) {
                    int amount = Number.getInteger(message);
                    SchedulerUtil.runTask(Storage.getStorage(), () -> {
                        activeGUI.setTransferAmountAndUpdate(amount);
                        p.sendMessage(ChatUtils.colorize("&aTransfer amount set to " + amount));
                    });
                }
            } else {
                SoundManager.playChatErrorSound(p);
                p.sendMessage(ChatUtils.colorize(Objects.requireNonNull(File.getMessage().getString("user.unknown_number")).replace("<number>", message)));
            }
            MythicTransferGUI.setWaitingForInput(p, false);
            e.setCancelled(true);
        }

        // Handle crafting amount input
        if (craftingRequests.containsKey(p.getUniqueId())) {
            String recipeId = craftingRequests.get(p.getUniqueId());
            CraftingManager.handleCraftAmountInput(p, recipeId, message);
            craftingRequests.remove(p.getUniqueId());
            e.setCancelled(true);
        }

        // Handle recipe editing input
        if (RecipeEditManager.isEditing(p)) {
            if (RecipeEditManager.handleChatInput(p, message)) {
                e.setCancelled(true);
            }
        }
    }
}
