package net.danh.storage.CMD.handler.user;

import net.danh.storage.CMD.handler.BaseCommand;
import net.danh.storage.GUI.TransferGUI;
import net.danh.storage.GUI.TransferMultiGUI;
import net.danh.storage.Manager.MineManager;
import net.danh.storage.Manager.TransferManager;
import net.danh.storage.Utils.Number;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;

import java.util.ArrayList;
import java.util.List;

public class TransferCommand extends BaseCommand {

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!requirePlayer(sender)) {
            return;
        }

        Player player = (Player) sender;

        if (args.length == 0) {
            sendUsage(sender);
            return;
        }

        if (args.length == 1) {
            if (args[0].equalsIgnoreCase("log")) {
                if (checkPermission(sender, "storage.transfer.log")) {
                    TransferManager.displayTransferHistory(player, null, 1);
                }
                return;
            }

            if (args[0].equalsIgnoreCase("multi")) {
                if (checkPermission(sender, "storage.transfer.multi")) {
                    sendMessage(sender, "admin.transfer_commands.error_opening_gui");
                }
                return;
            }

            if (checkPermission(sender, "storage.transfer.use")) {
                Player target = getPlayer(args[0]);
                if (target != null && !target.equals(player)) {
                    sendMessage(sender, "admin.transfer_commands.error_opening_gui");
                }
            }
        }

        if (args.length == 2) {
            if (args[0].equalsIgnoreCase("log")) {
                if (checkPermission(sender, "storage.transfer.log")) {
                    if (isValidNumber(args[1])) {
                        int page = Number.getInteger(args[1]);
                        if (page > 0) {
                            TransferManager.displayTransferHistory(player, null, page);
                        } else {
                            sendNumberTooLow(sender);
                        }
                    } else {
                        sendInvalidNumber(sender, args[1]);
                    }
                }
                return;
            }

            if (args[0].equalsIgnoreCase("multi")) {
                if (checkPermission(sender, "storage.transfer.multi")) {
                    Player target = getPlayer(args[1]);
                    if (target != null && !target.equals(player)) {
                        try {
                            player.openInventory(new TransferMultiGUI(player, target.getName()).getInventory());
                        } catch (Exception e) {
                            sendMessage(sender, "admin.transfer_commands.error_opening_gui");
                        }
                    }
                }
                return;
            }

            if (checkPermission(sender, "storage.transfer.use")) {
                Player target = getPlayer(args[0]);
                if (target != null && !target.equals(player)) {
                    String material = args[1];
                    if (MineManager.getPluginBlocks().contains(material)) {
                        try {
                            player.openInventory(new TransferGUI(player, target.getName(), material).getInventory());
                        } catch (Exception e) {
                            sendMessage(sender, "admin.transfer_commands.error_opening_gui");
                        }
                    }
                }
            }
        }

        if (args.length == 3) {
            if (args[0].equalsIgnoreCase("log")) {
                if (checkPermission(sender, "storage.transfer.log.others")) {
                    String targetName = args[1];
                    if (isValidNumber(args[2])) {
                        int page = Number.getInteger(args[2]);
                        if (page > 0) {
                            TransferManager.displayTransferHistory(player, targetName, page);
                        } else {
                            sendNumberTooLow(sender);
                        }
                    } else {
                        sendInvalidNumber(sender, args[2]);
                    }
                }
            }
        }
    }

    @Override
    public List<String> getTabCompletions(CommandSender sender, String[] args) {
        List<String> completions = new ArrayList<>();
        List<String> commands = new ArrayList<>();

        if (args.length == 1) {
            if (checkPermission(sender, "storage.transfer.log")) {
                commands.add("log");
            }
            if (checkPermission(sender, "storage.transfer.multi")) {
                commands.add("multi");
            }
            if (checkPermission(sender, "storage.transfer.use")) {
                commands.addAll(getOnlinePlayerNamesExcept(sender.getName()));
            }
            StringUtil.copyPartialMatches(args[0], commands, completions);
        }

        if (args.length == 2) {
            if (args[0].equalsIgnoreCase("log")) {
                if (checkPermission(sender, "storage.transfer.log.others")) {
                    commands.addAll(getOnlinePlayerNames());
                }
                commands.add("1");
                commands.add("2");
                commands.add("3");
                StringUtil.copyPartialMatches(args[1], commands, completions);
            } else if (args[0].equalsIgnoreCase("multi") && checkPermission(sender, "storage.transfer.multi")) {
                commands.addAll(getOnlinePlayerNamesExcept(sender.getName()));
                StringUtil.copyPartialMatches(args[1], commands, completions);
            } else if (checkPermission(sender, "storage.transfer.use")) {
                commands.addAll(MineManager.getPluginBlocks());
                StringUtil.copyPartialMatches(args[1], commands, completions);
            }
        }

        if (args.length == 3) {
            if (args[0].equalsIgnoreCase("log") && checkPermission(sender, "storage.transfer.log.others")) {
                commands.add("1");
                commands.add("2");
                commands.add("3");
                StringUtil.copyPartialMatches(args[2], commands, completions);
            }
        }

        return completions;
    }

    @Override
    public String getPermission() {
        return null;
    }

    @Override
    public String getUsage() {
        return "/storage transfer <player|log|multi>";
    }

    @Override
    public String getDescription() {
        return "Transfer materials to other players";
    }
}
