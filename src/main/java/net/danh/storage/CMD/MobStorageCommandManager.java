package net.danh.storage.CMD;

import net.danh.storage.Action.MobDeposit;
import net.danh.storage.Action.MobSell;
import net.danh.storage.Action.MobWithdraw;
import net.danh.storage.CMD.handler.BaseCommand;
import net.danh.storage.GUI.Mob.MobStorageGUI;
import net.danh.storage.GUI.Mob.MobTransferGUI;
import net.danh.storage.GUI.Mob.MobTransferMultiGUI;
import net.danh.storage.GUI.Mob.ViewMobStorageGUI;
import net.danh.storage.Manager.Mob.MobStorageManager;
import net.danh.storage.Manager.Mob.MobTransferManager;
import net.danh.storage.Storage;
import net.danh.storage.Utils.AutoPickupCache;
import net.danh.storage.Utils.ChatUtils;
import net.danh.storage.Utils.File;
import net.danh.storage.Utils.Number;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.StringUtil;

import java.util.*;
import java.util.stream.Collectors;

public class MobStorageCommandManager extends BaseCommand {

    private static final List<String> USER_COMMANDS = Arrays.asList("help", "toggle", "view", "transfer", "deposit", "withdraw", "sell", "autosell", "groundstore", "reload");
    private static final List<String> ADMIN_COMMANDS = Arrays.asList("add", "remove", "set", "max", "reset", "resetlimit", "reload");

    public void handleCommand(CommandSender sender, String[] args) {
        if (args.length > 0 && isReloadCommand(args)) {
            reload(sender);
            return;
        }

        if (!MobStorageManager.isSystemEnabled()) {
            sendMessage(sender, "mobstorage.system_disabled");
            return;
        }

        if (args.length == 0) {
            openGUI(sender);
            return;
        }

        String commandName = args[0].toLowerCase(Locale.ENGLISH);
        if ("admin".equals(commandName)) {
            handleAdmin(sender, Arrays.copyOfRange(args, 1, args.length));
            return;
        }

        if ("reload".equals(commandName)) {
            reload(sender);
        } else if ("help".equals(commandName)) {
            sendMessageList(sender, "mobstorage.user.help");
        } else if ("toggle".equals(commandName)) {
            toggle(sender);
        } else if ("groundstore".equals(commandName)) {
            groundStore(sender);
        } else if ("deposit".equals(commandName)) {
            deposit(sender, args);
        } else if ("withdraw".equals(commandName)) {
            withdraw(sender, args);
        } else if ("sell".equals(commandName)) {
            sell(sender, args);
        } else if ("autosell".equals(commandName)) {
            autoSell(sender, args);
        } else if ("view".equals(commandName)) {
            view(sender, args);
        } else if ("transfer".equals(commandName)) {
            transfer(sender, args);
        } else {
            sendMessage(sender, "mobstorage.admin.unknown_command", "#command#", commandName);
        }
    }

    private void openGUI(CommandSender sender) {
        if (!requirePlayer(sender)) {
            return;
        }
        Player player = (Player) sender;
        if (!sender.hasPermission("storage.mobstorage.use")) {
            sendMessage(sender, "mobstorage.admin.no_permission");
            return;
        }
        if (isMobStorageWorldBlacklisted(player)) {
            sendWorldBlacklisted(sender, "MobStorage", player.getWorld().getName());
            return;
        }
        player.openInventory(new MobStorageGUI(player).getInventory());
    }

    private void toggle(CommandSender sender) {
        if (!requirePlayer(sender)) {
            return;
        }
        if (!sender.hasPermission("storage.mobstorage.toggle")) {
            sendMessage(sender, "mobstorage.admin.no_permission");
            return;
        }
        Player player = (Player) sender;
        if (isMobStorageWorldBlacklisted(player)) {
            sendWorldBlacklisted(sender, "MobStorage", player.getWorld().getName());
            return;
        }
        boolean current = MobStorageManager.getToggleStatus(player);
        boolean applied = MobStorageManager.setToggleStatus(player, !current, true);
        if (applied != current) {
            sendMessage(sender, applied ? "mobstorage.toggle_enabled" : "mobstorage.toggle_disabled");
        }
    }

    private void groundStore(CommandSender sender) {
        if (!requirePlayer(sender)) {
            return;
        }
        if (!sender.hasPermission("storage.mobstorage.groundstore")) {
            sendMessage(sender, "mobstorage.ground_store_no_permission");
            return;
        }
        if (!MobStorageManager.isGroundStoreSystemEnabled()) {
            sendMessage(sender, "mobstorage.ground_store_system_disabled");
            return;
        }
        Player player = (Player) sender;
        if (isMobStorageWorldBlacklisted(player)) {
            sendWorldBlacklisted(sender, "MobStorage", player.getWorld().getName());
            return;
        }
        boolean enabled = MobStorageManager.toggleGroundStore(player, true);
        sendMessage(sender, enabled ? "mobstorage.ground_store_toggle_on" : "mobstorage.ground_store_toggle_off");
    }

    private void deposit(CommandSender sender, String[] args) {
        if (!requirePlayer(sender)) {
            return;
        }
        if (!sender.hasPermission("storage.mobstorage.deposit")) {
            sendMessage(sender, "mobstorage.admin.no_permission");
            return;
        }
        if (args.length < 2 || args.length > 3) {
            sendMessage(sender, "mobstorage.admin.invalid_usage", "#usage#", "/mobstorage deposit <item> [amount|all]");
            return;
        }
        Player player = (Player) sender;
        String itemName = args[1].toUpperCase(Locale.ENGLISH);
        if (!MobStorageManager.isConfiguredDrop(itemName)) {
            sendMessage(sender, "mobstorage.invalid_item", "#item#", itemName);
            return;
        }
        long amount = args.length == 2 || "all".equalsIgnoreCase(args[2]) ? countEligibleItems(player, itemName) : Number.getLong(args[2]);
        if (amount <= 0) {
            sendMessage(sender, "mobstorage.action.deposit.no_items");
            return;
        }
        new MobDeposit(player, itemName, amount).doAction();
    }

    private void withdraw(CommandSender sender, String[] args) {
        if (!requirePlayer(sender)) {
            return;
        }
        if (!sender.hasPermission("storage.mobstorage.withdraw")) {
            sendMessage(sender, "mobstorage.admin.no_permission");
            return;
        }
        if (args.length < 2 || args.length > 3) {
            sendMessage(sender, "mobstorage.admin.invalid_usage", "#usage#", "/mobstorage withdraw <item> [amount|all]");
            return;
        }
        Player player = (Player) sender;
        String itemName = args[1].toUpperCase(Locale.ENGLISH);
        if (!MobStorageManager.isConfiguredDrop(itemName)) {
            sendMessage(sender, "mobstorage.invalid_item", "#item#", itemName);
            return;
        }
        long amount = args.length == 2 || "all".equalsIgnoreCase(args[2]) ? MobStorageManager.getPlayerItem(player, itemName) : Number.getLong(args[2]);
        if (amount <= 0) {
            sendMessage(sender, "mobstorage.no_items");
            return;
        }
        new MobWithdraw(player, itemName, amount).doAction();
    }

    private void sell(CommandSender sender, String[] args) {
        if (!requirePlayer(sender)) {
            return;
        }
        if (!sender.hasPermission("storage.mobstorage.sell")) {
            sendMessage(sender, "mobstorage.admin.no_permission");
            return;
        }
        if (args.length < 2 || args.length > 3) {
            sendMessage(sender, "mobstorage.admin.invalid_usage", "#usage#", "/mobstorage sell <item> [amount|all]");
            return;
        }
        Player player = (Player) sender;
        if (isMobStorageWorldBlacklisted(player)) {
            sendWorldBlacklisted(sender, "MobStorage", player.getWorld().getName());
            return;
        }
        String itemName = args[1].toUpperCase(Locale.ENGLISH);
        if (!MobStorageManager.isConfiguredDrop(itemName)) {
            sendMessage(sender, "mobstorage.invalid_item", "#item#", itemName);
            return;
        }
        long amount = args.length == 2 || "all".equalsIgnoreCase(args[2]) ? -1 : Number.getLong(args[2]);
        if (args.length == 3 && amount <= 0 && !"all".equalsIgnoreCase(args[2])) {
            sendMessage(sender, "mobstorage.admin.invalid_number", "#number#", args[2]);
            return;
        }
        new MobSell(player, itemName, amount).doAction();
    }

    private void autoSell(CommandSender sender, String[] args) {
        if (!requirePlayer(sender)) {
            return;
        }
        if (!sender.hasPermission("storage.mobstorage.autosell")) {
            sendMessage(sender, "mobstorage.admin.no_permission");
            return;
        }

        Player player = (Player) sender;
        if (isMobStorageWorldBlacklisted(player)) {
            sendWorldBlacklisted(sender, "MobStorage", player.getWorld().getName());
            return;
        }
        if (!MobStorageManager.isAutoSellSystemEnabled()) {
            String msg = File.getMessage().getString("mobstorage.autosell.system_disabled", "");
            if (msg != null && !msg.trim().isEmpty()) {
                player.sendMessage(ChatUtils.colorizewp(msg));
            }
            return;
        }
        if (args.length < 2) {
            sendMessage(sender, "mobstorage.admin.invalid_usage", "#usage#", "/mobstorage autosell <toggle|set|list|clear> [item] [on|off]");
            return;
        }

        String sub = args[1].toLowerCase(Locale.ENGLISH);
        if ("clear".equals(sub)) {
            int cleared = MobStorageManager.clearAutoSell(player);
            player.sendMessage(ChatUtils.colorizewp(File.getMessage().getString(
                            "mobstorage.autosell.cleared",
                            "#prefix# &aCleared &e#count# &aAutoSell mob drops.")
                    .replace("#count#", String.valueOf(cleared))));
            return;
        }
        if ("list".equals(sub)) {
            Set<String> enabled = MobStorageManager.getEnabledAutoSellItems(player);
            if (enabled.isEmpty()) {
                player.sendMessage(ChatUtils.colorizewp(File.getMessage().getString("mobstorage.autosell.list_empty", "")));
                return;
            }
            String joined = enabled.stream()
                    .map(MobStorageManager::getItemDisplayName)
                    .map(ChatUtils::colorizewp)
                    .collect(Collectors.joining(ChatUtils.colorizewp("&7, ")));
            player.sendMessage(ChatUtils.colorizewp(File.getMessage().getString(
                            "mobstorage.autosell.list",
                            "#prefix# &aAutoSell mob drops: &f#items#")
                    .replace("#items#", joined)));
            return;
        }
        if (args.length < 3) {
            sendMessage(sender, "mobstorage.admin.invalid_usage", "#usage#", "/mobstorage autosell <toggle|set> <item> [on|off]");
            return;
        }

        String itemName = args[2].toUpperCase(Locale.ENGLISH);
        if (!MobStorageManager.isConfiguredDrop(itemName)) {
            sendMessage(sender, "mobstorage.invalid_item", "#item#", itemName);
            return;
        }
        if ("toggle".equals(sub)) {
            boolean enabled = MobStorageManager.toggleItemAutoSell(player, itemName);
            sendAutoSellToggleMessage(player, itemName, enabled);
            return;
        }
        if ("set".equals(sub)) {
            if (args.length < 4) {
                sendMessage(sender, "mobstorage.admin.invalid_usage", "#usage#", "/mobstorage autosell set <item> <on|off>");
                return;
            }
            Boolean desired = parseOnOff(args[3]);
            if (desired == null) {
                sendMessage(sender, "mobstorage.admin.invalid_usage", "#usage#", "/mobstorage autosell set <item> <on|off>");
                return;
            }
            boolean enabled = MobStorageManager.setItemAutoSell(player, itemName, desired);
            sendAutoSellToggleMessage(player, itemName, enabled);
            return;
        }
        sendMessage(sender, "mobstorage.admin.invalid_usage", "#usage#", "/mobstorage autosell <toggle|set|list|clear> [item] [on|off]");
    }

    private void sendAutoSellToggleMessage(Player player, String itemName, boolean enabled) {
        String key = enabled ? "mobstorage.autosell.toggle_on" : "mobstorage.autosell.toggle_off";
        String msg = File.getMessage().getString(key, "");
        if (msg != null && !msg.trim().isEmpty()) {
            player.sendMessage(ChatUtils.colorizewp(msg.replace("#material#", MobStorageManager.getItemDisplayName(itemName))));
        }
    }

    private Boolean parseOnOff(String raw) {
        if (raw == null) {
            return null;
        }
        String value = raw.trim().toLowerCase(Locale.ENGLISH);
        if ("on".equals(value) || "true".equals(value) || "enable".equals(value) || "enabled".equals(value)) {
            return true;
        }
        if ("off".equals(value) || "false".equals(value) || "disable".equals(value) || "disabled".equals(value)) {
            return false;
        }
        return null;
    }

    private void view(CommandSender sender, String[] args) {
        if (!requirePlayer(sender)) {
            return;
        }
        if (!sender.hasPermission("storage.mobstorage.view")) {
            sendMessage(sender, "mobstorage.admin.no_permission");
            return;
        }
        Player viewer = (Player) sender;
        if (isMobStorageWorldBlacklisted(viewer)) {
            sendWorldBlacklisted(sender, "MobStorage", viewer.getWorld().getName());
            return;
        }
        Player target;
        if (args.length >= 2) {
            target = Bukkit.getPlayer(args[1]);
            if (target == null) {
                sendMessage(sender, "mobstorage.player_not_found", "#player#", args[1]);
                return;
            }
        } else {
            target = viewer;
        }
        sendMessage(sender, "mobstorage.viewing_storage", "#player#", target.getName());
        viewer.openInventory(new ViewMobStorageGUI(viewer, target).getInventory());
    }

    private void transfer(CommandSender sender, String[] args) {
        if (!requirePlayer(sender)) {
            return;
        }
        if (!sender.hasPermission("storage.mobstorage.transfer.use")) {
            sendMessage(sender, "mobstorage.admin.no_permission");
            return;
        }
        Player player = (Player) sender;
        if (isMobStorageWorldBlacklisted(player)) {
            sendWorldBlacklisted(sender, "MobStorage", player.getWorld().getName());
            return;
        }
        if (!File.getMobStorageConfig().getBoolean("transfer.enabled", true)) {
            sendMessage(sender, "mobstorage.transfer.disabled");
            return;
        }
        if (args.length >= 2) {
            if ("log".equalsIgnoreCase(args[1])) {
                handleTransferLog(player, args);
                return;
            }
            if ("multi".equalsIgnoreCase(args[1])) {
                handleTransferMulti(player, args);
                return;
            }
        }
        if (args.length < 3 || args.length > 4) {
            sendMessage(sender, "mobstorage.transfer.usage");
            return;
        }
        String targetPlayer = args[1];
        Player receiver = Bukkit.getPlayer(targetPlayer);
        if (receiver == null || !receiver.isOnline()) {
            sendMessage(sender, "mobstorage.transfer.failed_offline", "#player#", targetPlayer);
            return;
        }
        if (player.getName().equalsIgnoreCase(receiver.getName())) {
            sendMessage(sender, "mobstorage.transfer.failed_same_player");
            return;
        }
        String itemName = args[2].toUpperCase(Locale.ENGLISH);
        if (!MobStorageManager.isConfiguredDrop(itemName)) {
            sendMessage(sender, "mobstorage.invalid_item", "#item#", itemName);
            return;
        }
        int currentAmount = MobStorageManager.getPlayerItem(player, itemName);
        if (currentAmount <= 0) {
            sendMessage(sender, "mobstorage.transfer.failed_insufficient",
                    new String[]{"#item#", "#current#"},
                    new String[]{MobStorageManager.getItemDisplayName(itemName), "0"});
            return;
        }
        if (args.length == 3) {
            player.openInventory(new MobTransferGUI(player, targetPlayer, itemName).getInventory());
            return;
        }
        int amount;
        if ("all".equalsIgnoreCase(args[3])) {
            amount = currentAmount;
        } else {
            amount = (int) Number.getLong(args[3]);
            if (amount <= 0) {
                sendMessage(sender, "mobstorage.admin.invalid_number", "#number#", args[3]);
                return;
            }
        }
        MobTransferManager.executeTransfer(player, targetPlayer, itemName, amount);
    }

    private void handleTransferMulti(Player player, String[] args) {
        if (args.length < 3) {
            sendMessage(player, "mobstorage.transfer.usage_multi");
            return;
        }
        String targetPlayer = args[2];
        Player receiver = Bukkit.getPlayer(targetPlayer);
        if (receiver == null || !receiver.isOnline()) {
            sendMessage(player, "mobstorage.transfer.failed_offline", "#player#", targetPlayer);
            return;
        }
        if (player.getName().equalsIgnoreCase(receiver.getName())) {
            sendMessage(player, "mobstorage.transfer.failed_same_player");
            return;
        }
        player.openInventory(new MobTransferMultiGUI(player, targetPlayer).getInventory());
    }

    private void handleTransferLog(Player player, String[] args) {
        String targetPlayer = null;
        int page = 1;

        if (args.length >= 3) {
            if (args[2] != null && args[2].matches("\\d+")) {
                page = (int) Number.getLong(args[2]);
                if (page < 1) {
                    page = 1;
                }
            } else {
                targetPlayer = args[2];
            }
        }

        if (args.length >= 4) {
            if (args[3] != null && args[3].matches("\\d+")) {
                page = (int) Number.getLong(args[3]);
                if (page < 1) {
                    page = 1;
                }
            } else {
                page = 1;
            }
        }

        MobTransferManager.displayTransferHistory(player, targetPlayer, page);
    }

    private void handleAdmin(CommandSender sender, String[] args) {
        if (args.length == 0 || "help".equalsIgnoreCase(args[0])) {
            sendMessageList(sender, "mobstorage.admin.help");
            return;
        }
        String action = args[0].toLowerCase(Locale.ENGLISH);
        if (!sender.hasPermission("storage.mobstorage.admin." + action) && !sender.hasPermission("storage.mobstorage.admin")) {
            sendMessage(sender, "mobstorage.admin.no_permission");
            return;
        }
        if ("reload".equals(action)) {
            reload(sender);
            return;
        }
        if ("max".equals(action)) {
            adminMax(sender, args);
            return;
        }
        if ("reset".equals(action)) {
            adminReset(sender, args);
            return;
        }
        if ("resetlimit".equals(action)) {
            adminResetLimit(sender, args);
            return;
        }
        if ("add".equals(action) || "remove".equals(action) || "set".equals(action)) {
            adminAmount(sender, args, action);
            return;
        }
        sendMessage(sender, "mobstorage.admin.unknown_command", "#command#", action);
    }

    private boolean isReloadCommand(String[] args) {
        if ("reload".equalsIgnoreCase(args[0])) {
            return true;
        }
        return args.length >= 2 && "admin".equalsIgnoreCase(args[0]) && "reload".equalsIgnoreCase(args[1]);
    }

    private void adminMax(CommandSender sender, String[] args) {
        if (args.length != 3) {
            sendMessage(sender, "mobstorage.admin.invalid_usage", "#usage#", "/mobstorage admin max <player> <amount>");
            return;
        }
        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sendMessage(sender, "mobstorage.player_not_found", "#player#", args[1]);
            return;
        }
        int amount = Number.getInteger(args[2]);
        if (amount < 0) {
            sendMessage(sender, "mobstorage.admin.invalid_number", "#number#", args[2]);
            return;
        }
        MobStorageManager.setMaxStorageOverride(target, amount);
        MobStorageManager.playermaxdata.put(target.getUniqueId(), amount);
        MobStorageManager.savePlayerData(target);
        sendMessage(sender, "mobstorage.admin.set_max_storage", new String[]{"#player#", "#amount#"}, new String[]{target.getName(), String.valueOf(amount)});
    }

    private void adminAmount(CommandSender sender, String[] args, String action) {
        if (args.length != 4) {
            sendMessage(sender, "mobstorage.admin.invalid_usage", "#usage#", "/mobstorage admin " + action + " <player> <item> <amount>");
            return;
        }
        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sendMessage(sender, "mobstorage.player_not_found", "#player#", args[1]);
            return;
        }
        String itemName = args[2].toUpperCase(Locale.ENGLISH);
        if (!MobStorageManager.isConfiguredDrop(itemName)) {
            sendMessage(sender, "mobstorage.invalid_item", "#item#", itemName);
            return;
        }
        int amount = Number.getInteger(args[3]);
        if (amount <= 0) {
            sendMessage(sender, "mobstorage.admin.invalid_number", "#number#", args[3]);
            return;
        }
        boolean success = true;
        if ("add".equals(action)) {
            success = MobStorageManager.addItemAmount(target, itemName, amount, false);
        } else if ("remove".equals(action)) {
            success = MobStorageManager.removeItemAmount(target, itemName, amount, false);
        } else {
            MobStorageManager.setItemAmount(target, itemName, amount);
        }
        if (!success) {
            String messageKey = "add".equals(action) ? "mobstorage.admin.storage_full" : "mobstorage.admin.not_enough_items";
            sendMessage(sender, messageKey,
                    new String[]{"#player#", "#item#", "#amount#"},
                    new String[]{target.getName(), MobStorageManager.getItemDisplayName(itemName), String.valueOf(amount)});
            return;
        }
        MobStorageManager.savePlayerData(target);
        sendMessage(sender, "mobstorage.admin." + action + "_success",
                new String[]{"#player#", "#item#", "#amount#"},
                new String[]{target.getName(), MobStorageManager.getItemDisplayName(itemName), String.valueOf(amount)});
    }

    private void adminReset(CommandSender sender, String[] args) {
        if (args.length < 2 || args.length > 3) {
            sendMessage(sender, "mobstorage.admin.invalid_usage", "#usage#", "/mobstorage admin reset <player> [item]");
            return;
        }
        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sendMessage(sender, "mobstorage.player_not_found", "#player#", args[1]);
            return;
        }
        if (args.length == 3) {
            String itemName = args[2].toUpperCase(Locale.ENGLISH);
            if (!MobStorageManager.isConfiguredDrop(itemName)) {
                sendMessage(sender, "mobstorage.invalid_item", "#item#", itemName);
                return;
            }
            MobStorageManager.setItemAmount(target, itemName, 0);
            MobStorageManager.savePlayerData(target);
            sendMessage(sender, "mobstorage.admin.reset_item_success",
                    new String[]{"#player#", "#item#"},
                    new String[]{target.getName(), MobStorageManager.getItemDisplayName(itemName)});
            if (target.isOnline()) {
                sendMessage(target, "mobstorage.admin.reset_item_notify",
                        new String[]{"#player#", "#item#"},
                        new String[]{sender.getName(), MobStorageManager.getItemDisplayName(itemName)});
            }
            return;
        }
        for (String itemName : MobStorageManager.getConfiguredDrops()) {
            MobStorageManager.setItemAmount(target, itemName, 0);
        }
        MobStorageManager.savePlayerData(target);
        sendMessage(sender, "mobstorage.admin.reset_all_success", "#player#", target.getName());
        if (target.isOnline()) {
            sendMessage(target, "mobstorage.admin.reset_notify", "#player#", sender.getName());
        }
    }

    private void adminResetLimit(CommandSender sender, String[] args) {
        if (args.length != 2) {
            sendMessage(sender, "mobstorage.admin.invalid_usage", "#usage#", "/mobstorage admin resetlimit <player>");
            return;
        }
        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sendMessage(sender, "mobstorage.player_not_found", "#player#", args[1]);
            return;
        }
        int defaultMax = File.getMobStorageConfig().getInt("settings.default_max_storage", 5000);

        MobStorageManager.clearMaxStorageOverride(target);
        MobStorageManager.savePlayerData(target);

        net.danh.storage.Database.PlayerData existing = Storage.dataStorage.getData(target.getName());
        if (existing != null) {
            Integer parsedOverride = null;
            String rawData = existing.data();
            if (rawData != null && !rawData.isEmpty()) {
                int idx = rawData.indexOf("mobmaxoverride:");
                if (idx >= 0) {
                    int start = idx + "mobmaxoverride:".length();
                    int end = rawData.indexOf(';', start);
                    String raw = end >= 0
                            ? rawData.substring(start, end)
                            : rawData.substring(start);
                    raw = raw.trim();
                    try {
                        parsedOverride = Integer.parseInt(raw);
                    } catch (NumberFormatException ignored) {
                    }
                }
            }

            if (parsedOverride != null
                    && parsedOverride >= 0
                    && existing.max() == parsedOverride) {
                net.danh.storage.Database.PlayerData cleaned = new net.danh.storage.Database.PlayerData(
                        existing.player(),
                        existing.data(),
                        Math.max(0, defaultMax),
                        existing.autoPickup());
                Storage.dataStorage.updateTable(cleaned);
            }
        }

        MobStorageManager.loadPlayerData(target);
        MobStorageManager.savePlayerData(target);

        sendMessage(sender, "mobstorage.admin.reset_limit_storage", "#player#", target.getName());
    }

    private long countEligibleItems(Player player, String itemName) {
        Material material = MobStorageManager.resolveMaterial(itemName.toUpperCase(Locale.ENGLISH));
        if (material == null) {
            return 0;
        }
        long count = 0;
        for (ItemStack item : player.getInventory().getContents()) {
            if (item == null || item.getType() != material) {
                continue;
            }
            if (!File.getMobStorageConfig().getBoolean("settings.allow_custom_item_meta", false)
                    && item.hasItemMeta()) {
                continue;
            }
            count += item.getAmount();
        }
        return count;
    }

    private boolean isMobStorageWorldBlacklisted(Player player) {
        return player != null && File.getMobStorageConfig().getStringList("blacklist_world").contains(player.getWorld().getName());
    }

    private void reloadOnlinePlayers() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            MobStorageManager.savePlayerData(player);
            MobStorageManager.loadPlayerData(player);
        }
    }

    private void reload(CommandSender sender) {
        if (!sender.hasPermission("storage.mobstorage.admin.reload") && !sender.hasPermission("storage.mobstorage.admin")) {
            sendMessage(sender, "mobstorage.admin.no_permission");
            return;
        }
        File.getFileSetting().reload("mobstorage.yml", "GUI/mobstorage.yml", "GUI/mob-items.yml", "GUI/mob-transfer.yml", "GUI/mob-transfer-multi.yml", "GUI/view-mobstorage.yml", "message.yml");
        File.updateMobStorageConfig();
        MobStorageManager.reloadConfiguredDrops();
        AutoPickupCache.reload();
        reloadOnlinePlayers();
        sendMessage(sender, "mobstorage.admin.reload_success");
    }

    public List<String> getCommandTabCompletions(CommandSender sender, String[] args) {
        List<String> completions = new ArrayList<>();
        if (args.length == 1) {
            List<String> options = new ArrayList<>(USER_COMMANDS);
            options.add("admin");
            StringUtil.copyPartialMatches(args[0], options, completions);
        } else if (args.length == 2 && "admin".equalsIgnoreCase(args[0])) {
            StringUtil.copyPartialMatches(args[1], ADMIN_COMMANDS, completions);
        } else if (args.length == 2 && ("deposit".equalsIgnoreCase(args[0]) || "withdraw".equalsIgnoreCase(args[0]) || "sell".equalsIgnoreCase(args[0]))) {
            StringUtil.copyPartialMatches(args[1], MobStorageManager.getConfiguredDrops(), completions);
        } else if (args.length == 2 && "autosell".equalsIgnoreCase(args[0])) {
            StringUtil.copyPartialMatches(args[1], Arrays.asList("toggle", "set", "list", "clear"), completions);
        } else if (args.length == 3 && "autosell".equalsIgnoreCase(args[0]) && !("list".equalsIgnoreCase(args[1]) || "clear".equalsIgnoreCase(args[1]))) {
            StringUtil.copyPartialMatches(args[2], MobStorageManager.getConfiguredDrops(), completions);
        } else if (args.length == 4 && "autosell".equalsIgnoreCase(args[0]) && "set".equalsIgnoreCase(args[1])) {
            StringUtil.copyPartialMatches(args[3], Arrays.asList("on", "off"), completions);
        } else if (args.length == 2 && "view".equalsIgnoreCase(args[0])) {
            StringUtil.copyPartialMatches(args[1], getOnlinePlayerNames(), completions);
        } else if (args.length == 2 && "transfer".equalsIgnoreCase(args[0])) {
            List<String> suggestions = new ArrayList<>();
            suggestions.add("log");
            suggestions.add("multi");
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (sender instanceof Player && !p.getName().equalsIgnoreCase(sender.getName())) {
                    suggestions.add(p.getName());
                }
            }
            StringUtil.copyPartialMatches(args[1], suggestions, completions);
        } else if (args.length == 3 && "transfer".equalsIgnoreCase(args[0])) {
            if ("log".equalsIgnoreCase(args[1])) {
                List<String> suggestions = new ArrayList<>();
                if (sender.hasPermission("storage.mobstorage.transfer.log.others")) {
                    suggestions.addAll(getOnlinePlayerNames());
                }
                suggestions.add("1");
                suggestions.add("2");
                suggestions.add("3");
                StringUtil.copyPartialMatches(args[2], suggestions, completions);
            } else if ("multi".equalsIgnoreCase(args[1])) {
                StringUtil.copyPartialMatches(args[2], getOnlinePlayerNames(), completions);
            } else {
                StringUtil.copyPartialMatches(args[2], MobStorageManager.getConfiguredDrops(), completions);
            }
        } else if (args.length == 4 && "transfer".equalsIgnoreCase(args[0])) {
            if ("log".equalsIgnoreCase(args[1])) {
                StringUtil.copyPartialMatches(args[3], Arrays.asList("1", "2", "3"), completions);
            } else if (!"multi".equalsIgnoreCase(args[1])) {
                StringUtil.copyPartialMatches(args[3], Arrays.asList("all", "1", "10", "64", "100"), completions);
            }
        } else if (args.length == 3 && ("deposit".equalsIgnoreCase(args[0]) || "withdraw".equalsIgnoreCase(args[0]) || "sell".equalsIgnoreCase(args[0]))) {
            StringUtil.copyPartialMatches(args[2], Arrays.asList("all", "1", "10", "64", "100"), completions);
        } else if (args.length == 3 && "admin".equalsIgnoreCase(args[0]) && ADMIN_COMMANDS.contains(args[1].toLowerCase(Locale.ENGLISH))) {
            StringUtil.copyPartialMatches(args[2], getOnlinePlayerNames(), completions);
        } else if (args.length == 4 && "admin".equalsIgnoreCase(args[0]) && !"max".equalsIgnoreCase(args[1])) {
            StringUtil.copyPartialMatches(args[3], MobStorageManager.getConfiguredDrops(), completions);
        }
        Collections.sort(completions);
        return completions;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        handleCommand(sender, args);
    }

    @Override
    public List<String> getTabCompletions(CommandSender sender, String[] args) {
        return getCommandTabCompletions(sender, args);
    }

    @Override
    public String getPermission() {
        return null;
    }

    @Override
    public String getUsage() {
        return "/mobstorage [command]";
    }

    @Override
    public String getDescription() {
        return "MobStorage command manager";
    }
}
