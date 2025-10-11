package net.danh.storage.Listeners;

import net.danh.storage.Manager.MythicStorageManager;
import net.danh.storage.Storage;
import net.danh.storage.Utils.SchedulerUtil;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.server.PluginEnableEvent;

public class MythicMobsLoadListener implements Listener {

    private boolean initialized = false;

    public MythicMobsLoadListener() {
        SchedulerUtil.runTaskLater(Storage.getStorage(), this::checkTimeout, 300L);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPluginEnable(PluginEnableEvent event) {
        if (initialized) return;

        if (event.getPlugin().getName().equals("MythicMobs")) {
            Storage.getStorage().getLogger().info("[MythicStorage] MythicMobs plugin enabled, initializing MythicStorage...");
            initializeMythicStorage();
            initialized = true;
            unregister();
        }
    }

    private void checkTimeout() {
        if (initialized) return;

        if (Bukkit.getPluginManager().getPlugin("MythicMobs") == null) {
            Storage.getStorage().getLogger().info("[MythicStorage] MythicMobs not found after timeout - feature disabled");
            initialized = true;
            unregister();
        }
    }

    private void initializeMythicStorage() {
        try {
            MythicStorageManager.initialize();

            if (MythicStorageManager.isSystemEnabled()) {
                MythicMobDeath.registerListener(Storage.getStorage());
                Storage.getStorage().getLogger().info("[MythicStorage] Successfully initialized after MythicMobs load");
            }
        } catch (Exception e) {
            Storage.getStorage().getLogger().warning("[MythicStorage] Failed to initialize: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void unregister() {
        HandlerList.unregisterAll(this);
    }

    public boolean isInitialized() {
        return initialized;
    }
}
