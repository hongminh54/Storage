package net.danh.storage.Manager;

import net.danh.storage.Data.TransferData;
import net.danh.storage.Database.TransferDatabase;
import net.danh.storage.Storage;
import net.danh.storage.Utils.ChatNavigationHelper;
import net.danh.storage.Utils.ChatUtils;
import net.danh.storage.Utils.File;
import net.danh.storage.Utils.TaskWrapper;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.text.SimpleDateFormat;
import java.util.*;

public class TransferManager {

    private static final Map<String, TaskWrapper> activeTransfers = new HashMap<>();
    private static TransferDatabase transferDatabase;

    public static void initialize() {
        transferDatabase = new TransferDatabase(Storage.getStorage());
        transferDatabase.createTransferTable();
    }

    public static boolean canTransfer(Player sender, String receiverName, String material, int amount) {
        if (sender == null || receiverName == null || material == null) {
            return false;
        }

        FileConfiguration config = File.getConfig();

        if (!config.getBoolean("transfer.enabled", true)) {
            return false;
        }

        if (!sender.hasPermission("storage.transfer.use")) {
            sender.sendMessage(ChatUtils.colorize(File.getMessage().getString("admin.no_permission")));
            return false;
        }

        if (sender.getName().equalsIgnoreCase(receiverName)) {
            sender.sendMessage(ChatUtils.colorize(File.getMessage().getString("transfer.failed_same_player")));
            return false;
        }

        Player receiver = Bukkit.getPlayer(receiverName);
        if (receiver == null || !receiver.isOnline()) {
            sender.sendMessage(ChatUtils.colorize(File.getMessage().getString("transfer.failed_offline").replace("#player#", receiverName)));
            return false;
        }

        if (amount <= 0) {
            sender.sendMessage(ChatUtils.colorize(File.getMessage().getString("admin.number_too_low")));
            return false;
        }

        if (!MineManager.getPluginBlocks().contains(material)) {
            String materialsStr = String.join(", ", MineManager.getPluginBlocks().size() > 10 ?
                    new ArrayList<>(MineManager.getPluginBlocks()).subList(0, 10) : MineManager.getPluginBlocks());
            if (MineManager.getPluginBlocks().size() > 10) {
                materialsStr += "...";
            }
            sender.sendMessage(ChatUtils.colorize(File.getMessage().getString("admin.invalid_material")
                    .replace("#material#", material).replace("#materials#", materialsStr)));
            return false;
        }

        int currentAmount = MineManager.getPlayerBlock(sender, material);
        if (currentAmount < amount) {
            sender.sendMessage(ChatUtils.colorize(File.getMessage().getString("transfer.failed_insufficient").replace("#material#", getDisplayName(material)).replace("#current#", String.valueOf(currentAmount))));
            return false;
        }

        if (isTransferInProgress(sender)) {
            sender.sendMessage(ChatUtils.colorize(File.getMessage().getString("transfer.failed_in_progress")));
            return false;
        }

        return true;
    }

    private static boolean canTransferForMulti(Player sender, String receiverName, String material, int amount) {
        if (sender == null || receiverName == null || material == null) {
            return false;
        }

        FileConfiguration config = File.getConfig();

        if (!config.getBoolean("transfer.enabled", true)) {
            return false;
        }

        if (!sender.hasPermission("storage.transfer.multi")) {
            sender.sendMessage(ChatUtils.colorize(File.getMessage().getString("admin.no_permission")));
            return false;
        }

        if (sender.getName().equalsIgnoreCase(receiverName)) {
            sender.sendMessage(ChatUtils.colorize(File.getMessage().getString("transfer.failed_same_player")));
            return false;
        }

        Player receiver = Bukkit.getPlayer(receiverName);
        if (receiver == null || !receiver.isOnline()) {
            sender.sendMessage(ChatUtils.colorize(File.getMessage().getString("transfer.failed_offline").replace("#player#", receiverName)));
            return false;
        }

        if (amount <= 0) {
            sender.sendMessage(ChatUtils.colorize(File.getMessage().getString("admin.number_too_low")));
            return false;
        }

        if (!MineManager.getPluginBlocks().contains(material)) {
            String materialsStr = String.join(", ", MineManager.getPluginBlocks().size() > 10 ?
                    new ArrayList<>(MineManager.getPluginBlocks()).subList(0, 10) : MineManager.getPluginBlocks());
            if (MineManager.getPluginBlocks().size() > 10) {
                materialsStr += "...";
            }
            sender.sendMessage(ChatUtils.colorize(File.getMessage().getString("admin.invalid_material")
                    .replace("#material#", material).replace("#materials#", materialsStr)));
            return false;
        }

        int currentAmount = MineManager.getPlayerBlock(sender, material);
        if (currentAmount < amount) {
            sender.sendMessage(ChatUtils.colorize(File.getMessage().getString("transfer.failed_insufficient")
                    .replace("#material#", getDisplayName(material))
                    .replace("#current#", String.valueOf(currentAmount))));
            return false;
        }

        int maxTransferable = MineManager.getMaxTransferableAmount(sender, receiver, material);
        if (maxTransferable <= 0) {
            sender.sendMessage(ChatUtils.colorize(File.getMessage().getString("transfer.failed_receiver_full")
                    .replace("#player#", receiverName)));
            return false;
        }

        return true;
    }

    public static int getOptimalTransferAmount(Player sender, String receiverName, String material, int requestedAmount) {
        Player receiver = Bukkit.getPlayer(receiverName);
        if (receiver == null) {
            return 0;
        }

        int maxTransferable = MineManager.getMaxTransferableAmount(sender, receiver, material);
        return Math.min(requestedAmount, maxTransferable);
    }

    public static boolean executeTransfer(Player sender, String receiverName, String material, int amount) {
        if (!canTransfer(sender, receiverName, material, amount)) {
            return false;
        }

        Player receiver = Bukkit.getPlayer(receiverName);
        if (receiver == null) {
            return false;
        }

        // Start transfer process with delay
        startTransferProcess(sender, receiver, material, amount);
        return true;
    }

    public static boolean executeMultiTransfer(Player sender, String receiverName, Map<String, Integer> materials) {
        if (materials == null || materials.isEmpty()) {
            return false;
        }

        Player receiver = Bukkit.getPlayer(receiverName);
        if (receiver == null) {
            return false;
        }

        Map<String, Integer> optimizedMaterials = MineManager.calculateOptimalMultiTransfer(sender, receiver, materials);

        if (optimizedMaterials.isEmpty()) {
            sender.sendMessage(ChatUtils.colorize(File.getMessage().getString("transfer.failed_receiver_full")
                    .replace("#player#", receiverName)));
            return false;
        }

        for (Map.Entry<String, Integer> entry : optimizedMaterials.entrySet()) {
            if (!canTransferForMulti(sender, receiverName, entry.getKey(), entry.getValue())) {
                return false;
            }
        }

        startMultiTransferProcess(sender, receiver, optimizedMaterials);
        return true;
    }

    private static void startTransferProcess(Player sender, Player receiver, String material, int amount) {
        FileConfiguration config = File.getConfig();
        int transferDelay = config.getInt("transfer.delay", 3); // Default 3 seconds

        String displayName = getDisplayName(material);

        // Notify players that transfer is starting
        sender.sendMessage(ChatUtils.colorize(File.getMessage().getString("transfer.processing_send").replace("#amount#", String.valueOf(amount)).replace("#material#", displayName).replace("#player#", receiver.getName()).replace("#time#", String.valueOf(transferDelay))));

        receiver.sendMessage(ChatUtils.colorize(File.getMessage().getString("transfer.processing_receive").replace("#amount#", String.valueOf(amount)).replace("#material#", displayName).replace("#player#", sender.getName()).replace("#time#", String.valueOf(transferDelay))));

        // Cancel any existing transfer for this player
        cancelTransfer(sender);

        // Start processing animation
        ParticleManager.playTransferProcessingAnimation(sender, transferDelay);

        // Create and start the transfer task
        TaskWrapper transferTask = TaskWrapper.runTaskLater(Storage.getStorage(), () -> {
            // Stop processing animation
            ParticleManager.stopTransferProcessingAnimation(sender);
            completeTransfer(sender, receiver, material, amount);
            activeTransfers.remove(sender.getName());
        }, transferDelay * 20L);

        activeTransfers.put(sender.getName(), transferTask);
    }

    private static void startMultiTransferProcess(Player sender, Player receiver, Map<String, Integer> materials) {
        FileConfiguration config = File.getConfig();
        int transferDelay = config.getInt("transfer.delay", 3); // Default 3 seconds

        int totalItems = materials.values().stream().mapToInt(Integer::intValue).sum();

        // Create materials list for processing message
        StringBuilder materialsList = new StringBuilder();
        int count = 0;
        for (Map.Entry<String, Integer> entry : materials.entrySet()) {
            if (count > 0) {
                materialsList.append(", ");
            }
            materialsList.append(entry.getValue()).append(" ").append(getDisplayName(entry.getKey()));
            count++;
        }

        // Notify players that multi transfer is starting
        sender.sendMessage(ChatUtils.colorize(File.getMessage().getString("transfer.processing_multi_send_detailed")
                .replace("#materials#", materialsList.toString())
                .replace("#player#", receiver.getName())
                .replace("#time#", String.valueOf(transferDelay))));

        receiver.sendMessage(ChatUtils.colorize(File.getMessage().getString("transfer.processing_multi_receive_detailed")
                .replace("#materials#", materialsList.toString())
                .replace("#player#", sender.getName())
                .replace("#time#", String.valueOf(transferDelay))));

        // Cancel any existing transfer for this player
        cancelTransfer(sender);

        // Start processing animation
        ParticleManager.playTransferProcessingAnimation(sender, transferDelay);

        // Create and start the multi transfer task
        TaskWrapper transferTask = TaskWrapper.runTaskLater(Storage.getStorage(), () -> {
            // Stop processing animation
            ParticleManager.stopTransferProcessingAnimation(sender);
            completeMultiTransfer(sender, receiver, materials);
            activeTransfers.remove(sender.getName());
        }, transferDelay * 20L);

        activeTransfers.put(sender.getName(), transferTask);
    }

    private static void completeMultiTransfer(Player sender, Player receiver, Map<String, Integer> materials) {
        // Double-check conditions before completing transfer
        if (!sender.isOnline() || !receiver.isOnline()) {
            if (sender.isOnline()) {
                sender.sendMessage(ChatUtils.colorize(File.getMessage().getString("transfer.failed_offline_during").replace("#player#", receiver.getName())));
            }
            return;
        }

        int successCount = 0;
        int totalCount = materials.size();
        StringBuilder successfulTransfers = new StringBuilder();

        for (Map.Entry<String, Integer> entry : materials.entrySet()) {
            String material = entry.getKey();
            int amount = entry.getValue();

            // Check if sender still has enough of this material
            int currentAmount = MineManager.getPlayerBlock(sender, material);
            if (currentAmount < amount) {
                String displayName = getDisplayName(material);
                sender.sendMessage(ChatUtils.colorize(File.getMessage().getString("transfer.failed_insufficient_during")
                        .replace("#material#", displayName)));
                continue;
            }

            // Perform the transfer
            if (MineManager.removeBlockAmount(sender, material, amount)) {
                if (MineManager.addBlockAmount(receiver, material, amount)) {
                    // Log successful transfer
                    TransferData transferData = new TransferData(
                            sender.getName(),
                            receiver.getName(),
                            material,
                            amount,
                            System.currentTimeMillis(),
                            "SUCCESS_MULTI"
                    );
                    transferDatabase.insertTransfer(transferData);

                    // Add to successful transfers list
                    if (successCount > 0) {
                        successfulTransfers.append(", ");
                    }
                    successfulTransfers.append(amount).append(" ").append(getDisplayName(material));
                    successCount++;
                } else {
                    // Rollback if receiver couldn't receive
                    MineManager.addBlockAmount(sender, material, amount);
                    sender.sendMessage(ChatUtils.colorize(File.getMessage().getString("transfer.failed_receiver_full")
                            .replace("#player#", receiver.getName())));
                }
            }
        }

        // Send completion messages
        if (successCount > 0) {
            String transferList = successfulTransfers.toString();

            sender.sendMessage(ChatUtils.colorize(File.getMessage().getString("transfer.success_multi_send_detailed")
                    .replace("#materials#", transferList)
                    .replace("#player#", receiver.getName())));

            receiver.sendMessage(ChatUtils.colorize(File.getMessage().getString("transfer.success_multi_receive_detailed")
                    .replace("#materials#", transferList)
                    .replace("#player#", sender.getName())));

            playTransferEffects(sender, receiver);
        } else {
            sender.sendMessage(ChatUtils.colorize(File.getMessage().getString("transfer.failed_multi_all")));
        }
    }

    private static void completeTransfer(Player sender, Player receiver, String material, int amount) {
        // Double-check conditions before completing transfer
        if (!sender.isOnline() || !receiver.isOnline()) {
            handleFailedTransfer(sender, receiver.getName(), material, amount, "PLAYER_OFFLINE");
            return;
        }

        int currentAmount = MineManager.getPlayerBlock(sender, material);
        if (currentAmount < amount) {
            handleFailedTransfer(sender, receiver.getName(), material, amount, "INSUFFICIENT_RESOURCES");
            return;
        }

        if (MineManager.removeBlockAmount(sender, material, amount)) {
            if (MineManager.addBlockAmount(receiver, material, amount)) {
                handleSuccessfulTransfer(sender, receiver, material, amount);
            } else {
                // Rollback if receiver couldn't receive
                MineManager.addBlockAmount(sender, material, amount);
                handleFailedTransfer(sender, receiver.getName(), material, amount, "RECEIVER_FULL");
            }
        } else {
            handleFailedTransfer(sender, receiver.getName(), material, amount, "REMOVAL_FAILED");
        }
    }

    private static void handleSuccessfulTransfer(Player sender, Player receiver, String material, int amount) {
        long timestamp = System.currentTimeMillis();
        TransferData transferData = new TransferData(sender.getName(), receiver.getName(), material, amount, timestamp, "SUCCESS");

        transferDatabase.insertTransfer(transferData);

        String displayName = getDisplayName(material);

        sender.sendMessage(ChatUtils.colorize(File.getMessage().getString("transfer.success_send").replace("#amount#", String.valueOf(amount)).replace("#material#", displayName).replace("#player#", receiver.getName())));

        receiver.sendMessage(ChatUtils.colorize(File.getMessage().getString("transfer.success_receive").replace("#amount#", String.valueOf(amount)).replace("#material#", displayName).replace("#player#", sender.getName())));

        playTransferEffects(sender, receiver);
    }

    private static void handleFailedTransfer(Player sender, String receiverName, String material, int amount, String reason) {
        TransferData failedTransfer = new TransferData(sender.getName(), receiverName, material, amount, System.currentTimeMillis(), "FAILED_" + reason);
        transferDatabase.insertTransfer(failedTransfer);

        String displayName = getDisplayName(material);
        String errorMessage = getErrorMessage(reason, displayName, receiverName);

        if (sender.isOnline()) {
            sender.sendMessage(ChatUtils.colorize(errorMessage));
            ParticleManager.playTransferFailedParticle(sender);
        }
    }

    private static String getErrorMessage(String reason, String materialName, String receiverName) {
        switch (reason) {
            case "PLAYER_OFFLINE":
                return File.getMessage().getString("transfer.failed_offline_during").replace("#player#", receiverName);
            case "INSUFFICIENT_RESOURCES":
                return File.getMessage().getString("transfer.failed_insufficient_during").replace("#material#", materialName);
            case "RECEIVER_FULL":
                return File.getMessage().getString("transfer.failed_receiver_full").replace("#player#", receiverName);
            default:
                return File.getMessage().getString("transfer.failed_unknown");
        }
    }

    private static void playTransferEffects(Player sender, Player receiver) {
        FileConfiguration config = File.getConfig();

        if (config.getBoolean("transfer.sounds.enabled", true)) {
            String successSound = config.getString("transfer.sounds.success.name");
            if (successSound != null && !successSound.equalsIgnoreCase("none")) {
                float volume = (float) config.getDouble("transfer.sounds.success.volume", 1.0);
                float pitch = (float) config.getDouble("transfer.sounds.success.pitch", 1.2);

                // Handle legacy sound names
                if (successSound.equals("ORB_PICKUP")) {
                    successSound = "ENTITY_EXPERIENCE_ORB_PICKUP";
                }
                SoundManager.playSound(sender, successSound, volume, pitch);
            }

            String receiveSound = config.getString("transfer.sounds.receive.name");
            if (receiveSound != null && !receiveSound.equalsIgnoreCase("none")) {
                float volume = (float) config.getDouble("transfer.sounds.receive.volume", 0.8);
                float pitch = (float) config.getDouble("transfer.sounds.receive.pitch", 1.0);

                // Handle legacy sound names
                if (receiveSound.equals("ITEM_PICKUP")) {
                    receiveSound = "ENTITY_ITEM_PICKUP";
                }
                SoundManager.playSound(receiver, receiveSound, volume, pitch);
            }
        }

        // Play beam effect from sender to receiver
        ParticleManager.playTransferBeamEffect(sender, receiver);

        // Play enhanced success and receive particles
        ParticleManager.playTransferSuccessParticle(sender);
        ParticleManager.playTransferReceiveParticle(receiver);
    }

    public static boolean isTransferInProgress(Player player) {
        return activeTransfers.containsKey(player.getName());
    }

    public static void cancelTransfer(Player player) {
        TaskWrapper task = activeTransfers.remove(player.getName());
        if (task != null) {
            task.cancel();
            player.sendMessage(ChatUtils.colorize(File.getMessage().getString("transfer.cancelled")));
        }
        // Stop any processing animation
        ParticleManager.stopTransferProcessingAnimation(player);
    }

    public static void cancelAllTransfers() {
        for (TaskWrapper task : activeTransfers.values()) {
            task.cancel();
        }
        activeTransfers.clear();
    }

    public static void displayTransferHistory(Player player, String targetPlayerName) {
        displayTransferHistory(player, targetPlayerName, 1);
    }

    public static void displayTransferHistory(Player player, String targetPlayerName, int page) {
        String playerToCheck = targetPlayerName != null ? targetPlayerName : player.getName();

        if (!player.getName().equalsIgnoreCase(playerToCheck) && !player.hasPermission("storage.transfer.log.others")) {
            player.sendMessage(ChatUtils.colorizewp(File.getMessage().getString("admin.no_permission")));
            return;
        }

        if (!player.hasPermission("storage.transfer.log")) {
            player.sendMessage(ChatUtils.colorizewp(File.getMessage().getString("admin.no_permission")));
            return;
        }

        FileConfiguration config = File.getConfig();
        int itemsPerPage = config.getInt("transfer.max_history_display", 10);

        // Ensure page is at least 1
        page = Math.max(1, page);
        int offset = (page - 1) * itemsPerPage;

        // Get total count and calculate total pages
        int totalTransfers = transferDatabase.getTotalTransferCount(playerToCheck);
        int totalPages = Math.max(1, (int) Math.ceil((double) totalTransfers / itemsPerPage));

        // Ensure page doesn't exceed total pages
        page = Math.min(page, totalPages);
        offset = (page - 1) * itemsPerPage;

        List<TransferData> transfers = transferDatabase.getTransferHistory(playerToCheck, itemsPerPage, offset);

        if (transfers.isEmpty()) {
            player.sendMessage(ChatUtils.colorizewp(File.getMessage().getString("transfer.log_no_history").replace("#player#", playerToCheck)));
            return;
        }

        // Send header with page info
        String headerMessage = File.getMessage().getString("transfer.log_header_paginated")
                .replace("#player#", playerToCheck)
                .replace("#current_page#", String.valueOf(page))
                .replace("#total_pages#", String.valueOf(totalPages));
        player.sendMessage(ChatUtils.colorizewp(headerMessage));

        // Add empty line for better spacing
        player.sendMessage("");

        SimpleDateFormat dateFormat = new SimpleDateFormat("MM/dd HH:mm");

        for (TransferData transfer : transfers) {
            String timeStr = dateFormat.format(new Date(transfer.getTimestamp()));
            String displayName = getDisplayName(transfer.getMaterial());

            if (transfer.getSender().equalsIgnoreCase(playerToCheck)) {
                String message = File.getMessage().getString("transfer.log_entry_sent").replace("#time#", timeStr).replace("#amount#", String.valueOf(transfer.getAmount())).replace("#material#", displayName).replace("#receiver#", transfer.getReceiver());
                player.sendMessage(ChatUtils.colorizewp(message));
            } else {
                String message = File.getMessage().getString("transfer.log_entry_received").replace("#time#", timeStr).replace("#amount#", String.valueOf(transfer.getAmount())).replace("#material#", displayName).replace("#sender#", transfer.getSender());
                player.sendMessage(ChatUtils.colorizewp(message));
            }
        }

        // Send pagination footer with clickable navigation
        sendPaginationFooter(player, playerToCheck, page, totalPages, totalTransfers);
    }

    private static void sendPaginationFooter(Player player, String targetPlayer, int currentPage, int totalPages, int totalTransfers) {
        FileConfiguration messageConfig = File.getMessage();

        // Add empty line and separator
        player.sendMessage("");
        String separator = messageConfig.getString("transfer.log_nav_separator");
        player.sendMessage(ChatUtils.colorizewp(separator));

        // Show total count and page info on same line
        String footerInfo = messageConfig.getString("transfer.log_footer_info")
                .replace("#total#", String.valueOf(totalTransfers))
                .replace("#current#", String.valueOf(currentPage))
                .replace("#total_pages#", String.valueOf(totalPages));
        player.sendMessage(ChatUtils.colorizewp(footerInfo));

        // If only one page, don't show navigation
        if (totalPages <= 1) {
            return;
        }

        // Send navigation components
        sendNavigationComponents(player, targetPlayer, currentPage, totalPages, messageConfig);
    }

    private static void sendNavigationComponents(Player player, String targetPlayer, int currentPage, int totalPages, FileConfiguration messageConfig) {
        boolean hasPrev = currentPage > 1;
        boolean hasNext = currentPage < totalPages;
        int prevPage = hasPrev ? currentPage - 1 : 0;
        int nextPage = hasNext ? currentPage + 1 : 0;

        ChatNavigationHelper.sendStorageTransferNavigation(player, targetPlayer, currentPage, totalPages,
                prevPage, nextPage, hasPrev, hasNext);
    }


    private static String getDisplayName(String material) {
        FileConfiguration config = File.getConfig();
        String displayName = config.getString("items." + material);
        return displayName != null ? displayName : material.replace(";0", "").replace("_", " ");
    }

    public static TransferDatabase getTransferDatabase() {
        return transferDatabase;
    }
}
