package net.danh.storage.CMD.handler;

import org.bukkit.command.CommandSender;

import java.util.List;

public interface CommandHandler {

    void execute(CommandSender sender, String[] args);

    List<String> getTabCompletions(CommandSender sender, String[] args);

    String getPermission();

    String getUsage();

    String getDescription();
}
