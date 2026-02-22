package net.danh.storage.CMD.handler.mythic.user;

import net.danh.storage.Action.MythicWithdraw;
import net.danh.storage.CMD.handler.mythic.MythicCommand;
import net.danh.storage.Manager.Mythic.MythicStorageManager;
import net.danh.storage.Utils.Number;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MythicWithdrawCommand extends MythicCommand {

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!requirePlayer(sender)) {
            return;
        }

        Player player = (Player) sender;

        if (isMythicStorageWorldBlacklisted(player)) {
            sendWorldBlacklisted(sender, "MythicStorage", player.getWorld().getName());
            return;
        }

        if (args.length < 1 || args.length > 2) {
            sendUsage(sender);
            return;
        }

        String itemName = args[0];
        if (!MythicStorageManager.isConfiguredDrop(itemName)) {
            sendInvalidItem(sender, itemName);
            return;
        }

        long amount;
        if (args.length == 1) {
            amount = Integer.MAX_VALUE;
        } else {
            if ("all".equalsIgnoreCase(args[1])) {
                amount = Integer.MAX_VALUE;
            } else {
                amount = Number.getLong(args[1]);
                if (amount <= 0) {
                    sendInvalidNumber(sender, args[1]);
                    return;
                }
            }
        }

        new MythicWithdraw(player, itemName, amount).doAction();
    }

    @Override
    public List<String> getTabCompletions(CommandSender sender, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            StringUtil.copyPartialMatches(args[0], getConfiguredDrops(), completions);
            return completions;
        }

        if (args.length == 2) {
            List<String> suggestions = Arrays.asList("all", "1", "10", "64", "100", "1000");
            StringUtil.copyPartialMatches(args[1], suggestions, completions);
            return completions;
        }

        return completions;
    }

    @Override
    public String getPermission() {
        return "storage.mythicstorage.use";
    }

    @Override
    public String getUsage() {
        return "/mythicstorage withdraw <item> [amount|all]";
    }

    @Override
    public String getDescription() {
        return "Withdraw MythicMobs items from MythicStorage into inventory";
    }
}
