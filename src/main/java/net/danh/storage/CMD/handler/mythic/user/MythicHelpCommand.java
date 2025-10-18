package net.danh.storage.CMD.handler.mythic.user;

import net.danh.storage.CMD.handler.mythic.MythicCommand;
import org.bukkit.command.CommandSender;

import java.util.ArrayList;
import java.util.List;

public class MythicHelpCommand extends MythicCommand {

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (sender.hasPermission("storage.mythicstorage.admin")) {
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
        return "/mythicstorage help";
    }

    @Override
    public String getDescription() {
        return "Show MythicStorage help";
    }
}
