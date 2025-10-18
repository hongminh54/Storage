package net.danh.storage.Listeners;

import net.danh.storage.Enchant.TNTEnchant;
import net.danh.storage.GUI.*;
import net.danh.storage.Manager.*;
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
        MythicStorageManager.loadPlayerData(p);
    }

    @EventHandler
    public void onQuit(@NotNull PlayerQuitEvent e) {
        Player p = e.getPlayer();
        MineManager.savePlayerData(p);
        MythicStorageManager.savePlayerData(p);
        MineManager.cleanupPlayerData(p);
        MythicStorageManager.cleanupPlayerData(p);
        MythicMobDeath.cleanupPlayer(p);
        PersonalStorage.playerCurrentPage.remove(p);
        MythicStorageGUI.playerCurrentPage.remove(p);
        ViewMythicStorageGUI.playerCurrentPage.remove(p);
        Chat.chat_return_page.remove(p);
        Chat.chat_mythic_withdraw.remove(p);
        Chat.chat_mythic_deposit.remove(p);

        // Cleanup transfer data
        TransferGUI.setWaitingForInput(p, false);
        MythicTransferGUI.setWaitingForInput(p, false);
        TransferManager.cancelTransfer(p);
        MythicTransferManager.cancelTransfer(p);

        // Cleanup transfer GUI data
        if (TransferMultiGUI.getActiveGUI(p) != null) {
            // GUI will be auto-closed by inventory close event
        }

        if (MythicTransferGUI.getActiveGUI(p) != null) {
            MythicTransferGUI.removeActiveGUI(p);
        }

        if (MythicTransferMultiGUI.getActiveGUI(p) != null) {
            MythicTransferMultiGUI.removeActiveGUI(p);
        }

        // Cleanup storage full notification data
        StorageFullNotificationManager.removePlayer(p);

        // Cleanup enchant cooldown data
        TNTEnchant.clearPlayerCooldown(p);

        // Cleanup sound tracking data
        SoundManager.cleanupPlayer(p);
    }
}
