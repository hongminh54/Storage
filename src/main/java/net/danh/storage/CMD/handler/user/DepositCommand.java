package net.danh.storage.CMD.handler.user;

import net.danh.storage.Action.Deposit;
import net.danh.storage.CMD.handler.BaseCommand;
import net.danh.storage.Manager.MineManager;
import net.danh.storage.Utils.Number;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;

import java.util.ArrayList;
import java.util.List;

public class DepositCommand extends BaseCommand {

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

        long amount;
        if (args.length == 1) {
            amount = 0;
        } else {
            if ("all".equalsIgnoreCase(args[1])) {
                amount = 0;
            } else {
                amount = Number.getLong(args[1]);
                if (amount <= 0) {
                    sendInvalidNumber(sender, args[1]);
                    return;
                }
            }
        }

        new Deposit(player, material, amount).doAction();
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
            if (!(sender instanceof Player player)) {
                return completions;
            }

            String material = resolveMaterial(args[0]);
            if (!MineManager.getPluginBlocks().contains(material)) {
                return completions;
            }

            Deposit action = new Deposit(player, material, 1L);
            int count = action.getPlayerAmount();
            if (count <= 0) {
                return completions;
            }

            List<String> suggestions = new ArrayList<>();
            suggestions.add("all");
            suggestions.add("1");
            if (count >= 10) {
                suggestions.add("10");
            }
            if (count >= 64) {
                suggestions.add("64");
            }
            suggestions.add(String.valueOf(count));

            StringUtil.copyPartialMatches(args[1], suggestions, completions);
            return completions;
        }

        return completions;
    }

    @Override
    public String getPermission() {
        return "storage.deposit";
    }

    @Override
    public String getUsage() {
        return "/storage deposit <item> [amount|all]";
    }

    @Override
    public String getDescription() {
        return "Deposit items from inventory into Storage";
    }
}
