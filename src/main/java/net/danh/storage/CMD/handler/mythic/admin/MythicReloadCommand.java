package net.danh.storage.CMD.handler.mythic.admin;

import net.danh.storage.CMD.handler.mythic.MythicCommand;
import net.danh.storage.Manager.MythicStorageManager;
import net.danh.storage.Utils.File;
import org.bukkit.command.CommandSender;

import java.util.ArrayList;
import java.util.List;

public class MythicReloadCommand extends MythicCommand {

    @Override
    public void execute(CommandSender sender, String[] args) {
        File.getFileSetting().reload("mythicstorage.yml", "GUI/mythicstorage.yml", "message.yml");
        MythicStorageManager.reloadConfiguredDrops();
        sendMessage(sender, "admin.reload_success");
    }

    @Override
    public List<String> getTabCompletions(CommandSender sender, String[] args) {
        return new ArrayList<>();
    }

    @Override
    public String getPermission() {
        return "storage.mythicstorage.admin";
    }

    @Override
    public String getUsage() {
        return "/mythicstorage reload";
    }

    @Override
    public String getDescription() {
        return "Reload MythicStorage configuration";
    }
}
