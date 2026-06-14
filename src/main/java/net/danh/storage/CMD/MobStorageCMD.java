package net.danh.storage.CMD;

import net.danh.storage.API.CMDBase;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class MobStorageCMD extends CMDBase {

    private final MobStorageCommandManager commandManager;

    public MobStorageCMD(String name) {
        super(name);
        this.commandManager = new MobStorageCommandManager();
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
