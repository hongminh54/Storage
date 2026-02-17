package net.danh.storage.CMD.handler.crop.user;

import net.danh.storage.CMD.handler.crop.CropCommand;
import net.danh.storage.Manager.Crop.CropStorageManager;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class CropGroundStoreCommand extends CropCommand {

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!requirePlayer(sender)) {
            return;
        }

        Player player = (Player) sender;

        if (!checkPermission(sender, getPermission())) {
            sendMessage(sender, "ground_store_no_permission");
            return;
        }

        if (!CropStorageManager.isGroundStoreSystemEnabled()) {
            sendMessage(sender, "ground_store_system_disabled");
            return;
        }

        if (isCropStorageWorldBlacklisted(player)) {
            sendWorldBlacklisted(sender, "CropStorage", player.getWorld().getName());
            return;
        }

        boolean enabled = CropStorageManager.toggleGroundStore(player);
        sendMessage(sender, enabled
                ? "ground_store_toggle_on"
                : "ground_store_toggle_off");
    }

    @Override
    public List<String> getTabCompletions(CommandSender sender, String[] args) {
        return new ArrayList<>();
    }

    @Override
    public String getPermission() {
        return "storage.cropstorage.groundstore";
    }

    @Override
    public String getUsage() {
        return "/cropstorage groundstore";
    }

    @Override
    public String getDescription() {
        return "Toggle CropStorage ground-store mode";
    }
}
