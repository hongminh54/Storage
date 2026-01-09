package net.danh.storage.CMD.handler.mythic.admin;

import net.danh.storage.CMD.handler.mythic.MythicCommand;
import net.danh.storage.Manager.Mythic.MythicStorageManager;
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

        int amount = Number.getInteger(args[2]);
        if (amount <= 0) {
            if (Number.getInteger(args[2]) == -1) {
                sendInvalidNumber(sender, args[2]);
            } else {
                sendNumberTooLow(sender);
            }
            return;
        }

        List<String> items = parseItems(args[1]);
        if (items.isEmpty()) {
            sendInvalidItem(sender, args[1]);
            return;
        }

        for (String itemName : items) {
            MythicStorageManager.removeItemAmount(target, itemName, amount);
        }

        String itemDisplay = items.size() == 1 ? getItemDisplayName(items.get(0)) :
                (items.size() == getConfiguredDrops().size() ? "*" : formatItemList(items));

        String[] placeholders = {"#amount#", "#item#", "#player#"};
        String[] replacements = {String.valueOf(amount), itemDisplay, target.getName()};
        sendMessage(sender, "admin.remove_success", placeholders, replacements);

        if (target.isOnline()) {
            String[] notifyPlaceholders = {"#amount#", "#item#", "#player#"};
            String[] notifyReplacements = {String.valueOf(amount), itemDisplay, sender.getName()};
            sendMessage(target, "admin.remove_notify", notifyPlaceholders, notifyReplacements);
        }
    }

    private List<String> parseItems(String input) {
        List<String> result = new ArrayList<>();
        List<String> configuredDrops = getConfiguredDrops();
        if (input.equals("*")) {
            result.addAll(configuredDrops);
        } else if (input.contains(",")) {
            for (String item : input.split(",")) {
                String trimmed = item.trim();
                if (configuredDrops.contains(trimmed)) {
                    result.add(trimmed);
                }
            }
        } else {
            if (configuredDrops.contains(input)) {
                result.add(input);
            }
        }
        return result;
    }

    private String formatItemList(List<String> items) {
        List<String> displayNames = new ArrayList<>();
        for (String item : items) {
            displayNames.add(getItemDisplayName(item));
        }
        return String.join(", ", displayNames);
    }

    @Override
    public List<String> getTabCompletions(CommandSender sender, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            StringUtil.copyPartialMatches(args[0], getOnlinePlayerNames(), completions);
        } else if (args.length == 2) {
            List<String> suggestions = new ArrayList<>();
            suggestions.add("*");
            suggestions.addAll(getConfiguredDrops());
            StringUtil.copyPartialMatches(args[1], suggestions, completions);
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
        return "/mythicstorage remove <player> <item|*|item1,item2,...> <amount>";
    }

    @Override
    public String getDescription() {
        return "Remove MythicMobs item amount from player's storage";
    }
}
