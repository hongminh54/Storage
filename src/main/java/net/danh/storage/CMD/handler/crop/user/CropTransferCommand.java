package net.danh.storage.CMD.handler.crop.user;

import net.danh.storage.CMD.handler.crop.CropCommand;
import net.danh.storage.GUI.Crop.CropTransferGUI;
import net.danh.storage.GUI.Crop.CropTransferMultiGUI;
import net.danh.storage.Manager.Crop.CropStorageManager;
import net.danh.storage.Manager.Crop.CropTransferManager;
import net.danh.storage.Utils.ChatUtils;
import net.danh.storage.Utils.File;
import net.danh.storage.Utils.Number;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;

import java.util.ArrayList;
import java.util.List;

public class CropTransferCommand extends CropCommand {

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!requirePlayer(sender)) {
            return;
        }

        Player player = (Player) sender;

        if (isCropStorageWorldBlacklisted(player)) {
            sendWorldBlacklisted(sender, "CropStorage", player.getWorld().getName());
            return;
        }

        // Check if transfer is enabled
        if (!File.getCropStorageConfig().getBoolean("transfer.enabled", true)) {
            sender.sendMessage(ChatUtils.colorize(File.getMessage().getString("cropstorage.transfer.disabled")));
            return;
        }

        // No args - show usage
        if (args.length == 0) {
            sendUsage(sender);
            return;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "log":
                handleLogCommand(player, args);
                break;
            case "multi":
                handleMultiTransfer(player, args);
                break;
            default:
                // Single item transfer: /cropstorage transfer <player> <item> [amount]
                handleSingleTransfer(player, args);
                break;
        }
    }

    private void handleSingleTransfer(Player player, String[] args) {
        if (args.length < 2) {
            sendUsage(player);
            return;
        }

        String targetPlayer = args[0];
        Player receiver = Bukkit.getPlayer(targetPlayer);
        if (receiver == null || !receiver.isOnline()) {
            player.sendMessage(ChatUtils.colorize(File.getMessage().getString("cropstorage.transfer.failed_offline")
                    .replace("#player#", targetPlayer)));
            return;
        }

        if (player.getName().equalsIgnoreCase(receiver.getName())) {
            player.sendMessage(ChatUtils.colorize(File.getMessage().getString("cropstorage.transfer.failed_same_player")));
            return;
        }

        String itemName = args[1].toUpperCase();
        if (!CropStorageManager.isConfiguredDrop(itemName)) {
            sendInvalidItem(player, itemName);
            return;
        }

        // Check if player has the item
        int currentAmount = CropStorageManager.getPlayerItem(player, itemName);
        if (currentAmount <= 0) {
            player.sendMessage(ChatUtils.colorize(File.getMessage().getString("cropstorage.transfer.failed_insufficient")
                    .replace("#item#", CropStorageManager.getItemDisplayName(itemName))
                    .replace("#current#", "0")));
            return;
        }

        // No amount specified - open GUI
        if (args.length == 2) {
            player.openInventory(new CropTransferGUI(player, targetPlayer, itemName).getInventory());
            return;
        }

        // Amount specified
        int amount;
        if ("all".equalsIgnoreCase(args[2])) {
            amount = currentAmount;
        } else {
            amount = (int) Number.getLong(args[2]);
            if (amount <= 0) {
                sendInvalidNumber(player, args[2]);
                return;
            }
        }

        CropTransferManager.executeTransfer(player, targetPlayer, itemName, amount);
    }

    private void handleMultiTransfer(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(ChatUtils.colorize(File.getMessage().getString("cropstorage.transfer.usage_multi")));
            return;
        }

        String targetPlayer = args[1];
        Player receiver = Bukkit.getPlayer(targetPlayer);
        if (receiver == null || !receiver.isOnline()) {
            player.sendMessage(ChatUtils.colorize(File.getMessage().getString("cropstorage.transfer.failed_offline")
                    .replace("#player#", targetPlayer)));
            return;
        }

        if (player.getName().equalsIgnoreCase(receiver.getName())) {
            player.sendMessage(ChatUtils.colorize(File.getMessage().getString("cropstorage.transfer.failed_same_player")));
            return;
        }

        // Open multi-transfer GUI
        player.openInventory(new CropTransferMultiGUI(player, targetPlayer).getInventory());
    }

    private void handleLogCommand(Player player, String[] args) {
        String targetPlayer = null;
        int page = 1;

        if (args.length >= 2) {
            if (args[1] != null && args[1].matches("\\d+")) {
                page = (int) Number.getLong(args[1]);
                if (page < 1) {
                    page = 1;
                }
            } else {
                targetPlayer = args[1];
            }
        }

        if (args.length >= 3) {
            if (args[2] != null && args[2].matches("\\d+")) {
                page = (int) Number.getLong(args[2]);
                if (page < 1) {
                    page = 1;
                }
            } else {
                page = 1;
            }
        }

        CropTransferManager.displayTransferHistory(player, targetPlayer, page);
    }

    @Override
    public List<String> getTabCompletions(CommandSender sender, String[] args) {
        List<String> completions = new ArrayList<>();

        boolean isPlayer = sender instanceof Player;
        String senderName = isPlayer ? sender.getName() : "";

        if (args.length == 1) {
            // Suggest subcommands and players
            List<String> suggestions = new ArrayList<>();
            suggestions.add("log");
            suggestions.add("multi");

            for (Player p : Bukkit.getOnlinePlayers()) {
                if (!p.getName().equalsIgnoreCase(senderName)) {
                    suggestions.add(p.getName());
                }
            }

            StringUtil.copyPartialMatches(args[0], suggestions, completions);
            return completions;
        }

        if (args.length == 2) {
            String firstArg = args[0].toLowerCase();

            if (firstArg.equals("log")) {
                // Suggest player names (if allowed) and page numbers
                List<String> suggestions = new ArrayList<>();
                if (checkPermission(sender, "storage.cropstorage.transfer.log.others")) {
                    for (Player p : Bukkit.getOnlinePlayers()) {
                        suggestions.add(p.getName());
                    }
                }
                suggestions.add("1");
                suggestions.add("2");
                suggestions.add("3");
                StringUtil.copyPartialMatches(args[1], suggestions, completions);
                return completions;
            }

            if (firstArg.equals("multi")) {
                // Suggest player names for multi transfer
                List<String> playerNames = new ArrayList<>();
                for (Player p : Bukkit.getOnlinePlayers()) {
                    if (sender instanceof Player && !p.getName().equalsIgnoreCase(sender.getName())) {
                        playerNames.add(p.getName());
                    }
                }
                StringUtil.copyPartialMatches(args[1], playerNames, completions);
                return completions;
            }

            // Single item transfer - suggest items
            Player target = Bukkit.getPlayer(args[0]);
            if (target != null && !target.getName().equalsIgnoreCase(senderName)) {
                List<String> items = new ArrayList<>(getConfiguredDrops());
                StringUtil.copyPartialMatches(args[1], items, completions);
            }
            return completions;
        }

        if (args.length == 3) {
            String firstArg = args[0].toLowerCase();

            if (firstArg.equals("log")) {
                // Suggest page numbers
                completions.add("1");
                completions.add("2");
                completions.add("3");
                return completions;
            }

            // Single item transfer - suggest amounts
            if (sender instanceof Player p) {
                Player target = Bukkit.getPlayer(args[0]);
                String itemName = args[1].toUpperCase();

                if (target != null && !target.getName().equalsIgnoreCase(p.getName()) && CropStorageManager.isConfiguredDrop(itemName)) {
                    int currentAmount = CropStorageManager.getPlayerItem(p, itemName);

                    List<String> suggestions = new ArrayList<>();
                    suggestions.add("all");
                    suggestions.add("1");
                    if (currentAmount >= 10) {
                        suggestions.add("10");
                    }
                    if (currentAmount >= 64) {
                        suggestions.add("64");
                    }
                    if (currentAmount > 0) {
                        suggestions.add(String.valueOf(currentAmount));
                    }

                    StringUtil.copyPartialMatches(args[2], suggestions, completions);
                }
            }

            return completions;
        }

        return completions;
    }

    @Override
    public String getPermission() {
        return "storage.cropstorage.transfer.use";
    }

    @Override
    public String getUsage() {
        return "/cropstorage transfer <player> <item> [amount/all]\n" +
                "/cropstorage transfer multi <player>\n" +
                "/cropstorage transfer log [player] [page]";
    }

    @Override
    public String getDescription() {
        return "Transfer crop items to another player";
    }
}
