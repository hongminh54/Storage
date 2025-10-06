package net.danh.storage.CMD;

import net.danh.storage.API.CMDBase;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class MythicStorageCMD extends CMDBase {

    private final MythicStorageCommandManager commandManager;

    public MythicStorageCMD(String name) {
        super(name);
        this.commandManager = new MythicStorageCommandManager();
    }

    @Override
    public void execute(@NotNull CommandSender sender, String[] args) {
        commandManager.handleCommand(sender, args);
    }

    @Override
    public List<String> TabComplete(@NotNull CommandSender sender, String[] args) {
        return commandManager.getCommandTabCompletions(sender, args);
    }
}
