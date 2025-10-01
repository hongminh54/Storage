package net.danh.storage.Listeners;

import net.danh.storage.Manager.MythicStorageManager;
import net.danh.storage.Storage;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.server.PluginEnableEvent;

public class MythicMobsLoadListener implements Listener {

    private boolean initialized = false;

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPluginEnable(PluginEnableEvent event) {
        if (initialized) return;

        if (event.getPlugin().getName().equals("MythicMobs")) {
            Storage.getStorage().getLogger().info("[MythicStorage] Detected MythicMobs plugin enabled, initializing MythicStorage...");

            // Initialize MythicStorage after MythicMobs is fully loaded
            initializeMythicStorage();
            initialized = true;
        }
    }

    public void initializeMythicStorage() {
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

    public boolean isInitialized() {
        return initialized;
    }

    public void setInitialized(boolean initialized) {
        this.initialized = initialized;
    }
}
