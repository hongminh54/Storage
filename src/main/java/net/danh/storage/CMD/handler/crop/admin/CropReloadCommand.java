package net.danh.storage.CMD.handler.crop.admin;

import net.danh.storage.CMD.handler.crop.CropCommand;
import net.danh.storage.Manager.Crop.CropStorageManager;
import net.danh.storage.Utils.ChatUtils;
import net.danh.storage.Utils.File;
import org.bukkit.command.CommandSender;

import java.util.ArrayList;
import java.util.List;

public class CropReloadCommand extends CropCommand {

    @Override
    public void execute(CommandSender sender, String[] args) {
        File.getFileSetting().reload("cropstorage.yml", "GUI/cropstorage.yml", "message.yml");
        CropStorageManager.reloadConfiguredDrops();

        sendMessage(sender, "admin.reload_success");

        if (CropStorageManager.hasInvalidItems()) {
            sender.sendMessage(ChatUtils.colorizewp("&c&l[!] WARNING: Invalid items detected!"));
            sender.sendMessage(ChatUtils.colorizewp("&e" + CropStorageManager.getInvalidItems().size()
                    + " &7item(s) failed to load: &c" + String.join(", ", CropStorageManager.getInvalidItems())));
            sender.sendMessage(ChatUtils.colorizewp("&7These items will &cNOT &7appear in the GUI!"));
            sender.sendMessage(ChatUtils.colorizewp("&7Check console for detailed error messages and fixes"));
        } else {
            sender.sendMessage(ChatUtils.colorizewp("&a✓ All items loaded successfully!"));
        }
    }

    @Override
    public List<String> getTabCompletions(CommandSender sender, String[] args) {
        return new ArrayList<>();
    }

    @Override
    public String getPermission() {
        return "storage.cropstorage.admin";
    }

    @Override
    public String getUsage() {
        return "/cropstorage reload";
    }

    @Override
    public String getDescription() {
        return "Reload CropStorage configuration";
    }
}
