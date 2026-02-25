package net.danh.storage.Listeners;

import net.danh.storage.Storage;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.event.server.PluginEnableEvent;
import org.bukkit.event.server.ServiceRegisterEvent;
import org.bukkit.event.server.ServiceUnregisterEvent;

public class PluginLoadListener implements Listener {

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPluginEnable(PluginEnableEvent event) {
        String name = event.getPlugin().getName();
        if (name == null) {
            return;
        }
        if (name.equals("Vault")) {
            Storage.refreshVaultEconomyHook();
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
        if (name.equals("Vault")) {
            Storage.refreshVaultEconomyHook();
        }
        if (name.equals("MMOItems")) {
            Storage.updateMmoitemsHookState(false);
            return;
        }
        if (name.equals("MythicLib")) {
            Storage.updateMythicLibHookState(false);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onServiceRegister(ServiceRegisterEvent event) {
        if (event.getProvider() == null) {
            return;
        }
        Class<?> service = event.getProvider().getService();
        if (service == net.milkbowl.vault2.economy.Economy.class || service == Economy.class) {
            Storage.refreshVaultEconomyHook();
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onServiceUnregister(ServiceUnregisterEvent event) {
        if (event.getProvider() == null) {
            return;
        }
        Class<?> service = event.getProvider().getService();
        if (service == net.milkbowl.vault2.economy.Economy.class || service == Economy.class) {
            Storage.refreshVaultEconomyHook();
        }
    }
}
