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

public class MythicAddCommand extends MythicCommand {

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

        if (MythicStorageManager.addItemAmount(target, itemName, amount)) {
            String displayName = getItemDisplayName(itemName);
            String[] placeholders = {"#amount#", "#item#", "#player#"};
            String[] replacements = {String.valueOf(amount), displayName, target.getName()};
            sendMessage(sender, "admin.add_success", placeholders, replacements);

            if (target.isOnline()) {
                String[] notifyPlaceholders = {"#amount#", "#item#", "#player#"};
                String[] notifyReplacements = {String.valueOf(amount), displayName, sender.getName()};
                sendMessage(target, "admin.add_notify", notifyPlaceholders, notifyReplacements);
            }
        } else {
            sendMessage(sender, "admin.storage_full", "#player#", target.getName());
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
        return "/mythicstorage add <player> <item> <amount>";
    }

    @Override
    public String getDescription() {
        return "Add MythicMobs item amount to player's storage";
    }
}
