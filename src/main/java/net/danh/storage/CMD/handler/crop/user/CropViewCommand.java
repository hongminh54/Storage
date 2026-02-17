package net.danh.storage.CMD.handler.crop.user;

import net.danh.storage.CMD.handler.crop.CropCommand;
import net.danh.storage.GUI.Crop.ViewCropStorageGUI;
import net.danh.storage.Manager.Crop.CropStorageManager;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;

import java.util.ArrayList;
import java.util.List;

public class CropViewCommand extends CropCommand {

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!requirePlayer(sender)) {
            return;
        }

        Player viewer = (Player) sender;

        if (args.length != 1) {
            sendUsage(sender);
            return;
        }

        String targetName = args[0];

        Player target = getPlayer(targetName);

        if (target == null) {
            OfflinePlayer offlineTarget = Bukkit.getOfflinePlayer(targetName);

            if (!offlineTarget.hasPlayedBefore()) {
                sendPlayerNotFound(sender, targetName);
                return;
            }

            CropStorageManager.loadOfflinePlayerData(offlineTarget.getName());

            try {
                viewer.openInventory(new ViewCropStorageGUI(viewer, offlineTarget.getName()).getInventory());
                sendMessage(sender, "viewing_storage", "#player#", offlineTarget.getName());
            } catch (IndexOutOfBoundsException e) {
                sendMessage(sender, "admin.not_enough_slot");
            } catch (Exception e) {
                sendMessage(sender, "admin.error_opening_gui");
            }
            return;
        }

        try {
            viewer.openInventory(new ViewCropStorageGUI(viewer, target).getInventory());
            sendMessage(sender, "viewing_storage", "#player#", target.getName());
        } catch (IndexOutOfBoundsException e) {
            sendMessage(sender, "admin.not_enough_slot");
        } catch (Exception e) {
            sendMessage(sender, "admin.error_opening_gui");
        }
    }

    @Override
    public List<String> getTabCompletions(CommandSender sender, String[] args) {
        List<String> completions = new ArrayList<>();
        if (args.length == 1) {
            StringUtil.copyPartialMatches(args[0], getOnlinePlayerNames(), completions);
        }
        return completions;
    }

    @Override
    public String getPermission() {
        return "storage.cropstorage.view";
    }

    @Override
    public String getUsage() {
        return "/cropstorage view <player>";
    }

    @Override
    public String getDescription() {
        return "View crop storage of another player";
    }
}
