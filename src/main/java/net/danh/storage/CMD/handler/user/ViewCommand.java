package net.danh.storage.CMD.handler.user;

import net.danh.storage.CMD.handler.BaseCommand;
import net.danh.storage.GUI.ViewStorageGUI;
import net.danh.storage.Manager.MineManager;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;

import java.util.ArrayList;
import java.util.List;

public class ViewCommand extends BaseCommand {

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!requirePlayer(sender)) {
            return;
        }

        Player player = (Player) sender;

        if (args.length != 1) {
            sendUsage(sender);
            return;
        }

        String targetPlayerName = args[0];
        Player targetPlayer = getPlayer(targetPlayerName);

        if (targetPlayer == null) {
            OfflinePlayer offlineTarget = Bukkit.getOfflinePlayer(targetPlayerName);

            if (!offlineTarget.hasPlayedBefore()) {
                sendInvalidPlayer(sender, targetPlayerName);
                return;
            }

            if (!MineManager.loadOfflinePlayerData(offlineTarget.getName())) {
                sendMessage(sender, "view.no_data", "#player#", offlineTarget.getName());
                return;
            }

            try {
                player.openInventory(new ViewStorageGUI(player, offlineTarget.getName()).getInventory());
                sendMessage(sender, "view.viewing_storage", "#player#", offlineTarget.getName());
            } catch (IndexOutOfBoundsException e) {
                sendMessage(sender, "admin.not_enough_slot");
            }
            return;
        }

        try {
            player.openInventory(new ViewStorageGUI(player, targetPlayer).getInventory());
            sendMessage(sender, "view.viewing_storage", "#player#", targetPlayer.getName());
        } catch (IndexOutOfBoundsException e) {
            sendMessage(sender, "admin.not_enough_slot");
        }
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
        return "storage.view";
    }

    @Override
    public String getUsage() {
        return "/storage view <player>";
    }

    @Override
    public String getDescription() {
        return "View another player's storage";
    }
}
