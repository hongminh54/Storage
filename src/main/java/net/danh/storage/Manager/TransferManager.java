package net.danh.storage.Manager;

import net.danh.storage.Data.TransferData;
import net.danh.storage.Database.TransferDatabase;
import net.danh.storage.Storage;
import net.danh.storage.Utils.Chat;
import net.danh.storage.Utils.File;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TransferManager {

    private static final Map<String, BukkitRunnable> activeTransfers = new HashMap<>();
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
            sender.sendMessage(Chat.colorize(File.getMessage().getString("transfer.failed_permission")));
            return false;
        }

        if (sender.getName().equalsIgnoreCase(receiverName)) {
            sender.sendMessage(Chat.colorize(File.getMessage().getString("transfer.failed_same_player")));
            return false;
        }

        Player receiver = Bukkit.getPlayer(receiverName);
        if (receiver == null || !receiver.isOnline()) {
            sender.sendMessage(Chat.colorize(File.getMessage().getString("transfer.failed_offline").replace("#player#", receiverName)));
            return false;
        }

        if (amount <= 0) {
            sender.sendMessage(Chat.colorize(File.getMessage().getString("transfer.failed_invalid_amount")));
            return false;
        }

        if (!MineManager.getPluginBlocks().contains(material)) {
            sender.sendMessage(Chat.colorize(File.getMessage().getString("transfer.failed_invalid_material")));
            return false;
        }

        int currentAmount = MineManager.getPlayerBlock(sender, material);
        if (currentAmount < amount) {
            sender.sendMessage(Chat.colorize(File.getMessage().getString("transfer.failed_insufficient").replace("#material#", getDisplayName(material)).replace("#current#", String.valueOf(currentAmount))));
            return false;
        }

        if (isTransferInProgress(sender)) {
            sender.sendMessage(Chat.colorize(File.getMessage().getString("transfer.failed_in_progress")));
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
            sender.sendMessage(Chat.colorize(File.getMessage().getString("transfer.failed_permission_multi")));
            return false;
        }

        if (sender.getName().equalsIgnoreCase(receiverName)) {
            sender.sendMessage(Chat.colorize(File.getMessage().getString("transfer.failed_same_player")));
            return false;
        }

        Player receiver = Bukkit.getPlayer(receiverName);
        if (receiver == null || !receiver.isOnline()) {
            sender.sendMessage(Chat.colorize(File.getMessage().getString("transfer.failed_offline").replace("#player#", receiverName)));
            return false;
        }

        if (amount <= 0) {
            sender.sendMessage(Chat.colorize(File.getMessage().getString("transfer.failed_invalid_amount")));
            return false;
        }

        if (!MineManager.getPluginBlocks().contains(material)) {
            sender.sendMessage(Chat.colorize(File.getMessage().getString("transfer.failed_invalid_material")));
            return false;
        }

        int currentAmount = MineManager.getPlayerBlock(sender, material);
        if (currentAmount < amount) {
            sender.sendMessage(Chat.colorize(File.getMessage().getString("transfer.failed_insufficient")
                    .replace("#material#", getDisplayName(material))
                    .replace("#current#", String.valueOf(currentAmount))));
            return false;
        }

        return true;
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

        // Validate all transfers first
        for (Map.Entry<String, Integer> entry : materials.entrySet()) {
            if (!canTransferForMulti(sender, receiverName, entry.getKey(), entry.getValue())) {
                return false;
            }
        }

        Player receiver = Bukkit.getPlayer(receiverName);
        if (receiver == null) {
            return false;
        }

        // Start multi transfer process
        startMultiTransferProcess(sender, receiver, materials);
        return true;
    }

    private static void startTransferProcess(Player sender, Player receiver, String material, int amount) {
        FileConfiguration config = File.getConfig();
        int transferDelay = config.getInt("transfer.delay", 3); // Default 3 seconds

        String displayName = getDisplayName(material);

        // Notify players that transfer is starting
        sender.sendMessage(Chat.colorize(File.getMessage().getString("transfer.processing_send").replace("#amount#", String.valueOf(amount)).replace("#material#", displayName).replace("#player#", receiver.getName()).replace("#time#", String.valueOf(transferDelay))));

        receiver.sendMessage(Chat.colorize(File.getMessage().getString("transfer.processing_receive").replace("#amount#", String.valueOf(amount)).replace("#material#", displayName).replace("#player#", sender.getName()).replace("#time#", String.valueOf(transferDelay))));

        // Cancel any existing transfer for this player
        cancelTransfer(sender);

        // Create and start the transfer task
        BukkitRunnable transferTask = new BukkitRunnable() {
            @Override
            public void run() {
                completeTransfer(sender, receiver, material, amount);
                activeTransfers.remove(sender.getName());
            }
        };

        activeTransfers.put(sender.getName(), transferTask);
        transferTask.runTaskLater(Storage.getStorage(), transferDelay * 20L); // Convert seconds to ticks
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
        sender.sendMessage(Chat.colorize(File.getMessage().getString("transfer.processing_multi_send_detailed")
                .replace("#materials#", materialsList.toString())
                .replace("#player#", receiver.getName())
                .replace("#time#", String.valueOf(transferDelay))));

        receiver.sendMessage(Chat.colorize(File.getMessage().getString("transfer.processing_multi_receive_detailed")
                .replace("#materials#", materialsList.toString())
                .replace("#player#", sender.getName())
                .replace("#time#", String.valueOf(transferDelay))));

        // Cancel any existing transfer for this player
        cancelTransfer(sender);

        // Create and start the multi transfer task
        BukkitRunnable transferTask = new BukkitRunnable() {
            @Override
            public void run() {
                completeMultiTransfer(sender, receiver, materials);
                activeTransfers.remove(sender.getName());
            }
        };

        activeTransfers.put(sender.getName(), transferTask);
        transferTask.runTaskLater(Storage.getStorage(), transferDelay * 20L); // Convert seconds to ticks
    }

    private static void completeMultiTransfer(Player sender, Player receiver, Map<String, Integer> materials) {
        // Double-check conditions before completing transfer
        if (!sender.isOnline() || !receiver.isOnline()) {
            if (sender.isOnline()) {
                sender.sendMessage(Chat.colorize(File.getMessage().getString("transfer.failed_offline_during").replace("#player#", receiver.getName())));
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
                sender.sendMessage(Chat.colorize(File.getMessage().getString("transfer.failed_insufficient_during")
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
                    sender.sendMessage(Chat.colorize(File.getMessage().getString("transfer.failed_receiver_full")
                            .replace("#player#", receiver.getName())));
                }
            }
        }

        // Send completion messages
        if (successCount > 0) {
            String transferList = successfulTransfers.toString();

            sender.sendMessage(Chat.colorize(File.getMessage().getString("transfer.success_multi_send_detailed")
                    .replace("#materials#", transferList)
                    .replace("#player#", receiver.getName())));

            receiver.sendMessage(Chat.colorize(File.getMessage().getString("transfer.success_multi_receive_detailed")
                    .replace("#materials#", transferList)
                    .replace("#player#", sender.getName())));

            playTransferEffects(sender, receiver);
        } else {
            sender.sendMessage(Chat.colorize(File.getMessage().getString("transfer.failed_multi_all")));
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

        sender.sendMessage(Chat.colorize(File.getMessage().getString("transfer.success_send").replace("#amount#", String.valueOf(amount)).replace("#material#", displayName).replace("#player#", receiver.getName())));

        receiver.sendMessage(Chat.colorize(File.getMessage().getString("transfer.success_receive").replace("#amount#", String.valueOf(amount)).replace("#material#", displayName).replace("#player#", sender.getName())));

        playTransferEffects(sender, receiver);
    }

    private static void handleFailedTransfer(Player sender, String receiverName, String material, int amount, String reason) {
        TransferData failedTransfer = new TransferData(sender.getName(), receiverName, material, amount, System.currentTimeMillis(), "FAILED_" + reason);
        transferDatabase.insertTransfer(failedTransfer);

        String displayName = getDisplayName(material);
        String errorMessage = getErrorMessage(reason, displayName, receiverName);

        if (sender.isOnline()) {
            sender.sendMessage(Chat.colorize(errorMessage));
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

        ParticleManager.playTransferSuccessParticle(sender);
        ParticleManager.playTransferReceiveParticle(receiver);
    }

    public static boolean isTransferInProgress(Player player) {
        return activeTransfers.containsKey(player.getName());
    }

    public static void cancelTransfer(Player player) {
        BukkitRunnable task = activeTransfers.remove(player.getName());
        if (task != null) {
            task.cancel();
            player.sendMessage(Chat.colorize(File.getMessage().getString("transfer.cancelled")));
        }
    }

    public static void cancelAllTransfers() {
        for (BukkitRunnable task : activeTransfers.values()) {
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
            player.sendMessage(Chat.colorizewp(File.getMessage().getString("transfer.failed_permission_log_others")));
            return;
        }

        if (!player.hasPermission("storage.transfer.log")) {
            player.sendMessage(Chat.colorizewp(File.getMessage().getString("transfer.failed_permission_log")));
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
            player.sendMessage(Chat.colorizewp(File.getMessage().getString("transfer.log_no_history").replace("#player#", playerToCheck)));
            return;
        }

        // Send header with page info
        String headerMessage = File.getMessage().getString("transfer.log_header_paginated", "&6=== Transfer History for #player# (Page #current_page#/#total_pages#) ===")
                .replace("#player#", playerToCheck)
                .replace("#current_page#", String.valueOf(page))
                .replace("#total_pages#", String.valueOf(totalPages));
        player.sendMessage(Chat.colorizewp(headerMessage));

        // Add empty line for better spacing
        player.sendMessage("");

        SimpleDateFormat dateFormat = new SimpleDateFormat("MM/dd HH:mm");

        for (TransferData transfer : transfers) {
            String timeStr = dateFormat.format(new Date(transfer.getTimestamp()));
            String displayName = getDisplayName(transfer.getMaterial());

            if (transfer.getSender().equalsIgnoreCase(playerToCheck)) {
                String message = File.getMessage().getString("transfer.log_entry_sent").replace("#time#", timeStr).replace("#amount#", String.valueOf(transfer.getAmount())).replace("#material#", displayName).replace("#receiver#", transfer.getReceiver());
                player.sendMessage(Chat.colorizewp(message));
            } else {
                String message = File.getMessage().getString("transfer.log_entry_received").replace("#time#", timeStr).replace("#amount#", String.valueOf(transfer.getAmount())).replace("#material#", displayName).replace("#sender#", transfer.getSender());
                player.sendMessage(Chat.colorizewp(message));
            }
        }

        // Send pagination footer with clickable navigation
        sendPaginationFooter(player, playerToCheck, page, totalPages, totalTransfers);
    }

    private static void sendPaginationFooter(Player player, String targetPlayer, int currentPage, int totalPages, int totalTransfers) {
        FileConfiguration messageConfig = File.getMessage();

        // Add empty line and separator
        player.sendMessage("");
        String separator = messageConfig.getString("transfer.log_nav_separator", "&7─────────────────────────────────────────────");
        player.sendMessage(Chat.colorizewp(separator));

        // Show total count and page info on same line
        String footerInfo = messageConfig.getString("transfer.log_footer_info", "&7Total: &e#total# &7| Page &e#current#&7/&e#total_pages#")
                .replace("#total#", String.valueOf(totalTransfers))
                .replace("#current#", String.valueOf(currentPage))
                .replace("#total_pages#", String.valueOf(totalPages));
        player.sendMessage(Chat.colorizewp(footerInfo));

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

        // Try using tellraw command for clickable text
        if (hasPrev && hasNext) {
            // Both buttons active
            String prevCommand = buildNavigationCommand(targetPlayer, player.getName(), currentPage - 1);
            String nextCommand = buildNavigationCommand(targetPlayer, player.getName(), currentPage + 1);
            sendTellrawNavigation(player, prevCommand, nextCommand, true, true, currentPage - 1, currentPage + 1);
        } else if (hasPrev) {
            // Only previous active
            String prevCommand = buildNavigationCommand(targetPlayer, player.getName(), currentPage - 1);
            sendTellrawNavigation(player, prevCommand, "", true, false, currentPage - 1, 0);
        } else if (hasNext) {
            // Only next active
            String nextCommand = buildNavigationCommand(targetPlayer, player.getName(), currentPage + 1);
            sendTellrawNavigation(player, "", nextCommand, false, true, 0, currentPage + 1);
        } else {
            // Both disabled
            sendTellrawNavigation(player, "", "", false, false, 0, 0);
        }
    }

    private static void sendTellrawNavigation(Player player, String prevCommand, String nextCommand, boolean hasPrev, boolean hasNext, int prevPage, int nextPage) {
        try {
            FileConfiguration messageConfig = File.getMessage();

            // Get configurable texts and colors - strip color codes for JSON
            String prevText = stripColorCodes(messageConfig.getString("transfer.log_nav_previous", "◀ Previous"));
            String prevDisabledText = stripColorCodes(messageConfig.getString("transfer.log_nav_previous_disabled", "◀ Previous"));
            String prevHover = stripColorCodes(messageConfig.getString("transfer.log_nav_previous_hover", "Click to go to page #page#")
                    .replace("#page#", String.valueOf(prevPage)));

            String nextText = stripColorCodes(messageConfig.getString("transfer.log_nav_next", "Next ▶"));
            String nextDisabledText = stripColorCodes(messageConfig.getString("transfer.log_nav_next_disabled", "Next ▶"));
            String nextHover = stripColorCodes(messageConfig.getString("transfer.log_nav_next_hover", "Click to go to page #page#")
                    .replace("#page#", String.valueOf(nextPage)));

            String spacing = stripColorCodes(messageConfig.getString("transfer.log_nav_spacing", "     "));

            // Convert colors to JSON format (support both named colors and hex)
            String activeColor = convertToJsonColor(messageConfig.getString("transfer.log_nav_colors.active", "green"));
            String disabledColor = convertToJsonColor(messageConfig.getString("transfer.log_nav_colors.disabled", "dark_gray"));
            String spacingColor = convertToJsonColor(messageConfig.getString("transfer.log_nav_colors.spacing", "gray"));

            // Build JSON for tellraw command
            StringBuilder json = new StringBuilder();
            json.append("[\"\"");

            // Previous button
            if (hasPrev) {
                json.append(",{\"text\":\"").append(escapeJson(prevText)).append("\",\"color\":\"").append(activeColor)
                        .append("\",\"clickEvent\":{\"action\":\"run_command\",\"value\":\"").append(escapeJson(prevCommand))
                        .append("\"},\"hoverEvent\":{\"action\":\"show_text\",\"value\":\"").append(escapeJson(prevHover)).append("\"}}");
            } else {
                json.append(",{\"text\":\"").append(escapeJson(prevDisabledText)).append("\",\"color\":\"").append(disabledColor).append("\"}");
            }

            // Spacing
            json.append(",{\"text\":\"").append(escapeJson(spacing)).append("\",\"color\":\"").append(spacingColor).append("\"}");

            // Next button
            if (hasNext) {
                json.append(",{\"text\":\"").append(escapeJson(nextText)).append("\",\"color\":\"").append(activeColor)
                        .append("\",\"clickEvent\":{\"action\":\"run_command\",\"value\":\"").append(escapeJson(nextCommand))
                        .append("\"},\"hoverEvent\":{\"action\":\"show_text\",\"value\":\"").append(escapeJson(nextHover)).append("\"}}");
            } else {
                json.append(",{\"text\":\"").append(escapeJson(nextDisabledText)).append("\",\"color\":\"").append(disabledColor).append("\"}");
            }

            json.append("]");

            // Execute tellraw command
            String tellrawCommand = "tellraw " + player.getName() + " " + json;
            player.getServer().dispatchCommand(player.getServer().getConsoleSender(), tellrawCommand);

        } catch (Exception e) {
            // Fallback to simple text using config
            useFallbackNavigation(player, prevCommand, nextCommand, hasPrev, hasNext);
        }
    }

    private static void useFallbackNavigation(Player player, String prevCommand, String nextCommand, boolean hasPrev, boolean hasNext) {
        FileConfiguration messageConfig = File.getMessage();

        // Get colors from config and convert to Minecraft format
        String activeColorCode = convertToMinecraftColor(messageConfig.getString("transfer.log_nav_colors.active", "green"));
        String disabledColorCode = convertToMinecraftColor(messageConfig.getString("transfer.log_nav_colors.disabled", "dark_gray"));
        String spacingColorCode = convertToMinecraftColor(messageConfig.getString("transfer.log_nav_colors.spacing", "gray"));

        // Get clean text without color codes
        String prevText = messageConfig.getString("transfer.log_nav_previous", "◀ Previous");
        String prevDisabledText = messageConfig.getString("transfer.log_nav_previous_disabled", "◀ Previous");
        String nextText = messageConfig.getString("transfer.log_nav_next", "Next ▶");
        String nextDisabledText = messageConfig.getString("transfer.log_nav_next_disabled", "Next ▶");
        String spacing = messageConfig.getString("transfer.log_nav_spacing", "     ");

        StringBuilder fallback = new StringBuilder();

        if (hasPrev) {
            fallback.append(activeColorCode).append(prevText);
        } else {
            fallback.append(disabledColorCode).append(prevDisabledText);
        }

        fallback.append(spacingColorCode).append(spacing);

        if (hasNext) {
            fallback.append(activeColorCode).append(nextText);
        } else {
            fallback.append(disabledColorCode).append(nextDisabledText);
        }

        player.sendMessage(Chat.colorizewp(fallback.toString()));

        // Show help message and manual commands since clickable navigation failed
        String helpMessage = messageConfig.getString("transfer.log_nav_help", "&7Use &e/storage transfer log [player] [page]&7 to navigate");
        player.sendMessage(Chat.colorizewp(helpMessage));

        // Show available commands for manual navigation
        if (hasPrev) {
            player.sendMessage(Chat.colorizewp("&7Previous: &e" + prevCommand));
        }
        if (hasNext) {
            player.sendMessage(Chat.colorizewp("&7Next: &e" + nextCommand));
        }
    }

    private static String convertToJsonColor(String color) {
        if (color == null) return "white";

        // If it's already a hex color in JSON format (#RRGGBB), return as is
        if (color.matches("#[0-9a-fA-F]{6}")) {
            return color;
        }

        // Convert Minecraft hex format to JSON format
        if (color.matches("&#[0-9a-fA-F]{6}")) {
            return color.substring(1); // Remove & to get #RRGGBB
        }

        if (color.matches("<#[0-9a-fA-F]{6}>")) {
            return color.substring(1, color.length() - 1); // Remove < > to get #RRGGBB
        }

        // Convert legacy color codes to named colors
        switch (color.toLowerCase()) {
            case "&0":
            case "black":
                return "black";
            case "&1":
            case "dark_blue":
                return "dark_blue";
            case "&2":
            case "dark_green":
                return "dark_green";
            case "&3":
            case "dark_aqua":
                return "dark_aqua";
            case "&4":
            case "dark_red":
                return "dark_red";
            case "&5":
            case "dark_purple":
                return "dark_purple";
            case "&6":
            case "gold":
                return "gold";
            case "&7":
            case "gray":
                return "gray";
            case "&8":
            case "dark_gray":
                return "dark_gray";
            case "&9":
            case "blue":
                return "blue";
            case "&a":
            case "green":
                return "green";
            case "&b":
            case "aqua":
                return "aqua";
            case "&c":
            case "red":
                return "red";
            case "&d":
            case "light_purple":
                return "light_purple";
            case "&e":
            case "yellow":
                return "yellow";
            case "&f":
            case "white":
                return "white";
            default:
                return color; // Return as is if it's already a valid JSON color name
        }
    }

    private static String convertToMinecraftColor(String color) {
        if (color == null) return "&f";

        // If it's already a Minecraft hex format (&#RRGGBB), return as is
        if (color.matches("&#[0-9a-fA-F]{6}")) {
            return color;
        }

        // Convert other hex formats to Minecraft format
        if (color.matches("#[0-9a-fA-F]{6}")) {
            return "&" + color; // Add & to get &#RRGGBB
        }

        if (color.matches("<#[0-9a-fA-F]{6}>")) {
            return "&" + color.substring(1, color.length() - 1); // Convert <#RRGGBB> to &#RRGGBB
        }

        // Convert named colors to legacy color codes
        switch (color.toLowerCase()) {
            case "black":
                return "&0";
            case "dark_blue":
                return "&1";
            case "dark_green":
                return "&2";
            case "dark_aqua":
                return "&3";
            case "dark_red":
                return "&4";
            case "dark_purple":
                return "&5";
            case "gold":
                return "&6";
            case "gray":
                return "&7";
            case "dark_gray":
                return "&8";
            case "blue":
                return "&9";
            case "green":
                return "&a";
            case "aqua":
                return "&b";
            case "red":
                return "&c";
            case "light_purple":
                return "&d";
            case "yellow":
                return "&e";
            case "white":
                return "&f";
            default:
                // If it's already a legacy color code, return as is
                if (color.matches("&[0-9a-fA-F]")) {
                    return color;
                }
                return "&f"; // Default to white
        }
    }

    private static String stripColorCodes(String text) {
        if (text == null) return "";

        // Remove legacy color codes (&x)
        String result = text.replaceAll("&[0-9a-fA-F]", "");

        // Remove hex color codes (&#xxxxxx and <#xxxxxx>)
        result = result.replaceAll("&#[0-9a-fA-F]{6}", "");
        result = result.replaceAll("<#[0-9a-fA-F]{6}>", "");

        // Remove formatting codes (&l, &o, &n, &m, &k, &r)
        result = result.replaceAll("&[lLnNmMoOkKrR]", "");

        return result;
    }

    private static String escapeJson(String text) {
        if (text == null) return "";
        return text.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    private static String buildNavigationCommand(String targetPlayer, String currentPlayerName, int page) {
        if (targetPlayer == null || targetPlayer.equals(currentPlayerName)) {
            // Own log: /storage transfer log <page>
            return "/storage transfer log " + page;
        } else {
            // Other player's log: /storage transfer log <player> <page>
            return "/storage transfer log " + targetPlayer + " " + page;
        }
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
