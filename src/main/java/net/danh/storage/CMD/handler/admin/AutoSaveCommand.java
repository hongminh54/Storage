package net.danh.storage.CMD.handler.admin;

import net.danh.storage.CMD.handler.BaseCommand;
import net.danh.storage.Manager.AutoSaveManager;
import org.bukkit.command.CommandSender;

import java.util.ArrayList;
import java.util.List;

public class AutoSaveCommand extends BaseCommand {

    @Override
    public void execute(CommandSender sender, String[] args) {
        String statusOn = getStatusMessage(true);
        String statusOff = getStatusMessage(false);

        sendMessage(sender, "admin.autosave.status_header");
        sendMessage(sender, "admin.autosave.enabled", "#status#",
                AutoSaveManager.isEnabled() ? statusOn : statusOff);
        sendMessage(sender, "admin.autosave.running", "#status#",
                AutoSaveManager.isRunning() ? statusOn : statusOff);
        sendMessage(sender, "admin.autosave.interval", "#minutes#",
                String.valueOf(AutoSaveManager.getIntervalMinutes()));
        sendMessage(sender, "admin.autosave.async", "#status#",
                AutoSaveManager.isAsync() ? statusOn : statusOff);
        sendMessage(sender, "admin.autosave.log_activity", "#status#",
                AutoSaveManager.isLogActivity() ? statusOn : statusOff);
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
        return "/storage autosave";
    }

    @Override
    public String getDescription() {
        return "Show auto-save status";
    }
}
