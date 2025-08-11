package net.danh.storage.CMD.handler.user;

import net.danh.storage.CMD.handler.BaseCommand;
import net.danh.storage.Manager.MineManager;
import net.danh.storage.Utils.PermissionStorageLimit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class StorageLimitCommand extends BaseCommand {

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!requirePlayer(sender)) {
            return;
        }

        Player player = (Player) sender;
        int currentLimit = MineManager.getMaxBlock(player);
        
        if (PermissionStorageLimit.hasStoragePermissionsConfigured()) {
            sendColorizedMessage(sender, "user.storage_limit_info", "#limit#", String.valueOf(currentLimit));
        } else {
            sendColorizedMessage(sender, "user.storage_limit_default", "#limit#", String.valueOf(currentLimit));
        }
    }

    @Override
    public List<String> getTabCompletions(CommandSender sender, String[] args) {
        return new ArrayList<>();
    }

    @Override
    public String getPermission() {
        return null;
    }

    @Override
    public String getUsage() {
        return "/storage limit";
    }

    @Override
    public String getDescription() {
        return "Check your storage limit";
    }
}