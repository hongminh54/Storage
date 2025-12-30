package net.danh.storage.CMD.handler.mythic.user;

import net.danh.storage.CMD.handler.mythic.MythicCommand;
import net.danh.storage.Manager.MythicStorageManager;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class MythicGroundStoreCommand extends MythicCommand {

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

        if (!MythicStorageManager.isGroundStoreSystemEnabled()) {
            sendMessage(sender, "ground_store_system_disabled");
            return;
        }

        if (isMythicStorageWorldBlacklisted(player)) {
            sendWorldBlacklisted(sender, "MythicStorage", player.getWorld().getName());
            return;
        }

        boolean enabled = MythicStorageManager.toggleGroundStore(player);
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
        return "storage.mythicstorage.groundstore";
    }

    @Override
    public String getUsage() {
        return "/mythicstorage groundstore";
    }

    @Override
    public String getDescription() {
        return "Toggle MythicStorage ground-store mode";
    }
}
