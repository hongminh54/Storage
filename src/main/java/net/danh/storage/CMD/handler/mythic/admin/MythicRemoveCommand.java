package net.danh.storage.CMD.handler.mythic.admin;

import net.danh.storage.CMD.handler.mythic.MythicCommand;
import net.danh.storage.Manager.MythicStorageManager;
import net.danh.storage.Utils.Number;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MythicRemoveCommand extends MythicCommand {

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (args.length != 3) {
            sendUsage(sender);
            return;
        }

        Player target = getPlayer(args[0]);
        if (target == null) {
            sendPlayerNotFound(sender, args[0]);
            return;
        }

        String itemName = args[1];
        if (!MythicStorageManager.isConfiguredDrop(itemName)) {
            sendInvalidItem(sender, itemName);
            return;
        }

        int amount = Number.getInteger(args[2]);
        if (amount <= 0) {
            if (Number.getInteger(args[2]) == -1) {
                sendInvalidNumber(sender, args[2]);
            } else {
                sendNumberTooLow(sender);
            }
            return;
        }

        int currentAmount = MythicStorageManager.getPlayerItem(target, itemName);
        if (currentAmount < amount) {
            String[] placeholders = {"#player#", "#item#"};
            String[] replacements = {target.getName(), itemName};
            sendMessage(sender, "admin.not_enough_items", placeholders, replacements);
            return;
        }

        if (MythicStorageManager.removeItemAmount(target, itemName, amount)) {
            String[] placeholders = {"#amount#", "#item#", "#player#"};
            String[] replacements = {String.valueOf(amount), itemName, target.getName()};
            sendMessage(sender, "admin.remove_success", placeholders, replacements);

            if (target.isOnline()) {
                String[] notifyPlaceholders = {"#amount#", "#item#", "#player#"};
                String[] notifyReplacements = {String.valueOf(amount), itemName, sender.getName()};
                sendMessage(target, "admin.remove_notify", notifyPlaceholders, notifyReplacements);
            }
        }
    }

    @Override
    public List<String> getTabCompletions(CommandSender sender, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            StringUtil.copyPartialMatches(args[0], getOnlinePlayerNames(), completions);
        } else if (args.length == 2) {
            StringUtil.copyPartialMatches(args[1], getConfiguredDrops(), completions);
        } else if (args.length == 3) {
            completions.addAll(Arrays.asList("1", "10", "64", "100"));
        }

        return completions;
    }

    @Override
    public String getPermission() {
        return "storage.mythicstorage.admin";
    }

    @Override
    public String getUsage() {
        return "/mythicstorage remove <player> <item> <amount>";
    }

    @Override
    public String getDescription() {
        return "Remove MythicMobs item amount from player's storage";
    }
}
