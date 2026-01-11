package net.danh.storage.CMD.handler.mythic.admin;

import net.danh.storage.CMD.handler.mythic.MythicCommand;
import net.danh.storage.Manager.Mythic.MythicStorageManager;
import net.danh.storage.Utils.Number;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MythicMaxCommand extends MythicCommand {

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (args.length != 2) {
            sendUsage(sender);
            return;
        }

        Player target = getPlayer(args[0]);
        if (target == null) {
            sendPlayerNotFound(sender, args[0]);
            return;
        }

        int amount = Number.getInteger(args[1]);
        if (amount <= 0) {
            if (Number.getInteger(args[1]) == -1) {
                sendInvalidNumber(sender, args[1]);
            } else {
                sendNumberTooLow(sender);
            }
            return;
        }

        MythicStorageManager.setMaxStorageOverride(target, amount);
        MythicStorageManager.playermaxdata.put(target.getUniqueId(), amount);
        MythicStorageManager.savePlayerData(target);

        sendMessage(sender, "admin.set_max_storage", new String[]{"#player#",
                "#amount#"}, new String[]{target.getName(), String.valueOf(amount)});
    }

    @Override
    public List<String> getTabCompletions(CommandSender sender, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            StringUtil.copyPartialMatches(args[0], getOnlinePlayerNames(), completions);
        }

        if (args.length == 2) {
            StringUtil.copyPartialMatches(args[1], Collections.singleton("<number>"), completions);
        }

        return completions;
    }

    @Override
    public String getPermission() {
        return "storage.mythicstorage.admin";
    }

    @Override
    public String getUsage() {
        return "/mythicstorage max <player> <amount>";
    }

    @Override
    public String getDescription() {
        return "Set max MythicStorage for a player";
    }
}
