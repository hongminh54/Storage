package net.danh.storage.Manager;

import net.danh.storage.Data.MythicTransferData;
import net.danh.storage.Database.MythicTransferDatabase;
import net.danh.storage.Storage;
import net.danh.storage.Utils.Chat;
import net.danh.storage.Utils.ChatNavigationHelper;
import net.danh.storage.Utils.File;
import net.danh.storage.Utils.TaskWrapper;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MythicTransferManager {

    private static final Map<String, TaskWrapper> activeTransfers = new HashMap<>();
    private static MythicTransferDatabase transferDatabase;

    public static void initialize() {
        transferDatabase = new MythicTransferDatabase(Storage.getStorage());
        transferDatabase.createTransferTable();
    }

    public static MythicTransferDatabase getTransferDatabase() {
        return transferDatabase;
    }

    public static boolean canTransfer(Player sender, String receiverName, String itemName, int amount) {
        if (sender == null || receiverName == null || itemName == null) {
            return false;
        }

        if (!MythicStorageManager.isSystemEnabled()) {
            sender.sendMessage(Chat.colorize(File.getMessage().getString("mythicstorage.system_disabled")));
            return false;
        }

        if (!sender.hasPermission("storage.mythicstorage.transfer")) {
            sender.sendMessage(Chat.colorize(File.getMessage().getString("admin.no_permission")));
            return false;
        }

        if (sender.getName().equalsIgnoreCase(receiverName)) {
            sender.sendMessage(Chat.colorize(File.getMessage().getString("mythicstorage.transfer.failed_same_player")));
            return false;
        }

        Player receiver = Bukkit.getPlayer(receiverName);
        if (receiver == null || !receiver.isOnline()) {
            sender.sendMessage(Chat.colorize(File.getMessage().getString("mythicstorage.transfer.failed_offline").replace("#player#", receiverName)));
            return false;
        }

        if (amount <= 0) {
            sender.sendMessage(Chat.colorize(File.getMessage().getString("admin.number_too_low")));
            return false;
        }

        if (!MythicStorageManager.isConfiguredDrop(itemName)) {
            sender.sendMessage(Chat.colorize(File.getMessage().getString("mythicstorage.invalid_item").replace("#item#", itemName)));
            return false;
        }

        int currentAmount = MythicStorageManager.getPlayerItem(sender, itemName);
        if (currentAmount < amount) {
            String displayName = MythicStorageManager.getItemDisplayNameOrId(itemName, sender);
            sender.sendMessage(Chat.colorize(File.getMessage().getString("mythicstorage.transfer.failed_insufficient")
                    .replace("#item#", displayName)
                    .replace("#current#", String.valueOf(currentAmount))));
            return false;
        }

        if (isTransferInProgress(sender)) {
            sender.sendMessage(Chat.colorize(File.getMessage().getString("mythicstorage.transfer.failed_in_progress")));
            return false;
        }

        return true;
    }

    private static boolean canTransferForMulti(Player sender, String receiverName, String itemName, int amount) {
        if (sender == null || receiverName == null || itemName == null) {
            return false;
        }

        if (!MythicStorageManager.isSystemEnabled()) {
            return false;
        }

        if (!sender.hasPermission("storage.mythicstorage.transfer.multi")) {
            sender.sendMessage(Chat.colorize(File.getMessage().getString("admin.no_permission")));
            return false;
        }

        if (sender.getName().equalsIgnoreCase(receiverName)) {
            sender.sendMessage(Chat.colorize(File.getMessage().getString("mythicstorage.transfer.failed_same_player")));
            return false;
        }

        Player receiver = Bukkit.getPlayer(receiverName);
        if (receiver == null || !receiver.isOnline()) {
            sender.sendMessage(Chat.colorize(File.getMessage().getString("mythicstorage.transfer.failed_offline").replace("#player#", receiverName)));
            return false;
        }

        if (amount <= 0) {
            sender.sendMessage(Chat.colorize(File.getMessage().getString("admin.number_too_low")));
            return false;
        }

        if (!MythicStorageManager.isConfiguredDrop(itemName)) {
            sender.sendMessage(Chat.colorize(File.getMessage().getString("mythicstorage.invalid_item").replace("#item#", itemName)));
            return false;
        }

        int currentAmount = MythicStorageManager.getPlayerItem(sender, itemName);
        if (currentAmount < amount) {
            String displayName = MythicStorageManager.getItemDisplayNameOrId(itemName, sender);
            sender.sendMessage(Chat.colorize(File.getMessage().getString("mythicstorage.transfer.failed_insufficient")
                    .replace("#item#", displayName)
                    .replace("#current#", String.valueOf(currentAmount))));
            return false;
        }

        return true;
    }

    public static boolean executeTransfer(Player sender, String receiverName, String itemName, int amount) {
        if (!canTransfer(sender, receiverName, itemName, amount)) {
            return false;
        }

        Player receiver = Bukkit.getPlayer(receiverName);
        if (receiver == null) {
            return false;
        }

        startTransferProcess(sender, receiver, itemName, amount);
        return true;
    }

    public static boolean executeMultiTransfer(Player sender, String receiverName, Map<String, Integer> items) {
        if (items == null || items.isEmpty()) {
            return false;
        }

        Player receiver = Bukkit.getPlayer(receiverName);
        if (receiver == null) {
            return false;
        }

        for (Map.Entry<String, Integer> entry : items.entrySet()) {
            if (!canTransferForMulti(sender, receiverName, entry.getKey(), entry.getValue())) {
                return false;
            }
        }

        startMultiTransferProcess(sender, receiver, items);
        return true;
    }

    private static void startTransferProcess(Player sender, Player receiver, String itemName, int amount) {
        FileConfiguration config = File.getMythicStorageConfig();
        int transferDelay = config.getInt("transfer.delay", 3);

        String displayName = MythicStorageManager.getItemDisplayNameOrId(itemName, sender);

        sender.sendMessage(Chat.colorize(File.getMessage().getString("mythicstorage.transfer.processing_send")
                .replace("#amount#", String.valueOf(amount))
                .replace("#item#", displayName)
                .replace("#player#", receiver.getName())
                .replace("#time#", String.valueOf(transferDelay))));

        receiver.sendMessage(Chat.colorize(File.getMessage().getString("mythicstorage.transfer.processing_receive")
                .replace("#amount#", String.valueOf(amount))
                .replace("#item#", displayName)
                .replace("#player#", sender.getName())
                .replace("#time#", String.valueOf(transferDelay))));

        cancelTransfer(sender);

        playMythicTransferProcessingAnimation(sender, transferDelay);

        TaskWrapper transferTask = TaskWrapper.runTaskLater(Storage.getStorage(), () -> {
            stopMythicTransferProcessingAnimation(sender);
            completeTransfer(sender, receiver, itemName, amount);
            activeTransfers.remove(sender.getName());
        }, transferDelay * 20L);

        activeTransfers.put(sender.getName(), transferTask);
    }

    private static void startMultiTransferProcess(Player sender, Player receiver, Map<String, Integer> items) {
        FileConfiguration config = File.getMythicStorageConfig();
        int transferDelay = config.getInt("transfer.delay", 3);

        int totalItems = items.values().stream().mapToInt(Integer::intValue).sum();

        StringBuilder itemsList = new StringBuilder();
        int count = 0;
        for (Map.Entry<String, Integer> entry : items.entrySet()) {
            if (count > 0) {
                itemsList.append(", ");
            }
            String displayName = MythicStorageManager.getItemDisplayNameOrId(entry.getKey(), sender);
            itemsList.append(entry.getValue()).append(" ").append(displayName);
            count++;
        }

        sender.sendMessage(Chat.colorize(File.getMessage().getString("mythicstorage.transfer.processing_multi_send_detailed")
                .replace("#items#", itemsList.toString())
                .replace("#player#", receiver.getName())
                .replace("#time#", String.valueOf(transferDelay))));

        receiver.sendMessage(Chat.colorize(File.getMessage().getString("mythicstorage.transfer.processing_multi_receive_detailed")
                .replace("#items#", itemsList.toString())
                .replace("#player#", sender.getName())
                .replace("#time#", String.valueOf(transferDelay))));

        cancelTransfer(sender);

        playMythicTransferProcessingAnimation(sender, transferDelay);

        TaskWrapper transferTask = TaskWrapper.runTaskLater(Storage.getStorage(), () -> {
            stopMythicTransferProcessingAnimation(sender);
            completeMultiTransfer(sender, receiver, items);
            activeTransfers.remove(sender.getName());
        }, transferDelay * 20L);

        activeTransfers.put(sender.getName(), transferTask);
    }

    private static void completeTransfer(Player sender, Player receiver, String itemName, int amount) {
        if (!sender.isOnline() || !receiver.isOnline()) {
            handleFailedTransfer(sender, receiver.getName(), itemName, amount, "PLAYER_OFFLINE");
            return;
        }

        int currentAmount = MythicStorageManager.getPlayerItem(sender, itemName);
        if (currentAmount < amount) {
            handleFailedTransfer(sender, receiver.getName(), itemName, amount, "INSUFFICIENT_RESOURCES");
            return;
        }

        if (MythicStorageManager.removeItemAmount(sender, itemName, amount)) {
            if (MythicStorageManager.addItemAmount(receiver, itemName, amount)) {
                handleSuccessfulTransfer(sender, receiver, itemName, amount);
            } else {
                MythicStorageManager.addItemAmount(sender, itemName, amount);
                handleFailedTransfer(sender, receiver.getName(), itemName, amount, "RECEIVER_FULL");
            }
        } else {
            handleFailedTransfer(sender, receiver.getName(), itemName, amount, "REMOVAL_FAILED");
        }
    }

    private static void handleSuccessfulTransfer(Player sender, Player receiver, String itemName, int amount) {
        long timestamp = System.currentTimeMillis();
        MythicTransferData transferData = new MythicTransferData(
                sender.getName(),
                receiver.getName(),
                itemName,
                amount,
                timestamp,
                "SUCCESS"
        );

        if (transferDatabase != null) {
            transferDatabase.insertTransfer(transferData);
        }

        String displayName = MythicStorageManager.getItemDisplayNameOrId(itemName, sender);

        sender.sendMessage(Chat.colorize(File.getMessage().getString("mythicstorage.transfer.success_send")
                .replace("#amount#", String.valueOf(amount))
                .replace("#item#", displayName)
                .replace("#player#", receiver.getName())));

        receiver.sendMessage(Chat.colorize(File.getMessage().getString("mythicstorage.transfer.success_receive")
                .replace("#amount#", String.valueOf(amount))
                .replace("#item#", displayName)
                .replace("#player#", sender.getName())));

        playTransferEffects(sender, receiver);
    }

    private static void completeMultiTransfer(Player sender, Player receiver, Map<String, Integer> items) {
        if (!sender.isOnline() || !receiver.isOnline()) {
            if (sender.isOnline()) {
                sender.sendMessage(Chat.colorize(File.getMessage().getString("mythicstorage.transfer.failed_offline_during").replace("#player#", receiver.getName())));
            }
            return;
        }

        int successCount = 0;
        int totalCount = items.size();
        StringBuilder successfulTransfers = new StringBuilder();

        for (Map.Entry<String, Integer> entry : items.entrySet()) {
            String itemName = entry.getKey();
            int amount = entry.getValue();

            int currentAmount = MythicStorageManager.getPlayerItem(sender, itemName);
            if (currentAmount < amount) {
                String displayName = MythicStorageManager.getItemDisplayNameOrId(itemName, sender);
                sender.sendMessage(Chat.colorize(File.getMessage().getString("mythicstorage.transfer.failed_insufficient_during")
                        .replace("#item#", displayName)));
                continue;
            }

            if (MythicStorageManager.removeItemAmount(sender, itemName, amount)) {
                if (MythicStorageManager.addItemAmount(receiver, itemName, amount)) {
                    long timestamp = System.currentTimeMillis();
                    MythicTransferData transferData = new MythicTransferData(
                            sender.getName(),
                            receiver.getName(),
                            itemName,
                            amount,
                            timestamp,
                            "SUCCESS_MULTI"
                    );
                    if (transferDatabase != null) {
                        transferDatabase.insertTransfer(transferData);
                    }

                    if (successCount > 0) {
                        successfulTransfers.append(", ");
                    }
                    String displayName = MythicStorageManager.getItemDisplayNameOrId(itemName, sender);
                    successfulTransfers.append(amount).append(" ").append(displayName);
                    successCount++;
                } else {
                    MythicStorageManager.addItemAmount(sender, itemName, amount);
                    String displayName = MythicStorageManager.getItemDisplayNameOrId(itemName, sender);
                    sender.sendMessage(Chat.colorize(File.getMessage().getString("mythicstorage.transfer.failed_receiver_full_during")
                            .replace("#item#", displayName)
                            .replace("#player#", receiver.getName())));
                }
            }
        }

        if (successCount > 0) {
            sender.sendMessage(Chat.colorize(File.getMessage().getString("mythicstorage.transfer.success_multi_send")
                    .replace("#count#", String.valueOf(successCount))
                    .replace("#total#", String.valueOf(totalCount))
                    .replace("#items#", successfulTransfers.toString())
                    .replace("#player#", receiver.getName())));

            receiver.sendMessage(Chat.colorize(File.getMessage().getString("mythicstorage.transfer.success_multi_receive")
                    .replace("#count#", String.valueOf(successCount))
                    .replace("#total#", String.valueOf(totalCount))
                    .replace("#items#", successfulTransfers.toString())
                    .replace("#player#", sender.getName())));

            playTransferEffects(sender, receiver);
        } else {
            sender.sendMessage(Chat.colorize(File.getMessage().getString("mythicstorage.transfer.failed_all_items")));
        }
    }

    private static void handleFailedTransfer(Player sender, String receiverName, String itemName, int amount, String reason) {
        MythicTransferData failedTransfer = new MythicTransferData(
                sender.getName(),
                receiverName,
                itemName,
                amount,
                System.currentTimeMillis(),
                "FAILED_" + reason
        );
        if (transferDatabase != null) {
            transferDatabase.insertTransfer(failedTransfer);
        }

        String displayName = MythicStorageManager.getItemDisplayNameOrId(itemName, sender);
        String errorMessage = getErrorMessage(reason, displayName, receiverName);

        if (sender.isOnline()) {
            sender.sendMessage(Chat.colorize(errorMessage));
            playMythicTransferFailedParticle(sender);
        }
    }

    private static String getErrorMessage(String reason, String itemName, String receiverName) {
        switch (reason) {
            case "PLAYER_OFFLINE":
                return File.getMessage().getString("mythicstorage.transfer.failed_offline_during").replace("#player#", receiverName);
            case "INSUFFICIENT_RESOURCES":
                return File.getMessage().getString("mythicstorage.transfer.failed_insufficient_during").replace("#item#", itemName);
            case "RECEIVER_FULL":
                return File.getMessage().getString("mythicstorage.transfer.failed_receiver_full").replace("#player#", receiverName);
            default:
                return File.getMessage().getString("mythicstorage.transfer.failed_unknown");
        }
    }

    private static void playTransferEffects(Player sender, Player receiver) {
        FileConfiguration config = File.getMythicStorageConfig();

        if (config.getBoolean("transfer.sounds.enabled", true)) {
            String successSound = config.getString("transfer.sounds.success.name");
            if (successSound != null && !successSound.equalsIgnoreCase("none")) {
                float volume = (float) config.getDouble("transfer.sounds.success.volume", 1.0);
                float pitch = (float) config.getDouble("transfer.sounds.success.pitch", 1.2);

                if (successSound.equals("ORB_PICKUP")) {
                    successSound = "ENTITY_EXPERIENCE_ORB_PICKUP";
                }
                SoundManager.playSound(sender, successSound, volume, pitch);
            }

            String receiveSound = config.getString("transfer.sounds.receive.name");
            if (receiveSound != null && !receiveSound.equalsIgnoreCase("none")) {
                float volume = (float) config.getDouble("transfer.sounds.receive.volume", 0.8);
                float pitch = (float) config.getDouble("transfer.sounds.receive.pitch", 1.0);

                if (receiveSound.equals("ITEM_PICKUP")) {
                    receiveSound = "ENTITY_ITEM_PICKUP";
                }
                SoundManager.playSound(receiver, receiveSound, volume, pitch);
            }
        }

        playMythicTransferBeamEffect(sender, receiver);
        playMythicTransferSuccessParticle(sender);
        playMythicTransferReceiveParticle(receiver);
    }

    // Mythic Transfer specific particle/animation methods - use mythicstorage.yml config
    private static void playMythicTransferProcessingAnimation(Player player, int durationSeconds) {
        FileConfiguration config = File.getMythicStorageConfig();
        if (!config.getBoolean("transfer.particles.enabled", true)) return;

        // Reuse ParticleManager animation logic
        ParticleManager.playTransferProcessingAnimation(player, durationSeconds);
    }

    private static void stopMythicTransferProcessingAnimation(Player player) {
        ParticleManager.stopTransferProcessingAnimation(player);
    }

    private static void playMythicTransferBeamEffect(Player sender, Player receiver) {
        FileConfiguration config = File.getMythicStorageConfig();
        if (!config.getBoolean("transfer.particles.enabled", true)) return;

        ParticleManager.playTransferBeamEffect(sender, receiver);
    }

    private static void playMythicTransferSuccessParticle(Player player) {
        FileConfiguration config = File.getMythicStorageConfig();
        if (!config.getBoolean("transfer.particles.enabled", true)) return;

        ParticleManager.playTransferSuccessParticle(player);
    }

    private static void playMythicTransferReceiveParticle(Player player) {
        FileConfiguration config = File.getMythicStorageConfig();
        if (!config.getBoolean("transfer.particles.enabled", true)) return;

        ParticleManager.playTransferReceiveParticle(player);
    }

    private static void playMythicTransferFailedParticle(Player player) {
        FileConfiguration config = File.getMythicStorageConfig();
        if (!config.getBoolean("transfer.particles.enabled", true)) return;

        ParticleManager.playTransferFailedParticle(player);
    }

    public static boolean isTransferInProgress(Player player) {
        return activeTransfers.containsKey(player.getName());
    }

    public static void cancelTransfer(Player player) {
        TaskWrapper task = activeTransfers.remove(player.getName());
        if (task != null) {
            task.cancel();
            player.sendMessage(Chat.colorize(File.getMessage().getString("mythicstorage.transfer.cancelled")));
        }
        stopMythicTransferProcessingAnimation(player);
    }

    public static void cancelAllTransfers() {
        for (TaskWrapper task : activeTransfers.values()) {
            task.cancel();
        }
        activeTransfers.clear();
    }

    public static void displayTransferHistory(Player player, String targetPlayerName, int page) {
        String playerToCheck = targetPlayerName != null ? targetPlayerName : player.getName();

        if (!player.getName().equalsIgnoreCase(playerToCheck) && !player.hasPermission("storage.mythicstorage.transfer.log.others")) {
            player.sendMessage(Chat.colorizewp(File.getMessage().getString("admin.no_permission")));
            return;
        }

        if (!player.hasPermission("storage.mythicstorage.transfer.log")) {
            player.sendMessage(Chat.colorizewp(File.getMessage().getString("admin.no_permission")));
            return;
        }

        FileConfiguration config = File.getMythicStorageConfig();
        int itemsPerPage = config.getInt("transfer.max_history_display", 10);

        if (transferDatabase == null) {
            player.sendMessage(Chat.colorize(File.getMessage().getString("mythicstorage.system_disabled")));
            return;
        }

        page = Math.max(1, page);
        int offset = (page - 1) * itemsPerPage;

        int totalTransfers = transferDatabase.getTotalTransferCount(playerToCheck);
        int totalPages = Math.max(1, (int) Math.ceil((double) totalTransfers / itemsPerPage));

        page = Math.min(page, totalPages);
        offset = (page - 1) * itemsPerPage;

        List<MythicTransferData> transfers = transferDatabase.getTransferHistory(playerToCheck, itemsPerPage, offset);

        if (transfers.isEmpty()) {
            player.sendMessage(Chat.colorizewp(File.getMessage().getString("mythicstorage.transfer.log_no_history").replace("#player#", playerToCheck)));
            return;
        }

        String headerMessage = File.getMessage().getString("mythicstorage.transfer.log_header_paginated")
                .replace("#player#", playerToCheck)
                .replace("#current_page#", String.valueOf(page))
                .replace("#total_pages#", String.valueOf(totalPages));
        player.sendMessage(Chat.colorizewp(headerMessage));

        player.sendMessage("");

        SimpleDateFormat dateFormat = new SimpleDateFormat("MM/dd HH:mm");

        for (MythicTransferData transfer : transfers) {
            String timeStr = dateFormat.format(new Date(transfer.getTimestamp()));
            String displayName = MythicStorageManager.getItemDisplayNameOrId(transfer.getItemName(), player);

            if (transfer.getSender().equalsIgnoreCase(playerToCheck)) {
                String message = File.getMessage().getString("mythicstorage.transfer.log_entry_sent")
                        .replace("#time#", timeStr)
                        .replace("#amount#", String.valueOf(transfer.getAmount()))
                        .replace("#item#", displayName)
                        .replace("#receiver#", transfer.getReceiver());
                player.sendMessage(Chat.colorizewp(message));
            } else {
                String message = File.getMessage().getString("mythicstorage.transfer.log_entry_received")
                        .replace("#time#", timeStr)
                        .replace("#amount#", String.valueOf(transfer.getAmount()))
                        .replace("#item#", displayName)
                        .replace("#sender#", transfer.getSender());
                player.sendMessage(Chat.colorizewp(message));
            }
        }

        sendPaginationFooter(player, playerToCheck, page, totalPages, totalTransfers);
    }

    private static void sendPaginationFooter(Player player, String targetPlayer, int currentPage, int totalPages, int totalTransfers) {
        FileConfiguration messageConfig = File.getMessage();

        player.sendMessage("");
        String separator = messageConfig.getString("mythicstorage.transfer.log_nav_separator");
        player.sendMessage(Chat.colorizewp(separator));

        String footerInfo = messageConfig.getString("mythicstorage.transfer.log_footer_info")
                .replace("#total#", String.valueOf(totalTransfers))
                .replace("#current#", String.valueOf(currentPage))
                .replace("#total_pages#", String.valueOf(totalPages));
        player.sendMessage(Chat.colorizewp(footerInfo));

        if (totalPages <= 1) {
            return;
        }

        sendNavigationComponents(player, targetPlayer, currentPage, totalPages, messageConfig);
    }

    private static void sendNavigationComponents(Player player, String targetPlayer, int currentPage, int totalPages, FileConfiguration messageConfig) {
        boolean hasPrev = currentPage > 1;
        boolean hasNext = currentPage < totalPages;
        int prevPage = hasPrev ? currentPage - 1 : 0;
        int nextPage = hasNext ? currentPage + 1 : 0;

        ChatNavigationHelper.sendMythicTransferNavigation(player, targetPlayer, currentPage, totalPages,
                prevPage, nextPage, hasPrev, hasNext);
    }


}
