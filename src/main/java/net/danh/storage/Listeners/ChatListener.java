package net.danh.storage.Listeners;

import net.danh.storage.Action.*;
import net.danh.storage.GUI.ConvertOptionGUI;
import net.danh.storage.GUI.Crafting.RecipeListGUI;
import net.danh.storage.GUI.Crop.CropStorageGUI;
import net.danh.storage.GUI.Crop.CropTransferGUI;
import net.danh.storage.GUI.Crop.CropTransferMultiGUI;
import net.danh.storage.GUI.Mob.MobStorageGUI;
import net.danh.storage.GUI.Mob.MobTransferGUI;
import net.danh.storage.GUI.Mob.MobTransferMultiGUI;
import net.danh.storage.GUI.Mythic.MythicStorageGUI;
import net.danh.storage.GUI.Mythic.MythicTransferGUI;
import net.danh.storage.GUI.Mythic.MythicTransferMultiGUI;
import net.danh.storage.GUI.PersonalStorage;
import net.danh.storage.GUI.TransferGUI;
import net.danh.storage.GUI.TransferMultiGUI;
import net.danh.storage.Manager.Crafting.CraftingManager;
import net.danh.storage.Manager.Crafting.RecipeEditManager;
import net.danh.storage.Manager.SoundManager;
import net.danh.storage.Storage;
import net.danh.storage.Utils.*;
import net.danh.storage.Utils.Number;
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
    public static HashMap<UUID, String> chat_crop_withdraw = new HashMap<>();
    public static HashMap<UUID, String> chat_crop_deposit = new HashMap<>();
    public static HashMap<UUID, String> chat_crop_sell = new HashMap<>();
    public static HashMap<UUID, String> chat_mob_withdraw = new HashMap<>();
    public static HashMap<UUID, String> chat_mob_deposit = new HashMap<>();
    public static HashMap<UUID, String> chat_mob_sell = new HashMap<>();
    public static HashMap<UUID, String> chat_crop_multi_transfer_item = new HashMap<>();
    public static HashMap<UUID, String> chat_crop_multi_transfer_target = new HashMap<>();
    public static HashMap<UUID, String> chat_mob_multi_transfer_item = new HashMap<>();
    public static HashMap<UUID, String> chat_mob_multi_transfer_target = new HashMap<>();
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
                            MythicTransferMultiGUI newGui = new MythicTransferMultiGUI(p, target);
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
                handleCancel(p,
                        () -> p.openInventory(new PersonalStorage(p, returnPage).getInventory(SoundContext.SILENT)));
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
                p.sendMessage(
                        ChatUtils.colorize(Objects.requireNonNull(File.getMessage().getString("user.unknown_number"))
                                .replace("<number>", message)));
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
                handleCancel(p,
                        () -> p.openInventory(new PersonalStorage(p, returnPage).getInventory(SoundContext.SILENT)));
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
                p.sendMessage(
                        ChatUtils.colorize(Objects.requireNonNull(File.getMessage().getString("user.unknown_number"))
                                .replace("<number>", message)));
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
                handleCancel(p,
                        () -> p.openInventory(new PersonalStorage(p, returnPage).getInventory(SoundContext.SILENT)));
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
                p.sendMessage(
                        ChatUtils.colorize(Objects.requireNonNull(File.getMessage().getString("user.unknown_number"))
                                .replace("<number>", message)));
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
                handleCancel(p,
                        () -> p.openInventory(new MythicStorageGUI(p, returnPage).getInventory(SoundContext.SILENT)));
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
                p.sendMessage(
                        ChatUtils.colorize(Objects.requireNonNull(File.getMessage().getString("user.unknown_number"))
                                .replace("<number>", message)));
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
                handleCancel(p,
                        () -> p.openInventory(new MythicStorageGUI(p, returnPage).getInventory(SoundContext.SILENT)));
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
                p.sendMessage(
                        ChatUtils.colorize(Objects.requireNonNull(File.getMessage().getString("user.unknown_number"))
                                .replace("<number>", message)));
            }
            chat_mythic_deposit.remove(playerId);
            chat_return_page.remove(playerId);
            e.setCancelled(true);
        }

        // Handle CropStorage withdraw input
        if (chat_crop_withdraw.containsKey(playerId)
                && chat_crop_withdraw.get(playerId) != null) {
            if (isCancelCommand(message)) {
                int returnPage = chat_return_page.getOrDefault(playerId,
                        CropStorageGUI.getPlayerCurrentPage(p));
                handleCancel(p,
                        () -> p.openInventory(new CropStorageGUI(p, returnPage).getInventory(SoundContext.SILENT)));
                chat_crop_withdraw.remove(playerId);
                chat_return_page.remove(playerId);
                e.setCancelled(true);
                return;
            }
            if (Number.getInteger(message) > 0) {
                String itemName = chat_crop_withdraw.get(playerId);
                int amount = Number.getInteger(message);
                int returnPage = chat_return_page.getOrDefault(playerId,
                        CropStorageGUI.getPlayerCurrentPage(p));
                SchedulerUtil.runTask(Storage.getStorage(), () -> {
                    new CropWithdraw(p, itemName, amount).doAction();
                    SoundManager.playChatWithdrawSound(p);
                    p.openInventory(new CropStorageGUI(p, returnPage).getInventory(SoundContext.SILENT));
                });
            } else {
                SoundManager.playChatErrorSound(p);
                p.sendMessage(
                        ChatUtils.colorize(Objects.requireNonNull(File.getMessage().getString("user.unknown_number"))
                                .replace("<number>", message)));
            }
            chat_crop_withdraw.remove(playerId);
            chat_return_page.remove(playerId);
            e.setCancelled(true);
        }

        // Handle CropStorage deposit input
        if (chat_crop_deposit.containsKey(playerId)
                && chat_crop_deposit.get(playerId) != null) {
            if (isCancelCommand(message)) {
                int returnPage = chat_return_page.getOrDefault(playerId,
                        CropStorageGUI.getPlayerCurrentPage(p));
                handleCancel(p,
                        () -> p.openInventory(new CropStorageGUI(p, returnPage).getInventory(SoundContext.SILENT)));
                chat_crop_deposit.remove(playerId);
                chat_return_page.remove(playerId);
                e.setCancelled(true);
                return;
            }
            if (Number.getInteger(message) > 0) {
                String itemName = chat_crop_deposit.get(playerId);
                int amount = Number.getInteger(message);
                int returnPage = chat_return_page.getOrDefault(playerId,
                        CropStorageGUI.getPlayerCurrentPage(p));
                SchedulerUtil.runTask(Storage.getStorage(), () -> {
                    new CropDeposit(p, itemName, amount).doAction();
                    SoundManager.playChatDepositSound(p);
                    p.openInventory(new CropStorageGUI(p, returnPage).getInventory(SoundContext.SILENT));
                });
            } else {
                SoundManager.playChatErrorSound(p);
                p.sendMessage(
                        ChatUtils.colorize(Objects.requireNonNull(File.getMessage().getString("user.unknown_number"))
                                .replace("<number>", message)));
            }
            chat_crop_deposit.remove(playerId);
            chat_return_page.remove(playerId);
            e.setCancelled(true);
        }

        // Handle CropStorage sell input
        if (chat_crop_sell.containsKey(playerId)
                && chat_crop_sell.get(playerId) != null) {
            if (isCancelCommand(message)) {
                int returnPage = chat_return_page.getOrDefault(playerId,
                        CropStorageGUI.getPlayerCurrentPage(p));
                handleCancel(p,
                        () -> p.openInventory(new CropStorageGUI(p, returnPage).getInventory(SoundContext.SILENT)));
                chat_crop_sell.remove(playerId);
                chat_return_page.remove(playerId);
                e.setCancelled(true);
                return;
            }
            if (Number.getInteger(message) > 0) {
                String itemName = chat_crop_sell.get(playerId);
                int amount = Number.getInteger(message);
                int returnPage = chat_return_page.getOrDefault(playerId,
                        CropStorageGUI.getPlayerCurrentPage(p));
                SchedulerUtil.runTask(Storage.getStorage(), () -> {
                    new CropSell(p, itemName, amount).doAction();
                    SoundManager.playChatSellSound(p);
                    p.openInventory(new CropStorageGUI(p, returnPage).getInventory(SoundContext.SILENT));
                });
            } else {
                SoundManager.playChatErrorSound(p);
                p.sendMessage(
                        ChatUtils.colorize(Objects.requireNonNull(File.getMessage().getString("user.unknown_number"))
                                .replace("<number>", message)));
            }
            chat_crop_sell.remove(playerId);
            chat_return_page.remove(playerId);
            e.setCancelled(true);
        }

        if (chat_mob_withdraw.containsKey(playerId)
                && chat_mob_withdraw.get(playerId) != null) {
            if (isCancelCommand(message)) {
                int returnPage = chat_return_page.getOrDefault(playerId,
                        MobStorageGUI.getPlayerCurrentPage(p));
                handleCancel(p,
                        () -> p.openInventory(new MobStorageGUI(p, returnPage).getInventory(SoundContext.SILENT)));
                chat_mob_withdraw.remove(playerId);
                chat_return_page.remove(playerId);
                e.setCancelled(true);
                return;
            }
            if (Number.getInteger(message) > 0) {
                String itemName = chat_mob_withdraw.get(playerId);
                int amount = Number.getInteger(message);
                int returnPage = chat_return_page.getOrDefault(playerId,
                        MobStorageGUI.getPlayerCurrentPage(p));
                SchedulerUtil.runTask(Storage.getStorage(), () -> {
                    new MobWithdraw(p, itemName, amount).doAction();
                    SoundManager.playChatWithdrawSound(p);
                    p.openInventory(new MobStorageGUI(p, returnPage).getInventory(SoundContext.SILENT));
                });
            } else {
                SoundManager.playChatErrorSound(p);
                p.sendMessage(
                        ChatUtils.colorize(Objects.requireNonNull(File.getMessage().getString("user.unknown_number"))
                                .replace("<number>", message)));
            }
            chat_mob_withdraw.remove(playerId);
            chat_return_page.remove(playerId);
            e.setCancelled(true);
        }

        if (chat_mob_deposit.containsKey(playerId)
                && chat_mob_deposit.get(playerId) != null) {
            if (isCancelCommand(message)) {
                int returnPage = chat_return_page.getOrDefault(playerId,
                        MobStorageGUI.getPlayerCurrentPage(p));
                handleCancel(p,
                        () -> p.openInventory(new MobStorageGUI(p, returnPage).getInventory(SoundContext.SILENT)));
                chat_mob_deposit.remove(playerId);
                chat_return_page.remove(playerId);
                e.setCancelled(true);
                return;
            }
            if (Number.getInteger(message) > 0) {
                String itemName = chat_mob_deposit.get(playerId);
                int amount = Number.getInteger(message);
                int returnPage = chat_return_page.getOrDefault(playerId,
                        MobStorageGUI.getPlayerCurrentPage(p));
                SchedulerUtil.runTask(Storage.getStorage(), () -> {
                    new MobDeposit(p, itemName, amount).doAction();
                    SoundManager.playChatDepositSound(p);
                    p.openInventory(new MobStorageGUI(p, returnPage).getInventory(SoundContext.SILENT));
                });
            } else {
                SoundManager.playChatErrorSound(p);
                p.sendMessage(
                        ChatUtils.colorize(Objects.requireNonNull(File.getMessage().getString("user.unknown_number"))
                                .replace("<number>", message)));
            }
            chat_mob_deposit.remove(playerId);
            chat_return_page.remove(playerId);
            e.setCancelled(true);
        }

        if (chat_mob_sell.containsKey(playerId)
                && chat_mob_sell.get(playerId) != null) {
            if (isCancelCommand(message)) {
                int returnPage = chat_return_page.getOrDefault(playerId,
                        MobStorageGUI.getPlayerCurrentPage(p));
                handleCancel(p,
                        () -> p.openInventory(new MobStorageGUI(p, returnPage).getInventory(SoundContext.SILENT)));
                chat_mob_sell.remove(playerId);
                chat_return_page.remove(playerId);
                e.setCancelled(true);
                return;
            }
            if (Number.getInteger(message) > 0) {
                String itemName = chat_mob_sell.get(playerId);
                int amount = Number.getInteger(message);
                int returnPage = chat_return_page.getOrDefault(playerId,
                        MobStorageGUI.getPlayerCurrentPage(p));
                SchedulerUtil.runTask(Storage.getStorage(), () -> {
                    new MobSell(p, itemName, amount).doAction();
                    SoundManager.playChatSellSound(p);
                    p.openInventory(new MobStorageGUI(p, returnPage).getInventory(SoundContext.SILENT));
                });
            } else {
                SoundManager.playChatErrorSound(p);
                p.sendMessage(
                        ChatUtils.colorize(Objects.requireNonNull(File.getMessage().getString("user.unknown_number"))
                                .replace("<number>", message)));
            }
            chat_mob_sell.remove(playerId);
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
                handleCancel(p, () -> p.openInventory(
                        new ConvertOptionGUI(p, fromMaterial, returnPage).getInventory(SoundContext.SILENT)));
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
                    SoundManager.playItemSound(p, net.danh.storage.Utils.File.getConvertOreConfig(),
                            "option_items.convert_option", SoundContext.INITIAL_OPEN);
                    p.openInventory(
                            new ConvertOptionGUI(p, fromMaterial, returnPage).getInventory(SoundContext.SILENT));
                });
            } else {
                SoundManager.playChatErrorSound(p);
                p.sendMessage(
                        ChatUtils.colorize(Objects.requireNonNull(File.getMessage().getString("user.unknown_number"))
                                .replace("<number>", message)));
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
                p.sendMessage(
                        ChatUtils.colorize(Objects.requireNonNull(File.getMessage().getString("user.unknown_number"))
                                .replace("<number>", message)));
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
                p.sendMessage(
                        ChatUtils.colorize(Objects.requireNonNull(File.getMessage().getString("user.unknown_number"))
                                .replace("<number>", message)));
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

        // Handle CropTransferGUI amount input
        if (CropTransferGUI.isWaitingForInput(p)) {
            if (isCancelCommand(message)) {
                CropTransferGUI.setWaitingForInput(p, false);
                CropTransferGUI activeGUI = CropTransferGUI.getActiveGUI(p);
                if (activeGUI != null) {
                    handleCancel(p, activeGUI::updateGUI);
                }
                e.setCancelled(true);
                return;
            }
            if (Number.getInteger(message) > 0) {
                CropTransferGUI activeGUI = CropTransferGUI.getActiveGUI(p);
                if (activeGUI != null) {
                    int amount = Number.getInteger(message);
                    SchedulerUtil.runTask(Storage.getStorage(), () -> {
                        activeGUI.setTransferAmountAndUpdate(amount);
                        p.sendMessage(ChatUtils.colorize(
                                File.getMessage().getString("cropstorage.transfer.gui_enter_amount_success")
                                        .replace("#amount#", String.valueOf(amount))));
                    });
                }
            } else {
                SoundManager.playChatErrorSound(p);
                p.sendMessage(
                        ChatUtils.colorize(Objects.requireNonNull(File.getMessage().getString("user.unknown_number"))
                                .replace("<number>", message)));
            }
            CropTransferGUI.setWaitingForInput(p, false);
            e.setCancelled(true);
        }

        // Handle CropTransferGUI receiver input
        if (CropTransferGUI.isWaitingForReceiver(p)) {
            if (isCancelCommand(message)) {
                CropTransferGUI.setWaitingForReceiver(p, false);
                CropTransferGUI activeGUI = CropTransferGUI.getActiveGUI(p);
                if (activeGUI != null) {
                    handleCancel(p, activeGUI::updateGUI);
                }
                e.setCancelled(true);
                return;
            }
            Player targetPlayer = Bukkit.getPlayer(message);
            if (targetPlayer != null && targetPlayer.isOnline()) {
                CropTransferGUI activeGUI = CropTransferGUI.getActiveGUI(p);
                if (activeGUI != null) {
                    SchedulerUtil.runTask(Storage.getStorage(), () -> {
                        activeGUI.setTargetPlayerAndUpdate(message);
                        p.sendMessage(ChatUtils.colorize(
                                File.getMessage().getString("cropstorage.transfer.gui_enter_receiver_success")
                                        .replace("#player#", message)));
                    });
                }
            } else {
                SoundManager.playChatErrorSound(p);
                p.sendMessage(ChatUtils.colorize(
                        File.getMessage().getString("cropstorage.transfer.failed_offline")
                                .replace("#player#", message)));
            }
            CropTransferGUI.setWaitingForReceiver(p, false);
            e.setCancelled(true);
        }

        // Handle MobTransferGUI amount input
        if (MobTransferGUI.isWaitingForInput(p)) {
            if (isCancelCommand(message)) {
                MobTransferGUI.setWaitingForInput(p, false);
                MobTransferGUI activeGUI = MobTransferGUI.getActiveGUI(p);
                if (activeGUI != null) {
                    handleCancel(p, activeGUI::updateGUI);
                }
                e.setCancelled(true);
                return;
            }
            if (Number.getInteger(message) > 0) {
                MobTransferGUI activeGUI = MobTransferGUI.getActiveGUI(p);
                if (activeGUI != null) {
                    int amount = Number.getInteger(message);
                    SchedulerUtil.runTask(Storage.getStorage(), () -> {
                        activeGUI.setTransferAmountAndUpdate(amount);
                        p.sendMessage(ChatUtils.colorize(
                                File.getMessage().getString("mobstorage.transfer.gui_enter_amount_success")
                                        .replace("#amount#", String.valueOf(amount))));
                    });
                }
            } else {
                SoundManager.playChatErrorSound(p);
                p.sendMessage(
                        ChatUtils.colorize(Objects.requireNonNull(File.getMessage().getString("user.unknown_number"))
                                .replace("<number>", message)));
            }
            MobTransferGUI.setWaitingForInput(p, false);
            e.setCancelled(true);
        }

        // Handle CropTransferMultiGUI amount input
        if (chat_crop_multi_transfer_item.containsKey(playerId)
                && chat_crop_multi_transfer_item.get(playerId) != null) {
            if (isCancelCommand(message)) {
                CropTransferMultiGUI gui = CropTransferMultiGUI.getActiveGUI(p);
                String target = chat_crop_multi_transfer_target.get(playerId);
                chat_crop_multi_transfer_item.remove(playerId);
                chat_crop_multi_transfer_target.remove(playerId);
                if (gui != null) {
                    handleCancel(p, () -> p.openInventory(
                            gui.getInventory(SoundContext.SILENT)));
                } else if (target != null) {
                    handleCancel(p, () -> p.openInventory(
                            new CropTransferMultiGUI(p, target)
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
                String itemName = chat_crop_multi_transfer_item.get(playerId);
                CropTransferMultiGUI gui = CropTransferMultiGUI.getActiveGUI(p);
                SchedulerUtil.runTask(Storage.getStorage(), () -> {
                    if (gui != null) {
                        gui.setSelectedAmount(itemName, amount);
                        p.openInventory(gui.getInventory(SoundContext.SILENT));
                    } else {
                        String target = chat_crop_multi_transfer_target.get(playerId);
                        if (target != null) {
                            CropTransferMultiGUI newGui = new CropTransferMultiGUI(p, target);
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
            chat_crop_multi_transfer_item.remove(playerId);
            chat_crop_multi_transfer_target.remove(playerId);
            e.setCancelled(true);
        }

        // Handle CropTransferMultiGUI receiver input
        if (CropTransferMultiGUI.isWaitingForReceiver(p)) {
            if (isCancelCommand(message)) {
                CropTransferMultiGUI.setWaitingForReceiver(p, false);
                CropTransferMultiGUI activeGUI = CropTransferMultiGUI.getActiveGUI(p);
                if (activeGUI != null) {
                    handleCancel(p, activeGUI::updateGUI);
                }
                e.setCancelled(true);
                return;
            }
            Player targetPlayer = Bukkit.getPlayer(message);
            if (targetPlayer != null && targetPlayer.isOnline()) {
                CropTransferMultiGUI activeGUI = CropTransferMultiGUI.getActiveGUI(p);
                if (activeGUI != null) {
                    SchedulerUtil.runTask(Storage.getStorage(), () -> {
                        activeGUI.setTargetPlayerAndUpdate(message);
                        p.sendMessage(ChatUtils.colorize(
                                File.getMessage().getString("cropstorage.transfer.gui_enter_receiver_success")
                                        .replace("#player#", message)));
                    });
                }
            } else {
                SoundManager.playChatErrorSound(p);
                p.sendMessage(ChatUtils.colorize(
                        File.getMessage().getString("cropstorage.transfer.failed_offline")
                                .replace("#player#", message)));
            }
            CropTransferMultiGUI.setWaitingForReceiver(p, false);
            e.setCancelled(true);
        }

        // Handle MobTransferMultiGUI amount input
        if (chat_mob_multi_transfer_item.containsKey(playerId)
                && chat_mob_multi_transfer_item.get(playerId) != null) {
            if (isCancelCommand(message)) {
                MobTransferMultiGUI gui = MobTransferMultiGUI.getActiveGUI(p);
                String target = chat_mob_multi_transfer_target.get(playerId);
                chat_mob_multi_transfer_item.remove(playerId);
                chat_mob_multi_transfer_target.remove(playerId);
                if (gui != null) {
                    handleCancel(p, () -> p.openInventory(
                            gui.getInventory(SoundContext.SILENT)));
                } else if (target != null) {
                    handleCancel(p, () -> p.openInventory(
                            new MobTransferMultiGUI(p, target)
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
                String itemName = chat_mob_multi_transfer_item.get(playerId);
                MobTransferMultiGUI gui = MobTransferMultiGUI.getActiveGUI(p);
                SchedulerUtil.runTask(Storage.getStorage(), () -> {
                    if (gui != null) {
                        gui.setSelectedAmount(itemName, amount);
                        p.openInventory(gui.getInventory(SoundContext.SILENT));
                    } else {
                        String target = chat_mob_multi_transfer_target.get(playerId);
                        if (target != null) {
                            MobTransferMultiGUI newGui = new MobTransferMultiGUI(p, target);
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
            chat_mob_multi_transfer_item.remove(playerId);
            chat_mob_multi_transfer_target.remove(playerId);
            e.setCancelled(true);
        }

        // Handle MobTransferMultiGUI receiver input
        if (MobTransferMultiGUI.isWaitingForReceiver(p)) {
            if (isCancelCommand(message)) {
                MobTransferMultiGUI.setWaitingForReceiver(p, false);
                MobTransferMultiGUI activeGUI = MobTransferMultiGUI.getActiveGUI(p);
                if (activeGUI != null) {
                    handleCancel(p, activeGUI::updateGUI);
                }
                e.setCancelled(true);
                return;
            }
            Player targetPlayer = Bukkit.getPlayer(message);
            if (targetPlayer != null && targetPlayer.isOnline()) {
                MobTransferMultiGUI activeGUI = MobTransferMultiGUI.getActiveGUI(p);
                if (activeGUI != null) {
                    SchedulerUtil.runTask(Storage.getStorage(), () -> {
                        activeGUI.setTargetPlayerAndUpdate(message);
                        p.sendMessage(ChatUtils.colorize(
                                File.getMessage().getString("mobstorage.transfer.gui_enter_receiver_success")
                                        .replace("#player#", message)));
                    });
                }
            } else {
                SoundManager.playChatErrorSound(p);
                p.sendMessage(ChatUtils.colorize(
                        File.getMessage().getString("mobstorage.transfer.failed_offline")
                                .replace("#player#", message)));
            }
            MobTransferMultiGUI.setWaitingForReceiver(p, false);
            e.setCancelled(true);
        }
    }
}
