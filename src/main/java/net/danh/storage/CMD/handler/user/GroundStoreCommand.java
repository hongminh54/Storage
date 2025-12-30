package net.danh.storage.CMD.handler.user;

import net.danh.storage.CMD.handler.BaseCommand;
import net.danh.storage.Manager.MineManager;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class GroundStoreCommand extends BaseCommand {

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!requirePlayer(sender)) {
            return;
        }

        Player player = (Player) sender;

        if (!checkPermission(sender, getPermission())) {
            sendMessage(sender, "user.ground_store.no_permission");
            return;
        }

        if (!MineManager.isGroundStoreSystemEnabled()) {
            sendMessage(sender, "user.ground_store.system_disabled");
            return;
        }

        if (isStorageWorldBlacklisted(player)) {
            sendWorldBlacklisted(sender, "Storage", player.getWorld().getName());
            return;
        }

        boolean enabled = MineManager.toggleGroundStore(player);
        sendMessage(sender, enabled
                ? "user.ground_store.toggle_on"
                : "user.ground_store.toggle_off");
    }

    @Override
    public List<String> getTabCompletions(CommandSender sender, String[] args) {
        return new ArrayList<>();
    }

    @Override
    public String getPermission() {
        return "storage.groundstore";
    }

    @Override
    public String getUsage() {
        return "/storage groundstore";
    }

    @Override
    public String getDescription() {
        return "Toggle ground-store mode";
    }
}
