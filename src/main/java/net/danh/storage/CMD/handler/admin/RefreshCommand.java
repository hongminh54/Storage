package net.danh.storage.CMD.handler.admin;

import net.danh.storage.CMD.handler.BaseCommand;
import net.danh.storage.Utils.PermissionStorageLimit;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;

import java.util.ArrayList;
import java.util.List;

public class RefreshCommand extends BaseCommand {

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (args.length == 0) {
            // Refresh all online players
            refreshAllPlayers(sender);
        } else if (args.length == 1) {
            // Refresh specific player
            refreshSpecificPlayer(sender, args[0]);
        } else {
            sendUsage(sender);
        }
    }

    private void refreshAllPlayers(CommandSender sender) {
        int refreshedCount = 0;

        for (Player player : Bukkit.getOnlinePlayers()) {
            PermissionStorageLimit.refreshPlayerStorageLimit(player);
            refreshedCount++;
        }

        String[] placeholders = {"#count#"};
        String[] replacements = {String.valueOf(refreshedCount)};
        sendMessage(sender, "admin.refresh_all_success", placeholders, replacements);
    }

    private void refreshSpecificPlayer(CommandSender sender, String playerName) {
        Player target = getPlayer(playerName);
        if (target == null) {
            sendInvalidPlayer(sender, playerName);
            return;
        }

        int oldLimit = net.danh.storage.Manager.MineManager.getMaxBlock(target);
        PermissionStorageLimit.refreshPlayerStorageLimit(target);
        int newLimit = net.danh.storage.Manager.MineManager.getMaxBlock(target);

        String[] placeholders = {"#player#", "#old_limit#", "#new_limit#"};
        String[] replacements = {target.getName(), String.valueOf(oldLimit), String.valueOf(newLimit)};
        sendMessage(sender, "admin.refresh_player_success", placeholders, replacements);
    }

    @Override
    public List<String> getTabCompletions(CommandSender sender, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            List<String> playerNames = getOnlinePlayerNames();
            StringUtil.copyPartialMatches(args[0], playerNames, completions);
        }

        return completions;
    }

    @Override
    public String getPermission() {
        return "storage.admin.refresh";
    }

    @Override
    public String getUsage() {
        return "/storage refresh [player]";
    }

    @Override
    public String getDescription() {
        return "Refresh storage permissions for player(s)";
    }
}
