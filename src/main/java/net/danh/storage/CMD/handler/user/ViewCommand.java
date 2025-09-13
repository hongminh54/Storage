package net.danh.storage.CMD.handler.user;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;

import net.danh.storage.CMD.handler.BaseCommand;
import net.danh.storage.GUI.ViewStorageGUI;

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
            sendInvalidPlayer(sender, targetPlayerName);
            return;
        }

        // Open ViewStorageGUI for target player
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
