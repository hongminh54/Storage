package net.danh.storage.CMD;

import net.danh.storage.API.CMDBase;
import net.danh.storage.CMD.handler.MythicStorageCmdHandler;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class MythicStorageCMD extends CMDBase {

    private final MythicStorageCmdHandler commandHandler;

    public MythicStorageCMD(String name) {
        super(name);
        this.commandHandler = new MythicStorageCmdHandler();
    }

    @Override
    public void execute(@NotNull CommandSender sender, String[] args) {
        commandHandler.handleCommand(sender, args);
    }

    @Override
    public List<String> TabComplete(@NotNull CommandSender sender, String[] args) {
        return commandHandler.getTabCompletions(sender, args);
    }
}
