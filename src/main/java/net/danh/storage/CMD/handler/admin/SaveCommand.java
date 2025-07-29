package net.danh.storage.CMD.handler.admin;

import net.danh.storage.CMD.handler.BaseCommand;
import net.danh.storage.Manager.AutoSaveManager;
import org.bukkit.command.CommandSender;

import java.util.ArrayList;
import java.util.List;

public class SaveCommand extends BaseCommand {

    @Override
    public void execute(CommandSender sender, String[] args) {
        AutoSaveManager.forceSave();
        sendMessage(sender, "admin.autosave.force_save_completed");
    }

    @Override
    public List<String> getTabCompletions(CommandSender sender, String[] args) {
        return new ArrayList<>();
    }

    @Override
    public String getPermission() {
        return "storage.admin.reload";
    }

    @Override
    public String getUsage() {
        return "/storage save";
    }

    @Override
    public String getDescription() {
        return "Force save all player data";
    }
}
