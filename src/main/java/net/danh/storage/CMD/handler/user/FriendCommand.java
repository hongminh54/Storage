package net.danh.storage.CMD.handler.user;

import net.danh.storage.CMD.handler.BaseCommand;
import net.danh.storage.Database.FriendDatabase;
import net.danh.storage.Manager.Crop.CropStorageManager;
import net.danh.storage.Manager.Friend.FriendManager;
import net.danh.storage.Manager.MineManager;
import net.danh.storage.Manager.Mythic.MythicStorageManager;
import net.danh.storage.MythicMobs.MythicMobsHelper;
import net.danh.storage.Storage;
import net.danh.storage.Utils.ChatUtils;
import net.danh.storage.Utils.File;
import net.danh.storage.Utils.MaterialUtils;
import net.danh.storage.Utils.SchedulerUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.util.StringUtil;

import java.util.*;

public class FriendCommand extends BaseCommand {

    private static final String[] SUBCOMMANDS = {"add", "accept", "deny", "remove", "list", "withdraw", "deposit", "history", "settings", "help"};
    private static final String[] STORAGE_TYPES = FriendManager.getStorageTypes();

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!requirePlayer(sender)) return;

        Player player = (Player) sender;

        if (!FriendManager.isSystemEnabled()) {
            sendMessage(sender, "friends.system_disabled");
            return;
        }

        if (args.length == 0) {
            sendMessageList(sender, "friends.help");
            return;
        }

        String sub = args[0].toLowerCase();
        switch (sub) {
            case "add":
                handleAdd(player, args);
                break;
            case "accept":
                handleAccept(player, args);
                break;
            case "deny":
                handleDeny(player, args);
                break;
            case "remove":
                handleRemove(player, args);
                break;
            case "list":
                handleList(player);
                break;
            case "withdraw":
                handleWithdraw(player, args);
                break;
            case "deposit":
                handleDeposit(player, args);
                break;
            case "history":
                handleHistory(player, args);
                break;
            case "settings":
                handleSettings(player, args);
                break;
            case "help":
            default:
                sendMessageList(sender, "friends.help");
                break;
        }
    }

    private void handleAdd(Player player, String[] args) {
        if (!player.hasPermission("storage.friends.add")) {
            sendMessage(player, "admin.no_permission");
            return;
        }

        if (args.length < 2) {
            sendMessage(player, "admin.invalid_usage", "#usage#", "/storage friends add <player>");
            return;
        }

        String targetName = args[1];
        Player target = Bukkit.getPlayer(targetName);
        OfflinePlayer offlineTarget = null;
        UUID targetUuid;

        if (target != null) {
            targetUuid = target.getUniqueId();
        } else {
            offlineTarget = Bukkit.getOfflinePlayer(targetName);
            if (!offlineTarget.hasPlayedBefore()) {
                sendMessage(player, "friends.player_not_found", "#player#", targetName);
                return;
            }
            targetUuid = offlineTarget.getUniqueId();
        }

        if (player.getUniqueId().equals(targetUuid)) {
            sendMessage(player, "friends.cannot_add_self");
            return;
        }

        String displayTargetName = target != null ? target.getName()
                : (offlineTarget != null && offlineTarget.getName() != null ? offlineTarget.getName() : targetName);

        if (FriendManager.isFriend(player.getUniqueId(), targetUuid)) {
            sendMessage(player, "friends.already_friends", "#player#", displayTargetName);
            return;
        }

        if (FriendManager.hasPendingRequest(player.getUniqueId(), targetUuid)) {
            sendMessage(player, "friends.already_sent_request", "#player#", displayTargetName);
            return;
        }

        // Check if target sent us a request — auto-accept
        if (FriendManager.hasPendingRequest(targetUuid, player.getUniqueId())) {
            if (FriendManager.acceptRequest(player, targetUuid)) {
                sendMessage(player, "friends.request_accepted", "#player#", displayTargetName);
                if (target != null) {
                    sendMessage(target, "friends.request_accepted_notify", "#player#", player.getName());
                }
            }
            return;
        }

        int maxFriends = FriendManager.getMaxFriends(player);
        int currentCount = FriendManager.getFriendCount(player.getUniqueId());
        if (currentCount >= maxFriends) {
            sendMessage(player, "friends.max_friends_reached",
                    new String[]{"#current#", "#max#"},
                    new String[]{String.valueOf(currentCount), String.valueOf(maxFriends)});
            return;
        }

        int targetMax = target != null ? FriendManager.getMaxFriends(target)
                : File.getFriendStorageConfig().getInt("settings.max_friends_default", 10);
        int targetCount = FriendManager.getFriendCount(targetUuid);
        if (targetCount >= targetMax) {
            sendMessage(player, "friends.target_max_friends", "#player#", displayTargetName);
            return;
        }

        if (FriendManager.sendRequest(player, targetUuid)) {
            sendMessage(player, "friends.request_sent", "#player#", displayTargetName);
            if (target != null) {
                sendMessage(target, "friends.request_received", "#player#", player.getName());
            }
        }
    }

    private void handleAccept(Player player, String[] args) {
        if (!player.hasPermission("storage.friends.accept")) {
            sendMessage(player, "admin.no_permission");
            return;
        }

        if (args.length < 2) {
            sendMessage(player, "admin.invalid_usage", "#usage#", "/storage friends accept <player>");
            return;
        }

        String senderName = args[1];
        Player senderPlayer = Bukkit.getPlayer(senderName);
        UUID senderUuid = null;

        if (senderPlayer != null) {
            senderUuid = senderPlayer.getUniqueId();
        } else {
            OfflinePlayer offlineSender = Bukkit.getOfflinePlayer(senderName);
            if (offlineSender.hasPlayedBefore()) {
                senderUuid = offlineSender.getUniqueId();
            }
        }

        if (senderUuid == null) {
            sendMessage(player, "friends.player_not_found", "#player#", senderName);
            return;
        }

        if (!FriendManager.hasPendingRequest(senderUuid, player.getUniqueId())) {
            sendMessage(player, "friends.no_pending_request", "#player#", senderName);
            return;
        }

        if (FriendManager.acceptRequest(player, senderUuid)) {
            sendMessage(player, "friends.request_accepted", "#player#", senderName);
            if (senderPlayer != null && senderPlayer.isOnline()) {
                sendMessage(senderPlayer, "friends.request_accepted_notify", "#player#", player.getName());
            }
        }
    }

    private void handleDeny(Player player, String[] args) {
        if (!player.hasPermission("storage.friends.deny")) {
            sendMessage(player, "admin.no_permission");
            return;
        }

        if (args.length < 2) {
            sendMessage(player, "admin.invalid_usage", "#usage#", "/storage friends deny <player>");
            return;
        }

        String senderName = args[1];
        Player senderPlayer = Bukkit.getPlayer(senderName);
        UUID senderUuid = null;

        if (senderPlayer != null) {
            senderUuid = senderPlayer.getUniqueId();
        } else {
            OfflinePlayer offlineSender = Bukkit.getOfflinePlayer(senderName);
            if (offlineSender.hasPlayedBefore()) {
                senderUuid = offlineSender.getUniqueId();
            }
        }

        if (senderUuid == null) {
            sendMessage(player, "friends.player_not_found", "#player#", senderName);
            return;
        }

        if (!FriendManager.hasPendingRequest(senderUuid, player.getUniqueId())) {
            sendMessage(player, "friends.no_pending_request", "#player#", senderName);
            return;
        }

        if (FriendManager.denyRequest(player, senderUuid)) {
            sendMessage(player, "friends.request_denied", "#player#", senderName);
            if (senderPlayer != null && senderPlayer.isOnline()) {
                sendMessage(senderPlayer, "friends.request_denied_notify", "#player#", player.getName());
            }
        }
    }

    private void handleRemove(Player player, String[] args) {
        if (!player.hasPermission("storage.friends.remove")) {
            sendMessage(player, "admin.no_permission");
            return;
        }

        if (args.length < 2) {
            sendMessage(player, "admin.invalid_usage", "#usage#", "/storage friends remove <player>");
            return;
        }

        String targetName = args[1];
        UUID friendUuid = resolveUuid(targetName);

        if (friendUuid == null) {
            sendMessage(player, "friends.player_not_found", "#player#", targetName);
            return;
        }

        if (!FriendManager.isFriend(player.getUniqueId(), friendUuid)) {
            sendMessage(player, "friends.not_friends", "#player#", targetName);
            return;
        }

        if (FriendManager.removeFriend(player, friendUuid)) {
            sendMessage(player, "friends.friend_removed", "#player#", targetName);
            Player friendPlayer = Bukkit.getPlayer(friendUuid);
            if (friendPlayer != null && friendPlayer.isOnline()) {
                sendMessage(friendPlayer, "friends.friend_removed_notify", "#player#", player.getName());
            }
        }
    }

    private void handleList(Player player) {
        if (!player.hasPermission("storage.friends.list")) {
            sendMessage(player, "admin.no_permission");
            return;
        }

        Set<UUID> friends = FriendManager.getFriends(player.getUniqueId());
        int maxFriends = FriendManager.getMaxFriends(player);

        sendMessage(player, "friends.list_header",
                new String[]{"#count#", "#max#"},
                new String[]{String.valueOf(friends.size()), String.valueOf(maxFriends)});

        if (friends.isEmpty()) {
            sendMessage(player, "friends.list_empty");
        } else {
            for (UUID friendUuid : friends) {
                String name = resolvePlayerName(friendUuid);
                sendMessage(player, "friends.list_entry", "#player#", name);
            }
        }

        // Show pending requests
        Map<UUID, Long> pending = FriendManager.getPendingRequests(player.getUniqueId());
        if (!pending.isEmpty()) {
            sendMessage(player, "friends.pending_header");
            for (Map.Entry<UUID, Long> entry : pending.entrySet()) {
                String name = resolvePlayerName(entry.getKey());
                String timeRemaining = formatTimeRemaining(entry.getValue() - System.currentTimeMillis());
                sendMessage(player, "friends.pending_entry",
                        new String[]{"#player#", "#time#"},
                        new String[]{name, timeRemaining});
            }
        }
    }

    private void handleWithdraw(Player player, String[] args) {
        if (!player.hasPermission("storage.friends.withdraw")) {
            sendMessage(player, "admin.no_permission");
            return;
        }

        if (args.length < 4) {
            sendMessage(player, "admin.invalid_usage", "#usage#",
                    "/storage friends withdraw <friend> <storage|mythicstorage|cropstorage> <item> [amount|all]");
            return;
        }

        String friendName = args[1];
        String storageType = args[2].toLowerCase();
        String item = args[3];
        String amountStr = args.length >= 5 ? args[4] : "all";

        if (!isValidStorageType(storageType)) {
            sendMessage(player, "friends.invalid_storage_type", "#type#", storageType);
            return;
        }

        UUID ownerUuid = resolveUuid(friendName);
        if (ownerUuid == null) {
            sendMessage(player, "friends.player_not_found", "#player#", friendName);
            return;
        }
        String ownerName = resolvePlayerName(ownerUuid);

        if (!FriendManager.isFriend(player.getUniqueId(), ownerUuid)) {
            sendMessage(player, "friends.not_friends", "#player#", ownerName);
            return;
        }

        // Check if owner allows this storage type
        if (!FriendManager.isStorageAccessAllowed(ownerUuid, storageType)) {
            sendMessage(player, "friends.storage_access_denied",
                    new String[]{"#player#", "#type#"},
                    new String[]{ownerName, storageType});
            return;
        }

        // Check specific permission for this action type
        if (!FriendManager.isWithdrawAllowed(ownerUuid, storageType)) {
            sendMessage(player, "friends.withdraw_not_allowed",
                    new String[]{"#player#", "#type#"},
                    new String[]{ownerName, storageType});
            return;
        }

        // Load owner data if offline
        Player owner = Bukkit.getPlayer(ownerUuid);
        if (owner == null) {
            if (!File.getFriendStorageConfig().getBoolean("settings.allow_offline_access", false)) {
                sendMessage(player, "friends.offline_access_denied", "#player#", ownerName);
                return;
            }
            loadOfflineData(ownerName, storageType);
        }

        int amount = parseWithdrawAmount(amountStr, ownerName, item, storageType);
        if (amount <= 0) {
            if (!amountStr.equalsIgnoreCase("all")) {
                sendMessage(player, "admin.invalid_number", "#number#", amountStr);
            }
            return;
        }

        boolean success = executeWithdraw(player, ownerUuid, ownerName, storageType, item, amount);

        if (success) {
            // Log action
            FriendManager.logAction(ownerUuid, player.getUniqueId(), "WITHDRAW",
                    storageType + ":" + item + " x" + amount);
            // Notify owner
            FriendManager.notifyOwnerAction(ownerUuid, player.getUniqueId(), "withdraw", storageType, item, amount);
        }
    }

    // ==================== Deposit Command ====================

    private void handleDeposit(Player player, String[] args) {
        if (!player.hasPermission("storage.friends.deposit")) {
            sendMessage(player, "admin.no_permission");
            return;
        }

        if (args.length < 4) {
            sendMessage(player, "admin.invalid_usage", "#usage#",
                    "/storage friends deposit <friend> <storage|mythicstorage|cropstorage> <item> [amount|all] [hand]");
            return;
        }

        String friendName = args[1];
        String storageType = args[2].toLowerCase();
        String item = args[3];

        if (!isValidStorageType(storageType)) {
            sendMessage(player, "friends.invalid_storage_type", "#type#", storageType);
            return;
        }

        // Parse optional args: [amount|all] [hand]  (order-insensitive)
        String amountStr = "all";
        boolean fromHand = false;
        for (int i = 4; i < args.length; i++) {
            if (args[i].equalsIgnoreCase("hand")) {
                fromHand = true;
            } else {
                amountStr = args[i];
            }
        }

        UUID ownerUuid = resolveUuid(friendName);
        if (ownerUuid == null) {
            sendMessage(player, "friends.player_not_found", "#player#", friendName);
            return;
        }
        String ownerName = resolvePlayerName(ownerUuid);

        if (!FriendManager.isFriend(player.getUniqueId(), ownerUuid)) {
            sendMessage(player, "friends.not_friends", "#player#", ownerName);
            return;
        }

        // Check if owner allows this storage type
        if (!FriendManager.isStorageAccessAllowed(ownerUuid, storageType)) {
            sendMessage(player, "friends.storage_access_denied",
                    new String[]{"#player#", "#type#"},
                    new String[]{ownerName, storageType});
            return;
        }

        // Check specific permission for this action type
        if (!FriendManager.isDepositAllowed(ownerUuid, storageType)) {
            sendMessage(player, "friends.deposit_not_allowed",
                    new String[]{"#player#", "#type#"},
                    new String[]{ownerName, storageType});
            return;
        }

        // Load owner data if offline
        Player owner = Bukkit.getPlayer(ownerUuid);
        if (owner == null) {
            if (!File.getFriendStorageConfig().getBoolean("settings.allow_offline_access", false)) {
                sendMessage(player, "friends.offline_access_denied", "#player#", ownerName);
                return;
            }
            loadOfflineData(ownerName, storageType);
        }

        // Also need player's own data loaded if depositing from their storage
        if (!fromHand) {
            loadOfflineData(player.getName(), storageType);
        }

        // Resolve amount
        int amount = parseAmountForDeposit(amountStr, player, item, storageType, fromHand);
        if (amount <= 0) {
            if (!amountStr.equalsIgnoreCase("all")) {
                sendMessage(player, "admin.invalid_number", "#number#", amountStr);
            } else {
                // "all" returned 0 → nothing to deposit
                sendMessage(player, "friends.deposit_nothing",
                        new String[]{"#material#", "#source#"},
                        new String[]{item, fromHand ? "inventory" : "storage"});
            }
            return;
        }

        boolean success = fromHand
                ? executeDeposit(player, ownerUuid, ownerName, storageType, item, amount)
                : executeDepositFromStorage(player, ownerUuid, ownerName, storageType, item, amount);

        if (success) {
            FriendManager.logAction(ownerUuid, player.getUniqueId(), "DEPOSIT",
                    storageType + ":" + item + " x" + amount + (fromHand ? " [hand]" : " [storage]"));
            FriendManager.notifyOwnerAction(ownerUuid, player.getUniqueId(), "deposit", storageType, item, amount);
        }
    }

    private void handleHistory(Player player, String[] args) {
        if (!player.hasPermission("storage.friends.history")) {
            sendMessage(player, "admin.no_permission");
            return;
        }

        // /storage friends history [page] [mine]
        // Default mode: show actions others performed on YOUR storage
        // "mine" mode:  show actions YOU performed on others' storage
        int page = 1;
        boolean mineMode = false;

        for (int i = 1; i < args.length; i++) {
            if (args[i].equalsIgnoreCase("mine")) {
                mineMode = true;
            } else {
                try {
                    page = Integer.parseInt(args[i]);
                    if (page < 1) page = 1;
                } catch (NumberFormatException e) {
                    sendMessage(player, "admin.invalid_number", "#number#", args[i]);
                    return;
                }
            }
        }

        FriendDatabase db = FriendManager.getDatabase();
        if (db == null) {
            sendMessage(player, "friends.history_error");
            return;
        }

        final int requestedPage = page;
        final boolean finalMineMode = mineMode;
        final UUID playerUuid = player.getUniqueId();
        final int pageSize = 10;

        SchedulerUtil.runTaskAsynchronously(Storage.getStorage(), () -> {
            int totalLogs = finalMineMode
                    ? db.getActorActionLogCount(playerUuid)
                    : db.getPlayerActionLogCount(playerUuid);
            int totalPages = (int) Math.ceil((double) totalLogs / pageSize);
            if (totalPages == 0) totalPages = 1;
            int queryPage = Math.min(requestedPage, totalPages);
            List<FriendDatabase.FriendActionLog> logs = finalMineMode
                    ? db.getActorActionLogs(playerUuid, queryPage, pageSize)
                    : db.getPlayerActionLogs(playerUuid, queryPage, pageSize);
            final int displayPage = queryPage;
            final int displayTotalPages = totalPages;

            SchedulerUtil.runTask(Storage.getStorage(), player, () -> {
                if (!player.isOnline()) {
                    return;
                }
                showHistory(player, logs, displayPage, displayTotalPages, finalMineMode);
            });
        });
    }

    private void showHistory(Player player, List<FriendDatabase.FriendActionLog> logs,
                             int page, int totalPages, boolean mineMode) {
        if (mineMode) {
            sendMessage(player, "friends.history_header_mine",
                    new String[]{"#page#", "#total#"},
                    new String[]{String.valueOf(page), String.valueOf(totalPages)});
        } else {
            sendMessage(player, "friends.history_header",
                    new String[]{"#page#", "#total#"},
                    new String[]{String.valueOf(page), String.valueOf(totalPages)});
        }

        if (logs.isEmpty()) {
            sendMessage(player, "friends.history_empty");
        } else {
            for (FriendDatabase.FriendActionLog log : logs) {
                String timeStr = formatTimestamp(log.timestamp);
                if (mineMode) {
                    // Show owner name (whose storage was accessed)
                    String ownerName = resolvePlayerName(log.ownerUuid);
                    sendMessage(player, "friends.history_entry_mine",
                            new String[]{"#time#", "#player#", "#action#", "#detail#"},
                            new String[]{timeStr, ownerName, log.action, log.detail});
                } else {
                    // Show actor name (who accessed my storage)
                    String actorName = resolvePlayerName(log.actorUuid);
                    sendMessage(player, "friends.history_entry",
                            new String[]{"#time#", "#player#", "#action#", "#detail#"},
                            new String[]{timeStr, actorName, log.action, log.detail});
                }
            }
        }

        // Navigation hint
        String modeFlag = mineMode ? " mine" : "";
        if (totalPages > 1) {
            StringBuilder nav = new StringBuilder();
            if (page > 1) {
                nav.append(File.getMessage().getString("friends.history_nav_prev", "&a[Prev]")
                        .replace("#page#", String.valueOf(page - 1))
                        .replace("#mode#", modeFlag.trim()));
            }
            nav.append(" &7| ");
            if (page < totalPages) {
                nav.append(File.getMessage().getString("friends.history_nav_next", "&a[Next]")
                        .replace("#page#", String.valueOf(page + 1))
                        .replace("#mode#", modeFlag.trim()));
            }
            player.sendMessage(ChatUtils.colorize(nav.toString()));
        }

        // Hint about switching mode
        if (!mineMode) {
            sendMessage(player, "friends.history_hint_mine");
        } else {
            sendMessage(player, "friends.history_hint_incoming");
        }
    }

    private void handleSettings(Player player, String[] args) {
        if (!player.hasPermission("storage.friends.settings")) {
            sendMessage(player, "admin.no_permission");
            return;
        }

        if (args.length < 2) {
            showSettings(player);
            return;
        }

        String action = args[1].toLowerCase();

        if (action.equals("help")) {
            sendMessageList(player, "friends.settings_help");
            return;
        }

        if (args.length < 3) {
            sendMessage(player, "admin.invalid_usage", "#usage#",
                    "/storage friends settings <storage|mythicstorage|cropstorage|all> <allow|deny> [deposit|withdraw|access|all]");
            return;
        }

        String setting = args[1].toLowerCase();
        String value = args[2].toLowerCase();
        String permissionType = args.length >= 4 ? args[3].toLowerCase() : "all";

        if (!value.equals("allow") && !value.equals("deny")) {
            sendMessage(player, "friends.invalid_setting_value", "#value#", value);
            return;
        }

        if (!permissionType.equals("deposit") && !permissionType.equals("withdraw") &&
                !permissionType.equals("access") && !permissionType.equals("both") && !permissionType.equals("all")) {
            sendMessage(player, "friends.invalid_permission_type", "#type#", permissionType);
            return;
        }

        boolean allowed = value.equals("allow");
        boolean setDeposit = permissionType.equals("deposit") || permissionType.equals("both") || permissionType.equals("all");
        boolean setWithdraw = permissionType.equals("withdraw") || permissionType.equals("both") || permissionType.equals("all");
        boolean setAccess = permissionType.equals("access") || permissionType.equals("all");

        if (!setting.equals("all") && !isValidStorageType(setting)) {
            sendMessage(player, "friends.invalid_storage_type", "#type#", setting);
            return;
        }

        applySettingsAsync(player, setting, allowed, setDeposit, setWithdraw, setAccess, permissionType);
    }

    private void applySettingsAsync(Player player, String setting, boolean allowed,
                                    boolean setDeposit, boolean setWithdraw, boolean setAccess,
                                    String permissionType) {
        UUID playerUuid = player.getUniqueId();
        SchedulerUtil.runTaskAsynchronously(Storage.getStorage(), () -> {
            if (setting.equals("all")) {
                for (String storageType : STORAGE_TYPES) {
                    applySetting(playerUuid, storageType, allowed, setDeposit, setWithdraw, setAccess);
                }
            } else {
                applySetting(playerUuid, setting, allowed, setDeposit, setWithdraw, setAccess);
            }

            SchedulerUtil.runTask(Storage.getStorage(), player, () -> {
                if (!player.isOnline()) {
                    return;
                }
                String status = allowed
                        ? File.getMessage().getString("user.status.status_on", "&aOn")
                        : File.getMessage().getString("user.status.status_off", "&cOff");
                if (setting.equals("all")) {
                    sendMessage(player, "friends.settings_all_updated",
                            new String[]{"#type#", "#status#"},
                            new String[]{permissionType, status});
                } else {
                    sendMessage(player, "friends.settings_updated",
                            new String[]{"#storage#", "#type#", "#status#"},
                            new String[]{setting, permissionType, status});
                }
            });
        });
    }

    private void applySetting(UUID playerUuid, String storageType, boolean allowed,
                              boolean setDeposit, boolean setWithdraw, boolean setAccess) {
        if (setDeposit) {
            FriendManager.setDepositAllowed(playerUuid, storageType, allowed);
        }
        if (setWithdraw) {
            FriendManager.setWithdrawAllowed(playerUuid, storageType, allowed);
        }
        if (setAccess) {
            FriendManager.setStorageAccess(playerUuid, storageType, allowed);
        }
    }

    private void showSettings(Player player) {
        sendMessage(player, "friends.settings_header");

        for (String storageType : STORAGE_TYPES) {
            boolean accessAllowed = FriendManager.isStorageAccessAllowed(player.getUniqueId(), storageType);
            boolean depositAllowed = FriendManager.isDepositAllowed(player.getUniqueId(), storageType);
            boolean withdrawAllowed = FriendManager.isWithdrawAllowed(player.getUniqueId(), storageType);

            String accessStatus = accessAllowed ?
                    File.getMessage().getString("friends.icon.yes", "&a✔") :
                    File.getMessage().getString("friends.icon.no", "&c✘");
            String depositStatus = depositAllowed ?
                    File.getMessage().getString("friends.icon.yes", "&a✔") :
                    File.getMessage().getString("friends.icon.no", "&c✘");
            String withdrawStatus = withdrawAllowed ?
                    File.getMessage().getString("friends.icon.yes", "&a✔") :
                    File.getMessage().getString("friends.icon.no", "&c✘");

            sendMessage(player, "friends.settings_entry_detailed",
                    new String[]{"#type#", "#access#", "#deposit#", "#withdraw#"},
                    new String[]{storageType, accessStatus, depositStatus, withdrawStatus});
        }
    }

    private boolean executeWithdraw(Player player, UUID ownerUuid, String ownerName, String storageType, String item, int amount) {
        switch (storageType) {
            case "storage":
                return executeStorageWithdraw(player, ownerUuid, ownerName, item, amount);
            case "mythicstorage":
                return executeMythicWithdraw(player, ownerUuid, ownerName, item, amount);
            case "cropstorage":
                return executeCropWithdraw(player, ownerUuid, ownerName, item, amount);
            default:
                sendMessage(player, "friends.invalid_storage_type", "#type#", storageType);
                return false;
        }
    }

    private boolean executeDeposit(Player player, UUID ownerUuid, String ownerName, String storageType, String item, int amount) {
        switch (storageType) {
            case "storage":
                return executeStorageDeposit(player, ownerUuid, ownerName, item, amount);
            case "mythicstorage":
                return executeMythicDeposit(player, ownerUuid, ownerName, item, amount);
            case "cropstorage":
                return executeCropDeposit(player, ownerUuid, ownerName, item, amount);
            default:
                sendMessage(player, "friends.invalid_storage_type", "#type#", storageType);
                return false;
        }
    }

    private boolean executeStorageWithdraw(Player player, UUID ownerUuid, String ownerName, String material, int amount) {
        String normalizedMaterial = normalizeMaterial(material);
        int available = MineManager.getPlayerBlock(ownerName, normalizedMaterial);

        if (available < amount) {
            sendMessage(player, "friends.owner_not_enough",
                    new String[]{"#player#", "#material#", "#available#"},
                    new String[]{ownerName, material, String.valueOf(available)});
            return false;
        }

        // Check player inventory space
        int freeSlots = calculateFreeItemSlots(player, normalizedMaterial);
        if (freeSlots < amount) {
            sendMessage(player, "friends.inventory_full", "#slots#", String.valueOf(freeSlots));
            return false;
        }

        // Execute withdraw - remove from owner's storage
        MineManager.removeItemAmount(ownerName, normalizedMaterial, amount);
        // Add items to player's inventory
        addItemsToInventory(player, normalizedMaterial, amount);

        sendMessage(player, "friends.withdraw_success",
                new String[]{"#amount#", "#material#", "#player#"},
                new String[]{String.valueOf(amount), material, ownerName});
        return true;
    }

    private boolean executeStorageDeposit(Player player, UUID ownerUuid, String ownerName, String material, int amount) {
        String normalizedMaterial = normalizeMaterial(material);

        // Check if player has items
        int playerHas = countPlayerItems(player, normalizedMaterial);
        if (playerHas < amount) {
            sendMessage(player, "friends.not_enough_items",
                    new String[]{"#material#", "#available#"},
                    new String[]{material, String.valueOf(playerHas)});
            return false;
        }

        // Check owner storage space
        int ownerCurrent = MineManager.getPlayerBlock(ownerName, normalizedMaterial);
        Player ownerPlayer = Bukkit.getPlayer(ownerUuid);
        int ownerMax = ownerPlayer != null
                ? MineManager.getMaxBlock(ownerPlayer)
                : MineManager.getMaxStorage(ownerUuid);
        int availableSpace = ownerMax - ownerCurrent;

        if (availableSpace < amount) {
            sendMessage(player, "friends.owner_storage_full",
                    new String[]{"#player#", "#space#"},
                    new String[]{ownerName, String.valueOf(availableSpace)});
            return false;
        }

        // Execute deposit - remove from player inventory, add to owner storage
        removePlayerItems(player, normalizedMaterial, amount);
        MineManager.addItemAmount(ownerName, normalizedMaterial, amount);

        sendMessage(player, "friends.deposit_success",
                new String[]{"#amount#", "#material#", "#player#"},
                new String[]{String.valueOf(amount), material, ownerName});
        return true;
    }

    private boolean executeMythicWithdraw(Player player, UUID ownerUuid, String ownerName, String item, int amount) {
        int available = MythicStorageManager.getPlayerItem(ownerName, item);

        if (available < amount) {
            sendMessage(player, "friends.owner_not_enough",
                    new String[]{"#player#", "#material#", "#available#"},
                    new String[]{ownerName, item, String.valueOf(available)});
            return false;
        }

        // Check inventory space
        int freeSlots = calculateFreeSlots(player);
        if (freeSlots < 1) {
            sendMessage(player, "friends.inventory_full", "#slots#", "0");
            return false;
        }

        // Execute withdraw - remove from owner, give to player
        MythicStorageManager.removeItemAmount(ownerName, item, amount);
        giveMythicItem(player, item, amount);

        sendMessage(player, "friends.withdraw_success",
                new String[]{"#amount#", "#material#", "#player#"},
                new String[]{String.valueOf(amount), item, ownerName});
        return true;
    }

    private boolean executeMythicDeposit(Player player, UUID ownerUuid, String ownerName, String item, int amount) {
        // Check if player has the mythic item
        int playerHas = countMythicItems(player, item);
        if (playerHas < amount) {
            sendMessage(player, "friends.not_enough_items",
                    new String[]{"#material#", "#available#"},
                    new String[]{item, String.valueOf(playerHas)});
            return false;
        }

        // Check owner storage space
        int ownerCurrent = MythicStorageManager.getPlayerItem(ownerName, item);
        Player ownerPlayer = Bukkit.getPlayer(ownerUuid);
        int ownerMax = ownerPlayer != null
                ? MythicStorageManager.getMaxStorage(ownerPlayer)
                : MythicStorageManager.getMaxStorage(ownerUuid);
        int availableSpace = ownerMax - ownerCurrent;

        if (availableSpace < amount) {
            sendMessage(player, "friends.owner_storage_full",
                    new String[]{"#player#", "#space#"},
                    new String[]{ownerName, String.valueOf(availableSpace)});
            return false;
        }

        // Execute deposit - remove from player, add to owner
        removeMythicItems(player, item, amount);
        MythicStorageManager.addItemAmount(ownerName, item, amount);

        sendMessage(player, "friends.deposit_success",
                new String[]{"#amount#", "#material#", "#player#"},
                new String[]{String.valueOf(amount), item, ownerName});
        return true;
    }

    private boolean executeCropWithdraw(Player player, UUID ownerUuid, String ownerName, String crop, int amount) {
        String normalizedCrop = normalizeCrop(crop);
        int available = CropStorageManager.getPlayerItem(ownerName, normalizedCrop);

        if (available < amount) {
            sendMessage(player, "friends.owner_not_enough",
                    new String[]{"#player#", "#material#", "#available#"},
                    new String[]{ownerName, crop, String.valueOf(available)});
            return false;
        }

        // Check inventory space
        int freeSlots = calculateFreeSlots(player);
        if (freeSlots < amount) {
            sendMessage(player, "friends.inventory_full", "#slots#", String.valueOf(freeSlots));
            return false;
        }

        // Execute withdraw - remove from owner, give to player
        CropStorageManager.removeItemAmount(ownerName, normalizedCrop, amount, false);
        giveCropItem(player, normalizedCrop, amount);

        sendMessage(player, "friends.withdraw_success",
                new String[]{"#amount#", "#material#", "#player#"},
                new String[]{String.valueOf(amount), crop, ownerName});
        return true;
    }

    private boolean executeCropDeposit(Player player, UUID ownerUuid, String ownerName, String crop, int amount) {
        String normalizedCrop = normalizeCrop(crop);

        // Check if player has crops
        int playerHas = countCropItems(player, normalizedCrop);
        if (playerHas < amount) {
            sendMessage(player, "friends.not_enough_items",
                    new String[]{"#material#", "#available#"},
                    new String[]{crop, String.valueOf(playerHas)});
            return false;
        }

        // Check owner storage space
        int ownerCurrent = CropStorageManager.getPlayerItem(ownerName, normalizedCrop);
        int ownerMax = CropStorageManager.getMaxStorage(ownerName);
        int availableSpace = ownerMax - ownerCurrent;

        if (availableSpace < amount) {
            sendMessage(player, "friends.owner_storage_full",
                    new String[]{"#player#", "#space#"},
                    new String[]{ownerName, String.valueOf(availableSpace)});
            return false;
        }

        // Execute deposit - remove from player, add to owner
        removeCropItems(player, normalizedCrop, amount);
        CropStorageManager.addItemAmount(ownerName, normalizedCrop, amount, false);

        sendMessage(player, "friends.deposit_success",
                new String[]{"#amount#", "#material#", "#player#"},
                new String[]{String.valueOf(amount), crop, ownerName});
        return true;
    }

    private int parseAmount(String amountStr, Player player, String item, String storageType) {
        if (amountStr.equalsIgnoreCase("all")) {
            // Normalize for storage type before counting
            String normalizedItem;
            if (storageType.equals("cropstorage")) {
                normalizedItem = normalizeCrop(item);
            } else if (storageType.equals("mythicstorage")) {
                normalizedItem = item; // mythic items use internal name as-is
            } else {
                normalizedItem = normalizeMaterial(item);
            }
            if (storageType.equals("mythicstorage")) {
                return countMythicItems(player, normalizedItem);
            }
            return countPlayerItems(player, normalizedItem);
        }
        try {
            return Integer.parseInt(amountStr);
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    private int parseWithdrawAmount(String amountStr, String ownerName, String item, String storageType) {
        if (amountStr.equalsIgnoreCase("all")) {
            switch (storageType) {
                case "storage":
                    return MineManager.getPlayerBlock(ownerName, normalizeMaterial(item));
                case "mythicstorage":
                    return MythicStorageManager.getPlayerItem(ownerName, item);
                case "cropstorage":
                    return CropStorageManager.getPlayerItem(ownerName, normalizeCrop(item));
                default:
                    return 0;
            }
        }
        try {
            return Integer.parseInt(amountStr);
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    private int parseAmountForDeposit(String amountStr, Player player, String item,
                                      String storageType, boolean fromHand) {
        if (amountStr.equalsIgnoreCase("all")) {
            if (fromHand) {
                return parseAmount("all", player, item, storageType);
            }
            return getPlayerOwnStorageAmount(player, item, storageType);
        }
        try {
            return Integer.parseInt(amountStr);
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    private int getPlayerOwnStorageAmount(Player player, String item, String storageType) {
        String playerName = player.getName();
        switch (storageType) {
            case "storage":
                return MineManager.getPlayerBlock(playerName, normalizeMaterial(item));
            case "mythicstorage":
                return MythicStorageManager.getPlayerItem(playerName, item);
            case "cropstorage":
                return CropStorageManager.getPlayerItem(playerName, normalizeCrop(item));
            default:
                return 0;
        }
    }

    private boolean executeDepositFromStorage(Player player, UUID ownerUuid, String ownerName,
                                              String storageType, String item, int amount) {
        switch (storageType) {
            case "storage":
                return executeStorageDepositFromStorage(player, ownerName, item, amount);
            case "mythicstorage":
                return executeMythicDepositFromStorage(player, ownerName, item, amount);
            case "cropstorage":
                return executeCropDepositFromStorage(player, ownerName, item, amount);
            default:
                sendMessage(player, "friends.invalid_storage_type", "#type#", storageType);
                return false;
        }
    }

    private boolean executeStorageDepositFromStorage(Player player, String ownerName,
                                                     String material, int amount) {
        String normalizedMaterial = normalizeMaterial(material);
        String playerName = player.getName();

        int playerHas = MineManager.getPlayerBlock(playerName, normalizedMaterial);
        if (playerHas < amount) {
            sendMessage(player, "friends.not_enough_in_storage",
                    new String[]{"#material#", "#available#"},
                    new String[]{material, String.valueOf(playerHas)});
            return false;
        }

        int ownerCurrent = MineManager.getPlayerBlock(ownerName, normalizedMaterial);
        UUID ownerUuid = resolveUuid(ownerName);
        Player ownerPlayer = ownerUuid != null ? Bukkit.getPlayer(ownerUuid) : null;
        int ownerMax = ownerUuid != null
                ? (ownerPlayer != null ? MineManager.getMaxBlock(ownerPlayer) : MineManager.getMaxStorage(ownerUuid))
                : MineManager.getMaxStorage();
        int availableSpace = ownerMax - ownerCurrent;
        if (availableSpace < amount) {
            sendMessage(player, "friends.owner_storage_full",
                    new String[]{"#player#", "#space#"},
                    new String[]{ownerName, String.valueOf(availableSpace)});
            return false;
        }

        MineManager.removeItemAmount(playerName, normalizedMaterial, amount);
        MineManager.addItemAmount(ownerName, normalizedMaterial, amount);

        sendMessage(player, "friends.deposit_from_storage_success",
                new String[]{"#amount#", "#material#", "#player#"},
                new String[]{String.valueOf(amount), material, ownerName});
        return true;
    }

    private boolean executeMythicDepositFromStorage(Player player, String ownerName,
                                                    String item, int amount) {
        String playerName = player.getName();

        int playerHas = MythicStorageManager.getPlayerItem(playerName, item);
        if (playerHas < amount) {
            sendMessage(player, "friends.not_enough_in_storage",
                    new String[]{"#material#", "#available#"},
                    new String[]{item, String.valueOf(playerHas)});
            return false;
        }

        int ownerCurrent = MythicStorageManager.getPlayerItem(ownerName, item);
        UUID ownerUuid = resolveUuid(ownerName);
        Player ownerPlayer = ownerUuid != null ? Bukkit.getPlayer(ownerUuid) : null;
        int ownerMax = ownerUuid != null
                ? (ownerPlayer != null ? MythicStorageManager.getMaxStorage(ownerPlayer) : MythicStorageManager.getMaxStorage(ownerUuid))
                : MythicStorageManager.getMaxStorage();
        int availableSpace = ownerMax - ownerCurrent;
        if (availableSpace < amount) {
            sendMessage(player, "friends.owner_storage_full",
                    new String[]{"#player#", "#space#"},
                    new String[]{ownerName, String.valueOf(availableSpace)});
            return false;
        }

        MythicStorageManager.removeItemAmount(playerName, item, amount);
        MythicStorageManager.addItemAmount(ownerName, item, amount);

        sendMessage(player, "friends.deposit_from_storage_success",
                new String[]{"#amount#", "#material#", "#player#"},
                new String[]{String.valueOf(amount), item, ownerName});
        return true;
    }

    private boolean executeCropDepositFromStorage(Player player, String ownerName,
                                                  String crop, int amount) {
        String normalizedCrop = normalizeCrop(crop);
        String playerName = player.getName();

        int playerHas = CropStorageManager.getPlayerItem(playerName, normalizedCrop);
        if (playerHas < amount) {
            sendMessage(player, "friends.not_enough_in_storage",
                    new String[]{"#material#", "#available#"},
                    new String[]{crop, String.valueOf(playerHas)});
            return false;
        }

        int ownerCurrent = CropStorageManager.getPlayerItem(ownerName, normalizedCrop);
        int ownerMax = CropStorageManager.getMaxStorage(ownerName);
        int availableSpace = ownerMax - ownerCurrent;
        if (availableSpace < amount) {
            sendMessage(player, "friends.owner_storage_full",
                    new String[]{"#player#", "#space#"},
                    new String[]{ownerName, String.valueOf(availableSpace)});
            return false;
        }

        CropStorageManager.removeItemAmount(playerName, normalizedCrop, amount, false);
        CropStorageManager.addItemAmount(ownerName, normalizedCrop, amount, false);

        sendMessage(player, "friends.deposit_from_storage_success",
                new String[]{"#amount#", "#material#", "#player#"},
                new String[]{String.valueOf(amount), crop, ownerName});
        return true;
    }

    private String normalizeMaterial(String material) {
        String normalized = material.replace(":", ";");
        if (!normalized.contains(";")) {
            normalized += ";0";
        }
        return normalized;
    }

    private String normalizeCrop(String crop) {
        return crop.toUpperCase();
    }


    private void loadOfflineData(String playerName, String storageType) {
        switch (storageType) {
            case "storage":
                MineManager.loadOfflinePlayerData(playerName);
                break;
            case "mythicstorage":
                MythicStorageManager.loadOfflinePlayerData(playerName);
                break;
            case "cropstorage":
                CropStorageManager.loadOfflinePlayerData(playerName);
                break;
        }
    }

    private int countPlayerItems(Player player, String material) {
        ItemStack template = getItemStack(material);
        if (template == null) return 0;

        int count = 0;
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.isSimilar(template)) {
                count += item.getAmount();
            }
        }
        return count;
    }

    private int countMythicItems(Player player, String internalName) {
        int count = 0;
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null) {
                String mythicName = MythicStorageManager.getMythicMobsHelper().getMythicItemInternalName(item);
                if (internalName.equalsIgnoreCase(mythicName)) {
                    count += item.getAmount();
                }
            }
        }
        return count;
    }

    private int countCropItems(Player player, String cropKey) {
        return countPlayerItems(player, cropKey);
    }

    private void removePlayerItems(Player player, String material, int amount) {
        ItemStack template = getItemStack(material);
        if (template == null) return;

        int toRemove = amount;
        PlayerInventory inv = player.getInventory();
        ItemStack[] contents = inv.getContents();

        for (int i = 0; i < contents.length && toRemove > 0; i++) {
            ItemStack item = contents[i];
            if (item != null && item.isSimilar(template)) {
                if (item.getAmount() <= toRemove) {
                    toRemove -= item.getAmount();
                    contents[i] = null;
                } else {
                    item.setAmount(item.getAmount() - toRemove);
                    toRemove = 0;
                }
            }
        }
        inv.setContents(contents);
    }

    private void removeMythicItems(Player player, String internalName, int amount) {
        int toRemove = amount;
        PlayerInventory inv = player.getInventory();
        ItemStack[] contents = inv.getContents();

        for (int i = 0; i < contents.length && toRemove > 0; i++) {
            ItemStack item = contents[i];
            if (item != null) {
                String mythicName = MythicStorageManager.getMythicMobsHelper().getMythicItemInternalName(item);
                if (internalName.equalsIgnoreCase(mythicName)) {
                    if (item.getAmount() <= toRemove) {
                        toRemove -= item.getAmount();
                        contents[i] = null;
                    } else {
                        item.setAmount(item.getAmount() - toRemove);
                        toRemove = 0;
                    }
                }
            }
        }
        inv.setContents(contents);
    }

    private void removeCropItems(Player player, String cropKey, int amount) {
        removePlayerItems(player, cropKey, amount);
    }

    private void addItemsToInventory(Player player, String material, int amount) {
        ItemStack template = getItemStack(material);
        if (template == null) return;

        int remaining = amount;
        while (remaining > 0) {
            ItemStack toAdd = template.clone();
            int stackSize = Math.min(remaining, template.getMaxStackSize());
            toAdd.setAmount(stackSize);
            player.getInventory().addItem(toAdd);
            remaining -= stackSize;
        }
    }

    private void giveMythicItem(Player player, String internalName, int amount) {
        ItemStack mythicItem = MythicMobsHelper.getMythicItemStack(internalName, amount);
        if (mythicItem != null) {
            player.getInventory().addItem(mythicItem);
        }
    }

    private void giveCropItem(Player player, String cropKey, int amount) {
        addItemsToInventory(player, cropKey, amount);
    }

    private ItemStack getItemStack(String material) {
        return MaterialUtils.createItem(material);
    }

    private int calculateFreeItemSlots(Player player, String material) {
        ItemStack template = getItemStack(material);
        if (template == null) return 0;

        int freeSlots = 0;
        for (ItemStack item : player.getInventory().getContents()) {
            if (isEmptyItem(item)) {
                freeSlots += template.getMaxStackSize();
            } else if (item.isSimilar(template)) {
                freeSlots += template.getMaxStackSize() - item.getAmount();
            }
        }
        return freeSlots;
    }

    private int calculateFreeSlots(Player player) {
        int freeSlots = 0;
        for (ItemStack item : player.getInventory().getContents()) {
            if (isEmptyItem(item)) {
                freeSlots++;
            }
        }
        return freeSlots * 64;
    }

    private boolean isEmptyItem(ItemStack item) {
        return item == null || item.getType() == Material.AIR;
    }

    private UUID resolveUuid(String playerName) {
        Player online = Bukkit.getPlayer(playerName);
        if (online != null) return online.getUniqueId();

        OfflinePlayer offline = Bukkit.getOfflinePlayer(playerName);
        if (offline.hasPlayedBefore()) return offline.getUniqueId();

        return null;
    }

    private String resolvePlayerName(UUID uuid) {
        Player online = Bukkit.getPlayer(uuid);
        if (online != null) return online.getName();

        OfflinePlayer offline = Bukkit.getOfflinePlayer(uuid);
        String name = offline.getName();
        return name != null ? name : uuid.toString();
    }

    private String formatTimeRemaining(long millis) {
        if (millis <= 0) return "expired";
        long seconds = millis / 1000;
        if (seconds < 60) return seconds + "s";
        long minutes = seconds / 60;
        if (minutes < 60) return minutes + "m " + (seconds % 60) + "s";
        long hours = minutes / 60;
        return hours + "h " + (minutes % 60) + "m";
    }

    private String formatTimestamp(long millis) {
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("dd/MM HH:mm");
        return sdf.format(new java.util.Date(millis));
    }

    private boolean isValidStorageType(String type) {
        for (String validType : STORAGE_TYPES) {
            if (validType.equalsIgnoreCase(type)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public List<String> getTabCompletions(CommandSender sender, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            StringUtil.copyPartialMatches(args[0], Arrays.asList(SUBCOMMANDS), completions);
        } else if (args.length == 2) {
            String sub = args[0].toLowerCase();
            switch (sub) {
                case "add":
                case "withdraw":
                case "deposit":
                    if (sender instanceof Player) {
                        StringUtil.copyPartialMatches(args[1],
                                getOnlinePlayerNamesExcept(sender.getName()), completions);
                    }
                    break;
                case "accept":
                case "deny":
                    if (sender instanceof Player) {
                        Player player = (Player) sender;
                        Map<UUID, Long> pending = FriendManager.getPendingRequests(player.getUniqueId());
                        List<String> senderNames = new ArrayList<>();
                        for (UUID senderUuid : pending.keySet()) {
                            senderNames.add(resolvePlayerName(senderUuid));
                        }
                        StringUtil.copyPartialMatches(args[1], senderNames, completions);
                    }
                    break;
                case "remove":
                    if (sender instanceof Player) {
                        Player player = (Player) sender;
                        Set<UUID> friends = FriendManager.getFriends(player.getUniqueId());
                        List<String> friendNames = new ArrayList<>();
                        for (UUID friendUuid : friends) {
                            friendNames.add(resolvePlayerName(friendUuid));
                        }
                        StringUtil.copyPartialMatches(args[1], friendNames, completions);
                    }
                    break;
                case "settings":
                    List<String> settingsOptions = new ArrayList<>();
                    settingsOptions.add("storage");
                    settingsOptions.add("mythicstorage");
                    settingsOptions.add("cropstorage");
                    settingsOptions.add("all");
                    settingsOptions.add("help");
                    StringUtil.copyPartialMatches(args[1], settingsOptions, completions);
                    break;
                case "history":
                    // Page numbers for history
                    break;
            }
        } else if (args.length == 3) {
            String sub = args[0].toLowerCase();
            switch (sub) {
                case "withdraw":
                case "deposit":
                    StringUtil.copyPartialMatches(args[2], Arrays.asList(STORAGE_TYPES), completions);
                    break;
                case "settings":
                    String setting = args[1].toLowerCase();
                    if (isValidStorageType(setting) || setting.equals("all")) {
                        StringUtil.copyPartialMatches(args[2], Arrays.asList("allow", "deny"), completions);
                    }
                    break;
            }
        } else if (args.length == 4) {
            String sub = args[0].toLowerCase();
            if (sub.equals("withdraw") || sub.equals("deposit")) {
                String storageType = args[2].toLowerCase();
                if (sender instanceof Player) {
                    String friendName = args[1];
                    UUID friendUuid = resolveUuid(friendName);
                    if (friendUuid != null) {
                        completions.addAll(getStorageItems(friendUuid, storageType, args[3]));
                    }
                }
            } else if (sub.equals("settings")) {
                String setting = args[1].toLowerCase();
                if (isValidStorageType(setting) || setting.equals("all")) {
                    StringUtil.copyPartialMatches(args[3], Arrays.asList("deposit", "withdraw", "access", "both", "all"), completions);
                }
            }
        } else if (args.length == 5) {
            String sub = args[0].toLowerCase();
            if (sub.equals("withdraw") || sub.equals("deposit")) {
                List<String> amounts = new ArrayList<>();
                amounts.add("all");
                amounts.add("1");
                amounts.add("10");
                amounts.add("64");
                StringUtil.copyPartialMatches(args[4], amounts, completions);
            }
        }

        Collections.sort(completions);
        return completions;
    }

    private List<String> getStorageItems(UUID ownerUuid, String storageType, String partial) {
        Set<String> items = new LinkedHashSet<>();
        String normalizedPartial = partial.toLowerCase();
        String ownerName = resolvePlayerName(ownerUuid);
        if (ownerName == null || ownerName.equals(ownerUuid.toString())) {
            return new ArrayList<>(items);
        }

        switch (storageType) {
            case "storage":
                for (String key : new ArrayList<>(MineManager.playerdata.keySet())) {
                    if (key.startsWith(ownerName + "_")) {
                        String material = key.substring(ownerName.length() + 1);
                        int amount = MineManager.playerdata.getOrDefault(key, 0);
                        String display = material.split(";")[0];
                        if (amount > 0 && display.toLowerCase().startsWith(normalizedPartial)) {
                            items.add(display);
                            if (items.size() >= 80) return new ArrayList<>(items);
                        }
                    }
                }
                break;
            case "mythicstorage":
                for (String key : new ArrayList<>(MythicStorageManager.playerdata.keySet())) {
                    if (key.startsWith(ownerName + "_")) {
                        String material = key.substring(ownerName.length() + 1);
                        int amount = MythicStorageManager.playerdata.getOrDefault(key, 0);
                        if (amount > 0 && material.toLowerCase().startsWith(normalizedPartial)) {
                            items.add(material);
                            if (items.size() >= 80) return new ArrayList<>(items);
                        }
                    }
                }
                break;
            case "cropstorage":
                for (String key : new ArrayList<>(CropStorageManager.playerdata.keySet())) {
                    if (key.startsWith(ownerName + "_")) {
                        String material = key.substring(ownerName.length() + 1);
                        int amount = CropStorageManager.playerdata.getOrDefault(key, 0);
                        if (amount > 0 && material.toLowerCase().startsWith(normalizedPartial)) {
                            items.add(material);
                            if (items.size() >= 80) return new ArrayList<>(items);
                        }
                    }
                }
                break;
        }

        return new ArrayList<>(items);
    }

    @Override
    public String getPermission() {
        return "storage.friends.use";
    }

    @Override
    public String getUsage() {
        return "/storage friends <add|accept|deny|remove|list|withdraw|deposit|history|settings> [args...]";
    }

    @Override
    public String getDescription() {
        return "Friend Storage management - withdraw/deposit from friend's storage";
    }
}
