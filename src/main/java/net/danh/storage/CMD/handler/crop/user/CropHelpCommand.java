package net.danh.storage.CMD.handler.crop.user;

import net.danh.storage.CMD.handler.crop.CropCommand;
import org.bukkit.command.CommandSender;

import java.util.ArrayList;
import java.util.List;

public class CropHelpCommand extends CropCommand {

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (sender.hasPermission("storage.cropstorage.admin")) {
            sendMessageList(sender, "admin.help");
        }
        sendMessageList(sender, "user.help");
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
        return "/cropstorage help";
    }

    @Override
    public String getDescription() {
        return "Show CropStorage help";
    }
}
