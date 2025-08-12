package net.danh.storage.CMD.handler.user;

import net.danh.storage.CMD.handler.BaseCommand;
import net.danh.storage.Manager.ItemManager;
import net.danh.storage.Manager.MineManager;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class ToggleCommand extends BaseCommand {

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!requirePlayer(sender)) {
            return;
        }

        Player player = (Player) sender;
        boolean currentStatus = MineManager.getToggleStatus(player);
        boolean newStatus = !currentStatus;

        MineManager.toggle.put(player, newStatus);

        String statusText = ItemManager.getStatus(player);
        sendMessage(sender, "user.status.toggle", "#status#", statusText);
    }

    @Override
    public List<String> getTabCompletions(CommandSender sender, String[] args) {
        return new ArrayList<>();
    }

    @Override
    public String getPermission() {
        return "storage.toggle";
    }

    @Override
    public String getUsage() {
        return "/storage toggle";
    }

    @Override
    public String getDescription() {
        return "Toggle auto pickup";
    }
}
