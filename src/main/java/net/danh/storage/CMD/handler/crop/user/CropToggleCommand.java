package net.danh.storage.CMD.handler.crop.user;

import net.danh.storage.CMD.handler.crop.CropCommand;
import net.danh.storage.Manager.Crop.CropStorageManager;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class CropToggleCommand extends CropCommand {

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

        boolean currentStatus = CropStorageManager.getToggleStatus(player);
        boolean newStatus = !currentStatus;

        CropStorageManager.setToggleStatus(player, newStatus);
        CropStorageManager.savePlayerData(player);

        String messageKey = newStatus ? "toggle_enabled" : "toggle_disabled";
        sendMessage(sender, messageKey);
    }

    @Override
    public List<String> getTabCompletions(CommandSender sender, String[] args) {
        return new ArrayList<>();
    }

    @Override
    public String getPermission() {
        return "storage.cropstorage.toggle";
    }

    @Override
    public String getUsage() {
        return "/cropstorage toggle";
    }

    @Override
    public String getDescription() {
        return "Toggle crop auto-pickup";
    }
}
