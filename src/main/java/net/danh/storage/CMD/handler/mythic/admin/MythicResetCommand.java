package net.danh.storage.CMD.handler.mythic.admin;

import net.danh.storage.CMD.handler.mythic.MythicCommand;
import net.danh.storage.Manager.MythicStorageManager;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;

import java.util.ArrayList;
import java.util.List;

public class MythicResetCommand extends MythicCommand {

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (args.length < 1) {
            sendUsage(sender);
            return;
        }

        Player target = getPlayer(args[0]);
        if (target == null) {
            sendPlayerNotFound(sender, args[0]);
            return;
        }

        if (args.length >= 2) {
            // Reset specific item
            String itemName = args[1];
            if (!MythicStorageManager.isConfiguredDrop(itemName)) {
                sendInvalidItem(sender, itemName);
                return;
            }

            MythicStorageManager.setItemAmount(target, itemName, 0);

            String[] placeholders = {"#item#", "#player#"};
            String[] replacements = {itemName, target.getName()};
            sendMessage(sender, "admin.reset_item_success", placeholders, replacements);

            if (target.isOnline()) {
                String[] notifyPlaceholders = {"#item#", "#player#"};
                String[] notifyReplacements = {itemName, sender.getName()};
                sendMessage(target, "admin.reset_item_notify", notifyPlaceholders, notifyReplacements);
            }
        } else {
            // Reset all items
            for (String itemName : MythicStorageManager.getConfiguredDrops()) {
                MythicStorageManager.setItemAmount(target, itemName, 0);
            }

            sendMessage(sender, "admin.reset_all_success", "#player#", target.getName());

            if (target.isOnline()) {
                sendMessage(target, "admin.reset_notify", "#player#", sender.getName());
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
        }

        return completions;
    }

    @Override
    public String getPermission() {
        return "storage.mythicstorage.admin";
    }

    @Override
    public String getUsage() {
        return "/mythicstorage reset <player> [item]";
    }

    @Override
    public String getDescription() {
        return "Reset MythicMobs item amount in player's storage";
    }
}
