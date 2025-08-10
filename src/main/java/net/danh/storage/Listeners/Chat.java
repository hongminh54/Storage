package net.danh.storage.Listeners;

import net.danh.storage.Action.ConvertOre;
import net.danh.storage.Action.Deposit;
import net.danh.storage.Action.Sell;
import net.danh.storage.Action.Withdraw;
import net.danh.storage.GUI.ConvertOptionGUI;
import net.danh.storage.GUI.PersonalStorage;
import net.danh.storage.GUI.TransferGUI;
import net.danh.storage.Manager.SoundManager;
import net.danh.storage.Storage;
import net.danh.storage.Utils.File;
import net.danh.storage.Utils.Number;
import net.danh.storage.Utils.SoundContext;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Objects;

public class Chat implements Listener {
    public static HashMap<Player, String> chat_deposit = new HashMap<>();
    public static HashMap<Player, String> chat_withdraw = new HashMap<>();
    public static HashMap<Player, String> chat_sell = new HashMap<>();
    public static HashMap<Player, String> chat_convert_from = new HashMap<>();
    public static HashMap<Player, String> chat_convert_to = new HashMap<>();
    public static HashMap<Player, Integer> chat_return_page = new HashMap<>();

    // Recipe editing chat handlers
    public static HashMap<Player, String> chat_recipe_edit_type = new HashMap<>();
    public static HashMap<Player, String> chat_recipe_id = new HashMap<>();
    public static HashMap<Player, String> chat_recipe_field = new HashMap<>();

    // Recipe crafting chat handlers
    public static HashMap<Player, String> chat_recipe_craft = new HashMap<>();

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onChat(@NotNull AsyncPlayerChatEvent e) {
        Player p = e.getPlayer();
        String message = ChatColor.stripColor(e.getMessage());
        if (chat_deposit.containsKey(p) && chat_deposit.get(p) != null) {
            if (Number.getInteger(message) > 0) {
                new Deposit(p, chat_deposit.get(p), (long) Number.getInteger(message)).doAction();
                SoundManager.playChatDepositSound(p);
                int returnPage = chat_return_page.getOrDefault(p, PersonalStorage.getPlayerCurrentPage(p));
                Bukkit.getScheduler().runTask(Storage.getStorage(), () -> p.openInventory(new PersonalStorage(p, returnPage).getInventory(SoundContext.SILENT)));
            } else {
                SoundManager.playChatErrorSound(p);
                p.sendMessage(net.danh.storage.Utils.Chat.colorize(Objects.requireNonNull(File.getMessage().getString("user.unknown_number")).replace("<number>", message)));
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
                Bukkit.getScheduler().runTask(Storage.getStorage(), () -> p.openInventory(new PersonalStorage(p, returnPage).getInventory(SoundContext.SILENT)));
            } else {
                SoundManager.playChatErrorSound(p);
                p.sendMessage(net.danh.storage.Utils.Chat.colorize(Objects.requireNonNull(File.getMessage().getString("user.unknown_number")).replace("<number>", message)));
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
                Bukkit.getScheduler().runTask(Storage.getStorage(), () -> p.openInventory(new PersonalStorage(p, returnPage).getInventory(SoundContext.SILENT)));
            } else {
                SoundManager.playChatErrorSound(p);
                p.sendMessage(net.danh.storage.Utils.Chat.colorize(Objects.requireNonNull(File.getMessage().getString("user.unknown_number")).replace("<number>", message)));
            }
            chat_sell.remove(p);
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
                Bukkit.getScheduler().runTask(Storage.getStorage(), () -> p.openInventory(new ConvertOptionGUI(p, fromMaterial, returnPage).getInventory(SoundContext.SILENT)));
            } else {
                SoundManager.playChatErrorSound(p);
                p.sendMessage(net.danh.storage.Utils.Chat.colorize(Objects.requireNonNull(File.getMessage().getString("user.unknown_number")).replace("<number>", message)));
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
                    Bukkit.getScheduler().runTask(Storage.getStorage(), () -> {
                        activeGUI.updateGUI();
                        p.sendMessage(net.danh.storage.Utils.Chat.colorize("&aTransfer amount set to " + amount));
                    });
                }
            } else {
                SoundManager.playChatErrorSound(p);
                p.sendMessage(net.danh.storage.Utils.Chat.colorize(Objects.requireNonNull(File.getMessage().getString("user.unknown_number")).replace("<number>", message)));
            }
            TransferGUI.setWaitingForInput(p, false);
            e.setCancelled(true);
        }

        // Handle recipe editing input
        if (chat_recipe_edit_type.containsKey(p) && chat_recipe_edit_type.get(p) != null) {
            String editType = chat_recipe_edit_type.get(p);
            String recipeId = chat_recipe_id.get(p);
            String field = chat_recipe_field.get(p);

            Bukkit.getScheduler().runTask(Storage.getStorage(), () -> {
                net.danh.storage.Manager.RecipeEditManager.handleChatInput(p, editType, recipeId, field, message);
            });

            chat_recipe_edit_type.remove(p);
            chat_recipe_id.remove(p);
            chat_recipe_field.remove(p);
            e.setCancelled(true);
        }

        // Handle recipe crafting amount input
        if (chat_recipe_craft.containsKey(p) && chat_recipe_craft.get(p) != null) {
            String recipeId = chat_recipe_craft.get(p);

            Bukkit.getScheduler().runTask(Storage.getStorage(), () -> {
                net.danh.storage.Manager.CraftingManager.handleCraftAmountInput(p, recipeId, message);
            });

            chat_recipe_craft.remove(p);
            e.setCancelled(true);
        }
    }
}
