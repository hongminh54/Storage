package net.danh.storage.CMD.handler.user;

import net.danh.storage.CMD.handler.BaseCommand;
import org.bukkit.command.CommandSender;

import java.util.ArrayList;
import java.util.List;

public class HelpCommand extends BaseCommand {

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!requirePlayer(sender)) {
            return;
        }

        if (sender.hasPermission("storage.admin")) {
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
        return "/storage help";
    }

    @Override
    public String getDescription() {
        return "Show help information";
    }
}
