package net.danh.storage.CMD.handler.user;

import net.danh.storage.Action.Withdraw;
import net.danh.storage.CMD.handler.BaseCommand;
import net.danh.storage.Manager.MineManager;
import net.danh.storage.Utils.Number;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class WithdrawCommand extends BaseCommand {

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!requirePlayer(sender)) {
            return;
        }

        Player player = (Player) sender;

        if (isStorageWorldBlacklisted(player)) {
            sendWorldBlacklisted(sender, "Storage", player.getWorld().getName());
            return;
        }

        if (args.length < 1 || args.length > 2) {
            sendUsage(sender);
            return;
        }

        String material = resolveMaterial(args[0]);
        if (!MineManager.getPluginBlocks().contains(material)) {
            sendInvalidMaterial(sender, material, MineManager.getPluginBlocks());
            return;
        }

        int amount;
        if (args.length == 1) {
            amount = 0;
        } else {
            if ("all".equalsIgnoreCase(args[1])) {
                amount = 0;
            } else {
                long parsed = Number.getLong(args[1]);
                if (parsed <= 0) {
                    sendInvalidNumber(sender, args[1]);
                    return;
                }
                amount = parsed > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) parsed;
            }
        }

        new Withdraw(player, material, amount).doAction();
    }

    private String resolveMaterial(String raw) {
        if (raw == null) {
            return ";0";
        }
        String material = raw.replace(":", ";");
        if (!material.contains(";")) {
            return material + ";0";
        }
        return material;
    }

    @Override
    public List<String> getTabCompletions(CommandSender sender, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            StringUtil.copyPartialMatches(args[0], MineManager.getPluginBlocks(), completions);
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
        return "storage.withdraw";
    }

    @Override
    public String getUsage() {
        return "/storage withdraw <item> [amount|all]";
    }

    @Override
    public String getDescription() {
        return "Withdraw items from Storage into inventory";
    }
}
