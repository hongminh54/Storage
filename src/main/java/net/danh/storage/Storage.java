package net.danh.storage;

import net.danh.storage.CMD.StorageCMD;
import net.danh.storage.Database.Database;
import net.danh.storage.Database.SQLite;
import net.danh.storage.GUI.GUI;
import net.danh.storage.Listeners.BlockBreak;
import net.danh.storage.Listeners.BlockPlace;
import net.danh.storage.Listeners.Chat;
import net.danh.storage.Listeners.JoinQuit;
import net.danh.storage.Manager.*;
import net.danh.storage.NMS.NMSAssistant;
import net.danh.storage.Placeholder.PAPI;
import net.danh.storage.Utils.File;
import net.danh.storage.Utils.UpdateChecker;
import net.xconfig.bukkit.model.SimpleConfigurationManager;
import org.bukkit.Bukkit;
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
    public void onLoad() {
        storage = this;
        if (getServer().getPluginManager().getPlugin("WorldGuard") != null) {
            WorldGuard = true;
            net.danh.storage.WorldGuard.WorldGuard.register(storage);
            getLogger().log(Level.INFO, "Hook with WorldGuard");
        }
    }

    @Override
    public void onEnable() {
        getLogger().log(Level.INFO, "Loading...");
        GUI.register(storage);
        SimpleConfigurationManager.register(storage);
        File.loadFiles();
        File.loadGUI();
        File.updateConfig();
        File.updateMessage();
        File.updateEventConfig();
        File.updateEnchantConfig();
        File.updateSpecialMaterialConfig();
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new PAPI().register();
        }
        registerEvents(new UpdateChecker(storage), new JoinQuit(), new BlockBreak(), new Chat(), new BlockPlace());
        new UpdateChecker(storage).fetch();
        new StorageCMD("storage");
        db = new SQLite(Storage.getStorage());
        db.load();
        TransferManager.initialize();
        MineManager.loadBlocks();
        ConvertOreManager.loadConvertOptions();
        AutoSaveManager.initialize();
        EventManager.initialize();
        EnchantManager.loadEnchants();
        SpecialMaterialManager.loadSpecialMaterials();
        getLogger().log(Level.INFO, "Loading completed. Have fun!");
        if (new NMSAssistant().isVersionLessThanOrEqualTo(12)) {
            getLogger().log(Level.WARNING, "Some material can working incorrect way with your version server (" + new NMSAssistant().getNMSVersion() + ")");
            getLogger().log(Level.WARNING, "If material doesn't work, you should go to discord and report to author!");
        }
    }

    @Override
    public void onDisable() {
        getLogger().log(Level.INFO, "Shutting down...");
        EventManager.shutdown();
        AutoSaveManager.stopAutoSave();
        for (Player p : getServer().getOnlinePlayers()) {
            MineManager.savePlayerData(p);
        }
        TransferManager.cancelAllTransfers();
        File.saveFiles();
        getLogger().log(Level.INFO, "Shutting down completed. See you again!");
    }


    public void registerEvents(Listener... listeners) {
        Arrays.asList(listeners).forEach(listener -> getServer().getPluginManager().registerEvents(listener, storage));
    }
}
