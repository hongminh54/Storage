package net.danh.storage.CMD.handler.admin;

import net.danh.storage.CMD.handler.BaseCommand;
import net.danh.storage.Manager.MineManager;
import net.danh.storage.Utils.Number;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MaxCommand extends BaseCommand {

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (args.length != 2) {
            return;
        }

        Player target = getPlayer(args[0]);
        if (target == null) {
            return;
        }

        int amount = Number.getInteger(args[1]);
        if (amount <= 0) {
            return;
        }

        MineManager.playermaxdata.put(target, amount);

        String[] placeholders = {"#player#", "#amount#"};
        String[] replacements = {target.getName(), String.valueOf(amount)};
        sendMessage(sender, "admin.set_max_storage", placeholders, replacements);
    }

    @Override
    public List<String> getTabCompletions(CommandSender sender, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            List<String> playerNames = getOnlinePlayerNames();
            StringUtil.copyPartialMatches(args[0], playerNames, completions);
        }

        if (args.length == 2) {
            StringUtil.copyPartialMatches(args[1], Collections.singleton("<number>"), completions);
        }

        return completions;
    }

    @Override
    public String getPermission() {
        return "storage.admin.max";
    }

    @Override
    public String getUsage() {
        return "/storage max <player> <amount>";
    }

    @Override
    public String getDescription() {
        return "Set max storage for a player";
    }
}
