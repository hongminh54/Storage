package net.danh.storage;

import dev.digitality.digitalgui.DigitalGUI;
import net.danh.storage.CMD.StorageCMD;
import net.danh.storage.Listeners.BlockBreak;
import net.danh.storage.Listeners.JoinQuit;
import net.danh.storage.Manager.DatabaseManager.Database;
import net.danh.storage.Manager.DatabaseManager.SQLite;
import net.danh.storage.Manager.GameManager.MineManager;
import net.danh.storage.Manager.UtilsManager.FileManager;
import net.danh.storage.Manager.UtilsManager.GitManager;
import net.danh.storage.NMS.NMSAssistant;
import net.danh.storage.Utils.UpdateChecker;
import net.xconfig.bukkit.model.SimpleConfigurationManager;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Arrays;
import java.util.logging.Level;

public final class Storage extends JavaPlugin {

    public static Database db;
    private static Storage storage;

    private static boolean WorldGuard;

    public static Storage getStorage() {
        return storage;
    }

    public static boolean isWorldGuardInstalled() {
        return WorldGuard;
    }

    @Override
    public void onEnable() {
        storage = this;
        DigitalGUI.register(storage);
        SimpleConfigurationManager.register(storage);
        FileManager.loadFiles();
        registerEvents(new UpdateChecker(storage), new JoinQuit(), new BlockBreak());
        new UpdateChecker(storage).fetch();
        GitManager.checkGitUpdate();
        new StorageCMD("storage");
        db = new SQLite(Storage.getStorage());
        db.load();
        MineManager.loadBlocks();
        if (getServer().getPluginManager().isPluginEnabled("WorldGuard")) {
            WorldGuard = true;
            getLogger().log(Level.INFO, "[✔️] WorldGuard Support");
        }
        if (new NMSAssistant().isVersionLessThanOrEqualTo(12)) {
            getLogger().log(Level.WARNING, "Some material can working incorrect way with your version server (" + new NMSAssistant().getNMSVersion() + ")");
            getLogger().log(Level.WARNING, "If material doesn't work, you should go to discord and report to author!");
        }
    }

    @Override
    public void onDisable() {
        for (Player p : getServer().getOnlinePlayers()) {
            MineManager.savePlayerData(p);
        }
        FileManager.saveFiles();
    }


    public void registerEvents(Listener... listeners) {
        Arrays.asList(listeners).forEach(listener -> getServer().getPluginManager().registerEvents(listener, storage));
    }
}
