package net.danh.storage.CMD.handler.mythic.user;

import net.danh.storage.CMD.handler.BaseCommand;
import net.danh.storage.GUI.Mythic.MythicTransferGUI;
import net.danh.storage.GUI.Mythic.MythicTransferMultiGUI;
import net.danh.storage.Manager.Mythic.MythicStorageManager;
import net.danh.storage.Manager.Mythic.MythicTransferManager;
import net.danh.storage.Utils.Number;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;

import java.util.ArrayList;
import java.util.List;

public class MythicTransferCommand extends BaseCommand {

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
                if (checkPermission(sender, "storage.mythicstorage.transfer.log")) {
                    MythicTransferManager.displayTransferHistory(player, null, 1);
                }
                return;
            }

            if (args[0].equalsIgnoreCase("multi")) {
                if (checkPermission(sender, "storage.mythicstorage.transfer.multi")) {
                    sendMessage(sender, "mythicstorage.admin.error_opening_gui");
                }
                return;
            }

            // Single player name without item - show usage
            sendUsage(sender);
            return;
        }

        if (args.length == 2) {
            if (args[0].equalsIgnoreCase("log")) {
                if (checkPermission(sender, "storage.mythicstorage.transfer.log")) {
                    if (isValidNumber(args[1])) {
                        int page = Number.getInteger(args[1]);
                        if (page > 0) {
                            MythicTransferManager.displayTransferHistory(player, null, page);
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
                if (checkPermission(sender, "storage.mythicstorage.transfer.multi")) {
                    Player target = getPlayer(args[1]);
                    if (target == null) {
                        sendInvalidPlayer(sender, args[1]);
                        return;
                    }

                    if (target.equals(player)) {
                        sendMessage(sender, "mythicstorage.transfer.failed_same_player");
                        return;
                    }

                    try {
                        player.openInventory(new MythicTransferMultiGUI(player, target.getName()).getInventory());
                    } catch (Exception e) {
                        sendMessage(sender, "mythicstorage.admin.error_opening_gui");
                    }
                }
                return;
            }

            if (checkPermission(sender, "storage.mythicstorage.transfer")) {
                Player target = getPlayer(args[0]);
                if (target == null) {
                    sendInvalidPlayer(sender, args[0]);
                    return;
                }

                if (target.equals(player)) {
                    sendMessage(sender, "mythicstorage.transfer.failed_same_player");
                    return;
                }

                String itemName = args[1];
                if (!MythicStorageManager.isConfiguredDrop(itemName)) {
                    sendMessage(sender, "mythicstorage.invalid_item", "#item#", itemName);
                    return;
                }

                try {
                    player.openInventory(new MythicTransferGUI(player, target.getName(), itemName).getInventory());
                } catch (Exception e) {
                    sendMessage(sender, "mythicstorage.admin.error_opening_gui");
                }
            }
        }

        if (args.length == 3) {
            if (args[0].equalsIgnoreCase("log")) {
                if (checkPermission(sender, "storage.mythicstorage.transfer.log.others")) {
                    String targetName = args[1];
                    if (isValidNumber(args[2])) {
                        int page = Number.getInteger(args[2]);
                        if (page > 0) {
                            MythicTransferManager.displayTransferHistory(player, targetName, page);
                        } else {
                            sendNumberTooLow(sender);
                        }
                    } else {
                        sendInvalidNumber(sender, args[2]);
                    }
                }
                return;
            }

            if (checkPermission(sender, "storage.mythicstorage.transfer")) {
                Player target = getPlayer(args[0]);
                if (target == null) {
                    sendInvalidPlayer(sender, args[0]);
                    return;
                }

                if (target.equals(player)) {
                    sendMessage(sender, "mythicstorage.transfer.failed_same_player");
                    return;
                }

                String itemName = args[1];
                if (!MythicStorageManager.isConfiguredDrop(itemName)) {
                    sendMessage(sender, "mythicstorage.invalid_item", "#item#", itemName);
                    return;
                }

                int currentAmount = MythicStorageManager.getPlayerItem(player, itemName);
                int amount;
                if ("all".equalsIgnoreCase(args[2])) {
                    amount = currentAmount;
                } else {
                    amount = (int) Number.getLong(args[2]);
                    if (amount <= 0) {
                        sendInvalidNumber(sender, args[2]);
                        return;
                    }
                }

                MythicTransferManager.executeTransfer(player, target.getName(), itemName, amount);
            }
        }
    }

    @Override
    public List<String> getTabCompletions(CommandSender sender, String[] args) {
        List<String> completions = new ArrayList<>();
        List<String> commands = new ArrayList<>();

        if (args.length == 1) {
            if (checkPermission(sender, "storage.mythicstorage.transfer.log")) {
                commands.add("log");
            }
            if (checkPermission(sender, "storage.mythicstorage.transfer.multi")) {
                commands.add("multi");
            }
            if (checkPermission(sender, "storage.mythicstorage.transfer")) {
                commands.addAll(getOnlinePlayerNamesExcept(sender.getName()));
            }
            StringUtil.copyPartialMatches(args[0], commands, completions);
        }

        if (args.length == 2) {
            if (args[0].equalsIgnoreCase("log")) {
                if (checkPermission(sender, "storage.mythicstorage.transfer.log.others")) {
                    commands.addAll(getOnlinePlayerNames());
                }
                commands.add("1");
                commands.add("2");
                commands.add("3");
                StringUtil.copyPartialMatches(args[1], commands, completions);
            } else if (args[0].equalsIgnoreCase("multi")) {
                if (checkPermission(sender, "storage.mythicstorage.transfer.multi")) {
                    commands.addAll(getOnlinePlayerNamesExcept(sender.getName()));
                }
                StringUtil.copyPartialMatches(args[1], commands, completions);
            } else if (checkPermission(sender, "storage.mythicstorage.transfer")) {
                commands.addAll(MythicStorageManager.getConfiguredDrops());
                StringUtil.copyPartialMatches(args[1], commands, completions);
            }
        }

        if (args.length == 3) {
            if (args[0].equalsIgnoreCase("log") && checkPermission(sender, "storage.mythicstorage.transfer.log.others")) {
                commands.add("1");
                commands.add("2");
                commands.add("3");
                StringUtil.copyPartialMatches(args[2], commands, completions);
            } else if (checkPermission(sender, "storage.mythicstorage.transfer")) {
                Player target = getPlayer(args[0]);
                String itemName = args[1];
                if (target != null && sender instanceof Player player && MythicStorageManager.isConfiguredDrop(itemName)) {
                    int currentAmount = MythicStorageManager.getPlayerItem(player, itemName);

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
        }

        return completions;
    }

    @Override
    public String getPermission() {
        return null;
    }

    @Override
    public String getUsage() {
        return "/mythicstorage transfer <player> <item> [amount/all]\n" +
                "/mythicstorage transfer multi <player>\n" +
                "/mythicstorage transfer log [player] [page]";
    }

    @Override
    public String getDescription() {
        return "Transfer MythicMobs items to other players";
    }
}
