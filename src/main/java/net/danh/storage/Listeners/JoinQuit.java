package net.danh.storage.Listeners;

import net.danh.storage.Manager.MineManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.jetbrains.annotations.NotNull;

public class JoinQuit implements Listener {

    @EventHandler
    public void onJoin(@NotNull PlayerJoinEvent e) {
        Player p = e.getPlayer();
        MineManager.loadPlayerData(p);
        MineManager.toggle.put(p, true);
    }

    @EventHandler
    public void onQuit(@NotNull PlayerQuitEvent e) {
        Player p = e.getPlayer();
        MineManager.savePlayerData(p);
    }
}
