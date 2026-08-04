package net.danh.storage.Listeners;

import net.danh.storage.Enchant.HasteEnchant;
import net.danh.storage.Enchant.MultiplierEnchant;
import net.danh.storage.Enchant.TNTEnchant;
import net.danh.storage.Enchant.VeinMinerEnchant;
import net.danh.storage.GUI.*;
import net.danh.storage.GUI.Crafting.RecipeEditorGUI;
import net.danh.storage.GUI.Crop.CropStorageGUI;
import net.danh.storage.GUI.Crop.CropTransferGUI;
import net.danh.storage.GUI.Crop.CropTransferMultiGUI;
import net.danh.storage.GUI.Crop.ViewCropStorageGUI;
import net.danh.storage.GUI.Mob.MobStorageGUI;
import net.danh.storage.GUI.Mob.MobTransferGUI;
import net.danh.storage.GUI.Mob.MobTransferMultiGUI;
import net.danh.storage.GUI.Mythic.MythicStorageGUI;
import net.danh.storage.GUI.Mythic.MythicTransferGUI;
import net.danh.storage.GUI.Mythic.MythicTransferMultiGUI;
import net.danh.storage.GUI.Mythic.ViewMythicStorageGUI;
import net.danh.storage.Listeners.Mob.MobDeath;
import net.danh.storage.Listeners.Mythic.MythicMobDeath;
import net.danh.storage.Manager.Crafting.CraftingManager;
import net.danh.storage.Manager.Crafting.RecipeEditManager;
import net.danh.storage.Manager.Crop.CropStorageManager;
import net.danh.storage.Manager.Crop.CropTransferManager;
import net.danh.storage.Manager.Friend.FriendManager;
import net.danh.storage.Manager.*;
import net.danh.storage.Manager.Mob.MobStorageManager;
import net.danh.storage.Manager.Mob.MobTransferManager;
import net.danh.storage.Manager.Mythic.MythicStorageManager;
import net.danh.storage.Manager.Mythic.MythicTransferManager;
import net.danh.storage.Storage;
import net.danh.storage.Utils.SchedulerUtil;
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
        CropStorageManager.loadPlayerData(p);
        MobStorageManager.loadPlayerData(p);
        FriendManager.loadPlayerDataAsync(p, () -> {
            if (p.isOnline()) {
                FriendManager.notifyPendingRequests(p);
            }
        });

        SchedulerUtil.runTaskLater(Storage.getStorage(), p, () -> {
            if (!p.isOnline()) {
                return;
            }
            MineManager.scheduleAutoSellOnJoin(p);
            CropStorageManager.scheduleAutoSellOnJoin(p);
            MobStorageManager.scheduleAutoSellOnJoin(p);
        }, 1L);
    }

    @EventHandler
    public void onQuit(@NotNull PlayerQuitEvent e) {
        Player p = e.getPlayer();
        MineManager.savePlayerData(p);
        MineManager.cleanupPlayerData(p);
        FriendManager.cleanupPlayerData(p);

        if (MythicStorageManager.isSystemEnabled()) {
            MythicStorageManager.savePlayerData(p);
            MythicStorageManager.cleanupPlayerData(p);
            MythicMobDeath.cleanupPlayer(p);
        }
        if (CropStorageManager.isSystemEnabled()) {
            CropStorageManager.savePlayerData(p);
            CropStorageManager.cleanupPlayerData(p);
        }
        if (MobStorageManager.isSystemEnabled()) {
            MobStorageManager.savePlayerData(p);
            MobStorageManager.cleanupPlayerData(p);
            MobDeath.cleanupPlayer(p);
        }
        PersonalStorage.playerCurrentPage.remove(p.getUniqueId());
        MythicStorageGUI.playerCurrentPage.remove(p.getUniqueId());
        ViewMythicStorageGUI.playerCurrentPage.remove(p.getUniqueId());
        CropStorageGUI.playerCurrentPage.remove(p.getUniqueId());
        ViewCropStorageGUI.playerCurrentPage.remove(p.getUniqueId());
        MobStorageGUI.playerCurrentPage.remove(p.getUniqueId());
        ChatListener.chat_return_page.remove(p.getUniqueId());
        ChatListener.chat_mythic_withdraw.remove(p.getUniqueId());
        ChatListener.chat_mythic_deposit.remove(p.getUniqueId());
        ChatListener.chat_crop_withdraw.remove(p.getUniqueId());
        ChatListener.chat_crop_deposit.remove(p.getUniqueId());
        ChatListener.chat_crop_sell.remove(p.getUniqueId());
        ChatListener.chat_mob_withdraw.remove(p.getUniqueId());
        ChatListener.chat_mob_deposit.remove(p.getUniqueId());
        ChatListener.chat_mob_sell.remove(p.getUniqueId());


        // Cleanup transfer data
        TransferGUI.setWaitingForInput(p, false);
        MythicTransferGUI.setWaitingForInput(p, false);
        CropTransferGUI.setWaitingForInput(p, false);
        CropTransferGUI.setWaitingForReceiver(p, false);
        CropTransferMultiGUI.setWaitingForReceiver(p, false);
        TransferManager.cancelTransfer(p);
        MythicTransferManager.cancelTransfer(p);
        CropTransferManager.cancelTransfer(p);
        MobTransferManager.cancelTransfer(p);
        MobTransferGUI.setWaitingForInput(p, false);
        MobTransferMultiGUI.setWaitingForReceiver(p, false);

        // Cleanup transfer GUI data (TransferGUI and TransferMultiGUI auto-cleanup on
        // close)

        if (MythicTransferGUI.getActiveGUI(p) != null) {
            MythicTransferGUI.removeActiveGUI(p);
        }

        if (MythicTransferMultiGUI.getActiveGUI(p) != null) {
            MythicTransferMultiGUI.removeActiveGUI(p);
        }

        if (CropTransferGUI.getActiveGUI(p) != null) {
            CropTransferGUI.removeActiveGUI(p);
        }

        if (CropTransferMultiGUI.getActiveGUI(p) != null) {
            CropTransferMultiGUI.removeActiveGUI(p);
        }

        if (MobTransferGUI.getActiveGUI(p) != null) {
            MobTransferGUI.removeActiveGUI(p);
        }

        if (MobTransferMultiGUI.getActiveGUI(p) != null) {
            MobTransferMultiGUI.removeActiveGUI(p);
        }

        // Cleanup crop transfer chat data
        ChatListener.chat_crop_multi_transfer_item.remove(p.getUniqueId());
        ChatListener.chat_crop_multi_transfer_target.remove(p.getUniqueId());

        // Cleanup mob transfer chat data
        ChatListener.chat_mob_multi_transfer_item.remove(p.getUniqueId());
        ChatListener.chat_mob_multi_transfer_target.remove(p.getUniqueId());

        // Cleanup storage full notification data
        StorageFullNotificationManager.removePlayer(p);

        // Cleanup enchant cooldown data
        TNTEnchant.clearPlayerCooldown(p);
        HasteEnchant.clearPlayerCooldown(p);
        MultiplierEnchant.clearPlayerCooldown(p);
        VeinMinerEnchant.clearPlayerCooldown(p);

        // Cleanup sound tracking data
        SoundManager.cleanupPlayer(p);

        // Cleanup particle tracking data
        ParticleManager.cleanupPlayer(p);

        // Cleanup crafting data
        CraftingManager.cancelCrafting(p);
        ChatListener.craftingRequests.remove(p.getUniqueId());

        // Cleanup recipe editing data
        RecipeEditManager.cancelEdit(p);
        RecipeEditorGUI.cleanupBackup(p.getUniqueId());

        // Cleanup remaining chat data
        ChatListener.chat_deposit.remove(p.getUniqueId());
        ChatListener.chat_withdraw.remove(p.getUniqueId());
        ChatListener.chat_sell.remove(p.getUniqueId());
        ChatListener.chat_convert_from.remove(p.getUniqueId());
        ChatListener.chat_convert_to.remove(p.getUniqueId());

        // Cleanup GUI page tracking
        ViewStorageGUI.playerCurrentPage.remove(p.getUniqueId());
        ConvertOreGUI.playerCurrentPage.remove(p.getUniqueId());

        // Cleanup GUI item mapper
        GUI.getItemMapper().remove(p.getUniqueId());
    }
}
