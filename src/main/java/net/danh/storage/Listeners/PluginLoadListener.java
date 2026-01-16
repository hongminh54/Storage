package net.danh.storage.Listeners;

import net.danh.storage.Storage;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.event.server.PluginEnableEvent;

public class PluginLoadListener implements Listener {

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPluginEnable(PluginEnableEvent event) {
        String name = event.getPlugin().getName();
        if (name == null) {
            return;
        }
        if (name.equals("MMOItems")) {
            Storage.updateMmoitemsHookState(true);
            return;
        }
        if (name.equals("MythicLib")) {
            Storage.updateMythicLibHookState(true);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPluginDisable(PluginDisableEvent event) {
        String name = event.getPlugin().getName();
        if (name == null) {
            return;
        }
        if (name.equals("MMOItems")) {
            Storage.updateMmoitemsHookState(false);
            return;
        }
        if (name.equals("MythicLib")) {
            Storage.updateMythicLibHookState(false);
        }
    }
}
