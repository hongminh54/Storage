package net.danh.storage.CMD.handler.admin;

import net.danh.storage.CMD.handler.BaseCommand;
import net.danh.storage.Manager.AutoSaveManager;
import net.danh.storage.Manager.Crafting.CraftingManager;
import net.danh.storage.Manager.EnchantManager;
import net.danh.storage.Manager.Event.EventManager;
import net.danh.storage.Manager.MineManager;
import net.danh.storage.Storage;
import net.danh.storage.Utils.File;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class ReloadCommand extends BaseCommand {

    @Override
    public void execute(CommandSender sender, String[] args) {
        File.reloadFiles();

        Storage plugin = Storage.getStorage();
        if (plugin != null) {
            plugin.applyDebugVanillaStorageConfigIfEnabled();
        }

        MineManager.loadBlocks();
        AutoSaveManager.restartAutoSave();
        EventManager.reloadEvents();
        EnchantManager.loadEnchants();
        CraftingManager.loadRecipes();

        for (Player player : Storage.getStorage().getServer().getOnlinePlayers()) {
            MineManager.convertOfflineData(player);
            MineManager.loadPlayerData(player);
        }

        sendMessage(sender, "admin.reload");
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
        return "/storage reload";
    }

    @Override
    public String getDescription() {
        return "Reload plugin configuration";
    }
}
