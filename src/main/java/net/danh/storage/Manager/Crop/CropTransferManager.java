package net.danh.storage.Manager.Crop;

import net.danh.storage.API.events.CropStorageMultiTransferEvent;
import net.danh.storage.API.events.CropStorageTransferEvent;
import net.danh.storage.Data.CropTransferData;
import net.danh.storage.Database.CropTransferDatabase;
import net.danh.storage.Manager.ParticleManager;
import net.danh.storage.Manager.SoundManager;
import net.danh.storage.Storage;
import net.danh.storage.Utils.ChatUtils;
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

public class CropTransferManager {

    private static final Map<String, TaskWrapper> activeTransfers = new HashMap<>();
    private static CropTransferDatabase transferDatabase;

    public static void initialize() {
        transferDatabase = new CropTransferDatabase(Storage.getStorage());
        transferDatabase.createTransferTable();
    }

	private static void ensureInitialized() {
		if (transferDatabase == null) {
			initialize();
		}
	}

    public static boolean canTransfer(Player sender, String receiverName, String itemName, int amount) {
        if (sender == null || receiverName == null || itemName == null) {
            return false;
        }

        FileConfiguration config = File.getCropStorageConfig();

        if (!config.getBoolean("transfer.enabled", true)) {
            return false;
        }

        if (!sender.hasPermission("storage.cropstorage.transfer.use")) {
            sender.sendMessage(ChatUtils.colorize(File.getMessage().getString("admin.no_permission")));
            return false;
        }

        if (sender.getName().equalsIgnoreCase(receiverName)) {
            sender.sendMessage(ChatUtils.colorize(File.getMessage().getString("cropstorage.transfer.failed_same_player")));
            return false;
        }

        Player receiver = Bukkit.getPlayer(receiverName);
        if (receiver == null || !receiver.isOnline()) {
            sender.sendMessage(ChatUtils.colorize(File.getMessage().getString("cropstorage.transfer.failed_offline").replace("#player#", receiverName)));
            return false;
        }

        if (amount <= 0) {
            sender.sendMessage(ChatUtils.colorize(File.getMessage().getString("admin.number_too_low")));
            return false;
        }

        if (!CropStorageManager.isConfiguredDrop(itemName)) {
            sender.sendMessage(ChatUtils.colorize(File.getMessage().getString("cropstorage.transfer.failed_invalid_item")
                    .replace("#item#", CropStorageManager.getItemDisplayName(itemName))));
            return false;
        }

        int currentAmount = CropStorageManager.getPlayerItem(sender, itemName);
        if (currentAmount < amount) {
            sender.sendMessage(ChatUtils.colorize(File.getMessage().getString("cropstorage.transfer.failed_insufficient")
                    .replace("#item#", CropStorageManager.getItemDisplayName(itemName))
                    .replace("#current#", String.valueOf(currentAmount))));
            return false;
        }

        if (isTransferInProgress(sender)) {
            sender.sendMessage(ChatUtils.colorize(File.getMessage().getString("cropstorage.transfer.failed_in_progress")));
            return false;
        }

        return true;
    }

    private static boolean canTransferForMulti(Player sender, String receiverName, String itemName, int amount) {
        if (sender == null || receiverName == null || itemName == null) {
            return false;
        }

        FileConfiguration config = File.getCropStorageConfig();

        if (!config.getBoolean("transfer.enabled", true)) {
            return false;
        }

        if (!sender.hasPermission("storage.cropstorage.transfer.multi")) {
            sender.sendMessage(ChatUtils.colorize(File.getMessage().getString("admin.no_permission")));
            return false;
        }

        if (sender.getName().equalsIgnoreCase(receiverName)) {
            sender.sendMessage(ChatUtils.colorize(File.getMessage().getString("cropstorage.transfer.failed_same_player")));
            return false;
        }

        Player receiver = Bukkit.getPlayer(receiverName);
        if (receiver == null || !receiver.isOnline()) {
            sender.sendMessage(ChatUtils.colorize(File.getMessage().getString("cropstorage.transfer.failed_offline").replace("#player#", receiverName)));
            return false;
        }

        if (amount <= 0) {
            sender.sendMessage(ChatUtils.colorize(File.getMessage().getString("admin.number_too_low")));
            return false;
        }

        if (!CropStorageManager.isConfiguredDrop(itemName)) {
            sender.sendMessage(ChatUtils.colorize(File.getMessage().getString("cropstorage.transfer.failed_invalid_item")
                    .replace("#item#", CropStorageManager.getItemDisplayName(itemName))));
            return false;
        }

        int currentAmount = CropStorageManager.getPlayerItem(sender, itemName);
        if (currentAmount < amount) {
            sender.sendMessage(ChatUtils.colorize(File.getMessage().getString("cropstorage.transfer.failed_insufficient")
                    .replace("#item#", CropStorageManager.getItemDisplayName(itemName))
                    .replace("#current#", String.valueOf(currentAmount))));
            return false;
        }

        int maxTransferable = getMaxTransferableAmount(sender, receiver, itemName);
        if (maxTransferable <= 0) {
            sender.sendMessage(ChatUtils.colorize(File.getMessage().getString("cropstorage.transfer.failed_receiver_full")
                    .replace("#player#", receiverName)));
            return false;
        }

        return true;
    }

    public static int getMaxTransferableAmount(Player sender, Player receiver, String itemName) {
        int receiverCurrent = CropStorageManager.getPlayerItem(receiver, itemName);
        int receiverMax = CropStorageManager.getMaxStorage(receiver);
        int receiverSpace = receiverMax - receiverCurrent;

        int senderAmount = CropStorageManager.getPlayerItem(sender, itemName);

        return Math.max(0, Math.min(senderAmount, receiverSpace));
    }

    public static Map<String, Integer> calculateOptimalMultiTransfer(Player sender, Player receiver, Map<String, Integer> items) {
        Map<String, Integer> optimized = new HashMap<>();

        for (Map.Entry<String, Integer> entry : items.entrySet()) {
            String itemName = entry.getKey();
            int requestedAmount = entry.getValue();
            int maxTransferable = getMaxTransferableAmount(sender, receiver, itemName);

            if (maxTransferable > 0) {
                optimized.put(itemName, Math.min(requestedAmount, maxTransferable));
            }
        }

        return optimized;
    }

    public static int getOptimalTransferAmount(Player sender, String receiverName, String itemName, int requestedAmount) {
        Player receiver = Bukkit.getPlayer(receiverName);
        if (receiver == null) {
            return 0;
        }

        int maxTransferable = getMaxTransferableAmount(sender, receiver, itemName);
        return Math.min(requestedAmount, maxTransferable);
    }

    public static boolean executeTransfer(Player sender, String receiverName, String itemName, int amount) {
        return executeTransfer(sender, receiverName, itemName, amount, true);
    }

    public static boolean executeTransfer(Player sender, String receiverName, String itemName, int amount, boolean fireEvent) {
        if (!canTransfer(sender, receiverName, itemName, amount)) {
            return false;
        }

        Player receiver = Bukkit.getPlayer(receiverName);
        if (receiver == null) {
            return false;
        }

        if (fireEvent) {
            CropStorageTransferEvent event = new CropStorageTransferEvent(sender, receiver, itemName, amount);
            Bukkit.getPluginManager().callEvent(event);
            if (event.isCancelled()) {
                return false;
            }
            amount = event.getAmount();
        }

        startTransferProcess(sender, receiver, itemName, amount, fireEvent);
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

        Map<String, Integer> optimizedItems = calculateOptimalMultiTransfer(sender, receiver, items);

        if (optimizedItems.isEmpty()) {
            sender.sendMessage(ChatUtils.colorize(File.getMessage().getString("cropstorage.transfer.failed_receiver_full")
                    .replace("#player#", receiverName)));
            return false;
        }

        for (Map.Entry<String, Integer> entry : optimizedItems.entrySet()) {
            if (!canTransferForMulti(sender, receiverName, entry.getKey(), entry.getValue())) {
                return false;
            }
        }

        if (!optimizedItems.isEmpty()) {
            CropStorageMultiTransferEvent event = new CropStorageMultiTransferEvent(sender, receiver, optimizedItems);
            Bukkit.getPluginManager().callEvent(event);
            if (event.isCancelled()) {
                return false;
            }
        }

        startMultiTransferProcess(sender, receiver, optimizedItems);
        return true;
    }

	public static boolean isTransferInProgress(Player player) {
		if (player == null) {
			return false;
		}
		return activeTransfers.containsKey(player.getName());
	}

    private static void startTransferProcess(Player sender, Player receiver, String itemName, int amount, boolean fireEvent) {
        FileConfiguration config = File.getCropStorageConfig();
        int transferDelay = config.getInt("transfer.delay", 3); // Default 3 seconds

        String displayName = CropStorageManager.getItemDisplayName(itemName);

        // Notify players that transfer is starting
        sender.sendMessage(ChatUtils.colorize(File.getMessage().getString("cropstorage.transfer.processing_send")
                .replace("#amount#", String.valueOf(amount))
                .replace("#item#", displayName)
                .replace("#player#", receiver.getName())
                .replace("#time#", String.valueOf(transferDelay))));

        receiver.sendMessage(ChatUtils.colorize(File.getMessage().getString("cropstorage.transfer.processing_receive")
                .replace("#amount#", String.valueOf(amount))
                .replace("#item#", displayName)
                .replace("#player#", sender.getName())
                .replace("#time#", String.valueOf(transferDelay))));

        // Cancel any existing transfer for this player
        cancelTransfer(sender);

        // Start processing animation
        if (config.getBoolean("transfer.particles.enabled", true)) {
            ParticleManager.playTransferProcessingAnimation(sender, transferDelay, config);
        }

        // Store fireEvent flag for completion
        final boolean shouldFireEvent = fireEvent;

        // Create and start the transfer task
        TaskWrapper transferTask = TaskWrapper.runTaskLater(Storage.getStorage(), () -> {
            // Stop processing animation
            if (config.getBoolean("transfer.particles.enabled", true)) {
                ParticleManager.stopTransferProcessingAnimation(sender);
            }
            completeTransfer(sender, receiver, itemName, amount, shouldFireEvent);
            activeTransfers.remove(sender.getName());
        }, transferDelay * 20L);

        activeTransfers.put(sender.getName(), transferTask);
    }

	private static void startMultiTransferProcess(Player sender, Player receiver, Map<String, Integer> items) {
		FileConfiguration config = File.getCropStorageConfig();
		int transferDelay = config.getInt("transfer.delay", 3);

		StringBuilder itemsList = new StringBuilder();
		int count = 0;
		for (Map.Entry<String, Integer> entry : items.entrySet()) {
			if (count > 0) {
				itemsList.append(", ");
			}
			itemsList.append(entry.getValue()).append(" ")
					.append(CropStorageManager.getItemDisplayName(entry.getKey()));
			count++;
		}

		sender.sendMessage(ChatUtils.colorize(File.getMessage().getString("cropstorage.transfer.processing_multi_send_detailed")
				.replace("#items#", itemsList.toString())
				.replace("#player#", receiver.getName())
				.replace("#time#", String.valueOf(transferDelay))));

		receiver.sendMessage(ChatUtils.colorize(File.getMessage().getString("cropstorage.transfer.processing_multi_receive_detailed")
				.replace("#items#", itemsList.toString())
				.replace("#player#", sender.getName())
				.replace("#time#", String.valueOf(transferDelay))));

		cancelTransfer(sender);

		if (config.getBoolean("transfer.particles.enabled", true)) {
			ParticleManager.playTransferProcessingAnimation(sender, transferDelay, config);
		}

		TaskWrapper transferTask = TaskWrapper.runTaskLater(Storage.getStorage(), () -> {
			if (config.getBoolean("transfer.particles.enabled", true)) {
				ParticleManager.stopTransferProcessingAnimation(sender);
			}
			completeMultiTransfer(sender, receiver, items);
			activeTransfers.remove(sender.getName());
		}, transferDelay * 20L);

		activeTransfers.put(sender.getName(), transferTask);
	}

    private static void completeMultiTransfer(Player sender, Player receiver, Map<String, Integer> items) {
        // Double-check conditions before completing transfer
        if (!sender.isOnline() || !receiver.isOnline()) {
            if (sender.isOnline()) {
                sender.sendMessage(ChatUtils.colorize(File.getMessage().getString("cropstorage.transfer.failed_offline_during")
                        .replace("#player#", receiver.getName())));
            }
            return;
        }

        int successCount = 0;
        int totalCount = items.size();
        StringBuilder successfulTransfers = new StringBuilder();

        for (Map.Entry<String, Integer> entry : items.entrySet()) {
            String itemName = entry.getKey();
            int amount = entry.getValue();

            // Check if sender still has enough of this item
            int currentAmount = CropStorageManager.getPlayerItem(sender, itemName);
            if (currentAmount < amount) {
                String displayName = CropStorageManager.getItemDisplayName(itemName);
                sender.sendMessage(ChatUtils.colorize(File.getMessage().getString("cropstorage.transfer.failed_insufficient_during")
                        .replace("#item#", displayName)));
                continue;
            }

            if (CropStorageManager.removeItemAmount(sender, itemName, amount, false)) {
                if (CropStorageManager.addItemAmount(receiver, itemName, amount, false)) {
                    // Log successful transfer
                    CropTransferData transferData = new CropTransferData(
                            sender.getName(),
                            receiver.getName(),
                            itemName,
                            amount,
                            System.currentTimeMillis(),
                            "SUCCESS_MULTI"
                    );
                    ensureInitialized();
                    if (transferDatabase != null) {
                        transferDatabase.insertTransfer(transferData);
                    }

                    // Add to successful transfers list
                    if (successCount > 0) {
                        successfulTransfers.append(", ");
                    }
                    successfulTransfers.append(amount).append(" ").append(CropStorageManager.getItemDisplayName(itemName));
                    successCount++;
                } else {
                    // Rollback if receiver couldn't receive
                    CropStorageManager.addItemAmount(sender, itemName, amount, false);
                    sender.sendMessage(ChatUtils.colorize(File.getMessage().getString("cropstorage.transfer.failed_receiver_full")
                            .replace("#player#", receiver.getName())));
                }
            }
        }

        // Send completion messages
        if (successCount > 0) {
            String transferList = successfulTransfers.toString();

            sender.sendMessage(ChatUtils.colorize(File.getMessage().getString("cropstorage.transfer.success_multi_send_detailed")
                    .replace("#items#", transferList)
                    .replace("#player#", receiver.getName())));

            receiver.sendMessage(ChatUtils.colorize(File.getMessage().getString("cropstorage.transfer.success_multi_receive_detailed")
                    .replace("#items#", transferList)
                    .replace("#player#", sender.getName())));

            playTransferEffects(sender, receiver);
        } else {
            sender.sendMessage(ChatUtils.colorize(File.getMessage().getString("cropstorage.transfer.failed_multi_all")));
        }
    }

    private static void completeTransfer(Player sender, Player receiver, String itemName, int amount, boolean fireEvent) {
        // Double-check conditions before completing transfer
        if (!sender.isOnline() || !receiver.isOnline()) {
            handleFailedTransfer(sender, receiver.getName(), itemName, amount, "PLAYER_OFFLINE");
            return;
        }

        int currentAmount = CropStorageManager.getPlayerItem(sender, itemName);
        if (currentAmount < amount) {
            handleFailedTransfer(sender, receiver.getName(), itemName, amount, "INSUFFICIENT_RESOURCES");
            return;
        }

        if (CropStorageManager.removeItemAmount(sender, itemName, amount, false)) {
            if (CropStorageManager.addItemAmount(receiver, itemName, amount, false)) {
                handleSuccessfulTransfer(sender, receiver, itemName, amount, fireEvent);
            } else {
                // Rollback if receiver couldn't receive
                CropStorageManager.addItemAmount(sender, itemName, amount, false);
                handleFailedTransfer(sender, receiver.getName(), itemName, amount, "RECEIVER_FULL");
            }
        } else {
            handleFailedTransfer(sender, receiver.getName(), itemName, amount, "REMOVAL_FAILED");
        }
    }

    private static void handleSuccessfulTransfer(Player sender, Player receiver, String itemName, int amount, boolean fireEvent) {
        long timestamp = System.currentTimeMillis();
        CropTransferData transferData = new CropTransferData(sender.getName(), receiver.getName(), itemName, amount, timestamp, "SUCCESS");

        ensureInitialized();
        if (transferDatabase != null) {
            transferDatabase.insertTransfer(transferData);
        }

        String displayName = CropStorageManager.getItemDisplayName(itemName);

        sender.sendMessage(ChatUtils.colorize(File.getMessage().getString("cropstorage.transfer.success_send")
                .replace("#amount#", String.valueOf(amount))
                .replace("#item#", displayName)
                .replace("#player#", receiver.getName())));

        receiver.sendMessage(ChatUtils.colorize(File.getMessage().getString("cropstorage.transfer.success_receive")
                .replace("#amount#", String.valueOf(amount))
                .replace("#item#", displayName)
                .replace("#player#", sender.getName())));

        playTransferEffects(sender, receiver);
    }

    private static void handleFailedTransfer(Player sender, String receiverName, String itemName, int amount, String reason) {
        CropTransferData failedTransfer = new CropTransferData(sender.getName(), receiverName, itemName, amount, System.currentTimeMillis(), "FAILED_" + reason);
        ensureInitialized();
        if (transferDatabase != null) {
            transferDatabase.insertTransfer(failedTransfer);
        }

        String displayName = CropStorageManager.getItemDisplayName(itemName);
        String errorMessage = getErrorMessage(reason, displayName, receiverName);

        if (sender.isOnline()) {
            sender.sendMessage(ChatUtils.colorize(errorMessage));
            if (File.getCropStorageConfig().getBoolean("transfer.particles.enabled", true)) {
                ParticleManager.playTransferFailedParticle(sender, File.getCropStorageConfig());
            }
        }
    }

    private static String getErrorMessage(String reason, String itemName, String receiverName) {
        switch (reason) {
            case "PLAYER_OFFLINE":
                return File.getMessage().getString("cropstorage.transfer.failed_offline_during").replace("#player#", receiverName);
            case "INSUFFICIENT_RESOURCES":
                return File.getMessage().getString("cropstorage.transfer.failed_insufficient_during").replace("#item#", itemName);
            case "RECEIVER_FULL":
                return File.getMessage().getString("cropstorage.transfer.failed_receiver_full").replace("#player#", receiverName);
            default:
                return File.getMessage().getString("cropstorage.transfer.failed_unknown");
        }
    }

    private static void playTransferEffects(Player sender, Player receiver) {
        FileConfiguration config = File.getCropStorageConfig();

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

        if (config.getBoolean("transfer.particles.enabled", true)) {
            // Play beam effect from sender to receiver
            ParticleManager.playTransferBeamEffect(sender, receiver, config);

            // Play enhanced success and receive particles
            ParticleManager.playTransferSuccessParticle(sender, config);
            ParticleManager.playTransferReceiveParticle(receiver, config);
        }
    }

    // ... (rest of the code remains the same)

    public static void cancelTransfer(Player player) {
        TaskWrapper task = activeTransfers.remove(player.getName());
        if (task != null) {
            task.cancel();
            player.sendMessage(ChatUtils.colorize(File.getMessage().getString("cropstorage.transfer.cancelled")));
        }
        // Stop any processing animation
        if (File.getCropStorageConfig().getBoolean("transfer.particles.enabled", true)) {
            ParticleManager.stopTransferProcessingAnimation(player);
        }
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

        if (!player.getName().equalsIgnoreCase(playerToCheck) && !player.hasPermission("storage.cropstorage.transfer.log.others")) {
            player.sendMessage(ChatUtils.colorize(File.getMessage().getString("admin.no_permission")));
            return;
        }

        if (!player.hasPermission("storage.cropstorage.transfer.log")) {
            player.sendMessage(ChatUtils.colorize(File.getMessage().getString("admin.no_permission")));
            return;
        }

        FileConfiguration config = File.getCropStorageConfig();
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

        List<CropTransferData> transfers = transferDatabase.getTransferHistory(playerToCheck, itemsPerPage, offset);

        if (transfers.isEmpty()) {
            player.sendMessage(ChatUtils.colorize(File.getMessage().getString("cropstorage.transfer.log_no_history").replace("#player#", playerToCheck)));
            return;
        }

        // Send header with page info
        String headerMessage = File.getMessage().getString("cropstorage.transfer.log_header_paginated")
                .replace("#player#", playerToCheck)
                .replace("#current_page#", String.valueOf(page))
                .replace("#total_pages#", String.valueOf(totalPages));
        player.sendMessage(ChatUtils.colorize(headerMessage));

        // Add empty line for better spacing
        player.sendMessage("");

        SimpleDateFormat dateFormat = new SimpleDateFormat("MM/dd HH:mm");

        for (CropTransferData transfer : transfers) {
            String timeStr = dateFormat.format(new Date(transfer.getTimestamp()));
            String displayName = CropStorageManager.getItemDisplayName(transfer.getItemName());

            if (transfer.getSender().equalsIgnoreCase(playerToCheck)) {
                String message = File.getMessage().getString("cropstorage.transfer.log_entry_sent")
                        .replace("#time#", timeStr)
                        .replace("#amount#", String.valueOf(transfer.getAmount()))
                        .replace("#item#", displayName)
                        .replace("#receiver#", transfer.getReceiver());
                player.sendMessage(ChatUtils.colorize(message));
            } else {
                String message = File.getMessage().getString("cropstorage.transfer.log_entry_received")
                        .replace("#time#", timeStr)
                        .replace("#amount#", String.valueOf(transfer.getAmount()))
                        .replace("#item#", displayName)
                        .replace("#sender#", transfer.getSender());
                player.sendMessage(ChatUtils.colorize(message));
            }
        }

        // Send pagination footer
        sendPaginationFooter(player, playerToCheck, page, totalPages, totalTransfers);
    }

    private static void sendPaginationFooter(Player player, String targetPlayer, int currentPage, int totalPages, int totalTransfers) {
        FileConfiguration messageConfig = File.getMessage();

        // Add empty line and separator
        player.sendMessage("");
        String separator = messageConfig.getString("cropstorage.transfer.log_nav_separator");
        if (separator != null) {
            player.sendMessage(ChatUtils.colorize(separator));
        }

        // Show total count and page info on same line
        String footerInfo = messageConfig.getString("cropstorage.transfer.log_footer_info")
                .replace("#total#", String.valueOf(totalTransfers))
                .replace("#current#", String.valueOf(currentPage))
                .replace("#total_pages#", String.valueOf(totalPages));
        player.sendMessage(ChatUtils.colorize(footerInfo));

        // If only one page, don't show navigation
        if (totalPages <= 1) {
            return;
        }

        // Send navigation info
        boolean hasPrev = currentPage > 1;
        boolean hasNext = currentPage < totalPages;

        String navTemplate = messageConfig.getString("cropstorage.transfer.log_navigation");
        if (navTemplate != null && !navTemplate.isEmpty()) {
            String navInfo = navTemplate
                    .replace("#prev_page#", hasPrev ? String.valueOf(currentPage - 1) : "-")
                    .replace("#next_page#", hasNext ? String.valueOf(currentPage + 1) : "-");
            player.sendMessage(ChatUtils.colorize(navInfo));
        }
    }

    public static CropTransferDatabase getTransferDatabase() {
        return transferDatabase;
    }
}
