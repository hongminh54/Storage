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
                // Single item transfer: /cropstorage transfer <item> <player> [amount]
                handleSingleTransfer(player, args);
                break;
        }
    }

    private void handleSingleTransfer(Player player, String[] args) {
        if (args.length < 2) {
            sendUsage(player);
            return;
        }

        String itemName = args[0].toUpperCase();
        if (!CropStorageManager.isConfiguredDrop(itemName)) {
            sendInvalidItem(player, itemName);
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
            targetPlayer = args[1];
        }

        if (args.length >= 3) {
            page = (int) Number.getLong(args[2]);
            if (page < 1) page = 1;
        }

        CropTransferManager.displayTransferHistory(player, targetPlayer, page);
    }

    @Override
    public List<String> getTabCompletions(CommandSender sender, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            // Suggest subcommands and items
            List<String> suggestions = new ArrayList<>(getConfiguredDrops());
            suggestions.add("log");
            suggestions.add("multi");
            StringUtil.copyPartialMatches(args[0], suggestions, completions);
            return completions;
        }

        if (args.length == 2) {
            String firstArg = args[0].toLowerCase();

            if (firstArg.equals("log")) {
                // Suggest player names for log
                List<String> playerNames = new ArrayList<>();
                for (Player p : Bukkit.getOnlinePlayers()) {
                    playerNames.add(p.getName());
                }
                StringUtil.copyPartialMatches(args[1], playerNames, completions);
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

            // Single item transfer - suggest player names
            if (CropStorageManager.isConfiguredDrop(firstArg.toUpperCase())) {
                List<String> playerNames = new ArrayList<>();
                for (Player p : Bukkit.getOnlinePlayers()) {
                    if (sender instanceof Player && !p.getName().equalsIgnoreCase(sender.getName())) {
                        playerNames.add(p.getName());
                    }
                }
                StringUtil.copyPartialMatches(args[1], playerNames, completions);
                return completions;
            }
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
            if (CropStorageManager.isConfiguredDrop(firstArg.toUpperCase())) {
                if (sender instanceof Player) {
                    Player player = (Player) sender;
                    String itemName = firstArg.toUpperCase();
                    int currentAmount = CropStorageManager.getPlayerItem(player, itemName);

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
                return completions;
            }
        }

        return completions;
    }

    @Override
    public String getPermission() {
        return "storage.cropstorage.transfer.use";
    }

    @Override
    public String getUsage() {
        return "/cropstorage transfer <item> <player> [amount/all]\n" +
                "/cropstorage transfer multi <player>\n" +
                "/cropstorage transfer log [player] [page]";
    }

    @Override
    public String getDescription() {
        return "Transfer crop items to another player";
    }
}
