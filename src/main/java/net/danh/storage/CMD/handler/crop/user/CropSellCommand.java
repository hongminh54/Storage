package net.danh.storage.CMD.handler.crop.user;

import net.danh.storage.Action.CropSell;
import net.danh.storage.CMD.handler.crop.CropCommand;
import net.danh.storage.Manager.Crop.CropStorageManager;
import net.danh.storage.Utils.Number;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CropSellCommand extends CropCommand {

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

        if (args.length < 1 || args.length > 2) {
            sendUsage(sender);
            return;
        }

        String itemName = args[0].toUpperCase();
        if (!CropStorageManager.isConfiguredDrop(itemName)) {
            sendInvalidItem(sender, itemName);
            return;
        }

        long amount;
        if (args.length == 1 || "all".equalsIgnoreCase(args[1])) {
            amount = -1;
        } else {
            amount = Number.getLong(args[1]);
            if (amount <= 0) {
                sendInvalidNumber(sender, args[1]);
                return;
            }
        }

        new CropSell(player, itemName, amount).doAction();
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
        return "storage.cropstorage.use";
    }

    @Override
    public String getUsage() {
        return "/cropstorage sell <item> [amount|all]";
    }

    @Override
    public String getDescription() {
        return "Sell crop items from CropStorage";
    }
}
