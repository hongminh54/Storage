package net.danh.storage.Listeners;

import net.danh.storage.Enchant.TNTEnchant;
import net.danh.storage.GUI.PersonalStorage;
import net.danh.storage.GUI.TransferGUI;
import net.danh.storage.Manager.MineManager;
import net.danh.storage.Manager.SoundManager;
import net.danh.storage.Manager.StorageFullNotificationManager;
import net.danh.storage.Manager.TransferManager;
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
    }

    @EventHandler
    public void onQuit(@NotNull PlayerQuitEvent e) {
        Player p = e.getPlayer();
        MineManager.savePlayerData(p);
        PersonalStorage.playerCurrentPage.remove(p);
        Chat.chat_return_page.remove(p);

        // Cleanup transfer data
        TransferGUI.setWaitingForInput(p, false);
        TransferManager.cancelTransfer(p);

        // Cleanup storage full notification data
        StorageFullNotificationManager.removePlayer(p);

        // Cleanup enchant cooldown data
        TNTEnchant.clearPlayerCooldown(p);

        // Cleanup sound tracking data
        SoundManager.cleanupPlayer(p);
    }
}
