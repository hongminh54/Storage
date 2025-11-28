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
        ChatListener.chat_return_page.remove(p);
        ChatListener.chat_mythic_withdraw.remove(p);
        ChatListener.chat_mythic_deposit.remove(p);

        // Cleanup transfer data
        TransferGUI.setWaitingForInput(p, false);
        MythicTransferGUI.setWaitingForInput(p, false);
        TransferManager.cancelTransfer(p);
        MythicTransferManager.cancelTransfer(p);

        // Cleanup transfer GUI data (TransferGUI and TransferMultiGUI auto-cleanup on close)

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
        // Other enchant classes handle their own cleanup or don't have public cleanup methods

        // Cleanup sound tracking data
        SoundManager.cleanupPlayer(p);

        // Cleanup crafting data
        CraftingManager.cancelCrafting(p);
        ChatListener.craftingRequests.remove(p.getUniqueId());

        // Cleanup recipe editing data
        RecipeEditManager.cancelEdit(p);
        RecipeEditorGUI.cleanupBackup(p.getUniqueId());

        // Cleanup remaining chat data
        ChatListener.chat_deposit.remove(p);
        ChatListener.chat_withdraw.remove(p);
        ChatListener.chat_sell.remove(p);
        ChatListener.chat_convert_from.remove(p);
        ChatListener.chat_convert_to.remove(p);

        // Cleanup GUI page tracking
        ViewStorageGUI.playerCurrentPage.remove(p);
        ConvertOreGUI.playerCurrentPage.remove(p);

        // Cleanup GUI item mapper
        GUI.getItemMapper().remove(p.getUniqueId());
    }
}
