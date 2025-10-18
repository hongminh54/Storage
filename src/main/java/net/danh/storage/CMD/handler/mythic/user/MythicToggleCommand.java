package net.danh.storage.CMD.handler.mythic.user;

import net.danh.storage.CMD.handler.mythic.MythicCommand;
import net.danh.storage.Manager.MythicStorageManager;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class MythicToggleCommand extends MythicCommand {

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!requirePlayer(sender)) {
            return;
        }

        Player player = (Player) sender;

        if (isMythicStorageWorldBlacklisted(player)) {
            sendWorldBlacklisted(sender, "MythicStorage", player.getWorld().getName());
            return;
        }

        boolean currentStatus = MythicStorageManager.getToggleStatus(player);
        boolean newStatus = !currentStatus;

        MythicStorageManager.setToggleStatus(player, newStatus);

        String messageKey = newStatus ? "toggle_enabled" : "toggle_disabled";
        sendMessage(sender, messageKey);
    }

    @Override
    public List<String> getTabCompletions(CommandSender sender, String[] args) {
        return new ArrayList<>();
    }

    @Override
    public String getPermission() {
        return "storage.mythicstorage.toggle";
    }

    @Override
    public String getUsage() {
        return "/mythicstorage toggle";
    }

    @Override
    public String getDescription() {
        return "Toggle MythicMobs auto-pickup";
    }
}
