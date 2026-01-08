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
    public static HashMap<UUID, String> chat_deposit = new HashMap<>();
    public static HashMap<UUID, String> chat_withdraw = new HashMap<>();
    public static HashMap<UUID, String> chat_sell = new HashMap<>();
    public static HashMap<UUID, String> chat_mythic_withdraw = new HashMap<>();
    public static HashMap<UUID, String> chat_mythic_deposit = new HashMap<>();
    public static HashMap<UUID, String> chat_multi_transfer_material = new HashMap<>();
    public static HashMap<UUID, String> chat_multi_transfer_target = new HashMap<>();
    public static HashMap<UUID, String> chat_multi_mythic_transfer_item = new HashMap<>();
    public static HashMap<UUID, String> chat_multi_mythic_transfer_target = new HashMap<>();
    public static HashMap<UUID, String> chat_convert_from = new HashMap<>();
    public static HashMap<UUID, String> chat_convert_to = new HashMap<>();
    public static HashMap<UUID, Integer> chat_return_page = new HashMap<>();
    public static HashMap<UUID, String> craftingRequests = new HashMap<>();

    private boolean isCancelCommand(String message) {
        return message.trim().equalsIgnoreCase("cancel");
    }

    private void handleCancel(Player player, Runnable gui) {
        SoundManager.playChatErrorSound(player);
        player.sendMessage(ChatUtils.colorize(
                File.getMessage().getString("user.chat_input_cancelled")));
        SchedulerUtil.runTask(Storage.getStorage(), gui);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onChat(@NotNull AsyncPlayerChatEvent e) {
        Player p = e.getPlayer();
        UUID playerId = p.getUniqueId();
        String message = ChatColor.stripColor(e.getMessage());

        if (chat_multi_transfer_material.containsKey(playerId)
                && chat_multi_transfer_material.get(playerId) != null) {
            if (isCancelCommand(message)) {
                TransferMultiGUI gui = TransferMultiGUI.getActiveGUI(p);
                String target = chat_multi_transfer_target.get(playerId);
                chat_multi_transfer_material.remove(playerId);
                chat_multi_transfer_target.remove(playerId);
                if (gui != null) {
                    handleCancel(p, () -> p.openInventory(
                            gui.getInventory(SoundContext.SILENT)));
                } else if (target != null) {
                    handleCancel(p, () -> p.openInventory(
                            new TransferMultiGUI(p, target)
                                    .getInventory(SoundContext.SILENT)));
                } else {
                    handleCancel(p, () -> {
                    });
                }
                e.setCancelled(true);
                return;
            }

            int amount = Number.getInteger(message);
            if (amount > 0) {
                String material = chat_multi_transfer_material.get(playerId);
                TransferMultiGUI gui = TransferMultiGUI.getActiveGUI(p);
                SchedulerUtil.runTask(Storage.getStorage(), () -> {
                    if (gui != null) {
                        gui.setSelectedAmount(material, amount);
                        p.openInventory(gui.getInventory(SoundContext.SILENT));
                    } else {
                        String target = chat_multi_transfer_target.get(playerId);
                        if (target != null) {
                            TransferMultiGUI newGui = new TransferMultiGUI(p, target);
                            newGui.setSelectedAmount(material, amount);
                            p.openInventory(newGui.getInventory(SoundContext.SILENT));
                        }
                    }
                });
            } else {
                SoundManager.playChatErrorSound(p);
                p.sendMessage(ChatUtils.colorize(Objects.requireNonNull(
                                File.getMessage().getString("user.unknown_number"))
                        .replace("<number>", message)));
            }
            chat_multi_transfer_material.remove(playerId);
            chat_multi_transfer_target.remove(playerId);
            e.setCancelled(true);
            return;
        }

        if (chat_multi_mythic_transfer_item.containsKey(playerId)
                && chat_multi_mythic_transfer_item.get(playerId) != null) {
            if (isCancelCommand(message)) {
                MythicTransferMultiGUI gui = MythicTransferMultiGUI.getActiveGUI(p);
                String target = chat_multi_mythic_transfer_target.get(playerId);
                chat_multi_mythic_transfer_item.remove(playerId);
                chat_multi_mythic_transfer_target.remove(playerId);
                if (gui != null) {
                    handleCancel(p, () -> p.openInventory(
                            gui.getInventory(SoundContext.SILENT)));
                } else if (target != null) {
                    handleCancel(p, () -> p.openInventory(
                            new MythicTransferMultiGUI(p, target)
                                    .getInventory(SoundContext.SILENT)));
                } else {
                    handleCancel(p, () -> {
                    });
                }
                e.setCancelled(true);
                return;
            }

            int amount = Number.getInteger(message);
            if (amount > 0) {
                String itemName = chat_multi_mythic_transfer_item.get(playerId);
                MythicTransferMultiGUI gui = MythicTransferMultiGUI.getActiveGUI(p);
                SchedulerUtil.runTask(Storage.getStorage(), () -> {
                    if (gui != null) {
                        gui.setSelectedAmount(itemName, amount);
                        p.openInventory(gui.getInventory(SoundContext.SILENT));
                    } else {
                        String target = chat_multi_mythic_transfer_target.get(playerId);
                        if (target != null) {
                            MythicTransferMultiGUI newGui =
                                    new MythicTransferMultiGUI(p, target);
                            newGui.setSelectedAmount(itemName, amount);
                            p.openInventory(newGui.getInventory(SoundContext.SILENT));
                        }
                    }
                });
            } else {
                SoundManager.playChatErrorSound(p);
                p.sendMessage(ChatUtils.colorize(Objects.requireNonNull(
                                File.getMessage().getString("user.unknown_number"))
                        .replace("<number>", message)));
            }
            chat_multi_mythic_transfer_item.remove(playerId);
            chat_multi_mythic_transfer_target.remove(playerId);
            e.setCancelled(true);
            return;
        }

        if (chat_deposit.containsKey(playerId)
                && chat_deposit.get(playerId) != null) {
            if (isCancelCommand(message)) {
                int returnPage = chat_return_page.getOrDefault(playerId,
                        PersonalStorage.getPlayerCurrentPage(p));
                handleCancel(p, () -> p.openInventory(new PersonalStorage(p, returnPage).getInventory(SoundContext.SILENT)));
                chat_deposit.remove(playerId);
                chat_return_page.remove(playerId);
                e.setCancelled(true);
                return;
            }
            if (Number.getInteger(message) > 0) {
                String material = chat_deposit.get(playerId);
                int amount = Number.getInteger(message);
                int returnPage = chat_return_page.getOrDefault(playerId,
                        PersonalStorage.getPlayerCurrentPage(p));
                SchedulerUtil.runTask(Storage.getStorage(), () -> {
                    new Deposit(p, material, (long) amount).doAction();
                    SoundManager.playChatDepositSound(p);
                    p.openInventory(new PersonalStorage(p, returnPage).getInventory(SoundContext.SILENT));
                });
            } else {
                SoundManager.playChatErrorSound(p);
                p.sendMessage(ChatUtils.colorize(Objects.requireNonNull(File.getMessage().getString("user.unknown_number")).replace("<number>", message)));
            }
            chat_deposit.remove(playerId);
            chat_return_page.remove(playerId);
            e.setCancelled(true);
        }
        if (chat_withdraw.containsKey(playerId)
                && chat_withdraw.get(playerId) != null) {
            if (isCancelCommand(message)) {
                int returnPage = chat_return_page.getOrDefault(playerId,
                        PersonalStorage.getPlayerCurrentPage(p));
                handleCancel(p, () -> p.openInventory(new PersonalStorage(p, returnPage).getInventory(SoundContext.SILENT)));
                chat_withdraw.remove(playerId);
                chat_return_page.remove(playerId);
                e.setCancelled(true);
                return;
            }
            if (Number.getInteger(message) > 0) {
                String material = chat_withdraw.get(playerId);
                int amount = Number.getInteger(message);
                int returnPage = chat_return_page.getOrDefault(playerId,
                        PersonalStorage.getPlayerCurrentPage(p));
                SchedulerUtil.runTask(Storage.getStorage(), () -> {
                    new Withdraw(p, material, amount).doAction();
                    SoundManager.playChatWithdrawSound(p);
                    p.openInventory(new PersonalStorage(p, returnPage).getInventory(SoundContext.SILENT));
                });
            } else {
                SoundManager.playChatErrorSound(p);
                p.sendMessage(ChatUtils.colorize(Objects.requireNonNull(File.getMessage().getString("user.unknown_number")).replace("<number>", message)));
            }
            chat_withdraw.remove(playerId);
            chat_return_page.remove(playerId);
            e.setCancelled(true);
        }
        if (chat_sell.containsKey(playerId)
                && chat_sell.get(playerId) != null) {
            if (isCancelCommand(message)) {
                int returnPage = chat_return_page.getOrDefault(playerId,
                        PersonalStorage.getPlayerCurrentPage(p));
                handleCancel(p, () -> p.openInventory(new PersonalStorage(p, returnPage).getInventory(SoundContext.SILENT)));
                chat_sell.remove(playerId);
                chat_return_page.remove(playerId);
                e.setCancelled(true);
                return;
            }
            if (Number.getInteger(message) > 0) {
                String material = chat_sell.get(playerId);
                int amount = Number.getInteger(message);
                int returnPage = chat_return_page.getOrDefault(playerId,
                        PersonalStorage.getPlayerCurrentPage(p));
                SchedulerUtil.runTask(Storage.getStorage(), () -> {
                    new Sell(p, material, amount).doAction();
                    SoundManager.playChatSellSound(p);
                    p.openInventory(new PersonalStorage(p, returnPage).getInventory(SoundContext.SILENT));
                });
            } else {
                SoundManager.playChatErrorSound(p);
                p.sendMessage(ChatUtils.colorize(Objects.requireNonNull(File.getMessage().getString("user.unknown_number")).replace("<number>", message)));
            }
            chat_sell.remove(playerId);
            chat_return_page.remove(playerId);
            e.setCancelled(true);
        }

        if (chat_mythic_withdraw.containsKey(playerId)
                && chat_mythic_withdraw.get(playerId) != null) {
            if (isCancelCommand(message)) {
                int returnPage = chat_return_page.getOrDefault(playerId,
                        MythicStorageGUI.getPlayerCurrentPage(p));
                handleCancel(p, () -> p.openInventory(new MythicStorageGUI(p, returnPage).getInventory(SoundContext.SILENT)));
                chat_mythic_withdraw.remove(playerId);
                chat_return_page.remove(playerId);
                e.setCancelled(true);
                return;
            }
            if (Number.getInteger(message) > 0) {
                String itemName = chat_mythic_withdraw.get(playerId);
                int amount = Number.getInteger(message);
                int returnPage = chat_return_page.getOrDefault(playerId,
                        MythicStorageGUI.getPlayerCurrentPage(p));
                SchedulerUtil.runTask(Storage.getStorage(), () -> {
                    new MythicWithdraw(p, itemName, amount).doAction();
                    SoundManager.playChatWithdrawSound(p);
                    p.openInventory(new MythicStorageGUI(p, returnPage).getInventory(SoundContext.SILENT));
                });
            } else {
                SoundManager.playChatErrorSound(p);
                p.sendMessage(ChatUtils.colorize(Objects.requireNonNull(File.getMessage().getString("user.unknown_number")).replace("<number>", message)));
            }
            chat_mythic_withdraw.remove(playerId);
            chat_return_page.remove(playerId);
            e.setCancelled(true);
        }

        if (chat_mythic_deposit.containsKey(playerId)
                && chat_mythic_deposit.get(playerId) != null) {
            if (isCancelCommand(message)) {
                int returnPage = chat_return_page.getOrDefault(playerId,
                        MythicStorageGUI.getPlayerCurrentPage(p));
                handleCancel(p, () -> p.openInventory(new MythicStorageGUI(p, returnPage).getInventory(SoundContext.SILENT)));
                chat_mythic_deposit.remove(playerId);
                chat_return_page.remove(playerId);
                e.setCancelled(true);
                return;
            }
            if (Number.getInteger(message) > 0) {
                String itemName = chat_mythic_deposit.get(playerId);
                int amount = Number.getInteger(message);
                int returnPage = chat_return_page.getOrDefault(playerId,
                        MythicStorageGUI.getPlayerCurrentPage(p));
                SchedulerUtil.runTask(Storage.getStorage(), () -> {
                    new MythicDeposit(p, itemName, amount).doAction();
                    SoundManager.playChatDepositSound(p);
                    p.openInventory(new MythicStorageGUI(p, returnPage).getInventory(SoundContext.SILENT));
                });
            } else {
                SoundManager.playChatErrorSound(p);
                p.sendMessage(ChatUtils.colorize(Objects.requireNonNull(File.getMessage().getString("user.unknown_number")).replace("<number>", message)));
            }
            chat_mythic_deposit.remove(playerId);
            chat_return_page.remove(playerId);
            e.setCancelled(true);
        }

        // Handle convert amount input
        if (chat_convert_from.containsKey(playerId)
                && chat_convert_from.get(playerId) != null) {
            if (isCancelCommand(message)) {
                String fromMaterial = chat_convert_from.get(playerId);
                int returnPage = chat_return_page.getOrDefault(playerId, 0);
                chat_convert_from.remove(playerId);
                chat_convert_to.remove(playerId);
                chat_return_page.remove(playerId);
                handleCancel(p, () -> p.openInventory(new ConvertOptionGUI(p, fromMaterial, returnPage).getInventory(SoundContext.SILENT)));
                e.setCancelled(true);
                return;
            }
            if (Number.getInteger(message) > 0) {
                String fromMaterial = chat_convert_from.get(playerId);
                String toMaterial = chat_convert_to.get(playerId);
                int amount = Number.getInteger(message);
                int returnPage = chat_return_page.getOrDefault(playerId, 0);

                SchedulerUtil.runTask(Storage.getStorage(), () -> {
                    new ConvertOre(p, fromMaterial, toMaterial, amount).doAction();
                    SoundManager.playItemSound(p, net.danh.storage.Utils.File.getConvertOreConfig(), "option_items.convert_option", SoundContext.INITIAL_OPEN);
                    p.openInventory(new ConvertOptionGUI(p, fromMaterial, returnPage).getInventory(SoundContext.SILENT));
                });
            } else {
                SoundManager.playChatErrorSound(p);
                p.sendMessage(ChatUtils.colorize(Objects.requireNonNull(File.getMessage().getString("user.unknown_number")).replace("<number>", message)));
            }
            chat_convert_from.remove(playerId);
            chat_convert_to.remove(playerId);
            chat_return_page.remove(playerId);
            e.setCancelled(true);
        }

        // Handle transfer amount input
        if (TransferGUI.isWaitingForInput(p)) {
            if (isCancelCommand(message)) {
                TransferGUI.setWaitingForInput(p, false);
                TransferGUI activeGUI = TransferGUI.getActiveGUI(p);
                if (activeGUI != null) {
                    handleCancel(p, () -> activeGUI.updateGUI());
                }
                e.setCancelled(true);
                return;
            }
            if (Number.getInteger(message) > 0) {
                TransferGUI activeGUI = TransferGUI.getActiveGUI(p);
                if (activeGUI != null) {
                    int amount = Number.getInteger(message);
                    activeGUI.setTransferAmount(amount);
                    SchedulerUtil.runTask(Storage.getStorage(), () -> {
                        activeGUI.updateGUI();
                        p.sendMessage(ChatUtils.colorize(
                                File.getMessage().getString("transfer.gui_enter_amount_success")
                                        .replace("#amount#", String.valueOf(amount))));
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
            if (isCancelCommand(message)) {
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
                        p.sendMessage(ChatUtils.colorize(
                                File.getMessage().getString("mythicstorage.transfer.gui_enter_amount_success")
                                        .replace("#amount#", String.valueOf(amount))));
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
            if (isCancelCommand(message)) {
                craftingRequests.remove(p.getUniqueId());
                handleCancel(p, () -> p.openInventory(new RecipeListGUI(p).getInventory(SoundContext.SILENT)));
                e.setCancelled(true);
                return;
            }
            String recipeId = craftingRequests.get(p.getUniqueId());
            CraftingManager.handleCraftAmountInput(p, recipeId, message);
            craftingRequests.remove(p.getUniqueId());
            e.setCancelled(true);
        }

        // Handle recipe editing input
        if (RecipeEditManager.isEditing(p)) {
            if (isCancelCommand(message)) {
                RecipeEditManager.handleCancel(p);
                e.setCancelled(true);
                return;
            }
            if (RecipeEditManager.handleChatInput(p, message)) {
                e.setCancelled(true);
            }
        }
    }
}
